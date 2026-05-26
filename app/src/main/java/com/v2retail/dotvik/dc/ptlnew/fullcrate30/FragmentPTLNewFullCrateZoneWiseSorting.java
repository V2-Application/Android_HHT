package com.v2retail.dotvik.dc.ptlnew.fullcrate30;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.v2retail.commons.SapJsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.BinCrateData;
import com.v2retail.dotvik.dc.ptlnew.HubZoneCrate;
import com.v2retail.dotvik.dc.ptlnew.ScanData;
import com.v2retail.dotvik.dc.ptlnew.withpallate.ZoneData;
import com.v2retail.dotvik.modal.picking.HUSave;
import com.v2retail.dotvik.modal.putaway.ETDataStorePutway;
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

public class FragmentPTLNewFullCrateZoneWiseSorting extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_CRATE = 1500;
    private static final int REQUEST_VALIDATE_PALLATE = 1501;
    private static final int REQUEST_GET_HUB_STATIONS = 1502;
    private static final int REQUEST_VALIDATE_ZONE_CRATE = 1503;


    private static final String TAG = FragmentPTLNewFullCrateHubStnCrateZoneMapping.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    LinearLayout ll_screen_1, ll_screen_2, ll_screen_scan_pallete, ll_screen_3, ll_screen_scan_article;
    Button btn_back, btn_plus, btn_minus, btn_empty_crate;
    EditText txt_scan_crate, txt_hub_station, txt_received_crate, txt_proposed_zone, txt_scan_pallate, txt_pallate;
    EditText txt_scan_article, txt_article, txt_rec_qty, txt_scan_qty, txt_scan_zone_crate, txt_zone_crate;
    CheckBox chk_sc;
    Spinner dd_station_list;

    String zoneNature;
    String crateNature;
    String crateZone;

    List<String> stations = new ArrayList<String>();
    ArrayAdapter<String> stationsAdapter;

    Map<String, ETData> etDataMap = new HashMap<>();
    Map<String, HUSave> eanDataMap = new HashMap<>();
    ZoneData scanData = null;

    ETData currentItem = null;

    boolean spinnerTouched = false;

    public FragmentPTLNewFullCrateZoneWiseSorting() {
        // Required empty public constructor
    }

    public static FragmentPTLNewFullCrateZoneWiseSorting newInstance() {
        return new FragmentPTLNewFullCrateZoneWiseSorting();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("Crate Putway(Sorting) Zone-Wise");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_full_crate_zone_wise_sorting, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        ll_screen_1  = rootView.findViewById(R.id.ll_ptl_new_full_crate_sorting_zonewise_screen_1);

        //Single Zone Controls
        ll_screen_2  = rootView.findViewById(R.id.ll_ptl_new_full_crate_sorting_zonewise_screen_2);
        ll_screen_scan_pallete  = rootView.findViewById(R.id.ll_ptl_new_full_crate_sorting_zonewise_screen_scan_pallete);
        txt_scan_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_scan_crate);
        txt_hub_station = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_hub_station);

        txt_received_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_received_crate);
        txt_proposed_zone = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_proposed_zone);
        txt_scan_pallate = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_scan_pallate);
        txt_pallate = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_pallate);
        dd_station_list = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_dd_stations);
        dd_station_list.setSelection(0);

        //Mix Zone Controls
        ll_screen_scan_article  = rootView.findViewById(R.id.ll_ptl_new_full_crate_sorting_zonewise_screen_scan_article);
        ll_screen_3 = rootView.findViewById(R.id.ll_ptl_new_full_crate_sorting_zonewise_screen_3);
        txt_scan_article = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_scan_article);
        txt_article = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_article);
        txt_rec_qty = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_req_qty);
        btn_minus = rootView.findViewById(R.id.btn_ptl_new_full_crate_sorting_zonewise_scan_qty_minus);
        txt_scan_qty = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_scan_qty);
        btn_plus = rootView.findViewById(R.id.btn_ptl_new_full_crate_sorting_zonewise_scan_qty_plus);
        chk_sc = rootView.findViewById(R.id.chk_ptl_new_full_crate_sorting_zonewise_scan_qty_sc);
        txt_scan_zone_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_scan_zone_crate);
        txt_zone_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_sorting_zonewise_zone_crate);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_full_crate_sorting_zonewise_back);
        btn_empty_crate = rootView.findViewById(R.id.btn_ptl_new_full_crate_sorting_zonewise_empty_crate);

        btn_back.setOnClickListener(this);
        btn_empty_crate.setOnClickListener(this);
        btn_minus.setOnClickListener(this);
        btn_plus.setOnClickListener(this);

        stationsAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, stations);
        stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_station_list.setAdapter(stationsAdapter);
        dd_station_list.setOnTouchListener((v, me) -> {spinnerTouched = true; v.performClick(); return false;});

        clear();
        addInputEvents();

        return rootView;
    }

    private void addInputEvents(){
        txt_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_crate);
                    if (!value.isEmpty()) {
                        validateCrate(value);
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
                    validateCrate(value);
                }
            }
        });
        txt_scan_pallate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_pallate);
                    if (!value.isEmpty()) {
                        validatePalate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_pallate.addTextChangedListener(new TextWatcher() {
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
                    validatePalate(value);
                }
            }
        });
        chk_sc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    scanData.setShortClose("X");
                } else {
                    scanData.setShortClose("");
                }
            }
        });
        dd_station_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    if(dd_station_list.getSelectedItem() != null){
                        String key = dd_station_list.getSelectedItem().toString();
                        if(!key.isEmpty()){
                            UIFuncs.enableInput(con, txt_scan_crate);
                        }
                    }
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        txt_scan_zone_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_zone_crate);
                    if (!value.isEmpty()) {
                        validateZoneCrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_zone_crate.addTextChangedListener(new TextWatcher() {
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
                    validateZoneCrate(value);
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
                        validateScannedArticle(value);
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
                    validateScannedArticle(value);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_full_crate_sorting_zonewise_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_ptl_new_full_crate_sorting_zonewise_scan_qty_plus:
                plusMinusQty("plus");
                break;
            case R.id.btn_ptl_new_full_crate_sorting_zonewise_scan_qty_minus:
                plusMinusQty("minus");
                break;
        }
    }

    private void clear(){
        step3();
        step2();
        step1();
        getStationsList();
    }

    private void step1(){
        ll_screen_1.setVisibility(View.VISIBLE);
        ll_screen_2.setVisibility(View.GONE);
        ll_screen_scan_article.setVisibility(View.GONE);
        ll_screen_scan_pallete.setVisibility(View.GONE);
        btn_empty_crate.setVisibility(View.INVISIBLE);
        txt_scan_crate.setText("");
        etDataMap.clear();
        UIFuncs.disableInput(con, txt_scan_crate);
    }

    private void step2(){
        ll_screen_2.setVisibility(View.VISIBLE);
        ll_screen_scan_pallete.setVisibility(View.VISIBLE);
        ll_screen_3.setVisibility(View.GONE);
        ll_screen_1.setVisibility(View.GONE);
        txt_received_crate.setText("");
        txt_proposed_zone.setText("");
        txt_scan_pallate.setText("");
        txt_pallate.setText("");
        UIFuncs.enableInput(con, txt_scan_pallate);
    }

    private void step3(){
        ll_screen_2.setVisibility(View.VISIBLE);
        ll_screen_3.setVisibility(View.VISIBLE);
        ll_screen_scan_pallete.setVisibility(View.GONE);
        ll_screen_1.setVisibility(View.GONE);
        txt_received_crate.setText("");
        txt_proposed_zone.setText("");
        txt_scan_pallate.setText("");
        txt_pallate.setText("");
        txt_scan_article.setText("");
        txt_article.setText("");
        txt_rec_qty.setText("");
        txt_scan_qty.setText("");
        chk_sc.setChecked(false);
        txt_scan_zone_crate.setText("");
        txt_zone_crate.setText("");
        UIFuncs.disableInput(con, txt_scan_qty);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.disableInput(con, txt_scan_zone_crate);
        btn_minus.setVisibility(View.VISIBLE);
        btn_plus.setVisibility(View.VISIBLE);
        chk_sc.setVisibility(View.VISIBLE);
        btn_plus.setEnabled(false);
        btn_minus.setEnabled(false);
        if("S".equalsIgnoreCase(crateNature)){
           ll_screen_scan_article.setVisibility(View.GONE);
            UIFuncs.enableInput(con, txt_scan_zone_crate);
        }else{
            ll_screen_scan_article.setVisibility(View.VISIBLE);
            btn_minus.setVisibility(View.GONE);
            btn_plus.setVisibility(View.GONE);
            chk_sc.setVisibility(View.GONE);
            UIFuncs.enableInput(con, txt_scan_article);
        }
        currentItem = null;
        scanData = null;
    }

    private void getStationsList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZPTL_STN_LIST_RCV_AT_HUBSTN_V3);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            showProcessingAndSubmit(Vars.ZPTL_STN_LIST_RCV_AT_HUBSTN_V3, REQUEST_GET_HUB_STATIONS, args);
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
    public void setStationsList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                stations.clear();
                stations.add("Select");
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    stations.add(ET_RECORD.getString("HUB_STN"));
                }
                ((BaseAdapter) dd_station_list.getAdapter()).notifyDataSetChanged();
                dd_station_list.setEnabled(true);
                dd_station_list.invalidate();
                dd_station_list.setSelection(0);
                dd_station_list.requestFocus();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    //Step 1
    private void validateCrate(String crate){
        JSONObject args = new JSONObject();
        try {
            String rfc = Vars.ZWM_PTL_CRATE_VALIDATE_V3;
            args.put("bapiname", rfc);
            args.put("IM_PLANT", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate);
            args.put("IM_ZONE", dd_station_list.getSelectedItem().toString());
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_CRATE, args);
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

    private void setData(JSONObject responsebody){
        try{
            zoneNature = responsebody.getString("ZONE_NATURE");
            crateNature = responsebody.getString("CRATE_NATURE");
            crateZone = responsebody.getString("CRATE_ZONE");
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            JSONArray EAN_DATA_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");
            int totalEanRecords = EAN_DATA_ARRAY.length();
            if(totalEanRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEanRecords; recordIndex++){
                    HUSave eanData = new Gson().fromJson(EAN_DATA_ARRAY.getJSONObject(recordIndex).toString(), HUSave.class);
                    eanDataMap.put(eanData.getLgean11(), eanData);
                }
            }
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    ETData etData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), ETData.class);
                    etData.setScanQty("0");
                    etDataMap.put(etData.getArticle(), etData);
                }
            }
            if("01".equalsIgnoreCase(zoneNature)){
                step2();
            }else{
                step3();
                if(!"S".equalsIgnoreCase(crateNature)){
                    getNextItemToScan();
                }
            }
            txt_received_crate.setText(UIFuncs.toUpperTrim(txt_scan_crate));
            txt_hub_station.setText(dd_station_list.getSelectedItem().toString());
            txt_proposed_zone.setText(crateZone);
        }catch (Exception exce){
            box.getErrBox(exce);
        }
    }

    //Step 2
    private void validatePalate(String palate){
        JSONObject args = new JSONObject();
        try {
            String rfc = Vars.ZWM_PTL_SZ_PALATE_VALIDATE_V3;
            args.put("bapiname", rfc);
            args.put("IM_PLANT", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_received_crate));
            args.put("IM_PALATE", palate);
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_PALLATE, args);
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

    //Step 3
    private void getNextItemToScan(){
        double recQty = 0.00;

        for (Map.Entry<String, ETData> etDataEntry: etDataMap.entrySet()) {
            ETData etData = etDataEntry.getValue();
            if(!etData.isScanned()){
                currentItem = etData;
                scanData = new ZoneData();
                scanData.setCrate(etData.getCrate());
                scanData.setsCrate(UIFuncs.toUpperTrim(txt_received_crate));
                scanData.setdCrate(etData.getZoneCrate());
                scanData.setArticle(etData.getArticle());
                scanData.setPltRecHubZone(UIFuncs.toUpperTrim(txt_hub_station));
                recQty = Util.convertStringToDouble(etData.getQty());
                break;
            }
        }

        btn_plus.setEnabled(recQty > 1);
        btn_minus.setEnabled(recQty > 1);
        chk_sc.setEnabled(recQty > 1);
        txt_proposed_zone.setText(currentItem.getZoneCrate());
        txt_rec_qty.setText(Util.formatDouble(recQty));
        txt_scan_qty.setText(Util.formatDouble(recQty));
        scanData.setScanQty(UIFuncs.toUpperTrim(txt_scan_qty));
        scanData.setQty(UIFuncs.toUpperTrim(txt_rec_qty));
        if("S".equalsIgnoreCase(crateNature)){
            txt_article.setText(UIFuncs.removeLeadingZeros(currentItem.getArticle()));
            UIFuncs.enableInput(con, txt_scan_zone_crate);
        }
    }

    private void plusMinusQty(String mode){
        double recQty = Util.convertStringToDouble(UIFuncs.toUpperTrim(txt_rec_qty));
        double scanQty = Util.convertStringToDouble(UIFuncs.toUpperTrim(txt_scan_qty));

        if("plus".equalsIgnoreCase(mode) && (scanQty + 1) <= recQty){
            txt_scan_qty.setText(Util.formatDouble((scanQty + 1)));
        }

        else if("minus".equalsIgnoreCase(mode) && (scanQty - 1) >= 1){
            txt_scan_qty.setText(Util.formatDouble((scanQty - 1)));
        }

        scanData.setScanQty(UIFuncs.toUpperTrim(txt_scan_qty));
    }

    private void validateScannedArticle(String barcode){
        if(eanDataMap.containsKey(barcode)){
            HUSave eanData = eanDataMap.get(barcode);
            String matnr = eanData.getLgarticle();
            if(etDataMap.containsKey(matnr)){
                ETData etData = etDataMap.get(matnr);
                double scanQty = Util.convertStringToDouble(etData.getScanQty());
                double reqQty = Util.convertStringToDouble(etData.getQty());

                txt_rec_qty.setText(Util.formatDouble(reqQty));
                txt_proposed_zone.setText(etData.getZoneCrate());

                if((scanQty + 1) > reqQty){
                    box.getBox("Not Allowed", "Scanned QTY cannot be greater than Required QTY");
                }else{
                    etData.setScanQty((scanQty + 1) + "");
                    txt_article.setText(UIFuncs.removeLeadingZeros(matnr));
                    txt_scan_qty.setText(etData.getScanQty());
                }
            }
        }else{
            box.getBox("Invalid EAN", "Scanned EAN not found");
        }
        txt_scan_article.setText("");
        txt_scan_article.requestFocus();
    }

    private JSONArray getScanDataToSubmit(){
        try {
            if(!"01".equalsIgnoreCase(zoneNature) && !"S".equalsIgnoreCase(crateNature)){
                JSONArray arr = new JSONArray();
                for (Map.Entry<String, ETData> etDataEntry: etDataMap.entrySet()) {
                    ETData etData = etDataEntry.getValue();
                    if(Util.convertStringToDouble(etData.getScanQty()) > 0){
                        ZoneData scanData = new ZoneData();
                        scanData.setCrate(etData.getCrate());
                        scanData.setsCrate(UIFuncs.toUpperTrim(txt_received_crate));
                        scanData.setdCrate(etData.getZoneCrate());
                        scanData.setArticle(etData.getArticle());
                        scanData.setPltRecHubZone(UIFuncs.toUpperTrim(txt_hub_station));
                        scanData.setQty(etData.getQty());
                        scanData.setScanQty(etData.getScanQty());
                        scanData.setZoneCrate(UIFuncs.toUpperTrim(txt_zone_crate));
                        arr.put(new JSONObject(new Gson().toJson(scanData)));
                    }
                }
                if(arr.length() == 0){
                    txt_scan_article.setText("");
                    txt_scan_article.requestFocus();
                    box.getBox("Not To Save", "Please to save, please scan some articles");
                    return null;
                }
                return arr;
            }else{
                double recQty = Util.convertStringToDouble(UIFuncs.toUpperTrim(txt_rec_qty));
                double scanQty = Util.convertStringToDouble(scanData.getScanQty());
                if(scanQty < recQty && !chk_sc.isChecked()){
                    box.getBox("Short Close?", "Scanned quantity is less than required quantity. Please check the box SC to confirm");
                    return null;
                }
                if(scanQty == recQty && chk_sc.isChecked()){
                    box.getBox("Invalid Input", "Scanned quantity is equal to required quantity. But SC is checked, please check.");
                    return null;
                }
                scanData.setZoneCrate(UIFuncs.toUpperTrim(txt_zone_crate));
                return new JSONArray().put(new JSONObject(new Gson().toJson(scanData)));
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    private void validateZoneCrate(String zoneCrate){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_MZ_SART_HUBCRT_VALI_V3);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_HUB_STN", dd_station_list.getSelectedItem().toString());
            JSONArray etScanData = getScanDataToSubmit();
            if(etScanData == null){
                return;
            }
            args.put("IT_DATA", etScanData);
            showProcessingAndSubmit(Vars.ZWM_PTL_MZ_SART_HUBCRT_VALI_V3, REQUEST_VALIDATE_ZONE_CRATE, args);

        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if(dialog!=null) {
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
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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
                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            txt_scan_crate.setText("");
                                            txt_scan_crate.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_GET_HUB_STATIONS) {
                                            setStationsList(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_CRATE) {
                                            setData(responsebody);
                                        } else if (request == REQUEST_VALIDATE_PALLATE) {
                                            txt_pallate.setText(UIFuncs.toUpperTrim(txt_scan_pallate));
                                            txt_scan_pallate.setText("");
                                            txt_scan_pallate.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_ZONE_CRATE) {
                                            if("S".equalsIgnoreCase(crateNature)) {
                                                if ("X".equalsIgnoreCase(scanData.isShortClose())) {
                                                    box.getBox("Short Closed", "Article short closed. Going back to scan new Rec Crate");
                                                    clear();
                                                } else {
                                                    getNextItemToScan();
                                                    txt_zone_crate.setText(UIFuncs.toUpperTrim(txt_scan_zone_crate));
                                                    txt_scan_zone_crate.setText("");
                                                    txt_scan_zone_crate.requestFocus();

                                                }
                                            }
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

    public class ETData {
        @SerializedName("DCRATE")
        private String dCrate;
        @SerializedName("ARTICLE")
        private String article;
        @SerializedName("CRATE")
        private String crate;
        @SerializedName("QTY")
        private String qty;
        @SerializedName("ZONE_COUNT")
        private String zoneCount;
        @SerializedName("SCRATE")
        private String sCrate;
        @SerializedName("ZONE_CRATE")
        private String zoneCrate;
        @SerializedName("SHORT_CLOSE")
        private String shortClose;
        @SerializedName("SCAN_QTY")
        private String scanQty;
        private boolean scanned;

        public ETData newInstance(ETData source){
            ETData target = new ETData();
            target.dCrate = source.getdCrate();
            target.article = source.getArticle();
            target.crate = source.getCrate();
            target.qty = source.getQty();
            target.zoneCount = source.getZoneCount();
            target.sCrate = source.getsCrate();
            target.zoneCrate = source.getZoneCrate();
            target.shortClose = source.getShortClose();
            target.scanned = source.scanned;
            target.scanQty = source.scanQty;
            return target;
        }

        public String getdCrate() {
            return dCrate;
        }

        public void setdCrate(String dCrate) {
            this.dCrate = dCrate;
        }

        public String getArticle() {
            return article;
        }

        public void setArticle(String article) {
            this.article = article;
        }

        public String getCrate() {
            return crate;
        }

        public void setCrate(String crate) {
            this.crate = crate;
        }

        public String getQty() {
            return qty;
        }

        public void setQty(String qty) {
            this.qty = qty;
        }

        public String getZoneCount() {
            return zoneCount;
        }

        public void setZoneCount(String zoneCount) {
            this.zoneCount = zoneCount;
        }

        public String getsCrate() {
            return sCrate;
        }

        public void setsCrate(String sCrate) {
            this.sCrate = sCrate;
        }

        public String getZoneCrate() {
            return zoneCrate;
        }

        public void setZoneCrate(String zoneCrate) {
            this.zoneCrate = zoneCrate;
        }

        public String getShortClose() {
            return shortClose;
        }

        public void setShortClose(String shortClose) {
            this.shortClose = shortClose;
        }

        public boolean isScanned() {
            return scanned;
        }

        public void setScanned(boolean scanned) {
            this.scanned = scanned;
        }

        public String getScanQty() {
            return scanQty;
        }

        public void setScanQty(String scanQty) {
            this.scanQty = scanQty;
        }
    }
}