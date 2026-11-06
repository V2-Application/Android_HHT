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
import java.util.List;
import java.util.Locale;

/**
 * GRT Putway-MSA Bin. Scanning an MSA bin calls {@link Vars#ZWM_GRT_PUTWAY_BIN_RFC}
 * and binds each returned LGPLA into the BIN column. Scanning a crate calls
 * {@link Vars#ZWM_GRT_PUTWAY_CRATE_RFC} and maps EX_CRATE and EX_QTY onto that bin.
 * Save posts each mapped crate and bin through {@link Vars#ZWM_RFC_GRT_PUTWAY_POST}.
 */
public class FragmentGrtPutwayMsaBin extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentGrtPutwayMsaBin";
    private static final String TITLE = "GRT Putway-MSA Bin";
    private static final int REQUEST_BIN = 1;
    private static final int REQUEST_CRATE = 2;
    private static final int REQUEST_SAVE = 3;

    private EditText txtMsaBin;
    private EditText txtBinNo;
    private EditText txtCrateNo;
    private EditText txtQty;
    private RecyclerView rowList;
    private final List<BinRow> rows = new ArrayList<>();
    private BinAdapter adapter;
    private AlertBox box;
    private ProgressDialog dialog;
    private String url = "";
    private String werks = "";
    private String user = "";
    private boolean loadInProgress;
    private boolean suppressScan;
    private int activeRequest;
    /** Bin shown after the last crate map, so the next crate can take the next open bin. */
    private String lastMappedBin = "";
    private final List<BinRow> pendingSave = new ArrayList<>();
    private String saveMessage = "";

    public FragmentGrtPutwayMsaBin() {
    }

    public static FragmentGrtPutwayMsaBin newInstance() {
        return new FragmentGrtPutwayMsaBin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grt_putway_msa_bin, container, false);
        box = new AlertBox(getContext());
        SharedPreferencesData data = new SharedPreferencesData(getContext());
        url = data.read("URL");
        werks = data.read("WERKS");
        user = data.read("USER");

        txtMsaBin = view.findViewById(R.id.txt_grt_msa_bin_scan);
        txtBinNo = view.findViewById(R.id.txt_grt_msa_bin_no);
        txtCrateNo = view.findViewById(R.id.txt_grt_msa_crate_no);
        txtQty = view.findViewById(R.id.txt_grt_msa_qty);
        Button btnReset = view.findViewById(R.id.btn_grt_msa_reset);
        Button btnSave = view.findViewById(R.id.btn_grt_msa_save);
        btnReset.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        rowList = view.findViewById(R.id.rv_grt_msa_rows);
        rowList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BinAdapter(rows);
        rowList.setAdapter(adapter);

        txtMsaBin.setOnEditorActionListener(this::onMsaBinDone);
        txtBinNo.setOnEditorActionListener(this::onBinNoDone);
        txtCrateNo.setOnEditorActionListener(this::onCrateDone);
        UIFuncs.disableKeyInput(txtMsaBin, txtMsaBin, getContext());
        UIFuncs.disableKeyInput(txtBinNo, txtBinNo, getContext());
        UIFuncs.disableKeyInput(txtCrateNo, txtCrateNo, getContext());
        watchScanner(txtMsaBin, () -> loadBins(text(txtMsaBin)));
        watchScanner(txtBinNo, () -> acceptBinNo(text(txtBinNo)));
        watchScanner(txtCrateNo, () -> loadCrate(text(txtCrateNo)));
        return view;
    }

    @Override
    public void onDestroyView() {
        dismissDialog();
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
        if (view.getId() == R.id.btn_grt_msa_reset) {
            if (!loadInProgress) {
                clearForm();
            }
        } else if (view.getId() == R.id.btn_grt_msa_save) {
            save();
        }
    }

    private boolean onMsaBinDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        loadBins(text(txtMsaBin));
        return true;
    }

    private boolean onBinNoDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        acceptBinNo(text(txtBinNo));
        return true;
    }

    private boolean onCrateDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        loadCrate(text(txtCrateNo));
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
                if (!scannerReading || loadInProgress || suppressScan) {
                    return;
                }
                if (!s.toString().trim().isEmpty()) {
                    onScan.run();
                }
            }
        });
    }

    private void loadBins(String bin) {
        if (loadInProgress) {
            return;
        }
        bin = bin == null ? "" : bin.trim().toUpperCase(Locale.ROOT);
        if (bin.isEmpty()) {
            box.getBox("Alert", "Please scan MSA bin.");
            txtMsaBin.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        if (!bin.equals(text(txtMsaBin))) {
            txtMsaBin.setText(bin);
            txtMsaBin.setSelection(bin.length());
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_BIN;
            args.put("bapiname", Vars.ZWM_GRT_PUTWAY_BIN_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(args);
        } catch (JSONException e) {
            loadInProgress = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void showProcessingAndSubmit(JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        submitRequest(args);
    }

    private void submitRequest(JSONObject args) {
        if (url == null || !url.contains("/")) {
            dismissDialog();
            loadInProgress = false;
            box.getBox("Err", "Gateway URL is not configured");
            return;
        }

        String rfc = args.optString("bapiname", Vars.ZWM_GRT_PUTWAY_BIN_RFC);
        String rfcUrl = url.substring(0, url.lastIndexOf("/"));
        rfcUrl += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "payload -> " + args);

        RequestQueue requestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, args,
                responsebody -> {
                    dismissDialog();
                    loadInProgress = false;
                    Log.d(TAG, "response -> " + responsebody);
                    if (!isAdded()) {
                        return;
                    }
                    if (responsebody == null || responsebody.length() == 0) {
                        UIFuncs.errorSound(getContext());
                        box.getBox("Err", "No response from Server");
                        onRequestFailed();
                        return;
                    }
                    if (activeRequest == REQUEST_CRATE) {
                        handleCrateResponse(responsebody);
                    } else if (activeRequest == REQUEST_SAVE) {
                        handleSaveResponse(responsebody);
                    } else {
                        handleBinResponse(responsebody);
                    }
                }, error -> {
            Log.i(TAG, "Error: " + error);
            dismissDialog();
            loadInProgress = false;
            if (!isAdded()) {
                return;
            }
            UIFuncs.errorSound(getContext());
            box.getBox("Err", volleyErrorMessage(error));
            onRequestFailed();
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

    private void handleBinResponse(JSONObject responsebody) {
        if ("fail".equalsIgnoreCase(responsebody.optString("status"))) {
            UIFuncs.errorSound(getContext());
            box.getBox("Err", responsebody.optString("error", "Request failed."));
            bindBins(new ArrayList<BinRow>());
            txtMsaBin.requestFocus();
            return;
        }
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        if (returnobj != null) {
            String type = returnobj.optString("TYPE", "");
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", returnobj.optString("MESSAGE", "MSA bin lookup failed."));
                bindBins(new ArrayList<BinRow>());
                txtMsaBin.setText("");
                txtMsaBin.requestFocus();
                return;
            }
        }
        JSONArray etData = responsebody.optJSONArray("ET_DATA");
        List<BinRow> loaded = new ArrayList<>();
        if (etData != null) {
            for (int i = 0; i < etData.length(); i++) {
                JSONObject item = etData.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String bin = item.optString("LGPLA", "").trim();
                if (bin.isEmpty()) {
                    continue;
                }
                loaded.add(new BinRow(String.valueOf(loaded.size() + 1), bin, "", ""));
            }
        }
        if (loaded.isEmpty()) {
            box.getBox("Alert", "No bin found for the scanned MSA bin.");
            bindBins(loaded);
            txtMsaBin.requestFocus();
            return;
        }
        bindBins(loaded);
        lastMappedBin = "";
        txtBinNo.setText("");
        txtCrateNo.setText("");
        txtQty.setText("");
        txtBinNo.requestFocus();
    }

    private void acceptBinNo(String bin) {
        bin = bin == null ? "" : bin.trim().toUpperCase(Locale.ROOT);
        if (bin.isEmpty()) {
            box.getBox("Alert", "Please scan bin.");
            txtBinNo.requestFocus();
            return;
        }
        if (rows.isEmpty()) {
            box.getBox("Alert", "Please scan MSA bin.");
            txtBinNo.setText("");
            txtMsaBin.requestFocus();
            return;
        }
        int index = indexOfBin(bin);
        if (index < 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Bin not found in the list.");
            txtBinNo.setText("");
            txtBinNo.requestFocus();
            return;
        }
        if (!rows.get(index).crate.isEmpty()) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "This bin is already assigned to a crate.");
            txtBinNo.setText("");
            txtBinNo.requestFocus();
            return;
        }
        String matched = rows.get(index).bin;
        if (!matched.equals(text(txtBinNo))) {
            txtBinNo.setText(matched);
            txtBinNo.setSelection(matched.length());
        }
        lastMappedBin = "";
        txtCrateNo.requestFocus();
    }

    private void loadCrate(String crate) {
        if (loadInProgress) {
            return;
        }
        crate = crate == null ? "" : crate.trim().toUpperCase(Locale.ROOT);
        if (crate.isEmpty()) {
            box.getBox("Alert", "Please scan crate.");
            txtCrateNo.requestFocus();
            return;
        }
        if (rows.isEmpty()) {
            box.getBox("Alert", "Please scan MSA bin.");
            txtCrateNo.setText("");
            txtMsaBin.requestFocus();
            return;
        }
        if (indexOfCrate(crate) >= 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "This crate is already in the list.");
            clearCrateScan();
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        loadInProgress = true;
        if (!crate.equals(text(txtCrateNo))) {
            txtCrateNo.setText(crate);
            txtCrateNo.setSelection(crate.length());
        }
        JSONObject args = new JSONObject();
        try {
            activeRequest = REQUEST_CRATE;
            args.put("bapiname", Vars.ZWM_GRT_PUTWAY_CRATE_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(args);
        } catch (JSONException e) {
            loadInProgress = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void handleCrateResponse(JSONObject responsebody) {
        if ("fail".equalsIgnoreCase(responsebody.optString("status"))) {
            UIFuncs.errorSound(getContext());
            box.getBox("Err", responsebody.optString("error", "Request failed."));
            clearCrateScan();
            return;
        }
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        if (returnobj != null) {
            String type = returnobj.optString("TYPE", "");
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", returnobj.optString("MESSAGE", "Crate lookup failed."));
                clearCrateScan();
                return;
            }
        }
        String crate = responsebody.optString("EX_CRATE", "").trim();
        String qty = displayCount(responsebody.opt("EX_QTY"));
        if (crate.isEmpty()) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "No crate returned for the scan.");
            clearCrateScan();
            return;
        }
        if (indexOfCrate(crate) >= 0) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "This crate is already in the list.");
            clearCrateScan();
            return;
        }
        int binIndex = resolveBinIndex();
        if (binIndex == -2) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "Bin not found in the list.");
            txtBinNo.setText("");
            txtBinNo.requestFocus();
            return;
        }
        if (binIndex == -3) {
            UIFuncs.errorSound(getContext());
            box.getBox("Alert", "This bin is already assigned to a crate.");
            txtBinNo.setText("");
            txtBinNo.requestFocus();
            return;
        }
        if (binIndex < 0) {
            box.getBox("Alert", "No open bin left in the list.");
            clearCrateScan();
            return;
        }
        BinRow target = rows.remove(binIndex);
        target.crate = crate;
        target.qty = qty;
        rows.add(0, target);
        renumberRows();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (rowList != null) {
            rowList.scrollToPosition(0);
        }
        lastMappedBin = target.bin;
        suppressScan = true;
        txtBinNo.setText(target.bin);
        txtQty.setText(qty);
        txtCrateNo.setText("");
        suppressScan = false;
        txtCrateNo.requestFocus();
    }

    /**
     * BIN NO selects the bin when the user scanned one. Otherwise the first
     * bin that does not yet have a crate is used.
     * -2 bin not in the list, -3 bin already assigned, -1 no open bin.
     */
    private int resolveBinIndex() {
        String scannedBin = text(txtBinNo);
        boolean userPickedBin = !scannedBin.isEmpty()
                && !binKey(scannedBin).equals(binKey(lastMappedBin));
        if (userPickedBin) {
            int index = indexOfBin(scannedBin);
            if (index < 0) {
                return -2;
            }
            if (!rows.get(index).crate.isEmpty()) {
                return -3;
            }
            return index;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).crate.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void renumberRows() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).srNo = String.valueOf(i + 1);
        }
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

    private int indexOfCrate(String crate) {
        String key = binKey(crate);
        if (key.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (key.equals(binKey(rows.get(i).crate))) {
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

    private void clearCrateScan() {
        txtCrateNo.setText("");
        txtQty.setText("");
        txtCrateNo.requestFocus();
    }

    private void onRequestFailed() {
        if (activeRequest == REQUEST_SAVE) {
            pendingSave.clear();
            return;
        }
        if (activeRequest == REQUEST_CRATE) {
            clearCrateScan();
            return;
        }
        bindBins(new ArrayList<BinRow>());
    }

    private void save() {
        if (loadInProgress) {
            return;
        }
        pendingSave.clear();
        for (BinRow row : rows) {
            if (!TextUtils.isEmpty(row.bin) && !TextUtils.isEmpty(row.crate)) {
                pendingSave.add(row);
            }
        }
        if (pendingSave.isEmpty()) {
            box.getBox("Alert", "Please scan crate.");
            txtCrateNo.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            pendingSave.clear();
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        saveMessage = "";
        postNext();
    }

    private void postNext() {
        if (pendingSave.isEmpty()) {
            return;
        }
        BinRow row = pendingSave.get(0);
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_SAVE;
            args.put("bapiname", Vars.ZWM_RFC_GRT_PUTWAY_POST);
            args.put("IM_WERKS", werks);
            args.put("IM_CRATE", row.crate);
            args.put("IM_LGPLA", row.bin);
            args.put("IM_USER", user);
            showProcessingAndSubmit(args);
        } catch (JSONException e) {
            loadInProgress = false;
            pendingSave.clear();
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void handleSaveResponse(JSONObject responsebody) {
        if ("fail".equalsIgnoreCase(responsebody.optString("status"))) {
            pendingSave.clear();
            UIFuncs.errorSound(getContext());
            box.getBox("Err", responsebody.optString("error", "Save failed."));
            return;
        }
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        String type = returnobj == null ? "" : returnobj.optString("TYPE", "");
        String message = returnobj == null ? "" : returnobj.optString("MESSAGE", "").trim();
        if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
            pendingSave.clear();
            UIFuncs.errorSound(getContext());
            box.getBox("Err", message.isEmpty() ? "Save failed." : message);
            return;
        }
        if (!message.isEmpty()) {
            saveMessage = message;
        }
        if (!pendingSave.isEmpty()) {
            removeSavedRow(pendingSave.remove(0));
        }
        if (!pendingSave.isEmpty()) {
            postNext();
            return;
        }
        suppressScan = true;
        txtBinNo.setText("");
        txtCrateNo.setText("");
        txtQty.setText("");
        suppressScan = false;
        lastMappedBin = "";
        String shown = saveMessage.isEmpty() ? "Saved successfully." : saveMessage;
        saveMessage = "";
        box.getBox("Success", shown);
        txtCrateNo.requestFocus();
    }

    private void removeSavedRow(BinRow saved) {
        for (int i = 0; i < rows.size(); i++) {
            BinRow row = rows.get(i);
            if (binKey(row.bin).equals(binKey(saved.bin))
                    && binKey(row.crate).equals(binKey(saved.crate))) {
                rows.remove(i);
                break;
            }
        }
        renumberRows();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void bindBins(List<BinRow> loaded) {
        rows.clear();
        rows.addAll(loaded);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void clearForm() {
        txtMsaBin.setText("");
        txtBinNo.setText("");
        txtCrateNo.setText("");
        txtQty.setText("");
        lastMappedBin = "";
        pendingSave.clear();
        saveMessage = "";
        bindBins(new ArrayList<BinRow>());
        txtMsaBin.requestFocus();
    }

    private static String text(EditText field) {
        return field == null || field.getText() == null ? "" : field.getText().toString().trim();
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

    private static class BinRow {
        String srNo;
        final String bin;
        String crate;
        String qty;

        BinRow(String srNo, String bin, String crate, String qty) {
            this.srNo = srNo;
            this.bin = bin;
            this.crate = crate;
            this.qty = qty;
        }
    }

    private static class BinAdapter extends RecyclerView.Adapter<BinAdapter.RowHolder> {
        private final List<BinRow> rows;

        BinAdapter(List<BinRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grt_putway_msa_bin, parent, false);
            return new RowHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            BinRow row = rows.get(position);
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
                srNo = itemView.findViewById(R.id.txt_grt_msa_row_srno);
                bin = itemView.findViewById(R.id.txt_grt_msa_row_bin);
                crate = itemView.findViewById(R.id.txt_grt_msa_row_crate);
                qty = itemView.findViewById(R.id.txt_grt_msa_row_qty);
            }
        }
    }
}
