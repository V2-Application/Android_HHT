package com.v2retail.dotvik.srm;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SrmApprovalDetailActivity extends AppCompatActivity {

    private TextView tvTicket, tvVendorName, tvType, tvDivision, tvCompany,
                     tvPan, tvGstin, tvBank, tvIfsc, tvMobile, tvCity;
    private LinearLayout llChecklist, llDocs, layoutApproveBar, llApprovalTrail;
    private Button btnApprove, btnReject;
    private ProgressBar progress;
    private ScrollView scrollContent;
    private TextView tvCurrentStage, tvStageMsg;

    private String appId;
    private String currentStage;
    private final List<CheckBox> checkBoxes = new ArrayList<>();

    private static final String[][] ROLE_CHECKS = {
        {},  // placeholder index 0
        {"Vendor details complete and accurate",       // L1
         "Business type matches supporting documents",
         "Contact information valid and verified",
         "Division / section allocation correct",
         "No duplicate vendor detected",
         "All mandatory documents submitted"},
        {"Vendor aligns with business division strategy", // L2
         "Merchandise category and division match",
         "Commercial terms acceptable",
         "Vendor relation type appropriate",
         "Sourcing recommendation reviewed",
         "Financial credentials noted"},
        {"Bank account verified against cancelled cheque", // L3
         "PAN number matches GST records",
         "GSTIN validated on GST portal",
         "Withholding tax code identified",
         "No outstanding dues or blacklisting",
         "Financial standing acceptable"},
        {"Purchase terms and conditions acceptable",    // L4
         "Payment terms within company policy",
         "Article/category approved for procurement",
         "MOQ and pricing within policy",
         "Vendor meets compliance requirements",
         "PO Committee quorum decision recorded"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_approval_detail);

        appId = getIntent().getStringExtra("app_id");

        tvTicket      = findViewById(R.id.tvDetailTicket);
        tvVendorName  = findViewById(R.id.tvDetailVendorName);
        tvType        = findViewById(R.id.tvDetailType);
        tvDivision    = findViewById(R.id.tvDetailDivision);
        tvCompany     = findViewById(R.id.tvDetailCompany);
        tvPan         = findViewById(R.id.tvDetailPan);
        tvGstin       = findViewById(R.id.tvDetailGstin);
        tvBank        = findViewById(R.id.tvDetailBank);
        tvIfsc        = findViewById(R.id.tvDetailIfsc);
        tvMobile      = findViewById(R.id.tvDetailMobile);
        tvCity        = findViewById(R.id.tvDetailCity);
        llChecklist   = findViewById(R.id.llDetailChecklist);
        llDocs        = findViewById(R.id.llDetailDocs);
        layoutApproveBar = findViewById(R.id.layoutDetailApproveBar);
        llApprovalTrail  = findViewById(R.id.llDetailTrail);
        btnApprove    = findViewById(R.id.btnDetailApprove);
        btnReject     = findViewById(R.id.btnDetailReject);
        progress      = findViewById(R.id.progressDetail);
        scrollContent = findViewById(R.id.scrollDetail);
        tvCurrentStage= findViewById(R.id.tvDetailCurrentStage);
        tvStageMsg    = findViewById(R.id.tvDetailStageMsg);

        btnApprove.setOnClickListener(v -> confirmApprove());
        btnReject.setOnClickListener(v -> confirmReject());
        findViewById(R.id.btnDetailBack).setOnClickListener(v -> finish());

        loadDetail();
    }

    private void loadDetail() {
        progress.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.GONE);
        SrmApiClient.get(this, "/applications/" + appId, res -> {
            progress.setVisibility(View.GONE);
            scrollContent.setVisibility(View.VISIBLE);
            try {
                JSONObject data = res.getJSONObject("data");
                currentStage = data.optString("current_stage", "");
                populateFields(data);
                populateDocs(data.optJSONArray("documents"));
                populateChecklist();
                populateTrail(data.optJSONArray("approvalTrail"));
                updateApproveBar();
            } catch (Exception e) {
                Toast.makeText(this, "Error loading detail", Toast.LENGTH_SHORT).show();
            }
        }, e -> {
            progress.setVisibility(View.GONE);
            Toast.makeText(this, SrmApiClient.parseError(e), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void populateFields(JSONObject d) {
        tvTicket.setText(d.optString("ticket_no", "—"));
        tvVendorName.setText(d.optString("vendor_name", "—"));
        tvType.setText(d.optString("vendor_type", "—"));
        tvDivision.setText(d.optString("division", "—"));
        tvCompany.setText(d.optString("company_code", "—"));
        tvPan.setText(d.optString("pan_no", "—"));
        tvGstin.setText(d.optString("gstin", "—"));
        tvBank.setText(d.optString("bank_name", "—") + " — " + d.optString("bank_account_no", ""));
        tvIfsc.setText(d.optString("ifsc_code", "—"));
        tvMobile.setText(d.optString("mobile", "—"));
        tvCity.setText(d.optString("city", "—") + ", " + d.optString("state", ""));
        tvCurrentStage.setText(stageLabel(currentStage));
    }

    private void populateDocs(JSONArray docs) {
        llDocs.removeAllViews();
        String[] reqTypes  = {"PAN_CARD","CANCELLED_CHEQUE","GST_CERTIFICATE","BILL_COPY"};
        String[] reqLabels = {"PAN Card","Cancelled Cheque","GST Certificate","Bill Copy"};
        String[] reqIcons  = {"📄","🏦","📑","🧾"};
        for (int i = 0; i < reqTypes.length; i++) {
            boolean found = false;
            String docId = null;
            if (docs != null) {
                for (int j = 0; j < docs.length(); j++) {
                    try {
                        JSONObject d = docs.getJSONObject(j);
                        if (reqTypes[i].equals(d.optString("doc_type"))) {
                            found = true; docId = d.optString("id"); break;
                        }
                    } catch (Exception ignored) {}
                }
            }
            View row = getLayoutInflater().inflate(R.layout.item_srm_doc_row, llDocs, false);
            TextView icon   = row.findViewById(R.id.tvDocRowIcon);
            TextView label  = row.findViewById(R.id.tvDocRowLabel);
            TextView status = row.findViewById(R.id.tvDocRowStatus);
            icon.setText(reqIcons[i]);
            label.setText(reqLabels[i]);
            if (found) {
                status.setText("✔ Uploaded"); status.setTextColor(0xFF388E3C);
            } else {
                status.setText("✗ Missing"); status.setTextColor(0xFFC62828);
            }
            llDocs.addView(row);
        }
    }

    private void populateChecklist() {
        llChecklist.removeAllViews();
        checkBoxes.clear();
        String role = SrmApiClient.getSavedRole(this);
        int idx = roleCheckIdx(role);
        if (idx < 1 || idx >= ROLE_CHECKS.length) return;
        for (String check : ROLE_CHECKS[idx]) {
            CheckBox cb = new CheckBox(this);
            cb.setText(check);
            cb.setTextColor(0xFFE8EDF5);
            cb.setTextSize(13f);
            cb.setPadding(0, 12, 0, 12);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF3B82F6));
            cb.setOnCheckedChangeListener((b, c) -> updateApproveBar());
            checkBoxes.add(cb);
            llChecklist.addView(cb);
        }
    }

    private void populateTrail(JSONArray trail) {
        llApprovalTrail.removeAllViews();
        if (trail == null || trail.length() == 0) return;
        for (int i = 0; i < trail.length(); i++) {
            try {
                JSONObject entry = trail.getJSONObject(i);
                View row = getLayoutInflater().inflate(R.layout.item_srm_trail_row, llApprovalTrail, false);
                TextView tvLevel   = row.findViewById(R.id.tvTrailLevel);
                TextView tvApprover= row.findViewById(R.id.tvTrailApprover);
                TextView tvAction  = row.findViewById(R.id.tvTrailAction);
                TextView tvDate    = row.findViewById(R.id.tvTrailDate);
                TextView tvRemarks = row.findViewById(R.id.tvTrailRemarks);
                tvLevel.setText(stageLabel(entry.optString("level")));
                tvApprover.setText(entry.optString("approver_name", ""));
                String action = entry.optString("action", "");
                tvAction.setText(action);
                tvAction.setTextColor("APPROVED".equals(action) ? 0xFF388E3C : 0xFFC62828);
                String date = entry.optString("created_at", "");
                tvDate.setText(date.length() >= 10 ? date.substring(0, 10) : date);
                String remarks = entry.optString("remarks", "");
                if (!remarks.isEmpty() && !remarks.equals("null")) {
                    tvRemarks.setText("\"" + remarks + "\"");
                    tvRemarks.setVisibility(View.VISIBLE);
                } else {
                    tvRemarks.setVisibility(View.GONE);
                }
                llApprovalTrail.addView(row);
            } catch (Exception ignored) {}
        }
    }

    private void updateApproveBar() {
        String role = SrmApiClient.getSavedRole(this);
        int stageIdx = stageIndex(currentStage);
        int roleIdx  = roleCheckIdx(role);
        boolean isMyStage = (roleIdx == stageIdx) || "admin".equals(role);
        boolean allChecked = checkBoxes.stream().allMatch(CheckBox::isChecked);
        boolean canAct = isMyStage && !isFinalized();

        layoutApproveBar.setVisibility(canAct ? View.VISIBLE : View.GONE);
        btnApprove.setEnabled(allChecked);
        btnApprove.setAlpha(allChecked ? 1f : 0.4f);

        if (canAct) {
            tvStageMsg.setText("Awaiting your review as " + SrmApiClient.getSavedRole(this));
            tvStageMsg.setVisibility(View.VISIBLE);
        } else if (isFinalized()) {
            tvStageMsg.setText(currentStage.equals("APPROVED") ? "✅ Approved — SAP created" : "✕ Rejected");
            tvStageMsg.setVisibility(View.VISIBLE);
        }
    }

    private boolean isFinalized() {
        return "APPROVED".equals(currentStage) || "REJECTED".equals(currentStage);
    }

    private void confirmApprove() {
        new AlertDialog.Builder(this, R.style.SrmAlertDialog)
                .setTitle("Confirm Approval")
                .setMessage("Approve and forward to the next level?")
                .setPositiveButton("Approve", (d, w) -> doApprove())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReject() {
        final EditText input = new EditText(this);
        input.setHint("Rejection reason (required)");
        input.setTextColor(0xFFE8EDF5);
        input.setHintTextColor(0xFF5A6A8A);
        input.setPadding(32, 20, 32, 20);
        new AlertDialog.Builder(this, R.style.SrmAlertDialog)
                .setTitle("Reject Application")
                .setView(input)
                .setPositiveButton("Reject", (d, w) -> {
                    String remarks = input.getText().toString().trim();
                    if (remarks.isEmpty()) {
                        Toast.makeText(this, "Rejection reason is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doReject(remarks);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doApprove() {
        progress.setVisibility(View.VISIBLE);
        try {
            JSONObject body = new JSONObject();
            body.put("remarks", "");
            SrmApiClient.post(this, "/applications/" + appId + "/approve", body, res -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "✔ Approved and forwarded!", Toast.LENGTH_SHORT).show();
                finish();
            }, e -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, SrmApiClient.parseError(e), Toast.LENGTH_LONG).show();
            });
        } catch (Exception ignored) {}
    }

    private void doReject(String remarks) {
        progress.setVisibility(View.VISIBLE);
        try {
            JSONObject body = new JSONObject();
            body.put("remarks", remarks);
            SrmApiClient.post(this, "/applications/" + appId + "/reject", body, res -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Application rejected", Toast.LENGTH_SHORT).show();
                finish();
            }, e -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, SrmApiClient.parseError(e), Toast.LENGTH_LONG).show();
            });
        } catch (Exception ignored) {}
    }

    private int roleCheckIdx(String role) {
        switch (role) {
            case "subdiv":  return 1;
            case "divhead": return 2;
            case "finance": return 3;
            case "pocomm":  return 4;
            default: return 0;
        }
    }

    private int stageIndex(String stage) {
        switch (stage) {
            case "L1": return 1; case "L2": return 2;
            case "L3": return 3; case "L4": return 4;
            default: return 0;
        }
    }

    private String stageLabel(String s) {
        switch (s) {
            case "L1": return "Level 1 – Sub Div Head";
            case "L2": return "Level 2 – Division Head";
            case "L3": return "Level 3 – Finance";
            case "L4": return "Level 4 – PO Committee";
            case "L5": return "Level 5 – MDM Team";
            case "APPROVED": return "Created in SAP ✓";
            case "REJECTED": return "Rejected";
            default: return s;
        }
    }
}
