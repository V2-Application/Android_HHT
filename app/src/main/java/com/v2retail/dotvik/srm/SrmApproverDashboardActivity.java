package com.v2retail.dotvik.srm;

import android.app.ProgressDialog;
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

public class SrmApproverDashboardActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private TextView tvTitle, tvRole, tvQueueCount, tvTotalCount;
    private RecyclerView rvQueue;
    private Button btnLogout;
    private List<JSONObject> queueList = new ArrayList<>();
    private QueueAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_approver_dashboard);

        prefs        = new SharedPreferencesData(getApplicationContext());
        tvTitle      = findViewById(R.id.srm_tv_title);
        tvRole       = findViewById(R.id.srm_tv_role);
        tvQueueCount = findViewById(R.id.srm_tv_queue_count);
        tvTotalCount = findViewById(R.id.srm_tv_total_count);
        rvQueue      = findViewById(R.id.srm_rv_queue);
        btnLogout    = findViewById(R.id.srm_btn_logout);

        String role = prefs.read(SrmVars.PREF_SRM_USER_ROLE);
        String name = prefs.read(SrmVars.PREF_SRM_USER_NAME);
        tvTitle.setText(name != null ? name : "Approver");
        tvRole.setText(roleLabel(role));

        adapter = new QueueAdapter();
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        rvQueue.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> doLogout());
        loadDashboard(role);
    }

    @Override protected void onResume() { super.onResume(); loadDashboard(prefs.read(SrmVars.PREF_SRM_USER_ROLE)); }

    private void loadDashboard(String role) {
        String statsUrl = getSrmBaseUrl() + SrmVars.APPLICATIONS_STATS;
        JsonObjectRequest statsReq = new JsonObjectRequest(Request.Method.GET, statsUrl, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    tvTotalCount.setText(String.valueOf(d.optInt("total", 0)));
                    tvQueueCount.setText(String.valueOf(d.optInt(getStageKey(role), 0)));
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return authHeader(); } };

        String myStage = getMyStage(role);
        String queueUrl = getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST + "?stage=" + myStage + "&limit=50";
        JsonObjectRequest queueReq = new JsonObjectRequest(Request.Method.GET, queueUrl, null,
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

    class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvTicket, tvType, tvDate;
            Button btnReview;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.srm_item_vendor_name);
                tvTicket  = v.findViewById(R.id.srm_item_ticket);
                tvType    = v.findViewById(R.id.srm_item_type);
                tvDate    = v.findViewById(R.id.srm_item_date);
                btnReview = v.findViewById(R.id.srm_item_btn_review);
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
                String dt = app.optString("submitted_at", "");
                h.tvDate.setText(dt.length() >= 10 ? dt.substring(0, 10) : dt);
                h.btnReview.setOnClickListener(v -> {
                    try {
                        android.content.Intent intent = new android.content.Intent(
                            SrmApproverDashboardActivity.this, SrmApprovalDetailActivity.class);
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

    private String getMyStage(String role) {
        switch (role == null ? "" : role) {
            case SrmVars.ROLE_SUBDIV:  return SrmVars.STAGE_L1;
            case SrmVars.ROLE_DIVHEAD: return SrmVars.STAGE_L2;
            case SrmVars.ROLE_FINANCE: return SrmVars.STAGE_L3;
            case SrmVars.ROLE_POCOMM:  return SrmVars.STAGE_L4;
            default: return SrmVars.STAGE_L1;
        }
    }

    private String getStageKey(String role) {
        switch (role == null ? "" : role) {
            case SrmVars.ROLE_SUBDIV:  return "l1";
            case SrmVars.ROLE_DIVHEAD: return "l2";
            case SrmVars.ROLE_FINANCE: return "l3";
            case SrmVars.ROLE_POCOMM:  return "l4";
            default: return "l1";
        }
    }

    private String roleLabel(String role) {
        switch (role == null ? "" : role) {
            case SrmVars.ROLE_SUBDIV:  return "Sub Division Head — L1 Approver";
            case SrmVars.ROLE_DIVHEAD: return "Division Head — L2 Approver";
            case SrmVars.ROLE_FINANCE: return "Finance Department — L3 Approver";
            case SrmVars.ROLE_POCOMM:  return "PO Committee — L4 Approver";
            default: return "Approver";
        }
    }

    private void doLogout() {
        prefs.write(SrmVars.PREF_SRM_TOKEN, "");
        startActivity(new android.content.Intent(this, SrmLoginActivity.class));
        finish();
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
