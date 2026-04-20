package com.v2retail.dotvik.srm;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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

/** Track Application — vendor enters ticket number and sees live pipeline status */
public class SrmTrackApplicationActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private EditText etTicket;
    private Button btnSearch;
    private TextView tvVendorName, tvStage, tvSubmitted, tvError;
    private RecyclerView rvTrail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_track);
        prefs      = new SharedPreferencesData(getApplicationContext());
        etTicket   = findViewById(R.id.srm_et_ticket);
        btnSearch  = findViewById(R.id.srm_btn_search);
        tvVendorName = findViewById(R.id.srm_tv_vendor_name);
        tvStage    = findViewById(R.id.srm_tv_stage);
        tvSubmitted= findViewById(R.id.srm_tv_submitted);
        tvError    = findViewById(R.id.srm_tv_error);
        if (tvError != null) tvError.setVisibility(android.view.View.GONE);
        if (btnSearch != null) btnSearch.setOnClickListener(v -> search());
    }

    private void search() {
        String ticket = etTicket != null ? etTicket.getText().toString().trim() : "";
        if (ticket.isEmpty()) return;
        String url = getSrmBaseUrl() + "/api/applications/" + ticket;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONObject d = response.getJSONObject("data");
                    if (tvVendorName != null) tvVendorName.setText(d.optString("vendor_name", "—"));
                    if (tvStage != null) tvStage.setText(SrmVars.stageName(d.optString("current_stage", "")));
                    String dt = d.optString("submitted_at", "");
                    if (tvSubmitted != null) tvSubmitted.setText(dt.length() >= 10 ? dt.substring(0,10) : dt);
                } catch (Exception e) { if (tvError != null) { tvError.setVisibility(android.view.View.VISIBLE); tvError.setText("Not found"); } }
            },
            error -> { if (tvError != null) { tvError.setVisibility(android.view.View.VISIBLE); tvError.setText("Not found"); } }
        ) { @Override public Map<String,String> getHeaders() { Map<String,String> h=new HashMap<>(); h.put("Authorization","Bearer "+prefs.read(SrmVars.PREF_SRM_TOKEN)); return h; } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    String getSrmBaseUrl() { String u=prefs.read(SrmVars.PREF_SRM_URL); return (u!=null&&!u.isEmpty())?u:"http://192.168.151.49:5000"; }
}
