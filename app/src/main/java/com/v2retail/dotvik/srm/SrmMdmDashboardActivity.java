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

public class SrmMdmDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvSapQueue, tvCreated, tvTotal;
    private ListView lvSapQueue;
    private ProgressBar progress;
    private Button btnLogout;
    private TextView tvSapStatus, tvEmptyQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_mdm_dashboard);

        tvWelcome   = findViewById(R.id.tvMdmWelcome);
        tvSapQueue  = findViewById(R.id.tvMdmSapQueue);
        tvCreated   = findViewById(R.id.tvMdmCreated);
        tvTotal     = findViewById(R.id.tvMdmTotal);
        lvSapQueue  = findViewById(R.id.lvMdmQueue);
        progress    = findViewById(R.id.progressMdmDash);
        btnLogout   = findViewById(R.id.btnMdmLogout);
        tvSapStatus = findViewById(R.id.tvMdmSapStatus);
        tvEmptyQueue= findViewById(R.id.tvMdmEmptyQueue);

        try {
            JSONObject user = new JSONObject(SrmApiClient.getSavedUser(this));
            tvWelcome.setText(user.optString("full_name", "MDM User"));
        } catch (Exception ignored) {}

        btnLogout.setOnClickListener(v -> logout());
        loadData();
        loadSapStatus();
    }

    @Override protected void onResume() { super.onResume(); loadData(); }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        // Stats
        SrmApiClient.get(this, "/applications/stats", res -> {
            try {
                JSONObject d = res.getJSONObject("data");
                tvSapQueue.setText(String.valueOf(d.optInt("l5", 0)));
                tvCreated.setText(String.valueOf(d.optInt("approved", 0)));
                tvTotal.setText(String.valueOf(d.optInt("total", 0)));
            } catch (Exception ignored) {}
        }, e -> {});

        // SAP Push Queue (L5 applications)
        SrmApiClient.get(this, "/applications?stage=L5&limit=50", res -> {
            progress.setVisibility(View.GONE);
            try {
                JSONArray data = res.getJSONArray("data");
                if (data.length() == 0) {
                    tvEmptyQueue.setVisibility(View.VISIBLE);
                    lvSapQueue.setVisibility(View.GONE);
                } else {
                    tvEmptyQueue.setVisibility(View.GONE);
                    lvSapQueue.setVisibility(View.VISIBLE);
                    String[] items = new String[data.length()];
                    final String[] ids = new String[data.length()];
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject app = data.getJSONObject(i);
                        items[i] = app.optString("ticket_no") + "  |  " + app.optString("vendor_name")
                                + "\n" + app.optString("vendor_type") + " · " + app.optString("division")
                                + "  ·  SAP READY";
                        ids[i] = app.optString("id");
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            R.layout.item_srm_queue_row, R.id.tvQueueRowText, items);
                    lvSapQueue.setAdapter(adapter);
                    lvSapQueue.setOnItemClickListener((p, v, pos, id) -> {
                        Intent i = new Intent(this, SrmMdmSapPushActivity.class);
                        i.putExtra("app_id", ids[pos]);
                        startActivity(i);
                    });
                }
            } catch (Exception e) { progress.setVisibility(View.GONE); }
        }, e -> progress.setVisibility(View.GONE));
    }

    private void loadSapStatus() {
        SrmApiClient.get(this, "/mdm/sap-status", res -> {
            try {
                JSONObject d = res.getJSONObject("data");
                boolean rfc = d.optBoolean("rfcAvailable", false);
                tvSapStatus.setText(rfc ? "SAP RFC: Connected ✓" : "SAP RFC: Offline (REST fallback active)");
                tvSapStatus.setTextColor(rfc ? 0xFF388E3C : 0xFFF59E0B);
            } catch (Exception ignored) {}
        }, e -> tvSapStatus.setText("SAP Status: Unknown"));
    }

    private void logout() {
        SrmApiClient.post(this, "/auth/logout", new JSONObject(), r -> {}, e -> {});
        SrmApiClient.clearSession(this);
        startActivity(new Intent(this, SrmLoginActivity.class));
        finish();
    }
}
