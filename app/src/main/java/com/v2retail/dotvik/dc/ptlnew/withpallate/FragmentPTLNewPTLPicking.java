package com.v2retail.dotvik.dc.ptlnew.withpallate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
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
import com.v2retail.commons.SapJsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.dc.ptlnew.ScanData;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
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

public class FragmentPTLNewPTLPicking extends Fragment implements  View.OnClickListener {
    private static final int REQUEST_ZONE_LIST = 1500;
    private static final int REQUEST_SECTION_LIST = 1501;
    private static final int REQUEST_PICKLIST_LIST = 1502;
    private static final int REQUEST_GET_PICKLIST_DATA = 1503;
    private static final int REQUEST_PALLETE_VALIDATE = 1504;
    private static final int REQUEST_CRATE_VALIDATE = 1505;
    private static final int REQUEST_MSA_BIN_VALIDATE = 1506;
    private static final int REQUEST_MSA_CRATE_VALIDATE = 1507;
    private static final int REQUEST_SAVE = 1508;

    private static final String TAG = FragmentPTLNewPTLPicking.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    List<String> zones = new ArrayList<String>();
    ArrayAdapter<String> zoneAdapter;

    List<String> sections = new ArrayList<String>();
    ArrayAdapter<String> sectionAdapter;

    List<String> picklists = new ArrayList<String>();
    ArrayAdapter<String> picklistAdapter;

    boolean spinnerTouched = false;

    Spinner dd_zone_list, dd_section_list, dd_picklist_list;
    Button btn_back, btn_prev, btn_continue, btn_save;
    LinearLayout ll_screen_1, ll_screen_2, ll_screen_3;
    EditText txt_picklistno, txt_scan_pallete, txt_scan_crate, txt_scan_msa_bin, txt_scanned_msa_bin, txt_scan_msa_crate, txt_scanned_msa_crate;
    EditText txt_picklistno_2, txt_scan_pallete_2, txt_scan_crate_2, txt_scan_msa_bin_2, txt_scan_msa_crate_2, txt_scan_article;
    TableLayout table_bin_crate, table_article_scan;

    Map<String, PicklistData> picklistDataMap = new LinkedHashMap<>();
    Map<String, PicklistData> etDataMap = new HashMap<>();
    Map<String, HUEANData> eanDataMap = new HashMap<>();

    int currentStep = 0;

    public FragmentPTLNewPTLPicking() {

    }

    public static FragmentPTLNewPTLPicking newInstance() {
        return new FragmentPTLNewPTLPicking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL Picking");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_ptl_picking, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        dd_zone_list = rootView.findViewById(R.id.ptl_new_picking_dd_zone);
        dd_zone_list.setSelection(0);
        dd_section_list = rootView.findViewById(R.id.ptl_new_picking_dd_section);
        dd_section_list.setSelection(0);
        dd_picklist_list = rootView.findViewById(R.id.ptl_new_picking_dd_picklist);
        dd_picklist_list.setSelection(0);

        txt_picklistno = rootView.findViewById(R.id.txt_ptl_new_picklist_picklistno);
        txt_scan_pallete = rootView.findViewById(R.id.txt_ptl_new_picklist_scan_pallete);
        txt_scan_crate = rootView.findViewById(R.id.txt_ptl_new_picklist_scan_crate);
        txt_scan_msa_bin = rootView.findViewById(R.id.txt_ptl_new_picklist_msa_bin);
        txt_scan_msa_crate = rootView.findViewById(R.id.txt_ptl_new_picklist_msa_crate);
        txt_scanned_msa_bin = rootView.findViewById(R.id.txt_ptl_new_picklist_msa_bin_scanned);
        txt_scanned_msa_crate = rootView.findViewById(R.id.txt_ptl_new_picklist_msa_crate_scanned);
        txt_picklistno_2 = rootView.findViewById(R.id.txt_ptl_new_picklist_picklistno_2);
        txt_scan_pallete_2 = rootView.findViewById(R.id.txt_ptl_new_picklist_scan_pallete_2);
        txt_scan_crate_2 = rootView.findViewById(R.id.txt_ptl_new_picklist_scan_crate_2);
        txt_scan_msa_bin_2 = rootView.findViewById(R.id.txt_ptl_new_picklist_msa_bin_2);
        txt_scan_msa_crate_2 = rootView.findViewById(R.id.txt_ptl_new_picklist_msa_crate_2);
        txt_scan_article = rootView.findViewById(R.id.txt_ptl_new_picklist_scan_article);
        table_bin_crate = rootView.findViewById(R.id.table_ptl_new_picking_msa_bin_crate);
        table_article_scan = rootView.findViewById(R.id.table_ptl_new_picking_scanned_articles);

        ll_screen_1 = rootView.findViewById(R.id.ll_ptl_new_picking_screen_1);
        ll_screen_2 = rootView.findViewById(R.id.ll_ptl_new_picking_screen_2);
        ll_screen_3 = rootView.findViewById(R.id.ll_ptl_new_picking_screen_3);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_picking_back);
        btn_prev = rootView.findViewById(R.id.btn_ptl_new_picking_prev);
        btn_continue = rootView.findViewById(R.id.btn_ptl_new_picking_continue);
        btn_save = rootView.findViewById(R.id.btn_ptl_new_picking_save);

        zoneAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, zones);
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_zone_list.setAdapter(zoneAdapter);
        dd_zone_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        sectionAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_section_list.setAdapter(sectionAdapter);
        dd_section_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        picklistAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, picklists);
        picklistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_picklist_list.setAdapter(picklistAdapter);
        dd_picklist_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        clear();
        addInputEvents();

        return rootView;
    }

    private void addInputEvents(){
        dd_zone_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    sectionAdapter.clear();
                    getSectionList();
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                sectionAdapter.clear();
            }
        });

        dd_section_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    picklistAdapter.clear();
                    getPicklistList();
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                picklistAdapter.clear();
            }
        });

        dd_picklist_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    if(dd_picklist_list.getSelectedItem() != null && !dd_picklist_list.getSelectedItem().toString().isEmpty()){
                        getPicklistListData(dd_picklist_list.getSelectedItem().toString());
                    }
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        btn_back.setOnClickListener(this);
        btn_prev.setOnClickListener(this);
        btn_continue.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        txt_scan_pallete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_pallete);
                    if (!value.isEmpty()) {
                        validatePallete(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_pallete.addTextChangedListener(new TextWatcher() {
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
                    validatePallete(value);
                }
            }
        });

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

        txt_scan_msa_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_msa_bin);
                    if (!value.isEmpty()) {
                        validateMSABin(value);
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
                    validateMSABin(value);
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

        txt_scan_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_article);
                    if (!value.isEmpty()) {
                        populateCurrentArticle(value);
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
                    populateCurrentArticle(value);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_picking_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_ptl_new_picking_continue:
                if(currentStep == 2){
                    if(UIFuncs.toUpperTrim(txt_scanned_msa_crate).length() > 0){
                        step3(1);
                    }
                }
                else if(currentStep == 3){
                    step2("cont");
                }
                break;
            case R.id.btn_ptl_new_picking_prev:
                box.getBox("Alert", "Are you sure you want to go to previous screen?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(currentStep == 2){
                            step1();
                        }
                        else if(currentStep == 3){
                            step2("prev");
                        }
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });
                break;
            case R.id.btn_ptl_new_picking_save:
                saveScannedData();
                break;
        }
    }

    private void clear(){
        step3(0);
        step2("");
        step1();
    }

    private void step1(){
        step1Visibility();
        picklistDataMap.clear();
        zoneAdapter.clear();
        sectionAdapter.clear();
        picklistAdapter.clear();
        picklists.clear();
        zones.clear();
        sections.clear();
        dd_picklist_list.setEnabled(false);
        dd_section_list.setEnabled(false);
        getZoneList();
    }

    private void step2(String mode){
        if(dd_picklist_list.getSelectedItem() != null){
            txt_picklistno.setText(dd_picklist_list.getSelectedItem().toString());
        }else{
            txt_picklistno.setText("");
        }

        if(mode.equals("cont")){
            step3(0);
            step2Visibility();
            txt_scanned_msa_bin.setText("");
            txt_scanned_msa_crate.setText("");
            UIFuncs.enableInput(con, txt_scan_msa_bin);
            return;
        }
        else if(mode.equals("prev")){
            step2Visibility();
            return;
        }
        etDataMap.clear();
        eanDataMap.clear();
        step2Visibility();
        table_bin_crate.removeAllViews();
        txt_scan_pallete.setText("");
        txt_scan_crate.setText("");
        txt_scan_msa_bin.setText("");
        txt_scan_msa_crate.setText("");
        txt_scanned_msa_bin.setText("");
        txt_scanned_msa_crate.setText("");
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_msa_bin);
        UIFuncs.disableInput(con, txt_scan_msa_crate);
        UIFuncs.disableInput(con, txt_scanned_msa_bin);
        UIFuncs.disableInput(con, txt_scanned_msa_crate);
        UIFuncs.enableInput(con, txt_scan_pallete);
    }

    private void step3(int mode){
        step3Visibility();
        if(mode == 0){
            table_article_scan.removeAllViews();
            txt_picklistno_2.setText("");
            txt_scan_pallete_2.setText("");
            txt_scan_crate_2.setText("");
            txt_scan_msa_bin_2.setText("");
            txt_scan_msa_crate_2.setText("");
        }
        if(mode == 1){
            txt_picklistno_2.setText(UIFuncs.toUpperTrim(txt_picklistno));
            txt_scan_pallete_2.setText(UIFuncs.toUpperTrim(txt_scan_pallete));
            txt_scan_crate_2.setText(UIFuncs.toUpperTrim((txt_scan_crate)));
            txt_scan_msa_bin_2.setText(UIFuncs.toUpperTrim((txt_scanned_msa_bin)));
            txt_scan_msa_crate_2.setText(UIFuncs.toUpperTrim((txt_scanned_msa_crate)));
            generateHeaderRow();
            generateNonScannedArticleRows(null);
        }
        txt_scan_article.setText("");
        UIFuncs.enableInput(con, txt_scan_article);
    }

    private void step1Visibility(){
        ll_screen_1.setVisibility(View.VISIBLE);
        ll_screen_2.setVisibility(View.GONE);
        ll_screen_3.setVisibility(View.GONE);
        btn_prev.setVisibility(View.GONE);
        btn_continue.setVisibility(View.GONE);
        btn_save.setVisibility(View.GONE);
        currentStep = 1;
    }

    private void step2Visibility(){
        ll_screen_1.setVisibility(View.GONE);
        ll_screen_2.setVisibility(View.VISIBLE);
        ll_screen_3.setVisibility(View.GONE);
        btn_prev.setVisibility(View.VISIBLE);
        btn_continue.setVisibility(View.VISIBLE);
        if(!etDataMap.isEmpty() && etDataMap.size() > 0){
            btn_save.setVisibility(View.VISIBLE);
        }else{
            btn_save.setVisibility(View.GONE);
        }
        currentStep = 2;
    }

    private void step3Visibility(){
        ll_screen_1.setVisibility(View.GONE);
        ll_screen_2.setVisibility(View.GONE);
        ll_screen_3.setVisibility(View.VISIBLE);
        btn_prev.setVisibility(View.VISIBLE);
        btn_continue.setVisibility(View.VISIBLE);
        btn_save.setVisibility(View.VISIBLE);
        currentStep = 3;
    }

    //Step 1 Process

    private void getZoneList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GET_ZONE);
            args.put("IM_USER", USER);
            args.put("IM_ZONE", null);
            showProcessingAndSubmit(Vars.ZWM_PTL_GET_ZONE, REQUEST_ZONE_LIST, args);
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

    private void getSectionList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_MSA_SECTION_LIST);
            args.put("IM_USER", USER);
            args.put("IM_ZONE", dd_zone_list.getSelectedItem().toString());
            showProcessingAndSubmit(Vars.ZWM_GET_MSA_SECTION_LIST, REQUEST_SECTION_LIST, args);
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

    private void getPicklistList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZGRT_PICK_GET_TO_LIST_PTL);
            args.put("IM_USER", USER);
            args.put("IM_ZONE", dd_zone_list.getSelectedItem().toString());
            args.put("IM_SECTION", dd_section_list.getSelectedItem().toString());
            showProcessingAndSubmit(Vars.ZGRT_PICK_GET_TO_LIST_PTL, REQUEST_PICKLIST_LIST, args);
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

    private void getPicklistListData(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GET_TO_DETAILS);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_GET_TO_DETAILS, REQUEST_GET_PICKLIST_DATA, args);
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

    public void setZoneList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_ZONE");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                zones.clear();
                zones.add("Select");
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    zones.add(ET_RECORD.getString("ZONE"));
                }
                ((BaseAdapter) dd_zone_list.getAdapter()).notifyDataSetChanged();
                dd_zone_list.invalidate();
                dd_zone_list.setSelection(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void setSectionList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_SECTION");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                sections.clear();
                sections.add("Select");
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    sections.add(ET_RECORD.getString("MERGE"));
                }
                ((BaseAdapter) dd_section_list.getAdapter()).notifyDataSetChanged();
                dd_section_list.setEnabled(true);
                dd_section_list.invalidate();
                dd_section_list.setSelection(0);
                dd_section_list.requestFocus();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void setPicklistList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                picklists.clear();
                picklists.add("Select");
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    picklists.add(ET_RECORD.getString("TANUM"));
                }
                ((BaseAdapter) dd_picklist_list.getAdapter()).notifyDataSetChanged();
                dd_picklist_list.setEnabled(true);
                dd_picklist_list.invalidate();
                dd_picklist_list.setSelection(0);
                dd_picklist_list.requestFocus();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void setPicklistListData(JSONObject responsebody){
        try
        {
            picklistDataMap = new LinkedHashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    PicklistData picklistData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), PicklistData.class);
                    picklistDataMap.put(picklistData.getBin()+"-"+picklistData.getCrate(), picklistData);
                }
            }
            if(!picklistDataMap.isEmpty()){
                step2("");
                populateBinCrateTable();
            }else{
                box.getBox("Empty", "Picklist Data is Empty");
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
        table_bin_crate.addView(tr, trParams);

        int rowNum = 1;
        for (Map.Entry<String, PicklistData> pickDataEntry :picklistDataMap.entrySet()) {
            PicklistData data = pickDataEntry.getValue();
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

            tr = new TableRow(getContext());
            tr.setId(rowNum);
            tr.setPadding(0,0,0,0);
            tr.setLayoutParams(trParams);
            tr.addView(tvSno);
            tr.addView(tvBin);
            tr.addView(tvCrate);
            tr.setTag(data);
            table_bin_crate.addView(tr, trParams);
            rowNum++;
        }
    }

    //Step 2 Process

    private void validatePallete(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_PALETTE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_SECTION", dd_section_list.getSelectedItem().toString());
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_PALETTE", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_PALETTE_VALIDATE, REQUEST_PALLETE_VALIDATE, args);
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

    private void validateCrate(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_CRATE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_SECTION", dd_section_list.getSelectedItem().toString());
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_PALETTE", UIFuncs.toUpperTrim(txt_scan_crate));
            args.put("IM_CRATE", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_CRATE_VALIDATE, REQUEST_CRATE_VALIDATE, args);
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

    private void validateMSABin(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_BIN_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_SECTION", dd_section_list.getSelectedItem().toString());
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_PALETTE", UIFuncs.toUpperTrim(txt_scan_pallete));
            args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_scan_crate));
            args.put("IM_BIN", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_BIN_VALIDATE, REQUEST_MSA_BIN_VALIDATE, args);
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

    private void validateMSACrate(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_MSA_CRATE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_SECTION", dd_section_list.getSelectedItem().toString());
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_PALETTE", UIFuncs.toUpperTrim(txt_scan_pallete));
            args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_scan_crate));
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_scanned_msa_bin));
            args.put("IM_MSA_CRATE", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_MSA_CRATE_VALIDATE, REQUEST_MSA_CRATE_VALIDATE, args);
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

    //Step 3 Process
    private void setMSABinTableData(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            JSONArray ET_EAN_DATA_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            int totalEanRecords = ET_EAN_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    PicklistData etData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), PicklistData.class);
                    if(!etDataMap.containsKey(etData.getMsaCrate() + "-" + etData.getBin() + "-" + UIFuncs.removeLeadingZeros(etData.getArticle()))){
                        etDataMap.put(etData.getMsaCrate() + "-" + etData.getBin() + "-" + UIFuncs.removeLeadingZeros(etData.getArticle()), etData);
                    }
                }
            }
            if(totalEanRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEanRecords; recordIndex++){
                    HUEANData eanData = new Gson().fromJson(ET_EAN_DATA_ARRAY.getJSONObject(recordIndex).toString(), HUEANData.class);
                    eanDataMap.put(eanData.getLgean11(), eanData);
                }
            }
            if(totalEtRecords > 0 && totalEanRecords > 0){
                step3(1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    private void generateHeaderRow(){
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0;
        headerTextSize = 16;

        table_article_scan.removeAllViews();

        //Create Header Row In Table
        TextView headerArticle = new TextView(getContext());
        TextView headerRQty = new TextView(getContext());
        TextView headerSQty = new TextView(getContext());
        headerArticle.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        headerArticle.setGravity(Gravity.CENTER);
        headerArticle.setPadding(5,5,0,5);
        headerArticle.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerArticle.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerArticle.setText("ARTICLE");

        headerRQty.setGravity(Gravity.CENTER);
        headerRQty.setPadding(5,5,5,5);
        headerRQty.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerRQty.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerRQty.setText(" R.QTY ");

        headerSQty.setGravity(Gravity.CENTER);
        headerSQty.setPadding(0,5,0,5);
        headerSQty.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSQty.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSQty.setText("SCAN QTY");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerArticle);
        tr.addView(headerRQty);
        tr.addView(headerSQty);
        table_article_scan.addView(tr, trParams);
    }
    private void populateCurrentArticle(String barcode){
        generateHeaderRow();
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(0, 0, 0, 0);
        String key = null;
        if(eanDataMap.containsKey(barcode)){
            HUEANData eanData = eanDataMap.get(barcode);
            key = UIFuncs.toUpperTrim(txt_scanned_msa_crate) + "-" + UIFuncs.toUpperTrim(txt_scanned_msa_bin) + "-" + UIFuncs.removeLeadingZeros(eanData.getLgmatnr());
            if(etDataMap.containsKey(key)){
                PicklistData etData = etDataMap.get(key);
                if(!"SIN".equals(etData.getEType())){
                    double maxQty= Double.parseDouble(etData.getQuantity());
                    if(etData.getSqty() >= maxQty){
                        box.getBox("Invalid", String.format("Already scanned maximum allowed Qty - %d", etData.getSqty()));
                    }else{
                        etData.setSqty(etData.getSqty() + 1);
                    }
                }
                populateArticleTableRow(etData, true);
            }else{
                box.getBox("Invalid", String.format("Scanned barcode is not associated with %s and %s",UIFuncs.toUpperTrim(txt_scanned_msa_crate), UIFuncs.toUpperTrim(txt_scanned_msa_bin)));
            }
        }else{
            box.getBox("Invalid", "Scanned barcode is not available");
        }
        generateNonScannedArticleRows(key);
        txt_scan_article.setText("");
    }
    private void generateNonScannedArticleRows(String key){
        for (Map.Entry<String, PicklistData> etDataEntry :etDataMap.entrySet()) {
            PicklistData etData = etDataEntry.getValue();
            if(!etDataEntry.getKey().equals(key) && etDataEntry.getKey().startsWith(UIFuncs.toUpperTrim(txt_scan_msa_crate_2) + "-" + UIFuncs.toUpperTrim(txt_scan_msa_bin_2) + "-")){
                populateArticleTableRow(etData, false);
            }
        }
    }
    private void populateArticleTableRow(PicklistData data, boolean isScanned){
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(0, 0, 0, 0);
        TableRow tr;
        TextView tvSno = new TextView(getContext());
        tvSno.setText(" " + UIFuncs.removeLeadingZeros(data.getArticle()) + " ");
        tvSno.setTextSize(16);
        tvSno.setHeight(Util.dpToPx(con,30));
        tvSno.setGravity(Gravity.CENTER);
        tvSno.setPadding(5,2,0,2);
        tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

        TextView tvBin = new TextView(getContext());
        tvBin.setText(Util.convertToDoubleString(data.getQuantity()));
        tvBin.setTextSize(16);
        tvBin.setHeight(Util.dpToPx(con,30));
        tvBin.setGravity(Gravity.CENTER);
        tvBin.setPadding(5,2,0,2);
        tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

        tr = new TableRow(getContext());
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(tvSno);
        tr.addView(tvBin);

        if(data.getEType().equals("SIN") && isScanned){
            LinearLayout qtyCell = new LinearLayout(getContext());
            qtyCell.setOrientation(LinearLayout.HORIZONTAL);
            qtyCell.setGravity(Gravity.CENTER_VERTICAL);
            TableRow.LayoutParams cellParams = new TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f);
            qtyCell.setLayoutParams(cellParams);
            Drawable cellBg = getResources().getDrawable(R.drawable.table_cell_border);
            TextView tvMinus = new TextView(getContext());
            tvMinus.setText("−");
            tvMinus.setTextSize(16);
            tvMinus.setGravity(Gravity.CENTER);
            tvMinus.setLayoutParams(
                    new LinearLayout.LayoutParams(Util.dpToPx(con,40),
                            Util.dpToPx(con,30)));
            tvMinus.setBackground(cellBg);
            tvMinus.setPadding(2, 10, 2, 10);
            EditText tvQty = new EditText(getContext());
            if(data.getSqty() == 0){
                tvQty.setText(Util.convertToDoubleString(data.getQuantity()));
                int current = Integer.parseInt(tvQty.getText().toString());
                data.setSqty(current);
            }else{
                tvQty.setText(String.valueOf(data.getSqty()));
            }
            tvQty.setTextSize(16);
            tvQty.setGravity(Gravity.CENTER);
            tvQty.setPadding(3, 2, 3, 2);
            LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(
                    0,
                    Util.dpToPx(con, 30),
                    1f
            );
            tvQty.setInputType(InputType.TYPE_CLASS_NUMBER);
            tvQty.setLayoutParams(qtyParams);
            tvQty.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                    View parent = (View) tvQty.getParent();  // This will be a LinearLayout if you wrapped the EditText
                    while (parent != null && !(parent instanceof TableRow)) {
                        parent = (View) parent.getParent();  // Climb up the hierarchy until you find the TableRow
                    }

                    if (parent != null) {
                        TableRow row = (TableRow) parent;
                        PicklistData tagdata = (PicklistData) row.getTag();
                        PicklistData data = etDataMap.get(tagdata.getMsaCrate() + "-" + tagdata.getBin() + "-" + UIFuncs.removeLeadingZeros(tagdata.getArticle()));
                        String text = s.toString().trim();
                        try {
                            int newQty = text.isEmpty() ? 0 : Integer.parseInt(text);
                            int rQty = Integer.parseInt(Util.convertToDoubleString(data.getQuantity()));
                            data.setSqty(newQty);
                            if(newQty > rQty){
                                data.setSqty(rQty);
                                tvQty.setText(rQty + "");
                            }
                            else if(newQty < 0){
                                data.setSqty(0);
                                tvQty.setText("0");
                            }
                            tvQty.selectAll();
                        } catch (NumberFormatException e) {
                            e.printStackTrace(); // Or show validation error
                        }
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
            });

            TextView tvPlus = new TextView(getContext());
            tvPlus.setText("+");
            tvPlus.setTextSize(16);
            tvPlus.setGravity(Gravity.CENTER);
            tvPlus.setLayoutParams(
                    new LinearLayout.LayoutParams(Util.dpToPx(con,40),
                            Util.dpToPx(con,30)));
            tvPlus.setBackground(cellBg);
            tvPlus.setPadding(2, 10, 2, 10);
            qtyCell.addView(tvMinus);
            qtyCell.addView(tvQty);
            qtyCell.addView(tvPlus);
            tr.addView(qtyCell);
            tvQty.selectAll();

            final int maxQty = Integer.parseInt(Util.convertToDoubleString(data.getQuantity()));

            View.OnClickListener incrementer = v -> {

                int current = Integer.parseInt(tvQty.getText().toString());

                if (v == tvMinus && current > 0) {
                    current--;
                } else if (v == tvPlus && current < maxQty) {
                    current++;
                } else {
                    return;
                }

                tvQty.setText(String.valueOf(current));
                tvQty.selectAll();
                data.setSqty(current);
            };

            tvMinus.setOnClickListener(incrementer);
            tvPlus.setOnClickListener(incrementer);
        }else{
            TextView tvQty = new TextView(getContext());
            tvQty.setText(String.valueOf(data.getSqty()));
            tvQty.setTextSize(16);
            tvQty.setHeight(Util.dpToPx(con,30));
            tvQty.setGravity(Gravity.CENTER);
            tvQty.setPadding(5,2,0,2);
            tvQty.setBackground(getResources().getDrawable(R.drawable.table_cell_border));
            tr.addView(tvQty);
        }
        tr.setTag(data);
        table_article_scan.addView(tr, trParams);
        return;
    }
    private void saveScannedData(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_V06_V09);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGNUM", "V2R");
            args.put("IM_LGORT", "0001");
            args.put("IM_LGTYP", "V06");
            JSONArray itData = getScanDataToSubmit();
            if(itData != null){
                args.put("IT_DATA", itData);
                showProcessingAndSubmit(Vars.ZWM_PTL_V06_V09, REQUEST_SAVE, args);
            }
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
    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, PicklistData> dataEntry: etDataMap.entrySet()) {
                PicklistData etData = dataEntry.getValue();
                if(etData.getSqty() > 0){
                    ScanData data = new ScanData().newInstance(etData, UIFuncs.toUpperTrim(txt_scan_crate), UIFuncs.toUpperTrim(txt_scan_pallete));
                    String scanDataJsonString = new Gson().toJson(data);
                    JSONObject itDataJson = new JSONObject(scanDataJsonString);
                    arrScanData.put(itDataJson);
                }
            }
            if (arrScanData.length() == 0) {
                box.getBox("Empty Request", "Noting to submit, please scan some articles");
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
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_PALLETE_VALIDATE) {
                                            txt_scan_pallete.setText("");
                                            txt_scan_pallete.requestFocus();
                                        }
                                        else if (request == REQUEST_CRATE_VALIDATE) {
                                            txt_scan_crate.setText("");
                                            txt_scan_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_MSA_BIN_VALIDATE) {
                                            txt_scan_msa_bin.setText("");
                                            txt_scan_msa_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_MSA_CRATE_VALIDATE) {
                                            txt_scan_msa_crate.setText("");
                                            txt_scan_msa_crate.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_GET_PICKLIST_DATA) {
                                            setPicklistListData(responsebody);
                                        }
                                        else if (request == REQUEST_ZONE_LIST) {
                                            setZoneList(responsebody);
                                        }
                                        else if (request == REQUEST_SECTION_LIST) {
                                            setSectionList(responsebody);
                                        }
                                        else if (request == REQUEST_PICKLIST_LIST) {
                                            setPicklistList(responsebody);
                                        }
                                        else if (request == REQUEST_PALLETE_VALIDATE) {
                                            UIFuncs.disableInput(con, txt_scan_pallete);
                                            UIFuncs.enableInput(con, txt_scan_crate);
                                        }
                                        else if (request == REQUEST_CRATE_VALIDATE) {
                                            UIFuncs.disableInput(con, txt_scan_crate);
                                            UIFuncs.enableInput(con, txt_scan_msa_bin);
                                        }
                                        else if (request == REQUEST_MSA_BIN_VALIDATE) {
                                            txt_scanned_msa_bin.setText(UIFuncs.toUpperTrim(txt_scan_msa_bin));
                                            txt_scan_msa_bin.setText("");
                                            UIFuncs.enableInput(con, txt_scan_msa_crate);
                                        }
                                        else if (request == REQUEST_MSA_CRATE_VALIDATE) {
                                            txt_scanned_msa_crate.setText(UIFuncs.toUpperTrim(txt_scan_msa_crate));
                                            txt_scan_msa_crate.setText("");
                                            setMSABinTableData(responsebody);
                                        }
                                        else if (request == REQUEST_SAVE) {
                                            box.getBox("Success", responsebody.get("EX_TANUM").toString(), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    getPicklistListData(UIFuncs.toUpperTrim(txt_picklistno));
                                                }
                                            });
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