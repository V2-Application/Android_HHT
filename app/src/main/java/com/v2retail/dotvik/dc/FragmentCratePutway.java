package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Crate Putway
 *
 * Flow:
 *   1. Scan Crate No  -> ZWM_CRATE_STK_VALIDATE -> copies into Crate No
 *   2. Scan Bin No    -> ZWM_BIN_STK_VALIDATE -> copies into Bin No
 *   3. Save -> ZWM_CRATE_STK_PUT_BIN_RFC / Reset -> clear all
 */
public class FragmentCratePutway extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentCratePutway.class.getName();
    private static final int REQUEST_VALIDATE_CRATE = 1501;
    private static final int REQUEST_VALIDATE_BIN = 1502;
    private static final int REQUEST_SAVE = 1503;

    private View view;
    private Activity activity;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;

    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText etScanCrate;
    private EditText etCrateNo;
    private EditText etScanBin;
    private EditText etBinNo;
    private Button btnReset;
    private Button btnSave;

    private String pendingCrate = "";
    private String pendingBin = "";
    private boolean isCrateValidationInProgress = false;
    private boolean isBinValidationInProgress = false;
    private boolean isSaveInProgress = false;

    public FragmentCratePutway() {}

    public static FragmentCratePutway newInstance() {
        return new FragmentCratePutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_crate_putway, container, false);

        etScanCrate = view.findViewById(R.id.crate_putway_et_scan_crate);
        etCrateNo = view.findViewById(R.id.crate_putway_et_crate_no);
        etScanBin = view.findViewById(R.id.crate_putway_et_scan_bin);
        etBinNo = view.findViewById(R.id.crate_putway_et_bin_no);
        btnReset = view.findViewById(R.id.crate_putway_btn_reset);
        btnSave = view.findViewById(R.id.crate_putway_btn_save);

        btnReset.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        addInputEvents();

        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Crate Putway");
        }
    }

    private void init() {
        activity = getActivity();
        con = getContext();
        box = new AlertBox(activity);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        resetFields();
    }

    private void addInputEvents() {
        etScanCrate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(etScanCrate);
                    if (!value.isEmpty()) {
                        triggerCrateValidation(value);
                        return true;
                    }
                }
                return false;
            }
        });

        etScanCrate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    triggerCrateValidation(value);
                }
            }
        });

        etScanBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(etScanBin);
                    if (!value.isEmpty()) {
                        triggerBinValidation(value);
                        return true;
                    }
                }
                return false;
            }
        });

        etScanBin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    triggerBinValidation(value);
                }
            }
        });
    }

    private void triggerCrateValidation(String crate) {
        if (isCrateValidationInProgress || !etScanCrate.isEnabled()) {
            return;
        }
        validateCrate(crate);
    }

    private void triggerBinValidation(String bin) {
        if (isBinValidationInProgress || !etScanBin.isEnabled()) {
            return;
        }
        validateBin(bin);
    }

    private void validateCrate(String crate) {
        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate No.");
            etScanCrate.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            isCrateValidationInProgress = true;
            pendingCrate = crate;
            args.put("bapiname", Vars.ZWM_CRATE_STK_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_CRATE_STK_VALIDATE, REQUEST_VALIDATE_CRATE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getErrBox(e);
        }
    }

    private void onCrateValidated(String crate) {
        isCrateValidationInProgress = false;
        etScanCrate.setText("");
        UIFuncs.disableInput(con, etScanCrate);
        etCrateNo.setText(crate);
        UIFuncs.enableInput(con, etScanBin);
        etScanBin.requestFocus();
    }

    private void validateBin(String bin) {
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin No.");
            etScanBin.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            isBinValidationInProgress = true;
            pendingBin = bin;
            args.put("bapiname", Vars.ZWM_BIN_STK_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZWM_BIN_STK_VALIDATE, REQUEST_VALIDATE_BIN, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getErrBox(e);
        }
    }

    private void onBinValidated(String bin) {
        isBinValidationInProgress = false;
        etScanBin.setText("");
        UIFuncs.disableInput(con, etScanBin);
        etBinNo.setText(bin);
    }

    private void resetFields() {
        pendingCrate = "";
        pendingBin = "";
        isCrateValidationInProgress = false;
        isBinValidationInProgress = false;
        isSaveInProgress = false;
        etScanCrate.setText("");
        etCrateNo.setText("");
        etScanBin.setText("");
        etBinNo.setText("");

        UIFuncs.enableInput(con, etScanCrate);
        UIFuncs.disableInput(con, etScanBin);

        etScanCrate.requestFocus();
    }

    private void save() {
        String crate = etCrateNo.getText().toString().trim();
        String bin = etBinNo.getText().toString().trim();

        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate No.");
            etScanCrate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin No.");
            if (etScanBin.isEnabled()) {
                etScanBin.requestFocus();
            }
            return;
        }

        JSONObject args = new JSONObject();
        try {
            isSaveInProgress = true;
            args.put("bapiname", Vars.ZWM_CRATE_STK_PUT_BIN_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_CRATE", crate);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZWM_CRATE_STK_PUT_BIN_RFC, REQUEST_SAVE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getErrBox(e);
        }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    if (dialog != null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload -> " + params);

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        if (dialog != null) {
                            dialog.dismiss();
                            dialog = null;
                        }
                        Log.d(TAG, "response -> " + responsebody);

                        if (responsebody == null) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from Server");
                        } else if (responsebody.equals("") || responsebody.equals("null")
                                || responsebody.equals("{}")) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "Unable to Connect Server/ Empty Response");
                        } else {
                            handleResponse(responsebody, request);
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
                Log.d(TAG, "Network response -> " + res);
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
            Log.d(TAG, "jsonRequest getHeaders -> " + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getErrBox(authFailureError);
        }
    }

    private void handleResponse(JSONObject responsebody, int request) {
        try {
            if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                String type = returnobj.optString("TYPE", "");
                if ("E".equals(type)) {
                    UIFuncs.errorSound(con);
                    box.getBox("Err", returnobj.optString("MESSAGE", "Validation failed"));
                    if (request == REQUEST_VALIDATE_CRATE) {
                        isCrateValidationInProgress = false;
                        pendingCrate = "";
                        etScanCrate.setText("");
                        etScanCrate.requestFocus();
                    } else if (request == REQUEST_VALIDATE_BIN) {
                        isBinValidationInProgress = false;
                        pendingBin = "";
                        etScanBin.setText("");
                        etScanBin.requestFocus();
                    } else if (request == REQUEST_SAVE) {
                        isSaveInProgress = false;
                    }
                } else if (request == REQUEST_VALIDATE_CRATE) {
                    onCrateValidated(pendingCrate);
                } else if (request == REQUEST_VALIDATE_BIN) {
                    onBinValidated(pendingBin);
                } else if (request == REQUEST_SAVE) {
                    isSaveInProgress = false;
                    String message = returnobj.optString("MESSAGE", "Saved successfully");
                    box.getBox("Success", message);
                    resetFields();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            box.getErrBox(e);
        }
    }

    private Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error);

                String err;
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
                } else {
                    err = error.toString();
                }

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                UIFuncs.errorSound(con);
                box.getBox("Err", err);
            }
        };
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.crate_putway_btn_reset) {
            resetFields();
        } else if (id == R.id.crate_putway_btn_save) {
            save();
        }
    }
}



