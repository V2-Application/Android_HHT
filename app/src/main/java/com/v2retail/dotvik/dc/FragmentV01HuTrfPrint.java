package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
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
import android.widget.Toast;

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
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FragmentV01HuTrfPrint extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentV01HuTrfPrint.class.getName();
    private static final int REQUEST_GET_HU = 1601;
    private static final int REQUEST_PRINT_HU = 1602;

    View rootView;
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    SharedPreferencesData data;

    String URL = "";
    String USER = "";

    Button btn_reset, btn_save;
    EditText txt_printer, txt_scan_po, txt_scan_dplant, txt_scan_crate;

    String tvsprinter;
    boolean requestInFlight = false;

    public FragmentV01HuTrfPrint() {
    }

    public static FragmentV01HuTrfPrint newInstance() {
        return new FragmentV01HuTrfPrint();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("V01 HU TRF PRINT");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_v01_hu_trf_print, container, false);
        con = getContext();
        box = new AlertBox(con);
        data = new SharedPreferencesData(con);
        URL = data.read("URL");
        USER = data.read("USER");

        txt_printer = rootView.findViewById(R.id.txt_v01_hu_trf_printer);
        txt_scan_po = rootView.findViewById(R.id.txt_v01_hu_trf_scan_po);
        txt_scan_dplant = rootView.findViewById(R.id.txt_v01_hu_trf_scan_dplant);
        txt_scan_crate = rootView.findViewById(R.id.txt_v01_hu_trf_scan_crate);

        btn_reset = rootView.findViewById(R.id.btn_v01_hu_trf_reset);
        btn_save = rootView.findViewById(R.id.btn_v01_hu_trf_save);

        btn_reset.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        loadSavedPrinter();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_v01_hu_trf_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                });
                break;
            case R.id.btn_v01_hu_trf_save:
                fetchHu();
                break;
        }
    }

    private void addInputEvents() {
        bindScanField(txt_printer, () -> {
            String value = UIFuncs.toUpperTrim(txt_printer);
            if (!value.isEmpty()) {
                validatePrinter(value, false);
            }
        });
        bindScanField(txt_scan_dplant, () -> acceptScan(txt_scan_dplant, txt_scan_po));
        bindScanField(txt_scan_po, () -> acceptScan(txt_scan_po, txt_scan_crate));
        bindScanField(txt_scan_crate, () -> {
            String value = UIFuncs.toUpperTrim(txt_scan_crate);
            if (value.isEmpty()) {
                return;
            }
            UIFuncs.hideKeyboard(getActivity());
            fetchHu();
        });
    }

    private void bindScanField(EditText field, Runnable onScan) {
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    onScan.run();
                    return true;
                }
                return false;
            }
        });
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
                if (!s.toString().toUpperCase().trim().isEmpty() && scannerReading) {
                    scannerReading = false;
                    onScan.run();
                }
            }
        });
    }

    private void loadSavedPrinter() {
        String defaultPrinter = data.read(Vars.TVS_PRINTER);
        if (defaultPrinter != null && !defaultPrinter.isEmpty()) {
            txt_printer.setText(defaultPrinter);
            validatePrinter(defaultPrinter, true);
        } else {
            UIFuncs.disableInput(con, txt_scan_dplant);
            UIFuncs.disableInput(con, txt_scan_po);
            UIFuncs.disableInput(con, txt_scan_crate);
            txt_printer.requestFocus();
        }
    }

    private void validatePrinter(String printerName, boolean isSilentCheck) {
        TSPLPrinter printerHelper = new TSPLPrinter(con);
        if (!printerHelper.findBluetoothPrinterByScan(printerName)) {
            if (!isSilentCheck) {
                box.getBox("Not Paired", "Scanned printer ( " + printerName + " ) is not paired with this device.");
            }
            this.tvsprinter = null;
            txt_printer.setText("");
            txt_printer.requestFocus();
            UIFuncs.disableInput(con, txt_scan_dplant);
            UIFuncs.disableInput(con, txt_scan_po);
            UIFuncs.disableInput(con, txt_scan_crate);
            return;
        }
        String resolved = printerHelper.getPrinterName();
        data.write(Vars.TVS_PRINTER, resolved);
        this.tvsprinter = resolved;
        txt_printer.setText(resolved);
        UIFuncs.enableInput(con, txt_scan_dplant);
        UIFuncs.disableInput(con, txt_scan_po);
        UIFuncs.disableInput(con, txt_scan_crate);
    }

    private void acceptScan(EditText current, EditText next) {
        String value = UIFuncs.toUpperTrim(current);
        if (value.isEmpty()) {
            return;
        }
        UIFuncs.hideKeyboard(getActivity());
        UIFuncs.disableInput(con, current);
        UIFuncs.enableInput(con, next);
    }

    private void clear() {
        txt_scan_dplant.setText("");
        txt_scan_po.setText("");
        txt_scan_crate.setText("");
        UIFuncs.disableInput(con, txt_scan_po);
        UIFuncs.disableInput(con, txt_scan_crate);
        if (tvsprinter != null && !tvsprinter.isEmpty()) {
            UIFuncs.enableInput(con, txt_scan_dplant);
        } else {
            UIFuncs.disableInput(con, txt_scan_dplant);
            txt_printer.requestFocus();
        }
    }

    private void fetchHu() {
        if (requestInFlight) {
            return;
        }
        String printer = tvsprinter != null ? tvsprinter : UIFuncs.toUpperTrim(txt_printer);
        String po = UIFuncs.toUpperTrim(txt_scan_po);
        String dplant = UIFuncs.toUpperTrim(txt_scan_dplant);
        String crate = UIFuncs.toUpperTrim(txt_scan_crate);

        if (printer.isEmpty()) {
            box.getBox("Alert", "Please scan TVS printer");
            txt_printer.requestFocus();
            return;
        }
        if (dplant.isEmpty()) {
            box.getBox("Alert", "Please scan D. Plant");
            UIFuncs.enableInput(con, txt_scan_dplant);
            return;
        }
        if (po.isEmpty()) {
            box.getBox("Alert", "Please scan PO No");
            UIFuncs.enableInput(con, txt_scan_po);
            return;
        }
        if (crate.isEmpty()) {
            box.getBox("Alert", "Please scan Crate");
            UIFuncs.enableInput(con, txt_scan_crate);
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_V01_HUTRF_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", dplant);
            args.put("IM_PO", po);
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_V01_HUTRF_RFC, REQUEST_GET_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void printHu(String hu) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_TVS_HU_PRINT_2);
            args.put("IM_USER", USER);
            args.put("IM_EXIDV", hu);
            showProcessingAndSubmit(Vars.ZWM_PTL_TVS_HU_PRINT_2, REQUEST_PRINT_HU, args);
        } catch (JSONException e) {
            requestInFlight = false;
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void sendToPrinter(JSONObject huObj) {
        TSPLPrinter printer = new TSPLPrinter(getContext(), Vars.PTL_NEW_MODULE_HU_CLOSE);
        printer.sendPrintCommandToBluetoothPrinter(this.tvsprinter, huObj, "1");
    }

    private void onHuReceived(JSONObject responsebody) {
        String hu = readHuExport(responsebody);
        if (hu.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "HU not found for this PO / D. Plant / Crate");
            resetCrateInput();
            return;
        }
        printHu(hu);
    }

    private void onPrintDataReceived(JSONObject responsebody) {
        JSONObject huObj = extractHuData(responsebody);
        if (huObj != null) {
            sendToPrinter(huObj);
            Toast.makeText(con, "Details sent to printer " + tvsprinter, Toast.LENGTH_SHORT).show();
            txt_scan_crate.setText("");
            UIFuncs.enableInput(con, txt_scan_crate);
            return;
        }
        UIFuncs.errorSound(con);
        box.getBox("Err", "Print data not received for HU");
        resetCrateInput();
    }

    private static JSONObject extractHuData(JSONObject responsebody) {
        JSONObject fromEx = firstHuDataRow(responsebody == null ? null : responsebody.opt("EX_HUDATA"));
        if (fromEx != null) {
            return fromEx;
        }
        return firstHuDataRow(responsebody == null ? null : responsebody.opt("ET_HUDATA"));
    }

    private static JSONObject firstHuDataRow(Object raw) {
        if (raw instanceof JSONObject) {
            JSONObject row = (JSONObject) raw;
            return isHuDataHeaderRow(row) ? null : row;
        }
        if (!(raw instanceof JSONArray)) {
            return null;
        }
        JSONArray arr = (JSONArray) raw;
        int start = tableStartIndex(arr, "DATUM", "SWERKS", "VEMNG", "DWERKS", "HHT_ID");
        for (int i = start; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row != null && !isHuDataHeaderRow(row)) {
                return row;
            }
        }
        return null;
    }

    private static boolean isHuDataHeaderRow(JSONObject row) {
        return SapJsonRows.isMetadataRow(row, "DATUM", "SWERKS", "VEMNG", "DWERKS", "HHT_ID");
    }

    private static String readHuExport(JSONObject obj) {
        if (obj == null) {
            return "";
        }
        String[] keys = {"EX_HU", "SAP_HU", "EXIDV", "HU"};
        for (String key : keys) {
            if (!obj.has(key) || obj.isNull(key)) {
                continue;
            }
            try {
                String hu = huFromValue(obj.get(key));
                if (!hu.isEmpty()) {
                    return hu;
                }
            } catch (JSONException ignored) {
            }
        }
        return "";
    }

    private static String huFromValue(Object raw) {
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            int start = tableStartIndex(arr, "EXIDV", "SAP_HU", "HU");
            for (int i = start; i < arr.length(); i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row != null) {
                    if (SapJsonRows.isMetadataRow(row, "EXIDV", "SAP_HU", "HU")) {
                        continue;
                    }
                    String nestedHu = huFromObject(row);
                    if (!nestedHu.isEmpty()) {
                        return nestedHu;
                    }
                    continue;
                }
                String text = normalizeHuText(arr.optString(i, ""));
                if (!text.isEmpty()) {
                    return text;
                }
            }
            return "";
        }
        if (raw instanceof JSONObject) {
            JSONObject nested = (JSONObject) raw;
            if (SapJsonRows.isMetadataRow(nested, "EXIDV", "SAP_HU", "HU")) {
                return "";
            }
            return huFromObject(nested);
        }
        return normalizeHuText(String.valueOf(raw));
    }

    private static String huFromObject(JSONObject nested) {
        String nestedHu = nested.optString("EXIDV",
                nested.optString("SAP_HU", nested.optString("HU", ""))).trim();
        return normalizeHuText(nestedHu);
    }

    private static String normalizeHuText(String text) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)
                || "EXIDV".equalsIgnoreCase(value)
                || "SAP_HU".equalsIgnoreCase(value)
                || "HU".equalsIgnoreCase(value)) {
            return "";
        }
        return UIFuncs.removeLeadingZeros(value);
    }

    /**
     * Header present at index 0 → start from 1. No header → start from 0.
     * Primitive arrays (not JSON objects) always start at 0.
     */
    private static int tableStartIndex(JSONArray arr, String... fieldKeys) {
        if (arr == null || arr.length() <= 1) {
            return 0;
        }
        JSONObject first = arr.optJSONObject(0);
        if (first == null) {
            return 0;
        }
        try {
            return SapJsonRows.startIndex(arr, fieldKeys);
        } catch (JSONException ignored) {
            return SapJsonRows.isMetadataRow(first, fieldKeys) ? 1 : 0;
        }
    }

    private void resetCrateInput() {
        txt_scan_crate.setText("");
        UIFuncs.enableInput(con, txt_scan_crate);
    }

    private boolean isReturnError(JSONObject responsebody) {
        JSONObject returnObj = extractReturn(responsebody);
        if (returnObj == null) {
            return false;
        }
        String type = returnObj.optString("TYPE", "").trim();
        return "E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type);
    }

    private String getReturnMessage(JSONObject responsebody) {
        JSONObject returnObj = extractReturn(responsebody);
        if (returnObj != null) {
            String message = returnObj.optString("MESSAGE", "").trim();
            if (!message.isEmpty()) {
                return message;
            }
        }
        return "Request failed.";
    }

    private static JSONObject extractReturn(JSONObject responsebody) {
        if (responsebody == null || !responsebody.has("EX_RETURN") || responsebody.isNull("EX_RETURN")) {
            return null;
        }
        try {
            Object raw = responsebody.get("EX_RETURN");
            if (raw instanceof JSONObject) {
                return (JSONObject) raw;
            }
            if (raw instanceof JSONArray) {
                JSONArray arr = (JSONArray) raw;
                int start = tableStartIndex(arr, "TYPE", "MESSAGE");
                JSONObject firstData = null;
                for (int i = start; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row == null || SapJsonRows.isMetadataRow(row, "TYPE", "MESSAGE")) {
                        continue;
                    }
                    String type = row.optString("TYPE", "").trim();
                    if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                        return row;
                    }
                    if (firstData == null) {
                        firstData = row;
                    }
                }
                return firstData;
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        requestInFlight = true;
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                requestInFlight = false;
                dismissDialog();
                box.getErrBox(e);
            }
        }, 500);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = GatewayUrls.noAclJsonRfcUrl(URL, rfc);
        if (url.isEmpty()) {
            requestInFlight = false;
            dismissDialog();
            UIFuncs.errorSound(con);
            box.getBox("Err", "Server URL missing. Please log in again.");
            return;
        }

        final JSONObject params = args;
        Log.d(TAG, "url -> " + url);
        Log.d(TAG, "payload -> " + params);

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        requestInFlight = false;
                        dismissDialog();
                        Log.d(TAG, "response -> " + responsebody);

                        if (responsebody == null || responsebody.length() == 0) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from Server");
                            resetCrateInput();
                            return;
                        }
                        if (isReturnError(responsebody)) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", getReturnMessage(responsebody));
                            resetCrateInput();
                            return;
                        }
                        if (request == REQUEST_GET_HU) {
                            onHuReceived(responsebody);
                        } else if (request == REQUEST_PRINT_HU) {
                            onPrintDataReceived(responsebody);
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
            public void retry(VolleyError error) {
            }
        });
        mRequestQueue.add(mJsonRequest);
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestInFlight = false;
                Log.i(TAG, "Error :" + error.toString());
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
                UIFuncs.errorSound(con);
                box.getBox("Err", err);
                resetCrateInput();
            }
        };
    }
}
