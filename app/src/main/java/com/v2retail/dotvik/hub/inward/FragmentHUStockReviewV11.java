package com.v2retail.dotvik.hub.inward;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * FragmentHUStockReviewV11 — V11-V01 HU Stock Review at Hub
 *
 * RFC: ZWM_HU_STOCK_REV_RFC
 *   IV_WERKS  — Plant (read from SharedPreferences, NOT from disabled EditText)
 *   IV_HU     — Handling Unit (scanned/typed)
 *   IV_LGTYP  — Storage Type (pre-set to "V11")
 *   ES_RETURN — BAPIRET2 {TYPE, MESSAGE}
 *
 * Branch: Android_HHT_Dev | Fixed: 2026-04-22
 */
public class FragmentHUStockReviewV11 extends Fragment implements View.OnClickListener {

    private static final String TAG   = "FragmentHUStockRevV11";
    private static final String DEFAULT_LGTYP = "V11";

    View    rootView;
    String  URL   = "";
    String  WERKS = "";   // always read from SharedPrefs — NEVER from disabled EditText
    String  USER  = "";
    Context con;
    AlertBox      box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_werks;
    EditText txt_hu;
    EditText txt_lgtyp;
    TextView txt_result_type;
    TextView txt_result_message;
    Button   btn_submit;
    Button   btn_clear;

    public FragmentHUStockReviewV11() {}

    public static FragmentHUStockReviewV11 newInstance() {
        return new FragmentHUStockReviewV11();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("V11-V01");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_hub_v11_v01, container, false);
        con  = getContext();
        box  = new AlertBox(con);

        // Load device settings
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        // Bind views
        txt_werks          = rootView.findViewById(R.id.txt_hub_v11_v01_werks);
        txt_hu             = rootView.findViewById(R.id.txt_hub_v11_v01_hu);
        txt_lgtyp          = rootView.findViewById(R.id.txt_hub_v11_v01_lgtyp);
        txt_result_type    = rootView.findViewById(R.id.txt_hub_v11_v01_result_type);
        txt_result_message = rootView.findViewById(R.id.txt_hub_v11_v01_result_message);
        btn_submit         = rootView.findViewById(R.id.btn_hub_v11_v01_submit);
        btn_clear          = rootView.findViewById(R.id.btn_hub_v11_v01_clear);

        btn_submit.setOnClickListener(this);
        btn_clear.setOnClickListener(this);

        // Show plant (display only)
        txt_werks.setText(WERKS);

        // Set Type default — done before clearScreen so clearScreen doesn't wipe WERKS display
        txt_lgtyp.setText(DEFAULT_LGTYP);

        addInputEvents();

        // Reset result panel and focus HU (clearScreen keeps WERKS + lgtyp intact)
        resetResultPanel();
        txt_hu.setText("");
        txt_hu.requestFocus();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_hub_v11_v01_submit) {
            submitV11();
        } else if (id == R.id.btn_hub_v11_v01_clear) {
            clearScreen();
        }
    }

    // ─── Input events ────────────────────────────────────────────────────────

    private void addInputEvents() {
        // HU: scanner auto-submit (Type = V11 already set, no need to pause)
        txt_hu.addTextChangedListener(new TextWatcher() {
            boolean fromScanner = false;
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Scanner pastes all chars at once from empty field
                fromScanner = (before == 0 && start == 0 && count > 3);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (fromScanner && s.toString().trim().length() > 0) {
                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() {
                            if (btn_submit != null) btn_submit.performClick();
                        }
                    }, 200);
                }
            }
        });

        txt_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    txt_lgtyp.requestFocus();
                    txt_lgtyp.selectAll();
                    return true;
                }
                return false;
            }
        });

        // Type: Enter triggers submit
        txt_lgtyp.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    btn_submit.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    // ─── Submit ───────────────────────────────────────────────────────────────

    private void submitV11() {
        // Read HU from field
        String hu = UIFuncs.toUpperTrim(txt_hu);

        // Read Type from field (may have been changed by user); default V11
        String lgtyp = UIFuncs.toUpperTrim(txt_lgtyp);
        if (lgtyp.isEmpty()) lgtyp = DEFAULT_LGTYP;

        // Validate
        if (hu.isEmpty()) {
            showError("Required", "Please scan or enter HU number");
            txt_hu.requestFocus();
            return;
        }

        // Use WERKS from instance variable — not from disabled EditText
        if (WERKS == null || WERKS.isEmpty()) {
            showError("Config Error", "Plant (WERKS) not set on device. Please log in again.");
            return;
        }

        Log.d(TAG, "submitV11 → WERKS=" + WERKS + " HU=" + hu + " LGTYP=" + lgtyp);

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname",  Vars.ZWM_HU_STOCK_REV_RFC);
            args.put("IV_WERKS",  WERKS);   // from SharedPrefs, always reliable
            args.put("IV_HU",     hu);
            args.put("IV_LGTYP",  lgtyp);
        } catch (JSONException e) {
            e.printStackTrace();
            box.getErrBox(e);
            return;
        }

        showProcessingAndSubmit(Vars.ZWM_HU_STOCK_REV_RFC, args);
    }

    // ─── Network ──────────────────────────────────────────────────────────────

    private void showProcessingAndSubmit(final String rfc, final JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Processing V11-V01...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, args);
                } catch (Exception e) {
                    dismissDialog();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }

    private void submitRequest(final String rfc, final JSONObject args) {
        // Match exact URL pattern used by FragmentHUGRC (working reference)
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        Log.d(TAG, "URL  → " + url);
        Log.d(TAG, "Body → " + args.toString());

        final JSONObject params = args;
        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissDialog();
                        Log.d(TAG, "Response → " + response);
                        if (response == null) {
                            UIFuncs.errorSound(con);
                            box.getBox("Error", "No response from server");
                        } else {
                            handleResponse(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error → " + error);
                        dismissDialog();
                        String msg;
                        if      (error instanceof TimeoutError
                              || error instanceof NoConnectionError) msg = "Communication Error!";
                        else if (error instanceof AuthFailureError)  msg = "Authentication Error!";
                        else if (error instanceof ServerError)       msg = "Server Side Error!";
                        else if (error instanceof NetworkError)      msg = "Network Error!";
                        else if (error instanceof ParseError)        msg = "Parse Error!";
                        else                                          msg = error.toString();
                        new AlertBox(getContext()).getBox("Error", msg);
                    }
                }
        ) {
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody()             { return params.toString().getBytes(); }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse r) {
                return super.parseNetworkResponse(r);
            }
        };

        req.setRetryPolicy(new RetryPolicy() {
            @Override public int getCurrentTimeout()    { return 50000; }
            @Override public int getCurrentRetryCount() { return 1; }
            @Override public void retry(VolleyError e) throws VolleyError {}
        });

        queue.add(req);
    }

    // ─── Response handler ────────────────────────────────────────────────────

    private void handleResponse(JSONObject response) {
        try {
            txt_result_type.setVisibility(View.VISIBLE);
            txt_result_message.setVisibility(View.VISIBLE);

            if (response.has("ES_RETURN")
                    && response.get("ES_RETURN") instanceof JSONObject) {

                JSONObject ret = response.getJSONObject("ES_RETURN");
                String type    = ret.optString("TYPE",    "");
                String message = ret.optString("MESSAGE", "No message returned");

                txt_result_type.setText("TYPE: " + type);
                txt_result_message.setText(message);

                if ("E".equals(type)) {
                    UIFuncs.errorSound(con);
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.holo_red_dark));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.holo_red_dark));
                } else if ("S".equals(type)) {
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.holo_green_dark));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.black));
                } else {
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.black));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.black));
                }
            } else {
                txt_result_type.setText("");
                txt_result_message.setText("No return data from SAP");
                txt_result_message.setTextColor(
                        getResources().getColor(android.R.color.darker_gray));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            box.getErrBox(e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void clearScreen() {
        txt_hu.setText("");
        txt_lgtyp.setText(DEFAULT_LGTYP);  // reset Type to V11
        // Keep txt_werks as-is — it shows WERKS from SharedPrefs
        resetResultPanel();
        txt_hu.requestFocus();
    }

    private void resetResultPanel() {
        txt_result_type.setText("");
        txt_result_message.setText("");
        txt_result_type.setVisibility(View.GONE);
        txt_result_message.setVisibility(View.GONE);
    }

    private void showError(String title, String msg) {
        UIFuncs.errorSound(con);
        new AlertBox(getContext()).getBox(title, msg);
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
