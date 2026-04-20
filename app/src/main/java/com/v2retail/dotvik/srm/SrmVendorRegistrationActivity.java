package com.v2retail.dotvik.srm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
 * Vendor Registration — 4-section form
 * Section A: Vendor Details  B: Banking & Tax
 * Section C: Buying Details  D: Submit
 *
 * Uses a ViewFlipper to navigate between steps.
 * On Step 4, sends POST /api/applications and returns to dashboard.
 */
public class SrmVendorRegistrationActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private ViewFlipper vf;
    private ProgressDialog progress;

    // Step 1 — Vendor Details
    private Spinner  spVendorType, spBusinessType, spVendorRelation, spCompanyCode, spDivision, spState;
    private EditText etVendorName, etAddress, etCity, etPostal, etContact, etMobile, etEmail;

    // Step 2 — Banking
    private EditText etBankName, etAccountNo, etBranch, etIfsc, etPan, etGstin, etServiceTax, etWht;

    // Step 3 — Buying
    private EditText etPaymentDays, etMerchandise, etVrp, etArticle, etBuying;

    // Buttons
    private Button btn1Next, btn2Back, btn2Next, btn3Back, btn3Next, btn4Back, btn4Submit;
    private TextView tvStep, tvResult;

    private static final String[] COMPANY_CODES = {"1100 – Store Level","2000 – VRL Foods","3000 – VRL E-Commerce","4000 – DC/HO Level","5000 – VRL Franchises","6000 – Logistics","7000 – VRL Infrastructure","8000 – VRL IT"};
    private static final String[] VENDOR_TYPES  = {"TRADING","IMPORTED","SERVICE","NONTRADING"};
    private static final String[] BIZ_TYPES     = {"TRADER","MANUFACTURER","IMPORTER"};
    private static final String[] RELATIONS     = {"PV – Permanent","OTV – One Time"};
    private static final String[] DIVISIONS     = {"Apparels","Non-Apparels","FMCG","Non-Trading","Services","Customer"};
    private static final String[] STATES        = {"Andhra Pradesh","Delhi","Gujarat","Haryana","Karnataka","Kerala","Maharashtra","Punjab","Rajasthan","Tamil Nadu","Telangana","Uttar Pradesh","West Bengal","Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_vendor_registration);
        prefs = new SharedPreferencesData(getApplicationContext());

        vf       = findViewById(R.id.srm_vf);
        tvStep   = findViewById(R.id.srm_tv_step);
        tvResult = findViewById(R.id.srm_tv_result);

        // Step 1
        spVendorType    = findViewById(R.id.srm_sp_vendor_type);
        spBusinessType  = findViewById(R.id.srm_sp_business_type);
        spVendorRelation= findViewById(R.id.srm_sp_vendor_relation);
        spCompanyCode   = findViewById(R.id.srm_sp_company_code);
        spDivision      = findViewById(R.id.srm_sp_division);
        spState         = findViewById(R.id.srm_sp_state);
        etVendorName    = findViewById(R.id.srm_et_vendor_name);
        etAddress       = findViewById(R.id.srm_et_address);
        etCity          = findViewById(R.id.srm_et_city);
        etPostal        = findViewById(R.id.srm_et_postal);
        etContact       = findViewById(R.id.srm_et_contact);
        etMobile        = findViewById(R.id.srm_et_mobile);
        etEmail         = findViewById(R.id.srm_et_email);
        btn1Next        = findViewById(R.id.srm_btn_step1_next);

        // Step 2
        etBankName  = findViewById(R.id.srm_et_bank_name);
        etAccountNo = findViewById(R.id.srm_et_account_no);
        etBranch    = findViewById(R.id.srm_et_branch);
        etIfsc      = findViewById(R.id.srm_et_ifsc);
        etPan       = findViewById(R.id.srm_et_pan);
        etGstin     = findViewById(R.id.srm_et_gstin);
        etServiceTax= findViewById(R.id.srm_et_service_tax);
        etWht       = findViewById(R.id.srm_et_wht);
        btn2Back    = findViewById(R.id.srm_btn_step2_back);
        btn2Next    = findViewById(R.id.srm_btn_step2_next);

        // Step 3
        etPaymentDays  = findViewById(R.id.srm_et_payment_days);
        etMerchandise  = findViewById(R.id.srm_et_merchandise);
        etVrp          = findViewById(R.id.srm_et_vrp);
        etArticle      = findViewById(R.id.srm_et_article);
        etBuying       = findViewById(R.id.srm_et_buying);
        btn3Back       = findViewById(R.id.srm_btn_step3_back);
        btn3Next       = findViewById(R.id.srm_btn_step3_next);

        // Step 4
        btn4Back   = findViewById(R.id.srm_btn_step4_back);
        btn4Submit = findViewById(R.id.srm_btn_step4_submit);

        populateSpinner(spVendorType,    VENDOR_TYPES);
        populateSpinner(spBusinessType,  BIZ_TYPES);
        populateSpinner(spVendorRelation,RELATIONS);
        populateSpinner(spCompanyCode,   COMPANY_CODES);
        populateSpinner(spDivision,      DIVISIONS);
        populateSpinner(spState,         STATES);

        if (btn1Next  != null) btn1Next.setOnClickListener(v -> { if (validateStep1()) goTo(1); });
        if (btn2Back  != null) btn2Back.setOnClickListener(v -> goTo(0));
        if (btn2Next  != null) btn2Next.setOnClickListener(v -> { if (validateStep2()) goTo(2); });
        if (btn3Back  != null) btn3Back.setOnClickListener(v -> goTo(1));
        if (btn3Next  != null) btn3Next.setOnClickListener(v -> goTo(3));
        if (btn4Back  != null) btn4Back.setOnClickListener(v -> goTo(2));
        if (btn4Submit!= null) btn4Submit.setOnClickListener(v -> submitApplication());

        updateStepLabel(0);
    }

    private void populateSpinner(Spinner sp, String[] items) {
        if (sp == null) return;
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
    }

    private void goTo(int idx) {
        if (vf != null) vf.setDisplayedChild(idx);
        updateStepLabel(idx);
    }

    private void updateStepLabel(int step) {
        if (tvStep == null) return;
        String[] labels = {"Step 1 of 4 — Vendor Details", "Step 2 of 4 — Banking & Tax",
                           "Step 3 of 4 — Buying Details", "Step 4 of 4 — Review & Submit"};
        tvStep.setText(labels[Math.min(step, 3)]);
    }

    private boolean validateStep1() {
        if (etVendorName == null || etVendorName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vendor Name is required", Toast.LENGTH_SHORT).show(); return false;
        }
        if (etMobile == null || etMobile.getText().toString().trim().length() != 10) {
            Toast.makeText(this, "Valid 10-digit mobile number required", Toast.LENGTH_SHORT).show(); return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (etPan == null || etPan.getText().toString().trim().length() != 10) {
            Toast.makeText(this, "Valid 10-character PAN required", Toast.LENGTH_SHORT).show(); return false;
        }
        if (etGstin == null || etGstin.getText().toString().trim().length() != 15) {
            Toast.makeText(this, "Valid 15-character GSTIN required", Toast.LENGTH_SHORT).show(); return false;
        }
        if (etBankName == null || etBankName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Bank Name is required", Toast.LENGTH_SHORT).show(); return false;
        }
        if (etIfsc == null || etIfsc.getText().toString().trim().length() != 11) {
            Toast.makeText(this, "Valid 11-character IFSC Code required", Toast.LENGTH_SHORT).show(); return false;
        }
        return true;
    }

    private void submitApplication() {
        progress = new ProgressDialog(this);
        progress.setMessage("Submitting application…");
        progress.setCancelable(false);
        progress.show();

        try {
            JSONObject body = new JSONObject();
            // Section A
            body.put("vendor_type",     VENDOR_TYPES[spVendorType != null ? spVendorType.getSelectedItemPosition() : 0]);
            body.put("business_type",   BIZ_TYPES[spBusinessType != null ? spBusinessType.getSelectedItemPosition() : 0]);
            body.put("vendor_relation", spVendorRelation != null && spVendorRelation.getSelectedItemPosition() == 0 ? "PV" : "OTV");
            body.put("company_code",    COMPANY_CODES[spCompanyCode != null ? spCompanyCode.getSelectedItemPosition() : 0].substring(0,4));
            body.put("division",        DIVISIONS[spDivision != null ? spDivision.getSelectedItemPosition() : 0]);
            body.put("vendor_name",     etVendorName != null ? etVendorName.getText().toString().trim() : "");
            body.put("vendor_address",  etAddress   != null ? etAddress.getText().toString().trim() : "");
            body.put("city",            etCity      != null ? etCity.getText().toString().trim() : "");
            body.put("state",           STATES[spState != null ? spState.getSelectedItemPosition() : 0]);
            body.put("postal_code",     etPostal    != null ? etPostal.getText().toString().trim() : "");
            body.put("country",         "India");
            body.put("contact_person",  etContact   != null ? etContact.getText().toString().trim() : "");
            body.put("mobile",          etMobile    != null ? etMobile.getText().toString().trim() : "");
            body.put("email",           etEmail     != null ? etEmail.getText().toString().trim() : "");
            // Section B
            body.put("bank_name",       etBankName  != null ? etBankName.getText().toString().trim() : "");
            body.put("bank_account_no", etAccountNo != null ? etAccountNo.getText().toString().trim() : "");
            body.put("bank_branch",     etBranch    != null ? etBranch.getText().toString().trim() : "");
            body.put("ifsc_code",       etIfsc      != null ? etIfsc.getText().toString().trim().toUpperCase() : "");
            body.put("pan_no",          etPan       != null ? etPan.getText().toString().trim().toUpperCase() : "");
            body.put("gstin",           etGstin     != null ? etGstin.getText().toString().trim().toUpperCase() : "");
            body.put("service_tax_no",  etServiceTax!= null ? etServiceTax.getText().toString().trim() : "");
            body.put("withholding_tax_code", etWht  != null ? etWht.getText().toString().trim() : "");
            // Section C
            body.put("payment_terms_days", etPaymentDays != null && !etPaymentDays.getText().toString().isEmpty() ? Integer.parseInt(etPaymentDays.getText().toString().trim()) : 30);
            body.put("merchandise_category", etMerchandise != null ? etMerchandise.getText().toString().trim() : "");
            body.put("vrp",             etVrp       != null ? etVrp.getText().toString().trim() : "");
            body.put("article_to_purchase", etArticle != null ? etArticle.getText().toString().trim() : "");
            body.put("buying_per_month", etBuying   != null && !etBuying.getText().toString().isEmpty() ? Double.parseDouble(etBuying.getText().toString().trim()) : 0);

            String url = getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST;
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    dismiss();
                    try {
                        String ticket = response.getJSONObject("data").optString("ticket_no", "");
                        Toast.makeText(this, "Submitted! Ticket: " + ticket, Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, SrmVendorDashboardActivity.class));
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Submitted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                error -> {
                    dismiss();
                    String msg = "Submission failed";
                    if (error.networkResponse != null) {
                        try { msg = new JSONObject(new String(error.networkResponse.data)).optString("message", msg); } catch (Exception ignored) {}
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            ) {
                @Override public Map<String,String> getHeaders() {
                    Map<String,String> h = new HashMap<>();
                    h.put("Authorization","Bearer " + prefs.read(SrmVars.PREF_SRM_TOKEN));
                    return h;
                }
                @Override public String getBodyContentType() { return "application/json"; }
                @Override public byte[] getBody() { return body.toString().getBytes(); }
            };
            req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(20000, 1, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            ApplicationController.getInstance().getRequestQueue().add(req);

        } catch (Exception e) {
            dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void dismiss() {
        if (progress != null && progress.isShowing()) { progress.dismiss(); progress = null; }
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
