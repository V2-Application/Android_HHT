package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class SrmVendorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvTicket, tvStage, tvSapCode;
    private TextView tvStatStage, tvStatDocs;
    private CardView cardApplication, cardSapCode;
    private LinearLayout llNoApp;
    private Button btnNewReg, btnTrack, btnDocs, btnLogout;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_vendor_dashboard);

        tvWelcome    = findViewById(R.id.tvSrmWelcome);
        tvTicket     = findViewById(R.id.tvSrmTicket);
        tvStage      = findViewById(R.id.tvSrmStage);
        tvSapCode    = findViewById(R.id.tvSrmSapCode);
        tvStatStage  = findViewById(R.id.tvSrmStatStage);
        tvStatDocs   = findViewById(R.id.tvSrmStatDocs);
        cardApplication = findViewById(R.id.cardSrmApplication);
        cardSapCode  = findViewById(R.id.cardSrmSapCode);
        llNoApp      = findViewById(R.id.llSrmNoApp);
        btnNewReg    = findViewById(R.id.btnSrmNewReg);
        btnTrack     = findViewById(R.id.btnSrmTrack);
        btnDocs      = findViewById(R.id.btnSrmDocs);
        btnLogout    = findViewById(R.id.btnSrmLogout);
        progress     = findViewById(R.id.progressSrmDash);

        // Welcome name
        try {
            JSONObject user = new JSONObject(SrmApiClient.getSavedUser(this));
            tvWelcome.setText("Welcome, " + user.optString("full_name", "Vendor"));
        } catch (Exception ignored) {}

        btnNewReg.setOnClickListener(v ->
                startActivity(new Intent(this, SrmVendorRegistrationActivity.class)));
        btnTrack.setOnClickListener(v ->
                startActivity(new Intent(this, SrmTrackApplicationActivity.class)));
        btnDocs.setOnClickListener(v ->
                startActivity(new Intent(this, SrmMyDocumentsActivity.class)));
        btnLogout.setOnClickListener(v -> logout());

        loadApplications();
    }

    @Override protected void onResume() { super.onResume(); loadApplications(); }

    private void loadApplications() {
        progress.setVisibility(View.VISIBLE);
        SrmApiClient.get(this, "/applications?limit=1", response -> {
            progress.setVisibility(View.GONE);
            try {
                JSONArray data = response.getJSONArray("data");
                if (data.length() == 0) {
                    llNoApp.setVisibility(View.VISIBLE);
                    cardApplication.setVisibility(View.GONE);
                } else {
                    JSONObject app = data.getJSONObject(0);
                    llNoApp.setVisibility(View.GONE);
                    cardApplication.setVisibility(View.VISIBLE);

                    String stage = app.optString("current_stage", "SUBMITTED");
                    tvTicket.setText(app.optString("ticket_no", "—"));
                    tvStage.setText(stageLabel(stage));
                    tvStatStage.setText(stageLabel(stage));

                    String sapCode = app.optString("sap_vendor_code", "");
                    if (!sapCode.isEmpty() && !sapCode.equals("null")) {
                        cardSapCode.setVisibility(View.VISIBLE);
                        tvSapCode.setText(sapCode);
                    } else {
                        cardSapCode.setVisibility(View.GONE);
                    }
                }
            } catch (Exception ignored) {}
        }, error -> progress.setVisibility(View.GONE));
    }

    private String stageLabel(String s) {
        switch (s) {
            case "SUBMITTED": return "Submitted";
            case "L1": return "Sub Div Head";
            case "L2": return "Division Head";
            case "L3": return "Finance";
            case "L4": return "PO Committee";
            case "L5": return "MDM Team";
            case "APPROVED": return "SAP Created ✓";
            case "REJECTED": return "Rejected";
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
