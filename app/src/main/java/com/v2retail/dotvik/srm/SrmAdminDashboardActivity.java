package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.*;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Admin Dashboard — full pipeline overview + user management */
public class SrmAdminDashboardActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private TextView tvTotal, tvPipeline, tvCreated, tvRejected, tvL1, tvL2, tvL3, tvL4, tvL5;
    private RecyclerView rvApps;
    private Button btnLogout;
    private List<JSONObject> appList = new ArrayList<>();
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_admin_dashboard);
        prefs      = new SharedPreferencesData(getApplicationContext());
        tvTotal    = findViewById(R.id.srm_tv_total);
        tvPipeline = findViewById(R.id.srm_tv_pipeline);
        tvCreated  = findViewById(R.id.srm_tv_created);
        tvRejected = findViewById(R.id.srm_tv_rejected);
        tvL1       = findViewById(R.id.srm_tv_l1);
        tvL2       = findViewById(R.id.srm_tv_l2);
        tvL3       = findViewById(R.id.srm_tv_l3);
        tvL4       = findViewById(R.id.srm_tv_l4);
        tvL5       = findViewById(R.id.srm_tv_l5);
        rvApps     = findViewById(R.id.srm_rv_apps);
        btnLogout  = findViewById(R.id.srm_btn_logout);

        adapter = new AppAdapter();
        if (rvApps != null) { rvApps.setLayoutManager(new LinearLayoutManager(this)); rvApps.setAdapter(adapter); }
        if (btnLogout != null) btnLogout.setOnClickListener(v -> { prefs.write(SrmVars.PREF_SRM_TOKEN,""); startActivity(new Intent(this,SrmLoginActivity.class)); finish(); });

        loadStats();
        loadApps();
    }

    @Override protected void onResume() { super.onResume(); loadStats(); loadApps(); }

    private void loadStats() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + SrmVars.APPLICATIONS_STATS, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    set(tvTotal,    d.optInt("total",0));
                    set(tvPipeline, d.optInt("inPipeline",0));
                    set(tvCreated,  d.optInt("approved",0));
                    set(tvRejected, d.optInt("rejected",0));
                    set(tvL1, d.optInt("l1",0)); set(tvL2, d.optInt("l2",0));
                    set(tvL3, d.optInt("l3",0)); set(tvL4, d.optInt("l4",0));
                    set(tvL5, d.optInt("l5",0));
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return auth(); } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void loadApps() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
            getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST + "?limit=30", null,
            response -> {
                try {
                    JSONArray arr = response.getJSONArray("data");
                    appList.clear();
                    for (int i=0;i<arr.length();i++) appList.add(arr.getJSONObject(i));
                    adapter.notifyDataSetChanged();
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { return auth(); } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void set(TextView tv, int val) { if (tv != null) tv.setText(String.valueOf(val)); }

    class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvTicket, tvType, tvDate;
            Button btnReview;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.srm_item_vendor_name);
                tvTicket = v.findViewById(R.id.srm_item_ticket);
                tvType   = v.findViewById(R.id.srm_item_type);
                tvDate   = v.findViewById(R.id.srm_item_date);
                btnReview= v.findViewById(R.id.srm_item_btn_review);
            }
        }
        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_srm_application,p,false));
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            try {
                JSONObject app = appList.get(pos);
                h.tvName.setText(app.optString("vendor_name",""));
                h.tvTicket.setText(app.optString("ticket_no","") + " · " + SrmVars.stageName(app.optString("current_stage","")));
                h.tvType.setText(app.optString("vendor_type",""));
                String dt = app.optString("submitted_at","");
                h.tvDate.setText(dt.length()>=10?dt.substring(0,10):dt);
                h.btnReview.setOnClickListener(v -> {
                    try {
                        String stage = app.optString("current_stage","");
                        Intent intent;
                        if (SrmVars.STAGE_L5.equals(stage)) {
                            intent = new Intent(SrmAdminDashboardActivity.this, SrmMdmSapPushActivity.class);
                        } else {
                            intent = new Intent(SrmAdminDashboardActivity.this, SrmApprovalDetailActivity.class);
                        }
                        intent.putExtra("app_id", app.getString("id"));
                        intent.putExtra("ticket_no", app.optString("ticket_no",""));
                        startActivity(intent);
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (Exception ignored) {}
        }
        @Override public int getItemCount() { return appList.size(); }
    }

    private Map<String,String> auth() { Map<String,String> h=new HashMap<>(); h.put("Authorization","Bearer "+prefs.read(SrmVars.PREF_SRM_TOKEN)); return h; }
    String getSrmBaseUrl() { String u=prefs.read(SrmVars.PREF_SRM_URL); return (u!=null&&!u.isEmpty())?u:"http://192.168.151.49:5000"; }
}
