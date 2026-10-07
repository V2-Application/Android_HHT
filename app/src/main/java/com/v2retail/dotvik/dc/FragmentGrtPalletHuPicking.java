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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentGrtPalletHuPicking extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentGrtPalletHuPicking.class.getName();
    private static final int REQUEST_GET_PICKLIST = 1801;
    private static final int REQUEST_GET_PICK_DETAILS = 1802;
    private static final int REQUEST_VALIDATE_PALLET = 1803;
    private static final int REQUEST_VALIDATE_BIN = 1804;
    private static final int REQUEST_VALIDATE_HU = 1805;
    private static final int REQUEST_SAVE_HU = 1806;
    private static final String PICKLIST_PLACEHOLDER = "Select Picklist";

    View rootView;
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    SharedPreferencesData data;

    String URL = "";
    String USER = "";
    String WERKS = "";

    Spinner dd_picklist;
    EditText txt_bin, txt_pallet, txt_hu;
    RecyclerView rv_scanned;
    Button btn_reset, btn_save;

    /** Display labels for the spinner; index 0 is the placeholder. */
    final List<String> picklists = new ArrayList<>();
    /** Raw ET_PICKLIST-PICKLIST values, aligned with {@link #picklists} (index 0 = ""). */
    final List<String> picklistNos = new ArrayList<>();
    /** ET_DATA rows of the loaded picklist (PICKLIST, SWERKS, RWERKS, LGPLA, CRATE, PALETTE). */
    final List<JSONObject> pickDetails = new ArrayList<>();
    final List<String[]> scannedRows = new ArrayList<>();
    ArrayAdapter<String> picklistAdapter;
    ScannedRowAdapter scannedAdapter;
    boolean requestInFlight = false;
    String loadedPicklist = "";
    String validatedBin = "";
    String validatedPallet = "";
    String validatedHu = "";
    /** Row taken out of the table when its HU was validated; put back if the save fails. */
    JSONObject removedPickRow = null;
    int removedPickRowIndex = -1;

    public FragmentGrtPalletHuPicking() {
    }

    public static FragmentGrtPalletHuPicking newInstance() {
        return new FragmentGrtPalletHuPicking();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("GRT Pallet HU Picking");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_pallet_hu_picking, container, false);
        con = getContext();
        box = new AlertBox(con);
        data = new SharedPreferencesData(con);
        URL = data.read("URL");
        USER = data.read("USER");
        WERKS = data.read("WERKS");

        dd_picklist = rootView.findViewById(R.id.dd_grt_pallet_hu_picking_picklist);
        txt_bin = rootView.findViewById(R.id.txt_grt_pallet_hu_picking_bin);
        txt_pallet = rootView.findViewById(R.id.txt_grt_pallet_hu_picking_pallet);
        txt_hu = rootView.findViewById(R.id.txt_grt_pallet_hu_picking_hu);
        rv_scanned = rootView.findViewById(R.id.rv_grt_pallet_hu_picking);
        btn_reset = rootView.findViewById(R.id.btn_grt_pallet_hu_picking_reset);
        btn_save = rootView.findViewById(R.id.btn_grt_pallet_hu_picking_save);

        resetPicklistItems();
        picklistAdapter = new ArrayAdapter<>(con, android.R.layout.simple_spinner_item, picklists);
        picklistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_picklist.setAdapter(picklistAdapter);
        dd_picklist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onPicklistSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        scannedAdapter = new ScannedRowAdapter(scannedRows);
        rv_scanned.setAdapter(scannedAdapter);

        btn_reset.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        bindScanField(txt_bin, this::validateBin);
        bindScanField(txt_pallet, this::validatePallet);
        bindScanField(txt_hu, this::validateHu);

        loadPicklists();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_grt_pallet_hu_picking_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                });
                break;
            case R.id.btn_grt_pallet_hu_picking_save:
                save();
                break;
        }
    }

    private void clear() {
        resetBinInput();
        resetPalletInput();
        resetHuInput();
        clearPickDetails();
        resetPicklistItems();
        picklistAdapter.notifyDataSetChanged();
        dd_picklist.setSelection(0);
        txt_bin.requestFocus();
        loadPicklists();
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

    private void validateBin() {
        if (requestInFlight) {
            return;
        }
        String bin = UIFuncs.toUpperTrim(txt_bin);
        if (bin.isEmpty()) {
            return;
        }
        if (loadedPicklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Please select Picklist");
            resetBinInput();
            return;
        }
        String plant = WERKS != null ? WERKS.trim().toUpperCase() : "";

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_BIN_VAL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", plant);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_BIN_VAL, REQUEST_VALIDATE_BIN, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onBinValidated() {
        validatedBin = UIFuncs.toUpperTrim(txt_bin);
        UIFuncs.disableInput(con, txt_bin);
        txt_pallet.requestFocus();
    }

    private void resetBinInput() {
        validatedBin = "";
        txt_bin.setText("");
        UIFuncs.enableInput(con, txt_bin);
    }

    private void validatePallet() {
        if (requestInFlight) {
            return;
        }
        String pallet = UIFuncs.toUpperTrim(txt_pallet);
        if (pallet.isEmpty()) {
            return;
        }
        if (loadedPicklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Please select Picklist");
            resetPalletInput();
            return;
        }
        if (validatedBin.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Please scan Bin No");
            resetPalletInput();
            txt_bin.requestFocus();
            return;
        }
        String plant = WERKS != null ? WERKS.trim().toUpperCase() : "";

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_PAL_VAL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", plant);
            args.put("IM_PALETTE", pallet);
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_PAL_VAL, REQUEST_VALIDATE_PALLET, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onPalletValidated() {
        validatedPallet = UIFuncs.toUpperTrim(txt_pallet);
        UIFuncs.disableInput(con, txt_pallet);
        txt_hu.requestFocus();
    }

    private void resetPalletInput() {
        validatedPallet = "";
        txt_pallet.setText("");
        UIFuncs.enableInput(con, txt_pallet);
    }

    private void validateHu() {
        if (requestInFlight) {
            return;
        }
        String hu = UIFuncs.toUpperTrim(txt_hu);
        if (hu.isEmpty()) {
            return;
        }
        if (loadedPicklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Please select Picklist");
            resetHuInput();
            return;
        }
        if (validatedBin.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Please scan Bin No");
            resetHuInput();
            txt_bin.requestFocus();
            return;
        }
        if (validatedPallet.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Please scan Pallet");
            resetHuInput();
            txt_pallet.requestFocus();
            return;
        }
        if (findPickRowIndexByHu(hu) < 0) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "HU " + hu + " not found in picklist or already scanned");
            resetHuInput();
            return;
        }
        String plant = WERKS != null ? WERKS.trim().toUpperCase() : "";

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_VAL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", plant);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_VAL, REQUEST_VALIDATE_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onHuValidated() {
        validatedHu = UIFuncs.toUpperTrim(txt_hu);
        UIFuncs.disableInput(con, txt_hu);
        UIFuncs.hideKeyboard(getActivity());
        removePickRow(findPickRowIndexByHu(validatedHu));
    }

    /** Index of the ET_DATA row whose CRATE matches {@code hu} (leading zeros ignored), or -1. */
    private int findPickRowIndexByHu(String hu) {
        String target = normalizeHu(hu);
        if (target.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < pickDetails.size(); i++) {
            if (target.equals(normalizeHu(pickDetails.get(i).optString("CRATE", "")))) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeHu(String hu) {
        if (hu == null) {
            return "";
        }
        String value = hu.trim().toUpperCase();
        return value.isEmpty() ? "" : UIFuncs.removeLeadingZeros(value);
    }

    private void removePickRow(int index) {
        if (index < 0 || index >= pickDetails.size()) {
            return;
        }
        removedPickRow = pickDetails.remove(index);
        removedPickRowIndex = index;
        scannedRows.remove(index);
        scannedAdapter.notifyDataSetChanged();
    }

    private void restoreRemovedPickRow() {
        if (removedPickRow == null) {
            return;
        }
        int index = Math.max(0, Math.min(removedPickRowIndex, pickDetails.size()));
        pickDetails.add(index, removedPickRow);
        scannedRows.add(index, toTableRow(removedPickRow));
        scannedAdapter.notifyDataSetChanged();
        removedPickRow = null;
        removedPickRowIndex = -1;
    }

    private static String[] toTableRow(JSONObject row) {
        return new String[]{
                row.optString("LGPLA", "").trim(),
                row.optString("PALETTE", "").trim(),
                row.optString("CRATE", "").trim()
        };
    }

    private void resetHuInput() {
        validatedHu = "";
        txt_hu.setText("");
        UIFuncs.enableInput(con, txt_hu);
    }

    private void save() {
        if (requestInFlight) {
            return;
        }
        if (loadedPicklist.isEmpty()) {
            box.getBox("Alert", "Please select Picklist");
            return;
        }
        if (validatedBin.isEmpty()) {
            box.getBox("Alert", "Please scan Bin No");
            txt_bin.requestFocus();
            return;
        }
        if (validatedPallet.isEmpty()) {
            box.getBox("Alert", "Please scan Pallet");
            txt_pallet.requestFocus();
            return;
        }
        String hu = UIFuncs.toUpperTrim(txt_hu);
        if (validatedHu.isEmpty() || !validatedHu.equals(hu)) {
            box.getBox("Alert", "Please scan HU");
            txt_hu.requestFocus();
            return;
        }
        String plant = WERKS != null ? WERKS.trim().toUpperCase() : "";

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_HU", validatedHu);
            args.put("IM_PLANT", plant);
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_SAVE, REQUEST_SAVE_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onSaveSuccess(JSONObject responsebody) {
        String message = "HU " + validatedHu + " saved successfully";
        JSONObject returnObj = extractReturn(responsebody);
        if (returnObj != null && !returnObj.optString("MESSAGE", "").trim().isEmpty()) {
            message = returnObj.optString("MESSAGE", "").trim();
        }
        Toast.makeText(con, message, Toast.LENGTH_SHORT).show();

        removedPickRow = null;
        removedPickRowIndex = -1;
        resetHuInput();

        if (pickDetails.isEmpty()) {
            box.getBox("Success", "Picklist completed");
            clear();
        }
    }

    private void clearPickDetails() {
        loadedPicklist = "";
        removedPickRow = null;
        removedPickRowIndex = -1;
        pickDetails.clear();
        scannedRows.clear();
        scannedAdapter.notifyDataSetChanged();
    }

    private void onPicklistSelected() {
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            clearPickDetails();
            return;
        }
        if (picklist.equals(loadedPicklist)) {
            return;
        }
        resetBinInput();
        resetPalletInput();
        resetHuInput();
        clearPickDetails();
        loadPicklistDetails(picklist);
    }

    /** Selected ET_PICKLIST-PICKLIST, or "" when the placeholder is selected. */
    private String getSelectedPicklist() {
        int pos = dd_picklist.getSelectedItemPosition();
        if (pos < 0 || pos >= picklistNos.size()) {
            return "";
        }
        return picklistNos.get(pos);
    }

    private void resetPicklistItems() {
        picklists.clear();
        picklistNos.clear();
        picklists.add(PICKLIST_PLACEHOLDER);
        picklistNos.add("");
    }

    private void loadPicklists() {
        if (requestInFlight) {
            return;
        }
        String plant = WERKS != null ? WERKS.trim().toUpperCase() : "";
        if (plant.isEmpty()) {
            box.getBox("Alert", "Plant missing. Please log in again.");
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_GET_PICKIST);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", plant);
            args.put("IM_DESKTOP", "");
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_GET_PICKIST, REQUEST_GET_PICKLIST, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onPicklistsReceived(JSONObject responsebody) {
        resetPicklistItems();
        JSONArray arr = responsebody.optJSONArray("ET_PICKLIST");
        if (arr != null) {
            try {
                for (int i = SapJsonRows.startIndex(arr, "PICKLIST", "STORE"); i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row == null || SapJsonRows.isMetadataRow(row, "PICKLIST", "STORE")) {
                        continue;
                    }
                    String picklist = row.optString("PICKLIST", "").trim();
                    if (picklist.isEmpty() || picklistNos.contains(picklist)) {
                        continue;
                    }
                    String store = row.optString("STORE", "").trim();
                    picklistNos.add(picklist);
                    picklists.add(store.isEmpty() ? picklist : picklist + " - " + store);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        picklistAdapter.notifyDataSetChanged();
        dd_picklist.setSelection(0);

        if (picklistNos.size() <= 1) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "No picklist found");
        } else {
            dd_picklist.requestFocus();
        }
    }

    private void loadPicklistDetails(String picklist) {
        if (requestInFlight) {
            return;
        }
        String plant = WERKS != null ? WERKS.trim().toUpperCase() : "";

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_PICK_DET);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", plant);
            args.put("IM_PICKLIST", picklist);
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_PICK_DET, REQUEST_GET_PICK_DETAILS, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onPicklistDetailsReceived(JSONObject responsebody) {
        clearPickDetails();
        JSONArray arr = responsebody.optJSONArray("ET_DATA");
        if (arr != null) {
            try {
                for (int i = SapJsonRows.startIndex(arr, "PICKLIST", "LGPLA", "CRATE", "PALETTE"); i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row == null || SapJsonRows.isMetadataRow(row, "PICKLIST", "LGPLA", "CRATE", "PALETTE")) {
                        continue;
                    }
                    pickDetails.add(row);
                    scannedRows.add(toTableRow(row));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        scannedAdapter.notifyDataSetChanged();

        if (pickDetails.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "No details found for selected picklist");
            dd_picklist.setSelection(0);
            return;
        }
        loadedPicklist = getSelectedPicklist();
        txt_bin.requestFocus();
    }

    private void resetFailedRequest(int request) {
        if (request == REQUEST_GET_PICK_DETAILS) {
            clearPickDetails();
            dd_picklist.setSelection(0);
        } else if (request == REQUEST_SAVE_HU) {
            restoreRemovedPickRow();
            resetHuInput();
            txt_hu.requestFocus();
        } else if (request == REQUEST_VALIDATE_PALLET) {
            resetPalletInput();
            txt_pallet.requestFocus();
        } else if (request == REQUEST_VALIDATE_BIN) {
            resetBinInput();
            txt_bin.requestFocus();
        } else if (request == REQUEST_VALIDATE_HU) {
            resetHuInput();
            txt_hu.requestFocus();
        }
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
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row != null) {
                        return row;
                    }
                }
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
                resetFailedRequest(request);
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
            resetFailedRequest(request);
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
                            resetFailedRequest(request);
                            return;
                        }
                        if (isReturnError(responsebody)) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", getReturnMessage(responsebody));
                            resetFailedRequest(request);
                            return;
                        }
                        if (request == REQUEST_GET_PICKLIST) {
                            onPicklistsReceived(responsebody);
                        } else if (request == REQUEST_GET_PICK_DETAILS) {
                            onPicklistDetailsReceived(responsebody);
                        } else if (request == REQUEST_VALIDATE_PALLET) {
                            onPalletValidated();
                        } else if (request == REQUEST_VALIDATE_BIN) {
                            onBinValidated();
                        } else if (request == REQUEST_VALIDATE_HU) {
                            onHuValidated();
                        } else if (request == REQUEST_SAVE_HU) {
                            onSaveSuccess(responsebody);
                        }
                    }
                }, volleyErrorListener(request)) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return params.toString().getBytes();
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

    Response.ErrorListener volleyErrorListener(int request) {
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
                resetFailedRequest(request);
            }
        };
    }

    /** Each row is {binNo, pallet, hu}; Sr is derived from position. */
    static class ScannedRowAdapter extends RecyclerView.Adapter<ScannedRowAdapter.RowHolder> {

        private final List<String[]> rows;

        ScannedRowAdapter(List<String[]> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grt_pallet_hu_picking_row, parent, false);
            return new RowHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            String[] row = rows.get(position);
            holder.sr.setText(String.valueOf(position + 1));
            holder.bin.setText(row[0]);
            holder.pallet.setText(row[1]);
            holder.hu.setText(row[2]);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class RowHolder extends RecyclerView.ViewHolder {
            final TextView sr, bin, pallet, hu;

            RowHolder(@NonNull View itemView) {
                super(itemView);
                sr = itemView.findViewById(R.id.tv_grt_pallet_hu_row_sr);
                bin = itemView.findViewById(R.id.tv_grt_pallet_hu_row_bin);
                pallet = itemView.findViewById(R.id.tv_grt_pallet_hu_row_pallet);
                hu = itemView.findViewById(R.id.tv_grt_pallet_hu_row_hu);
            }
        }
    }
}
