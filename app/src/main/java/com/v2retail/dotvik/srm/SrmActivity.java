package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.srm.api.SrmApiClient;

/**
 * SRM entry point. Checks saved token & role → routes to correct activity.
 * Called from the existing HHT app when user taps "Vendor Registration".
 */
public class SrmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String token = SrmApiClient.getToken(this);
        String role  = SrmApiClient.getSavedRole(this);

        if (token == null || token.isEmpty()) {
            startActivity(new Intent(this, SrmLoginActivity.class));
        } else {
            routeByRole(role);
        }
        finish();
    }

    void routeByRole(String role) {
        Intent i;
        switch (role) {
            case "vendor":  i = new Intent(this, SrmVendorDashboardActivity.class);  break;
            case "subdiv":
            case "divhead":
            case "finance":
            case "pocomm":  i = new Intent(this, SrmApproverDashboardActivity.class); break;
            case "mdm":     i = new Intent(this, SrmMdmDashboardActivity.class);      break;
            case "admin":   i = new Intent(this, SrmAdminDashboardActivity.class);    break;
            default:        i = new Intent(this, SrmLoginActivity.class);             break;
        }
        startActivity(i);
    }
}
