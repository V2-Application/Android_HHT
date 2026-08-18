package com.v2retail.dotvik;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.Vars;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.v2retail.ApplicationController;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IPActivity extends AppCompatActivity implements View.OnClickListener {

    //  RadioGroup portgrp;
    // RadioButton radioButton;
    Spinner addressSpinner;
    Button connect;
    Button exit;
    static String IpAdress;
    static String port;
    static String URL;
    static String Code;

    AlertBox box;
    ProgressDialog dialog;

    private static final String TAG = IPActivity.class.getName();

    private static final String APP_NAME_ANDROID_HHT = "ANDROID_HHT";
    /** Matches Gradle output: {@code V2_HHT_Azure_12_133.apk} */
    private static final String UPDATE_APK_PREFIX = "V2_HHT_Azure_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip);

        // Sentry.captureMessage("testing SDK setup");

        if(findViewById(R.id.ver)!=null) {
            ((TextView)findViewById(R.id.ver)).setText(BuildConfig.VERSION_NAME);
        }

        box = new AlertBox(IPActivity.this);
        dialog = new ProgressDialog(IPActivity.this);
        // portgrp=(RadioGroup)findViewById(R.id.portgrp);
        addressSpinner=(Spinner)findViewById(R.id.ip_spinner);

        int serverIndex = 0;
        SharedPreferencesData data = new SharedPreferencesData(IPActivity.this);
        String server = data.read("SERVER");
        if(server!=null && server.length()>0) {
            serverIndex = Integer.parseInt(server);
        }
        int urlcount = addressSpinner.getAdapter().getCount();
        if(serverIndex < urlcount){
            addressSpinner.setSelection(serverIndex);
        }
        addressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Log.d(TAG, "selected item is " + position);
                SharedPreferencesData data = new SharedPreferencesData(IPActivity.this);
                data.write("SERVER",  "" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });



        connect = (Button) findViewById(R.id.connect);
        exit = (Button) findViewById(R.id.exit);

        connect.setOnClickListener(this);
        exit.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.connect:

                String Ip= addressSpinner.getSelectedItem().toString();
                String iparr[]=Ip.split(" ");
                Log.d(TAG,"IP-> "+iparr[0].trim());
                URL=iparr[0].trim();
                if("https://app.v2axasync-prd.v2rtl.com:8443/xmwgw".equalsIgnoreCase(URL)){
                    URL = URL.replace("https:","http:").replace(":8443",":8080");
                    iparr[0] = URL;
                }
                Log.d(TAG,"URL -> "+URL);
                // Try update RFC dynamically for every cloud env.
                // Popup only if RFC returns valid EV_APP_* fields; otherwise connect normally.
                // Legacy xmwgw: skip and ping connectivity.
                if (URL.contains("xmwgw")) {
                    try {
                        checkIP(URL + "/index.jsp");
                    } catch (Exception e) {
                        box.getErrBox(e);
                    }
                } else {
                    getAppUpdate(iparr);
                }
                break;
            case R.id.exit:
                this.finish();
                break;
        }
    }


    private File publicUpdateApkFile(String apkName) {
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                apkName);
    }

    /**
     * Build download filename from server version, e.g. EV_APP_VERSION v12.134 → V2_HHT_Azure_12_134.apk
     */
    private static String buildUpdateApkName(String serverVersion) {
        String v = normalizeAppVersion(serverVersion);
        if (v.isEmpty()) {
            v = normalizeAppVersion(BuildConfig.VERSION_NAME);
        }
        if (v.isEmpty()) {
            return UPDATE_APK_PREFIX + "Update.apk";
        }
        return UPDATE_APK_PREFIX + v.replace('.', '_') + ".apk";
    }

    /**
     * True when the downloaded APK is signed with a different key than the
     * currently installed app (debug vs release). Android will refuse the install.
     */
    @SuppressWarnings("deprecation")
    private boolean hasSigningConflict(Context ctx, File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return false;
        }
        try {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            android.content.pm.PackageInfo installed = pm.getPackageInfo(
                    ctx.getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            android.content.pm.PackageInfo incoming = pm.getPackageArchiveInfo(
                    apkFile.getAbsolutePath(), android.content.pm.PackageManager.GET_SIGNATURES);
            if (installed == null || incoming == null
                    || installed.signatures == null || incoming.signatures == null
                    || installed.signatures.length == 0 || incoming.signatures.length == 0) {
                return false;
            }
            return !java.util.Arrays.equals(installed.signatures, incoming.signatures);
        } catch (Exception e) {
            Log.w(TAG, "Could not compare APK signatures", e);
            return false;
        }
    }

    /**
     * Launch the APK installer. If signatures do not match the installed app,
     * show uninstall-first guidance instead of the system conflict error.
     */
    private void launchInstaller(Context ctx, Uri apkUri, File apkFile, String apkName) {
        if (hasSigningConflict(ctx, apkFile)) {
            showSigningConflictDialog(ctx, apkName);
            return;
        }
        try {
            if (apkUri == null && apkFile != null && apkFile.exists()) {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    apkUri = androidx.core.content.FileProvider.getUriForFile(
                            ctx, ctx.getPackageName() + ".provider", apkFile);
                } else {
                    apkUri = Uri.fromFile(apkFile);
                }
            }
            if (apkUri == null) {
                Toast.makeText(ctx,
                        "APK saved in Downloads as " + apkName
                                + ". Open that file to install.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            android.content.Intent install = new android.content.Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(install);
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "No installer activity found", e);
            try {
                android.content.Intent fallback = new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + ctx.getPackageName()));
                fallback.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(ctx, "Please enable 'Install unknown apps' in Settings.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Install launch failed: " + e.getMessage(), e);
            showSigningConflictDialog(ctx, apkName);
        }
    }

    /**
     * Signing keys differ (usually debug build vs GitHub release). User must
     * uninstall first, then install the versioned APK from Downloads.
     */
    private void showSigningConflictDialog(Context ctx, String apkName) {
        runOnUiThread(() -> {
            new android.app.AlertDialog.Builder(ctx)
                .setTitle("Update Required — Action Needed")
                .setMessage("The new APK cannot replace this app because it was signed "
                    + "with a different key (debug vs release).\n\n"
                    + "1. Tap UNINSTALL to remove the current app.\n"
                    + "2. Open Files → Downloads → " + apkName + "\n"
                    + "3. Tap the file to install the new version.\n\n"
                    + "You will NOT lose any data.")
                .setCancelable(false)
                .setPositiveButton("UNINSTALL OLD APP", (dialog, which) -> {
                    try {
                        android.content.Intent uninstall = new android.content.Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:" + ctx.getPackageName()));
                        uninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(uninstall);
                    } catch (Exception ex) {
                        Toast.makeText(ctx,
                            "Go to Settings > Apps > V2RetailOps > Uninstall, then open Downloads/"
                                + apkName,
                            Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Open Downloads later", (dialog, which) -> {
                    Toast.makeText(ctx,
                        "APK saved in Downloads as " + apkName,
                        Toast.LENGTH_LONG).show();
                })
                .show();
        });
    }

    /**
     * Normalize versions for compare: "v12.134" and "12.134" are treated as equal.
     */
    private static String normalizeAppVersion(String version) {
        if (version == null) {
            return "";
        }
        String v = version.trim();
        if (v.length() > 1 && (v.charAt(0) == 'v' || v.charAt(0) == 'V')) {
            v = v.substring(1).trim();
        }
        return v;
    }

    /**
     * True only when the update RFC returned a usable payload for ANDROID_HHT.
     * If the RFC is missing in that env (or response is incomplete), returns false → no popup.
     */
    private static boolean isUpdateRfcPayloadAvailable(JSONObject response) {
        if (response == null) {
            return false;
        }
        // SAP / middleware error shapes — treat as RFC not available.
        if (response.has("EX_RETURN") && response.opt("EX_RETURN") instanceof JSONObject) {
            JSONObject ret = response.optJSONObject("EX_RETURN");
            if (ret != null) {
                String type = ret.optString("TYPE", ret.optString("type", "")).trim();
                if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                    return false;
                }
            }
        }
        String err = response.optString("error", response.optString("ERROR", "")).trim();
        if (err.length() > 0) {
            return false;
        }
        String msg = response.optString("message", response.optString("MESSAGE", "")).trim();
        if (msg.toLowerCase(java.util.Locale.ROOT).contains("not found")
                || msg.toLowerCase(java.util.Locale.ROOT).contains("does not exist")) {
            return false;
        }

        String appName = response.optString("EV_APP_NAME", "").trim();
        String version = response.optString("EV_APP_VERSION", "").trim();
        String url = response.optString("EV_APP_URL", "").trim();

        boolean appOk = APP_NAME_ANDROID_HHT.equalsIgnoreCase(appName);
        boolean versionOk = normalizeAppVersion(version).length() > 0;
        boolean urlOk = url.startsWith("http://") || url.startsWith("https://");
        return appOk && versionOk && urlOk;
    }

    private void proceedAfterVersionCheck(String[] iparr) {
        try {
            checkIP(iparr[0].trim() + "/index.jsp");
        } catch (Exception e) {
            box.getErrBox(e);
        }
    }

    private void showUpdateDialog(final String downloadUrl, final String serverVersion,
                                  final String[] iparr) {
        String installed = BuildConfig.VERSION_NAME;
        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("Installed: v" + normalizeAppVersion(installed)
                        + "\nAvailable: v" + normalizeAppVersion(serverVersion)
                        + "\n\nDownload and install now?")
                .setCancelable(false)
                .setPositiveButton("Update", (d, which) -> startUpdateDownload(downloadUrl, serverVersion))
                .setNegativeButton("Later", (d, which) -> proceedAfterVersionCheck(iparr))
                .show();
    }

    private void startUpdateDownload(String downloadUrl, String serverVersion) {
        final String apkName = buildUpdateApkName(serverVersion);
        Toast.makeText(IPActivity.this,
                "Downloading " + apkName + " to Downloads...",
                Toast.LENGTH_LONG).show();
        try {
            File publicDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (publicDir != null && !publicDir.exists()) {
                publicDir.mkdirs();
            }
            File existing = publicUpdateApkFile(apkName);
            if (existing.exists()) {
                existing.delete();
            }

            final android.app.DownloadManager dm =
                    (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            android.app.DownloadManager.Request req =
                    new android.app.DownloadManager.Request(Uri.parse(downloadUrl));
            req.setTitle(apkName);
            String downloadedAt = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())
                    .format(new Date());
            req.setDescription(downloadedAt);
            req.setMimeType("application/vnd.android.package-archive");
            req.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setVisibleInDownloadsUi(true);
            req.allowScanningByMediaScanner();
            req.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, apkName);
            final long dlId = dm.enqueue(req);

            android.content.BroadcastReceiver onDone =
                    new android.content.BroadcastReceiver() {
                        @Override
                        public void onReceive(Context ctx, android.content.Intent intent) {
                            long id = intent.getLongExtra(
                                    android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                            if (id != dlId) return;
                            ctx.unregisterReceiver(this);
                            handleDownloadComplete(ctx, dm, dlId, apkName);
                        }
                    };
            registerReceiver(onDone, new android.content.IntentFilter(
                    android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } catch (Exception e) {
            Log.e(TAG, "Auto-download failed", e);
            Toast.makeText(IPActivity.this,
                    "Please update manually from the download link.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleDownloadComplete(Context ctx, android.app.DownloadManager dm,
                                        long dlId, String apkName) {
        android.database.Cursor c = null;
        try {
            android.app.DownloadManager.Query query = new android.app.DownloadManager.Query();
            query.setFilterById(dlId);
            c = dm.query(query);
            if (c == null || !c.moveToFirst()) {
                Toast.makeText(ctx, "Download failed.", Toast.LENGTH_LONG).show();
                return;
            }
            int statusIdx = c.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS);
            int status = statusIdx >= 0 ? c.getInt(statusIdx) : -1;
            if (status != android.app.DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(ctx,
                        "Download failed. Check Files → Downloads.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Uri apkUri = dm.getUriForDownloadedFile(dlId);
            File apkFile = publicUpdateApkFile(apkName);
            launchInstaller(ctx, apkUri, apkFile, apkName);
        } catch (Exception e) {
            Log.e(TAG, "Download complete handling failed", e);
            Toast.makeText(ctx,
                    "APK saved in Downloads as " + apkName,
                    Toast.LENGTH_LONG).show();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Dynamically checks update via {@link Vars#ZGET_VENDOR_HU_DATA_RFC}.
     * Popup only when RFC is available and returns EV_APP_NAME / EV_APP_VERSION / EV_APP_URL
     * and the version differs from the installed app. Otherwise connect with no popup.
     */
    private void getAppUpdate(final String[] iparr) {
        final String installedVersion = BuildConfig.VERSION_NAME;
        Log.d(TAG, "Installed app version -> " + installedVersion);

        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZGET_VENDOR_HU_DATA_RFC);
            params.put("IV_APP_NAME", APP_NAME_ANDROID_HHT);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build update RFC request", e);
            proceedAfterVersionCheck(iparr);
            return;
        }

        String rfcUrl = URL + "/noacljsonrfcadaptor?bapiname="
                + Vars.ZGET_VENDOR_HU_DATA_RFC + "&aclclientid=android";
        Log.d(TAG, "Update RFC URL -> " + rfcUrl);

        JsonObjectRequest strreq = new SapJsonObjectRequest(Request.Method.POST,
                rfcUrl, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "Update RFC response -> "
                                    + (response != null ? response.toString() : "null"));

                            if (!isUpdateRfcPayloadAvailable(response)) {
                                Log.d(TAG, "Update RFC not available / incomplete for this env — skip popup");
                                proceedAfterVersionCheck(iparr);
                                return;
                            }

                            String serverVersion = response.optString("EV_APP_VERSION", "").trim();
                            String downloadUrl = response.optString("EV_APP_URL", "").trim();

                            String localNorm = normalizeAppVersion(installedVersion);
                            String serverNorm = normalizeAppVersion(serverVersion);

                            Log.d(TAG, "Version compare local=[" + localNorm
                                    + "] server=[" + serverNorm + "] url=[" + downloadUrl + "]");

                            if (!serverNorm.equalsIgnoreCase(localNorm)) {
                                showUpdateDialog(downloadUrl, serverVersion, iparr);
                            } else {
                                proceedAfterVersionCheck(iparr);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Update RFC parse failed", e);
                            proceedAfterVersionCheck(iparr);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.e(TAG, "Update RFC failed / not available — continuing without popup", e);
                proceedAfterVersionCheck(iparr);
            }
        });
        Volley.newRequestQueue(this).add(strreq);
        strreq.setRetryPolicy(new DefaultRetryPolicy(50000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }




    private void checkIP(final String ipAdress ) {
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    makeStringReq(ipAdress);
                } catch (Exception e) {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }


    private void makeStringReq(final String url) {

        final RequestQueue mRequestQueue;

        //RequestQueue initialized
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        StringRequest strReq = new StringRequest(Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        dialog.dismiss();
                        Log.d(TAG, "code->" + response.toString());

                        if (Code.equals("200")) {
                            SharedPreferencesData data = new SharedPreferencesData(IPActivity.this);
                            data.write("URL", URL + "/ValueXMW");
                            startActivity(new Intent(IPActivity.this, LoginActivity.class));
                            //  finish();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d(TAG, "Error: " + error.getMessage());
                        Log.i(TAG, "Error :" + error.toString());
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

                        dialog.dismiss();
                        box.getBox("Err", err);

                    }
                }) {

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                int mStatusCode = response.statusCode;
                Log.d(TAG, "status code->" + response.statusCode);
                Code = String.valueOf(response.statusCode);
                return super.parseNetworkResponse(response);
            }
        };

        mRequestQueue.add(strReq);

    }
}