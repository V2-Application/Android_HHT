package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * MDM Dashboard — shows SAP queue, created vendors, connection status
 */
public class SrmMdmDashboardActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private TextView tvWelcome, tvSapQueueCount, tvCreatedCount, tvTotalCount, tvSapStatus;
    private RecyclerView rvQueue;
    private Button btnLogout;
    private List<JSONObject> queueList = new ArrayList<>();
    private MdmQueueAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_mdm_dashboard);

        prefs           = new SharedPreferencesData(getApplicationContext());
        tvWelcome       = findViewById(R.id.srm_tv_welcome);
        tvSapQueueCount = findViewById(R.id.srm_tv_sap_queue_count);
        tvCreatedCount  = findViewById(R.id.srm_tv_created_count);
        tvTotalCount    = findViewById(R.id.srm_tv_total_count);
        tvSapStatus     = findViewById(R.id.srm_tv_sap_status);
        rvQueue         = findViewById(R.id.srm_rv_queue);
        btnLogout       = findViewById(R.id.srm_btn_logout);

        String name = prefs.read(SrmVars.PREF_SRM_USER_NAME);
        tvWelcome.setText("MDM — " + (name != null ? name : "Team"));

        adapter = new MdmQueueAdapter();
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        rvQueue.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            prefs.write(SrmVars.PREF_SRM_TOKEN, "");
            startActivity(new Intent(this, SrmLoginActivity.class));
            finish();
        });

        loadDashboard();
        loadSapStatus();
    }

    @Override protected void onResume() { super.onResume(); loadDashboard(); }

    private void loadDashboard() {
        // Stats
        JsonObjectRequest statsReq = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + SrmVars.APPLICATIONS_STATS, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    tvSapQueueCount.setText(String.valueOf(d.optInt("l5", 0)));
                    tvCreatedCount.setText(String.valueOf(d.optInt("approved", 0)));
                    tvTotalCount.setText(String.valueOf(d.optInt("total", 0)));
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };

        // SAP Queue (L5)
        JsonObjectRequest queueReq = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST + "?stage=L5&limit=50", null,
            response -> {
                try {
                    JSONArray arr = response.getJSONArray("data");
                    queueList.clear();
                    for (int i = 0; i < arr.length(); i++) queueList.add(arr.getJSONObject(i));
                    adapter.notifyDataSetChanged();
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };

        ApplicationController.getInstance().getRequestQueue().add(statsReq);
        ApplicationController.getInstance().getRequestQueue().add(queueReq);
    }

    private void loadSapStatus() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + SrmVars.MDM_SAP_STATUS, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    boolean rfc = d.optBoolean("rfcAvailable", false);
                    tvSapStatus.setText(rfc ? "SAP RFC: Connected" : "SAP: REST Fallback");
                    tvSapStatus.setTextColor(rfc ? 0xFF059669 : 0xFFF59E0B);
                } catch (Exception ignored) {}
            }, error -> tvSapStatus.setText("SAP: Unknown")
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    class MdmQueueAdapter extends RecyclerView.Adapter<MdmQueueAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvTicket, tvType;
            Button btnPush;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.srm_item_vendor_name);
                tvTicket = v.findViewById(R.id.srm_item_ticket);
                tvType   = v.findViewById(R.id.srm_item_type);
                btnPush  = v.findViewById(R.id.srm_item_btn_review);
                if (btnPush != null) btnPush.setText("SAP Push");
            }
        }
        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_srm_application, p, false));
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            try {
                JSONObject app = queueList.get(pos);
                h.tvName.setText(app.optString("vendor_name", ""));
                h.tvTicket.setText(app.optString("ticket_no", ""));
                h.tvType.setText(app.optString("vendor_type", ""));
                h.btnPush.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(SrmMdmDashboardActivity.this, SrmMdmSapPushActivity.class);
                        intent.putExtra("app_id", app.getString("id"));
                        intent.putExtra("ticket_no", app.optString("ticket_no", ""));
                        startActivity(intent);
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (Exception ignored) {}
        }
        @Override public int getItemCount() { return queueList.size(); }
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
