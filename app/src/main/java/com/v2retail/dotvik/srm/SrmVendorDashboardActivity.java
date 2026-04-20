package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vendor Dashboard — shows active application status + quick actions
 */
public class SrmVendorDashboardActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private TextView tvWelcome, tvTicket, tvStage, tvSapCode;
    private View cardStatus, cardSapCode;
    private Button btnRegister, btnTrack, btnDocs, btnLogout;
    private View pipelineL1, pipelineL2, pipelineL3, pipelineL4, pipelineL5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_vendor_dashboard);

        prefs      = new SharedPreferencesData(getApplicationContext());
        tvWelcome  = findViewById(R.id.srm_tv_welcome);
        tvTicket   = findViewById(R.id.srm_tv_ticket);
        tvStage    = findViewById(R.id.srm_tv_stage);
        tvSapCode  = findViewById(R.id.srm_tv_sap_code);
        cardStatus = findViewById(R.id.srm_card_status);
        cardSapCode= findViewById(R.id.srm_card_sap_code);
        btnRegister= findViewById(R.id.srm_btn_register);
        btnTrack   = findViewById(R.id.srm_btn_track);
        btnDocs    = findViewById(R.id.srm_btn_docs);
        btnLogout  = findViewById(R.id.srm_btn_logout);

        String name = prefs.read(SrmVars.PREF_SRM_USER_NAME);
        tvWelcome.setText("Welcome, " + (name != null ? name : "Vendor"));

        btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, SrmVendorRegistrationActivity.class)));
        btnTrack.setOnClickListener(v ->
            startActivity(new Intent(this, SrmTrackApplicationActivity.class)));
        btnDocs.setOnClickListener(v ->
            startActivity(new Intent(this, SrmMyDocumentsActivity.class)));
        btnLogout.setOnClickListener(v -> doLogout());

        loadApplications();
    }

    @Override
    protected void onResume() { super.onResume(); loadApplications(); }

    private void loadApplications() {
        String url = getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST + "?limit=1";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray data = response.getJSONArray("data");
                    if (data.length() > 0) {
                        JSONObject app = data.getJSONObject(0);
                        String ticket = app.optString("ticket_no", "—");
                        String stage  = app.optString("current_stage", "");
                        String sap    = app.optString("sap_vendor_code", "");

                        cardStatus.setVisibility(View.VISIBLE);
                        tvTicket.setText(ticket);
                        tvStage.setText(SrmVars.stageName(stage));

                        if (!sap.isEmpty()) {
                            cardSapCode.setVisibility(View.VISIBLE);
                            tvSapCode.setText(sap);
                        } else {
                            cardSapCode.setVisibility(View.GONE);
                        }
                        updatePipeline(stage);
                    } else {
                        cardStatus.setVisibility(View.GONE);
                        cardSapCode.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {}
            },
            error -> {}
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + prefs.read(SrmVars.PREF_SRM_TOKEN));
                return h;
            }
        };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void updatePipeline(String currentStage) {
        int idx = SrmVars.stageIndex(currentStage);
        int[] dotIds = { R.id.dot_l1, R.id.dot_l2, R.id.dot_l3, R.id.dot_l4, R.id.dot_l5 };
        for (int i = 0; i < dotIds.length; i++) {
            View dot = findViewById(dotIds[i]);
            if (dot == null) continue;
            if (i < idx - 1)      dot.setBackgroundResource(R.drawable.srm_dot_done);
            else if (i == idx - 1) dot.setBackgroundResource(R.drawable.srm_dot_active);
            else                   dot.setBackgroundResource(R.drawable.srm_dot_pending);
        }
    }

    private void doLogout() {
        prefs.write(SrmVars.PREF_SRM_TOKEN, "");
        prefs.write(SrmVars.PREF_SRM_REFRESH, "");
        prefs.write(SrmVars.PREF_SRM_USER_ROLE, "");
        startActivity(new Intent(this, SrmLoginActivity.class));
        finish();
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
