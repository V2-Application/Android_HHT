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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * FragmentHUStockReviewV11 — V11-V01 HU Stock Review at Hub
 *
 * RFC: ZWM_HU_STOCK_REV_RFC
 *   IV_WERKS  — Plant (read from SharedPreferences)
 *   IV_HU     — Handling Unit (scanned/typed)
 *   IV_LGTYP  — Storage Type (always "V11", read-only)
 *   ES_RETURN — BAPIRET2 {TYPE, MESSAGE}
 *
 * QTY behaviour (2026-04-23):
 *   QTY is a SESSION COUNTER — increments by +1 every time SAP returns TYPE="S".
 *   It does NOT reflect any SAP-returned quantity field. Duplicate HU scans within
 *   the same session (same value scanned twice in a row) are ignored to avoid
 *   accidental double-counts from scanner echo.
 */
public class FragmentHUStockReviewV11 extends Fragment {

    private static final String TAG   = "FragmentHUStockRevV11";
    private static final String DEFAULT_LGTYP = "V11";

    View    rootView;
    String  URL   = "";
    String  WERKS = "";
    String  USER  = "";
    Context con;
    AlertBox      box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_werks;
    EditText txt_hu;
    EditText txt_scanned_hu;
    EditText txt_lgtyp;
    EditText txt_qty;
    TextView txt_result_type;
    TextView txt_result_message;

    // ─── Session-scoped QTY state ────────────────────────────────────────────
    private int qtyCount = 0;                         // incremented on each TYPE="S"
    private String lastHu = "";                       // last HU that was sent for SAP
    private boolean inFlight = false;                 // guard against rapid double-scan
    private final Set<String> scannedHUs = new HashSet<>(); // optional dedupe within session

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

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        txt_werks          = rootView.findViewById(R.id.txt_hub_v11_v01_werks);
        txt_hu             = rootView.findViewById(R.id.txt_hub_v11_v01_hu);
        txt_scanned_hu     = rootView.findViewById(R.id.txt_hub_v11_v01_scanned_hu);
        txt_lgtyp          = rootView.findViewById(R.id.txt_hub_v11_v01_lgtyp);
        txt_qty            = rootView.findViewById(R.id.txt_hub_v11_v01_qty);
        txt_result_type    = rootView.findViewById(R.id.txt_hub_v11_v01_result_type);
        txt_result_message = rootView.findViewById(R.id.txt_hub_v11_v01_result_message);

        txt_werks.setText(WERKS);
        txt_lgtyp.setText(DEFAULT_LGTYP);
        txt_scanned_hu.setText("");

        // Initialize QTY counter (session starts at 0)
        qtyCount = 0;
        renderQty();

        addInputEvents();
        resetResultPanel();
        txt_hu.setText("");
        txt_hu.requestFocus();

        return rootView;
    }

    // ─── Input events ────────────────────────────────────────────────────────

    private void addInputEvents() {
        // Auto-submit when scanner pastes value; also submit on IME Done / Enter.
        txt_hu.addTextChangedListener(new TextWatcher() {
            boolean fromScanner = false;
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fromScanner = (before == 0 && start == 0 && count > 3);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (fromScanner && s.toString().trim().length() > 0) {
                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() { submitV11(); }
                    }, 200);
                }
            }
        });

        txt_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    UIFuncs.hideKeyboard(getActivity());
                    submitV11();
                    return true;
                }
                return false;
            }
        });
    }

    // ─── Submit ───────────────────────────────────────────────────────────────

    private void submitV11() {
        String hu = UIFuncs.toUpperTrim(txt_hu);
        String lgtyp = DEFAULT_LGTYP;

        if (hu.isEmpty()) {
            showError("Required", "Please scan or enter HU number");
            txt_hu.requestFocus();
            return;
        }

        if (WERKS == null || WERKS.isEmpty()) {
            showError("Config Error", "Plant (WERKS) not set on device. Please log in again.");
            return;
        }

        // Guard: ignore if a request is already in flight (rapid double-trigger)
        if (inFlight) {
            Log.d(TAG, "submitV11 ignored — request already in flight");
            return;
        }

        // Optional dedupe: same HU scanned twice in a row = scanner echo, skip
        if (hu.equals(lastHu)) {
            Log.d(TAG, "submitV11 ignored — duplicate of last HU (" + hu + ")");
            txt_hu.setText("");
            txt_hu.requestFocus();
            return;
        }

        // Echo scanned value to the read-only Scanned HU field
        txt_scanned_hu.setText(hu);
        lastHu = hu;
        inFlight = true;

        Log.d(TAG, "submitV11 → WERKS=" + WERKS + " HU=" + hu + " LGTYP=" + lgtyp);

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname",  Vars.ZWM_HU_STOCK_REV_RFC);
            args.put("IV_WERKS",  WERKS);
            args.put("IV_HU",     hu);
            args.put("IV_LGTYP",  lgtyp);
        } catch (JSONException e) {
            inFlight = false;
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

        JsonObjectRequest req = new SapJsonObjectRequest(
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
                        // Reset HU input for next scan
                        txt_hu.setText("");
                        txt_hu.requestFocus();
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
                        txt_hu.setText("");
                        txt_hu.requestFocus();
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
                    // ✅ SUCCESS — increment QTY counter
                    incrementQty();
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.holo_green_dark));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.black));
                } else {
                    // Unknown TYPE (W / I / empty) — treat as non-success, don't increment
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

    // ─── QTY counter ─────────────────────────────────────────────────────────

    private void incrementQty() {
        qtyCount++;
        Log.d(TAG, "QTY incremented → " + qtyCount);
        // Track HU for visibility only (dedupe already handled at submit-time)
        if (lastHu != null && !lastHu.isEmpty()) {
            scannedHUs.add(lastHu);
        }
        renderQty();
    }

    private void renderQty() {
        if (txt_qty != null) {
            txt_qty.setText(String.valueOf(qtyCount));
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
