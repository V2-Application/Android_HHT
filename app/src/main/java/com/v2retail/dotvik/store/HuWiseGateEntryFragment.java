package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * HU Wise Gate Entry — scan HUs against a gate entry vehicle/seal/invoice.
 */
public class HuWiseGateEntryFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = HuWiseGateEntryFragment.class.getName();

    private EditText etVehicleNo, etSealNo, etInvNo, etHuNo, etHuQty;
    private TextView tvStatus;
    private Button btnClear, btnBack;

    private AlertBox box;
    private ProgressDialog dialog;
    private FragmentManager fm;

    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private int huQty = 0;
    private boolean vehicleLocked = false;
    private boolean huScanInProgress = false;
    private final List<String> scannedHuList = new ArrayList<>();

    public HuWiseGateEntryFragment() {
    }

    public static HuWiseGateEntryFragment newInstance() {
        return new HuWiseGateEntryFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hu_wise_gate_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Home_Activity) {
            ((Home_Activity) getActivity()).setActionBarTitle("HU Wise Gate Entry");
        }
    }

    private void init(View view) {
        Context con = getContext();
        box = new AlertBox(con);
        fm = getFragmentManager();

        etVehicleNo = view.findViewById(R.id.et_vehicle_no);
        etSealNo = view.findViewById(R.id.et_seal_no);
        etInvNo = view.findViewById(R.id.et_inv_no);
        etHuNo = view.findViewById(R.id.et_hu_no);
        etHuQty = view.findViewById(R.id.et_hu_qty);
        tvStatus = view.findViewById(R.id.tv_status);
        btnClear = view.findViewById(R.id.btn_clear);
        btnBack = view.findViewById(R.id.btn_back);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        btnClear.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        etVehicleNo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    if (lockVehicleField()) {
                        etSealNo.requestFocus();
                    }
                    return true;
                }
                return false;
            }
        });
        etVehicleNo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    lockVehicleField();
                }
            }
        });

        etHuNo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    processHuScan();
                    return true;
                }
                return false;
            }
        });

        addHuScanTextWatcher();
        clearAllFields();
        etVehicleNo.requestFocus();
    }

    private void enableVehicleEntry() {
        vehicleLocked = false;
        etVehicleNo.setEnabled(true);
        etVehicleNo.setFocusable(true);
        etVehicleNo.setFocusableInTouchMode(true);
        etVehicleNo.setBackgroundResource(R.drawable.border);
    }

    private boolean lockVehicleField() {
        if (vehicleLocked) {
            return true;
        }
        String vehicleNo = etVehicleNo.getText().toString().trim();
        if (vehicleNo.isEmpty()) {
            return false;
        }
        vehicleLocked = true;
        etVehicleNo.setEnabled(false);
        etVehicleNo.setFocusable(false);
        etVehicleNo.setBackgroundResource(R.drawable.border_disabled_input);
        return true;
    }

    private void addHuScanTextWatcher() {
        etHuNo.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 2;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (scannerReading) {
                    processHuScan();
                }
            }
        });
    }

    private void processHuScan() {
        if (huScanInProgress) {
            return;
        }

        String huNo = etHuNo.getText().toString().trim();
        if (huNo.isEmpty()) {
            box.getBox("Alert", "Scan HU No.");
            return;
        }

        if (scannedHuList.contains(huNo)) {
            box.getBox("Alert", "HU already scanned.");
            clearHuInput();
            return;
        }

        String vehicleNo = etVehicleNo.getText().toString().trim();
        String sealNo = etSealNo.getText().toString().trim();
        String invNo = etInvNo.getText().toString().trim();
        if (vehicleNo.isEmpty()) {
            box.getBox("Alert", "Enter Vehicle No.");
            etVehicleNo.requestFocus();
            return;
        }
        lockVehicleField();
        if (sealNo.isEmpty()) {
            box.getBox("Alert", "Enter Seal No.");
            etSealNo.requestFocus();
            return;
        }
        if (invNo.isEmpty()) {
            box.getBox("Alert", "Enter Inv No.");
            etInvNo.requestFocus();
            return;
        }

        hideKeyboard();
        callGateEntryUpdate(huNo);
    }

    private void callGateEntryUpdate(final String huNo) {
        String rfc = Vars.Z_HU_GATE_ENTRY_UPDATE;
        String url = GatewayUrls.noAclJsonRfcUrl(URL, rfc);
        if (url.isEmpty()) {
            box.getBox("Err", "Server URL missing. Please log in again.");
            return;
        }

        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);

            JSONObject row = new JSONObject();
            row.put("SITE", WERKS);
            row.put("HU_NO", huNo);
            row.put("VEHICLE_NO", etVehicleNo.getText().toString().trim());
            row.put("SEAL_NO", etSealNo.getText().toString().trim());
            row.put("INVOICE_NO", etInvNo.getText().toString().trim());
            row.put("GE_DT", Util.DateTime("yyyyMMdd", new Date()));
            row.put("GE_TIME", Util.DateTime("HHmmss", new Date()));
            row.put("HHT_USER", USER);

            JSONArray itGateEntry = new JSONArray();
            itGateEntry.put(row);
            params.put("IT_GATE_ENTRY", itGateEntry);
        } catch (JSONException e) {
            box.getErrBox(e);
            return;
        }

        showProgress("Updating gate entry...");
        huScanInProgress = true;
        etHuNo.setEnabled(false);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest request = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissProgress();
                        huScanInProgress = false;
                        etHuNo.setEnabled(true);
                        Log.d(TAG, "response -> " + response);

                        if (response == null || response.length() == 0) {
                            box.getBox("Err", "No response from server.");
                            clearHuInput();
                            return;
                        }

                        try {
                            if (!isRfcSuccess(response)) {
                                showStatus(getRfcMessage(response, "HU update failed."), false);
                                box.getBox("Err", getRfcMessage(response, "HU update failed."));
                                clearHuInput();
                                return;
                            }

                            applyResponseHeader(response);
                            scannedHuList.add(huNo);
                            huQty++;
                            etHuQty.setText(String.valueOf(huQty));

                            String msg = getRfcMessage(response, "HU scanned successfully.");
                            showStatus(msg, true);
                            clearHuInput();
                        } catch (JSONException e) {
                            box.getErrBox(e);
                            clearHuInput();
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
                return super.parseNetworkResponse(response);
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
        Log.d(TAG, "payload -> " + params);
    }

    private void applyResponseHeader(JSONObject response) throws JSONException {
        JSONArray rows = response.optJSONArray("IT_GATE_ENTRY");
        if (rows == null || rows.length() == 0) {
            rows = response.optJSONArray("ET_GATE_ENTRY");
        }
        if (rows == null || rows.length() == 0) {
            return;
        }

        JSONObject row = rows.optJSONObject(0);
        if (row == null) {
            return;
        }

        setIfPresent(etSealNo, row.optString("SEAL_NO", ""));
        setIfPresent(etInvNo, row.optString("INVOICE_NO", ""));
    }

    private static void setIfPresent(EditText field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            field.setText(value.trim());
        }
    }

    private static boolean isRfcSuccess(JSONObject response) throws JSONException {
        if (response.has("EV_SUBRC")) {
            return response.optInt("EV_SUBRC", -1) == 0;
        }
        if (response.has("EX_RETURN") && response.get("EX_RETURN") instanceof JSONObject) {
            JSONObject exReturn = response.getJSONObject("EX_RETURN");
            String type = exReturn.optString("TYPE", "");
            return !"E".equals(type) && !"A".equals(type);
        }
        return true;
    }

    private static String getRfcMessage(JSONObject response, String fallback) throws JSONException {
        String message = response.optString("EV_MESSAGE", "").trim();
        if (!message.isEmpty()) {
            return message;
        }
        if (response.has("EX_RETURN") && response.get("EX_RETURN") instanceof JSONObject) {
            message = response.getJSONObject("EX_RETURN").optString("MESSAGE", "").trim();
            if (!message.isEmpty()) {
                return message;
            }
        }
        return fallback;
    }

    private void clearAllFields() {
        etVehicleNo.setText("");
        etSealNo.setText("");
        etInvNo.setText("");
        etHuNo.setText("");
        etHuQty.setText("0");
        huQty = 0;
        scannedHuList.clear();
        enableVehicleEntry();
        showStatus("Enter Vehicle No, then Seal No and Inv No, then scan HU.", true);
        etVehicleNo.requestFocus();
    }

    private void clearHuInput() {
        etHuNo.setText("");
        etHuNo.requestFocus();
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etHuNo.getWindowToken(), 0);
        }
    }

    private void showProgress(String message) {
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(getContext());
            dialog.setCancelable(false);
        }
        dialog.setMessage(message);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgress();
                huScanInProgress = false;
                etHuNo.setEnabled(true);
                Log.i(TAG, "Error: " + error);
                box.getBox("Err", "Network error while updating gate entry.");
                clearHuInput();
            }
        };
    }

    private void showStatus(String message, boolean ok) {
        if (tvStatus == null) {
            return;
        }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_clear) {
            clearAllFields();
        } else if (id == R.id.btn_back) {
            if (fm != null) {
                fm.popBackStack();
            }
        }
    }
}
