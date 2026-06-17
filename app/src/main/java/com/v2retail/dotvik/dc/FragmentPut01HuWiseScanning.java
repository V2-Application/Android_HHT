package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PUT01 HU Wise Scanning (Inbound Process New)
 * Flow: Scan HU → {@code ZVND_PUT01_HU_VAL_RFC} → auto {@code ZVND_PUT01_SAVE_DATA_RFC}
 */
public class FragmentPut01HuWiseScanning extends Fragment implements View.OnClickListener {

    private static final String DEFAULT_USER = "250";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etDcSite, etHu, etHuNumber, etPallate, etPo, etInv, etBoxHuQty, etTotScannedHu;
    private TextView tvStatus;
    private Button btnReset, btnBack;

    private String URL = "", USER = "", WERKS = "";
    private int totScannedHu = 0;

    public FragmentPut01HuWiseScanning() {}
    public static FragmentPut01HuWiseScanning newInstance() { return new FragmentPut01HuWiseScanning(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_put01_hu_wise_scanning, container, false);

        etDcSite       = view.findViewById(R.id.et_dc_site);
        etHu           = view.findViewById(R.id.et_hu);
        etHuNumber     = view.findViewById(R.id.et_hu_number);
        etPallate      = view.findViewById(R.id.et_pallate);
        etPo           = view.findViewById(R.id.tv_po);
        etInv          = view.findViewById(R.id.tv_inv);
        etBoxHuQty     = view.findViewById(R.id.et_box_hu_qty);
        etTotScannedHu = view.findViewById(R.id.et_tot_scanned_hu);
        tvStatus       = view.findViewById(R.id.tv_status);
        btnReset       = view.findViewById(R.id.btn_reset);
        btnBack        = view.findViewById(R.id.btn_back);

        btnReset.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        etHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) {
                String hu = etHu.getText().toString().trim();
                if (!hu.isEmpty()) onHuScanned(hu);
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
                    .setActionBarTitle("PUT01- HU-WISE SCANNING");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        WERKS = prefs.read("WERKS");
        if (USER == null || USER.trim().isEmpty()) USER = DEFAULT_USER;
        etDcSite.setText(WERKS != null ? WERKS : "");
        clearDisplayFields();
        showStatus("Scan HU Barcode.", true);
    }

    private void clearDisplayFields() {
        etHuNumber.setText("");
        etPallate.setText("");
        etPo.setText("");
        etInv.setText("");
        etBoxHuQty.setText("0 / 0");
    }

    private void onHuScanned(final String scannedHu) {
        if (WERKS == null || WERKS.trim().isEmpty()) {
            showStatus("Plant (DC Site) not found. Please log in again.", false);
            return;
        }
        String validateUrl = GatewayUrls.apiUrl(URL, "/api/" + Vars.ZVND_PUT01_HU_VAL_RFC);
        if (validateUrl.isEmpty()) {
            showStatus("Server URL missing. Please log in again.", false);
            return;
        }

        showProgress("Validating HU...");
        etHu.setEnabled(false);

        StringRequest req = new StringRequest(Request.Method.POST, validateUrl,
            new Response.Listener<String>() {
                @Override public void onResponse(String body) {
                    dismissProgress();
                    try {
                        JSONObject r = new JSONObject(body != null ? body : "{}");
                        if (!isSapSuccess(r)) {
                            showStatus(r.optString("Message", "HU validation failed"), false);
                            clearScanInput();
                            return;
                        }
                        JSONObject row = firstEtDataRow(r);
                        final String exidv = nonEmpty(row != null ? row.optString("EXIDV", "") : "", scannedHu);
                        final String palette = row != null ? row.optString("PALETTE", "") : "";
                        final String poNo = row != null ? row.optString("VPONO", "") : "";
                        final String invNo = row != null ? row.optString("INVNO", "") : "";
                        final String qty = row != null ? row.optString("QTY", "0") : "0";
                        savePut01(exidv, palette, poNo, invNo, qty, r.optString("Message", ""));
                    } catch (JSONException e) {
                        showStatus("Parse error while validating HU.", false);
                        clearScanInput();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    dismissProgress();
                    showStatus("Network error while validating HU. Please retry.", false);
                    clearScanInput();
                }
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("IM_HU", scannedHu);
                p.put("IM_USER", USER);
                p.put("IM_PLANT", WERKS);
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

    private void savePut01(final String exidv, final String palette,
                           final String poNo, final String invNo, final String qty,
                           final String validateMessage) {
        String saveUrl = GatewayUrls.apiUrl(URL, "/api/" + Vars.ZVND_PUT01_SAVE_DATA_RFC);
        if (saveUrl.isEmpty()) {
            showStatus("Server URL missing. Please log in again.", false);
            clearScanInput();
            return;
        }

        showProgress("Saving...");
        JSONObject body;
        try {
            body = new JSONObject();
            body.put("IM_USER", USER);
            JSONObject row = new JSONObject();
            row.put("WERKS", WERKS);
            row.put("EXIDV", exidv);
            row.put("PALETTE", palette);
            row.put("ZPUT01_SCAN", "X");
            JSONArray it = new JSONArray();
            it.put(row);
            body.put("IT_DATA", it);
        } catch (JSONException e) {
            dismissProgress();
            showStatus("Could not build save request.", false);
            clearScanInput();
            return;
        }

        final JSONObject payload = body;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, saveUrl, payload,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) {
                    dismissProgress();
                    if (!isSapSuccess(r)) {
                        showStatus(r.optString("Message", "Could not save HU data."), false);
                        clearScanInput();
                        return;
                    }
                    updateDisplayFields(exidv, palette, poNo, invNo, qty);
                    totScannedHu++;
                    etTotScannedHu.setText(String.valueOf(totScannedHu));
                    String msg = r.optString("Message", validateMessage);
                    if (msg == null || msg.trim().isEmpty()) msg = "Data saved successfully.";
                    showStatus(msg, true);
                    clearScanInput();
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    dismissProgress();
                    showStatus("Could not save HU data.", false);
                    clearScanInput();
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

    private JSONObject firstEtDataRow(JSONObject response) throws JSONException {
        JSONObject data = response.optJSONObject("Data");
        if (data == null) data = response;
        JSONArray et = data.optJSONArray("ET_DATA");
        if (et != null && et.length() > 0) {
            return et.getJSONObject(0);
        }
        return null;
    }

    private void updateDisplayFields(String exidv, String palette, String poNo, String invNo, String qty) {
        etHuNumber.setText(exidv);
        etPallate.setText(nonEmpty(palette, "—"));
        etPo.setText(nonEmpty(poNo, "—"));
        etInv.setText(nonEmpty(invNo, "—"));
        etBoxHuQty.setText("0 / " + nonEmpty(qty, "0"));
    }

    private void clearScanInput() {
        etHu.setText("");
        etHu.setEnabled(true);
        etHu.requestFocus();
    }

    private void resetAll() {
        totScannedHu = 0;
        etTotScannedHu.setText("0");
        clearDisplayFields();
        clearScanInput();
        showStatus("Scan HU Barcode.", true);
    }

    private static boolean isSapSuccess(JSONObject r) {
        Object status = r.opt("Status");
        return status != null && "S".equals(status.toString().toUpperCase(Locale.ROOT));
    }

    private static String nonEmpty(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_reset) resetAll();
        else if (id == R.id.btn_back) {
            if (getFragmentManager() != null) getFragmentManager().popBackStack();
        }
    }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus == null) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
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
}
