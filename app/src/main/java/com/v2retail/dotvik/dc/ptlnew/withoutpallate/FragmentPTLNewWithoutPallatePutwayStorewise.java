package com.v2retail.dotvik.dc.ptlnew.withoutpallate;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import android.widget.Toast;

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
import com.lidroid.xutils.db.sqlite.SqlInfo;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewArticlePutwayStorewise;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FragmentPTLNewWithoutPallatePutwayStorewise extends Fragment implements View.OnClickListener {

    private static final int REQUEST_ZONE_LIST = 1500;
    private static final int REQUEST_VALIDATE_ZONE_CRATE = 1501;
    private static final int REQUEST_VALIDATE_ZONE_HU = 1502;

    private static final String TAG = FragmentPTLNewWithoutPallatePutwayStorewise.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    List<String> zones = new ArrayList<String>();
    ArrayAdapter<String> zoneAdapter;

    boolean spinnerTouched = false;

    Spinner dd_zone_list;

    Button btn_back, btn_plus, btn_minus, btn_reset;
    EditText txt_scan_crate, txt_crate, txt_scan_article, txt_article, txt_proposed_store, txt_scan_hu, txt_hu, txt_scan_qty;

    Map<String, PicklistData> etDataMap = new LinkedHashMap<>();
    Map<String, HUEANData> eanDataMap = new HashMap<>();
    PicklistData currentScan = null;

    boolean isWoPallete = true;
    
    public FragmentPTLNewWithoutPallatePutwayStorewise() {
        // Required empty public constructor
    }
    
    public static FragmentPTLNewWithoutPallatePutwayStorewise newInstance(boolean isWoPallete) {
        FragmentPTLNewWithoutPallatePutwayStorewise newInstance = new FragmentPTLNewWithoutPallatePutwayStorewise();
        newInstance.isWoPallete = isWoPallete;
        return newInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL-Article Putway Storewise");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ptl_new_without_pallate_putway_storewise, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        FragmentActivity activity = getActivity();

        dd_zone_list = rootView.findViewById(R.id.ptl_new_article_putway_storewise_dd_zone);
        dd_zone_list.setSelection(0);

        txt_scan_crate = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_scan_crate);
        txt_crate = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_crate);
        txt_scan_article = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_scan_article);
        txt_article = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_article);
        txt_proposed_store = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_proposed_store);
        txt_scan_qty = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_scan_qty);
        txt_scan_hu = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_scan_hu);
        txt_hu = rootView.findViewById(R.id.txt_ptl_new_wo_pallate_article_putway_storewise_hu);

        btn_plus = rootView.findViewById(R.id.btn_ptl_new_wo_pallate_article_putway_storewise_scan_qty_plus);
        btn_minus = rootView.findViewById(R.id.btn_ptl_new_wo_pallate_article_putway_storewise_scan_qty_minus);
        btn_back = rootView.findViewById(R.id.btn_ptl_new_wo_pallate_article_putway_storewise_back);
        btn_reset = rootView.findViewById(R.id.btn_ptl_new_wo_pallate_article_putway_storewise_reset);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_plus.setOnClickListener(this);
        btn_minus.setOnClickListener(this);

        zoneAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, zones);
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_zone_list.setAdapter(zoneAdapter);
        dd_zone_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        clear(true);
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_wo_pallate_article_putway_storewise_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_ptl_new_wo_pallate_article_putway_storewise_reset:
                box.getBox("Confirm Reset", "Are you sure you want to reset?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear(true);
                    }
                });
                break;
            case R.id.btn_ptl_new_wo_pallate_article_putway_storewise_scan_qty_minus:
                if(!UIFuncs.toUpperTrim(txt_scan_qty).isEmpty()){
                    scanQuantity(Integer.parseInt(UIFuncs.toUpperTrim(txt_scan_qty)), 2);
                }
                break;
            case R.id.btn_ptl_new_wo_pallate_article_putway_storewise_scan_qty_plus:
                if(!UIFuncs.toUpperTrim(txt_scan_qty).isEmpty()){
                    scanQuantity(Integer.parseInt(UIFuncs.toUpperTrim(txt_scan_qty)), 1);
                }
                break;
        }
    }
    
    private void clear(boolean clearAll){
        txt_scan_qty.setText("");
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.disableInput(con, txt_hu);
        UIFuncs.disableInput(con, txt_scan_qty);
        UIFuncs.disableInput(con, txt_scan_article);
        if(clearAll){
            this.currentScan = null;
            UIFuncs.disableInput(con, txt_scan_crate);
            txt_crate.setText("");
            txt_scan_crate.setText("");
            txt_article.setText("");
            txt_proposed_store.setText("");
            txt_scan_article.setText("");
            txt_scan_hu.setText("");
            txt_hu.setText("");
            btn_plus.setEnabled(false);
            btn_minus.setEnabled(false);
            etDataMap = new HashMap<>();
            eanDataMap = new HashMap<>();
            dd_zone_list.requestFocus();
            getZoneList();
        }else{
            this.currentScan.setSqty(Integer.parseInt(Util.convertToDoubleString(this.currentScan.getQuantity())));
            this.currentScan = null;
            moveToNextArticle();
        }
    }

    private void moveToNextArticle(){
        for (Map.Entry<String, PicklistData> etData: etDataMap.entrySet()) {
            int sQty = etData.getValue().getSqty();
            if(sQty == 0){
                this.currentScan = etData.getValue();
                break;
            }
        }
        if(this.currentScan == null){
            clear(true);
        }else{
            setArticleFields();
        }
    }

    private void addInputEvents(){
        dd_zone_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinnerTouched) {
                    if(dd_zone_list.getSelectedItem() != null && !dd_zone_list.getSelectedItem().toString().isEmpty()){
                        UIFuncs.enableInput(con, txt_scan_crate);
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        txt_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_crate);
                    if (!value.isEmpty()) {
                        if(dd_zone_list.getSelectedItem() !=null && !dd_zone_list.getSelectedItem().toString().isEmpty()){
                            validateZoneCrate(value, dd_zone_list.getSelectedItem().toString());
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_crate.addTextChangedListener(new TextWatcher() {
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
                    if(dd_zone_list.getSelectedItem() !=null && !dd_zone_list.getSelectedItem().toString().isEmpty()){
                        validateZoneCrate(value , dd_zone_list.getSelectedItem().toString());
                    }
                }
            }
        });

        txt_scan_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_article);
                    if (!value.isEmpty()) {
                        validateArticleScan(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_article.addTextChangedListener(new TextWatcher() {
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
                    validateArticleScan(value);
                }
            }
        });

        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty()) {
                        validateZoneHU(value);
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
                    validateZoneHU(value);
                }
            }
        });

    }

    private void getZoneList(){
        JSONObject args = new JSONObject();
        try {
            String rfc = isWoPallete ? Vars.ZWM_PTL_GET_ZONE : Vars.ZWM_PTL_GET_ZONE_STATION_V3;
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            args.put("IM_ZONE", null);
            args.put("IM_WERKS", WERKS);
            showProcessingAndSubmit(rfc, REQUEST_ZONE_LIST, args);
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

    /**
     * v12.115 (2026-05-07): Three fixes for Zone dropdown — same pattern as the
     * sister fragment FragmentPTLNewArticlePutwayStorewise (withpallate folder).
     *  1. SAFEGUARD: JSON RFC adapter returns clean 0-indexed array with NO header row.
     *     Loop MUST start at i=0, length is the full array length.
     *     (Was: i=1 to length-1 — silently dropped the first zone.)
     *  2. DEDUPE: SAP's ET_ZONE returns the same zone-station for multiple stores —
     *     resulting in 8+ duplicate "F0-ST1" entries in the dropdown.
     *     Use a HashSet to keep only unique zones in display order.
     *  3. FIELD FALLBACK: ZWM_PTL_GET_ZONE returns ZONE; ZWM_PTL_GET_ZONE_STATION_V3
     *     returns ZONE_STATION. Try ZONE_STATION first, fall back to ZONE.
     */
    public void setZoneList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_ZONE");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                zones.clear();
                zones.add("Select");
                Set<String> seen = new HashSet<>();
                int duplicatesSkipped = 0;
                int blanksSkipped = 0;
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD = ET_DATA_ARRAY.getJSONObject(recordIndex);
                    String zone = ET_RECORD.optString("ZONE_STATION", "").trim();
                    if(zone.isEmpty()){
                        zone = ET_RECORD.optString("ZONE", "").trim();
                    }
                    if(zone.isEmpty()){
                        blanksSkipped++;
                        continue;
                    }
                    if(!seen.add(zone)){
                        duplicatesSkipped++;
                        continue;
                    }
                    zones.add(zone);
                }
                Log.d(TAG, "setZoneList: ET_ZONE rows=" + totalEtRecords
                        + " | unique zones loaded=" + (zones.size() - 1)
                        + " | duplicates skipped=" + duplicatesSkipped
                        + " | blanks skipped=" + blanksSkipped);

                if(zones.size() <= 1){
                    box.getBox("No Zones", "No zones returned by SAP for user " + USER);
                    return;
                }

                ((BaseAdapter) dd_zone_list.getAdapter()).notifyDataSetChanged();
                dd_zone_list.setEnabled(true);
                dd_zone_list.invalidate();
                dd_zone_list.setSelection(0);
                dd_zone_list.requestFocus();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void validateZoneCrate(String crate, String zone){
        JSONObject args = new JSONObject();
        try {
            String rfc = isWoPallete ? Vars.ZWM_PTL_ZONE_CRATE_VALIDATE : Vars.ZWM_PTL_ZONE_CRATE_VALIDATE_V3;
            this.currentScan = null;
            args.put("bapiname", rfc);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate);
            if(isWoPallete){
                args.put("IM_ZONE", zone);
            }else{
                args.put("IM_ZONE_STATION", zone);
            }

            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_ZONE_CRATE, args);
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

    private void setEtEanData(JSONObject responsebody){
        try
        {
            etDataMap = new HashMap<>();
            eanDataMap = new HashMap<>();
            txt_crate.setText(UIFuncs.toUpperTrim(txt_scan_crate));
            txt_scan_crate.setText("");
            txt_proposed_store.setText("");
            txt_scan_article.setText("");
            txt_article.setText("");
            txt_hu.setText("");
            txt_scan_hu.setText("");

            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            JSONArray ET_EAN_DATA_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            int totalEanRecords = ET_EAN_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    PicklistData etData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), PicklistData.class);
                    etDataMap.put(etData.getArticle() + "-" + etData.getStore(), etData);
                }
            }
            if(totalEanRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEanRecords; recordIndex++){
                    HUEANData eanData = new Gson().fromJson(ET_EAN_DATA_ARRAY.getJSONObject(recordIndex).toString(), HUEANData.class);
                    eanDataMap.put(eanData.getLgean11(), eanData);
                }
            }
            if(totalEtRecords > 0 && totalEanRecords > 0){
                UIFuncs.enableInput(con, txt_scan_article);
                return;
            }else{
                box.getBox("No Records Found", "No data available for scan");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
        txt_crate.setText("");
        txt_scan_crate.requestFocus();
    }

    private void validateArticleScan(String barcode){
        this.currentScan = null;
        if(eanDataMap.containsKey(barcode)){
            String matnr = eanDataMap.get(barcode).getLgmatnr();
            for (Map.Entry<String, PicklistData> etDataEntry: etDataMap.entrySet()) {
                if(etDataEntry.getValue().getArticle().equalsIgnoreCase(matnr)){
                    if(etDataEntry.getValue().getSqty() == 0){
                        this.currentScan = etDataEntry.getValue();
                        break;
                    }
                }
            }
            setArticleFields();
            return;
        }else{
            box.getBox("Invalid", "Scanned Barcode is invalid and not available in EAN Records");
        }
        txt_scan_article.setText("");
        txt_scan_article.requestFocus();
    }

    private void setArticleFields(){
        if(this.currentScan != null){
            txt_scan_qty.setText(Util.convertToDoubleString(this.currentScan.getQuantity()));
            txt_article.setText(UIFuncs.removeLeadingZeros(this.currentScan.getArticle()));
            txt_proposed_store.setText(this.currentScan.getStore());
            this.currentScan.setScanQty(Util.convertToDoubleString(this.currentScan.getQuantity()));
            txt_scan_hu.setText("");
            btn_minus.setEnabled(true);
            btn_plus.setEnabled(true);
            UIFuncs.disableInput(con, txt_scan_article);
            UIFuncs.enableInput(con, txt_scan_hu);
        }else{
            box.getBox("Invalid", "Article is invalid and not found in ET Records");
        }
    }

    private void scanQuantity(int qty, int mode){
        double pendingQuantity = Util.convertStringToDouble(this.currentScan.getQuantity()) - this.currentScan.getSqty();
        if(mode == 1){
            if(qty + 1 > pendingQuantity){
                return;
            }
            qty += 1;
        }
        else if(mode == 2){
            if(qty - 1 < 1){
                return;
            }
            qty -= 1;
        }
        this.currentScan.setScanQty(String.valueOf(qty));
        txt_scan_qty.setText(String.valueOf(qty));
    }

    /** Null/blank → empty string. JSON RFC adapter prefers "" over missing keys. */
    private static String safe(String s){
        return s == null ? "" : s.trim();
    }

    /**
     * v12.115 (2026-05-08): Build IS_DATA payload MANUALLY field-by-field for all
     * 24 fields of ZWM_PTL_MSA_CRATE_ST. Previous Gson-based serialization
     * caused SAP to return "HU Cannot Be Blank" because some fields were dropped
     * or mapped under wrong keys. Manual JSONObject.put() guarantees:
     *  - HU is always set explicitly from the scanned `hu` parameter
     *  - ZONE_STATION is sent (V3 RFC reads it; PicklistData field added in this build)
     *  - HUB defaults to WERKS (PicklistData field added in this build)
     *  - All 24 fields are present, no nulls, no Gson surprises
     */
    private JSONObject getScanDataToSubmit(String hu){
        try {
            if (currentScan == null) {
                box.getBox("Empty Request", "Nothing to submit, please scan some articles");
                txt_scan_article.requestFocus();
                return null;
            }
            String huNorm  = hu == null ? "" : hu.trim().toUpperCase();
            String zoneSel = dd_zone_list.getSelectedItem() != null
                    ? dd_zone_list.getSelectedItem().toString().trim() : "";

            if (huNorm.isEmpty()) {
                Log.w(TAG, "getScanDataToSubmit called with empty HU — aborting");
                box.getBox("HU Missing", "HU is empty. Re-scan the HU.");
                return null;
            }

            // Persist on currentScan so any downstream code sees consistent state.
            this.currentScan.setHu(huNorm);
            this.currentScan.setZone(zoneSel);
            this.currentScan.setZoneStation(zoneSel);
            if (this.currentScan.getPlant() == null || this.currentScan.getPlant().trim().isEmpty()) {
                this.currentScan.setPlant(WERKS);
            }
            if (this.currentScan.getHub() == null || this.currentScan.getHub().trim().isEmpty()) {
                this.currentScan.setHub(WERKS);
            }

            // Build IS_DATA with explicit field-by-field mapping for ZWM_PTL_MSA_CRATE_ST.
            JSONObject isData = new JSONObject();
            isData.put("PICKLIST",     safe(currentScan.getPicklist()));
            isData.put("BIN",          safe(currentScan.getBin()));
            isData.put("MSA_CRATE",    safe(currentScan.getMsaCrate()));
            isData.put("ARTICLE",      safe(currentScan.getArticle()));
            isData.put("QUANTITY",     safe(currentScan.getQuantity()));
            isData.put("ETYPE",        safe(currentScan.getEType()));
            isData.put("WAVE",         safe(currentScan.getWave()));
            isData.put("TANUM",        safe(currentScan.getTanum()));
            isData.put("PALATE",       safe(currentScan.getPalate()));
            isData.put("CRATE",        safe(currentScan.getCrate()));
            isData.put("STORE",        safe(currentScan.getStore()));
            isData.put("ITEMNO",       safe(currentScan.getItemNo()));
            isData.put("EBELN",        safe(currentScan.getEbeln()));
            isData.put("EBELP",        safe(currentScan.getEbelp()));
            isData.put("TAG",          safe(currentScan.getTag()));
            isData.put("HU",           huNorm);                 // explicit — required
            isData.put("ZONE",         zoneSel);
            isData.put("PLANT",        safe(currentScan.getPlant()));
            isData.put("SCAN_QTY",     safe(currentScan.getScanQty()));
            isData.put("ZONE_STATION", zoneSel);                // V3 reads this
            isData.put("MATKL",        safe(currentScan.getMatkl()));
            isData.put("DIVISION",     safe(currentScan.getDivision()));
            isData.put("FLOOR",        safe(currentScan.getFloor()));
            isData.put("HUB",          safe(currentScan.getHub()));

            Log.d(TAG, "IS_DATA payload: HU=" + huNorm + " ZONE=" + zoneSel
                    + " ZONE_STATION=" + zoneSel
                    + " STORE=" + safe(currentScan.getStore())
                    + " PICKLIST=" + safe(currentScan.getPicklist())
                    + " ARTICLE=" + safe(currentScan.getArticle())
                    + " BIN=" + safe(currentScan.getBin())
                    + " CRATE=" + safe(currentScan.getCrate())
                    + " | full=" + isData.toString());
            return isData;
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    private void validateZoneHU(String hu){
        JSONObject args = new JSONObject();
        try {
            String rfc = isWoPallete ? Vars.ZWM_PTL_ZONE_HU_VALIDATE : Vars.ZWM_PTL_ZONE_HU_VALIDATE_V3;
            args.put("bapiname", rfc);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            JSONObject itData = getScanDataToSubmit(hu);
            if(itData != null){
                args.put("IS_DATA", itData);
                Log.d(TAG, "validateZoneHU rfc=" + rfc + " sending IS_DATA.HU=" + itData.optString("HU", "(missing)"));
                showProcessingAndSubmit(rfc, REQUEST_VALIDATE_ZONE_HU, args);
            }
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

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

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

    private void submitRequest(String rfc, int request, JSONObject args) {

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
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {
                    UIFuncs.errorSound(con);
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
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
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_ZONE_CRATE) {
                                            txt_scan_crate.setText("");
                                            txt_scan_crate.requestFocus();
                                        }
                                        else if(request == REQUEST_VALIDATE_ZONE_HU){
                                            txt_scan_hu.setText("");
                                            txt_hu.setText("");
                                            txt_scan_hu.requestFocus();
                                        }
                                    } else {
                                        if(request == REQUEST_ZONE_LIST){
                                            setZoneList(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_ZONE_CRATE) {
                                            setEtEanData(responsebody);
                                            return;
                                        }
                                        else if (request == REQUEST_VALIDATE_ZONE_HU) {
                                            txt_hu.setText(UIFuncs.toUpperTrim(txt_scan_hu));
                                            clear(false);
                                            return;
                                        }
                                    }
                                }
                                return;
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
        Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
        Log.d(TAG, "jsonRequest getBodyContentType->" + mJsonRequest.getBodyContentType());
        Log.d(TAG, "jsonRequest getBody->" + mJsonRequest.getBody().toString());
        Log.d(TAG, "jsonRequest getMethod->" + mJsonRequest.getMethod());
        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            if (dialog != null) {
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

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }
}
