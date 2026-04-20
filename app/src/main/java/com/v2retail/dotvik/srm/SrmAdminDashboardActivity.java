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

public class SrmAdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotal, tvPipeline, tvCreated, tvRejected, tvL1, tvL2, tvL3, tvL5;
    private ListView lvApplications;
    private Spinner spStageFilter;
    private Button btnLogout, btnRefresh;
    private ProgressBar progress;
    private EditText etSearch;

    private JSONArray allApps = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_admin_dashboard);

        tvTotal    = findViewById(R.id.tvAdminTotal);
        tvPipeline = findViewById(R.id.tvAdminPipeline);
        tvCreated  = findViewById(R.id.tvAdminCreated);
        tvRejected = findViewById(R.id.tvAdminRejected);
        tvL1       = findViewById(R.id.tvAdminL1);
        tvL2       = findViewById(R.id.tvAdminL2);
        tvL3       = findViewById(R.id.tvAdminL3);
        tvL5       = findViewById(R.id.tvAdminL5);
        lvApplications = findViewById(R.id.lvAdminApps);
        spStageFilter  = findViewById(R.id.spAdminFilter);
        btnLogout  = findViewById(R.id.btnAdminLogout);
        btnRefresh = findViewById(R.id.btnAdminRefresh);
        progress   = findViewById(R.id.progressAdminDash);
        etSearch   = findViewById(R.id.etAdminSearch);

        setupStageFilter();
        btnLogout.setOnClickListener(v -> logout());
        btnRefresh.setOnClickListener(v -> loadData());
        lvApplications.setOnItemClickListener((parent, view, position, id) -> {
            try {
                JSONObject app = allApps.getJSONObject(position);
                Intent i = new Intent(this, SrmApprovalDetailActivity.class);
                i.putExtra("app_id", app.optString("id"));
                startActivity(i);
            } catch (Exception ignored) {}
        });

        loadData();
    }

    @Override protected void onResume() { super.onResume(); loadData(); }

    private void setupStageFilter() {
        String[] stages = {"All", "SUBMITTED", "L1", "L2", "L3", "L4", "L5", "APPROVED", "REJECTED"};
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stages);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStageFilter.setAdapter(a);
        spStageFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { loadApps(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void loadData() {
        // Stats
        SrmApiClient.get(this, "/applications/stats", res -> {
            try {
                JSONObject d = res.getJSONObject("data");
                tvTotal.setText(String.valueOf(d.optInt("total", 0)));
                tvPipeline.setText(String.valueOf(d.optInt("inPipeline", 0)));
                tvCreated.setText(String.valueOf(d.optInt("approved", 0)));
                tvRejected.setText(String.valueOf(d.optInt("rejected", 0)));
                tvL1.setText(String.valueOf(d.optInt("l1", 0)));
                tvL2.setText(String.valueOf(d.optInt("l2", 0)));
                tvL3.setText(String.valueOf(d.optInt("l3", 0)));
                tvL5.setText(String.valueOf(d.optInt("l5", 0)));
            } catch (Exception ignored) {}
        }, e -> {});
        loadApps();
    }

    private void loadApps() {
        progress.setVisibility(View.VISIBLE);
        String stage = spStageFilter.getSelectedItemPosition() == 0 ? "" :
                (String) spStageFilter.getSelectedItem();
        String url = "/applications?limit=100" + (stage.isEmpty() ? "" : "&stage=" + stage);

        SrmApiClient.get(this, url, res -> {
            progress.setVisibility(View.GONE);
            try {
                allApps = res.getJSONArray("data");
                buildList();
            } catch (Exception ignored) {}
        }, e -> progress.setVisibility(View.GONE));
    }

    private void buildList() {
        try {
            String[] items = new String[allApps.length()];
            for (int i = 0; i < allApps.length(); i++) {
                JSONObject app = allApps.getJSONObject(i);
                String stage = app.optString("current_stage", "");
                String sapCode = app.optString("sap_vendor_code", "");
                items[i] = app.optString("ticket_no") + "  |  " + app.optString("vendor_name")
                        + "\n" + app.optString("vendor_type") + " · " + app.optString("division")
                        + "  ·  " + stageLabel(stage)
                        + (sapCode.isEmpty() || sapCode.equals("null") ? "" : "  →  " + sapCode);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.item_srm_queue_row, R.id.tvQueueRowText, items);
            lvApplications.setAdapter(adapter);
        } catch (Exception ignored) {}
    }

    private String stageLabel(String s) {
        switch (s) {
            case "L1": return "L1 Sub Div"; case "L2": return "L2 Div Head";
            case "L3": return "L3 Finance"; case "L4": return "L4 PO Comm";
            case "L5": return "L5 MDM"; case "APPROVED": return "SAP Created ✓";
            case "REJECTED": return "Rejected"; case "SUBMITTED": return "Submitted";
            default: return s;
        }
    }

    private void logout() {
        SrmApiClient.post(this, "/auth/logout", new JSONObject(), r -> {}, e -> {});
        SrmApiClient.clearSession(this);
        startActivity(new Intent(this, SrmLoginActivity.class));
        finish();
    }
}
