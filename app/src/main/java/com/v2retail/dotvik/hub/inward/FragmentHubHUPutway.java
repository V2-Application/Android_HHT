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
 * FragmentHubHUPutway — HUB HU Putway process
 *
 * RFC: ZWM_HUB_HU_PUTWAY_RFC
 *   IV_WERKS     — Plant (from SharedPreferences "WERKS", read-only)
 *   IV_CRATE_HU  — Crate or HU being put away (scanned/typed)
 *   IV_BIN       — Destination bin (scanned/typed)
 *   IV_LGTYP     — Storage Type (scanned/typed, triggers submit)
 *   ES_RETURN    — BAPIRET2 {TYPE, MESSAGE}
 *
 * Flow:
 *   1. Plant pre-filled from login.
 *   2. User scans/enters Crate HU → cursor auto-advances to BIN.
 *   3. User scans/enters BIN → cursor auto-advances to Storage Type.
 *   4. User scans/enters Storage Type → request auto-submitted.
 *   5. ES_RETURN shown colour-coded (E=red, S=green, W/I=black).
 *   6. On success, Crate HU is cleared for the next item; BIN + Storage Type
 *      are retained so batch putaway to the same destination doesn't require
 *      re-scanning location every time.
 */
public class FragmentHubHUPutway extends Fragment {

    private static final String TAG = "FragmentHubHUPutway";

    View rootView;
    String URL   = "";
    String WERKS = "";
    String USER  = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_werks;
    EditText txt_crate_hu;
    EditText txt_bin;
    EditText txt_lgtyp;
    TextView txt_result_type;
    TextView txt_result_message;

    // Guard against rapid double-trigger from scanner echo
    private boolean inFlight = false;

    public FragmentHubHUPutway() {}

    public static FragmentHubHUPutway newInstance() {
        return new FragmentHubHUPutway();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("HUB HU Putway");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_hub_hu_putway, container, false);
        con = getContext();
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        txt_werks          = rootView.findViewById(R.id.txt_hub_putway_werks);
        txt_crate_hu       = rootView.findViewById(R.id.txt_hub_putway_crate_hu);
        txt_bin            = rootView.findViewById(R.id.txt_hub_putway_bin);
        txt_lgtyp          = rootView.findViewById(R.id.txt_hub_putway_lgtyp);
        txt_result_type    = rootView.findViewById(R.id.txt_hub_putway_result_type);
        txt_result_message = rootView.findViewById(R.id.txt_hub_putway_result_message);

        txt_werks.setText(WERKS);

        addInputEvents();
        resetResultPanel();

        // Initial cursor on Crate HU (Plant is auto-filled)
        txt_crate_hu.requestFocus();

        return rootView;
    }

    // ─── Input events ────────────────────────────────────────────────────────

    private void addInputEvents() {
        // CRATE HU: scanner paste or Enter/Next → advance to BIN
        attachAdvanceOnScan(txt_crate_hu, txt_bin, "Crate HU");

        // BIN: scanner paste or Enter/Next → advance to Storage Type
        attachAdvanceOnScan(txt_bin, txt_lgtyp, "BIN");

        // STORAGE TYPE (last field): scanner paste or Enter/Done → submit
        txt_lgtyp.addTextChangedListener(new TextWatcher() {
            boolean fromScanner = false;
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fromScanner = (before == 0 && start == 0 && count > 1);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (fromScanner && s.toString().trim().length() > 0) {
                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() { submitPutway(); }
                    }, 200);
                }
            }
        });

        txt_lgtyp.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    UIFuncs.hideKeyboard(getActivity());
                    submitPutway();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Shared helper: when `from` gets a scanner-paste or Enter/Next, move focus
     * to `next`. Used for the intermediate fields (Crate HU → BIN → Storage).
     */
    private void attachAdvanceOnScan(final EditText from, final EditText next,
                                     final String label) {
        from.addTextChangedListener(new TextWatcher() {
            boolean fromScanner = false;
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fromScanner = (before == 0 && start == 0 && count > 1);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (fromScanner && s.toString().trim().length() > 0) {
                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() { next.requestFocus(); }
                    }, 150);
                }
            }
        });

        from.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String val = UIFuncs.toUpperTrim(from);
                    if (val.isEmpty()) {
                        showError("Required", "Please scan or enter " + label);
                        from.requestFocus();
                        return true;
                    }
                    next.requestFocus();
                    return true;
                }
                return false;
            }
        });
    }

    // ─── Submit ───────────────────────────────────────────────────────────────

    private void submitPutway() {
        String crateHu = UIFuncs.toUpperTrim(txt_crate_hu);
        String bin     = UIFuncs.toUpperTrim(txt_bin);
        String lgtyp   = UIFuncs.toUpperTrim(txt_lgtyp);

        if (WERKS == null || WERKS.isEmpty()) {
            showError("Config Error",
                    "Plant (WERKS) not set on device. Please log in again.");
            return;
        }
        if (crateHu.isEmpty()) {
            showError("Required", "Please scan or enter Crate HU");
            txt_crate_hu.requestFocus();
            return;
        }
        if (bin.isEmpty()) {
            showError("Required", "Please scan or enter BIN");
            txt_bin.requestFocus();
            return;
        }
        if (lgtyp.isEmpty()) {
            showError("Required", "Please scan or enter Storage Type");
            txt_lgtyp.requestFocus();
            return;
        }

        if (inFlight) {
            Log.d(TAG, "submitPutway ignored — request already in flight");
            return;
        }
        inFlight = true;

        Log.d(TAG, "submitPutway → WERKS=" + WERKS
                + " CRATE_HU=" + crateHu
                + " BIN=" + bin
                + " LGTYP=" + lgtyp);

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname",    Vars.ZWM_HUB_HU_PUTWAY_RFC);
            args.put("IV_WERKS",    WERKS);
            args.put("IV_CRATE_HU", crateHu);
            args.put("IV_BIN",      bin);
            args.put("IV_LGTYP",    lgtyp);
        } catch (JSONException e) {
            inFlight = false;
            e.printStackTrace();
            box.getErrBox(e);
            return;
        }

        showProcessingAndSubmit(Vars.ZWM_HUB_HU_PUTWAY_RFC, args);
    }

    // ─── Network ──────────────────────────────────────────────────────────────

    private void showProcessingAndSubmit(final String rfc, final JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Processing HU Putway...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, args);
                } catch (Exception e) {
                    inFlight = false;
                    dismissDialog();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }

    private void submitRequest(final String rfc, final JSONObject args) {
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
                        inFlight = false;
                        Log.d(TAG, "Response → " + response);
                        if (response == null) {
                            UIFuncs.errorSound(con);
                            box.getBox("Error", "No response from server");
                        } else {
                            handleResponse(response);
                        }
                        // Clear Crate HU for next item; retain BIN + Storage Type
                        // so a batch of HUs going to the same location doesn't
                        // require re-scanning destination every time.
                        txt_crate_hu.setText("");
                        txt_crate_hu.requestFocus();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error → " + error);
                        dismissDialog();
                        inFlight = false;
                        String msg;
                        if      (error instanceof TimeoutError
                              || error instanceof NoConnectionError) msg = "Communication Error!";
                        else if (error instanceof AuthFailureError)  msg = "Authentication Error!";
                        else if (error instanceof ServerError)       msg = "Server Side Error!";
                        else if (error instanceof NetworkError)      msg = "Network Error!";
                        else if (error instanceof ParseError)        msg = "Parse Error!";
                        else                                          msg = error.toString();
                        new AlertBox(getContext()).getBox("Error", msg);
                        txt_crate_hu.setText("");
                        txt_crate_hu.requestFocus();
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
                String type    = ret.optString("TYPE",    "").trim();
                String message = ret.optString("MESSAGE", "No message returned");

                txt_result_type.setText("TYPE: " + type);
                txt_result_message.setText(message);

                if ("E".equalsIgnoreCase(type)) {
                    UIFuncs.errorSound(con);
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.holo_red_dark));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.holo_red_dark));
                } else if ("S".equalsIgnoreCase(type)) {
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.holo_green_dark));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.black));
                } else {
                    // Unknown TYPE (W / I / empty) — neutral
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
