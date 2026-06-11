package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Pallet Putway To BIN — Gate Entry sub-flow.
 *
 * RFC flow:
 *   1. Scan Pallet -> {@link Vars#ZWM_GATE_PALLATE_VALIDATE3_N}
 *   2. Scan BIN    -> {@link Vars#ZWM_GATE_BIN_VALIDATION3_N} (putway transaction)
 */
public class FragmentPalletPutwayToBin extends Fragment implements View.OnClickListener {

    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etScanPallet;
    private EditText etPallet;
    private EditText etScanBin;
    private EditText etBin;
    private EditText etBoxCount;
    private TextView tvStatus;
    private Button btnBack;

    private String URL = "";
    private String USER = "";
    private String WERKS = "";

    private String validatedPallet = "";
    private int boxCount = 0;

    public FragmentPalletPutwayToBin() {
    }

    public static FragmentPalletPutwayToBin newInstance() {
        return new FragmentPalletPutwayToBin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pallet_putway_to_bin, container, false);

        etScanPallet = view.findViewById(R.id.pallet_et_scan_pallet);
        etPallet = view.findViewById(R.id.pallet_et_pallet);
        etScanBin = view.findViewById(R.id.pallet_et_scan_bin);
        etBin = view.findViewById(R.id.pallet_et_bin);
        etBoxCount = view.findViewById(R.id.pallet_et_box_count);
        tvStatus = view.findViewById(R.id.pallet_tv_status);
        btnBack = view.findViewById(R.id.pallet_btn_back);

        btnBack.setOnClickListener(this);

        etScanPallet.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String pallet = etScanPallet.getText().toString().trim().toUpperCase();
                    if (!pallet.isEmpty()) {
                        validatePallet(pallet);
                    }
                    return true;
                }
                return false;
            }
        });

        etScanBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String bin = etScanBin.getText().toString().trim().toUpperCase();
                    if (!bin.isEmpty()) {
                        validateBin(bin);
                    }
                    return true;
                }
                return false;
            }
        });

        addScannerWatcher(etScanPallet, new ScanHandler() {
            @Override
            public void onScan(String value) {
                if (TextUtils.isEmpty(validatedPallet)) {
                    validatePallet(value);
                } else if (etScanBin.isEnabled()) {
                    validateBin(value);
                }
            }
        });

        addScannerWatcher(etScanBin, new ScanHandler() {
            @Override
            public void onScan(String value) {
                validateBin(value);
            }
        });

        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Pallet Putway To BIN");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        WERKS = prefs.read("WERKS");

        boxCount = 0;
        etBoxCount.setText("0");
        resetToPalletScan();
        etScanPallet.requestFocus();
    }

    private void validatePallet(final String pallet) {
        if (TextUtils.isEmpty(WERKS)) {
            showError("Store ID not found. Please login again.");
            etScanPallet.setText("");
            return;
        }

        showProgress("Validating pallet...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GATE_PALLATE_VALIDATE3_N);
            params.put("IM_USER", USER);
            params.put("IM_WERKS", WERKS);
            params.put("IM_PALL", pallet);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GATE_PALLATE_VALIDATE3_N, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                validatedPallet = pallet;
                etPallet.setText(pallet);
                etScanPallet.setText("");
                etScanPallet.setEnabled(false);
                etScanPallet.setBackgroundResource(R.drawable.border_disabled_input);
                etScanBin.setEnabled(true);
                etScanBin.setBackgroundResource(R.drawable.border);
                etScanBin.requestFocus();
                showStatus(getApiMessage(response, "Pallet validated successfully"), true);
            }

            @Override
            public void onError(String message) {
                showError(message != null ? message : "Invalid pallet");
                etScanPallet.setText("");
                etScanPallet.requestFocus();
            }
        });
    }

    private void validateBin(final String bin) {
        if (TextUtils.isEmpty(validatedPallet)) {
            showError("Please scan pallet first");
            etScanBin.setText("");
            return;
        }
        if (TextUtils.isEmpty(WERKS)) {
            showError("Store ID not found. Please login again.");
            etScanBin.setText("");
            return;
        }

        showProgress("Validating BIN...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GATE_BIN_VALIDATION3_N);
            params.put("IM_USER", USER);
            params.put("IM_WERKS", WERKS);
            params.put("IM_PALL", validatedPallet);
            params.put("IM_BIN", bin);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GATE_BIN_VALIDATION3_N, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                etBin.setText(bin);
                boxCount += 1;
                etBoxCount.setText(String.valueOf(boxCount));
                showStatus(getApiMessage(response, "BIN validated successfully"), true);
                clearPalletAndBinFields();
            }

            @Override
            public void onError(String message) {
                showError(message != null ? message : "Invalid BIN");
                etScanBin.setText("");
                etScanBin.requestFocus();
            }
        });
    }

    private void clearPalletAndBinFields() {
        validatedPallet = "";
        etPallet.setText("");
        etBin.setText("");
        resetToPalletScan();
        hideKeyboard();
        etScanPallet.requestFocus();
    }

    private void resetToPalletScan() {
        etScanPallet.setText("");
        etScanPallet.setEnabled(true);
        etScanPallet.setBackgroundResource(R.drawable.border);
        etScanBin.setText("");
        etScanBin.setEnabled(false);
        etScanBin.setBackgroundResource(R.drawable.border_disabled_input);
    }

    private void hideKeyboard() {
        if (activity == null || etScanPallet == null) {
            return;
        }
        etScanPallet.clearFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etScanPallet.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pallet_btn_back) {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        }
    }

    private interface ScanHandler {
        void onScan(String value);
    }

    private void addScannerWatcher(final EditText field, final ScanHandler handler) {
        field.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().trim().toUpperCase();
                if (!value.isEmpty() && scannerReading) {
                    handler.onScan(value);
                }
            }
        });
    }

    private interface RfcCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    private void postRfc(String rfcName, JSONObject params, final RfcCallback callback) {
        String rfcUrl = buildRfcUrl(rfcName);
        JsonObjectRequest req = new SapJsonObjectRequest(
                Request.Method.POST, rfcUrl, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissProgress();
                        if (response == null) {
                            callback.onError("Empty response from server.");
                            return;
                        }
                        if (isApiSuccess(response)) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError(getApiMessage(response, "Request failed"));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        dismissProgress();
                        callback.onError("Network error: " + parseVolleyError(error));
                    }
                });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        queue.add(req);
    }

    private String buildRfcUrl(String rfcName) {
        String base = URL;
        if (base.contains("/ValueXMW")) {
            base = base.replace("/ValueXMW", "");
        }
        return base + "/noacljsonrfcadaptor?bapiname=" + rfcName + "&aclclientid=android";
    }

    private boolean isApiSuccess(JSONObject response) {
        if (response.has("Status")) {
            return response.optBoolean("Status", false);
        }
        JSONObject ret = response.optJSONObject("EX_RETURN");
        if (ret != null) {
            String type = ret.optString("TYPE", "");
            return "S".equalsIgnoreCase(type) || type.isEmpty();
        }
        return response.length() > 0;
    }

    private String getApiMessage(JSONObject response, String fallback) {
        String message = response.optString("Message", "").trim();
        if (!message.isEmpty()) {
            return message;
        }
        JSONObject ret = response.optJSONObject("EX_RETURN");
        if (ret != null) {
            message = ret.optString("MESSAGE", "").trim();
            if (!message.isEmpty()) {
                return message;
            }
        }
        return fallback;
    }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus == null) {
            return;
        }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
    }

    private void showError(String msg) {
        showStatus(msg, false);
        if (box != null) {
            box.getBox("Error", msg);
        }
    }

    private void showProgress(String msg) {
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);
        }
        dialog.setMessage(msg);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private String parseVolleyError(VolleyError e) {
        if (e instanceof TimeoutError) {
            return "Request timed out";
        }
        if (e instanceof NoConnectionError) {
            return "No network connection";
        }
        if (e instanceof NetworkError) {
            return "Network error";
        }
        if (e instanceof ServerError) {
            if (e.networkResponse != null && e.networkResponse.data != null) {
                return "Server error: " + new String(e.networkResponse.data).substring(0,
                        Math.min(100, e.networkResponse.data.length));
            }
            return "Server error";
        }
        if (e instanceof ParseError) {
            return "Response parse error";
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }
}
