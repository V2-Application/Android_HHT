package com.v2retail.dotvik.dc.binwisepicking;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.BinCrateHU;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentMSABinwisePicking extends Fragment implements View.OnClickListener {

    private static final int REQUEST_PICKLIST = 1501;
    private static final int REQUEST_BIN_DATA = 1502;
    private static final int REQUEST_VALIDATE_HU = 1503;
    private static final int REQUEST_SAVE = 1504;

    private static final String TAG = FragmentMSABinwisePicking.class.getName();

    /** HHT scanners often inject keystrokes one-by-one; debounce collects the full barcode before RFC. */
    private static final long SCAN_DEBOUNCE_MS = 600L;
    private static final int SCAN_MIN_LEN_HU = 2;
    /** Same threshold as HU so bin/crate auto-commit after scan matches Scan Ext HU behavior. */
    private static final int SCAN_MIN_LEN_MSA_FIELD = 2;

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    Button btn_back, btn_save;

    EditText txt_store, txt_scan_hu, txt_scanned_hu, txt_scan_msa_bin, txt_scanned_msa_bin, txt_scan_msa_crate, txt_scanned_msa_crate, txt_sqty_tqty;
    RecyclerView rvBinCrate;
    BinCratePickAdapter binCrateAdapter;

    Map<String, PicklistData> picklistDataMap = new LinkedHashMap<>();
    List<String> picklists = new ArrayList<String>();
    ArrayAdapter<String> picklistAdapter;
    boolean spinnerTouched = false;
    Spinner dd_picklist_list;

    Map<String, BinCrateHU> binDataMap = new LinkedHashMap<>();
    Map<String, BinCrateHU> scannedBinData = new LinkedHashMap<>();

    int totalScanned = 0;

    PicklistData currentPicklist = null;

    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable huCommitRunnable;
    /** Prevents duplicate ZBIN_GRT_HU_VALIDATION calls while a request is in flight. */
    private volatile boolean huValidationInFlight;
    /** Skip debounced HU validation while clearing/filling {@link #txt_scan_hu} programmatically. */
    private boolean suppressHuScanScheduling;

    private Runnable msaBinCommitRunnable;
    private Runnable msaCrateCommitRunnable;
    /** Skip debounced bin/crate validation while clearing fields programmatically. */
    private boolean suppressMsaBinScheduling;
    private boolean suppressMsaCrateScheduling;

    private ExecutorService binParseExecutor;

    public FragmentMSABinwisePicking() {
        // Required empty public constructor
    }

    public static FragmentMSABinwisePicking newInstance() {
        return new FragmentMSABinwisePicking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("MSA Binwise - Picking");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_msa_binwise_picking, container, false);

        con = getContext();
        box=new AlertBox(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        txt_store = rootView.findViewById(R.id.txt_msa_binwise_picking_process_store_code);
        txt_scan_hu = rootView.findViewById(R.id.txt_msa_binwise_picking_process_scan_ext_hu);
        txt_scanned_hu = rootView.findViewById(R.id.txt_msa_binwise_picking_process_msa_scanned_ext_hu);
        txt_scan_msa_bin = rootView.findViewById(R.id.txt_msa_binwise_picking_process_msa_bin);
        txt_scanned_msa_bin = rootView.findViewById(R.id.txt_msa_binwise_picking_process_msa_bin_scanned);
        txt_scan_msa_crate = rootView.findViewById(R.id.txt_msa_binwise_picking_process_msa_crate);
        txt_scanned_msa_crate = rootView.findViewById(R.id.txt_msa_binwise_picking_process_msa_crate_scanned);
        txt_sqty_tqty = rootView.findViewById(R.id.txt_msa_binwise_picking_process_scanned_bin_tot_bin);

        rvBinCrate = rootView.findViewById(R.id.rv_msa_binwise_bin_crate);
        binCrateAdapter = new BinCratePickAdapter();
        rvBinCrate.setLayoutManager(new LinearLayoutManager(con));
        rvBinCrate.setAdapter(binCrateAdapter);
        rvBinCrate.setHasFixedSize(false);

        btn_back =  rootView.findViewById(R.id.btn_msa_binwise_picking_process_back);
        btn_save =  rootView.findViewById(R.id.btn_msa_binwise_picking_process_save);

        dd_picklist_list = rootView.findViewById(R.id.msa_binwise_picking_process_dd_picklist);
        dd_picklist_list.setSelection(0);
        picklistAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, picklists);
        picklistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_picklist_list.setAdapter(picklistAdapter);
        dd_picklist_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        addInputEvents();
        binParseExecutor = Executors.newSingleThreadExecutor();
        clear();
        getPicklists();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        cancelHuScanCommit();
        cancelMsaBinCommit();
        cancelMsaCrateCommit();
        if (binParseExecutor != null) {
            binParseExecutor.shutdownNow();
            binParseExecutor = null;
        }
        super.onDestroyView();
    }

    private void dismissProcessingSafely() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        dialog = null;
    }

    private void refreshBinCrateRecycler() {
        if (binCrateAdapter == null) return;
        binCrateAdapter.submitRows(new ArrayList<>(binDataMap.values()));
    }

    private void cancelHuScanCommit() {
        if (huCommitRunnable != null) {
            scanHandler.removeCallbacks(huCommitRunnable);
            huCommitRunnable = null;
        }
    }

    private void scheduleHuScanCommit() {
        cancelHuScanCommit();
        huCommitRunnable = () -> {
            huCommitRunnable = null;
            if (huValidationInFlight || txt_scan_hu == null) return;
            String value = UIFuncs.toUpperTrim(txt_scan_hu);
            if (value.length() >= SCAN_MIN_LEN_HU) {
                validateHU(value);
            }
        };
        scanHandler.postDelayed(huCommitRunnable, SCAN_DEBOUNCE_MS);
    }

    private void cancelMsaBinCommit() {
        if (msaBinCommitRunnable != null) {
            scanHandler.removeCallbacks(msaBinCommitRunnable);
            msaBinCommitRunnable = null;
        }
    }

    private void cancelMsaCrateCommit() {
        if (msaCrateCommitRunnable != null) {
            scanHandler.removeCallbacks(msaCrateCommitRunnable);
            msaCrateCommitRunnable = null;
        }
    }

    private void scheduleMsaBinCommit() {
        cancelMsaBinCommit();
        msaBinCommitRunnable = () -> {
            msaBinCommitRunnable = null;
            if (suppressMsaBinScheduling || txt_scan_msa_bin == null || !txt_scan_msa_bin.isEnabled()) {
                return;
            }
            String value = UIFuncs.toUpperTrim(txt_scan_msa_bin);
            if (value.length() >= SCAN_MIN_LEN_MSA_FIELD) {
                validateMsaBin(value);
            }
        };
        scanHandler.postDelayed(msaBinCommitRunnable, SCAN_DEBOUNCE_MS);
    }

    private void scheduleMsaCrateCommit() {
        cancelMsaCrateCommit();
        msaCrateCommitRunnable = () -> {
            msaCrateCommitRunnable = null;
            if (suppressMsaCrateScheduling || txt_scan_msa_crate == null || !txt_scan_msa_crate.isEnabled()) {
                return;
            }
            String value = UIFuncs.toUpperTrim(txt_scan_msa_crate);
            if (value.length() >= SCAN_MIN_LEN_MSA_FIELD) {
                validateMSACrate(value);
            }
        };
        scanHandler.postDelayed(msaCrateCommitRunnable, SCAN_DEBOUNCE_MS);
    }

    /**
     * Hardware scanners often send KEYCODE_ENTER; {@link EditorInfo#IME_ACTION_DONE} is not always set.
     */
    private boolean trySubmitScanHuFromEditor(TextView textView, int actionId, KeyEvent keyEvent) {
        boolean enterFromHw = keyEvent != null
                && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
        boolean imeSubmit = actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_GO;
        if (!imeSubmit && !enterFromHw) {
            return false;
        }
        cancelHuScanCommit();
        UIFuncs.hideKeyboard(getActivity());
        String value = UIFuncs.toUpperTrim(txt_scan_hu);
        if (!value.isEmpty()) {
            validateHU(value);
            return true;
        }
        return imeSubmit || enterFromHw;
    }

    private boolean trySubmitMsaBinFromEditor(TextView textView, int actionId, KeyEvent keyEvent) {
        boolean enterFromHw = keyEvent != null
                && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
        boolean imeSubmit = actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_GO;
        if (!imeSubmit && !enterFromHw) {
            return false;
        }
        cancelMsaBinCommit();
        UIFuncs.hideKeyboard(getActivity());
        String value = UIFuncs.toUpperTrim(txt_scan_msa_bin);
        if (!value.isEmpty()) {
            validateMsaBin(value);
            return true;
        }
        return imeSubmit || enterFromHw;
    }

    private boolean trySubmitMsaCrateFromEditor(TextView textView, int actionId, KeyEvent keyEvent) {
        boolean enterFromHw = keyEvent != null
                && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
        boolean imeSubmit = actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_GO;
        if (!imeSubmit && !enterFromHw) {
            return false;
        }
        cancelMsaCrateCommit();
        UIFuncs.hideKeyboard(getActivity());
        String value = UIFuncs.toUpperTrim(txt_scan_msa_crate);
        if (!value.isEmpty()) {
            validateMSACrate(value);
            return true;
        }
        return imeSubmit || enterFromHw;
    }

    private void addInputEvents(){
        dd_picklist_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    if(dd_picklist_list.getSelectedItem() != null){
                        String key = dd_picklist_list.getSelectedItem().toString();
                        if(dd_picklist_list.getSelectedItemPosition() > 0){
                            if(picklistDataMap.containsKey(key)){
                                currentPicklist = picklistDataMap.get(key);
                                txt_store.setText(currentPicklist.getStore());
                                getBinData(currentPicklist.getPicklist());
                            }
                        }else{
                            currentPicklist = null;
                            clear();
                        }
                    }
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                return trySubmitScanHuFromEditor(textView, actionId, keyEvent);
            }
        });
        txt_scan_hu.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                cancelHuScanCommit();
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_hu);
                if (!value.isEmpty()) {
                    validateHU(value);
                    return true;
                }
            }
            return false;
        });
        txt_scan_hu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressHuScanScheduling || huValidationInFlight) return;
                scheduleHuScanCommit();
            }
        });
        txt_scan_msa_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                return trySubmitMsaCrateFromEditor(textView, actionId, keyEvent);
            }
        });
        txt_scan_msa_crate.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                cancelMsaCrateCommit();
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_msa_crate);
                if (!value.isEmpty()) {
                    validateMSACrate(value);
                    return true;
                }
            }
            return false;
        });
        txt_scan_msa_crate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressMsaCrateScheduling) return;
                String t = s.toString();
                if (t.contains("\n") || t.contains("\r")) {
                    suppressMsaCrateScheduling = true;
                    String cleaned = t.replace("\r", "").replace("\n", "").trim();
                    txt_scan_msa_crate.setText(cleaned);
                    txt_scan_msa_crate.setSelection(Math.min(cleaned.length(), txt_scan_msa_crate.length()));
                    suppressMsaCrateScheduling = false;
                }
                scheduleMsaCrateCommit();
            }
        });
        txt_scan_msa_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                return trySubmitMsaBinFromEditor(textView, actionId, keyEvent);
            }
        });
        txt_scan_msa_bin.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                cancelMsaBinCommit();
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_msa_bin);
                if (!value.isEmpty()) {
                    validateMsaBin(value);
                    return true;
                }
            }
            return false;
        });
        txt_scan_msa_bin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressMsaBinScheduling) return;
                String t = s.toString();
                if (t.contains("\n") || t.contains("\r")) {
                    suppressMsaBinScheduling = true;
                    String cleaned = t.replace("\r", "").replace("\n", "").trim();
                    txt_scan_msa_bin.setText(cleaned);
                    txt_scan_msa_bin.setSelection(Math.min(cleaned.length(), txt_scan_msa_bin.length()));
                    suppressMsaBinScheduling = false;
                }
                scheduleMsaBinCommit();
            }
        });
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_msa_binwise_picking_process_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_msa_binwise_picking_process_save:
                save();
                break;
        }
    }

    private void clear(){
        cancelHuScanCommit();
        cancelMsaBinCommit();
        cancelMsaCrateCommit();
        huValidationInFlight = false;
        suppressHuScanScheduling = true;
        suppressMsaBinScheduling = true;
        suppressMsaCrateScheduling = true;
        binDataMap = new LinkedHashMap<>();
        refreshBinCrateRecycler();
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.disableInput(con, txt_scan_msa_bin);
        UIFuncs.disableInput(con, txt_scan_msa_crate);
        UIFuncs.disableInput(con, txt_sqty_tqty);
        txt_scanned_hu.setText("");
        txt_scan_hu.setText("");
        txt_scan_msa_crate.setText("");
        txt_scanned_msa_crate.setText("");
        txt_scan_msa_bin.setText("");
        txt_scanned_msa_bin.setText("");
        txt_sqty_tqty.setText("0/0");
        txt_store.setText("");
        suppressHuScanScheduling = false;
        suppressMsaBinScheduling = false;
        suppressMsaCrateScheduling = false;
    }

    /**
     * Parses {@code ET_BIN} off the UI thread (large lists caused ANRs when built into TableLayout).
     */
    private void scheduleParseBinDataResponse(JSONObject responsebody) {
        String etBinJson;
        try {
            if (!responsebody.has("ET_BIN")) {
                dismissProcessingSafely();
                showError("Not Found", "Bin Data is Empty");
                return;
            }
            etBinJson = responsebody.getJSONArray("ET_BIN").toString();
        } catch (JSONException e) {
            dismissProcessingSafely();
            new AlertBox(requireContext()).getErrBox(e);
            return;
        }
        if (binParseExecutor == null || binParseExecutor.isShutdown()) {
            dismissProcessingSafely();
            showError("Err", "Internal error — retry opening this screen.");
            return;
        }
        binParseExecutor.execute(() -> {
            LinkedHashMap<String, BinCrateHU> parsed = new LinkedHashMap<>();
            try {
                parsed = parseEtBinJsonToMap(etBinJson);
            } catch (Exception e) {
                Log.e(TAG, "ET_BIN parse failed", e);
            }
            final LinkedHashMap<String, BinCrateHU> result = parsed;
            FragmentActivity act = activity;
            if (act == null) return;
            act.runOnUiThread(() -> {
                dismissProcessingSafely();
                if (result.isEmpty()) {
                    showError("Not Found", "Bin Data is Empty");
                    return;
                }
                binDataMap = result;
                refreshBinCrateRecycler();
                txt_sqty_tqty.setText(totalScanned + " / " + (binDataMap.size() + scannedBinData.size()));
                UIFuncs.enableInput(con, txt_scan_hu);
                suppressHuScanScheduling = true;
                txt_scan_hu.setText("");
                suppressHuScanScheduling = false;
                txt_scan_hu.requestFocus();
                Log.d(TAG, "BIN data loaded rows=" + binDataMap.size());
            });
        });
    }

    private static LinkedHashMap<String, BinCrateHU> parseEtBinJsonToMap(String etBinJsonString) throws JSONException {
        LinkedHashMap<String, BinCrateHU> map = new LinkedHashMap<>();
        JSONArray ET_DATA_ARRAY = new JSONArray(etBinJsonString);
        int totalEtRecords = ET_DATA_ARRAY.length();
        Gson gson = new Gson();
        for (int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++) {
            BinCrateHU binCrateData = gson.fromJson(
                    ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), BinCrateHU.class);
            map.put(binCrateData.getBin() + "-" + binCrateData.getCrate(), binCrateData);
        }
        return map;
    }

    private void getPicklists(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZBIN_GRT_PICKLIST_VALIDATION);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            showProcessingAndSubmit(Vars.ZBIN_GRT_PICKLIST_VALIDATION, REQUEST_PICKLIST, args);
        } catch (JSONException e) {
            e.printStackTrace();
            dismissProcessingSafely();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void setPicklistListData(JSONObject responsebody) {
        try {
            currentPicklist = null;
            picklistDataMap = new LinkedHashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_PICKLIST");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if (totalEtRecords > 0) {
                picklists.clear();
                picklists.add("");
                for (int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++) {
                    PicklistData picklistData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), PicklistData.class);
                    picklists.add(UIFuncs.removeLeadingZeros(picklistData.getPicklist()));
                    picklistDataMap.put(UIFuncs.removeLeadingZeros(picklistData.getPicklist()), picklistData);
                }
            }
            if (!picklistDataMap.isEmpty()) {
                ((BaseAdapter) dd_picklist_list.getAdapter()).notifyDataSetChanged();
                dd_picklist_list.setEnabled(true);
                dd_picklist_list.invalidate();
                dd_picklist_list.setSelection(0);
                dd_picklist_list.requestFocus();
            } else {
                showError("Empty", "Picklist Data is Empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void getBinData(String picklist){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZBIN_GRT_BIN_DATA);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_PICKLIST", picklist);
            showProcessingAndSubmit(Vars.ZBIN_GRT_BIN_DATA, REQUEST_BIN_DATA, args);
        } catch (JSONException e) {
            e.printStackTrace();
            dismissProcessingSafely();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void validateHU(String hu){
        cancelHuScanCommit();
        if (huValidationInFlight) {
            Log.d(TAG, "validateHU: skipped duplicate while request in flight");
            return;
        }
        huValidationInFlight = true;
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZBIN_GRT_HU_VALIDATION);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZBIN_GRT_HU_VALIDATION, REQUEST_VALIDATE_HU, args);
        } catch (JSONException e) {
            huValidationInFlight = false;
            e.printStackTrace();
            dismissProcessingSafely();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void validateMsaBin(String bin){
        boolean binFound = false;
        for (Map.Entry<String, BinCrateHU> binDataEntry: binDataMap.entrySet()) {
            if(binDataEntry.getValue().getBin().equalsIgnoreCase(bin)){
                txt_scanned_msa_bin.setText(bin);
                suppressMsaBinScheduling = true;
                txt_scan_msa_bin.setText("");
                suppressMsaBinScheduling = false;
                UIFuncs.enableInput(con, txt_scan_msa_crate);
                // FIX 2026-04-30: focus must follow input enablement so the
                // next scanner keystroke goes to MSA Crate, not the previous
                // (now-disabled) MSA Bin field.
                txt_scan_msa_crate.requestFocus();
                binFound = true;
                break;
            }
        }

        if(!binFound){
            if(!UIFuncs.toUpperTrim(txt_scanned_msa_crate).isEmpty()){
                String key = bin + "-" + UIFuncs.toUpperTrim(txt_scanned_msa_crate);
                if(scannedBinData.containsKey(key)){
                    showError("Already Scanned", "MSA Crate and MSA Bin already scanned");
                    return;
                }
            }
            showError("Invalid Bin", "MSA Bin not found in the table");
            // Keep focus on the MSA Bin field so the user can rescan immediately
            suppressMsaBinScheduling = true;
            txt_scan_msa_bin.setText("");
            suppressMsaBinScheduling = false;
            txt_scan_msa_bin.requestFocus();
        }
    }

    public void validateMSACrate(String crate){
        String key = UIFuncs.toUpperTrim(txt_scanned_msa_bin)+"-"+crate.trim();
        if(binDataMap.containsKey(key)){
            BinCrateHU binData = binDataMap.get(key);
            binData.setHu(UIFuncs.toUpperTrim(txt_scanned_hu));
            binData.setPicklist(currentPicklist.getPicklist());
            scannedBinData.put(key, BinCrateHU.newInstance(binData));
            clearFieldsForNextScan(key);
            return;
        }else{
            if(scannedBinData.containsKey(key)){
                showError("Already Scanned", "MSA Crate and MSA Bin already scanned");
            }else{
                showError("Invalid Crate", "MSA Crate not found in the table");
            }
        }
        suppressMsaCrateScheduling = true;
        txt_scan_msa_crate.setText("");
        suppressMsaCrateScheduling = false;
        // Keep focus on the MSA Crate field for retry
        txt_scan_msa_crate.requestFocus();
    }

    private void clearFieldsForNextScan(String key){
        totalScanned++;
        binDataMap.remove(key);
        txt_scanned_msa_crate.setText(UIFuncs.toUpperTrim(txt_scan_msa_crate));
        suppressMsaCrateScheduling = true;
        txt_scan_msa_crate.setText("");
        suppressMsaCrateScheduling = false;
        txt_sqty_tqty.setText(totalScanned + " / " + (binDataMap.size() + scannedBinData.size()));
        refreshBinCrateRecycler();
        txt_scan_msa_bin.requestFocus();
    }

    public void save(){
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if(dataToSave != null){
            try {
                args.put("bapiname", Vars.ZBIN_GRT_DATA_SAVE);
                args.put("IM_USER", USER);
                args.put("IM_PLANT", WERKS);
                args.put("ET_SCAN_DATA", dataToSave);
                showProcessingAndSubmit(Vars.ZBIN_GRT_DATA_SAVE, REQUEST_SAVE, args);
            } catch (JSONException e) {
                e.printStackTrace();
                UIFuncs.errorSound(con);
                dismissProcessingSafely();
                AlertBox box = new AlertBox(getContext());
                box.getErrBox(e);
            }
        }
    }

    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, BinCrateHU> binCrateHUEntry : scannedBinData.entrySet()) {
                String scanDataJsonString = new Gson().toJson(binCrateHUEntry.getValue());
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "No data to submit please scan some bin and crates");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){
        dismissProcessingSafely();
        dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                if (request == REQUEST_VALIDATE_HU) {
                    huValidationInFlight = false;
                }
                dismissProcessingSafely();
                new AlertBox(requireContext()).getErrBox(e);
            }
        });
    }

    private void submitRequest(String rfc, int request, JSONObject args){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest;
        String url = GatewayUrls.noAclJsonRfcUrl(this.URL, rfc);
        if (url.isEmpty()) {
            dismissProcessingSafely();
            showError("Err", "Server URL is missing. Log in again from IP selection.");
            return;
        }

        final JSONObject params = args;

        Log.d(TAG, "RFC URL -> " + url);
        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                Log.d(TAG, "response ->" + responsebody);

                if (request == REQUEST_BIN_DATA) {
                    handleBinDataNetworkResponse(responsebody);
                    return;
                }

                dismissProcessingSafely();

                if (responsebody == null) {
                    if (request == REQUEST_VALIDATE_HU) huValidationInFlight = false;
                    new AlertBox(requireContext()).getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    if (request == REQUEST_VALIDATE_HU) huValidationInFlight = false;
                    new AlertBox(requireContext()).getBox("Err", "Unable to Connect Server/ Empty Response");
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.optString("TYPE", "");
                                if ("E".equals(type)) {
                                    showError("Err", returnobj.optString("MESSAGE", "Error"));
                                    if (request == REQUEST_VALIDATE_HU) {
                                        huValidationInFlight = false;
                                        suppressHuScanScheduling = true;
                                        txt_scan_hu.setText("");
                                        suppressHuScanScheduling = false;
                                        txt_scan_hu.requestFocus();
                                    }
                                } else {
                                    if (request == REQUEST_PICKLIST) {
                                        setPicklistListData(responsebody);
                                    } else if (request == REQUEST_VALIDATE_HU) {
                                        huValidationInFlight = false;
                                        suppressHuScanScheduling = true;
                                        txt_scanned_hu.setText(UIFuncs.toUpperTrim(txt_scan_hu));
                                        txt_scan_hu.setText("");
                                        suppressHuScanScheduling = false;
                                        UIFuncs.enableInput(con, txt_scan_msa_bin);
                                        txt_scan_msa_bin.requestFocus();
                                    } else if (request == REQUEST_SAVE) {
                                        suppressHuScanScheduling = true;
                                        suppressMsaBinScheduling = true;
                                        suppressMsaCrateScheduling = true;
                                        txt_scan_msa_crate.setText("");
                                        txt_scanned_msa_crate.setText("");
                                        txt_scan_msa_bin.setText("");
                                        txt_scanned_msa_bin.setText("");
                                        txt_scan_hu.setText("");
                                        txt_scanned_hu.setText("");
                                        suppressHuScanScheduling = false;
                                        suppressMsaBinScheduling = false;
                                        suppressMsaCrateScheduling = false;
                                        totalScanned = 0;
                                        scannedBinData = new HashMap<>();
                                        txt_sqty_tqty.setText(totalScanned + " / " + (binDataMap.size() + scannedBinData.size()));
                                        refreshBinCrateRecycler();
                                        UIFuncs.disableInput(con, txt_scan_msa_bin);
                                        UIFuncs.disableInput(con, txt_scan_msa_crate);
                                        UIFuncs.enableInput(con, txt_scan_hu);
                                        txt_scan_hu.requestFocus();
                                        new AlertBox(requireContext()).getBox("Success", returnobj.optString("MESSAGE", "OK"));
                                    }
                                }
                            }
                        } else if (request == REQUEST_VALIDATE_HU) {
                            huValidationInFlight = false;
                        }
                    } catch (JSONException e) {
                        if (request == REQUEST_VALIDATE_HU) huValidationInFlight = false;
                        e.printStackTrace();
                        new AlertBox(requireContext()).getErrBox(e);
                    }
                }
            }
        }, volleyErrorListener(request)) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return params.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {

                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res.toString());

                return res;
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
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mJsonRequest);
        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            if (request == REQUEST_VALIDATE_HU) {
                huValidationInFlight = false;
            }
            dismissProcessingSafely();
            new AlertBox(requireContext()).getErrBox(authFailureError);
        }
    }

    /** Loader stays visible until ET_BIN is parsed (background) and RecyclerView is updated. */
    private void handleBinDataNetworkResponse(JSONObject responsebody) {
        try {
            if (responsebody == null) {
                dismissProcessingSafely();
                showError("Err", "No response from Server");
                return;
            }
            if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                dismissProcessingSafely();
                showError("Err", "Unable to Connect Server/ Empty Response");
                return;
            }
            if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                String type = returnobj.optString("TYPE", "");
                if ("E".equals(type)) {
                    dismissProcessingSafely();
                    showError("Err", returnobj.optString("MESSAGE", "Error"));
                    return;
                }
                scheduleParseBinDataResponse(responsebody);
                return;
            }
            dismissProcessingSafely();
            showError("Err", "Unexpected server response");
        } catch (JSONException e) {
            dismissProcessingSafely();
            new AlertBox(requireContext()).getErrBox(e);
        }
    }

    Response.ErrorListener volleyErrorListener(final int request) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

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
                } else err = error.toString();

                dismissProcessingSafely();
                if (request == REQUEST_VALIDATE_HU) {
                    huValidationInFlight = false;
                }
                new AlertBox(requireContext()).getBox("Err", err);
            }
        };
    }
}
