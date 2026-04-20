package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class SrmApproverDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvRole, tvPendingCount, tvTotalCount, tvApprovedCount;
    private ListView lvQueue;
    private ProgressBar progress;
    private Button btnLogout;
    private TextView tvEmptyQueue;

    private String myStage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_approver_dashboard);

        tvWelcome      = findViewById(R.id.tvApproverWelcome);
        tvRole         = findViewById(R.id.tvApproverRole);
        tvPendingCount = findViewById(R.id.tvApproverPending);
        tvTotalCount   = findViewById(R.id.tvApproverTotal);
        tvApprovedCount= findViewById(R.id.tvApproverApproved);
        lvQueue        = findViewById(R.id.lvApproverQueue);
        progress       = findViewById(R.id.progressApproverDash);
        btnLogout      = findViewById(R.id.btnApproverLogout);
        tvEmptyQueue   = findViewById(R.id.tvApproverEmptyQueue);

        String role = SrmApiClient.getSavedRole(this);
        myStage = roleToStage(role);

        try {
            JSONObject user = new JSONObject(SrmApiClient.getSavedUser(this));
            tvWelcome.setText(user.optString("full_name", "Approver"));
        } catch (Exception ignored) {}
        tvRole.setText(roleLabel(role));

        btnLogout.setOnClickListener(v -> logout());
        loadData();
    }

    @Override protected void onResume() { super.onResume(); loadData(); }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        // Load stats
        SrmApiClient.get(this, "/applications/stats", res -> {
            try {
                JSONObject d = res.getJSONObject("data");
                tvPendingCount.setText(String.valueOf(d.optInt(myStage.toLowerCase(), 0)));
                tvTotalCount.setText(String.valueOf(d.optInt("total", 0)));
                tvApprovedCount.setText(String.valueOf(d.optInt("approved", 0)));
            } catch (Exception ignored) {}
        }, e -> {});

        // Load my queue
        SrmApiClient.get(this, "/applications?stage=" + myStage + "&limit=50", res -> {
            progress.setVisibility(View.GONE);
            try {
                JSONArray data = res.getJSONArray("data");
                if (data.length() == 0) {
                    tvEmptyQueue.setVisibility(View.VISIBLE);
                    lvQueue.setVisibility(View.GONE);
                } else {
                    tvEmptyQueue.setVisibility(View.GONE);
                    lvQueue.setVisibility(View.VISIBLE);
                    String[] items = new String[data.length()];
                    final String[] ids = new String[data.length()];
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject app = data.getJSONObject(i);
                        items[i] = app.optString("ticket_no") + "  |  " + app.optString("vendor_name")
                                + "\n" + app.optString("vendor_type") + " · " + app.optString("division")
                                + "  ·  " + app.optString("submitted_at", "").substring(0, Math.min(10, app.optString("submitted_at").length()));
                        ids[i] = app.optString("id");
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            R.layout.item_srm_queue_row, R.id.tvQueueRowText, items);
                    lvQueue.setAdapter(adapter);
                    lvQueue.setOnItemClickListener((parent, view, position, id) -> {
                        Intent i = new Intent(this, SrmApprovalDetailActivity.class);
                        i.putExtra("app_id", ids[position]);
                        startActivity(i);
                    });
                }
            } catch (Exception e) { progress.setVisibility(View.GONE); }
        }, e -> progress.setVisibility(View.GONE));
    }

    private String roleToStage(String role) {
        switch (role) {
            case "subdiv":  return "L1";
            case "divhead": return "L2";
            case "finance": return "L3";
            case "pocomm":  return "L4";
            default:        return "L1";
        }
    }

    private String roleLabel(String role) {
        switch (role) {
            case "subdiv":  return "Sub Division Head — Level 1";
            case "divhead": return "Division Head — Level 2";
            case "finance": return "Finance Department — Level 3";
            case "pocomm":  return "PO Committee — Level 4";
            default:        return role;
        }
    }

    private void logout() {
        SrmApiClient.post(this, "/auth/logout", new JSONObject(), r -> {}, e -> {});
        SrmApiClient.clearSession(this);
        startActivity(new Intent(this, SrmLoginActivity.class));
        finish();
    }
}
