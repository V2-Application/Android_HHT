package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.v2retail.commons.GatewayUrls;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LOT Picking Process — Pallet Picking from BIN.
 *
 * RFC flow:
 *   1. Screen load  -> {@link Vars#ZWM_GET_GATE_ENTRY_LIST4_RFC}
 *   2. Gate select  -> {@link Vars#ZWM_GET_GATE_ENTRY_DATA4_RFC}
 *   3. Scan BIN     -> {@link Vars#ZWM_GATE_BIN_VALIDATION4_N}
 *   4. Scan Pallet  -> {@link Vars#ZWM_GATE_PALLATE_VALIDATE4_N}
 *   5. After pick   -> clear form + remove gate entry from dropdown
 */
public class FragmentLotPickingFromBin extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentLotPickingFromBin";
    private static final String PLACEHOLDER_PO = "Auto fetch";
    private static final String SPINNER_DEFAULT = "Select Gate Entry No.";

    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private Spinner spGateEntry;
    private TextView tvPoNo;
    private EditText etScanBin;
    private EditText etBin;
    private EditText etScanPallet;
    private EditText etPallet;
    private TextView tvStatus;
    private TableLayout tableData;
    private Button btnBack;

    private ArrayAdapter<String> gateEntryAdapter;
    private final List<String> gateEntryOptions = new ArrayList<>();
    private boolean ignoreSpinnerSelection = false;

    private String URL = "";
    private String USER = "";
    private String PLANT = "";

    private String selectedGate = "";
    private String validatedBin = "";

    public FragmentLotPickingFromBin() {
    }

    public static FragmentLotPickingFromBin newInstance() {
        return new FragmentLotPickingFromBin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lot_picking_from_bin, container, false);

        spGateEntry = view.findViewById(R.id.lot_sp_gate_entry);
        tvPoNo = view.findViewById(R.id.lot_tv_po_no);
        etScanBin = view.findViewById(R.id.lot_et_scan_bin);
        etBin = view.findViewById(R.id.lot_et_bin);
        etScanPallet = view.findViewById(R.id.lot_et_scan_pallet);
        etPallet = view.findViewById(R.id.lot_et_pallet);
        tvStatus = view.findViewById(R.id.lot_tv_status);
        tableData = view.findViewById(R.id.lot_table_data);
        btnBack = view.findViewById(R.id.lot_btn_back);

        btnBack.setOnClickListener(this);

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

        addScannerWatcher(etScanBin, new ScanHandler() {
            @Override
            public void onScan(String value) {
                validateBin(value);
            }
        });

        addScannerWatcher(etScanPallet, new ScanHandler() {
            @Override
            public void onScan(String value) {
                validatePallet(value);
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
                    .setActionBarTitle("LOT Picking Process");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        PLANT = prefs.read("WERKS");
        Log.i(TAG, "Session values -> URL=" + URL + ", USER=" + USER + ", PLANT=" + PLANT);

        resetPoDisplay();
        resetBinAndPalletFields();
        setupGateEntrySpinner();
        buildTableHeader();
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
        if (TextUtils.isEmpty(PLANT)) {
            showError("Store ID not found");
            return;
        }

        showProgress("Loading gate entries...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GET_GATE_ENTRY_LIST4_RFC);
            params.put("IM_USER", USER);
            params.put("IM_PLANT", PLANT);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GET_GATE_ENTRY_LIST4_RFC, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // LIST4 returns ET_DATA[].EDOCNO (not IT_DATA)
                JSONArray etData = getEtDataArray(response);
                List<String> docNos = new ArrayList<>();
                if (etData != null) {
                    try {
                        int start = SapJsonRows.startIndex(etData, "EDOCNO", "DOCNO");
                        for (int i = start; i < etData.length(); i++) {
                            JSONObject row = etData.optJSONObject(i);
                            if (row == null
                                    || SapJsonRows.isMetadataRow(row, "EDOCNO", "DOCNO")) {
                                continue;
                            }
                            String docNo = row.optString("EDOCNO", "").trim();
                            if (docNo.isEmpty()) {
                                docNo = row.optString("DOCNO", "").trim();
                            }
                            if (!docNo.isEmpty()
                                    && !docNo.toLowerCase().contains("gate entry")) {
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

                Log.i(TAG, "Gate Entry bound count=" + docNos.size()
                        + " from ET_DATA size="
                        + (etData != null ? etData.length() : 0));
                if (docNos.isEmpty()) {
                    showStatus("No open gate entries found.", false);
                } else {
                    showStatus("Select Gate Entry No. (" + docNos.size() + ")", true);
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
        validatedBin = "";
        resetPoDisplay();
        resetBinAndPalletFields();
        enableBinScan();
        loadGateEntryData(gateNo);
    }

    private void loadGateEntryData(final String gateNo) {
        showProgress("Loading gate entry data...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GET_GATE_ENTRY_DATA4_RFC);
            params.put("IM_USER", USER);
            params.put("IM_PLANT", PLANT);
            params.put("IM_GET", gateNo);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GET_GATE_ENTRY_DATA4_RFC, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject data = response.optJSONObject("Data");
                if (data == null) {
                    data = response;
                }

                String poNo = data.optString("EX_PO", "").trim();
                setPoDisplay(poNo);

                JSONArray etData = data.optJSONArray("ET_Data");
                if (etData == null) {
                    etData = data.optJSONArray("ET_DATA");
                }
                if (etData == null) {
                    etData = getEtDataArray(response);
                }
                populateTable(etData);
                showStatus("Gate Entry " + gateNo + " selected. Scan BIN.", true);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void validateBin(final String bin) {
        if (TextUtils.isEmpty(selectedGate)) {
            showError("Please select Gate Entry No first");
            etScanBin.setText("");
            return;
        }
        if (TextUtils.isEmpty(PLANT)) {
            showError("Store ID not found");
            etScanBin.setText("");
            return;
        }

        showProgress("Validating BIN...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GATE_BIN_VALIDATION4_N);
            params.put("IM_USER", USER);
            params.put("IM_PLANT", PLANT);
            params.put("IM_GET", selectedGate);
            params.put("IM_BIN", bin);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GATE_BIN_VALIDATION4_N, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                validatedBin = bin;
                etBin.setText(bin);
                etScanBin.setText("");
                disableBinScan();
                enablePalletScan();
                etScanPallet.requestFocus();
                showStatus(getApiMessage(response, "BIN validated successfully"), true);
                hideKeyboard();
            }

            @Override
            public void onError(String message) {
                showError(message != null ? message : "Invalid BIN");
                etScanBin.setText("");
                etScanBin.requestFocus();
            }
        });
    }

    private void validatePallet(final String pallet) {
        if (TextUtils.isEmpty(selectedGate)) {
            showError("Please select Gate Entry No first");
            etScanPallet.setText("");
            return;
        }
        if (TextUtils.isEmpty(validatedBin)) {
            showError("Please scan BIN first");
            etScanPallet.setText("");
            return;
        }
        if (TextUtils.isEmpty(PLANT)) {
            showError("Store ID not found");
            etScanPallet.setText("");
            return;
        }

        showProgress("Validating pallet...");
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_GATE_PALLATE_VALIDATE4_N);
            params.put("IM_USER", USER);
            params.put("IM_PLANT", PLANT);
            params.put("IM_GET", selectedGate);
            params.put("IM_PALETTE", pallet);
            params.put("IM_BIN", validatedBin);
        } catch (JSONException e) {
            dismissProgress();
            box.getErrBox(e);
            return;
        }

        postRfc(Vars.ZWM_GATE_PALLATE_VALIDATE4_N, params, new RfcCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                String message = getApiMessage(response, "Pallet picked successfully");
                String completedGate = selectedGate;
                clearFormAfterPick();
                removeGateEntryFromList(completedGate);
                hideKeyboard();
                int remaining = Math.max(0, gateEntryOptions.size() - 1);
                if (remaining == 0) {
                    showStatus(message + ". No open gate entries left.", true);
                } else {
                    showStatus(message + ". Select next Gate Entry No. (" + remaining + ")", true);
                }
            }

            @Override
            public void onError(String message) {
                showError(message != null ? message : "Invalid Pallet");
                etScanPallet.setText("");
                etScanPallet.requestFocus();
            }
        });
    }

    /** Clear Gate Entry / PO / BIN / Pallet / table after successful pick. */
    private void clearFormAfterPick() {
        selectedGate = "";
        validatedBin = "";
        resetPoDisplay();
        etScanBin.setText("");
        etBin.setText("");
        etScanPallet.setText("");
        etPallet.setText("");
        disableBinScan();
        disablePalletScan();
        buildTableHeader();
    }

    /** Remove completed gate entry from the dropdown list. */
    private void removeGateEntryFromList(String gateNo) {
        if (TextUtils.isEmpty(gateNo) || gateEntryOptions == null) {
            return;
        }
        ignoreSpinnerSelection = true;
        for (int i = gateEntryOptions.size() - 1; i >= 1; i--) {
            if (gateNo.equalsIgnoreCase(gateEntryOptions.get(i))) {
                gateEntryOptions.remove(i);
                break;
            }
        }
        if (gateEntryAdapter != null) {
            gateEntryAdapter.notifyDataSetChanged();
        }
        if (spGateEntry != null) {
            spGateEntry.setSelection(0);
        }
        ignoreSpinnerSelection = false;
    }

    private void populateTable(JSONArray etData) {
        buildTableHeader();
        if (etData == null || etData.length() == 0) {
            return;
        }

        Map<String, TableRowData> grouped = new LinkedHashMap<>();
        try {
            int start = SapJsonRows.startIndex(etData);
            for (int i = start; i < etData.length(); i++) {
                JSONObject row = etData.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String palette = row.optString("PALETTE", "").trim();
                String bin = row.optString("BIN", "").trim();
                String boxNo = row.optString("BOX_NO", "").trim();
                String key = palette + "|" + bin;

                TableRowData existing = grouped.get(key);
                if (existing == null) {
                    existing = new TableRowData(palette, bin);
                    grouped.put(key, existing);
                }
                existing.addBox(boxNo);
            }
        } catch (JSONException e) {
            showError("Failed to parse table data.");
            return;
        }

        int rowNum = 1;
        int textSize = 13;
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);

        for (TableRowData data : grouped.values()) {
            TableRow tr = new TableRow(activity);
            tr.setLayoutParams(trParams);
            tr.addView(createCell(String.valueOf(rowNum++), textSize, false));
            tr.addView(createCell(data.palette, textSize, false));
            tr.addView(createCell(data.bin, textSize, false));
            tr.addView(createCell(data.getBoxDisplay(), textSize, false));
            tableData.addView(tr, trParams);
        }
    }

    private void buildTableHeader() {
        tableData.removeAllViews();
        int headerTextSize = 13;
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);

        TableRow header = new TableRow(activity);
        header.setLayoutParams(trParams);
        header.addView(createCell("Sr.", headerTextSize, true));
        header.addView(createCell("Pallet", headerTextSize, true));
        header.addView(createCell("BIN", headerTextSize, true));
        header.addView(createCell("BOX NO.", headerTextSize, true));
        tableData.addView(header, trParams);
    }

    private TextView createCell(String text, int textSize, boolean header) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(4, 4, 4, 4);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        if (header) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        } else {
            tv.setBackground(getResources().getDrawable(R.drawable.table_cell_border));
        }
        return tv;
    }

    private void resetPoDisplay() {
        tvPoNo.setText(PLACEHOLDER_PO);
        tvPoNo.setTextColor(0xFF888888);
    }

    private void setPoDisplay(String poNo) {
        if (TextUtils.isEmpty(poNo)) {
            resetPoDisplay();
        } else {
            tvPoNo.setText(poNo);
            tvPoNo.setTextColor(0xFF000000);
        }
    }

    private void resetBinAndPalletFields() {
        validatedBin = "";
        etBin.setText("");
        etPallet.setText("");
        etScanBin.setText("");
        etScanPallet.setText("");
        disablePalletScan();
        if (!TextUtils.isEmpty(selectedGate)) {
            enableBinScan();
        } else {
            disableBinScan();
        }
    }

    private void enableBinScan() {
        etScanBin.setEnabled(true);
        etScanBin.setBackgroundResource(R.drawable.border);
    }

    private void disableBinScan() {
        etScanBin.setEnabled(false);
        etScanBin.setBackgroundResource(R.drawable.border_disabled_input);
    }

    private void enablePalletScan() {
        etScanPallet.setEnabled(true);
        etScanPallet.setBackgroundResource(R.drawable.border);
    }

    private void disablePalletScan() {
        etScanPallet.setEnabled(false);
        etScanPallet.setBackgroundResource(R.drawable.border_disabled_input);
    }

    private void hideKeyboard() {
        if (activity == null || etScanBin == null) {
            return;
        }
        etScanBin.clearFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etScanBin.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.lot_btn_back) {
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

    private void postRfc(final String rfcName, JSONObject params, final RfcCallback callback) {
        String rfcUrl = GatewayUrls.noAclJsonRfcUrl(URL, rfcName);
        if (rfcUrl.isEmpty()) {
            callback.onError("Server URL missing. Please log in again.");
            return;
        }
        Log.i(TAG, "RFCrequest->" + rfcName);
        Log.i(TAG, "RFCurl->" + rfcUrl);
        Log.i(TAG, "RFCpayload->" + params);
        JsonObjectRequest req = new SapJsonObjectRequest(
                Request.Method.POST, rfcUrl, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissProgress();
                        Log.i(TAG, "RFCresponse->" + rfcName + ": " + response);
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
                        Log.e(TAG, "RFCerror->" + rfcName + ": " + parseVolleyError(error), error);
                        callback.onError("Network error: " + parseVolleyError(error));
                    }
                });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        queue.add(req);
    }

    private boolean isApiSuccess(JSONObject response) {
        if (response == null) {
            return false;
        }
        // LIST4 may return blank EX_RETURN.TYPE with valid ET_DATA rows
        JSONArray table = getEtDataArray(response);
        if (table != null && table.length() > 0) {
            return true;
        }
        if (response.has("Status")) {
            return response.optBoolean("Status", false);
        }
        JSONObject ret = response.optJSONObject("EX_RETURN");
        if (ret != null) {
            String type = ret.optString("TYPE", "").trim();
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

    /**
     * LIST4 / DATA4 return {@code ET_DATA} (EDOCNO, PALETTE, BIN…).
     * Prefer ET_* first; skip empty arrays so blank IT_DATA does not hide ET_DATA.
     */
    private JSONArray getEtDataArray(JSONObject response) {
        if (response == null) {
            return null;
        }
        JSONArray arr = firstNonEmptyArray(response,
                "ET_DATA", "ET_Data", "IT_DATA", "IT_Data");
        if (arr != null) {
            return arr;
        }
        JSONObject data = response.optJSONObject("Data");
        if (data != null) {
            return firstNonEmptyArray(data,
                    "ET_DATA", "ET_Data", "IT_DATA", "IT_Data");
        }
        return null;
    }

    private JSONArray firstNonEmptyArray(JSONObject obj, String... keys) {
        if (obj == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            JSONArray arr = obj.optJSONArray(key);
            if (arr != null && arr.length() > 0) {
                return arr;
            }
        }
        // Prefer a present empty array over null (caller may still handle empty)
        for (String key : keys) {
            JSONArray arr = obj.optJSONArray(key);
            if (arr != null) {
                return arr;
            }
        }
        return null;
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

    private static class TableRowData {
        final String palette;
        final String bin;
        final List<String> boxes = new ArrayList<>();

        TableRowData(String palette, String bin) {
            this.palette = palette;
            this.bin = bin;
        }

        void addBox(String boxNo) {
            if (!TextUtils.isEmpty(boxNo)) {
                boxes.add(boxNo);
            }
        }

        String getBoxDisplay() {
            if (boxes.isEmpty()) {
                return "";
            }
            if (boxes.size() == 1) {
                return boxes.get(0);
            }
            return String.valueOf(boxes.size());
        }
    }
}
