package com.v2retail.dotvik.srm;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class SrmTrackApplicationActivity extends AppCompatActivity {

    private EditText etTicket;
    private Button btnSearch;
    private ProgressBar progress;
    private LinearLayout layoutResult, layoutPipeline;
    private TextView tvTicketNo, tvVendorName, tvStatus, tvSubmitted, tvSapCode;
    private LinearLayout llSapCode;

    private static final String[] STAGES = {"SUBMITTED","L1","L2","L3","L4","L5","APPROVED"};
    private static final String[] STAGE_LABELS = {
        "Submitted","Level 1 – Sub Div Head","Level 2 – Division Head",
        "Level 3 – Finance","Level 4 – PO Committee","Level 5 – MDM Team","Created in SAP"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_track);

        etTicket     = findViewById(R.id.etSrmTrackTicket);
        btnSearch    = findViewById(R.id.btnSrmTrackSearch);
        progress     = findViewById(R.id.progressSrmTrack);
        layoutResult = findViewById(R.id.layoutSrmTrackResult);
        layoutPipeline= findViewById(R.id.layoutSrmTrackPipeline);
        tvTicketNo   = findViewById(R.id.tvSrmTrackTicket);
        tvVendorName = findViewById(R.id.tvSrmTrackVendor);
        tvStatus     = findViewById(R.id.tvSrmTrackStatus);
        tvSubmitted  = findViewById(R.id.tvSrmTrackSubmitted);
        tvSapCode    = findViewById(R.id.tvSrmTrackSapCode);
        llSapCode    = findViewById(R.id.llSrmTrackSapCode);

        btnSearch.setOnClickListener(v -> search());

        // Auto-search if launched with ticket
        String ticket = getIntent().getStringExtra("ticket_no");
        if (ticket != null) {
            etTicket.setText(ticket);
            search();
        }
    }

    private void search() {
        String q = etTicket.getText().toString().trim();
        if (q.isEmpty()) { Toast.makeText(this,"Enter ticket number",Toast.LENGTH_SHORT).show(); return; }
        progress.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);

        SrmApiClient.get(this, "/applications/" + q, response -> {
            progress.setVisibility(View.GONE);
            try {
                JSONObject data = response.getJSONObject("data");
                String stage    = data.optString("current_stage","SUBMITTED");
                String sapCode  = data.optString("sap_vendor_code","");

                tvTicketNo.setText(data.optString("ticket_no","—"));
                tvVendorName.setText(data.optString("vendor_name","—"));
                tvStatus.setText(stageLabel(stage));
                tvSubmitted.setText(data.optString("submitted_at","—").substring(0, Math.min(10, data.optString("submitted_at").length())));

                if (!sapCode.isEmpty() && !sapCode.equals("null")) {
                    llSapCode.setVisibility(View.VISIBLE);
                    tvSapCode.setText(sapCode);
                } else {
                    llSapCode.setVisibility(View.GONE);
                }

                buildPipeline(stage, data.optJSONArray("approvalTrail"));
                layoutResult.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            progress.setVisibility(View.GONE);
            Toast.makeText(this, SrmApiClient.parseError(error), Toast.LENGTH_LONG).show();
        });
    }

    private void buildPipeline(String currentStage, JSONArray trail) {
        layoutPipeline.removeAllViews();
        int stageIdx = indexOf(currentStage);
        boolean rejected = "REJECTED".equals(currentStage);

        for (int i = 0; i < STAGES.length; i++) {
            View row = getLayoutInflater().inflate(R.layout.item_srm_pipeline_step, layoutPipeline, false);
            TextView tvNum    = row.findViewById(R.id.tvPipelineNum);
            TextView tvLabel  = row.findViewById(R.id.tvPipelineLabel);
            TextView tvNote   = row.findViewById(R.id.tvPipelineNote);
            TextView tvBadge  = row.findViewById(R.id.tvPipelineBadge);
            View     indicator= row.findViewById(R.id.viewPipelineIndicator);

            tvNum.setText(String.valueOf(i + 1));
            tvLabel.setText(STAGE_LABELS[i]);

            String status;
            if (rejected && i < stageIdx) status = "approved";
            else if (!rejected && i < stageIdx) status = "approved";
            else if (i == stageIdx && !rejected) status = "active";
            else if (i == stageIdx && rejected) status = "rejected";
            else status = "pending";

            // Lookup trail note
            if (trail != null) {
                try {
                    for (int t = 0; t < trail.length(); t++) {
                        JSONObject entry = trail.getJSONObject(t);
                        if (STAGES[i].equals(entry.optString("level"))) {
                            tvNote.setText(entry.optString("approver_name","") + " · " +
                                    entry.optString("action","") + " · " +
                                    entry.optString("created_at","").substring(0,10));
                            tvNote.setVisibility(View.VISIBLE);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            applyStepStyle(indicator, tvBadge, tvNum, status);
            layoutPipeline.addView(row);
        }
    }

    private void applyStepStyle(View indicator, TextView badge, TextView num, String status) {
        switch (status) {
            case "approved":
                indicator.setBackgroundResource(R.drawable.bg_step_approved);
                badge.setText("APPROVED"); badge.setTextColor(0xFF2E7D32);
                num.setText("✓");
                break;
            case "active":
                indicator.setBackgroundResource(R.drawable.bg_step_active);
                badge.setText("IN REVIEW"); badge.setTextColor(0xFF1565C0);
                break;
            case "rejected":
                indicator.setBackgroundResource(R.drawable.bg_step_rejected);
                badge.setText("REJECTED"); badge.setTextColor(0xFFC62828);
                break;
            default:
                indicator.setBackgroundResource(R.drawable.bg_step_pending);
                badge.setText("PENDING"); badge.setTextColor(0xFF9E9E9E);
                break;
        }
    }

    private int indexOf(String stage) {
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i].equals(stage)) return i;
        return 0;
    }

    private String stageLabel(String s) {
        int idx = indexOf(s);
        return idx < STAGE_LABELS.length ? STAGE_LABELS[idx] : s;
    }
}
