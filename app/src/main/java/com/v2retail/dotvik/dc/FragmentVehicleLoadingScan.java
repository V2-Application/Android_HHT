package com.v2retail.dotvik.dc;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.volley.AuthFailureError;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
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

public class FragmentVehicleLoadingScan extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentVehicleLoadingScan.class.getSimpleName();
    private static final int REQUEST_SAVE_SCANNED_HU = 1603;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";
    private String hub = "";

    private EditText txtVehicleNo;
    private EditText txtTotalHu;
    private EditText txtScannedHu;
    private EditText txtPendingHu;
    private EditText txtScanHu;
    private EditText txtHuDisplay;
    private RadioGroup rgMode;
    private RadioButton rbScan;
    private Button btnBack;
    private TableLayout tableItems;
    private TextView tvNoData;

    private final Map<String, HuRow> allHus = new LinkedHashMap<>();
    private final Map<String, HuRow> scannedHus = new LinkedHashMap<>();

    public FragmentVehicleLoadingScan() {
    }

    public static FragmentVehicleLoadingScan newInstance(Bundle args) {
        FragmentVehicleLoadingScan fragment = new FragmentVehicleLoadingScan();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
        if (getArguments() != null) {
            hub = getArguments().getString(FragmentVehicleLoading.ARG_HUB, "");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Vehicle Loading");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_vehicle_loading_scan, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtVehicleNo = rootView.findViewById(R.id.txt_vehicle_loading_scan_vehicle_no);
        txtTotalHu = rootView.findViewById(R.id.txt_vehicle_loading_scan_total_hu);
        txtScannedHu = rootView.findViewById(R.id.txt_vehicle_loading_scan_scanned_hu);
        txtPendingHu = rootView.findViewById(R.id.txt_vehicle_loading_scan_pending_hu);
        txtScanHu = rootView.findViewById(R.id.txt_vehicle_loading_scan_hu);
        txtHuDisplay = rootView.findViewById(R.id.txt_vehicle_loading_scan_hu_display);
        rgMode = rootView.findViewById(R.id.rg_vehicle_loading_scan_mode);
        rbScan = rootView.findViewById(R.id.rb_vehicle_loading_scan_mode_scan);
        tableItems = rootView.findViewById(R.id.table_vehicle_loading_scan);
        tvNoData = rootView.findViewById(R.id.tv_vehicle_loading_scan_no_data);
        btnBack = rootView.findViewById(R.id.btn_vehicle_loading_scan_back);

        btnBack.setOnClickListener(this);

        if (getArguments() != null) {
            String plant = getArguments().getString(FragmentVehicleLoading.ARG_PLANT, "");
            if (!plant.isEmpty()) {
                WERKS = plant;
            }
            txtVehicleNo.setText(getArguments().getString(FragmentVehicleLoading.ARG_VEHICLE_NO, ""));
            loadHuDataFromBundle(getArguments().getString(FragmentVehicleLoading.ARG_HU_LIST, ""));
        }

        updateCounts();
        addInputEvents();

        return rootView;
    }

    private void loadHuDataFromBundle(String huListJson) {
        try {
            allHus.clear();
            scannedHus.clear();
            if (huListJson == null || huListJson.isEmpty()) {
                populateTableData();
                return;
            }

            JSONArray arr = new JSONArray(huListJson);
            for (int i = 0; i < arr.length(); i++) {
                HuRow row = new Gson().fromJson(arr.get(i).toString(), HuRow.class);
                row.applyDefaults(hub);
                String key = row.getHuKey();
                if (!key.isEmpty()) {
                    allHus.put(key, row);
                    if (row.isScanned()) {
                        scannedHus.put(key, HuRow.copyOf(row));
                    }
                }
            }

            updateCounts();
            populateTableData();
            txtScanHu.requestFocus();
        } catch (Exception e) {
            box.getErrBox(e);
        }
    }

    private void addInputEvents() {
        txtScanHu.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txtScanHu);
                if (!value.isEmpty()) {
                    processHuScan(value);
                    return true;
                }
            }
            return false;
        });

        txtScanHu.addTextChangedListener(new TextWatcher() {
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
                    processHuScan(value);
                }
            }
        });
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        box.getBox(title, message);
    }

    @SuppressLint("DefaultLocale")
    private void updateCounts() {
        txtTotalHu.setText(String.format("%d", allHus.size()));
        txtScannedHu.setText(String.format("%d", scannedHus.size()));
        txtPendingHu.setText(String.format("%d", allHus.size() - scannedHus.size()));
    }

    private boolean isScanMode() {
        return rbScan.isChecked();
    }

    private void processHuScan(String hu) {
        hu = UIFuncs.removeLeadingZeros(hu);
        if (hu.isEmpty()) {
            showError("Required", "Please scan HU.");
            return;
        }

        HuRow row;
        if (isScanMode()) {
            if (scannedHus.containsKey(hu)) {
                showError("Already Scanned", "HU " + hu + " is already scanned.");
                clearScanInput();
                return;
            }
            if (!allHus.containsKey(hu)) {
                showError("Invalid", "Scanned HU " + hu + " is not in the list.");
                clearScanInput();
                return;
            }
            row = allHus.get(hu);
        } else {
            if (!scannedHus.containsKey(hu)) {
                showError("Not Scanned", "HU " + hu + " is not in scanned list.");
                clearScanInput();
                return;
            }
            row = scannedHus.get(hu);
        }

        saveScannedHu(hu, row, isScanMode());
    }

    private void saveScannedHu(String hu, HuRow row, boolean scanMode) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_SAVE_SCANNEDHULIST_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_VEHICLE", UIFuncs.toUpperTrim(txtVehicleNo));
            if (!scanMode) {
                args.put("IM_REMOVE", "X");
            }

            JSONArray huList = new JSONArray();
            huList.put(buildHuListItem(row, scanMode));
            args.put("HU_LIST", huList);

            txtScanHu.setEnabled(false);
            showProcessingAndSubmit(Vars.ZWM_SAVE_SCANNEDHULIST_RFC, REQUEST_SAVE_SCANNED_HU, args, hu, scanMode);
        } catch (JSONException e) {
            box.getErrBox(e);
            clearScanInput();
        }
    }

    private JSONObject buildHuListItem(HuRow row, boolean scanMode) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("SRC_STORE", safe(row.srcStore));
        item.put("DST_STORE", safe(row.dstStore));
        item.put("LRNO", safe(row.lrno));
        item.put("EXTERNAL_HU", safe(row.externalHu));
        item.put("INTERNAL_HU", safe(row.internalHu));
        item.put("QUANTITY", safe(row.quantity));
        item.put("PALETTE", safe(row.palette));
        item.put("CLA_BIN", safe(row.claBin));
        item.put("DCLA_STATUS", safe(row.dclaStatus));
        item.put("UOM", safe(row.uom));
        item.put("SCAN", scanMode ? "" : "X");
        return item;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args,
                                        String hu, boolean scanMode) {
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                submitRequest(rfc, request, args, hu, scanMode);
            } catch (Exception e) {
                dismissDialog();
                txtScanHu.setEnabled(true);
                box.getErrBox(e);
                clearScanInput();
            }
        }, 500);
    }

    private void submitRequest(String rfc, int request, JSONObject args,
                               String hu, boolean scanMode) {
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        final JSONObject params = args;

        Log.d(TAG, "payload -> " + params);

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, responseBody -> {
            dismissDialog();
            txtScanHu.setEnabled(true);
            Log.d(TAG, "response -> " + responseBody);

            if (responseBody == null || responseBody.length() == 0) {
                showError("Err", "No response from Server");
                clearScanInput();
                return;
            }

            try {
                if (!isEtErrorSuccess(responseBody)) {
                    showError("Err", getEtErrorMessage(responseBody));
                    clearScanInput();
                    return;
                }

                if (responseBody.has("EX_RETURN") && responseBody.get("EX_RETURN") instanceof JSONObject) {
                    JSONObject returnObj = responseBody.getJSONObject("EX_RETURN");
                    if ("E".equals(returnObj.optString("TYPE", ""))) {
                        showError("Err", returnObj.optString("MESSAGE", "Error"));
                        clearScanInput();
                        return;
                    }
                }

                if (request == REQUEST_SAVE_SCANNED_HU) {
                    applyLocalScanState(hu, scanMode);
                }
            } catch (JSONException e) {
                box.getErrBox(e);
                clearScanInput();
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

        mRequestQueue.add(mJsonRequest);
    }

    private void applyLocalScanState(String hu, boolean scanMode) {
        if (scanMode) {
            HuRow row = allHus.get(hu);
            if (row != null) {
                scannedHus.put(hu, HuRow.copyOf(row));
                txtHuDisplay.setText(hu);
            }
        } else {
            scannedHus.remove(hu);
            txtHuDisplay.setText("");
        }
        updateCounts();
        populateTableData();
        clearScanInput();
    }

    private void clearScanInput() {
        txtScanHu.setText("");
        txtScanHu.requestFocus();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private boolean isEtErrorSuccess(JSONObject responseBody) throws JSONException {
        if (!responseBody.has("ET_ERROR")) {
            return true;
        }
        Object etError = responseBody.get("ET_ERROR");
        if (etError instanceof JSONObject) {
            String errorNumber = ((JSONObject) etError).optString("NUMBER", "").trim();
            return errorNumber.isEmpty() || "000".equals(errorNumber) || "0".equals(errorNumber);
        }
        String errorCode = String.valueOf(etError).trim();
        return errorCode.isEmpty() || "000".equals(errorCode) || "0".equals(errorCode);
    }

    private String getEtErrorMessage(JSONObject responseBody) throws JSONException {
        if (responseBody.has("ET_ERROR") && responseBody.get("ET_ERROR") instanceof JSONObject) {
            JSONObject etError = responseBody.getJSONObject("ET_ERROR");
            String message = etError.optString("MESSAGE", "").trim();
            if (!message.isEmpty()) {
                return message;
            }
            String number = etError.optString("NUMBER", "").trim();
            if (!number.isEmpty()) {
                return "Error: " + number;
            }
        }
        return "Unable to save scanned HU.";
    }

    private Response.ErrorListener volleyErrorListener() {
        return error -> {
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

            dismissDialog();
            txtScanHu.setEnabled(true);
            box.getBox("Err", err);
            clearScanInput();
        };
    }

    private void populateTableData() {
        tableItems.removeAllViews();

        List<HuRow> rows = new ArrayList<>(allHus.values());
        if (rows.isEmpty()) {
            tvNoData.setVisibility(View.VISIBLE);
            return;
        }
        tvNoData.setVisibility(View.GONE);

        int textSize = 13;
        for (HuRow row : rows) {
            TableRow tableRow = new TableRow(con);
            tableRow.setPadding(0, 0, 0, 0);

            String key = row.getHuKey();
            boolean scanned = scannedHus.containsKey(key);
            TextView tvHub = createCell(row.hub, textSize, scanned);
            TextView tvStore = createCell(row.store, textSize, scanned);
            TextView tvHu = createCell(key, textSize, scanned);
            TextView tvQty = createCell(row.huQty, textSize, scanned);

            tableRow.addView(tvHub);
            tableRow.addView(tvStore);
            tableRow.addView(tvHu);
            tableRow.addView(tvQty);
            tableItems.addView(tableRow);
        }
    }

    private TextView createCell(String text, int textSize, boolean scanned) {
        TextView tv = new TextView(con);
        tv.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(5, 4, 5, 4);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        tv.setTextColor(Color.BLACK);
        tv.setText(text == null ? "" : text);
        tv.setBackground(getResources().getDrawable(
                scanned ? R.drawable.table_cell_border_highlight : R.drawable.table_cell_border));
        return tv;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_vehicle_loading_scan_back) {
            goBackWithLoading();
        }
    }

    private void goBackWithLoading() {
        if (fm == null) {
            return;
        }
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        fm.popBackStack();
    }

    @Override
    public void onDestroyView() {
        dismissDialog();
        super.onDestroyView();
    }

    static class HuRow {
        @SerializedName("SRC_STORE")
        String srcStore = "";
        @SerializedName("DST_STORE")
        String dstStore = "";
        @SerializedName("LRNO")
        String lrno = "";
        @SerializedName("EXTERNAL_HU")
        String externalHu = "";
        @SerializedName("INTERNAL_HU")
        String internalHu = "";
        @SerializedName("QUANTITY")
        String quantity = "";
        @SerializedName("PALETTE")
        String palette = "";
        @SerializedName("CLA_BIN")
        String claBin = "";
        @SerializedName("DCLA_STATUS")
        String dclaStatus = "";
        @SerializedName("UOM")
        String uom = "";
        @SerializedName("SCAN")
        String scan = "";

        String hub = "";
        String store = "";
        String huNumber = "";
        String huQty = "";

        void applyDefaults(String defaultHub) {
            hub = defaultHub;
            if (hub.isEmpty()) {
                hub = srcStore;
            }
            store = dstStore;
            huNumber = externalHu;
            if (huNumber.isEmpty()) {
                huNumber = internalHu;
            }
            huQty = formatQty(quantity);
            if (uom.isEmpty()) {
                uom = "EA";
            }
        }

        boolean isScanned() {
            return "X".equalsIgnoreCase(scan);
        }

        String getHuKey() {
            return UIFuncs.removeLeadingZeros(huNumber);
        }

        private static String formatQty(String qty) {
            if (qty == null || qty.isEmpty()) {
                return "";
            }
            try {
                double value = Double.parseDouble(qty.trim());
                if (value == Math.rint(value)) {
                    return String.valueOf((long) value);
                }
            } catch (NumberFormatException ignored) {
            }
            return qty.trim();
        }

        static HuRow copyOf(HuRow source) {
            HuRow copy = new HuRow();
            copy.srcStore = source.srcStore;
            copy.dstStore = source.dstStore;
            copy.lrno = source.lrno;
            copy.externalHu = source.externalHu;
            copy.internalHu = source.internalHu;
            copy.quantity = source.quantity;
            copy.palette = source.palette;
            copy.claBin = source.claBin;
            copy.dclaStatus = source.dclaStatus;
            copy.uom = source.uom;
            copy.scan = source.scan;
            copy.hub = source.hub;
            copy.store = source.store;
            copy.huNumber = source.huNumber;
            copy.huQty = source.huQty;
            return copy;
        }
    }
}
