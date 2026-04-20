package com.v2retail.dotvik.srm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.MultipartUploadRequest;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import com.v2retail.dotvik.srm.model.VendorApplication;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SrmVendorRegistrationActivity extends AppCompatActivity {

    // ── Step tracking ────────────────────────────────────────────────────
    private int currentStep = 0; // 0..4
    private static final String[] STEP_TITLES = {
            "Vendor Details", "Banking & Tax", "Buying Details", "Documents", "Review & Submit"
    };
    private static final int STEPS = 5;

    // ── Data model ───────────────────────────────────────────────────────
    private final VendorApplication app = new VendorApplication();
    private final Map<String, Uri>    docUris   = new HashMap<>();
    private final Map<String, String> docNames  = new HashMap<>();
    private String createdAppId;

    // ── Stepper views ────────────────────────────────────────────────────
    private TextView  tvStepTitle, tvStepCount;
    private ProgressBar stepProgress;
    private Button btnBack, btnNext;
    private ProgressBar progressSubmit;

    // ── STEP 0: Vendor details ────────────────────────────────────────────
    private LinearLayout layoutStep0;
    private Spinner spVendorType, spBusinessType, spVendorRelation, spCompanyCode, spDivision;
    private EditText etVendorName, etContactPerson, etAddress, etCity, etMobile, etEmail;
    private Spinner spState;

    // ── STEP 1: Banking ───────────────────────────────────────────────────
    private LinearLayout layoutStep1;
    private EditText etBankName, etBankAccount, etBranch, etIfsc, etPan, etGstin, etServiceTax, etWht;

    // ── STEP 2: Buying ────────────────────────────────────────────────────
    private LinearLayout layoutStep2;
    private Spinner spCategory;
    private EditText etPaymentTerms, etVrp, etArticle, etBuyingMonth, etPurchaseValue;
    private CheckBox cbDecl1, cbDecl2, cbDecl3, cbDecl4;

    // ── STEP 3: Documents ─────────────────────────────────────────────────
    private LinearLayout layoutStep3;
    private static final String[] DOC_TYPES   = {"PAN_CARD","CANCELLED_CHEQUE","GST_CERTIFICATE","BILL_COPY"};
    private static final String[] DOC_LABELS  = {"PAN Card *","Cancelled Cheque *","GST Certificate *","Bill Copy *"};
    private static final int[]    DOC_PICK_RC = {101, 102, 103, 104};
    private TextView[] tvDocStatus;

    // ── STEP 4: Review ────────────────────────────────────────────────────
    private LinearLayout layoutStep4;
    private TextView tvReviewName, tvReviewPan, tvReviewGstin, tvReviewBank, tvReviewMobile,
                     tvReviewDivision, tvReviewCompany, tvReviewDocs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_vendor_registration);

        // Stepper controls
        tvStepTitle   = findViewById(R.id.tvSrmRegStepTitle);
        tvStepCount   = findViewById(R.id.tvSrmRegStepCount);
        stepProgress  = findViewById(R.id.progressSrmRegStep);
        btnBack       = findViewById(R.id.btnSrmRegBack);
        btnNext       = findViewById(R.id.btnSrmRegNext);
        progressSubmit= findViewById(R.id.progressSrmRegSubmit);

        // Step containers
        layoutStep0 = findViewById(R.id.layoutSrmStep0);
        layoutStep1 = findViewById(R.id.layoutSrmStep1);
        layoutStep2 = findViewById(R.id.layoutSrmStep2);
        layoutStep3 = findViewById(R.id.layoutSrmStep3);
        layoutStep4 = findViewById(R.id.layoutSrmStep4);

        // Step 0
        spVendorType    = findViewById(R.id.spSrmVendorType);
        spBusinessType  = findViewById(R.id.spSrmBusinessType);
        spVendorRelation= findViewById(R.id.spSrmVendorRelation);
        spCompanyCode   = findViewById(R.id.spSrmCompanyCode);
        spDivision      = findViewById(R.id.spSrmDivision);
        spState         = findViewById(R.id.spSrmState);
        etVendorName    = findViewById(R.id.etSrmVendorName);
        etContactPerson = findViewById(R.id.etSrmContactPerson);
        etAddress       = findViewById(R.id.etSrmAddress);
        etCity          = findViewById(R.id.etSrmCity);
        etMobile        = findViewById(R.id.etSrmMobile);
        etEmail         = findViewById(R.id.etSrmEmail);

        // Step 1
        etBankName    = findViewById(R.id.etSrmBankName);
        etBankAccount = findViewById(R.id.etSrmBankAccount);
        etBranch      = findViewById(R.id.etSrmBranch);
        etIfsc        = findViewById(R.id.etSrmIfsc);
        etPan         = findViewById(R.id.etSrmPan);
        etGstin       = findViewById(R.id.etSrmGstin);
        etServiceTax  = findViewById(R.id.etSrmServiceTax);
        etWht         = findViewById(R.id.etSrmWht);

        // Step 2
        spCategory     = findViewById(R.id.spSrmCategory);
        etPaymentTerms = findViewById(R.id.etSrmPaymentTerms);
        etVrp          = findViewById(R.id.etSrmVrp);
        etArticle      = findViewById(R.id.etSrmArticle);
        etBuyingMonth  = findViewById(R.id.etSrmBuyingMonth);
        etPurchaseValue= findViewById(R.id.etSrmPurchaseValue);
        cbDecl1 = findViewById(R.id.cbSrmDecl1);
        cbDecl2 = findViewById(R.id.cbSrmDecl2);
        cbDecl3 = findViewById(R.id.cbSrmDecl3);
        cbDecl4 = findViewById(R.id.cbSrmDecl4);

        // Step 3 – document upload buttons
        tvDocStatus = new TextView[]{
                findViewById(R.id.tvSrmDoc0Status),
                findViewById(R.id.tvSrmDoc1Status),
                findViewById(R.id.tvSrmDoc2Status),
                findViewById(R.id.tvSrmDoc3Status),
        };
        Button[] btnDocs = new Button[]{
                findViewById(R.id.btnSrmDoc0), findViewById(R.id.btnSrmDoc1),
                findViewById(R.id.btnSrmDoc2), findViewById(R.id.btnSrmDoc3),
        };
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            btnDocs[i].setOnClickListener(v -> pickFile(idx));
        }

        // Step 4
        tvReviewName     = findViewById(R.id.tvSrmReviewName);
        tvReviewPan      = findViewById(R.id.tvSrmReviewPan);
        tvReviewGstin    = findViewById(R.id.tvSrmReviewGstin);
        tvReviewBank     = findViewById(R.id.tvSrmReviewBank);
        tvReviewMobile   = findViewById(R.id.tvSrmReviewMobile);
        tvReviewDivision = findViewById(R.id.tvSrmReviewDivision);
        tvReviewCompany  = findViewById(R.id.tvSrmReviewCompany);
        tvReviewDocs     = findViewById(R.id.tvSrmReviewDocs);

        btnBack.setOnClickListener(v -> goBack());
        btnNext.setOnClickListener(v -> goNext());

        populateSpinners();
        showStep(0);
    }

    private void populateSpinners() {
        setupSpinner(spVendorType,    new String[]{"TRADING","IMPORTED","SERVICE","NONTRADING"},
                                      new String[]{"Trading Goods","Imported","Service Provider","Non-Trading"});
        setupSpinner(spBusinessType,  new String[]{"TRADER","MANUFACTURER","IMPORTER"},
                                      new String[]{"Trader","Manufacturer","Importer"});
        setupSpinner(spVendorRelation,new String[]{"PV","OTV"},
                                      new String[]{"Permanent (PV)","One Time (OTV)"});
        setupSpinner(spCompanyCode,   new String[]{"1100","2000","3000","4000","5000","6000","7000","8000"},
                                      new String[]{"1100 – Store Level","2000 – VRL Foods","3000 – VRL E-Comm",
                                                   "4000 – DC/HO Level","5000 – Franchises","6000 – Logistics",
                                                   "7000 – Infrastructure","8000 – VRL IT"});
        setupSpinner(spDivision, null,
                new String[]{"Apparels","Non-Apparels","FMCG","Non-Trading","Services","Customer"});
        setupSpinner(spState, null,
                new String[]{"Andhra Pradesh","Delhi","Gujarat","Haryana","Karnataka","Kerala",
                             "Madhya Pradesh","Maharashtra","Punjab","Rajasthan","Tamil Nadu",
                             "Telangana","Uttar Pradesh","West Bengal","Other"});
        setupSpinner(spCategory, null,
                new String[]{"Apparels – Menswear","Apparels – Womenswear","Apparels – Kidswear",
                             "Footwear","Accessories","FMCG – Food","FMCG – Non-Food",
                             "Home Furnishing","Electronics","Services – Logistics","Services – IT","Other"});
    }

    private void setupSpinner(Spinner sp, String[] values, String[] labels) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        if (values != null) sp.setTag(values); // store value keys
    }

    private String getSpinnerValue(Spinner sp) {
        String[] vals = (String[]) sp.getTag();
        int idx = sp.getSelectedItemPosition();
        if (vals != null && idx < vals.length) return vals[idx];
        return (String) sp.getSelectedItem();
    }

    // ── Navigation ────────────────────────────────────────────────────────
    private void goNext() {
        if (!validateStep(currentStep)) return;
        collectStep(currentStep);
        if (currentStep == STEPS - 1) { submitAll(); return; }
        showStep(currentStep + 1);
    }

    private void goBack() {
        if (currentStep == 0) { finish(); return; }
        showStep(currentStep - 1);
    }

    private void showStep(int step) {
        currentStep = step;
        tvStepTitle.setText(STEP_TITLES[step]);
        tvStepCount.setText((step + 1) + " / " + STEPS);
        stepProgress.setProgress((step + 1) * 100 / STEPS);
        btnBack.setText(step == 0 ? "Cancel" : "Back");
        btnNext.setText(step == STEPS - 1 ? "Submit" : "Next →");

        layoutStep0.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        layoutStep4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);

        if (step == 4) populateReview();
    }

    // ── Validation ────────────────────────────────────────────────────────
    private boolean validateStep(int step) {
        switch (step) {
            case 0:
                if (TextUtils.isEmpty(etVendorName.getText())) { etVendorName.setError("Required"); return false; }
                if (TextUtils.isEmpty(etContactPerson.getText())) { etContactPerson.setError("Required"); return false; }
                if (TextUtils.isEmpty(etMobile.getText()) || etMobile.getText().length() != 10) { etMobile.setError("10-digit mobile required"); return false; }
                if (TextUtils.isEmpty(etCity.getText())) { etCity.setError("Required"); return false; }
                break;
            case 1:
                if (TextUtils.isEmpty(etBankName.getText())) { etBankName.setError("Required"); return false; }
                if (TextUtils.isEmpty(etBankAccount.getText())) { etBankAccount.setError("Required"); return false; }
                if (TextUtils.isEmpty(etIfsc.getText()) || etIfsc.getText().length() != 11) { etIfsc.setError("11-char IFSC required"); return false; }
                if (TextUtils.isEmpty(etPan.getText()) || etPan.getText().length() != 10) { etPan.setError("10-char PAN required"); return false; }
                if (TextUtils.isEmpty(etGstin.getText()) || etGstin.getText().length() != 15) { etGstin.setError("15-char GSTIN required"); return false; }
                break;
            case 2:
                if (TextUtils.isEmpty(etVrp.getText())) { etVrp.setError("Required"); return false; }
                if (TextUtils.isEmpty(etArticle.getText())) { etArticle.setError("Required"); return false; }
                if (TextUtils.isEmpty(etPaymentTerms.getText())) { etPaymentTerms.setError("Required"); return false; }
                break;
            case 3:
                for (int i = 0; i < 4; i++) {
                    if (!docUris.containsKey(DOC_TYPES[i])) {
                        Toast.makeText(this, DOC_LABELS[i] + " is required", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    // ── Collect field values ───────────────────────────────────────────────
    private void collectStep(int step) {
        switch (step) {
            case 0:
                app.vendorType     = getSpinnerValue(spVendorType);
                app.businessType   = getSpinnerValue(spBusinessType);
                app.vendorRelation = getSpinnerValue(spVendorRelation);
                app.companyCode    = getSpinnerValue(spCompanyCode);
                app.division       = (String) spDivision.getSelectedItem();
                app.vendorName     = etVendorName.getText().toString().trim();
                app.contactPerson  = etContactPerson.getText().toString().trim();
                app.vendorAddress  = etAddress.getText().toString().trim();
                app.city           = etCity.getText().toString().trim();
                app.state          = (String) spState.getSelectedItem();
                app.mobile         = etMobile.getText().toString().trim();
                app.email          = etEmail.getText().toString().trim();
                break;
            case 1:
                app.bankName      = etBankName.getText().toString().trim();
                app.bankAccountNo = etBankAccount.getText().toString().trim();
                app.bankBranch    = etBranch.getText().toString().trim();
                app.ifscCode      = etIfsc.getText().toString().trim().toUpperCase();
                app.panNo         = etPan.getText().toString().trim().toUpperCase();
                app.gstin         = etGstin.getText().toString().trim().toUpperCase();
                app.serviceTaxNo  = etServiceTax.getText().toString().trim();
                app.withholdingTaxCode = etWht.getText().toString().trim();
                break;
            case 2:
                app.merchandiseCategory = (String) spCategory.getSelectedItem();
                app.paymentTermsDays    = etPaymentTerms.getText().toString().trim();
                app.vrp                 = etVrp.getText().toString().trim();
                app.articleToPurchase   = etArticle.getText().toString().trim();
                app.buyingPerMonth      = etBuyingMonth.getText().toString().trim();
                app.purchaseValuePa     = etPurchaseValue.getText().toString().trim();
                break;
        }
    }

    private void populateReview() {
        tvReviewName.setText(app.vendorName);
        tvReviewPan.setText(app.panNo);
        tvReviewGstin.setText(app.gstin);
        tvReviewBank.setText(app.bankName + " — " + app.bankAccountNo);
        tvReviewMobile.setText(app.mobile);
        tvReviewDivision.setText(app.division);
        tvReviewCompany.setText(app.companyCode);
        StringBuilder docs = new StringBuilder();
        for (String dt : DOC_TYPES) {
            docs.append(docUris.containsKey(dt) ? "✔ " : "✗ ").append(dt.replace("_", " ")).append("\n");
        }
        tvReviewDocs.setText(docs.toString().trim());
    }

    // ── Submit ─────────────────────────────────────────────────────────────
    private void submitAll() {
        btnNext.setEnabled(false);
        btnBack.setEnabled(false);
        progressSubmit.setVisibility(View.VISIBLE);

        SrmApiClient.post(this, "/applications", app.toJson(), response -> {
            try {
                JSONObject data = response.getJSONObject("data");
                createdAppId = data.getString("id");
                String ticket = data.getString("ticket_no");
                uploadDocuments(ticket);
            } catch (Exception e) {
                onSubmitError("Failed to parse response");
            }
        }, error -> onSubmitError(SrmApiClient.parseError(error)));
    }

    private void uploadDocuments(String ticket) {
        final int[] remaining = {docUris.size()};
        for (Map.Entry<String, Uri> entry : docUris.entrySet()) {
            String docType = entry.getKey();
            Uri    uri     = entry.getValue();
            String name    = docNames.getOrDefault(docType, "document.pdf");

            MultipartUploadRequest req = new MultipartUploadRequest(this,
                    createdAppId, docType, uri, name,
                    r -> {
                        remaining[0]--;
                        if (remaining[0] == 0) onSubmitSuccess(ticket);
                    },
                    e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) onSubmitSuccess(ticket); // proceed even if optional doc fails
                    });
            SrmApiClient.getQueue(this).add(req);
        }
    }

    private void onSubmitSuccess(String ticket) {
        progressSubmit.setVisibility(View.GONE);
        Intent i = new Intent(this, SrmTrackApplicationActivity.class);
        i.putExtra("ticket_no", ticket);
        i.putExtra("submitted", true);
        startActivity(i);
        finish();
    }

    private void onSubmitError(String msg) {
        runOnUiThread(() -> {
            progressSubmit.setVisibility(View.GONE);
            btnNext.setEnabled(true);
            btnBack.setEnabled(true);
            Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    // ── File picker ────────────────────────────────────────────────────────
    private void pickFile(int idx) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf","image/jpeg","image/png"});
        startActivityForResult(Intent.createChooser(intent, "Select " + DOC_LABELS[idx]),
                DOC_PICK_RC[idx]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;
        for (int i = 0; i < 4; i++) {
            if (requestCode == DOC_PICK_RC[i]) {
                Uri uri = data.getData();
                String name = getFileName(uri);
                docUris.put(DOC_TYPES[i], uri);
                docNames.put(DOC_TYPES[i], name);
                tvDocStatus[i].setText("✔ " + name);
                tvDocStatus[i].setTextColor(0xFF2E7D32);
                break;
            }
        }
    }

    private String getFileName(Uri uri) {
        String name = "document";
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) name = c.getString(idx);
            c.close();
        }
        return name;
    }
}
