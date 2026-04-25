package com.v2retail.dotvik.hub.inward;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
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
 * FragmentHubHUPutaway — HUB HU Putaway (batch mode)
 *
 * RFC: ZWM_HUB_HU_PUTWAY_RFC (re-used from HUB HU Putway)
 *   IV_WERKS     — Plant (auto-filled, read-only)
 *   IV_LGTYP     — Storage Type, hard-default "V01" (read-only)
 *   IV_BIN       — Destination BIN, scanned once, locks after success
 *   IV_CRATE_HU  — Crate / HU being put away — repeats per scan
 *   ES_RETURN    — BAPIRET2 {TYPE, MESSAGE}
 *
 * Flow:
 *   1. Plant + Storage Type pre-filled. Cursor on BIN.
 *   2. User scans BIN → BIN locks, cursor jumps to Crate HU.
 *   3. User scans Crate HU → RFC fired with all 4 params.
 *      • SUCCESS (TYPE != "E") → QT++, clear Crate HU, refocus Crate HU.
 *      • ERROR  (TYPE == "E"  or network) → red message, clear Crate HU,
 *        QT NOT incremented. BIN stays locked so the user can retry the
 *        same destination.
 *   4. User repeats step 3 until done.
 *   5. Reset button → wipes BIN + Crate HU + QT, unlocks BIN, focus BIN.
 *   6. Back button → pops fragment.
 */
public class FragmentHubHUPutaway extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentHubHUPutaway";

    /** Default value for Storage Type field — visible to user, sent as IV_LGTYP. */
    private static final String DEFAULT_LGTYP = "V01";

    View rootView;
    String URL   = "";
    String WERKS = "";
    String USER  = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_werks;
    EditText txt_lgtyp;
    EditText txt_bin;
    EditText txt_crate_hu;
    EditText txt_qt;
    TextView txt_result_type;
    TextView txt_result_message;
    Button   btn_reset;
    Button   btn_back;

    /** Locked BIN value once scanned. Empty until first BIN scan succeeds. */
    private String activeBin = "";

    /** Running counter — number of successful Crate HU putaway calls. */
    private int qty = 0;

    /** Re-entrancy guard against scanner-echo double-trigger. */
    private boolean inFlight = false;

    public FragmentHubHUPutaway() {}

    public static FragmentHubHUPutaway newInstance() {
        return new FragmentHubHUPutaway();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("HUB HU Putaway");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_hub_hu_putaway, container, false);
        con = getContext();
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        txt_werks          = rootView.findViewById(R.id.txt_hub_putaway_werks);
        txt_lgtyp          = rootView.findViewById(R.id.txt_hub_putaway_lgtyp);
        txt_bin            = rootView.findViewById(R.id.txt_hub_putaway_bin);
        txt_crate_hu       = rootView.findViewById(R.id.txt_hub_putaway_crate_hu);
        txt_qt             = rootView.findViewById(R.id.txt_hub_putaway_qt);
        txt_result_type    = rootView.findViewById(R.id.txt_hub_putaway_result_type);
        txt_result_message = rootView.findViewById(R.id.txt_hub_putaway_result_message);
        btn_reset          = rootView.findViewById(R.id.btn_hub_putaway_reset);
        btn_back           = rootView.findViewById(R.id.btn_hub_putaway_back);

        txt_werks.setText(WERKS);
        txt_lgtyp.setText(DEFAULT_LGTYP);  // already in XML, set again to be defensive

        btn_reset.setOnClickListener(this);
        btn_back.setOnClickListener(this);

        addInputEvents();
        resetState();          // sets initial UI: BIN enabled, Crate HU disabled, QT=0

        return rootView;
    }

    // ─── Input events ────────────────────────────────────────────────────────

    private void addInputEvents() {
        // BIN: scanner paste OR Enter/Done — lock it and advance to Crate HU
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
                        @Override public void run() { acceptBinScan(); }
                    }, 150);
                }
            }
        });

        txt_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    UIFuncs.hideKeyboard(getActivity());
                    acceptBinScan();
                    return true;
                }
                return false;
            }
        });

        // CRATE HU: scanner paste OR Enter/Done — submit RFC for that HU
        txt_crate_hu.addTextChangedListener(new TextWatcher() {
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
                        @Override public void run() { submitCrateHu(); }
                    }, 200);
                }
            }
        });

        txt_crate_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    UIFuncs.hideKeyboard(getActivity());
                    submitCrateHu();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_hub_putaway_reset) {
            resetState();
        } else if (id == R.id.btn_hub_putaway_back) {
            UIFuncs.hideKeyboard(getActivity());
            if (fm != null) {
                fm.popBackStack();
            }
        }
    }

    // ─── BIN handling ────────────────────────────────────────────────────────

    /** Validate BIN locally, lock the field, and advance focus to Crate HU. */
    private void acceptBinScan() {
        String bin = UIFuncs.toUpperTrim(txt_bin);
        if (bin.isEmpty()) {
            showError("Required", "Please scan or enter BIN");
            txt_bin.requestFocus();
            return;
        }
        // Normalise case in the field, lock it, store value
        txt_bin.setText(bin);
        activeBin = bin;
        lockBinField();

        // Open Crate HU for repeating scans
        UIFuncs.enableInput(con, txt_crate_hu);
        txt_crate_hu.requestFocus();
        Log.d(TAG, "BIN locked → " + activeBin + "; ready for Crate HU scans");
    }

    private void lockBinField() {
        txt_bin.setFocusable(false);
        txt_bin.setClickable(false);
        txt_bin.setCursorVisible(false);
        txt_bin.setBackgroundColor(Color.parseColor("#EEEEEE"));
        txt_bin.setTextColor(Color.parseColor("#555555"));
    }

    private void unlockBinField() {
        txt_bin.setFocusableInTouchMode(true);
        txt_bin.setFocusable(true);
        txt_bin.setClickable(true);
        txt_bin.setCursorVisible(true);
        txt_bin.setBackgroundResource(android.R.drawable.edit_text);
        txt_bin.setTextColor(Color.BLACK);
    }

    // ─── Submit Crate HU ─────────────────────────────────────────────────────

    private void submitCrateHu() {
        String crate = UIFuncs.toUpperTrim(txt_crate_hu);

        if (WERKS == null || WERKS.isEmpty()) {
            showError("Config Error",
                    "Plant (WERKS) not set on device. Please log in again.");
            return;
        }
        if (activeBin.isEmpty()) {
            showError("Required", "Please scan BIN first");
            txt_bin.requestFocus();
            return;
        }
        if (crate.isEmpty()) {
            showError("Required", "Please scan or enter Crate HU");
            txt_crate_hu.requestFocus();
            return;
        }

        if (inFlight) {
            Log.d(TAG, "submitCrateHu ignored — request already in flight");
            return;
        }
        inFlight = true;

        Log.d(TAG, "submitCrateHu → WERKS=" + WERKS
                + " LGTYP=" + DEFAULT_LGTYP
                + " BIN=" + activeBin
                + " CRATE_HU=" + crate);

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname",    Vars.ZWM_HUB_HU_PUTWAY_RFC);
            args.put("IV_WERKS",    WERKS);
            args.put("IV_CRATE_HU", crate);
            args.put("IV_BIN",      activeBin);
            args.put("IV_LGTYP",    DEFAULT_LGTYP);
        } catch (JSONException e) {
            inFlight = false;
            e.printStackTrace();
            box.getErrBox(e);
            return;
        }

        showProcessingAndSubmit(Vars.ZWM_HUB_HU_PUTWAY_RFC, args);
    }

    // ─── Network ─────────────────────────────────────────────────────────────

    private void showProcessingAndSubmit(final String rfc, final JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Processing HU Putaway...");
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
                    clearCrateForRetry();
                }
            }
        }, 400);
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
                            new AlertBox(getContext()).getBox("Error", "No response from server");
                            clearCrateForRetry();
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
                        // Network failure → don't increment QT; user retries same Crate HU
                        clearCrateForRetry();
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

    // ─── Response handling ───────────────────────────────────────────────────

    /**
     * Apply the SAP response. Increments QT only on TYPE != "E".
     * BIN stays locked in either case — user keeps batching to same destination.
     * On error, only Crate HU is cleared so user can retry or scan a different HU.
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
                    // Unknown TYPE (W / I / blank) — neutral, treated as success
                    txt_result_type.setTextColor(
                            getResources().getColor(android.R.color.black));
                    txt_result_message.setTextColor(
                            getResources().getColor(android.R.color.black));
                }
            } else {
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

        if (!isError) {
            qty++;
            txt_qt.setText(String.valueOf(qty));
            Log.d(TAG, "Putaway success — QT now " + qty);
        }
        clearCrateForRetry();
    }

    // ─── State helpers ───────────────────────────────────────────────────────

    /** Clears just Crate HU (used after every submit, success or fail). */
    private void clearCrateForRetry() {
        txt_crate_hu.setText("");
        txt_crate_hu.requestFocus();
    }

    /**
     * Reset button + initial state.
     * Wipes BIN + Crate HU + QT, re-enables BIN field, refocuses BIN.
     * Plant + Storage Type stay (they're constant per session).
     */
    private void resetState() {
        activeBin = "";
        qty = 0;

        txt_bin.setText("");
        txt_crate_hu.setText("");
        txt_qt.setText("0");

        unlockBinField();
        UIFuncs.disableInput(con, txt_crate_hu);

        // Hide previous result panel
        txt_result_type.setText("");
        txt_result_message.setText("");
        txt_result_type.setVisibility(View.GONE);
        txt_result_message.setVisibility(View.GONE);

        txt_bin.requestFocus();
        Log.d(TAG, "State reset — BIN unlocked, QT=0");
    }

    // ─── Generic helpers ─────────────────────────────────────────────────────

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
