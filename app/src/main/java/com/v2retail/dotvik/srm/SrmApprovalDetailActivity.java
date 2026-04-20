package com.v2retail.dotvik.srm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * Approval Detail — shows full vendor info, docs, checklist, approve/reject
 * Used by L1-L4 approvers and Admin
 */
public class SrmApprovalDetailActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private String appId, ticketNo;
    private TextView tvVendorName, tvTicket, tvType, tvDivision, tvPan, tvGstin,
                     tvBank, tvAccount, tvIfsc, tvCity, tvStage, tvPaymentTerms;
    private LinearLayout llChecklist, llDocs;
    private Button btnApprove, btnReject;
    private ProgressDialog progress;

    private final String[] CHECKLIST_SUBDIV = {
        "Vendor details complete and accurate",
        "Business type matches documents",
        "Contact information verified",
        "Division/section allocation correct",
        "No duplicate vendor detected",
        "All mandatory documents submitted"
    };
    private final String[] CHECKLIST_FINANCE = {
        "Bank details verified against cancelled cheque",
        "PAN matches GST records",
        "GSTIN validated on GST portal",
        "Withholding tax code identified",
        "No outstanding dues or blacklisting",
        "Financial standing acceptable"
    };
    private final String[] CHECKLIST_POCOMM = {
        "Purchase terms and conditions acceptable",
        "Payment terms within policy",
        "Article/category approved",
        "MOQ and pricing within policy",
        "Vendor meets compliance requirements",
        "PO Committee quorum recorded"
    };
    private final String[] CHECKLIST_DIVHEAD = {
        "Vendor aligns with business division strategy",
        "Merchandise category and division match",
        "Commercial terms are acceptable",
        "Vendor relation type appropriate",
        "Sourcing team recommendation reviewed",
        "Financial credentials noted"
    };

    private CheckBox[] checkBoxes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_approval_detail);

        prefs    = new SharedPreferencesData(getApplicationContext());
        appId    = getIntent().getStringExtra("app_id");
        ticketNo = getIntent().getStringExtra("ticket_no");

        tvVendorName   = findViewById(R.id.srm_tv_vendor_name);
        tvTicket       = findViewById(R.id.srm_tv_ticket);
        tvType         = findViewById(R.id.srm_tv_type);
        tvDivision     = findViewById(R.id.srm_tv_division);
        tvPan          = findViewById(R.id.srm_tv_pan);
        tvGstin        = findViewById(R.id.srm_tv_gstin);
        tvBank         = findViewById(R.id.srm_tv_bank);
        tvAccount      = findViewById(R.id.srm_tv_account);
        tvIfsc         = findViewById(R.id.srm_tv_ifsc);
        tvCity         = findViewById(R.id.srm_tv_city);
        tvStage        = findViewById(R.id.srm_tv_stage);
        tvPaymentTerms = findViewById(R.id.srm_tv_payment_terms);
        llChecklist    = findViewById(R.id.srm_ll_checklist);
        llDocs         = findViewById(R.id.srm_ll_docs);
        btnApprove     = findViewById(R.id.srm_btn_approve);
        btnReject      = findViewById(R.id.srm_btn_reject);

        buildChecklist();
        loadApplication();

        btnApprove.setOnClickListener(v -> {
            if (!allChecked()) {
                Toast.makeText(this, "Complete all checklist items first", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmAction(true);
        });
        btnReject.setOnClickListener(v -> confirmAction(false));
    }

    private void buildChecklist() {
        String role = prefs.read(SrmVars.PREF_SRM_USER_ROLE);
        String[] items;
        switch (role == null ? "" : role) {
            case SrmVars.ROLE_FINANCE: items = CHECKLIST_FINANCE; break;
            case SrmVars.ROLE_POCOMM:  items = CHECKLIST_POCOMM;  break;
            case SrmVars.ROLE_DIVHEAD: items = CHECKLIST_DIVHEAD; break;
            default:                   items = CHECKLIST_SUBDIV;  break;
        }

        checkBoxes = new CheckBox[items.length];
        llChecklist.removeAllViews();
        for (int i = 0; i < items.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(items[i]);
            cb.setTextSize(14f);
            cb.setPadding(8, 12, 8, 12);
            checkBoxes[i] = cb;
            llChecklist.addView(cb);
        }
    }

    private boolean allChecked() {
        if (checkBoxes == null) return false;
        for (CheckBox cb : checkBoxes) if (!cb.isChecked()) return false;
        return true;
    }

    private void loadApplication() {
        progress = new ProgressDialog(this);
        progress.setMessage("Loading…");
        progress.show();

        String url = getSrmBaseUrl() + "/" + appId;
        // Build full URL: /api/applications/:id
        String fullUrl = getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST.replace("applications", "applications") + "/" + appId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + "/api/applications/" + appId, null,
            response -> {
                dismiss();
                try {
                    JSONObject d = response.getJSONObject("data");
                    tvVendorName.setText(d.optString("vendor_name", "—"));
                    tvTicket.setText(d.optString("ticket_no", "—"));
                    tvType.setText(d.optString("vendor_type", "—"));
                    tvDivision.setText(d.optString("division", "—"));
                    tvPan.setText(d.optString("pan_no", "—"));
                    tvGstin.setText(d.optString("gstin", "—"));
                    tvBank.setText(d.optString("bank_name", "—"));
                    tvAccount.setText(d.optString("bank_account_no", "—"));
                    tvIfsc.setText(d.optString("ifsc_code", "—"));
                    tvCity.setText(d.optString("city", "—") + ", " + d.optString("state", ""));
                    tvStage.setText(SrmVars.stageName(d.optString("current_stage", "")));
                    tvPaymentTerms.setText(d.optString("payment_terms_days", "—") + " days");

                    // Documents
                    llDocs.removeAllViews();
                    JSONArray docs = d.optJSONArray("documents");
                    String[] docLabels = { "PAN_CARD:PAN Card", "CANCELLED_CHEQUE:Cancelled Cheque",
                                          "GST_CERTIFICATE:GST Certificate", "BILL_COPY:Bill Copy" };
                    for (String dl : docLabels) {
                        String[] parts = dl.split(":");
                        boolean found = false;
                        if (docs != null) {
                            for (int i = 0; i < docs.length(); i++) {
                                if (parts[0].equals(docs.getJSONObject(i).optString("doc_type"))) {
                                    found = true; break;
                                }
                            }
                        }
                        TextView tv = new TextView(this);
                        tv.setText((found ? "✔  " : "✗  ") + parts[1]);
                        tv.setTextColor(found ? 0xFF059669 : 0xFFDC2626);
                        tv.setTextSize(14f);
                        tv.setPadding(8, 8, 8, 8);
                        llDocs.addView(tv);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            },
            error -> { dismiss(); Toast.makeText(this, "Failed to load application", Toast.LENGTH_SHORT).show(); }
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };

        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void confirmAction(boolean approve) {
        if (approve) {
            new AlertDialog.Builder(this)
                .setTitle("Confirm Approval")
                .setMessage("Approve and forward to next level?")
                .setPositiveButton("Approve", (d, w) -> submitAction(true, ""))
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            EditText etRemark = new EditText(this);
            etRemark.setHint("Rejection reason (required)");
            new AlertDialog.Builder(this)
                .setTitle("Reject Application")
                .setView(etRemark)
                .setPositiveButton("Reject", (d, w) -> {
                    String r = etRemark.getText().toString().trim();
                    if (r.isEmpty()) { Toast.makeText(this, "Reason required", Toast.LENGTH_SHORT).show(); return; }
                    submitAction(false, r);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void submitAction(boolean approve, String remarks) {
        progress = new ProgressDialog(this);
        progress.setMessage(approve ? "Approving…" : "Rejecting…");
        progress.show();

        String endpoint = approve
            ? String.format(SrmVars.APPLICATIONS_APPROVE, appId)
            : String.format(SrmVars.APPLICATIONS_REJECT, appId);
        String url = getSrmBaseUrl() + endpoint;

        try {
            JSONObject body = new JSONObject();
            body.put("remarks", remarks);

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    dismiss();
                    String msg = approve ? "Approved and forwarded!" : "Application rejected";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    finish();
                },
                error -> {
                    dismiss();
                    String msg = "Action failed";
                    if (error.networkResponse != null) {
                        try { msg = new JSONObject(new String(error.networkResponse.data)).optString("message", msg); }
                        catch (Exception ignored) {}
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            ) {
                @Override public Map<String,String> getHeaders() { return authHeader(); }
                @Override public String getBodyContentType() { return "application/json"; }
                @Override public byte[] getBody() { return body.toString().getBytes(); }
            };
            ApplicationController.getInstance().getRequestQueue().add(req);
        } catch (Exception e) { dismiss(); e.printStackTrace(); }
    }

    private Map<String,String> authHeader() {
        Map<String,String> h = new HashMap<>();
        h.put("Authorization", "Bearer " + prefs.read(SrmVars.PREF_SRM_TOKEN));
        return h;
    }

    private void dismiss() {
        if (progress != null && progress.isShowing()) { progress.dismiss(); progress = null; }
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
