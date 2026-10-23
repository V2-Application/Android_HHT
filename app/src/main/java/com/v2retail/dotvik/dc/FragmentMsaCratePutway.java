package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Opens from GRC Done-Putway Pending. Next loads crate quantities through
 * {@link Vars#ZWM_CRATE_GET_QTY_RFC}. MSA Scan Bin loads bins through
 * {@link Vars#ZWM_CRATE_MSA_BIN_VAL_RFC} and shows them in random order
 * beside the crates. Bin Scan moves the scanned bin onto that crate's row.
 */
public class FragmentMsaCratePutway extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentMsaCratePutway";
    private static final String TITLE = "MSA Crate Putway";
    private static final String ARG_EBELN = "EBELN";
    private static final String ARG_MBLNR = "MBLNR";
    private static final String ARG_ET_DATA = "ET_DATA";

    private EditText txtMsaBin;
    private EditText txtCrate;
    private EditText txtQty;
    private EditText txtBin;
    private RecyclerView crateList;
    private final List<ScannedRow> rows = new ArrayList<>();
    /** Crates from the quantity RFC, keyed by crate id. */
    private final Map<String, CrateQty> crateCatalog = new LinkedHashMap<>();
    /** Valid bins from MSA bin validation, keyed by bin id so lookup ignores response order. */
    private final Map<String, String> validBins = new LinkedHashMap<>();
    private ScannedAdapter adapter;
    private AlertBox box;
    private ProgressDialog dialog;
    private String url = "";
    private String werks = "";
    private String user = "";
    private String ebeln = "";
    private String mblnr = "";
    private String etDataJson = "";
    private boolean loadInProgress;
    private boolean saveRequest;
    private boolean msaBinRequest;

    public FragmentMsaCratePutway() {
    }

    public static FragmentMsaCratePutway newInstance(String ebeln, String mblnr, String etData) {
        FragmentMsaCratePutway fragment = new FragmentMsaCratePutway();
        Bundle args = new Bundle();
        args.putString(ARG_EBELN, ebeln);
        args.putString(ARG_MBLNR, mblnr);
        args.putString(ARG_ET_DATA, etData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_msa_crate_putway, container, false);
        box = new AlertBox(getContext());
        SharedPreferencesData data = new SharedPreferencesData(getContext());
        url = data.read("URL");
        werks = data.read("WERKS");
        user = data.read("USER");
        Bundle args = getArguments();
        if (args != null) {
            ebeln = args.getString(ARG_EBELN, "");
            mblnr = args.getString(ARG_MBLNR, "");
            etDataJson = args.getString(ARG_ET_DATA, "");
        }

        txtMsaBin = view.findViewById(R.id.txt_msa_scan_bin);
        txtCrate = view.findViewById(R.id.txt_msa_crate_scan);
        txtQty = view.findViewById(R.id.txt_msa_crate_qty);
        txtBin = view.findViewById(R.id.txt_msa_bin_scan);
        Button btnBack = view.findViewById(R.id.btn_msa_crate_back);
        Button btnSave = view.findViewById(R.id.btn_msa_crate_save);
        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        txtMsaBin.setOnEditorActionListener(this::onMsaBinDone);
        txtCrate.setOnEditorActionListener(this::onCrateScanDone);
        txtBin.setOnEditorActionListener(this::onBinScanDone);
        UIFuncs.disableKeyInput(txtCrate, txtCrate, getContext());
        UIFuncs.disableKeyInput(txtBin, txtBin, getContext());
        watchScanner(txtMsaBin, () -> validateMsaBin(text(txtMsaBin)));
        watchScanner(txtCrate, () -> applyCrateFromTable(text(txtCrate)));
        watchScanner(txtBin, () -> mapBinToCrate(text(txtBin)));

        crateList = view.findViewById(R.id.rv_msa_scanned_crates);
        crateList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScannedAdapter(rows);
        crateList.setAdapter(adapter);
        bindPassedQtyRows(view);
        return view;
    }

    @Override
    public void onDestroyView() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle(TITLE);
        }
        if (txtMsaBin != null) {
            txtMsaBin.post(() -> txtMsaBin.requestFocus());
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_msa_crate_back) {
            openPutawayOperations();
        } else if (id == R.id.btn_msa_crate_save) {
            save();
        }
    }

    /** Skip GRC Done-Putway Pending and return to Putaway Operations. */
    private void openPutawayOperations() {
        FragmentManager fm = getParentFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count >= 2) {
            fm.popBackStack(fm.getBackStackEntryAt(count - 2).getId(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        if (count > 0) {
            fm.popBackStack();
        }
    }

    private boolean onMsaBinDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        validateMsaBin(text(txtMsaBin));
        return true;
    }

    private boolean onCrateScanDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        applyCrateFromTable(text(txtCrate));
        return true;
    }

    private boolean onBinScanDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        mapBinToCrate(text(txtBin));
        return true;
    }

    private static boolean isScanCommit(int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
            return true;
        }
        return event != null
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_DOWN;
    }

    private void watchScanner(EditText field, Runnable onScan) {
        field.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!scannerReading || loadInProgress) {
                    return;
                }
                if (!s.toString().trim().isEmpty()) {
                    onScan.run();
                }
            }
        });
    }

    private void validateMsaBin(String bin) {
        if (loadInProgress) {
            return;
        }
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan MSA Bin.");
            txtMsaBin.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            saveRequest = false;
            msaBinRequest = true;
            args.put("bapiname", Vars.ZWM_CRATE_MSA_BIN_VAL_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_MSA_BIN", bin);
            submitRequest(args);
        } catch (JSONException e) {
            loadInProgress = false;
            msaBinRequest = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void handleMsaBinResponse(JSONObject responsebody) {
        msaBinRequest = false;
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        if (returnobj != null) {
            String type = returnobj.optString("TYPE", "");
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", returnobj.optString("MESSAGE", "MSA bin validation failed."));
                txtMsaBin.setText("");
                txtMsaBin.requestFocus();
                return;
            }
        }
        validBins.clear();
        JSONArray etData = responsebody.optJSONArray("ET_DATA");
        if (etData != null) {
            for (int i = 0; i < etData.length(); i++) {
                JSONObject item = etData.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String bin = item.optString("LGPLA", "").trim();
                if (!bin.isEmpty()) {
                    validBins.put(binKey(bin), bin);
                }
            }
        }
        Log.d(TAG, "ZWM_CRATE_MSA_BIN_VAL_RFC EX_RETURN -> "
                + (returnobj == null ? "null" : returnobj.toString()));
        Log.d(TAG, "ZWM_CRATE_MSA_BIN_VAL_RFC bin count -> " + validBins.size());
        for (String bin : validBins.values()) {
            Log.d(TAG, "ZWM_CRATE_MSA_BIN_VAL_RFC LGPLA -> " + bin);
        }
        if (validBins.isEmpty()) {
            box.getBox("Alert", "No bin found for the scanned MSA bin.");
            txtMsaBin.requestFocus();
            return;
        }
        placeBinsRandom();
        txtCrate.requestFocus();
    }

    /**
     * Keeps every crate and quantity in place, and fills the bin column in random order.
     * A bin already assigned to a crate stays on that crate's row.
     */
    private void placeBinsRandom() {
        Map<String, String> assignedBinByCrate = new LinkedHashMap<>();
        for (ScannedRow row : rows) {
            if (row.assigned && !TextUtils.isEmpty(row.crate) && !TextUtils.isEmpty(row.bin)) {
                assignedBinByCrate.put(binKey(row.crate), row.bin);
            }
        }
        Set<String> takenBins = new HashSet<>();
        for (String bin : assignedBinByCrate.values()) {
            takenBins.add(binKey(bin));
        }
        List<String> openBins = new ArrayList<>();
        for (String bin : validBins.values()) {
            if (!takenBins.contains(binKey(bin))) {
                openBins.add(bin);
            }
        }
        Collections.shuffle(openBins);

        List<CrateQty> crates = new ArrayList<>(crateCatalog.values());
        rows.clear();
        int openBinCursor = 0;
        for (CrateQty crate : crates) {
            String assignedBin = assignedBinByCrate.get(binKey(crate.crate));
            if (assignedBin != null) {
                rows.add(new ScannedRow("", assignedBin, crate.crate, crate.qty, true));
            } else {
                String bin = openBinCursor < openBins.size() ? openBins.get(openBinCursor++) : "";
                rows.add(new ScannedRow("", bin, crate.crate, crate.qty, false));
            }
        }
        while (openBinCursor < openBins.size()) {
            rows.add(new ScannedRow("", openBins.get(openBinCursor++), "", "", false));
        }
        renumberRows();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void logLong(String message) {
        if (message == null) {
            return;
        }
        final int size = 3000;
        for (int start = 0; start < message.length(); start += size) {
            int end = Math.min(message.length(), start + size);
            Log.d(TAG, message.substring(start, end));
        }
    }

    private void bindPassedQtyRows(View view) {
        if (TextUtils.isEmpty(etDataJson)) {
            return;
        }
        try {
            bindQtyRows(new JSONArray(etDataJson), view);
        } catch (JSONException e) {
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void bindQtyRows(JSONArray etData, View view) {
        rows.clear();
        crateCatalog.clear();
        int alreadyAssigned = 0;
        if (etData != null) {
            for (int i = 0; i < etData.length(); i++) {
                JSONObject item = etData.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String crate = item.optString("CRATE", "").trim();
                String bin = item.optString("LGPLA", "").trim();
                String qty = displayCount(item.opt("SCAN_QTY"));
                if (crate.isEmpty() && bin.isEmpty() && qty.isEmpty()) {
                    continue;
                }
                if (!bin.isEmpty()) {
                    alreadyAssigned++;
                    continue;
                }
                crateCatalog.put(binKey(crate), new CrateQty(crate, qty));
                rows.add(new ScannedRow(String.valueOf(rows.size() + 1), "", crate, qty));
            }
        }
        adapter.notifyDataSetChanged();
        if (rows.isEmpty()) {
            String message = alreadyAssigned > 0
                    ? "All crates already have a bin assigned."
                    : "No crate quantity found.";
            view.post(() -> {
                if (isAdded()) {
                    box.getBox("Alert", message);
                }
            });
        }
    }

    private void submitRequest(JSONObject args) {
        if (url == null || !url.contains("/")) {
            loadInProgress = false;
            saveRequest = false;
            msaBinRequest = false;
            box.getBox("Err", "Gateway URL is not configured");
            return;
        }
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String rfcUrl = url.substring(0, url.lastIndexOf("/"));
        String rfc = args.optString("bapiname", Vars.ZWM_CRATE_GET_QTY_RFC);
        rfcUrl += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "payload -> " + args);

        RequestQueue requestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, args,
                responsebody -> {
                    dismissDialog();
                    loadInProgress = false;
                    if (saveRequest) {
                        logLong("ZWM_CRATE_PICKING_SAVE_RFC response -> "
                                + (responsebody == null ? "null" : responsebody.toString()));
                    } else if (msaBinRequest) {
                        logLong("ZWM_CRATE_MSA_BIN_VAL_RFC response -> "
                                + (responsebody == null ? "null" : responsebody.toString()));
                    }
                    if (!isAdded()) {
                        return;
                    }
                    if (responsebody == null || responsebody.length() == 0) {
                        saveRequest = false;
                        msaBinRequest = false;
                        UIFuncs.errorSound(getContext());
                        box.getBox("Err", "No response from Server");
                        return;
                    }
                    if (saveRequest) {
                        handleSaveResponse(responsebody);
                    } else if (msaBinRequest) {
                        handleMsaBinResponse(responsebody);
                    }
                }, error -> {
            dismissDialog();
            loadInProgress = false;
            saveRequest = false;
            msaBinRequest = false;
            if (!isAdded()) {
                return;
            }
            UIFuncs.errorSound(getContext());
            box.getBox("Err", volleyErrorMessage(error));
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return args.toString().getBytes();
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                return super.parseNetworkResponse(response);
            }
        };
        jsonRequest.setRetryPolicy(new RetryPolicy() {
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
        requestQueue.add(jsonRequest);
    }

    private void applyCrateFromTable(String crate) {
        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate.");
            clearCrateScan();
            return;
        }
        CrateQty knownCrate = crateCatalog.get(binKey(crate));
        if (knownCrate == null) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Crate not found in the list.");
            clearCrateScan();
            return;
        }
        if (indexOfCrate(crate) >= 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Bin already assigned to this crate.");
            clearCrateScan();
            return;
        }
        txtQty.setText(knownCrate.qty);
        txtBin.requestFocus();
    }

    private void mapBinToCrate(String bin) {
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin.");
            clearBinScan(txtBin);
            return;
        }
        if (validBins.isEmpty()) {
            box.getBox("Alert", "Please scan MSA Bin.");
            clearBinScan(txtMsaBin);
            return;
        }
        String crate = text(txtCrate);
        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate.");
            clearBinScan(null);
            clearCrateScan();
            return;
        }
        CrateQty knownCrate = crateCatalog.get(binKey(crate));
        if (knownCrate == null) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Crate not found in the list.");
            clearBinScan(null);
            clearCrateScan();
            return;
        }
        if (indexOfCrate(crate) >= 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Bin already assigned to this crate.");
            clearBinScan(null);
            clearCrateScan();
            return;
        }
        String matchedBin = validBins.get(binKey(bin));
        if (matchedBin == null) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Bin not found in the validated bin list.");
            clearBinScan(txtBin);
            return;
        }
        int crateIndex = indexOfCrateRow(knownCrate.crate);
        if (crateIndex < 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Crate not found in the list.");
            clearBinScan(null);
            clearCrateScan();
            return;
        }
        int binIndex = indexOfBin(matchedBin);
        if (binIndex < 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Bin not found in the validated bin list.");
            clearBinScan(txtBin);
            return;
        }
        ScannedRow binRow = rows.get(binIndex);
        if (binRow.assigned) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "This bin is already assigned to another crate.");
            clearBinScan(txtBin);
            return;
        }
        ScannedRow crateRow = rows.get(crateIndex);
        if (crateIndex != binIndex) {
            String displaced = crateRow.bin;
            crateRow.bin = matchedBin;
            binRow.bin = displaced == null ? "" : displaced;
            if (TextUtils.isEmpty(binRow.bin) && TextUtils.isEmpty(binRow.crate)) {
                rows.remove(binIndex);
                if (binIndex < crateIndex) {
                    crateIndex--;
                }
            }
        }
        crateRow.assigned = true;
        crateRow.bin = matchedBin;
        renumberRows();
        adapter.notifyDataSetChanged();
        final int scrollTo = crateIndex;
        if (crateList != null) {
            crateList.scrollToPosition(scrollTo);
        }
    }

    private void clearBinScan(EditText focus) {
        txtBin.setText("");
        if (focus != null) {
            focus.requestFocus();
        }
    }

    private void clearCrateScan() {
        txtCrate.setText("");
        txtQty.setText("");
        txtCrate.requestFocus();
    }

    private void focusCrateScan() {
        if (txtCrate == null) {
            return;
        }
        txtCrate.post(() -> {
            txtCrate.requestFocus();
            UIFuncs.hideKeyboard(getActivity());
        });
    }

    private void renumberRows() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).srNo = String.valueOf(i + 1);
        }
    }

    private int indexOfCrate(String crate) {
        String key = binKey(crate);
        for (int i = 0; i < rows.size(); i++) {
            ScannedRow row = rows.get(i);
            if (row.assigned && key.equals(binKey(row.crate))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfCrateRow(String crate) {
        String key = binKey(crate);
        for (int i = 0; i < rows.size(); i++) {
            if (key.equals(binKey(rows.get(i).crate))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfBin(String bin) {
        String key = binKey(bin);
        for (int i = 0; i < rows.size(); i++) {
            if (key.equals(binKey(rows.get(i).bin))) {
                return i;
            }
        }
        return -1;
    }

    private static String binKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }

    private static String displayCount(Object raw) {
        if (raw == null || raw == JSONObject.NULL) {
            return "";
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return "";
        }
        try {
            double number = Double.parseDouble(value);
            if (number == Math.rint(number)) {
                return String.valueOf((long) number);
            }
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    private String volleyErrorMessage(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return "Communication Error!";
        } else if (error instanceof AuthFailureError) {
            return "Authentication Error!";
        } else if (error instanceof ServerError) {
            return "Server Side Error!";
        } else if (error instanceof NetworkError) {
            return "Network Error!";
        } else if (error instanceof ParseError) {
            return "Parse Error!";
        }
        return error.toString();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void save() {
        if (loadInProgress) {
            return;
        }
        String crate = text(txtCrate);
        int crateIndex = indexOfCrate(crate);
        String bin = crateIndex >= 0 ? rows.get(crateIndex).bin : "";
        if (TextUtils.isEmpty(bin)) {
            bin = text(txtBin);
        }
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin.");
            txtBin.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate.");
            txtCrate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(ebeln)) {
            box.getBox("Alert", "Please select PO No.");
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            saveRequest = true;
            msaBinRequest = false;
            args.put("bapiname", Vars.ZWM_CRATE_PICKING_SAVE_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_EBELN", ebeln);
            args.put("IM_BIN", bin);
            args.put("IM_CRATE", crate);
            submitRequest(args);
        } catch (JSONException e) {
            loadInProgress = false;
            saveRequest = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void handleSaveResponse(JSONObject responsebody) {
        saveRequest = false;
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        Log.d(TAG, "ZWM_CRATE_PICKING_SAVE_RFC EX_RETURN -> "
                + (returnobj == null ? "null" : returnobj.toString()));
        String type = returnobj == null ? "" : returnobj.optString("TYPE", "");
        String message = returnobj == null ? "" : returnobj.optString("MESSAGE", "");
        if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
            UIFuncs.errorSound(getContext());
            box.getBox("Err", message);
            return;
        }
        if (!TextUtils.isEmpty(message)) {
            box.getBox("Success", message, (dialog, which) -> focusCrateScan());
        }
        removeSavedRow(text(txtCrate));
        txtCrate.setText("");
        txtQty.setText("");
        txtBin.setText("");
        focusCrateScan();
    }

    private void removeSavedRow(String crate) {
        int index = indexOfCrateRow(crate);
        if (index < 0) {
            index = indexOfCrate(crate);
        }
        if (index >= 0) {
            ScannedRow row = rows.remove(index);
            if (!TextUtils.isEmpty(row.bin)) {
                validBins.remove(binKey(row.bin));
            }
        }
        crateCatalog.remove(binKey(crate));
        renumberRows();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static class CrateQty {
        final String crate;
        final String qty;

        CrateQty(String crate, String qty) {
            this.crate = crate;
            this.qty = qty;
        }
    }

    private static class ScannedRow {
        String srNo;
        String bin;
        String crate;
        String qty;
        boolean assigned;

        ScannedRow(String srNo, String bin, String crate, String qty) {
            this(srNo, bin, crate, qty, false);
        }

        ScannedRow(String srNo, String bin, String crate, String qty, boolean assigned) {
            this.srNo = srNo;
            this.bin = bin;
            this.crate = crate;
            this.qty = qty;
            this.assigned = assigned;
        }
    }

    private static class ScannedAdapter extends RecyclerView.Adapter<ScannedAdapter.RowHolder> {
        private final List<ScannedRow> rows;

        ScannedAdapter(List<ScannedRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_msa_scanned_crate, parent, false);
            return new RowHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            ScannedRow row = rows.get(position);
            holder.srNo.setText(row.srNo);
            holder.bin.setText(row.bin);
            holder.crate.setText(row.crate);
            holder.qty.setText(row.qty);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class RowHolder extends RecyclerView.ViewHolder {
            final TextView srNo;
            final TextView bin;
            final TextView crate;
            final TextView qty;

            RowHolder(View itemView) {
                super(itemView);
                srNo = itemView.findViewById(R.id.txt_msa_row_srno);
                bin = itemView.findViewById(R.id.txt_msa_row_bin);
                crate = itemView.findViewById(R.id.txt_msa_row_crate);
                qty = itemView.findViewById(R.id.txt_msa_row_qty);
            }
        }
    }
}
