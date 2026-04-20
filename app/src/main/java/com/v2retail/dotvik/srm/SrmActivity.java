package com.v2retail.dotvik.srm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONObject;

/**
 * SRM Entry Point — reads stored SRM token, validates it,
 * then routes to role-specific dashboard.
 */
public class SrmActivity extends AppCompatActivity {

    private static final String TAG = "SrmActivity";
    private SharedPreferencesData prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new SharedPreferencesData(getApplicationContext());

        String token = prefs.read(SrmVars.PREF_SRM_TOKEN);
        String role  = prefs.read(SrmVars.PREF_SRM_USER_ROLE);

        if (token != null && !token.isEmpty() && role != null && !role.isEmpty()) {
            // Token exists — verify it then route
            verifyAndRoute(token, role);
        } else {
            // No token — go to SRM login
            goToLogin();
        }
    }

    private void verifyAndRoute(String token, String role) {
        String url = getSrmBaseUrl() + SrmVars.AUTH_ME;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    String liveRole = response.getJSONObject("data").getString("role");
                    prefs.write(SrmVars.PREF_SRM_USER_ROLE, liveRole);
                    routeByRole(liveRole);
                } catch (Exception e) { goToLogin(); }
            },
            error -> goToLogin()
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    void routeByRole(String role) {
        Intent intent;
        switch (role) {
            case SrmVars.ROLE_VENDOR:
                intent = new Intent(this, SrmVendorDashboardActivity.class); break;
            case SrmVars.ROLE_MDM:
                intent = new Intent(this, SrmMdmDashboardActivity.class); break;
            case SrmVars.ROLE_ADMIN:
                intent = new Intent(this, SrmAdminDashboardActivity.class); break;
            default:
                // subdiv, divhead, finance, pocomm — all go to approver dashboard
                intent = new Intent(this, SrmApproverDashboardActivity.class); break;
        }
        startActivity(intent);
        finish();
    }

    void goToLogin() {
        startActivity(new Intent(this, SrmLoginActivity.class));
        finish();
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
