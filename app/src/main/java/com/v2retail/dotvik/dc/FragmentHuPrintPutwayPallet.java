package com.v2retail.dotvik.dc;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HU Print &amp; Putway to Pallet.
 * Printer, vehicle, and PO stay after Save.
 * Save clears Scan Pallet, Pallet, Box No, HU.NO, and Scan HUNo.
 * A validated HU is shown in HU.NO and the scan box is cleared.
 */
public class FragmentHuPrintPutwayPallet extends Fragment {

    private static final String TAG = "HuPrintPutwayPallet";
    private static final String BOX_SELECT = "Select";

    private EditText etPrinter;
    private EditText etVehicle;
    private EditText etPo;
    private EditText etScanPallet;
    private EditText etPallet;
    private Spinner spBoxNo;
    private EditText etHuNo;
    private EditText etScanHu;
    private EditText etBillNo;
    private EditText etWeight;
    private AlertBox box;
    private Context con;
    private ProgressDialog dialog;
    private SharedPreferencesData data;
    private String url = "";
    private String tvsPrinter;
    private boolean suppressScan;
    private boolean suppressBoxEvent;
    private boolean requestInProgress;
    private String lastExtHu = "";
    private final List<String> boxItems = new ArrayList<>();

    public FragmentHuPrintPutwayPallet() {
    }

    public static FragmentHuPrintPutwayPallet newInstance() {
        return new FragmentHuPrintPutwayPallet();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("HU Print & Putway to Pallet");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hu_print_putway_pallet, container, false);
        con = getContext();
        box = new AlertBox(con);
        data = new SharedPreferencesData(con);

        etPrinter = view.findViewById(R.id.et_scan_printer);
        etVehicle = view.findViewById(R.id.et_scan_vehicle);
        etPo = view.findViewById(R.id.et_scan_po);
        etScanPallet = view.findViewById(R.id.et_scan_pallet);
        etPallet = view.findViewById(R.id.et_pallet);
        spBoxNo = view.findViewById(R.id.sp_box_no);
        etHuNo = view.findViewById(R.id.et_hu_no);
        etScanHu = view.findViewById(R.id.et_scan_hu);
        etBillNo = view.findViewById(R.id.et_bill_no);
        etWeight = view.findViewById(R.id.et_weight);
        Button btnReset = view.findViewById(R.id.btn_reset);
        Button btnSave = view.findViewById(R.id.btn_save);

        bindScan(etPrinter, new Runnable() {
            @Override
            public void run() {
                detectPrinter(etPrinter.getText().toString());
            }
        });
        bindScan(etVehicle, new Runnable() {
            @Override
            public void run() {
                focus(etPo);
            }
        });
        bindScan(etPo, new Runnable() {
            @Override
            public void run() {
                validatePo(etPo.getText().toString());
            }
        });
        bindScan(etScanPallet, new Runnable() {
            @Override
            public void run() {
                acceptPallet(etScanPallet.getText().toString());
            }
        });
        bindScan(etScanHu, new Runnable() {
            @Override
            public void run() {
                acceptHu(etScanHu.getText().toString());
            }
        });

        boxItems.clear();
        boxItems.add(BOX_SELECT);
        ArrayAdapter<String> boxAdapter = new ArrayAdapter<>(
                con, android.R.layout.simple_spinner_item, boxItems);
        boxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBoxNo.setAdapter(boxAdapter);
        spBoxNo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (suppressBoxEvent) {
                    suppressBoxEvent = false;
                    return;
                }
                if (position > 0) {
                    printSelectedBox(boxItems.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAll();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        url = data.read("URL") != null ? data.read("URL").trim() : "";
        restorePrinter();
        return view;
    }

    private void validatePo(String raw) {
        final String po = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (po.isEmpty()) {
            etPo.requestFocus();
            return;
        }
        if (requestInProgress) {
            return;
        }
        if (url.isEmpty()) {
            box.getBox("Alert", "Server URL missing. Please log in again.");
            return;
        }

        setFieldText(etPo, po);
        resetBoxDropdown();
        requestInProgress = true;
        showProgress("Validating PO...");

        // This gateway does not bind JSON table params reliably (same as IM_PARMS save).
        // Use form-urlencoded so TABLES EX_DATA is returned with EX_RETURN.
        final Map<String, String> form = new HashMap<String, String>();
        form.put("bapiname", Vars.ZWM_VND_HU_PO_VLDT);
        form.put("IM_PO", po);

        String rfcUrl = GatewayUrls.noAclJsonRfcUrl(url, Vars.ZWM_VND_HU_PO_VLDT);
        if (rfcUrl.isEmpty()) {
            requestInProgress = false;
            dismissProgress();
            box.getBox("Alert", "Server URL missing. Please log in again.");
            return;
        }
        Log.d(TAG, "RFC request -> " + Vars.ZWM_VND_HU_PO_VLDT + " " + form);
        StringRequest req = new StringRequest(Request.Method.POST, rfcUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String body) {
                        requestInProgress = false;
                        dismissProgress();
                        Log.d(TAG, "RFC response -> " + Vars.ZWM_VND_HU_PO_VLDT + ": " + body);
                        JSONObject response;
                        try {
                            response = new JSONObject(body != null ? body : "{}");
                            SapJsonRows.sanitizeResponse(response);
                        } catch (JSONException e) {
                            resetBoxDropdown();
                            setFieldText(etPo, "");
                            box.getBox("Alert", "Parse error while validating PO.");
                            etPo.requestFocus();
                            return;
                        }

                        if (!isSapSuccess(response)) {
                            resetBoxDropdown();
                            setFieldText(etPo, "");
                            box.getBox("Alert", sapMessage(response, "PO not valid."));
                            etPo.requestFocus();
                            return;
                        }

                        List<String> boxes = boxNumbersFrom(exData(response));
                        if (boxes.isEmpty()) {
                            resetBoxDropdown();
                            setFieldText(etPo, "");
                            Log.e(TAG, "PO OK but no BOX_NO in EX_DATA -> " + response);
                            box.getBox("Alert", "No box found for this PO.");
                            etPo.requestFocus();
                            return;
                        }

                        setBoxNumbers(boxes);
                        focus(etScanPallet);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestInProgress = false;
                        dismissProgress();
                        resetBoxDropdown();
                        setFieldText(etPo, "");
                        box.getBox("Alert", error.getMessage() != null ? error.getMessage() : "Network error");
                        etPo.requestFocus();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                return form;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    /** BOX_NO values from EX_DATA (ZTT_PO_HU / ZSTR_PO_HU). */
    private List<String> boxNumbersFrom(JSONArray rows) {
        Set<String> unique = new LinkedHashSet<>();
        if (rows == null) {
            return new ArrayList<>();
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            if (SapJsonRows.isMetadataRow(row, "BOX_NO", "PO_NO", "EXT_HU")) {
                continue;
            }
            String boxNo = rowField(row, "BOX_NO", "BOXNO", "BOX");
            if (!boxNo.isEmpty()
                    && !"BOX_NO".equalsIgnoreCase(boxNo)
                    && !"BOXNO".equalsIgnoreCase(boxNo)
                    && !"BOX".equalsIgnoreCase(boxNo)) {
                unique.add(boxNo);
            }
        }
        return new ArrayList<>(unique);
    }

    /** Reads a field by exact or case-insensitive key. */
    private static String rowField(JSONObject row, String... keys) {
        if (row == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String value = row.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        Iterator<String> it = row.keys();
        while (it.hasNext()) {
            String actual = it.next();
            if (actual == null) {
                continue;
            }
            for (String key : keys) {
                if (key != null && actual.equalsIgnoreCase(key)) {
                    String value = row.optString(actual, "").trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return "";
    }

    private void setBoxNumbers(List<String> boxes) {
        boxItems.clear();
        boxItems.add(BOX_SELECT);
        boxItems.addAll(boxes);
        notifyBoxes();
        selectBox(0);
    }

    private void printSelectedBox(final String boxNo) {
        if (tvsPrinter == null || tvsPrinter.isEmpty()) {
            box.getBox("Alert", "Scan Printer first!");
            selectBox(0);
            return;
        }
        final String po = text(etPo);
        if (po.isEmpty()) {
            box.getBox("Alert", "Scan PO No!");
            selectBox(0);
            return;
        }
        if (requestInProgress) {
            return;
        }
        requestInProgress = true;
        showProgress("Printing HU...");

        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_VND_HU_PRINT);
            params.put("IM_PO", po);
            params.put("IM_BOX", boxNo);
        } catch (JSONException e) {
            requestInProgress = false;
            dismissProgress();
            selectBox(0);
            box.getBox("Alert", "Could not build print request.");
            return;
        }

        callRfc(Vars.ZWM_VND_HU_PRINT, params, new RfcCb() {
            @Override
            public void ok(JSONObject response) {
                requestInProgress = false;
                if (!isSapSuccess(response)) {
                    selectBox(0);
                    box.getBox("Alert", sapMessage(response, "Could not print this box."));
                    return;
                }
                final String hub = returnedHu(response);
                if (hub.isEmpty()) {
                    selectBox(0);
                    box.getBox("Alert", "HU not returned for this box.");
                    return;
                }
                lastExtHu = hub;
                etHuNo.setText(hub);
                setFieldText(etScanHu, "");
                printLabel(boxNo, hub);
            }

            @Override
            public void err(String message) {
                requestInProgress = false;
                selectBox(0);
                box.getBox("Alert", message != null ? message : "Network error");
            }
        });
    }

    private String returnedHu(JSONObject response) {
        String hu = response.optString("IM_HU", "").trim();
        if (!hu.isEmpty()) {
            return hu;
        }
        JSONObject data = response.optJSONObject("Data");
        if (data != null) {
            hu = data.optString("IM_HU", "").trim();
        }
        return hu;
    }

    private void printLabel(final String boxNo, final String hub) {
        final String printerName = tvsPrinter;
        showProgress("Printing...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = new TSPLPrinter(con).sendBoxHubPrintCommand(printerName, boxNo, hub);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgress();
                        if (!isAdded()) {
                            return;
                        }
                        if (ok) {
                            Toast.makeText(con, "Printed " + boxNo, Toast.LENGTH_SHORT).show();
                        } else {
                            box.getBox("Print Failed",
                                    "Could not print. Check the printer is on and in range.");
                        }
                    }
                });
            }
        }).start();
    }

    private void selectBox(int position) {
        if (spBoxNo.getSelectedItemPosition() == position) {
            return;
        }
        suppressBoxEvent = true;
        spBoxNo.setSelection(position);
    }

    private void resetBoxDropdown() {
        boxItems.clear();
        boxItems.add(BOX_SELECT);
        notifyBoxes();
        selectBox(0);
    }

    private void notifyBoxes() {
        if (spBoxNo.getAdapter() instanceof ArrayAdapter) {
            ((ArrayAdapter<?>) spBoxNo.getAdapter()).notifyDataSetChanged();
        }
    }

    private JSONArray exData(JSONObject response) {
        JSONArray rows = asArray(response, "EX_DATA");
        if (rows != null) {
            return rows;
        }
        JSONObject data = responseBody(response);
        if (data != null && data != response) {
            return asArray(data, "EX_DATA");
        }
        return null;
    }

    /** Prefer gateway Data wrapper when present. */
    private JSONObject responseBody(JSONObject response) {
        if (response == null) {
            return null;
        }
        JSONObject data = response.optJSONObject("Data");
        return data != null ? data : response;
    }

    private JSONArray asArray(JSONObject source, String key) {
        if (source == null || !source.has(key) || source.isNull(key)) {
            return null;
        }
        Object value = source.opt(key);
        return normalizeTable(value);
    }

    /**
     * Normalizes SAP table JSON: array, single object, or {@code { "item": ... }} wrapper.
     */
    private JSONArray normalizeTable(Object value) {
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            if (obj.has("item") && !obj.isNull("item")) {
                return normalizeTable(obj.opt("item"));
            }
            JSONArray one = new JSONArray();
            one.put(obj);
            return one;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
                return null;
            }
            try {
                if (text.startsWith("[")) {
                    return new JSONArray(text);
                }
                if (text.startsWith("{")) {
                    return normalizeTable(new JSONObject(text));
                }
            } catch (JSONException ignored) {
            }
        }
        return null;
    }

    private JSONObject returnObject(JSONObject response) {
        JSONObject ret = extractReturn(response);
        if (ret != null) {
            return ret;
        }
        JSONObject data = response != null ? response.optJSONObject("Data") : null;
        if (data != null) {
            return extractReturn(data);
        }
        return null;
    }

    private JSONObject extractReturn(JSONObject source) {
        if (source == null || !source.has("EX_RETURN") || source.isNull("EX_RETURN")) {
            return null;
        }
        Object raw = source.opt("EX_RETURN");
        if (raw instanceof JSONObject) {
            JSONObject obj = (JSONObject) raw;
            if (obj.has("item") && !obj.isNull("item")) {
                Object item = obj.opt("item");
                if (item instanceof JSONObject) {
                    return (JSONObject) item;
                }
                if (item instanceof JSONArray) {
                    return firstReturnRow((JSONArray) item);
                }
            }
            return obj;
        }
        if (raw instanceof JSONArray) {
            return firstReturnRow((JSONArray) raw);
        }
        return null;
    }

    private JSONObject firstReturnRow(JSONArray arr) {
        if (arr == null || arr.length() == 0) {
            return null;
        }
        JSONObject firstData = null;
        for (int i = 0; i < arr.length(); i++) {
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

    /**
     * Success unless EX_RETURN/Status is E or A.
     * Also accepts flattened TYPE at top level, or any non-empty EX_DATA.
     */
    private boolean isSapSuccess(JSONObject response) {
        if (response == null) {
            return false;
        }

        Object status = response.opt("Status");
        if (status != null && !status.toString().trim().isEmpty()) {
            String s = status.toString().trim();
            if ("E".equalsIgnoreCase(s) || "A".equalsIgnoreCase(s)) {
                return false;
            }
            if ("S".equalsIgnoreCase(s)) {
                return true;
            }
        }

        JSONObject ret = returnObject(response);
        if (ret != null) {
            String type = ret.optString("TYPE", "").trim();
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                return false;
            }
            return true;
        }

        String topType = response.optString("TYPE", "").trim();
        if ("E".equalsIgnoreCase(topType) || "A".equalsIgnoreCase(topType)) {
            return false;
        }
        if ("S".equalsIgnoreCase(topType)
                || "I".equalsIgnoreCase(topType)
                || "W".equalsIgnoreCase(topType)) {
            return true;
        }

        JSONArray rows = exData(response);
        return rows != null && rows.length() > 0;
    }

    private String sapMessage(JSONObject response, String fallback) {
        if (response == null) {
            return fallback;
        }
        String msg = response.optString("Message", "").trim();
        if (!msg.isEmpty()) {
            return msg;
        }
        msg = response.optString("MESSAGE", "").trim();
        if (!msg.isEmpty()) {
            return msg;
        }
        JSONObject ret = returnObject(response);
        if (ret != null) {
            msg = ret.optString("MESSAGE", "").trim();
            if (!msg.isEmpty()) {
                return msg;
            }
        }
        return fallback;
    }

    private interface RfcCb {
        void ok(JSONObject response);
        void err(String message);
    }

    private void callRfc(final String rfcName, JSONObject params, final RfcCb cb) {
        String rfcUrl = GatewayUrls.noAclJsonRfcUrl(url, rfcName);
        if (rfcUrl.isEmpty()) {
            dismissProgress();
            cb.err("Server URL missing. Please log in again.");
            return;
        }
        Log.d(TAG, "RFC request -> " + rfcName + " " + params);
        SapJsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissProgress();
                        Log.d(TAG, "RFC response -> " + rfcName + ": " + response);
                        cb.ok(response != null ? response : new JSONObject());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        dismissProgress();
                        Log.e(TAG, "RFC error -> " + rfcName, error);
                        cb.err(error.getMessage() != null ? error.getMessage() : "Network error");
                    }
                });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void showProgress(String message) {
        if (con == null) {
            return;
        }
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(con);
            dialog.setCancelable(false);
        }
        dialog.setMessage(message);
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void bindScan(final EditText field, final Runnable onScan) {
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean tab = event != null && event.getKeyCode() == KeyEvent.KEYCODE_TAB;
                if (tab) {
                    return false;
                }
                boolean enter = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enter) {
                    onScan.run();
                    return true;
                }
                return false;
            }
        });
        field.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 2;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressScan) {
                    return;
                }
                if (scannerReading && s.toString().trim().length() > 0) {
                    onScan.run();
                }
            }
        });
    }

    private void restorePrinter() {
        String savedPrinter = data.read(Vars.TVS_PRINTER);
        if (savedPrinter != null && savedPrinter.length() > 0) {
            TSPLPrinter helper = new TSPLPrinter(con);
            if (helper.findBluetoothPrinterByScan(savedPrinter)) {
                applyPrinter(helper.getPrinterName());
                return;
            }
        }
        tvsPrinter = null;
        etPrinter.requestFocus();
    }

    private void detectPrinter(String raw) {
        String printerName = raw == null ? "" : raw.trim().toUpperCase();
        if (printerName.isEmpty()) {
            etPrinter.requestFocus();
            return;
        }
        TSPLPrinter helper = new TSPLPrinter(con);
        if (!helper.findBluetoothPrinterByScan(printerName)) {
            box.getBox("Not Paired",
                    "Printer is not paired with this device.\nPair TVS370 or PP310 in Bluetooth settings first.");
            etPrinter.setText("");
            tvsPrinter = null;
            etPrinter.requestFocus();
            return;
        }
        applyPrinter(helper.getPrinterName());
    }

    private void applyPrinter(String printerName) {
        tvsPrinter = printerName;
        setFieldText(etPrinter, printerName);
        data.write(Vars.TVS_PRINTER, printerName);
        focus(etVehicle);
    }

    private void acceptPallet(String raw) {
        final String pallet = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (pallet.isEmpty()) {
            etScanPallet.requestFocus();
            return;
        }
        if (requestInProgress) {
            return;
        }
        if (url.isEmpty()) {
            box.getBox("Alert", "Server URL missing. Please log in again.");
            return;
        }
        String user = data.getSapUserId() != null ? data.getSapUserId().trim() : "";
        String plant = data.read("WERKS") != null ? data.read("WERKS").trim() : "";
        if (user.isEmpty() || plant.isEmpty()) {
            box.getBox("Alert", "User or plant not found. Please log in again.");
            return;
        }

        setFieldText(etScanPallet, pallet);
        etPallet.setText("");
        requestInProgress = true;
        showProgress("Validating pallet...");

        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZWM_VND_PAL_VALD);
            params.put("IM_USER", user);
            params.put("IM_PLANT", plant);
            params.put("IM_PALL", pallet);
        } catch (JSONException e) {
            requestInProgress = false;
            dismissProgress();
            box.getBox("Alert", "Could not build pallet request.");
            return;
        }

        callRfc(Vars.ZWM_VND_PAL_VALD, params, new RfcCb() {
            @Override
            public void ok(JSONObject response) {
                requestInProgress = false;
                if (!isSapSuccess(response)) {
                    setFieldText(etScanPallet, "");
                    etPallet.setText("");
                    box.getBox("Alert", sapMessage(response, "Pallet not valid."));
                    etScanPallet.requestFocus();
                    return;
                }
                etPallet.setText(pallet);
                focus(etScanHu);
            }

            @Override
            public void err(String message) {
                requestInProgress = false;
                setFieldText(etScanPallet, "");
                etPallet.setText("");
                box.getBox("Alert", message != null ? message : "Network error");
                etScanPallet.requestFocus();
            }
        });
    }

    /** When Scan HUNo matches HU.NO, clear Box No, HU.NO, and Scan HUNo. */
    private void acceptHu(String raw) {
        String scanned = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (scanned.isEmpty()) {
            etScanHu.requestFocus();
            return;
        }
        String shown = text(etHuNo).toUpperCase(Locale.ROOT);
        if (shown.isEmpty()) {
            box.getBox("Alert", "Select Box No first!");
            setFieldText(etScanHu, "");
            return;
        }
        if (!sameHu(shown, scanned)) {
            box.getBox("Alert", "HU No does not match.");
            setFieldText(etScanHu, "");
            etScanHu.requestFocus();
            return;
        }
        lastExtHu = shown;
        selectBox(0);
        etHuNo.setText("");
        setFieldText(etScanHu, "");
        etScanHu.requestFocus();
    }

    private static boolean sameHu(String left, String right) {
        String a = UIFuncs.removeLeadingZeros(left);
        String b = UIFuncs.removeLeadingZeros(right);
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private void save() {
        if (tvsPrinter == null || tvsPrinter.isEmpty()) {
            box.getBox("Alert", "Scan Printer first!");
            etPrinter.requestFocus();
            return;
        }
        if (text(etVehicle).isEmpty()) {
            box.getBox("Alert", "Scan Vehicle No!");
            etVehicle.requestFocus();
            return;
        }
        if (text(etPo).isEmpty()) {
            box.getBox("Alert", "Scan PO No!");
            etPo.requestFocus();
            return;
        }
        if (text(etPallet).isEmpty()) {
            box.getBox("Alert", "Scan Pallet!");
            etScanPallet.requestFocus();
            return;
        }
        final String extHu = text(etHuNo).isEmpty() ? lastExtHu : text(etHuNo);
        if (extHu.isEmpty()) {
            box.getBox("Alert", "Scan HU No!");
            etScanHu.requestFocus();
            return;
        }
        if (requestInProgress) {
            return;
        }
        if (url.isEmpty()) {
            box.getBox("Alert", "Server URL missing. Please log in again.");
            return;
        }
        String user = data.getSapUserId() != null ? data.getSapUserId().trim() : "";
        String plant = data.read("WERKS") != null ? data.read("WERKS").trim() : "";
        if (user.isEmpty() || plant.isEmpty()) {
            box.getBox("Alert", "User or plant not found. Please log in again.");
            return;
        }

        String weight = formatHuWeight(text(etWeight));
        if (weight.isEmpty()) {
            weight = "0.000";
        }

        requestInProgress = true;
        showProgress("Saving...");
        // IM_PARMS is an import table (ZTT_UNLOAD_SAVE). Indexed form fields fill row 0.
        // A JSON array is left empty and SAP returns "Fill IM_PARMS first".
        final Map<String, String> saveParams = new HashMap<String, String>();
        saveParams.put("bapiname", Vars.ZVND_UNLOAD_SAVE_RFC);
        saveParams.put("IM_USER", user);
        saveParams.put("IM_PARMS[0].PLANT", clip(plant, 4));
        saveParams.put("IM_PARMS[0].VEHICLE", clip(text(etVehicle), 10));
        saveParams.put("IM_PARMS[0].EXT_HU", clip(extHu, 20));
        saveParams.put("IM_PARMS[0].PALETTE", clip(text(etPallet), 10));
        saveParams.put("IM_PARMS[0].PO_NO", clip(text(etPo), 10));
        saveParams.put("IM_PARMS[0].BILL_NO", clip(text(etBillNo), 16));
        saveParams.put("IM_PARMS[0].HU_WT", weight);

        String rfcUrl = GatewayUrls.noAclJsonRfcUrl(url, Vars.ZVND_UNLOAD_SAVE_RFC);
        if (rfcUrl.isEmpty()) {
            requestInProgress = false;
            dismissProgress();
            box.getBox("Alert", "Server URL missing. Please log in again.");
            return;
        }
        Log.d(TAG, "Save params -> " + saveParams);
        StringRequest req = new StringRequest(Request.Method.POST, rfcUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String body) {
                        requestInProgress = false;
                        dismissProgress();
                        Log.d(TAG, "RFC response -> " + Vars.ZVND_UNLOAD_SAVE_RFC + ": " + body);
                        JSONObject response;
                        try {
                            response = new JSONObject(body != null ? body : "{}");
                        } catch (JSONException e) {
                            box.getBox("Alert", "Parse error while saving.");
                            return;
                        }
                        if (!isSapSuccess(response)) {
                            box.getBox("Alert", sapMessage(response, "Could not save data."));
                            return;
                        }
                        String msg = sapMessage(response, "Saved");
                        clearAfterSave();
                        lastExtHu = "";
                        Toast.makeText(con, msg, Toast.LENGTH_SHORT).show();
                        etScanPallet.requestFocus();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestInProgress = false;
                        dismissProgress();
                        box.getBox("Alert", error.getMessage() != null ? error.getMessage() : "Network error");
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                return saveParams;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private static String clip(String value, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, max);
    }

    /** HU_WT is QUAN 15(3). */
    private static String formatHuWeight(String raw) {
        String t = raw == null ? "" : raw.trim().replace(',', '.');
        if (t.isEmpty()) {
            return "";
        }
        try {
            return new BigDecimal(t).setScale(3, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /** Scan Pallet, Pallet, Box No, HU.NO, and Scan HUNo. Printer, vehicle, and PO stay. */
    private void clearAfterSave() {
        etScanPallet.setText("");
        etPallet.setText("");
        selectBox(0);
        etHuNo.setText("");
        etScanHu.setText("");
        etBillNo.setText("");
        etWeight.setText("");
    }

    private void resetAll() {
        requestInProgress = false;
        lastExtHu = "";
        tvsPrinter = null;
        etPrinter.setText("");
        etVehicle.setText("");
        setFieldText(etPo, "");
        resetBoxDropdown();
        clearAfterSave();
        etPrinter.requestFocus();
    }

    private void focus(final EditText field) {
        field.post(new Runnable() {
            @Override
            public void run() {
                field.requestFocus();
            }
        });
    }

    private void setFieldText(EditText field, String value) {
        suppressScan = true;
        field.setText(value);
        suppressScan = false;
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
