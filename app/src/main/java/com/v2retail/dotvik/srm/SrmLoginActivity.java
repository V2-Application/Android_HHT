package com.v2retail.dotvik.srm;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.srm.api.SrmApiClient;
import org.json.JSONObject;

public class SrmLoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Spinner  spinnerRole;
    private Button   btnLogin;
    private ProgressBar progress;
    private TextView tvError;

    private final String[] ROLES       = {"vendor","admin","subdiv","divhead","finance","pocomm","mdm"};
    private final String[] ROLE_LABELS = {"Vendor","Admin","Sub-Division Head","Division Head","Finance Dept","PO Committee","MDM Team"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_login);

        etUsername  = findViewById(R.id.etSrmUsername);
        etPassword  = findViewById(R.id.etSrmPassword);
        spinnerRole = findViewById(R.id.spinnerSrmRole);
        btnLogin    = findViewById(R.id.btnSrmLogin);
        progress    = findViewById(R.id.progressSrmLogin);
        tvError     = findViewById(R.id.tvSrmLoginError);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ROLE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Demo credential quick-fill on role change
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                String key = ROLES[pos];
                etUsername.setText(key);
                String pass = key.equals("vendor") ? "vendor123"
                        : key.equals("admin") ? "admin123"
                        : key.equals("subdiv") ? "sub123"
                        : key.equals("divhead") ? "div123"
                        : key.equals("finance") ? "fin123"
                        : key.equals("pocomm") ? "po123"
                        : "mdm123";
                etPassword.setText(pass);
            }
            @Override public void onNothingSelected(AdapterView<?> a) {}
        });

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            tvError.setText("Enter username and password");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        tvError.setVisibility(View.GONE);
        btnLogin.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);

            SrmApiClient.post(this, "/auth/login", body, response -> {
                progress.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                try {
                    JSONObject data = response.getJSONObject("data");
                    String token    = data.getString("accessToken");
                    JSONObject user = data.getJSONObject("user");
                    String role     = user.getString("role");
                    SrmApiClient.saveSession(this, token, user.toString(), role);
                    routeByRole(role);
                } catch (Exception e) {
                    showError("Unexpected response format");
                }
            }, error -> {
                progress.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                showError(SrmApiClient.parseError(error));
            });
        } catch (Exception e) {
            showError("Error building request");
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void routeByRole(String role) {
        Intent i;
        switch (role) {
            case "vendor":  i = new Intent(this, SrmVendorDashboardActivity.class);  break;
            case "subdiv":
            case "divhead":
            case "finance":
            case "pocomm":  i = new Intent(this, SrmApproverDashboardActivity.class); break;
            case "mdm":     i = new Intent(this, SrmMdmDashboardActivity.class);      break;
            default:        i = new Intent(this, SrmAdminDashboardActivity.class);    break;
        }
        startActivity(i);
        finish();
    }
}
