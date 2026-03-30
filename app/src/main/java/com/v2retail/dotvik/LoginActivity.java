package com.v2retail.dotvik;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.ecomm.Ecomm_Process_Selection;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


/**
 * A login screen that offers login via userName/password.
 */
public class LoginActivity extends AppCompatActivity {

    private RequestQueue mRequestQueue;
    private JsonObjectRequest mJsonObjectRequest;
    private static final int REQUEST_LOGIN = 101;

    ProgressDialog dialog;
    private static final String TAG = LoginActivity.class.getName();
    Context con;
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    //  private UserLoginTask mAuthTask = null;

    // UI references.
    private RadioGroup locGrp;
    String loc;
    private EditText mUserName;
    private EditText mPasswordView;
    private TextView mResponseView;
    private View mProgressView;
    private View mLoginFormView;
    private String loc_name;
    AlertBox box;
    String URL = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(findViewById(R.id.ver)!=null) {
            ((TextView)findViewById(R.id.ver)).setText(BuildConfig.VERSION_NAME);
        }

        Log.d(TAG, TAG + " created");

        con = getApplicationContext();

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
       // String defLoc=data.read("LOC");
        Log.d(TAG,  "URL -> "+URL);

        box = new AlertBox(LoginActivity.this);
        dialog = new ProgressDialog(LoginActivity.this);

        // Set up the login form.

        locGrp=(RadioGroup)findViewById(R.id.loc);
        mUserName = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mResponseView = (TextView) findViewById(R.id.response);

        String username = data.read("USERNAME");

        if(username!=null && username.length()>0) {
            mUserName.setText(username);
        }

        String password = data.read("PASSWORD");
        if(password!=null && password.length()>0) {
            mPasswordView.setText(password);
        }

        mUserName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    mPasswordView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLoginButton = (Button) findViewById(R.id.email_sign_in_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
                try {
                    attemptLogin();
                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
       /* if(defLoc!=null  )
        switch (defLoc)
        {
            case "Store":
                 locGrp.check(R.id.store);
                break;
            case "DC":
            default:
                locGrp.check(R.id.dc);
                break;
        }*/
        mUserName.requestFocus();



    }




    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid userName, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
       /* if (mAuthTask != null) {
            return;
        }*/

        // Reset errors.
        mUserName.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userName = mUserName.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid userName address.
        if (TextUtils.isEmpty(userName)) {
            mUserName.setError(getString(R.string.error_field_required));
            focusView = mUserName;
            cancel = true;
        } else if (!isUserNameValid(userName)) {
            mUserName.setError(getString(R.string.error_invalid_email));
            focusView = mUserName;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            /*showProgress(true);
            mAuthTask = new UserLoginTask(userName, password);
            mAuthTask.execute((Void) null);*/
            try {

                login();
            } catch (Exception e) {
                box.getErrBox(e);
            }
        }
    }

    private boolean isUserNameValid(String userName) {
        //TODO: Replace this with your own logic
        if (!userName.equals(""))
            return true;
        else
            return false;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        if (!password.equals(""))
            return true;
        else
            return false;
    }

    /**
     * Shows the progress UI and hides the login form.
     */



    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */

    void clear() {

        mPasswordView.setText("");
        mUserName.setText("");
    }


    @Override
    protected void onPause() {
        super.onPause();
        clear();
    }

    public void onClear(View view) {
        clear();

    }
    void login() {

        InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);

        String user = mUserName.getText().toString().trim();
        String pass = mPasswordView.getText().toString().trim();

        // Old middleware (xmwgw) uses hash-delimited plain text format
        // New middleware (Azure) uses JSON
        if (URL != null && URL.contains("xmwgw")) {
            loginLegacy(user, pass);
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_USER_AUTHORITY_CHECK);
            args.put("IM_USERID", user);
            args.put("IM_PASSWORD", pass);
            showProcessingAndSubmit(Vars.ZWM_USER_AUTHORITY_CHECK, REQUEST_LOGIN, args);
        }catch (Exception e)
        {   dialog.dismiss();
            box.getErrBox(e);
        }
    }

    // Legacy login for old on-prem middleware (xmwgw)
    // Request: "scnrec#user#pass#<eol>" plain text
    // Response: "1#WERKS" (success) or "0" (failure)
    private void loginLegacy(String user, String pass) {
        dialog = new ProgressDialog(LoginActivity.this);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String body = "scnrec#" + user + "#" + pass + "#<eol>";
        Log.d(TAG, "loginLegacy body -> " + body);

        com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.POST, URL,
            new com.android.volley.Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (dialog != null) { dialog.dismiss(); dialog = null; }
                    Log.d(TAG, "loginLegacy response -> " + response);
                    handleLegacyLoginResponse(response, user, pass);
                }
            },
            volleyErrorListener()
        ) {
            @Override
            public byte[] getBody() { return body.getBytes(); }

            @Override
            public String getBodyContentType() { return "text/plain"; }
        };

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(50000, 1,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    // Parse old middleware response: "Response:1#WERKS" or "1#WERKS" = success
    private void handleLegacyLoginResponse(String response, String user, String pass) {
        if (response == null || response.trim().isEmpty()) {
            box.getBox("Err", "Empty response from server");
            return;
        }
        // Strip "Response:" prefix if present (old ValueXMW middleware format)
        String raw = response.trim();
        if (raw.startsWith("Response:")) raw = raw.substring("Response:".length());
        String[] parts = raw.split("#");
        if (parts.length >= 1 && parts[0].trim().equals("1")) {
            // Success — parts[1] is WERKS (plant/store code)
            String werks = parts.length >= 2 ? parts[1].trim() : "";
            SharedPreferencesData data = new SharedPreferencesData(getApplicationContext());
            data.write("USER", user.toUpperCase());
            data.write("WERKS", werks);
            data.write("USERNAME", user);
            data.write("PASSWORD", pass);
            // Now call ZWM_USER_AUTHORITY_CHECK to get EX_GROUP so we can route correctly
            // (same as Azure path — store users go to Home_Activity, DC to Process_Selection)
            fetchUserGroupAndRoute(user, pass, werks, data);
        } else {
            box.getBox("Err", "Login failed — invalid username or password");
        }
    }

    /**
     * After legacy login succeeds, call ZWM_USER_AUTHORITY_CHECK via the old middleware
     * to get EX_GROUP, then route the user to the correct screen (DC / Store / Hub / Ecomm).
     * The old middleware's noacljsonrfcadaptor endpoint handles this RFC call.
     */
    private void fetchUserGroupAndRoute(String user, String pass,
                                        String werks, SharedPreferencesData data) {
        // Build RFC URL from login URL:
        // e.g. "http://server:9080/xmwgw" → "http://server:9080/noacljsonrfcadaptor?bapiname=ZWM_USER_AUTHORITY_CHECK"
        String base = URL.contains("/") ? URL.substring(0, URL.lastIndexOf("/")) : URL;
        String rfcUrl = base + "/noacljsonrfcadaptor?bapiname=ZWM_USER_AUTHORITY_CHECK&aclclientid=android";

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_USER_AUTHORITY_CHECK);
            args.put("IM_USERID", user.toUpperCase());
            args.put("IM_PASSWORD", pass);
        } catch (JSONException e) {
            e.printStackTrace();
            // Fallback: route to DC if authority check fails to build
            routeUser("DC", werks, data);
            return;
        }

        final JSONObject params = args;
        com.android.volley.toolbox.JsonObjectRequest req =
            new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.POST, rfcUrl, params,
                responsebody -> {
                    String group = "";
                    try {
                        if (responsebody != null && responsebody.has("EX_GROUP")) {
                            group = responsebody.optString("EX_GROUP", "");
                        }
                        // Update WERKS from authority check if returned
                        if (responsebody != null && responsebody.has("EX_WERKS")) {
                            String authWerks = responsebody.optString("EX_WERKS", "");
                            if (!authWerks.isEmpty()) {
                                data.write("WERKS", authWerks);
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    routeUser(group, data.read("WERKS"), data);
                },
                error -> {
                    Log.w(TAG, "Authority check failed for legacy login — routing by WERKS prefix");
                    // Fallback: route by WERKS prefix (DC plant codes known patterns)
                    routeUser(guessDCOrStore(werks), werks, data);
                }
            ) {
                @Override public String getBodyContentType() { return "application/json"; }
                @Override public byte[] getBody() { return params.toString().getBytes(); }
            };

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(30000, 1,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    /**
     * Route to the correct Activity based on EX_GROUP (same logic as Azure path).
     */
    private void routeUser(String group, String werks, SharedPreferencesData data) {
        data.write("LOC", isDC_user(group) ? "DC" : "Store");
        if (isDC_user(group)) {
            startActivity(new android.content.Intent(LoginActivity.this,
                com.v2retail.dotvik.dc.Process_Selection_Activity.class));
        } else if (isEcomm_user(werks)) {
            startActivity(new android.content.Intent(LoginActivity.this,
                com.v2retail.dotvik.ecomm.Ecomm_Process_Selection.class));
        } else if (isHub_user(group)) {
            startActivity(new android.content.Intent(LoginActivity.this,
                com.v2retail.dotvik.hub.HubProcessSelectionActivity.class));
        } else {
            startActivity(new android.content.Intent(LoginActivity.this,
                com.v2retail.dotvik.store.Home_Activity.class));
        }
        moveTaskToBack(true);
        clear();
    }

    /**
     * Fallback: guess DC vs Store from WERKS prefix when authority RFC is unreachable.
     * DC plant codes at V2 Retail are "DC", Store codes are everything else.
     */
    private String guessDCOrStore(String werks) {
        if (werks == null || werks.isEmpty()) return "";
        // Known DC prefixes (distribution centres): HN4x, HD3x, DW0x etc.
        // This is a best-effort fallback — the RFC check above is the preferred path.
        // Return "DC" for nothing recognised so legacy behaves as before.
        return "DC";
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){

        dialog = new ProgressDialog(LoginActivity.this);

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL; // Use ValueXMW directly — works with Azure middleware

        final JSONObject params = args;

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

                if (responsebody == null) {
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null) {
                                    if (type.equals("E")) {
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    }
                                    else{
                                        if (request == REQUEST_LOGIN) {
                                            moveTaskToBack(true);
                                            String group = responsebody.getString("EX_GROUP");
                                            String werks = responsebody.getString("EX_WERKS");
                                            int radioButtonID = locGrp.getCheckedRadioButtonId();
                                            RadioButton radioButton = locGrp.findViewById(radioButtonID);
                                            String loc = (String) radioButton.getText();
                                            SharedPreferencesData data = new SharedPreferencesData(con);
                                            data.write("USER", mUserName.getText().toString().trim().toUpperCase());
                                            data.write("LOC",loc);
                                            data.write("WERKS", werks);
                                            data.write("USERNAME", mUserName.getText().toString().trim());
                                            data.write("PASSWORD", mPasswordView.getText().toString().trim());
                                            if(isDC_user(group)) {
                                                startActivity(new Intent(LoginActivity.this, Process_Selection_Activity.class));
                                            } else if(isEcomm_user(werks)) {
                                                startActivity(new Intent(LoginActivity.this, Ecomm_Process_Selection.class));
                                            } else {
                                                Intent intent = null;
                                                if(isHub_user(group)){
                                                    intent = new Intent(LoginActivity.this, HubProcessSelectionActivity.class);
                                                }else{
                                                    intent = new Intent(LoginActivity.this, Home_Activity.class);
                                                }
                                                startActivity(intent);
                                            }
                                            clear();
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        box.getErrBox(e);
                    }
                }
            }
        }, volleyErrorListener()) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return params.toString().getBytes();
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {

                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res.toString());

                return res;
            }
        };
        mJsonRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 1;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mJsonRequest);

        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getErrBox(authFailureError);
        }
    }

    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                String err = "";

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    err = "Communication Error!";

                } else if (error instanceof AuthFailureError) {
                    err = "Authentication Error!";
                } else if (error instanceof ServerError) {
                    err = "Server Side Error!";
                } else if (error instanceof NetworkError) {
                    err = "Network Error!";
                } else if (error instanceof ParseError) {
                    err = "Parse Error!";
                } else err = error.toString();

                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }
                box.getBox("Err", err);
            }
        };
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();


    }


    boolean isDC_user(String group) {
        boolean retVal = false;
        if(group!=null && group.equalsIgnoreCase("DC")) {
            return true;
        }
        return retVal;
    }

    boolean isEcomm_user(String myWerks) {
        if(myWerks!=null && myWerks.equalsIgnoreCase("DH25")) {
            return true;
        }
        return false;
    }

    boolean isHub_user(String hub) {
        if(hub!=null && hub.equalsIgnoreCase("HUB")) {
            return true;
        }
        return false;
    }
}

