package com.v2retail.dotvik.dc.binwisepicking;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.text.Editable;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.BinCrateData;
import com.v2retail.dotvik.dc.ptlnew.BinCrateHU;
import com.v2retail.dotvik.dc.ptlnew.ETPickData;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.dc.ptlnew.fullcrate30.FragmentPTLNewFullCratePicking;
import com.v2retail.dotvik.hub.models.GRCHU;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FragmentMSABinwisePicking extends Fragment implements View.OnClickListener {

    private static final int REQUEST_PICKLIST = 1501;
    private static final int REQUEST_BIN_DATA = 1502;
    private static final int REQUEST_VALIDATE_HU = 1503;
    private static final int REQUEST_SAVE = 1504;

    private static final String TAG = FragmentMSABinwisePicking.class.getName();

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
    TableLayout table_bin_crate;

    Map<String, PicklistData> picklistDataMap = new LinkedHashMap<>();
    List<String> picklists = new ArrayList<String>();
    ArrayAdapter<String> picklistAdapter;
    boolean spinnerTouched = false;
    Spinner dd_picklist_list;

    Map<String, BinCrateHU> binDataMap = new LinkedHashMap<>();
    Map<String, BinCrateHU> scannedBinData = new LinkedHashMap<>();

    int totalScanned = 0;

    PicklistData currentPicklist = null;

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
        dialog=new ProgressDialog(con);
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

        table_bin_crate = rootView.findViewById(R.id.table_msa_binwise_picking_process_msa_bin_crate);

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
        clear();
        getPicklists();

        return rootView;
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
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty()) {
                        validateHU(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_hu.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateHU(value);
                }
            }
        });
        txt_scan_msa_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_msa_crate);
                    if (!value.isEmpty()) {
                        validateMSACrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_msa_crate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateMSACrate(value);
                }
            }
        });
        txt_scan_msa_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_msa_bin);
                    if (!value.isEmpty()) {
                        validateMsaBin(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_msa_bin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateMsaBin(value);
                }
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
        binDataMap = new LinkedHashMap<>();
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void setBinData(JSONObject responsebody){
        try
        {
            binDataMap = new LinkedHashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_BIN");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    BinCrateHU binCrateData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), BinCrateHU.class);
                    binDataMap.put(binCrateData.getBin()+"-"+binCrateData.getCrate(), binCrateData);
                }
                populateBinCrateTable();
                UIFuncs.enableInput(con, txt_scan_hu);
                // FIX 2026-04-30: scanner keystrokes were going to the picklist
                // spinner because no view explicitly took focus after the bin
                // data loaded. Move focus to Scan Ext HU so the next scan lands
                // in the right field.
                txt_scan_hu.requestFocus();
                Log.d(TAG, "setBinData: txt_scan_hu enabled and focused for next scan");
                return;
            }else{
                showError("Not Found", "Bin Data is Empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void populateBinCrateTable() {
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 16;
        textSize = 15;

        table_bin_crate.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerBin = new TextView(getContext());
        TextView headerCrate = new TextView(getContext());
        TextView headerQty = new TextView(getContext());
        headerSno.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        headerSno.setGravity(Gravity.CENTER);
        headerSno.setPadding(5,5,0,5);
        headerSno.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSno.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSno.setText(" # ");

        headerBin.setGravity(Gravity.CENTER);
        headerBin.setPadding(0,5,0,5);
        headerBin.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerBin.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerBin.setText("Bin");

        headerCrate.setGravity(Gravity.CENTER);
        headerCrate.setPadding(0,5,0,5);
        headerCrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerCrate.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerCrate.setText("Crate");

        headerQty.setGravity(Gravity.CENTER);
        headerQty.setPadding(0,5,0,5);
        headerQty.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerQty.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerQty.setText("Qty");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerSno);
        tr.addView(headerBin);
        tr.addView(headerCrate);
        tr.addView(headerQty);
        table_bin_crate.addView(tr, trParams);

        int rowNum = 1;
        for (Map.Entry<String, BinCrateHU> binCrateHUEntry :binDataMap.entrySet()) {
            BinCrateHU data = binCrateHUEntry.getValue();
            TextView tvSno = new TextView(getContext());
            tvSno.setText(rowNum+"");
            tvSno.setTextSize(textSize);
            tvSno.setPadding(5,2,0,2);
            tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvBin = new TextView(getContext());
            tvBin.setText(data.getBin());
            tvBin.setTextSize(textSize);
            tvBin.setPadding(5,2,0,2);
            tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvCrate = new TextView(getContext());
            tvCrate.setText(data.getCrate());
            tvCrate.setTextSize(textSize);
            tvCrate.setPadding(5,2,0,2);
            tvCrate.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvQty = new TextView(getContext());
            tvQty.setText(Util.convertToDoubleString(data.getQty()));
            tvQty.setTextSize(textSize);
            tvQty.setPadding(5,2,0,2);
            tvQty.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            tr = new TableRow(getContext());
            tr.setId(rowNum);
            tr.setPadding(0,0,0,0);
            tr.setLayoutParams(trParams);
            tr.addView(tvSno);
            tr.addView(tvBin);
            tr.addView(tvCrate);
            tr.addView(tvQty);
            tr.setTag(data);
            table_bin_crate.addView(tr, trParams);
            rowNum++;
        }
    }

    private void validateHU(String hu){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZBIN_GRT_HU_VALIDATION);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZBIN_GRT_HU_VALIDATION, REQUEST_VALIDATE_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void validateMsaBin(String bin){
        boolean binFound = false;
        for (Map.Entry<String, BinCrateHU> binDataEntry: binDataMap.entrySet()) {
            if(binDataEntry.getValue().getBin().equalsIgnoreCase(bin)){
                txt_scanned_msa_bin.setText(bin);
                txt_scan_msa_bin.setText("");
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
            txt_scan_msa_bin.setText("");
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
        txt_scan_msa_crate.setText("");
        // Keep focus on the MSA Crate field for retry
        txt_scan_msa_crate.requestFocus();
    }

    private void clearFieldsForNextScan(String key){
        totalScanned++;
        binDataMap.remove(key);
        txt_scanned_msa_crate.setText(UIFuncs.toUpperTrim(txt_scan_msa_crate));
        txt_scan_msa_crate.setText("");
        txt_sqty_tqty.setText(totalScanned + " / " + (binDataMap.size() + scannedBinData.size()));
        populateBinCrateTable();
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
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
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

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }
    private void submitRequest(String rfc, int request, JSONObject args){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null) {
                                    if (type.equals("E")) {
                                        showError("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_HU) {
                                            txt_scan_hu.setText("");
                                            txt_scan_hu.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_PICKLIST) {
                                            setPicklistListData(responsebody);
                                        }else if (request == REQUEST_BIN_DATA) {
                                            setBinData(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_HU) {
                                            txt_scanned_hu.setText(UIFuncs.toUpperTrim(txt_scan_hu));
                                            txt_scan_hu.setText("");
                                            UIFuncs.enableInput(con, txt_scan_msa_bin);
                                            // FIX 2026-04-30: move focus to
                                            // MSA Bin so the next scan goes
                                            // there, not back to Scan Ext HU.
                                            txt_scan_msa_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_SAVE) {
                                            txt_scan_msa_crate.setText("");
                                            txt_scanned_msa_crate.setText("");
                                            txt_scan_msa_bin.setText("");
                                            txt_scanned_msa_bin.setText("");
                                            txt_scan_hu.setText("");
                                            txt_scanned_hu.setText("");
                                            totalScanned = 0;
                                            scannedBinData = new HashMap<>();
                                            txt_sqty_tqty.setText(totalScanned + " / " + (binDataMap.size() + scannedBinData.size()));
                                            UIFuncs.disableInput(con, txt_scan_msa_bin);
                                            UIFuncs.disableInput(con, txt_scan_msa_crate);
                                            UIFuncs.enableInput(con, txt_scan_hu);
                                            // FIX 2026-04-30: focus the next
                                            // active scan field after save so
                                            // the user can immediately rescan.
                                            txt_scan_hu.requestFocus();
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        AlertBox box = new AlertBox(getContext());
                        box.getErrBox(e);
                    }
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);
        }
    }
    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.i(TAG, "Error :" + error.toString());
                String err = "";

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

                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }
}
