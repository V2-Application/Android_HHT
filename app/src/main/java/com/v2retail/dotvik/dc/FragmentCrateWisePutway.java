package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.fragment.app.FragmentTransaction;
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
 * Crate Wise Putway. Independent of GRC Crate Details. Scanning a crate loads
 * ET_DATA rows (EBELN, CRATE, MBLNR) through {@link Vars#ZWM_CRATE_DETAILS_GET_RFC}
 * and fills TTL No. Crate from {@link Vars#ZWM_CRATE_PEN_RFC} for the selected
 * row. Next calls {@link Vars#ZWM_CRATE_GET_QTY_RFC} for every table row and
 * opens {@link FragmentCrateWisePutwayNext} with all Crate/QTY records preserved.
 */
public class FragmentCrateWisePutway extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentCrateWisePutway";
    private static final String TITLE = "Crate Wise Putway";
    private static final int REQUEST_GET = 1;
    private static final int REQUEST_PEN = 2;
    private static final int REQUEST_QTY = 4;

    private EditText txtScanCrate;
    private EditText txtTtl;
    private final List<CrateRow> loadedRows = new ArrayList<>();
    private CrateDataAdapter dataAdapter;
    private int selectedIndex = -1;
    private AlertBox box;
    private ProgressDialog dialog;
    private String url = "";
    private String werks = "";
    private String user = "";
    private boolean loadInProgress;
    private int activeRequest;
    private int ttlCount;
    private boolean suppressScan;
    /** Crates already scanned successfully (kept in memory for this screen's lifetime). */
    private final java.util.Set<String> scannedCrates = new java.util.HashSet<>();
    private String pendingCrate = "";
    private String nextPo = "";
    private String nextGrc = "";
    /** Unique EBELN/MBLNR pairs from the table, fetched one-by-one for Next. */
    private final List<CrateRow> qtyQueue = new ArrayList<>();
    private int qtyQueueIndex;
    private JSONArray mergedQtyData = new JSONArray();
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoScan = () -> {
        if (!isAdded() || loadInProgress || suppressScan) {
            return;
        }
        String crate = text(txtScanCrate);
        if (crate.length() > 3) {
            loadCrateData(crate);
        }
    };

    public FragmentCrateWisePutway() {
    }

    public static FragmentCrateWisePutway newInstance() {
        return new FragmentCrateWisePutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crate_wise_putway, container, false);
        box = new AlertBox(getContext());
        SharedPreferencesData data = new SharedPreferencesData(getContext());
        url = data.read("URL");
        werks = data.read("WERKS");
        user = data.read("USER");

        txtScanCrate = view.findViewById(R.id.txt_crate_wise_scan);
        txtTtl = view.findViewById(R.id.txt_crate_wise_ttl);
        Button btnBack = view.findViewById(R.id.btn_crate_wise_back);
        Button btnNext = view.findViewById(R.id.btn_crate_wise_next);

        btnBack.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        txtScanCrate.setOnEditorActionListener(this::onCrateScanDone);
        watchScanner(txtScanCrate, () -> loadCrateData(text(txtScanCrate)));

        setupDataList(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle(TITLE);
        }
        if (txtScanCrate != null) {
            txtScanCrate.post(() -> txtScanCrate.requestFocus());
        }
    }

    @Override
    public void onDestroyView() {
        scanHandler.removeCallbacks(autoScan);
        dismissDialog();
        super.onDestroyView();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_crate_wise_back) {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        } else if (id == R.id.btn_crate_wise_next) {
            onNext();
        }
    }

    private boolean onCrateScanDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        String crate = text(txtScanCrate);
        if (!crate.isEmpty()) {
            loadCrateData(crate);
        }
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
                if (suppressScan || loadInProgress) {
                    return;
                }
                scanHandler.removeCallbacks(autoScan);
                if (s.toString().trim().isEmpty()) {
                    return;
                }
                if (scannerReading) {
                    onScan.run();
                    return;
                }
                scanHandler.postDelayed(autoScan, 300);
            }
        });
    }

    private void setupDataList(View view) {
        RecyclerView list = view.findViewById(R.id.rv_crate_wise_data);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        dataAdapter = new CrateDataAdapter();
        list.setAdapter(dataAdapter);
    }

    private void loadCrateData(String crate) {
        if (loadInProgress) {
            return;
        }
        crate = crate == null ? "" : crate.trim().toUpperCase(Locale.ROOT);
        if (crate.isEmpty()) {
            box.getBox("Alert", "Please scan crate.");
            txtScanCrate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        if (scannedCrates.contains(crate)) {
            UIFuncs.errorSound(getContext());
            showScanError("Alert", "Crate " + crate + " is already scanned.");
            return;
        }
        pendingCrate = crate;
        if (!crate.equals(text(txtScanCrate))) {
            suppressScan = true;
            txtScanCrate.setText(crate);
            txtScanCrate.setSelection(crate.length());
            suppressScan = false;
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_GET;
            args.put("bapiname", Vars.ZWM_CRATE_DETAILS_GET_RFC);
            args.put("IM_USER", user);
            args.put("IM_WERKS", werks);
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_CRATE_DETAILS_GET_RFC, args);
        } catch (JSONException e) {
            loadInProgress = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void lookupPendingCrates(String mblnr) {
        if (loadInProgress) {
            return;
        }
        mblnr = mblnr == null ? "" : mblnr.trim();
        if (mblnr.isEmpty()) {
            setTtlText("");
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_PEN;
            args.put("bapiname", Vars.ZWM_CRATE_PEN_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_MBLNR", mblnr);
            showProcessingAndSubmit(Vars.ZWM_CRATE_PEN_RFC, args);
        } catch (JSONException e) {
            loadInProgress = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void showProcessingAndSubmit(String rfc, JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        submitRequest(rfc, args);
    }

    private void submitRequest(String rfc, JSONObject args) {
        if (url == null || !url.contains("/")) {
            dismissDialog();
            loadInProgress = false;
            box.getBox("Err", "Gateway URL is not configured");
            return;
        }

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
                        if (activeRequest == REQUEST_GET) {
                            showScanError("Err", "No response from Server");
                        } else {
                            box.getBox("Err", "No response from Server");
                        }
                        onRequestFailed();
                        return;
                    }
                    if (activeRequest == REQUEST_PEN) {
                        handlePendingResponse(responsebody);
                    } else if (activeRequest == REQUEST_QTY) {
                        handleQtyResponse(responsebody);
                    } else {
                        handleResponse(responsebody);
                    }
                }, error -> {
            Log.i(TAG, "Error: " + error);
            dismissDialog();
            loadInProgress = false;
            if (!isAdded()) {
                return;
            }
            UIFuncs.errorSound(getContext());
            if (activeRequest == REQUEST_GET) {
                showScanError("Err", volleyErrorMessage(error));
            } else {
                box.getBox("Err", volleyErrorMessage(error));
            }
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

    private void handleResponse(JSONObject responsebody) {
        try {
            if ("fail".equalsIgnoreCase(responsebody.optString("status"))) {
                UIFuncs.errorSound(getContext());
                showScanError("Err", responsebody.optString("error", "Request failed."));
                return;
            }
            JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
            if (returnobj != null) {
                String type = returnobj.optString("TYPE", "");
                if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                    UIFuncs.errorSound(getContext());
                    showScanError("Err", returnobj.optString("MESSAGE", "No data found."));
                    return;
                }
            }
            JSONArray etData = responsebody.optJSONArray("ET_DATA");
            List<CrateRow> rows = new ArrayList<>();
            if (etData != null) {
                for (int i = 0; i < etData.length(); i++) {
                    JSONObject item = etData.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String ebeln = item.optString("EBELN", "").trim();
                    String crate = item.optString("CRATE", "").trim();
                    String mblnr = item.optString("MBLNR", "").trim();
                    if (crate.isEmpty()) {
                        crate = text(txtScanCrate);
                    }
                    if (ebeln.isEmpty() && crate.isEmpty() && mblnr.isEmpty()) {
                        continue;
                    }
                    rows.add(new CrateRow(ebeln, crate, mblnr));
                }
            }
            if (rows.isEmpty()) {
                showScanError("Alert", "No data found for scanned crate.");
                return;
            }
            loadedRows.addAll(rows);
            if (!pendingCrate.isEmpty()) {
                scannedCrates.add(pendingCrate);
            }
            for (CrateRow r : rows) {
                if (!r.crate.isEmpty()) {
                    scannedCrates.add(r.crate.trim().toUpperCase(Locale.ROOT));
                }
            }
            applyScannedCrate();
        } catch (Exception e) {
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void handlePendingResponse(JSONObject responsebody) {
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        if (returnobj != null) {
            String type = returnobj.optString("TYPE", "");
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", returnobj.optString("MESSAGE", "Crate lookup failed."));
                setTtlText("");
                return;
            }
        }
        setTtlText(displayCount(responsebody.opt("EX_NO_CRATE")));
    }

    /** Shows an alert for a crate scan problem; tapping OK clears the scan box. */
    private void showScanError(String title, String message) {
        box.getBox(title, message, (d, which) -> clearScanBox());
    }

    private void onRequestFailed() {
        if (activeRequest == REQUEST_PEN || activeRequest == REQUEST_QTY) {
            return;
        }
    }

    private void setTtlText(String value) {
        if (txtTtl == null) {
            return;
        }
        txtTtl.setText(value);
    }

    private void applyScannedCrate() {
        ttlCount++;
        setTtlText(String.valueOf(ttlCount));
        nextPo = "";
        nextGrc = "";
        selectedIndex = loadedRows.size() == 1 ? 0 : -1;
        if (selectedIndex == 0) {
            CrateRow row = loadedRows.get(0);
            nextPo = row.ebeln;
            nextGrc = row.mblnr;
        }
        if (dataAdapter != null) {
            dataAdapter.notifyDataSetChanged();
        }
        clearScanBox();
    }

    private void clearScanBox() {
        scanHandler.removeCallbacks(autoScan);
        if (txtScanCrate == null) {
            return;
        }
        suppressScan = true;
        txtScanCrate.setText("");
        suppressScan = false;
        txtScanCrate.requestFocus();
    }

    private void selectRow(int index) {
        if (index < 0 || index >= loadedRows.size() || loadInProgress) {
            return;
        }
        selectedIndex = index;
        CrateRow row = loadedRows.get(index);
        nextPo = row.ebeln;
        nextGrc = row.mblnr;
        if (dataAdapter != null) {
            dataAdapter.notifyDataSetChanged();
        }
    }

    private void onNext() {
        if (loadInProgress) {
            return;
        }
        // Scan box is cleared after a successful load; only require loaded rows.
        if (loadedRows.isEmpty()) {
            box.getBox("Alert", "Please scan crate.");
            txtScanCrate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        qtyQueue.clear();
        mergedQtyData = new JSONArray();
        qtyQueueIndex = 0;
        java.util.LinkedHashSet<String> seenPairs = new java.util.LinkedHashSet<>();
        for (CrateRow row : loadedRows) {
            String pairKey = row.ebeln + "|" + row.mblnr;
            if (seenPairs.add(pairKey)) {
                qtyQueue.add(row);
            }
        }
        if (qtyQueue.isEmpty()) {
            box.getBox("Alert", "Please scan crate.");
            txtScanCrate.requestFocus();
            return;
        }
        fetchNextQty();
    }

    private void fetchNextQty() {
        if (qtyQueueIndex >= qtyQueue.size()) {
            ensureAllScannedRowsInPayload();
            openNextScreen();
            return;
        }
        CrateRow row = qtyQueue.get(qtyQueueIndex);
        nextPo = row.ebeln;
        nextGrc = row.mblnr;
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_QTY;
            args.put("bapiname", Vars.ZWM_CRATE_GET_QTY_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_EBELN", nextPo);
            args.put("IM_MBLNR", nextGrc);
            showProcessingAndSubmit(Vars.ZWM_CRATE_GET_QTY_RFC, args);
        } catch (JSONException e) {
            loadInProgress = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
    }

    private void handleQtyResponse(JSONObject responsebody) {
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        if (returnobj != null) {
            String type = returnobj.optString("TYPE", "");
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", returnobj.optString("MESSAGE", "Qty lookup failed."));
                return;
            }
        }
        appendQtyRowsForCurrentPair(responsebody.optJSONArray("ET_DATA"));
        qtyQueueIndex++;
        fetchNextQty();
    }

    /**
     * Keeps only crates that appear in the first-screen table for this EBELN/MBLNR,
     * and stamps EBELN/MBLNR onto each qty row so the next screen can show them all.
     */
    private void appendQtyRowsForCurrentPair(JSONArray etData) {
        java.util.HashSet<String> wantedCrates = new java.util.HashSet<>();
        for (CrateRow row : loadedRows) {
            if (row.ebeln.equals(nextPo) && row.mblnr.equals(nextGrc)) {
                wantedCrates.add(row.crate.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (etData == null) {
            return;
        }
        for (int i = 0; i < etData.length(); i++) {
            JSONObject item = etData.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String crate = item.optString("CRATE", "").trim();
            if (crate.isEmpty() || !wantedCrates.contains(crate.toUpperCase(Locale.ROOT))) {
                continue;
            }
            try {
                JSONObject copy = new JSONObject(item.toString());
                copy.put("EBELN", nextPo);
                copy.put("MBLNR", nextGrc);
                if (!copy.has("CRATE")) {
                    copy.put("CRATE", crate);
                }
                mergedQtyData.put(copy);
            } catch (JSONException ignored) {
            }
        }
    }

    /** Guarantee every first-screen table row appears on Next, even if qty RFC omitted it. */
    private void ensureAllScannedRowsInPayload() {
        java.util.HashSet<String> present = new java.util.HashSet<>();
        for (int i = 0; i < mergedQtyData.length(); i++) {
            JSONObject item = mergedQtyData.optJSONObject(i);
            if (item == null) {
                continue;
            }
            // Only count rows the next screen will actually show (no bin yet).
            if (!item.optString("LGPLA", "").trim().isEmpty()) {
                continue;
            }
            present.add(item.optString("EBELN", "").trim() + "|"
                    + item.optString("MBLNR", "").trim() + "|"
                    + item.optString("CRATE", "").trim().toUpperCase(Locale.ROOT));
        }
        for (CrateRow row : loadedRows) {
            String key = row.ebeln + "|" + row.mblnr + "|"
                    + row.crate.trim().toUpperCase(Locale.ROOT);
            if (present.contains(key)) {
                continue;
            }
            try {
                JSONObject item = new JSONObject();
                item.put("EBELN", row.ebeln);
                item.put("MBLNR", row.mblnr);
                item.put("CRATE", row.crate);
                item.put("SCAN_QTY", "");
                item.put("LGPLA", "");
                mergedQtyData.put(item);
                present.add(key);
            } catch (JSONException ignored) {
            }
        }
    }

    private void openNextScreen() {
        String po = loadedRows.isEmpty() ? nextPo : loadedRows.get(0).ebeln;
        String grc = loadedRows.isEmpty() ? nextGrc : loadedRows.get(0).mblnr;
        String payload = mergedQtyData.toString();
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.replace(R.id.home, FragmentCrateWisePutwayNext.newInstance(po, grc, payload), "inward");
        ft.addToBackStack("inward");
        ft.commit();
    }

    private void clearLoadedData() {
        loadedRows.clear();
        scannedCrates.clear();
        pendingCrate = "";
        selectedIndex = -1;
        ttlCount = 0;
        nextPo = "";
        nextGrc = "";
        setTtlText("");
        if (dataAdapter != null) {
            dataAdapter.notifyDataSetChanged();
        }
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

    private static class CrateRow {
        final String ebeln;
        final String crate;
        final String mblnr;

        CrateRow(String ebeln, String crate, String mblnr) {
            this.ebeln = ebeln;
            this.crate = crate;
            this.mblnr = mblnr;
        }
    }

    private class CrateDataAdapter extends RecyclerView.Adapter<CrateDataAdapter.RowHolder> {

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_crate_wise_putway_empty_bin, parent, false);
            return new RowHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            CrateRow row = loadedRows.get(position);
            holder.ebeln.setText(row.ebeln);
            holder.crate.setText(row.crate);
            holder.mblnr.setText(row.mblnr);
            holder.row.setBackgroundColor(position == selectedIndex ? 0xFFFFE4E4 : 0xFFFFFFFF);
            holder.itemView.setOnClickListener(v -> selectRow(holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return loadedRows.size();
        }

        class RowHolder extends RecyclerView.ViewHolder {
            final View row;
            final TextView ebeln;
            final TextView crate;
            final TextView mblnr;

            RowHolder(View itemView) {
                super(itemView);
                row = itemView.findViewById(R.id.row_crate_wise_data);
                ebeln = itemView.findViewById(R.id.txt_crate_wise_ebeln);
                crate = itemView.findViewById(R.id.txt_crate_wise_crate);
                mblnr = itemView.findViewById(R.id.txt_crate_wise_mblnr);
            }
        }
    }
}
