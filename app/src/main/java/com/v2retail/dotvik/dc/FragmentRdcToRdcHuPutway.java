package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * RDC TO RDC HU PUTWAY
 *
 * Flow:
 *   1. Scan Destination BIN  -> ZWM_HU_MVT_BIN_VAL_RFC  (validate bin)
 *   2. Scan HU               -> ZWM_HU_MVT_HU_VAL_RFC   (validate HU against bin)
 *   3. Save                  -> ZWM_HU_MVT_SAVE_RFC      (commit movement)
 *
 * BIN logic:
 *   - If BIN starts with "001", do NOT clear fields after save (consolidation mode).
 *   - Otherwise clear BIN + HU fields after each successful save.
 *
 * @version 12.107
 */
public class FragmentRdcToRdcHuPutway extends Fragment implements View.OnClickListener {

    private static final String TAG = "RdcToRdcHuPutway";

    private static final String RFC_BIN_VAL  = "ZWM_HU_MVT_BIN_VAL_RFC";
    private static final String RFC_HU_VAL   = "ZWM_HU_MVT_HU_VAL_RFC";
    private static final String RFC_SAVE     = "ZWM_HU_MVT_SAVE_RFC";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etBin;
    private EditText etHu;
    private TextView tvStatus;
    private Button   btnSave;
    private Button   btnReset;
    private Button   btnBack;

    private String URL    = "";
    private String USER   = "";
    private String WERKS  = "";

    // State flags
    private boolean binValidated = false;
    private String  validatedBin = "";

    public FragmentRdcToRdcHuPutway() {}

    public static FragmentRdcToRdcHuPutway newInstance() {
        return new FragmentRdcToRdcHuPutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_rdc_to_rdc_hu_putway, container, false);

        etBin    = view.findViewById(R.id.rdc_et_bin);
        etHu     = view.findViewById(R.id.rdc_et_hu);
        tvStatus = view.findViewById(R.id.rdc_tv_status);
        btnSave  = view.findViewById(R.id.rdc_btn_save);
        btnReset = view.findViewById(R.id.rdc_btn_reset);
        btnBack  = view.findViewById(R.id.rdc_btn_back);

        btnSave.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        // BIN scan triggers validation on IME action (scan gun sends Enter)
        etBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String bin = etBin.getText().toString().trim();
                if (!bin.isEmpty()) validateBin(bin);
                return true;
            }
        });

        // HU scan triggers HU validation
        etHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!binValidated) {
                    showStatus("Please scan and validate BIN first.", false);
                    return true;
                }
                String hu = etHu.getText().toString().trim();
                if (!hu.isEmpty()) validateHu(hu);
                return true;
            }
        });

        init();
        return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL   = prefs.read("URL");
        USER  = prefs.read("USER");
        WERKS = prefs.read("WERKS");

        etHu.setEnabled(false);
        btnSave.setEnabled(false);
        showStatus("Scan Destination BIN to begin.", true);
    }

    // ── BIN Validation ────────────────────────────────────────────────────────

    private void validateBin(final String bin) {
        showProgress("Validating BIN...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                callBinValRfc(bin);
            }
        }, 300);
    }

    private void callBinValRfc(final String bin) {
        String rfcUrl = buildRfcUrl(RFC_BIN_VAL);
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname",  RFC_BIN_VAL);
            params.put("IM_USER",   USER);
            params.put("IM_PLANT",  WERKS);
            params.put("IM_BIN",    bin);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, rfcUrl, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    dismissProgress();
                    if (response == null || response.length() == 0) {
                        showStatus("RFC not available on server. Contact SAP team.", false);
                        return;
                    }
                    JSONObject ret = response.optJSONObject("EX_RETURN");
                    if (ret == null) {
                        showStatus("RFC not available on server. Contact SAP team.", false);
                        return;
                    }
                    String type = ret.optString("TYPE", "");
                    String msg  = ret.optString("MESSAGE", "");
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        binValidated = true;
                        validatedBin = bin;
                        etBin.setEnabled(false);
                        etHu.setEnabled(true);
                        etHu.requestFocus();
                        showStatus("BIN OK: " + bin + " - Now scan HU.", true);
                    } else {
                        showStatus("BIN Error: " + msg, false);
                        etBin.setText("");
                        etBin.requestFocus();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dismissProgress();
                    showStatus("Network error: " + parseVolleyError(error), false);
                }
            });

        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        RequestQueue q = ApplicationController.getInstance().getRequestQueue();
        q.add(req);
    }

    // ── HU Validation ─────────────────────────────────────────────────────────

    private void validateHu(final String hu) {
        showProgress("Validating HU...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                callHuValRfc(hu);
            }
        }, 300);
    }

    private void callHuValRfc(final String hu) {
        String rfcUrl = buildRfcUrl(RFC_HU_VAL);
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname",  RFC_HU_VAL);
            params.put("IM_USER",   USER);
            params.put("IM_PLANT",  WERKS);
            params.put("IM_BIN",    validatedBin);
            params.put("IM_HU",     hu);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, rfcUrl, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    dismissProgress();
                    if (response == null || response.length() == 0) {
                        showStatus("RFC not available on server. Contact SAP team.", false);
                        return;
                    }
                    JSONObject ret = response.optJSONObject("EX_RETURN");
                    if (ret == null) {
                        showStatus("RFC not available on server. Contact SAP team.", false);
                        return;
                    }
                    String type = ret.optString("TYPE", "");
                    String msg  = ret.optString("MESSAGE", "");
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        btnSave.setEnabled(true);
                        showStatus("HU OK: " + hu + " - Ready to Save.", true);
                    } else {
                        showStatus("HU Error: " + msg, false);
                        etHu.setText("");
                        etHu.requestFocus();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dismissProgress();
                    showStatus("Network error: " + parseVolleyError(error), false);
                }
            });

        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveMovement() {
        final String hu  = etHu.getText().toString().trim();
        final String bin = validatedBin;

        if (TextUtils.isEmpty(bin) || TextUtils.isEmpty(hu)) {
            showStatus("BIN and HU are required.", false);
            return;
        }

        showProgress("Saving movement...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                callSaveRfc(bin, hu);
            }
        }, 300);
    }

    private void callSaveRfc(final String bin, final String hu) {
        String rfcUrl = buildRfcUrl(RFC_SAVE);
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname",  RFC_SAVE);
            params.put("IM_USER",   USER);
            params.put("IM_PLANT",  WERKS);
            params.put("IM_BIN",    bin);
            params.put("IM_HU",     hu);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, rfcUrl, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    dismissProgress();
                    if (response == null || response.length() == 0) {
                        showStatus("RFC not available on server. Contact SAP team.", false);
                        return;
                    }
                    JSONObject ret = response.optJSONObject("EX_RETURN");
                    if (ret == null) {
                        showStatus("RFC not available on server. Contact SAP team.", false);
                        return;
                    }
                    String type  = ret.optString("TYPE", "");
                    String msg   = ret.optString("MESSAGE", "Done");
                    String tanum = response.optString("EX_TANUM", "");

                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        String successMsg = "Saved! HU " + hu + " -> BIN " + bin;
                        if (!tanum.isEmpty()) successMsg += " | TO: " + tanum;
                        showStatus(successMsg, true);

                        // Clear logic: keep BIN if it starts with "001", else clear all
                        if (bin.startsWith("001")) {
                            // Consolidation bin - stay on same bin, clear only HU
                            etHu.setText("");
                            etHu.setEnabled(true);
                            etHu.requestFocus();
                            btnSave.setEnabled(false);
                        } else {
                            // Normal bin - clear everything for next scan
                            resetFields();
                        }
                    } else {
                        showStatus("Save Error: " + msg, false);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dismissProgress();
                    showStatus("Network error: " + parseVolleyError(error), false);
                }
            });

        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildRfcUrl(String rfcName) {
        String base = URL;
        if (base.contains("/ValueXMW")) {
            base = base.replace("/ValueXMW", "");
        }
        return base + "/noacljsonrfcadaptor?bapiname=" + rfcName + "&aclclientid=android";
    }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus == null) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok
            ? 0xFFE8F5E9   // green tint
            : 0xFFFFEBEE); // red tint
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
    }

    private void resetFields() {
        binValidated = false;
        validatedBin = "";
        etBin.setText("");
        etHu.setText("");
        etBin.setEnabled(true);
        etHu.setEnabled(false);
        btnSave.setEnabled(false);
        etBin.requestFocus();
        showStatus("Scan Destination BIN to begin.", true);
    }

    private void showProgress(String msg) {
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);
        }
        dialog.setMessage(msg);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private String parseVolleyError(VolleyError e) {
        if (e instanceof TimeoutError)     return "Request timed out";
        if (e instanceof NoConnectionError) return "No network connection";
        if (e instanceof NetworkError)     return "Network error";
        if (e instanceof ServerError) {
            if (e.networkResponse != null && e.networkResponse.data != null) {
                return "Server error: " + new String(e.networkResponse.data).substring(0,
                    Math.min(100, e.networkResponse.data.length));
            }
            return "Server error";
        }
        if (e instanceof ParseError)       return "Response parse error";
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rdc_btn_save) {
            saveMovement();
        } else if (id == R.id.rdc_btn_reset) {
            resetFields();
        } else if (id == R.id.rdc_btn_back) {
            if (getFragmentManager() != null) getFragmentManager().popBackStack();
        }
    }
}
