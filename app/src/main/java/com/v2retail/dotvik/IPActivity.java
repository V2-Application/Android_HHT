package com.v2retail.dotvik;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.v2retail.ApplicationController;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

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
                getAppUpdate(iparr);
                break;
            case R.id.exit:
                this.finish();
                break;
        }
    }


    /**
     * Launch the APK installer. If the install is blocked due to a signing
     * certificate conflict (old app signed with different key), show a dialog
     * offering to uninstall the old package so the user can re-install cleanly.
     */
    private void launchInstaller(Context ctx, java.io.File apkFile) {
        try {
            android.content.Intent install = new android.content.Intent(Intent.ACTION_VIEW);
            Uri apkUri;
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                apkUri = androidx.core.content.FileProvider.getUriForFile(
                    ctx, ctx.getPackageName() + ".provider", apkFile);
                install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                apkUri = Uri.fromFile(apkFile);
                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            ctx.startActivity(install);
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "No installer activity found", e);
            // Fallback: open package installer directly
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
            // Could be a signing conflict — offer to uninstall old package
            showSigningConflictDialog(ctx, apkFile);
        }
    }

    /**
     * Show a dialog when install fails due to signing certificate mismatch.
     * Guides the user to uninstall the old version first.
     */
    private void showSigningConflictDialog(Context ctx, java.io.File apkFile) {
        runOnUiThread(() -> {
            new android.app.AlertDialog.Builder(ctx)
                .setTitle("Update Required — Action Needed")
                .setMessage("A newer version is ready to install, but the existing app "
                    + "needs to be removed first (one-time process due to a security key change).\n\n"
                    + "Tap UNINSTALL to remove the current app, then re-open this file from "
                    + "notifications to install the new version.\n\n"
                    + "You will NOT lose any data.")
                .setCancelable(false)
                .setPositiveButton("UNINSTALL OLD APP", (dialog, which) -> {
                    try {
                        // Launch system uninstall for this package
                        android.content.Intent uninstall = new android.content.Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:" + ctx.getPackageName()));
                        uninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(uninstall);
                    } catch (Exception ex) {
                        Toast.makeText(ctx,
                            "Go to Settings > Apps > V2 HHT > Uninstall, then re-install from apk.v2retail.net/download",
                            Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Manual Install Later", (dialog, which) -> {
                    Toast.makeText(ctx,
                        "APK saved. Go to Settings > Apps > V2 HHT > Uninstall, then install from: apk.v2retail.net/download",
                        Toast.LENGTH_LONG).show();
                })
                .show();
        });
    }

    private void getAppUpdate(String iparr[]){

        String version = BuildConfig.VERSION_NAME;
        String data[] = version.split("\\.");
        String majorVersion = data[0];
        String minorVersion = data[1];
        Log.v("Version",minorVersion+"////"+majorVersion);


        JsonObjectRequest strreq = new JsonObjectRequest(Request.Method.GET,
                URL+ "/appversion?appName=V2RetailOps&platform=Android&majorVersion="+majorVersion+"&minorVersion="+minorVersion, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // get response
                        try {
                            if (response.getString("upgrade").equals("available")){
                                    // Auto-download — no button tap needed
                                    String downloadUrl = null;
                                    try {
                                        downloadUrl = response.getString("downloadLink");
                                    } catch (JSONException je) {
                                        downloadUrl = "https://apk.v2retail.net/download";
                                    }
                                    final String finalUrl = downloadUrl;

                                    Toast.makeText(IPActivity.this,
                                        "New version available — downloading update...",
                                        Toast.LENGTH_LONG).show();

                                    try {
                                        // Download to app-private external dir (no WRITE_EXTERNAL_STORAGE needed)
                                        java.io.File outFile = new java.io.File(
                                            getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                                            "V2_HHT_Update.apk");
                                        if (outFile.exists()) outFile.delete();

                                        android.app.DownloadManager dm =
                                            (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                                        android.app.DownloadManager.Request req =
                                            new android.app.DownloadManager.Request(Uri.parse(finalUrl));
                                        req.setTitle("V2 HHT Update");
                                        req.setDescription("Downloading latest version...");
                                        req.setNotificationVisibility(
                                            android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                        req.setDestinationUri(Uri.fromFile(outFile));
                                        final long dlId = dm.enqueue(req);

                                        // BroadcastReceiver fires when download completes → auto-launch installer
                                        final java.io.File apkFile = outFile;
                                        android.content.BroadcastReceiver onDone =
                                            new android.content.BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context ctx, android.content.Intent intent) {
                                                long id = intent.getLongExtra(
                                                    android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                                                if (id != dlId) return;
                                                ctx.unregisterReceiver(this);
                                                launchInstaller(ctx, apkFile);
                                            }
                                        };
                                        registerReceiver(onDone, new android.content.IntentFilter(
                                            android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                                    } catch (Exception e) {
                                        Log.e(TAG, "Auto-download failed", e);
                                        Toast.makeText(IPActivity.this,
                                            "Please update manually from apk.v2retail.net",
                                            Toast.LENGTH_LONG).show();
                                    }

                            }else {
                                try{
                                    checkIP(iparr[0].trim() + "/index.jsp");
                                }catch (Exception e)
                                {
                                    box.getErrBox(e);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                e.printStackTrace();
            }
        });
        Volley.newRequestQueue(this).add(strreq);
        strreq.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });

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