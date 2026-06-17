package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Inbound Process New — VND Box Put to BIN
 * Flow: Scan HU (client-side) → Scan BIN →
 *       {@link Vars#ZVND_PUTWAY_BIN_VAL_RFC} →
 *       {@link Vars#ZVND_PUTWAY_PALETTE_VAL_RFC} (IM_PALL = scanned HU) →
 *       {@link Vars#ZVND_PUTWAY_SAVE_DATA_RFC}
 */
public class FragmentVndBoxPutBin extends Fragment {

    private static final String DEFAULT_USER = "250";

    private Activity activity;
    private ProgressDialog dialog;

    private EditText etPlant, etScanHu, etHuNumber, etScanBin, etBin;
    private TextView tvStatus;

    private String URL = "";
    private String USER = "";
    private String werks = "";
    private String huNumber = "";
    private String poNo = "";
    private String billNo = "";
    private boolean requestInProgress = false;

    public FragmentVndBoxPutBin() {}

    public static FragmentVndBoxPutBin newInstance() {
        return new FragmentVndBoxPutBin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vnd_box_put_bin, container, false);

        etPlant    = view.findViewById(R.id.et_plant);
        etScanHu   = view.findViewById(R.id.et_scan_hu);
        etHuNumber = view.findViewById(R.id.et_hu_number);
        etScanBin  = view.findViewById(R.id.et_scan_bin);
        etBin      = view.findViewById(R.id.et_bin);
        tvStatus   = view.findViewById(R.id.tv_status);

        etScanHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                String hu = etScanHu.getText().toString().trim();
                if (!hu.isEmpty()) {
                    onHuScanned(hu);
                }
                return true;
            }
        });

        etScanBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                String bin = etScanBin.getText().toString().trim();
                if (!bin.isEmpty()) {
                    onBinScanned(bin);
                }
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
                    .setActionBarTitle("VND BOX PUT TO BIN");
        }
    }

    private void init() {
        activity = getActivity();
        if (activity == null) {
            return;
        }

        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        werks = prefs.read("WERKS");
        if (USER == null || USER.trim().isEmpty()) {
            USER = DEFAULT_USER;
        }
        if (werks == null) {
            werks = "";
        }

        etPlant.setText(werks);
        disableScanBin();
        clearAllAndRefocusHu(false);
        showStatus("Scan HU Barcode.", true);
    }

    /** Step A — client-side HU capture (no API). */
    private void onHuScanned(String scannedHu) {
        if (werks.trim().isEmpty()) {
            showStatus("Enter plant first.", false);
            return;
        }

        String hu = scannedHu.trim();
        if (hu.isEmpty()) {
            return;
        }

        huNumber = hu;
        poNo = "";
        billNo = "";
        etHuNumber.setText(hu);
        etBin.setText("");
        etScanBin.setText("");
        etScanHu.setText("");
        enableScanBin();
        etScanBin.requestFocus();
        showStatus("Scan destination BIN.", true);
    }

    /** Step B — BIN scan triggers validate + save chain. */
    private void onBinScanned(final String scannedBin) {
        if (requestInProgress) {
            return;
        }

        final String bin = scannedBin.trim();
        if (werks.trim().isEmpty() || huNumber.trim().isEmpty() || bin.isEmpty()) {
            showStatus("Plant, HU Number, and Bin are required. Scan HU first.", false);
            return;
        }

        String validateUrl = GatewayUrls.apiUrl(URL, "/api/" + Vars.ZVND_PUTWAY_BIN_VAL_RFC);
        if (validateUrl.isEmpty()) {
            showStatus("Server URL missing. Please log in again.", false);
            return;
        }

        requestInProgress = true;
        etScanBin.setEnabled(false);
        showProgress("Validating BIN...");

        StringRequest req = new StringRequest(Request.Method.POST, validateUrl,
            new Response.Listener<String>() {
                @Override public void onResponse(String body) {
                    try {
                        JSONObject r = new JSONObject(body != null ? body : "{}");
                        if (!isSuccess(r)) {
                            failAfterBinSubmit(r.optString("Message",
                                    exReturnMessage(r, "BIN validation failed")));
                            return;
                        }
                        validateHuAgainstBin(bin, r.optString("Message", ""));
                    } catch (JSONException e) {
                        failAfterBinSubmit("Parse error while validating BIN.");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    failAfterBinSubmit("Network error. Please retry.");
                }
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("IM_USER", USER);
                p.put("IM_PLANT", werks);
                p.put("IM_BIN", bin);
                return p;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void validateHuAgainstBin(final String bin, final String binValidateMessage) {
        showProgress("Validating HU...");
        String validateUrl = GatewayUrls.apiUrl(URL, "/api/" + Vars.ZVND_PUTWAY_PALETTE_VAL_RFC);
        if (validateUrl.isEmpty()) {
            failAfterBinSubmit("Server URL missing. Please log in again.");
            return;
        }

        StringRequest req = new StringRequest(Request.Method.POST, validateUrl,
            new Response.Listener<String>() {
                @Override public void onResponse(String body) {
                    try {
                        JSONObject r = new JSONObject(body != null ? body : "{}");
                        if (!isSuccess(r)) {
                            failAfterBinSubmit(r.optString("Message",
                                    exReturnMessage(r, "HU validation failed")));
                            return;
                        }

                        JSONArray et = r.optJSONArray("ET_DATA");
                        if (et == null) {
                            JSONObject data = r.optJSONObject("Data");
                            if (data != null) {
                                et = data.optJSONArray("ET_DATA");
                            }
                        }
                        if (et != null && et.length() > 0) {
                            JSONObject row = et.getJSONObject(0);
                            poNo = row.optString("PO_NO", "");
                            billNo = row.optString("BILL_NO", "");
                        }

                        savePutaway(bin, r.optString("Message", binValidateMessage));
                    } catch (JSONException e) {
                        failAfterBinSubmit("Parse error while validating HU.");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    failAfterBinSubmit("Network error. Please retry.");
                }
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("IM_USER", USER);
                p.put("IM_PLANT", werks);
                p.put("IM_BIN", bin);
                p.put("IM_PALL", huNumber);
                return p;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void savePutaway(final String bin, final String priorMessage) {
        showProgress("Saving...");
        String saveUrl = GatewayUrls.apiUrl(URL, "/api/" + Vars.ZVND_PUTWAY_SAVE_DATA_RFC);
        if (saveUrl.isEmpty()) {
            failAfterBinSubmit("Server URL missing. Please log in again.");
            return;
        }

        JSONObject body;
        try {
            body = new JSONObject();
            body.put("IM_USER", USER);
            JSONObject row = new JSONObject();
            row.put("PLANT", werks);
            row.put("BIN", bin);
            row.put("PALETTE", huNumber);
            row.put("PO_NO", poNo);
            row.put("BILL_NO", billNo);
            JSONArray it = new JSONArray();
            it.put(row);
            body.put("IT_DATA", it);
        } catch (JSONException e) {
            failAfterBinSubmit("Could not build save request.");
            return;
        }

        final JSONObject payload = body;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, saveUrl, payload,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) {
                    dismissProgress();
                    requestInProgress = false;
                    if (!isSuccess(r)) {
                        failAfterBinSubmit(r.optString("Message",
                                exReturnMessage(r, "Putaway to bin failed.")));
                        return;
                    }
                    String msg = r.optString("Message", priorMessage);
                    if (msg == null || msg.trim().isEmpty()) {
                        msg = "HU putaway to bin completed.";
                    }
                    etBin.setText(bin);
                    clearHuAndBinScanFields();
                    showStatus(msg, true);
                    etScanHu.requestFocus();
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    failAfterBinSubmit("Network error. Please retry.");
                }
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void failAfterBinSubmit(String message) {
        dismissProgress();
        requestInProgress = false;
        showStatus(message, false);
        clearAllAndRefocusHu(true);
    }

    private void clearHuAndBinScanFields() {
        huNumber = "";
        poNo = "";
        billNo = "";
        etHuNumber.setText("");
        etScanHu.setText("");
        etScanBin.setText("");
        disableScanBin();
    }

    private void clearAllAndRefocusHu(boolean showScanHuStatus) {
        huNumber = "";
        poNo = "";
        billNo = "";
        etHuNumber.setText("");
        etBin.setText("");
        etScanHu.setText("");
        etScanBin.setText("");
        disableScanBin();
        etScanHu.requestFocus();
        if (showScanHuStatus) {
            showStatus("Scan HU Barcode.", true);
        }
    }

    private void enableScanBin() {
        etScanBin.setEnabled(true);
        etScanBin.setBackgroundResource(R.drawable.border);
    }

    private void disableScanBin() {
        etScanBin.setEnabled(false);
        etScanBin.setBackgroundResource(R.drawable.border_disabled_input);
    }

    private static boolean isSuccess(JSONObject r) {
        Object status = r.opt("Status");
        if (status instanceof Boolean && (Boolean) status) {
            return true;
        }
        if (status != null && "S".equalsIgnoreCase(status.toString())) {
            return true;
        }
        if (status instanceof Number && ((Number) status).intValue() == 1) {
            return true;
        }
        JSONObject ret = r.optJSONObject("EX_RETURN");
        if (ret != null) {
            String type = ret.optString("TYPE", "");
            return "S".equalsIgnoreCase(type) || type.isEmpty();
        }
        return false;
    }

    private static String exReturnMessage(JSONObject r, String fallback) {
        JSONObject ret = r.optJSONObject("EX_RETURN");
        if (ret != null) {
            String msg = ret.optString("MESSAGE", "");
            if (!msg.trim().isEmpty()) {
                return msg.trim();
            }
        }
        return fallback;
    }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus == null) {
            return;
        }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
    }

    private void showProgress(String msg) {
        if (activity == null) {
            return;
        }
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);
        }
        dialog.setMessage(msg);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
