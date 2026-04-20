package com.v2retail.dotvik.srm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONObject;

/**
 * SRM Login Screen
 * POST /api/auth/login → stores JWT tokens → routes by role
 */
public class SrmLoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button   btnLogin;
    private TextView tvError, tvVersion;
    private CheckBox cbRemember;
    private ProgressDialog progress;
    private SharedPreferencesData prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_srm_login);

        prefs      = new SharedPreferencesData(getApplicationContext());
        etUsername = findViewById(R.id.srm_et_username);
        etPassword = findViewById(R.id.srm_et_password);
        btnLogin   = findViewById(R.id.srm_btn_login);
        tvError    = findViewById(R.id.srm_tv_error);
        cbRemember = findViewById(R.id.srm_cb_remember);

        // Pre-fill if remembered
        String saved = prefs.read(SrmVars.PREF_SRM_USERNAME);
        if (saved != null && !saved.isEmpty()) {
            etUsername.setText(saved);
            cbRemember.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(user)) { etUsername.setError("Required"); return; }
        if (TextUtils.isEmpty(pass)) { etPassword.setError("Required"); return; }

        progress = new ProgressDialog(this);
        progress.setMessage("Signing in…");
        progress.setCancelable(false);
        progress.show();

        try {
            JSONObject body = new JSONObject();
            body.put("username", user);
            body.put("password", pass);

            String url = getSrmBaseUrl() + SrmVars.AUTH_LOGIN;
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONObject userObj = data.getJSONObject("user");
                            String token   = data.getString("accessToken");
                            String refresh = data.getString("refreshToken");
                            String role    = userObj.getString("role");
                            String name    = userObj.optString("fullName", user);
                            String dept    = userObj.optString("department", "");

                            prefs.write(SrmVars.PREF_SRM_TOKEN,     token);
                            prefs.write(SrmVars.PREF_SRM_REFRESH,   refresh);
                            prefs.write(SrmVars.PREF_SRM_USER_ROLE, role);
                            prefs.write(SrmVars.PREF_SRM_USER_NAME, name);
                            prefs.write(SrmVars.PREF_SRM_USER_DEPT, dept);
                            prefs.write(SrmVars.PREF_SRM_USER_ID,   userObj.getString("id"));
                            if (cbRemember.isChecked()) {
                                prefs.write(SrmVars.PREF_SRM_USERNAME, user);
                            } else {
                                prefs.write(SrmVars.PREF_SRM_USERNAME, "");
                            }

                            new SrmActivity().routeByRole(SrmLoginActivity.this, role);
                            finish();
                        } else {
                            showError(response.optString("message", "Login failed"));
                        }
                    } catch (Exception e) { showError(e.getMessage()); }
                },
                error -> {
                    dismiss();
                    String msg = "Connection error";
                    if (error.networkResponse != null) {
                        try {
                            JSONObject err = new JSONObject(new String(error.networkResponse.data));
                            msg = err.optString("message", msg);
                        } catch (Exception ignored) {}
                    }
                    showError(msg);
                }
            ) {
                @Override public String getBodyContentType() { return "application/json"; }
                @Override public byte[] getBody() { return body.toString().getBytes(); }
            };
            req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(15000, 1,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            ApplicationController.getInstance().getRequestQueue().add(req);
        } catch (Exception e) {
            dismiss();
            showError(e.getMessage());
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void dismiss() {
        if (progress != null && progress.isShowing()) { progress.dismiss(); progress = null; }
    }

    String getSrmBaseUrl() {
        String url = prefs.read(SrmVars.PREF_SRM_URL);
        return (url != null && !url.isEmpty()) ? url : "http://192.168.151.49:5000";
    }
}
