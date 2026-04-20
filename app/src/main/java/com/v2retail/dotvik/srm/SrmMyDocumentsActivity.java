package com.v2retail.dotvik.srm;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** My Documents — show upload status for all 4 mandatory docs */
public class SrmMyDocumentsActivity extends AppCompatActivity {

    private SharedPreferencesData prefs;
    private LinearLayout llDocs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_my_documents);
        prefs  = new SharedPreferencesData(getApplicationContext());
        llDocs = findViewById(R.id.srm_ll_docs);
        loadApplication();
    }

    private void loadApplication() {
        String url = getSrmBaseUrl() + SrmVars.APPLICATIONS_LIST + "?limit=1";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray data = response.getJSONArray("data");
                    if (data.length() > 0) {
                        String appId = data.getJSONObject(0).getString("id");
                        loadDocs(appId);
                    }
                } catch (Exception ignored) {}
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { Map<String,String> h=new HashMap<>(); h.put("Authorization","Bearer "+prefs.read(SrmVars.PREF_SRM_TOKEN)); return h; } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void loadDocs(String appId) {
        String url = getSrmBaseUrl() + String.format(SrmVars.DOCUMENTS_LIST, appId);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray docs = response.getJSONArray("data");
                    if (llDocs == null) return;
                    llDocs.removeAllViews();
                    String[][] docTypes = {
                        {"PAN_CARD","PAN Card"}, {"CANCELLED_CHEQUE","Cancelled Cheque"},
                        {"GST_CERTIFICATE","GST Certificate"}, {"BILL_COPY","Bill Copy"},
                        {"MSME","MSME Certificate"}, {"TRADE_LICENCE","Trade Licence"}
                    };
                    for (String[] dt : docTypes) {
                        boolean found = false;
                        String origName = "";
                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject doc = docs.getJSONObject(i);
                            if (dt[0].equals(doc.optString("doc_type"))) {
                                found = true;
                                origName = doc.optString("original_name","");
                                break;
                            }
                        }
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 12, 0, 12);
                        row.setBackgroundColor(found ? 0xFFEEF6EC : 0xFFFEECEC);
                        android.view.View sep = new android.view.View(this);
                        sep.setLayoutParams(new LinearLayout.LayoutParams(4, LinearLayout.LayoutParams.MATCH_PARENT));
                        sep.setBackgroundColor(found ? 0xFF059669 : 0xFFDC2626);
                        row.addView(sep);
                        LinearLayout info = new LinearLayout(this);
                        info.setOrientation(LinearLayout.VERTICAL);
                        info.setPadding(16, 0, 0, 0);
                        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        TextView tvName = new TextView(this); tvName.setText(dt[1]); tvName.setTextColor(0xFF1A1A1A); tvName.setTextSize(13f); tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                        TextView tvStatus = new TextView(this); tvStatus.setText(found ? ("Uploaded: " + origName) : "Not uploaded"); tvStatus.setTextColor(found ? 0xFF059669 : 0xFFDC2626); tvStatus.setTextSize(11f);
                        info.addView(tvName); info.addView(tvStatus);
                        row.addView(info);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 8);
                        row.setLayoutParams(lp);
                        row.setPadding(12, 12, 12, 12);
                        llDocs.addView(row);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }, error -> {}
        ) { @Override public Map<String,String> getHeaders() { Map<String,String> h=new HashMap<>(); h.put("Authorization","Bearer "+prefs.read(SrmVars.PREF_SRM_TOKEN)); return h; } };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    String getSrmBaseUrl() { String u=prefs.read(SrmVars.PREF_SRM_URL); return (u!=null&&!u.isEmpty())?u:"http://192.168.151.49:5000"; }
}
