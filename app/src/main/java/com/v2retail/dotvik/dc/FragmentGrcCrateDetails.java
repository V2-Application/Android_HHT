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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * GRC Crate Details. Scanning a crate loads PO, GRC, vendor, and Put PND Crate
 * through {@link Vars#ZWM_CRATE_GET_DATA_RFC}. TTL No. Crate is filled from
 * {@link Vars#ZWM_CRATE_PEN_RFC} when a GRC is selected. Empty Bin Availability
 * is loaded through {@link Vars#ZWM_CRATE_COUNT_RFC}. Next calls
 * {@link Vars#ZWM_CRATE_GET_QTY_RFC} and opens MSA Crate Putway with that list.
 */
public class FragmentGrcCrateDetails extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentGrcCrateDetails";
    private static final String TITLE = "GRC Crate Details";
    private static final String SELECT_PO = "Select PO";
    private static final String SELECT_GRC = "Select GRC";
    private static final int REQUEST_GET = 1;
    private static final int REQUEST_PEN = 2;
    private static final int REQUEST_COUNT = 3;
    private static final int REQUEST_QTY = 4;

    private EditText txtScanCrate;
    private EditText txtVendor;
    private EditText txtTtl;
    private EditText txtPutPnd;
    private Spinner ddPo;
    private Spinner ddGrc;
    private ArrayAdapter<String> poAdapter;
    private ArrayAdapter<String> grcAdapter;
    private final List<String> poOptions = new ArrayList<>();
    private final List<String> grcOptions = new ArrayList<>();
    private final List<CrateRow> loadedRows = new ArrayList<>();
    private final List<EmptyBinRow> binRows = new ArrayList<>();
    private EmptyBinAdapter binAdapter;
    private AlertBox box;
    private ProgressDialog dialog;
    private String url = "";
    private String werks = "";
    private String user = "";
    private boolean ignorePoSelection;
    private boolean ignoreGrcSelection;
    private boolean loadInProgress;
    private int activeRequest;
    private String nextPo = "";
    private String nextGrc = "";

    public FragmentGrcCrateDetails() {
    }

    public static FragmentGrcCrateDetails newInstance() {
        return new FragmentGrcCrateDetails();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grc_crate_details, container, false);
        box = new AlertBox(getContext());
        SharedPreferencesData data = new SharedPreferencesData(getContext());
        url = data.read("URL");
        werks = data.read("WERKS");
        user = data.read("USER");

        txtScanCrate = view.findViewById(R.id.txt_grc_crate_scan);
        txtVendor = view.findViewById(R.id.txt_grc_crate_vendor);
        txtTtl = view.findViewById(R.id.txt_grc_crate_ttl);
        txtPutPnd = view.findViewById(R.id.txt_grc_crate_put_pnd);
        ddPo = view.findViewById(R.id.dd_grc_crate_po);
        ddGrc = view.findViewById(R.id.dd_grc_crate_grc);
        Button btnBack = view.findViewById(R.id.btn_grc_crate_back);
        Button btnNext = view.findViewById(R.id.btn_grc_crate_next);

        btnBack.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        txtScanCrate.setOnEditorActionListener(this::onCrateScanDone);
        watchScanner(txtScanCrate, () -> loadCrateData(text(txtScanCrate)));

        setupPoSpinner();
        setupGrcSpinner();
        setupBinList(view);
        loadEmptyBins();
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
        dismissDialog();
        super.onDestroyView();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_grc_crate_back) {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        } else if (id == R.id.btn_grc_crate_next) {
            onNext();
        }
    }

    private boolean onCrateScanDone(TextView textView, int actionId, KeyEvent event) {
        if (!isScanCommit(actionId, event)) {
            return false;
        }
        UIFuncs.hideKeyboard(getActivity());
        loadCrateData(text(txtScanCrate));
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

    private void setupPoSpinner() {
        poOptions.clear();
        poOptions.add(SELECT_PO);
        poAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, poOptions);
        poAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ddPo.setAdapter(poAdapter);
        ddPo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignorePoSelection) {
                    return;
                }
                onPoSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupGrcSpinner() {
        grcOptions.clear();
        grcOptions.add(SELECT_GRC);
        grcAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, grcOptions);
        grcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ddGrc.setAdapter(grcAdapter);
        ddGrc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreGrcSelection) {
                    return;
                }
                onGrcSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupBinList(View view) {
        RecyclerView list = view.findViewById(R.id.rv_grc_crate_empty_bins);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        binAdapter = new EmptyBinAdapter(binRows);
        list.setAdapter(binAdapter);
    }

    private void loadEmptyBins() {
        if (loadInProgress) {
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_COUNT;
            args.put("bapiname", Vars.ZWM_CRATE_COUNT_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            showProcessingAndSubmit(Vars.ZWM_CRATE_COUNT_RFC, args);
        } catch (JSONException e) {
            loadInProgress = false;
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
        }
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
        if (!crate.equals(text(txtScanCrate))) {
            txtScanCrate.setText(crate);
            txtScanCrate.setSelection(crate.length());
        }
        JSONObject args = new JSONObject();
        try {
            loadInProgress = true;
            activeRequest = REQUEST_GET;
            args.put("bapiname", Vars.ZWM_CRATE_GET_DATA_RFC);
            args.put("IM_USER", user);
            args.put("IM_PLANT", werks);
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_CRATE_GET_DATA_RFC, args);
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
                        box.getBox("Err", "No response from Server");
                        onRequestFailed();
                        return;
                    }
                    if (activeRequest == REQUEST_PEN) {
                        handlePendingResponse(responsebody);
                    } else if (activeRequest == REQUEST_COUNT) {
                        handleEmptyBinResponse(responsebody);
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

    private void handleResponse(JSONObject responsebody) {
        try {
            if ("fail".equalsIgnoreCase(responsebody.optString("status"))) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", responsebody.optString("error", "Request failed."));
                clearLoadedData();
                return;
            }
            JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
            if (returnobj != null) {
                String type = returnobj.optString("TYPE", "");
                if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                    UIFuncs.errorSound(getContext());
                    box.getBox("Err", returnobj.optString("MESSAGE", "No data found."));
                    clearLoadedData();
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
                    String po = item.optString("EBELN", "").trim();
                    String grc = item.optString("MBLNR", "").trim();
                    if (po.isEmpty() && grc.isEmpty()) {
                        continue;
                    }
                    rows.add(new CrateRow(
                            po,
                            grc,
                            displayCount(item.opt("CRATE_COUNT")),
                            item.optString("VENDOR_NAME", "").trim()));
                }
            }
            if (rows.isEmpty()) {
                box.getBox("Alert", "No data found for scanned crate.");
                clearLoadedData();
                return;
            }
            loadedRows.clear();
            loadedRows.addAll(rows);
            bindDropdowns();
        } catch (Exception e) {
            UIFuncs.errorSound(getContext());
            box.getErrBox(e);
            clearLoadedData();
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

    private void handleEmptyBinResponse(JSONObject responsebody) {
        JSONObject returnobj = responsebody.optJSONObject("EX_RETURN");
        if (returnobj != null) {
            String type = returnobj.optString("TYPE", "");
            if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                UIFuncs.errorSound(getContext());
                box.getBox("Err", returnobj.optString("MESSAGE", "Empty bin lookup failed."));
                bindEmptyBins(new ArrayList<EmptyBinRow>());
                return;
            }
        }
        JSONArray etData = responsebody.optJSONArray("ET_DATA");
        List<EmptyBinRow> rows = new ArrayList<>();
        if (etData != null) {
            for (int i = 0; i < etData.length(); i++) {
                JSONObject item = etData.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String section = item.optString("SECTION", "").trim();
                String emptyBin = displayCount(item.opt("EMPTY_BIN"));
                if (section.isEmpty() && emptyBin.isEmpty()) {
                    continue;
                }
                rows.add(new EmptyBinRow(String.valueOf(rows.size() + 1), section, emptyBin));
            }
        }
        bindEmptyBins(rows);
    }

    private void bindEmptyBins(List<EmptyBinRow> rows) {
        binRows.clear();
        binRows.addAll(rows);
        if (binAdapter != null) {
            binAdapter.notifyDataSetChanged();
        }
    }

    private void onRequestFailed() {
        if (activeRequest == REQUEST_PEN) {
            setTtlText("");
            return;
        }
        if (activeRequest == REQUEST_COUNT) {
            bindEmptyBins(new ArrayList<EmptyBinRow>());
            return;
        }
        if (activeRequest == REQUEST_QTY) {
            return;
        }
        clearLoadedData();
    }

    private void setTtlText(String value) {
        if (txtTtl == null) {
            return;
        }
        txtTtl.setText(value);
    }

    private void bindDropdowns() {
        LinkedHashSet<String> pos = new LinkedHashSet<>();
        LinkedHashSet<String> grcs = new LinkedHashSet<>();
        for (CrateRow row : loadedRows) {
            if (!row.po.isEmpty()) {
                pos.add(row.po);
            }
            if (!row.grc.isEmpty()) {
                grcs.add(row.grc);
            }
        }

        ignorePoSelection = true;
        ignoreGrcSelection = true;
        poOptions.clear();
        poOptions.add(SELECT_PO);
        poOptions.addAll(pos);
        grcOptions.clear();
        grcOptions.add(SELECT_GRC);
        grcOptions.addAll(grcs);
        poAdapter.notifyDataSetChanged();
        grcAdapter.notifyDataSetChanged();
        clearDetails();

        String pendingGrc = "";
        if (loadedRows.size() == 1) {
            selectValue(ddPo, poOptions, loadedRows.get(0).po);
            selectValue(ddGrc, grcOptions, loadedRows.get(0).grc);
            applyRow(loadedRows.get(0));
            pendingGrc = loadedRows.get(0).grc;
        } else {
            ddPo.setSelection(0);
            ddGrc.setSelection(0);
        }
        ignorePoSelection = false;
        ignoreGrcSelection = false;
        if (!pendingGrc.isEmpty()) {
            lookupPendingCrates(pendingGrc);
        }
    }

    private void onPoSelected(int position) {
        if (position <= 0) {
            return;
        }
        String po = poOptions.get(position);
        List<CrateRow> matches = new ArrayList<>();
        for (CrateRow row : loadedRows) {
            if (po.equals(row.po)) {
                matches.add(row);
            }
        }
        ignoreGrcSelection = true;
        grcOptions.clear();
        grcOptions.add(SELECT_GRC);
        for (CrateRow row : matches) {
            if (!row.grc.isEmpty() && !grcOptions.contains(row.grc)) {
                grcOptions.add(row.grc);
            }
        }
        grcAdapter.notifyDataSetChanged();
        String pendingGrc = "";
        if (matches.size() == 1) {
            selectValue(ddGrc, grcOptions, matches.get(0).grc);
            applyRow(matches.get(0));
            pendingGrc = matches.get(0).grc;
        } else {
            ddGrc.setSelection(0);
            txtVendor.setText(sameVendor(matches));
            setTtlText("");
            txtPutPnd.setText("");
        }
        ignoreGrcSelection = false;
        if (!pendingGrc.isEmpty()) {
            lookupPendingCrates(pendingGrc);
        }
    }

    private void onGrcSelected(int position) {
        if (position <= 0) {
            return;
        }
        String grc = grcOptions.get(position);
        String selectedPo = ddPo.getSelectedItemPosition() > 0
                ? poOptions.get(ddPo.getSelectedItemPosition()) : "";
        CrateRow match = null;
        for (CrateRow row : loadedRows) {
            if (!grc.equals(row.grc)) {
                continue;
            }
            if (!selectedPo.isEmpty() && !selectedPo.equals(row.po)) {
                continue;
            }
            match = row;
            break;
        }
        if (match == null) {
            return;
        }
        applyRow(match);
        lookupPendingCrates(match.grc);
        int poIndex = poOptions.indexOf(match.po);
        if (poIndex > 0 && ddPo.getSelectedItemPosition() != poIndex) {
            ignorePoSelection = true;
            ddPo.setSelection(poIndex);
            ignorePoSelection = false;
        }
    }

    private void applyRow(CrateRow row) {
        txtVendor.setText(row.vendorName);
        txtPutPnd.setText(row.crateCount);
    }

    private void onNext() {
        if (loadInProgress) {
            return;
        }
        if (ddPo.getSelectedItemPosition() <= 0) {
            box.getBox("Alert", "Please select PO No.");
            return;
        }
        if (ddGrc.getSelectedItemPosition() <= 0) {
            box.getBox("Alert", "Please select GRC No.");
            return;
        }
        if (TextUtils.isEmpty(werks)) {
            box.getBox("Alert", "Plant is not configured.");
            return;
        }
        nextPo = poOptions.get(ddPo.getSelectedItemPosition());
        nextGrc = grcOptions.get(ddGrc.getSelectedItemPosition());
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
        JSONArray etData = responsebody.optJSONArray("ET_DATA");
        String payload = etData == null ? "[]" : etData.toString();
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.replace(R.id.home, FragmentMsaCratePutway.newInstance(nextPo, nextGrc, payload), "inward");
        ft.addToBackStack("inward");
        ft.commit();
    }

    private void clearLoadedData() {
        loadedRows.clear();
        ignorePoSelection = true;
        ignoreGrcSelection = true;
        poOptions.clear();
        poOptions.add(SELECT_PO);
        grcOptions.clear();
        grcOptions.add(SELECT_GRC);
        if (poAdapter != null) {
            poAdapter.notifyDataSetChanged();
        }
        if (grcAdapter != null) {
            grcAdapter.notifyDataSetChanged();
        }
        if (ddPo != null) {
            ddPo.setSelection(0);
        }
        if (ddGrc != null) {
            ddGrc.setSelection(0);
        }
        clearDetails();
        ignorePoSelection = false;
        ignoreGrcSelection = false;
    }

    private void clearDetails() {
        if (txtVendor != null) {
            txtVendor.setText("");
        }
        setTtlText("");
        if (txtPutPnd != null) {
            txtPutPnd.setText("");
        }
    }

    private static void selectValue(Spinner spinner, List<String> options, String value) {
        int index = options.indexOf(value);
        spinner.setSelection(index > 0 ? index : 0);
    }

    private static String sameVendor(List<CrateRow> rows) {
        String vendor = null;
        for (CrateRow row : rows) {
            if (vendor == null) {
                vendor = row.vendorName;
            } else if (!vendor.equals(row.vendorName)) {
                return "";
            }
        }
        return vendor == null ? "" : vendor;
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
        final String po;
        final String grc;
        final String crateCount;
        final String vendorName;

        CrateRow(String po, String grc, String crateCount, String vendorName) {
            this.po = po;
            this.grc = grc;
            this.crateCount = crateCount;
            this.vendorName = vendorName;
        }
    }

    private static class EmptyBinRow {
        final String srNo;
        final String section;
        final String emptyBin;

        EmptyBinRow(String srNo, String section, String emptyBin) {
            this.srNo = srNo;
            this.section = section;
            this.emptyBin = emptyBin;
        }
    }

    private static class EmptyBinAdapter extends RecyclerView.Adapter<EmptyBinAdapter.RowHolder> {
        private final List<EmptyBinRow> rows;

        EmptyBinAdapter(List<EmptyBinRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grc_done_empty_bin, parent, false);
            return new RowHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            EmptyBinRow row = rows.get(position);
            holder.srNo.setText(row.srNo);
            holder.section.setText(row.section);
            holder.emptyBin.setText(row.emptyBin);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class RowHolder extends RecyclerView.ViewHolder {
            final TextView srNo;
            final TextView section;
            final TextView emptyBin;

            RowHolder(View itemView) {
                super(itemView);
                srNo = itemView.findViewById(R.id.txt_empty_bin_srno);
                section = itemView.findViewById(R.id.txt_empty_bin_section);
                emptyBin = itemView.findViewById(R.id.txt_empty_bin_count);
            }
        }
    }
}
