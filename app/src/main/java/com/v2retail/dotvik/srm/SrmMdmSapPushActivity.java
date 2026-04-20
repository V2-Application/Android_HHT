package com.v2retail.dotvik.srm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONObject;
import java.util.*;

/**
 * MDM SAP Push Screen — dual checklist + SAP field mapping + one-button push to SAP
 */
public class SrmMdmSapPushActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private String appId, ticketNo;
    private TextView tvVendorName, tvTicket, tvPan, tvGstin, tvBank, tvIfsc,
                     tvCompCode, tvType, tvSapStatus;
    private CheckBox cbAccBank, cbAccGst, cbAccPan;
    private CheckBox cbMdmBank, cbMdmGst, cbMdmPan, cbMdmDuplicate;
    private Spinner  spAccountGroup, spReconAccount, spPaymentTerms;
    private EditText etWhtCode, etPurchasingOrg, etToleranceGroup;
    private Button   btnPush, btnSaveChecklist;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_mdm_sap_push);

        prefs    = new SharedPreferencesData(getApplicationContext());
        appId    = getIntent().getStringExtra("app_id");
        ticketNo = getIntent().getStringExtra("ticket_no");

        tvVendorName   = findViewById(R.id.srm_tv_vendor_name);
        tvTicket       = findViewById(R.id.srm_tv_ticket);
        tvPan          = findViewById(R.id.srm_tv_pan);
        tvGstin        = findViewById(R.id.srm_tv_gstin);
        tvBank         = findViewById(R.id.srm_tv_bank);
        tvIfsc         = findViewById(R.id.srm_tv_ifsc);
        tvCompCode     = findViewById(R.id.srm_tv_comp_code);
        tvType         = findViewById(R.id.srm_tv_type);
        tvSapStatus    = findViewById(R.id.srm_tv_sap_status);

        cbAccBank      = findViewById(R.id.srm_cb_acc_bank);
        cbAccGst       = findViewById(R.id.srm_cb_acc_gst);
        cbAccPan       = findViewById(R.id.srm_cb_acc_pan);

        cbMdmBank      = findViewById(R.id.srm_cb_mdm_bank);
        cbMdmGst       = findViewById(R.id.srm_cb_mdm_gst);
        cbMdmPan       = findViewById(R.id.srm_cb_mdm_pan);
        cbMdmDuplicate = findViewById(R.id.srm_cb_mdm_duplicate);

        spAccountGroup  = findViewById(R.id.srm_sp_account_group);
        spReconAccount  = findViewById(R.id.srm_sp_recon_account);
        spPaymentTerms  = findViewById(R.id.srm_sp_payment_terms);
        etWhtCode       = findViewById(R.id.srm_et_wht_code);
        etPurchasingOrg = findViewById(R.id.srm_et_purchasing_org);
        etToleranceGroup= findViewById(R.id.srm_et_tolerance_group);

        btnPush         = findViewById(R.id.srm_btn_push_sap);
        btnSaveChecklist= findViewById(R.id.srm_btn_save_checklist);

        // Spinners
        ArrayAdapter<String> agAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"ZVEN – Standard Vendor", "ZSRV – Service Vendor", "ZIMP – Import Vendor"});
        agAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAccountGroup.setAdapter(agAdapter);

        ArrayAdapter<String> raAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"160000 – Domestic Vendors", "160010 – Import Vendors", "160020 – Service Vendors"});
        raAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReconAccount.setAdapter(raAdapter);

        ArrayAdapter<String> ptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"Z030 – Net 30 Days", "Z045 – Net 45 Days", "Z060 – Net 60 Days", "Z000 – Immediate"});
        ptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentTerms.setAdapter(ptAdapter);

        // Enable push button only when all checks are done
        CheckBox[] all = { cbAccBank, cbAccGst, cbAccPan, cbMdmBank, cbMdmGst, cbMdmPan, cbMdmDuplicate };
        for (CheckBox cb : all) {
            cb.setOnCheckedChangeListener((v, c) -> btnPush.setEnabled(allChecked()));
        }
        btnPush.setEnabled(false);

        btnSaveChecklist.setOnClickListener(v -> saveChecklist());
        btnPush.setOnClickListener(v -> confirmPush());

        loadApplication();
        loadChecklist();
        loadSapStatus();
    }

    private boolean allChecked() {
        return cbAccBank.isChecked() && cbAccGst.isChecked() && cbAccPan.isChecked() &&
               cbMdmBank.isChecked() && cbMdmGst.isChecked() && cbMdmPan.isChecked() &&
               cbMdmDuplicate.isChecked();
    }

    private void loadApplication() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + "/api/applications/" + appId, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    tvVendorName.setText(d.optString("vendor_name", "—"));
                    tvTicket.setText(d.optString("ticket_no", "—"));
                    tvPan.setText(d.optString("pan_no", "—"));
                    tvGstin.setText(d.optString("gstin", "—"));
                    tvBank.setText(d.optString("bank_name", "—") + " | " + d.optString("bank_account_no", ""));
                    tvIfsc.setText(d.optString("ifsc_code", "—"));
                    tvCompCode.setText(d.optString("company_code", "—"));
                    tvType.setText(d.optString("vendor_type", "—"));
                    etPurchasingOrg.setText("V2RL");
                } catch (Exception e) { e.printStackTrace(); }
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void loadChecklist() {
        String url = getSrmBaseUrl() + String.format(SrmVars.MDM_CHECKLIST_GET, appId);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    cbAccBank.setChecked(d.optBoolean("acc_bank_verified", false));
                    cbAccGst.setChecked(d.optBoolean("acc_gst_verified", false));
                    cbAccPan.setChecked(d.optBoolean("acc_pan_verified", false));
                    cbMdmBank.setChecked(d.optBoolean("mdm_bank_verified", false));
                    cbMdmGst.setChecked(d.optBoolean("mdm_gst_verified", false));
                    cbMdmPan.setChecked(d.optBoolean("mdm_pan_verified", false));
                    cbMdmDuplicate.setChecked(d.optBoolean("mdm_duplicate_checked", false));
                    if (!d.optString("sap_wht_code", "").isEmpty())
                        etWhtCode.setText(d.optString("sap_wht_code"));
                    if (!d.optString("sap_tolerance_group", "").isEmpty())
                        etToleranceGroup.setText(d.optString("sap_tolerance_group", "V001"));
                    btnPush.setEnabled(allChecked());
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void loadSapStatus() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + SrmVars.MDM_SAP_STATUS, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    boolean rfc = d.optBoolean("rfcAvailable", false);
                    tvSapStatus.setText(rfc ? "SAP RFC: Connected" : "SAP RFC: REST Fallback Active");
                    tvSapStatus.setTextColor(rfc ? 0xFF059669 : 0xFFF59E0B);
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void saveChecklist() {
        try {
            JSONObject body = new JSONObject();
            body.put("acc_bank_verified",   cbAccBank.isChecked());
            body.put("acc_gst_verified",    cbAccGst.isChecked());
            body.put("acc_pan_verified",    cbAccPan.isChecked());
            body.put("mdm_bank_verified",   cbMdmBank.isChecked());
            body.put("mdm_gst_verified",    cbMdmGst.isChecked());
            body.put("mdm_pan_verified",    cbMdmPan.isChecked());
            body.put("mdm_duplicate_checked", cbMdmDuplicate.isChecked());
            body.put("sap_account_group",   spAccountGroup.getSelectedItemPosition() == 0 ? "ZVEN" : spAccountGroup.getSelectedItemPosition() == 1 ? "ZSRV" : "ZIMP");
            body.put("sap_recon_account",   spReconAccount.getSelectedItemPosition() == 0 ? "160000" : spReconAccount.getSelectedItemPosition() == 1 ? "160010" : "160020");
            body.put("sap_payment_terms",   spPaymentTerms.getSelectedItemPosition() == 0 ? "Z030" : spPaymentTerms.getSelectedItemPosition() == 1 ? "Z045" : spPaymentTerms.getSelectedItemPosition() == 2 ? "Z060" : "Z000");
            body.put("sap_wht_code",         etWhtCode.getText().toString().trim());
            body.put("sap_purchasing_org",   etPurchasingOrg.getText().toString().trim());
            body.put("sap_tolerance_group",  etToleranceGroup.getText().toString().trim());

            String url = getSrmBaseUrl() + String.format(SrmVars.MDM_CHECKLIST_PUT, appId);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.PUT, url, body,
                response -> Toast.makeText(this, "Checklist saved", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
            ) {
                @Override public Map<String,String> getHeaders() { return authHeader(); }
                @Override public String getBodyContentType() { return "application/json"; }
                @Override public byte[] getBody() { return body.toString().getBytes(); }
            };
            ApplicationController.getInstance().getRequestQueue().add(req);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void confirmPush() {
        new AlertDialog.Builder(this)
            .setTitle("Push to SAP")
            .setMessage("Create vendor in SAP? A permanent Vendor Code will be generated. This cannot be undone.")
            .setPositiveButton("Push to SAP", (d, w) -> doPush())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void doPush() {
        progress = new ProgressDialog(this);
        progress.setMessage("Creating vendor in SAP…");
        progress.setCancelable(false);
        progress.show();

        String url = getSrmBaseUrl() + String.format(SrmVars.MDM_PUSH_SAP, appId);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(),
            response -> {
                dismiss();
                try {
                    String sapCode = response.getJSONObject("data").getString("sapVendorCode");
                    new AlertDialog.Builder(this)
                        .setTitle("Vendor Created in SAP!")
                        .setMessage("SAP Vendor Code: " + sapCode + "\n\nThe vendor has been successfully created and the ticket is closed.")
                        .setPositiveButton("Done", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                } catch (Exception e) { Toast.makeText(this, "Push succeeded", Toast.LENGTH_LONG).show(); finish(); }
            },
            error -> {
                dismiss();
                String msg = "SAP push failed";
                if (error.networkResponse != null) {
                    try { msg = new JSONObject(new String(error.networkResponse.data)).optString("message", msg); }
                    catch (Exception ignored) {}
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        ) {
            @Override public Map<String,String> getHeaders() { return authHeader(); }
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody() { return "{}".getBytes(); }
        };
        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(60000, 0,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void dismiss() {
        if (progress != null && progress.isShowing()) { progress.dismiss(); progress = null; }
    }

    private Map<String,String> authHeader() {
        Map<String,String> h = new HashMap<>();
        h.put("Authorization", "Bearer " + prefs.read(SrmVars.PREF_SRM_TOKEN));
        return h;
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
