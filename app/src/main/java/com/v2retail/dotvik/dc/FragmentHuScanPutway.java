package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
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
 * Unloading — HU Scanning & Putway to Palette
 * Flow: Scan HU → ZVND_UNLOAD_HU_VALIDATE_RFC (shows PO/Inv/Vendor)
 *       Scan Palette → ZVND_UNLOAD_PALLATE_VALIDATION
 *       Save → ZVND_UNLOAD_SAVE_RFC
 * @version 12.106
 */
public class FragmentHuScanPutway extends Fragment implements View.OnClickListener {

    private static final String TAG          = "HuScanPutway";
    private static final String RFC_HU_VAL   = "ZVND_UNLOAD_HU_VALIDATE_RFC";
    private static final String RFC_PALL_VAL = "ZVND_UNLOAD_PALLATE_VALIDATION";
    private static final String RFC_SAVE     = "ZVND_UNLOAD_SAVE_RFC";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etVehicle, etHu, etPalette;
    private TextView tvDc, tvPo, tvInv, tvVendor, tvStatus;
    private Button btnSave, btnReset, btnBack;

    private String URL = "", USER = "", WERKS = "";
    private boolean huValidated = false;
    private String validatedHu = "", poNo = "", billNo = "";

    public FragmentHuScanPutway() {}
    public static FragmentHuScanPutway newInstance() { return new FragmentHuScanPutway(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_hu_scan_putway, container, false);
        tvDc     = view.findViewById(R.id.tv_dc);
        etVehicle= view.findViewById(R.id.et_vehicle);
        etHu     = view.findViewById(R.id.et_hu);
        etPalette= view.findViewById(R.id.et_palette);
        tvPo     = view.findViewById(R.id.tv_po);
        tvInv    = view.findViewById(R.id.tv_inv);
        tvVendor = view.findViewById(R.id.tv_vendor);
        tvStatus = view.findViewById(R.id.tv_status);
        btnSave  = view.findViewById(R.id.btn_save);
        btnReset = view.findViewById(R.id.btn_reset);
        btnBack  = view.findViewById(R.id.btn_back);

        btnSave.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        etHu.setOnEditorActionListener((v, a, e) -> { String hu = etHu.getText().toString().trim(); if (!hu.isEmpty()) validateHu(hu); return true; });
        etPalette.setOnEditorActionListener((v, a, e) -> { if (!huValidated) { showStatus("Scan HU first.", false); return true; } String p = etPalette.getText().toString().trim(); if (!p.isEmpty()) validatePalette(p); return true; });

        init(); return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL"); USER = prefs.read("USER"); WERKS = prefs.read("WERKS");
        tvDc.setText("DC: " + WERKS);
        etPalette.setEnabled(false);
        btnSave.setEnabled(false);
        showStatus("Enter Vehicle No. and Scan HU.", true);
    }

    private void validateHu(String hu) {
        showProgress("Validating HU...");
        JSONObject p = new JSONObject();
        try { p.put("bapiname", RFC_HU_VAL); p.put("IM_USER", USER); p.put("IM_PLANT", WERKS); p.put("IM_HU", hu); }
        catch (JSONException e) { dismissProgress(); return; }
        callRfc(RFC_HU_VAL, p, response -> {
            try {
                if (isSuccess(response)) {
                    huValidated = true; validatedHu = hu;
                    // Parse ET_DATA for PO/Inv/Vendor
                    org.json.JSONArray et = response.optJSONArray("ET_DATA");
                    if (et != null && et.length() > 0) {
                        JSONObject row = et.getJSONObject(0);
                        poNo   = row.optString("PO_NO","");
                        billNo = row.optString("BILL_NO","");
                        String vendor = row.optString("VENDOR_NAME","");
                        tvPo.setText("PO: " + poNo); tvPo.setVisibility(View.VISIBLE);
                        tvInv.setText("INV: " + billNo); tvInv.setVisibility(View.VISIBLE);
                        tvVendor.setText("Vendor: " + vendor); tvVendor.setVisibility(View.VISIBLE);
                    }
                    etHu.setEnabled(false);
                    etPalette.setEnabled(true);
                    etPalette.requestFocus();
                    showStatus("HU OK: " + hu + " — Scan Palette.", true);
                } else { showStatus("HU Error: " + getMsg(response), false); etHu.setText(""); etHu.requestFocus(); }
            } catch (JSONException e) { showStatus("Parse error: " + e.getMessage(), false); }
        }, err -> showStatus("Network error: " + err, false));
    }

    private void validatePalette(String palette) {
        showProgress("Validating Palette...");
        JSONObject p = new JSONObject();
        try { p.put("bapiname", RFC_PALL_VAL); p.put("IM_USER", USER); p.put("IM_PLANT", WERKS); p.put("IM_HU", validatedHu); p.put("IM_PALL", palette); }
        catch (JSONException e) { dismissProgress(); return; }
        callRfc(RFC_PALL_VAL, p, response -> {
            try {
                if (isSuccess(response)) {
                    btnSave.setEnabled(true);
                    etPalette.setEnabled(false);
                    showStatus("Palette OK — Ready to Save.", true);
                } else { showStatus("Palette Error: " + getMsg(response), false); etPalette.setText(""); etPalette.requestFocus(); }
            } catch (JSONException e) { showStatus("Parse error: " + e.getMessage(), false); }
        }, err -> showStatus("Network: " + err, false));
    }

    private void save() {
        String vehicle = etVehicle.getText().toString().trim();
        String hu      = validatedHu;
        String palette = etPalette.getText().toString().trim();
        if (vehicle.isEmpty()) { showStatus("Enter Vehicle No.", false); return; }
        showProgress("Saving...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", RFC_SAVE); p.put("IM_USER", USER);
            JSONObject parms = new JSONObject();
            parms.put("PLANT", WERKS); parms.put("VEHICLE", vehicle);
            parms.put("EXT_HU", hu); parms.put("PALETTE", palette);
            parms.put("PO_NO", poNo); parms.put("BILL_NO", billNo);
            p.put("IM_PARMS", parms);
        } catch (JSONException e) { dismissProgress(); return; }
        callRfc(RFC_SAVE, p, response -> {
            try {
                if (isSuccess(response)) { showStatus("✔ Saved! HU " + hu + " → Palette " + palette, true); resetFields(); }
                else { showStatus("Save Error: " + getMsg(response), false); }
            } catch (JSONException e) { showStatus("Parse error", false); }
        }, err -> showStatus("Network: " + err, false));
    }

    private void resetFields() {
        huValidated = false; validatedHu = ""; poNo = ""; billNo = "";
        etVehicle.setText(""); etHu.setText(""); etPalette.setText("");
        etHu.setEnabled(true); etPalette.setEnabled(false); btnSave.setEnabled(false);
        tvPo.setVisibility(View.GONE); tvInv.setVisibility(View.GONE); tvVendor.setVisibility(View.GONE);
        etHu.requestFocus();
        showStatus("Enter Vehicle No. and Scan HU.", true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_save)  { save(); }
        else if (id == R.id.btn_reset) { resetFields(); }
        else if (id == R.id.btn_back) { if (getFragmentManager()!=null) getFragmentManager().popBackStack(); }
    }

    private void callRfc(String name, JSONObject params, RfcCallback cb) {
        String base = URL.contains("/ValueXMW") ? URL.replace("/ValueXMW","") : URL;
        String url = base + "/noacljsonrfcadaptor?bapiname=" + name + "&aclclientid=android";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, params,
            r -> { dismissProgress(); cb.onSuccess(r); },
            e -> { dismissProgress(); cb.onError(parseErr(e)); });
        req.setRetryPolicy(new DefaultRetryPolicy(90000,0,1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    interface RfcCallback { void onSuccess(JSONObject r); void onError(String e); }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus==null) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
    }
    private void showProgress(String msg) {
        if (dialog==null||!dialog.isShowing()) { dialog=new ProgressDialog(activity); dialog.setCancelable(false); }
        dialog.setMessage(msg); dialog.show();
    }
    private void dismissProgress() { if (dialog!=null&&dialog.isShowing()) dialog.dismiss(); }
    private boolean isSuccess(JSONObject r) { JSONObject ret=r.optJSONObject("EX_RETURN"); if(ret==null) return true; String t=ret.optString("TYPE",""); return "S".equalsIgnoreCase(t)||t.isEmpty(); }
    private String getMsg(JSONObject r) { JSONObject ret=r.optJSONObject("EX_RETURN"); return ret!=null?ret.optString("MESSAGE",""):""; }
    private String parseErr(VolleyError e) {
        if(e instanceof TimeoutError) return "Timeout";
        if(e instanceof NoConnectionError) return "No connection";
        if(e instanceof NetworkError) return "Network error";
        if(e instanceof ParseError) return "Parse error";
        return e.getMessage()!=null?e.getMessage():"Unknown";
    }
}
