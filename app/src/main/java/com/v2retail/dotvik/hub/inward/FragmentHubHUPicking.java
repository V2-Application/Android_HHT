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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * FragmentHubHUPicking — HUB HU Picking process
 *
 * RFC: ZWM_HUB_HU_PICKING_RFC
 *   IV_WERKS  — Plant (from SharedPreferences "WERKS", read-only)
 *   IV_STORE  — Destination store (scanned/typed)
 *   IV_BIN    — Source bin (scanned/typed, triggers submit)
 *   ES_RETURN — BAPIRET2 {TYPE, MESSAGE}
 *
 * Post-response field handling:
 *   SUCCESS (TYPE=S / W / I) → clear BIN only, retain Store for batch picks
 *   ERROR   (TYPE=E / missing ES_RETURN / network / parse fail) →
 *           clear ALL scannable fields, focus back to Store
 */
public class FragmentHubHUPicking extends Fragment {

    private static final String TAG = "FragmentHubHUPicking";

    View rootView;
    String URL   = "";
    String WERKS = "";
    String USER  = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_werks;
    EditText txt_store;
    EditText txt_bin;
    TextView txt_result_type;
    TextView txt_result_message;

    // Guard against rapid double-trigger from scanner echo
    private boolean inFlight = false;

    public FragmentHubHUPicking() {}

    public static FragmentHubHUPicking newInstance() {
        return new FragmentHubHUPicking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("HUB HU Picking");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_hub_hu_picking, container, false);
        con = getContext();
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        txt_werks          = rootView.findViewById(R.id.txt_hub_picking_werks);
        txt_store          = rootView.findViewById(R.id.txt_hub_picking_store);
        txt_bin            = rootView.findViewById(R.id.txt_hub_picking_bin);
        txt_result_type    = rootView.findViewById(R.id.txt_hub_picking_result_type);
        txt_result_message = rootView.findViewById(R.id.txt_hub_picking_result_message);

        txt_werks.setText(WERKS);

        addInputEvents();
        resetResultPanel();

        // Initial cursor on Store (Plant is auto-filled)
        txt_store.requestFocus();

        return rootView;
    }

    // ─── Input events ────────────────────────────────────────────────────────

    private void addInputEvents() {
        // STORE: scanner paste or Enter/Next advances to BIN
        txt_store.addTextChangedListener(new TextWatcher() {
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
                        @Override public void run() {
                            txt_bin.requestFocus();
                        }
                    }, 150);
                }
            }
        });

        txt_store.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String storeVal = UIFuncs.toUpperTrim(txt_store);
                    if (storeVal.isEmpty()) {
                        showError("Required", "Please scan or enter Store");
                        txt_store.requestFocus();
                        return true;
                    }
                    txt_bin.requestFocus();
                    return true;
                }
                return false;
            }
        });

        // BIN: scanner paste or Enter/Done triggers submit
        txt_bin.addTextChangedListener(new TextWatcher() {
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
                        @Override public void run() { submitPicking(); }
                    }, 200);
                }
            }
        });

        txt_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    UIFuncs.hideKeyboard(getActivity());
                    submitPicking();
                    return true;
                }
                return false;
            }
        });
    }

    // ─── Submit ───────────────────────────────────────────────────────────────

    private void submitPicking() {
        String store = UIFuncs.toUpperTrim(txt_store);
        String bin   = UIFuncs.toUpperTrim(txt_bin);

        if (WERKS == null || WERKS.isEmpty()) {
            showError("Config Error",
                    "Plant (WERKS) not set on device. Please log in again.");
            return;
        }
        if (store.isEmpty()) {
            showError("Required", "Please scan or enter Store");
            txt_store.requestFocus();
            return;
        }
        if (bin.isEmpty()) {
            showError("Required", "Please scan or enter BIN");
            txt_bin.requestFocus();
            return;
        }

        if (inFlight) {
            Log.d(TAG, "submitPicking ignored — request already in flight");
            return;
        }
        inFlight = true;

        Log.d(TAG, "submitPicking → WERKS=" + WERKS
                + " STORE=" + store + " BIN=" + bin);

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_HUB_HU_PICKING_RFC);
            args.put("IV_WERKS", WERKS);
            args.put("IV_STORE", store);
            args.put("IV_BIN",   bin);
        } catch (JSONException e) {
            inFlight = false;
            e.printStackTrace();
            box.getErrBox(e);
            return;
        }

        showProcessingAndSubmit(Vars.ZWM_HUB_HU_PICKING_RFC, args);
    }

    // ─── Network ──────────────────────────────────────────────────────────────

    private void showProcessingAndSubmit(final String rfc, final JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Processing HU Picking...");
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
                    clearAllInputs();
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
                            // Null response treated as error — clear everything
                            UIFuncs.errorSound(con);
                            new AlertBox(getContext()).getBox("Error", "No response from server");
                            clearAllInputs();
                        } else {
                            // handleResponse now owns the post-response field clearing,
                            // so onResponse stays lean.
                            handleResponse(response);
                        }
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
                        // Network-level error → clear everything
                        clearAllInputs();
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

    /**
     * Processes the RFC response and drives post-response UI state.
     *
     * Outcomes:
     *   • TYPE=E, missing ES_RETURN, or JSON parse failure → treated as error;
     *     calls {@link #clearAllInputs()} to wipe every scannable field.
     *   • TYPE=S / W / I / other → treated as success; calls
     *     {@link #clearForNextScan()} to clear BIN only and retain Store.
     */
    private void handleResponse(JSONObject response) {
        boolean isError = false;

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
                    isError = true;
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
                    // Unknown TYPE (W / I / empty) — neutral, treated as success
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.black));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.black));
                }
            } else {
                // Missing ES_RETURN → treat as error so user restarts the flow
                isError = true;
                txt_result_type.setText("");
                txt_result_message.setText("No return data from SAP");
                txt_result_message.setTextColor(
                        getResources().getColor(android.R.color.darker_gray));
            }
        } catch (JSONException e) {
            isError = true;
            e.printStackTrace();
            box.getErrBox(e);
        }

        if (isError) {
            clearAllInputs();
        } else {
            clearForNextScan();
        }
    }

    // ─── Post-response clearing helpers ──────────────────────────────────────

    /**
     * Error recovery — wipe every scannable field. Plant stays
     * (it's auto-filled from login). Focus returns to Store.
     */
    private void clearAllInputs() {
        txt_store.setText("");
        txt_bin.setText("");
        txt_store.requestFocus();
    }

    /**
     * Success path — clear BIN only; retain Store so consecutive picks
     * to the same destination don't require re-scanning.
     */
    private void clearForNextScan() {
        txt_bin.setText("");
        txt_bin.requestFocus();
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
