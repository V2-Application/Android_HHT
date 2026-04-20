package com.v2retail.dotvik.srm;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONObject;

public class SrmMdmSapPushActivity extends AppCompatActivity {

    private String appId;

    // Vendor info
    private TextView tvTicket, tvVendorName, tvPan, tvGstin, tvBank,
                     tvIfsc, tvCompany, tvDivision;

    // Accounts team checklist
    private CheckBox cbAccBank, cbAccGst, cbAccPan;

    // MDM team checklist
    private CheckBox cbMdmBank, cbMdmGst, cbMdmPan, cbMdmDup;

    // SAP mapping
    private Spinner spAccountGroup, spReconAccount, spPaymentTerms;
    private EditText etWhtCode, etPurchasingOrg;

    // Actions
    private Button btnSave, btnPush;
    private ProgressBar progress;
    private TextView tvPushHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_mdm_sap_push);

        appId = getIntent().getStringExtra("app_id");

        tvTicket    = findViewById(R.id.tvSapTicket);
        tvVendorName= findViewById(R.id.tvSapVendorName);
        tvPan       = findViewById(R.id.tvSapPan);
        tvGstin     = findViewById(R.id.tvSapGstin);
        tvBank      = findViewById(R.id.tvSapBank);
        tvIfsc      = findViewById(R.id.tvSapIfsc);
        tvCompany   = findViewById(R.id.tvSapCompany);
        tvDivision  = findViewById(R.id.tvSapDivision);

        cbAccBank = findViewById(R.id.cbSapAccBank);
        cbAccGst  = findViewById(R.id.cbSapAccGst);
        cbAccPan  = findViewById(R.id.cbSapAccPan);
        cbMdmBank = findViewById(R.id.cbSapMdmBank);
        cbMdmGst  = findViewById(R.id.cbSapMdmGst);
        cbMdmPan  = findViewById(R.id.cbSapMdmPan);
        cbMdmDup  = findViewById(R.id.cbSapMdmDup);

        spAccountGroup  = findViewById(R.id.spSapAccountGroup);
        spReconAccount  = findViewById(R.id.spSapReconAccount);
        spPaymentTerms  = findViewById(R.id.spSapPaymentTerms);
        etWhtCode       = findViewById(R.id.etSapWhtCode);
        etPurchasingOrg = findViewById(R.id.etSapPurchasingOrg);

        btnSave  = findViewById(R.id.btnSapSave);
        btnPush  = findViewById(R.id.btnSapPush);
        progress = findViewById(R.id.progressSapPush);
        tvPushHint= findViewById(R.id.tvSapPushHint);

        setupSpinners();
        loadApplication();
        loadChecklist();

        // Watch all checkboxes to update push button
        View.OnClickListener checkWatcher = v -> updatePushButton();
        cbAccBank.setOnClickListener(checkWatcher); cbAccGst.setOnClickListener(checkWatcher);
        cbAccPan.setOnClickListener(checkWatcher);  cbMdmBank.setOnClickListener(checkWatcher);
        cbMdmGst.setOnClickListener(checkWatcher);  cbMdmPan.setOnClickListener(checkWatcher);
        cbMdmDup.setOnClickListener(checkWatcher);

        btnSave.setOnClickListener(v -> saveChecklist());
        btnPush.setOnClickListener(v -> confirmPush());
        findViewById(R.id.btnSapBack).setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        setupSpinner(spAccountGroup, new String[]{"ZVEN – Standard Vendor","ZSRV – Service Vendor","ZIMP – Import Vendor"});
        setupSpinner(spReconAccount, new String[]{"160000 – Domestic Vendors","160010 – Import Vendors","160020 – Service Vendors"});
        setupSpinner(spPaymentTerms, new String[]{"Z030 – Net 30 Days","Z045 – Net 45 Days","Z060 – Net 60 Days","Z000 – Immediate"});
        etPurchasingOrg.setText("V2RL");
    }

    private void setupSpinner(Spinner sp, String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
    }

    private void loadApplication() {
        SrmApiClient.get(this, "/applications/" + appId, res -> {
            try {
                JSONObject d = res.getJSONObject("data");
                tvTicket.setText(d.optString("ticket_no", "—"));
                tvVendorName.setText(d.optString("vendor_name", "—"));
                tvPan.setText(d.optString("pan_no", "—"));
                tvGstin.setText(d.optString("gstin", "—"));
                tvBank.setText(d.optString("bank_name","—") + " — " + d.optString("bank_account_no",""));
                tvIfsc.setText(d.optString("ifsc_code","—"));
                tvCompany.setText(d.optString("company_code","—"));
                tvDivision.setText(d.optString("division","—"));
            } catch (Exception ignored) {}
        }, e -> Toast.makeText(this, SrmApiClient.parseError(e), Toast.LENGTH_LONG).show());
    }

    private void loadChecklist() {
        SrmApiClient.get(this, "/mdm/checklist/" + appId, res -> {
            try {
                JSONObject d = res.getJSONObject("data");
                cbAccBank.setChecked(d.optBoolean("acc_bank_verified"));
                cbAccGst.setChecked(d.optBoolean("acc_gst_verified"));
                cbAccPan.setChecked(d.optBoolean("acc_pan_verified"));
                cbMdmBank.setChecked(d.optBoolean("mdm_bank_verified"));
                cbMdmGst.setChecked(d.optBoolean("mdm_gst_verified"));
                cbMdmPan.setChecked(d.optBoolean("mdm_pan_verified"));
                cbMdmDup.setChecked(d.optBoolean("mdm_duplicate_checked"));
                updatePushButton();
            } catch (Exception ignored) {}
        }, e -> {});
    }

    private void updatePushButton() {
        boolean allDone = cbAccBank.isChecked() && cbAccGst.isChecked() && cbAccPan.isChecked()
                && cbMdmBank.isChecked() && cbMdmGst.isChecked()
                && cbMdmPan.isChecked() && cbMdmDup.isChecked();
        btnPush.setEnabled(allDone);
        btnPush.setAlpha(allDone ? 1f : 0.4f);
        tvPushHint.setText(allDone
                ? "✅ All checks complete — ready to push"
                : "Complete all checklist items to enable SAP push");
        tvPushHint.setTextColor(allDone ? 0xFF388E3C : 0xFF9E9E9E);
    }

    private void saveChecklist() {
        progress.setVisibility(View.VISIBLE);
        try {
            JSONObject body = new JSONObject();
            body.put("acc_bank_verified",   cbAccBank.isChecked());
            body.put("acc_gst_verified",    cbAccGst.isChecked());
            body.put("acc_pan_verified",    cbAccPan.isChecked());
            body.put("mdm_bank_verified",   cbMdmBank.isChecked());
            body.put("mdm_gst_verified",    cbMdmGst.isChecked());
            body.put("mdm_pan_verified",    cbMdmPan.isChecked());
            body.put("mdm_duplicate_checked", cbMdmDup.isChecked());
            body.put("sap_purchasing_org",  etPurchasingOrg.getText().toString().trim());
            body.put("sap_wht_code",        etWhtCode.getText().toString().trim());

            SrmApiClient.patch(this, "/mdm/checklist/" + appId, body, res -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Checklist saved", Toast.LENGTH_SHORT).show();
            }, e -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Save failed: " + SrmApiClient.parseError(e), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) { progress.setVisibility(View.GONE); }
    }

    private void confirmPush() {
        new AlertDialog.Builder(this, R.style.SrmAlertDialog)
                .setTitle("Confirm SAP Push")
                .setMessage("You are about to create this vendor in SAP ERP.\n\nA permanent Vendor Code will be generated. This cannot be undone.")
                .setPositiveButton("Push to SAP", (d, w) -> doPush())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doPush() {
        progress.setVisibility(View.VISIBLE);
        btnPush.setEnabled(false);
        SrmApiClient.post(this, "/mdm/push-sap/" + appId, new JSONObject(), res -> {
            progress.setVisibility(View.GONE);
            try {
                String sapCode = res.getJSONObject("data").optString("sapVendorCode", "—");
                showSuccess(sapCode);
            } catch (Exception e) { showSuccess("—"); }
        }, e -> {
            progress.setVisibility(View.GONE);
            btnPush.setEnabled(true);
            Toast.makeText(this, "SAP Push failed: " + SrmApiClient.parseError(e), Toast.LENGTH_LONG).show();
        });
    }

    private void showSuccess(String sapCode) {
        new AlertDialog.Builder(this, R.style.SrmAlertDialog)
                .setTitle("🎉 Vendor Created in SAP!")
                .setMessage("SAP Vendor Code: " + sapCode + "\n\nThe vendor has been successfully registered. Ticket closed.")
                .setPositiveButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}
