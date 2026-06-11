package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gate Entry Lot Putway — Box Putaway to Pallet
 *
 * RFC flow: see {@link Vars#ZWM_GET_GATE_ENTRY_LIST_RFC} through {@link Vars#ZWM_GATE_SAVE3_N}.
 */
public class FragmentGateEntryLotPutway extends Fragment implements View.OnClickListener {

    private static final String PLACEHOLDER_PO  = "Automatically as per Gate entry";
    private static final String SPINNER_DEFAULT = "Select Gate Entry No";

    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etDcSite;
    private Spinner spGateEntry;
    private TextView tvPoNo;
    private TextView tvVendorInv;
    private EditText etPallet;
    private EditText etBox;
    private TextView tvStatus;
    private Button btnBack;
    private Button btnComplete;

    private ArrayAdapter<String> gateEntryAdapter;
    private final List<String> gateEntryOptions = new ArrayList<>();
    private boolean ignoreSpinnerSelection = false;

    private String URL = "";
    private String USER = "";
    private String WERKS = "";
    private String selectedGate = "";
    private String validatedPallet = "";
    private String pendingBox = "";

    public FragmentGateEntryLotPutway() {
    }

    public static FragmentGateEntryLotPutway newInstance() {
        return new FragmentGateEntryLotPutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gate_entry_lot_putway, container, false);

        etDcSite = view.findViewById(R.id.gate_et_dc_site);
        spGateEntry = view.findViewById(R.id.gate_sp_gate_entry);
        tvPoNo = view.findViewById(R.id.gate_tv_po_no);
        tvVendorInv = view.findViewById(R.id.gate_tv_vendor_inv);
        etPallet = view.findViewById(R.id.gate_et_pallet);
        etBox = view.findViewById(R.id.gate_et_box);
        tvStatus = view.findViewById(R.id.gate_tv_status);
        btnBack = view.findViewById(R.id.gate_btn_back);
        btnComplete = view.findViewById(R.id.gate_btn_complete);

        btnBack.setOnClickListener(this);
        btnComplete.setOnClickListener(this);

        etPallet.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String pallet = etPallet.getText().toString().trim();
                    if (!pallet.isEmpty()) {
                        validatePallet(pallet);
                    }
                    return true;
                }
                return false;
            }
        });

        etBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String box = etBox.getText().toString().trim();
                    if (!box.isEmpty()) {
                        onBoxScanned(box);
                    }
                    return true;
                }
                return false;
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
                    .setActionBarTitle("Gate Entry Lot Putway");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        WERKS = prefs.read("WERKS");

        etDcSite.setText(WERKS);
        resetGateEntryDetails();
        setupGateEntrySpinner();
        updateFieldStates();
        loadGateEntryList();
    }

    private void setupGateEntrySpinner() {
        gateEntryOptions.clear();
        gateEntryOptions.add(SPINNER_DEFAULT);
        gateEntryAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, gateEntryOptions);
        gateEntryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGateEntry.setAdapter(gateEntryAdapter);
        spGateEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreSpinnerSelection || position <= 0) {
                    return;
                }
                onGateEntrySelected(gateEntryOptions.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadGateEntryList() {
        if (TextUtils.isEmpty(WERKS)) {
            showError("Store ID not found. Please login again.");
            return;
        }

        showProgress("Loading gate entries...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GET_GATE_ENTRY_LIST_RFC);
            params.put("IM_USER", USER);
            params.put("IM_WERKS", WERKS);
            params.put("IM_DOCNO", "");
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GET_GATE_ENTRY_LIST_RFC, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray etData = getEtDataArray(response);
                List<String> docNos = new ArrayList<>();
                if (etData != null) {
                    try {
                        int start = SapJsonRows.startIndex(etData);
                        for (int i = start; i < etData.length(); i++) {
                            JSONObject row = etData.optJSONObject(i);
                            if (row == null) {
                                continue;
                            }
                            String docNo = row.optString("DOCNO", "").trim();
                            if (!docNo.isEmpty()) {
                                docNos.add(docNo);
                            }
                        }
                    } catch (JSONException e) {
                        showError("Failed to parse gate entry list.");
                        return;
                    }
                }

                ignoreSpinnerSelection = true;
                gateEntryOptions.clear();
                gateEntryOptions.add(SPINNER_DEFAULT);
                gateEntryOptions.addAll(docNos);
                gateEntryAdapter.notifyDataSetChanged();
                spGateEntry.setSelection(0);
                ignoreSpinnerSelection = false;

                if (docNos.isEmpty()) {
                    showStatus("No open gate entries found.", false);
                } else {
                    showStatus("Select Gate Entry No.", true);
                }
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void onGateEntrySelected(String gateNo) {
        selectedGate = gateNo;
        resetPalletAndBox();
        resetGateEntryDetails();
        etPallet.setEnabled(true);
        etPallet.requestFocus();
        updateFieldStates();
        loadGateEntryData(gateNo);
    }

    private void loadGateEntryData(final String gateNo) {
        showProgress("Loading gate entry data...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GET_GATE_ENTRY_DATA_RFC);
            params.put("IM_USER", USER);
            params.put("IM_GATE", gateNo);
            params.put("IM_WERKS", WERKS);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GET_GATE_ENTRY_DATA_RFC, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray etData = getEtDataArray(response);
                String poNo = "";
                String invNo = "";
                if (etData != null) {
                    try {
                        int start = SapJsonRows.startIndex(etData);
                        if (start < etData.length()) {
                            JSONObject row = etData.optJSONObject(start);
                            if (row != null) {
                                poNo = row.optString("VPONO", "").trim();
                                invNo = row.optString("INVNO", "").trim();
                            }
                        }
                    } catch (JSONException e) {
                        showError("Failed to parse gate entry data.");
                        return;
                    }
                }
                setPoAndInvDisplay(poNo, invNo);
                showStatus("Gate Entry " + gateNo + " selected. Scan Pallet.", true);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void validatePallet(final String pallet) {
        if (TextUtils.isEmpty(selectedGate)) {
            showError("Please select Gate Entry No first");
            clearPalletField();
            return;
        }

        showProgress("Validating pallet...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GET_GATE_ENTRY_PALLATE_RFC);
            params.put("IM_USER", USER);
            params.put("IM_GATE", selectedGate);
            params.put("IM_WERKS", WERKS);
            params.put("IM_PALL", pallet);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GET_GATE_ENTRY_PALLATE_RFC, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                validatedPallet = pallet;
                etPallet.setText(pallet);
                etPallet.setEnabled(false);
                etBox.setEnabled(true);
                etBox.requestFocus();
                updateCompleteButton();
                showStatus(getApiMessage(response, "Pallet validated successfully"), true);
            }

            @Override
            public void onError(String message) {
                showError(message != null ? message : "Invalid pallet");
                clearPalletField();
            }
        });
    }

    private void onBoxScanned(String boxBarcode) {
        if (TextUtils.isEmpty(selectedGate) || TextUtils.isEmpty(validatedPallet)) {
            showError("Please fill all required fields (Gate Entry, Pallet, Box)");
            etBox.setText("");
            return;
        }
        pendingBox = boxBarcode;
        showWeightDialog(boxBarcode);
    }

    private void showWeightDialog(final String boxBarcode) {
        final EditText weightInput = new EditText(activity);
        weightInput.setHint("Enter box weight");
        weightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weightInput.setPadding(24, 16, 24, 16);

        new AlertDialog.Builder(activity)
                .setTitle("Box Weight")
                .setMessage("Enter weight for box: " + boxBarcode)
                .setView(weightInput)
                .setCancelable(false)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pendingBox = "";
                        etBox.setText("");
                        etBox.requestFocus();
                    }
                })
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String weight = weightInput.getText().toString().trim();
                        if (TextUtils.isEmpty(weight)) {
                            showError("Please enter box weight");
                            etBox.setText(boxBarcode);
                            showWeightDialog(boxBarcode);
                            return;
                        }
                        submitBox(boxBarcode, weight);
                    }
                })
                .show();
    }

    private void submitBox(final String boxBarcode, final String weight) {
        if (TextUtils.isEmpty(selectedGate) || TextUtils.isEmpty(validatedPallet)
                || TextUtils.isEmpty(boxBarcode)) {
            showError("Please fill all required fields (Gate Entry, Pallet, Box)");
            return;
        }

        showProgress("Submitting box...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GATE_BOX3N);
            params.put("IM_USER", USER);
            params.put("IM_GATE", selectedGate);
            params.put("IM_WERKS", WERKS);
            params.put("IM_PALL", validatedPallet);
            params.put("IM_BOX", boxBarcode);
            params.put("IM_WEIGHT", weight);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GATE_BOX3N, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pendingBox = "";
                etBox.setText("");
                etBox.requestFocus();
                showStatus(getApiMessage(response, "Box submitted successfully"), true);
            }

            @Override
            public void onError(String message) {
                showError(message);
                etBox.setText("");
                etBox.requestFocus();
            }
        });
    }

    private void completeGateEntry() {
        if (TextUtils.isEmpty(WERKS)) {
            showError("Store ID not found. Please login again.");
            return;
        }
        if (TextUtils.isEmpty(selectedGate)) {
            showError("Please select Gate Entry No first");
            return;
        }
        if (TextUtils.isEmpty(validatedPallet)) {
            showError("Please validate Pallet first");
            return;
        }

        showProgress("Saving gate entry...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GATE_SAVE3_N);
            params.put("IM_USER", USER);
            params.put("IM_GATE", selectedGate);
            params.put("IM_WERKS", WERKS);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GATE_SAVE3_N, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                String msg = getApiMessage(response, "Gate entry saved successfully");
                box.getBox("Success", msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getFragmentManager() != null) {
                            getFragmentManager().popBackStack();
                        }
                    }
                }, null);
            }

            @Override
            public void onError(String message) {
                showError(message != null ? message : "Failed to save gate entry");
            }
        });
    }

    private void resetGateEntryDetails() {
        setPoAndInvDisplay("", "");
    }

    private void setPoAndInvDisplay(String poNo, String invNo) {
        if (TextUtils.isEmpty(poNo)) {
            tvPoNo.setText(PLACEHOLDER_PO);
            tvPoNo.setTextColor(0xFF888888);
        } else {
            tvPoNo.setText(poNo);
            tvPoNo.setTextColor(0xFF000000);
        }

        if (TextUtils.isEmpty(invNo)) {
            tvVendorInv.setText(PLACEHOLDER_PO);
            tvVendorInv.setTextColor(0xFF888888);
        } else {
            tvVendorInv.setText(invNo);
            tvVendorInv.setTextColor(0xFF000000);
        }
    }

    private void resetPalletAndBox() {
        validatedPallet = "";
        pendingBox = "";
        etPallet.setText("");
        etBox.setText("");
        etPallet.setEnabled(!TextUtils.isEmpty(selectedGate));
        etBox.setEnabled(false);
        updateCompleteButton();
    }

    private void clearPalletField() {
        validatedPallet = "";
        etPallet.setText("");
        etPallet.setEnabled(!TextUtils.isEmpty(selectedGate));
        etBox.setText("");
        etBox.setEnabled(false);
        updateCompleteButton();
        if (!TextUtils.isEmpty(selectedGate)) {
            etPallet.requestFocus();
        }
    }

    private void updateFieldStates() {
        etPallet.setEnabled(!TextUtils.isEmpty(selectedGate) && TextUtils.isEmpty(validatedPallet));
        etBox.setEnabled(!TextUtils.isEmpty(validatedPallet));
        updateCompleteButton();
    }

    private void updateCompleteButton() {
        boolean enabled = !TextUtils.isEmpty(validatedPallet);
        btnComplete.setEnabled(enabled);
        btnComplete.setBackgroundColor(enabled ? 0xFFE71821 : 0xFF888888);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.gate_btn_back) {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        } else if (id == R.id.gate_btn_complete) {
            completeGateEntry();
        }
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

    private JSONArray getEtDataArray(JSONObject response) {
        JSONObject data = response.optJSONObject("Data");
        if (data != null) {
            JSONArray arr = data.optJSONArray("ET_Data");
            if (arr == null) {
                arr = data.optJSONArray("ET_DATA");
            }
            if (arr != null) {
                return arr;
            }
        }
        JSONArray arr = response.optJSONArray("ET_DATA");
        if (arr == null) {
            arr = response.optJSONArray("ET_Data");
        }
        return arr;
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
