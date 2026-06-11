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
import com.v2retail.commons.SapJsonObjectRequest;
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
 *   3. Auto Save             -> ZWM_HU_MVT_SAVE_RFC      (commit movement)
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

    private EditText etDcSite;
    private EditText etScanBin;
    private EditText etBinDisplay;
    private EditText etScanHu;
    private EditText etHuDisplay;
    private EditText etTotalScanned;
    private TextView tvStatus;
    private Button btnBack;

    private String URL    = "";
    private String USER   = "";
    private String WERKS  = "";

    private boolean binValidated = false;
    private String  validatedBin = "";
    private int     totalScanned = 0;

    public FragmentRdcToRdcHuPutway() {}

    public static FragmentRdcToRdcHuPutway newInstance() {
        return new FragmentRdcToRdcHuPutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_rdc_to_rdc_hu_putway, container, false);

        etDcSite       = view.findViewById(R.id.rdc_et_dc_site);
        etScanBin      = view.findViewById(R.id.rdc_et_bin);
        etBinDisplay   = view.findViewById(R.id.rdc_et_bin_display);
        etScanHu       = view.findViewById(R.id.rdc_et_hu);
        etHuDisplay    = view.findViewById(R.id.rdc_et_hu_display);
        etTotalScanned = view.findViewById(R.id.rdc_et_total_scanned);
        tvStatus       = view.findViewById(R.id.rdc_tv_status);
        btnBack        = view.findViewById(R.id.rdc_btn_back);

        btnBack.setOnClickListener(this);

        etScanBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String bin = etScanBin.getText().toString().trim();
                if (!bin.isEmpty()) validateBin(bin);
                return true;
            }
        });

        etScanHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!binValidated) {
                    showError("Please scan and validate BIN first.");
                    return true;
                }
                String hu = etScanHu.getText().toString().trim();
                if (!hu.isEmpty()) validateHu(hu);
                return true;
            }
        });

        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("RDC TO RDC HU PUTWAY");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL   = prefs.read("URL");
        USER  = prefs.read("USER");
        WERKS = prefs.read("WERKS");

        etDcSite.setText(WERKS);
        etScanHu.setEnabled(false);
        etTotalScanned.setText("0");
        etScanBin.requestFocus();
    }

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

        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    dismissProgress();
                    if (response == null || response.length() == 0) {
                        showError("RFC not available on server. Contact SAP team.");
                        return;
                    }
                    JSONObject ret = response.optJSONObject("EX_RETURN");
                    if (ret == null) {
                        showError("RFC not available on server. Contact SAP team.");
                        return;
                    }
                    String type = ret.optString("TYPE", "");
                    String msg  = ret.optString("MESSAGE", "");
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        binValidated = true;
                        validatedBin = bin;
                        etBinDisplay.setText(bin);
                        etScanBin.setText("");
                        etScanBin.setEnabled(false);
                        etScanHu.setEnabled(true);
                        etScanHu.requestFocus();
                        showStatus("BIN OK: " + bin + " - Now scan HU.", true);
                    } else {
                        showError("BIN Error: " + msg);
                        etScanBin.setText("");
                        etScanBin.requestFocus();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dismissProgress();
                    showError("Network error: " + parseVolleyError(error));
                }
            });

        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        RequestQueue q = ApplicationController.getInstance().getRequestQueue();
        q.add(req);
    }

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

        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    dismissProgress();
                    if (response == null || response.length() == 0) {
                        showError("RFC not available on server. Contact SAP team.");
                        return;
                    }
                    JSONObject ret = response.optJSONObject("EX_RETURN");
                    if (ret == null) {
                        showError("RFC not available on server. Contact SAP team.");
                        return;
                    }
                    String type = ret.optString("TYPE", "");
                    String msg  = ret.optString("MESSAGE", "");
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        etHuDisplay.setText(hu);
                        etScanHu.setText("");
                        showStatus("HU OK: " + hu + " - Saving...", true);
                        saveMovement(hu);
                    } else {
                        showError("HU Error: " + msg);
                        etScanHu.setText("");
                        etScanHu.requestFocus();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dismissProgress();
                    showError("Network error: " + parseVolleyError(error));
                }
            });

        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void saveMovement(final String hu) {
        final String bin = validatedBin;

        if (TextUtils.isEmpty(bin) || TextUtils.isEmpty(hu)) {
            showError("BIN and HU are required.");
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

        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, params,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    dismissProgress();
                    if (response == null || response.length() == 0) {
                        showError("RFC not available on server. Contact SAP team.");
                        return;
                    }
                    JSONObject ret = response.optJSONObject("EX_RETURN");
                    if (ret == null) {
                        showError("RFC not available on server. Contact SAP team.");
                        return;
                    }
                    String type  = ret.optString("TYPE", "");
                    String msg   = ret.optString("MESSAGE", "Done");
                    String tanum = response.optString("EX_TANUM", "");

                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        totalScanned++;
                        etTotalScanned.setText(String.valueOf(totalScanned));

                        String successMsg = "Saved! HU " + hu + " -> BIN " + bin;
                        if (!tanum.isEmpty()) successMsg += " | TO: " + tanum;
                        showStatus(successMsg, true);

                        if (bin.startsWith("001")) {
                            etHuDisplay.setText("");
                            etScanHu.setEnabled(true);
                            etScanHu.requestFocus();
                        } else {
                            resetFields();
                        }
                    } else {
                        showError("Save Error: " + msg);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dismissProgress();
                    showError("Network error: " + parseVolleyError(error));
                }
            });

        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

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
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
    }

    private void showError(String msg) {
        showStatus(msg, false);
        if (box != null) box.getBox("Error", msg);
    }

    private void resetFields() {
        binValidated = false;
        validatedBin = "";
        etScanBin.setText("");
        etBinDisplay.setText("");
        etScanHu.setText("");
        etHuDisplay.setText("");
        etScanBin.setEnabled(true);
        etScanHu.setEnabled(false);
        etScanBin.requestFocus();
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
        if (v.getId() == R.id.rdc_btn_back) {
            if (getFragmentManager() != null) getFragmentManager().popBackStack();
        }
    }
}
