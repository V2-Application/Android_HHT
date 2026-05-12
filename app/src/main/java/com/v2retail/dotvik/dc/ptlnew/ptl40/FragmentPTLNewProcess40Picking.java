package com.v2retail.dotvik.dc.ptlnew.ptl40;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.dc.ptlnew.ScanData;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickData;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentPTLNewProcess40Picking#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentPTLNewProcess40Picking extends Fragment  implements View.OnClickListener {

    private static final int REQUEST_BIN_DATA = 1500;
    private static final int REQUEST_GET_PICKLIST_DATA = 1501;
    private static final int REQUEST_VALIDATE_EMPTY_CRATE = 1502;
    private static final int REQUEST_VALIDATE_MSA_BIN = 1503;
    private static final int REQUEST_VALIDATE_MSA_CRATE = 1504;
    private static final int REQUEST_SAVE = 1505;

    private static final String TAG = FragmentPTLNewProcess40Picking.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    Button btn_back, btn_next, btn_save;
    LinearLayout ll_screen_1, ll_screen_2, ll_screen_3;
    EditText txt_picklist, txt_floor, txt_section, txt_scan_bin;
    EditText txt_picklistno, txt_scanned_floor, txt_scanned_floor_bin, txt_scanned_section, txt_empty_crate, txt_scanned_empty_crate, txt_scanned_bin, txt_scan_msa_bin, txt_scanned_msa_bin, txt_scan_msa_crate, txt_scanned_msa_crate;
    EditText txt_picklistno_2, txt_scanned_empty_crate_2, txt_scanned_msa_bin_2, txt_scanned_msa_crate_2, txt_scan_article;
    TableLayout table_bin_crate, table_article_scan;

    Map<String, BinCrateData> binDataMap = new LinkedHashMap<>();
    Map<String, ETPickData> picklistDataMap = new HashMap<>();
    Map<String, PicklistData> etDataMap = new HashMap<>();
    Map<String, HUEANData> eanDataMap = new HashMap<>();
    Map<String, String> crateArticle = new HashMap<>();

    int totalScanned = 0;
    int totalBin = 0;
    int currentStep = 1;

    public FragmentPTLNewProcess40Picking() {
        // Required empty public constructor
    }

    public static FragmentPTLNewProcess40Picking newInstance() {
        return new FragmentPTLNewProcess40Picking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL Picking - Process 4.0");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ptl_new_process40_picking, container, false);


        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        txt_scan_bin = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_scan_floor_bin);
        txt_floor = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_floor);
        txt_section = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_section);
        txt_picklist = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_picklist);

        txt_picklistno = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_picklistno);
        txt_scanned_floor = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_scanned_floor);
        txt_scanned_floor_bin = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_scanned_floor_bin);
        txt_scanned_section = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_scanned_section);
        txt_scanned_bin = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_scanned_bin_tot_bin);
        txt_empty_crate = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_empty_crate);
        txt_scanned_empty_crate = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_empty_crate_scanned);
        txt_scan_msa_bin = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_msa_bin);
        txt_scanned_msa_bin = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_msa_bin_scanned);
        txt_scan_msa_crate = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_msa_crate);
        txt_scanned_msa_crate = rootView.findViewById(R.id.txt_ptl_new_picking_process_40_msa_crate_scanned);

        txt_picklistno_2 = rootView.findViewById(R.id.txt_ptl_new_process_40_picking_picklistno_2);
        txt_scanned_empty_crate_2 = rootView.findViewById(R.id.txt_ptl_new_process_40_picking_scan_crate_2);
        txt_scanned_msa_bin_2 = rootView.findViewById(R.id.txt_ptl_new_process_40_picking_msa_bin_2);
        txt_scanned_msa_crate_2 = rootView.findViewById(R.id.txt_ptl_new_process_40_picking_msa_crate_2);
        txt_scan_article = rootView.findViewById(R.id.txt_ptl_new_process_40_picking_scan_article);

        table_bin_crate = rootView.findViewById(R.id.table_ptl_new_picking_process_40_msa_bin_crate);
        table_article_scan = rootView.findViewById(R.id.table_ptl_new_process_40_picking_scanned_articles);

        ll_screen_1 = rootView.findViewById(R.id.ll_ptl_new_picking_process_40_screen_1);
        ll_screen_2 = rootView.findViewById(R.id.ll_ptl_new_picking_process_40_screen_2);
        ll_screen_3 = rootView.findViewById(R.id.ll_ptl_new_process_40_picking_screen_3);

        btn_back =  rootView.findViewById(R.id.btn_ptl_new_picking_process_40_back);
        btn_next =  rootView.findViewById(R.id.btn_ptl_new_picking_process_40_next);
        btn_save =  rootView.findViewById(R.id.btn_ptl_new_picking_process_40_save);

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        addInputEvents();
        clear();

        return rootView;
    }

    private void addInputEvents(){
        txt_scan_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_bin);
                    if (!value.isEmpty()) {
                        validateBin(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_bin.addTextChangedListener(new TextWatcher() {
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
                    validateBin(value);
                }
            }
        });
        txt_empty_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_empty_crate);
                    if (!value.isEmpty()) {
                        validateEmptyCrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_empty_crate.addTextChangedListener(new TextWatcher() {
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
                    validateEmptyCrate(value);
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
            case R.id.btn_ptl_new_picking_process_40_back:
                if(currentStep == 3){
                    step2(1);
                }else{
                    box.confirmBack(fm, con);
                }
                break;
            case R.id.btn_ptl_new_picking_process_40_next:
                if(currentStep == 1){
                    if(!UIFuncs.toUpperTrim(txt_picklist).isEmpty()){
                        getPicklistListData(UIFuncs.toUpperTrim(txt_picklist));
                    }else{
                        box.getBox("Invalid", "Please scan Floor Bin first");
                        txt_scan_bin.requestFocus();
                    }
                }else if(currentStep == 2){
                    if(UIFuncs.toUpperTrim(txt_scanned_empty_crate).isEmpty()){
                        box.getBox("Invalid", "Please scan Empty Crate");
                        txt_empty_crate.requestFocus();
                        break;
                    }
                    if(UIFuncs.toUpperTrim(txt_scanned_msa_bin).isEmpty()){
                        box.getBox("Invalid", "Please scan MSA BIN");
                        txt_scan_msa_bin.requestFocus();
                        break;
                    }
                    if(UIFuncs.toUpperTrim(txt_scanned_msa_crate).isEmpty()){
                        box.getBox("Invalid", "Please scan MSA Crate");
                        txt_scan_msa_crate.requestFocus();
                        break;
                    }
                    if(etDataMap == null || etDataMap.isEmpty()){
                        box.getBox("Invalid", "MSA Crate details not loaded. Please re-scan MSA Crate");
                        txt_scan_msa_crate.requestFocus();
                        break;
                    }
                    step3(1);
                }
                break;
            case R.id.btn_ptl_new_picking_process_40_save:
                saveScannedData();
                break;
        }
    }

    private void clear(){
        step3(0);
        step2(0);
        step1();
        currentStep = 1;
    }

    private void step1(){
        ll_screen_1.setVisibility(View.VISIBLE);
        ll_screen_2.setVisibility(View.GONE);
        ll_screen_3.setVisibility(View.GONE);
        btn_next.setVisibility(View.VISIBLE);
        btn_save.setVisibility(View.INVISIBLE);
        txt_scan_bin.setText("");
        txt_section.setText("");
        txt_floor.setText("");
        txt_picklist.setText("");
        UIFuncs.enableInput(con, txt_scan_bin);
    }

    private void step2(int mode){
        currentStep = 2;
        ll_screen_1.setVisibility(View.GONE);
        ll_screen_2.setVisibility(View.VISIBLE);
        ll_screen_3.setVisibility(View.GONE);
        btn_next.setVisibility(View.VISIBLE);
        btn_save.setVisibility(View.INVISIBLE);
        if(mode == 0){
            totalScanned = 0;
            totalBin = 0;
            etDataMap = new HashMap<>();
            eanDataMap = new HashMap<>();
            txt_scan_msa_bin.setText("");
            txt_scanned_msa_bin.setText("");
            txt_scan_msa_crate.setText("");
            txt_scanned_msa_crate.setText("");
            txt_picklistno.setText("");
            UIFuncs.disableInput(con, txt_scanned_bin);
            txt_empty_crate.requestFocus();
        }else{
            txt_picklistno.setText(UIFuncs.removeLeadingZeros(UIFuncs.toUpperTrim(txt_picklist)));
            txt_scanned_floor.setText(UIFuncs.toUpperTrim(txt_floor));
            txt_scanned_floor_bin.setText(UIFuncs.toUpperTrim(txt_scan_bin));
            txt_scanned_section.setText(UIFuncs.toUpperTrim(txt_section));
            txt_scan_msa_crate.requestFocus();
        }
        txt_scanned_bin.setText(totalScanned + " / " + totalBin);
        if(mode == 2){
            txt_scanned_msa_crate.setText("");
            txt_scanned_msa_bin.setText("");
            txt_scanned_msa_bin.requestFocus();
        }
    }

    private void step3(int mode){
        currentStep = 3;
        btn_next.setVisibility(View.INVISIBLE);
        btn_save.setVisibility(View.VISIBLE);
        ll_screen_1.setVisibility(View.GONE);
        ll_screen_2.setVisibility(View.GONE);
        ll_screen_3.setVisibility(View.VISIBLE);
        if(mode == 0){
            table_article_scan.removeAllViews();
            txt_picklistno_2.setText("");
            txt_scanned_empty_crate_2.setText("");
            txt_scanned_msa_bin_2.setText("");
            txt_scanned_msa_crate_2.setText("");
        }
        if(mode == 1){
            txt_picklistno_2.setText(UIFuncs.toUpperTrim(txt_picklistno));
            txt_scanned_empty_crate_2.setText(UIFuncs.toUpperTrim((txt_scanned_empty_crate)));
            txt_scanned_msa_bin_2.setText(UIFuncs.toUpperTrim((txt_scanned_msa_bin)));
            txt_scanned_msa_crate_2.setText(UIFuncs.toUpperTrim((txt_scanned_msa_crate)));
            generateHeaderRow();
            generateNonScannedArticleRows(null);
        }
        txt_scan_article.setText("");
        UIFuncs.enableInput(con, txt_scan_article);
    }

    //Step 1
    private void validateBin(String bin){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZPTL_GET_DATA_FROM_BIN_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZPTL_GET_DATA_FROM_BIN_RFC, REQUEST_BIN_DATA, args);
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
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            boolean firstRowSet = false;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    BinCrateData binCrateData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), BinCrateData.class);
                    if(binCrateData == null){
                        continue;
                    }
                    String keyBin = (binCrateData.getBin() == null) ? "" : binCrateData.getBin().trim();
                    String keyCrate = (binCrateData.getCrate() == null) ? "" : binCrateData.getCrate().trim();
                    if(keyBin.isEmpty() && keyCrate.isEmpty()){
                        continue;
                    }
                    binDataMap.put(keyBin + "-" + keyCrate, binCrateData);
                    if(!firstRowSet){
                        txt_floor.setText(binCrateData.getFloor() == null ? "" : binCrateData.getFloor());
                        txt_section.setText(binCrateData.getSection() == null ? "" : binCrateData.getSection());
                        txt_picklist.setText(binCrateData.getPicklist() == null ? "" : binCrateData.getPicklist());
                        firstRowSet = true;
                    }
                }
            }
            if(!binDataMap.isEmpty()){
                btn_next.requestFocus();
            }else{
                box.getBox("Empty", "Picklist Data is Empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void getPicklistListData(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZGRT_PICK_GET_PICK_DATA_V4);
            args.put("IM_USER", USER);
            args.put("IM_TANUM", value);
            showProcessingAndSubmit(Vars.ZGRT_PICK_GET_PICK_DATA_V4, REQUEST_GET_PICKLIST_DATA, args);
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
            picklistDataMap = new LinkedHashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_PICKDATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if (totalEtRecords > 0) {
                for (int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++) {
                    ETPickData picklistData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), ETPickData.class);
                    if(!(picklistData.getLgbin() == null || picklistData.getLgbin().isEmpty()) && !(picklistData.getLgcrate() == null || picklistData.getLgcrate().isEmpty())){
                        picklistDataMap.put(picklistData.getLgbin() + "-" + picklistData.getLgcrate(), picklistData);
                    }
                }
            }
            if (!picklistDataMap.isEmpty()) {
                totalBin = picklistDataMap.size();
                step2(1);
                populateBinCrateTable();
            } else {
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
        for (Map.Entry<String, ETPickData> picklistDataEntry :picklistDataMap.entrySet()) {
            ETPickData data = picklistDataEntry.getValue();
            TextView tvSno = new TextView(getContext());
            tvSno.setText(rowNum+"");
            tvSno.setTextSize(textSize);
            tvSno.setPadding(5,2,0,2);
            tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvBin = new TextView(getContext());
            tvBin.setText(data.getLgbin());
            tvBin.setTextSize(textSize);
            tvBin.setPadding(5,2,0,2);
            tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvCrate = new TextView(getContext());
            tvCrate.setText(data.getLgcrate());
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

    //Step 2
    private void validateEmptyCrate(String crate){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_CRATE_VALIDATE_V4);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_PTL_CRATE_VALIDATE_V4, REQUEST_VALIDATE_EMPTY_CRATE, args);
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
    private void validateMsaBin(String bin){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_BIN_VALIDATE_V3);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZWM_PTL_BIN_VALIDATE_V3, REQUEST_VALIDATE_MSA_BIN, args);
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
            args.put("bapiname", Vars.ZWM_PTL_MSA_CRATE_VALIDATE_V4);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_scanned_msa_bin));
            args.put("IM_MSA_CRATE", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_MSA_CRATE_VALIDATE_V4, REQUEST_VALIDATE_MSA_CRATE, args);
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
    private void clearFieldsForNextScan(JSONObject responsebody){
        try {
            etDataMap = new LinkedHashMap<>();
            eanDataMap = new LinkedHashMap<>();
            crateArticle = new HashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            JSONArray ET_EAN_DATA_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");

            int totalEtRecords = ET_DATA_ARRAY.length();
            int totalEanRecords = ET_EAN_DATA_ARRAY.length();

            if (totalEtRecords > 0) {
                for (int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++) {
                    PicklistData picklistData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), PicklistData.class);
                    if(picklistData == null || picklistData.getArticle() == null || picklistData.getArticle().trim().isEmpty()){
                        continue;
                    }
                    etDataMap.put(picklistData.getMsaCrate() + "-" + picklistData.getBin() + "-" + UIFuncs.removeLeadingZeros(picklistData.getArticle()), picklistData);
                }
            }
            if (totalEanRecords > 0) {
                for (int recordIndex = 0; recordIndex < totalEanRecords; recordIndex++) {
                    HUEANData eanData = new Gson().fromJson(ET_EAN_DATA_ARRAY.getJSONObject(recordIndex).toString(), HUEANData.class);
                    if(eanData == null || eanData.getLgean11() == null || eanData.getLgean11().trim().isEmpty()){
                        continue;
                    }
                    eanDataMap.put(eanData.getLgean11(), eanData);
                }
            }
            if (!etDataMap.isEmpty()) {
                txt_scanned_msa_crate.setText(UIFuncs.toUpperTrim((txt_scan_msa_crate)));
                txt_scan_msa_crate.setText("");
                totalScanned = totalScanned + 1;
                step3(1);
            } else {
                box.getBox("Empty", "Picklist Data is Empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

    }

    //Step 3
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
        TextView headerSC = new TextView(getContext());
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

        headerSC.setGravity(Gravity.CENTER);
        headerSC.setPadding(5,5,0,5);
        headerSC.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSC.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSC.setText(" SC ");

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
        tr.addView(headerSC);
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
            String matnr = UIFuncs.removeLeadingZeros(eanData.getLgmatnr());
            if(allowArticleForCrate(matnr)){
                key = UIFuncs.toUpperTrim(txt_scanned_msa_crate_2) + "-" + UIFuncs.toUpperTrim(txt_scanned_msa_bin_2) + "-" + matnr;
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
                    crateArticle.put(UIFuncs.toUpperTrim(txt_scanned_empty_crate_2), matnr);
                    populateArticleTableRow(etData, true);
                }else{
                    box.getBox("Invalid", String.format("Scanned barcode is not associated with %s and %s",UIFuncs.toUpperTrim(txt_scanned_msa_crate_2), UIFuncs.toUpperTrim(txt_scanned_msa_bin_2)));
                }
            }
        }else{
            box.getBox("Invalid", "Scanned barcode is not available");
        }
        generateNonScannedArticleRows(key);
        txt_scan_article.setText("");
    }
    private boolean allowArticleForCrate(String matnr){
        if(crateArticle.containsKey(UIFuncs.toUpperTrim(txt_scanned_empty_crate_2))){
            if(!matnr.equalsIgnoreCase(crateArticle.get(UIFuncs.toUpperTrim(txt_scanned_empty_crate_2)))){
                box.getBox("Invalid", String.format("Crate %s already has article %s. Mix articles not allowed in same crate",UIFuncs.toUpperTrim(txt_scanned_empty_crate_2), matnr));
                return false;
            }
        }
        return true;
    }
    private void generateNonScannedArticleRows(String key){
        for (Map.Entry<String, PicklistData> etDataEntry :etDataMap.entrySet()) {
            PicklistData etData = etDataEntry.getValue();
            String dataKey = etDataEntry.getKey();
            String keyPrefix = UIFuncs.toUpperTrim(txt_scanned_msa_crate_2) + "-" + UIFuncs.toUpperTrim(txt_scanned_msa_bin_2) + "-";
            if(!dataKey.equals(key) && dataKey.startsWith(keyPrefix)){
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

        CheckBox cbSelect = new CheckBox(getContext());
        cbSelect.setGravity(Gravity.CENTER);
        cbSelect.setPadding(5, 2, 0, 2);
        cbSelect.setChecked(data.isConfirmShortScan());
        cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            data.setConfirmShortScan(true);
        });
        cbSelect.setBackground(getResources().getDrawable(R.drawable.table_cell_border));
        cbSelect.setHeight(Util.dpToPx(con, 30));

        tr = new TableRow(getContext());
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(tvSno);
        tr.addView(cbSelect);
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

                            if(data.getSqty() > 0 && data.getSqty() < rQty){
                                data.setShortScan(true);
                            }
                            if(data.getSqty() > 0 && data.getSqty() == rQty){
                                data.setShortScan(false);
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
                int rQty = Integer.parseInt(Util.convertToDoubleString(data.getQuantity()));
                if(data.getSqty() > 0 && data.getSqty() < rQty){
                    data.setShortScan(true);
                }
                if(data.getSqty() > 0 && data.getSqty() == rQty){
                    data.setShortScan(false);
                }
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
            int rQty = Integer.parseInt(Util.convertToDoubleString(data.getQuantity()));
            if(data.getSqty() > 0 && data.getSqty() < rQty){
                data.setShortScan(true);
            }
            if(data.getSqty() > 0 && data.getSqty() == rQty){
                data.setShortScan(false);
            }
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
                    if(etData.isShortScan() && !etData.isConfirmShortScan()){
                        box.getBox("Confirm", "Please check the checkboxes to confirm save with short count");
                        return null;
                    }
                    ScanData data = new ScanData().newInstance(etData, UIFuncs.toUpperTrim(txt_scanned_empty_crate_2), null);
                    data.setSclose(etData.isShortScan() ? "X":"");
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
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_BIN_DATA) {
                                            txt_scan_bin.setText("");
                                            txt_scan_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_EMPTY_CRATE) {
                                            txt_empty_crate.setText("");
                                            txt_empty_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_BIN) {
                                            txt_scan_msa_bin.setText("");
                                            txt_scan_msa_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_CRATE) {
                                            txt_scan_msa_crate.setText("");
                                            txt_scan_msa_crate.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_BIN_DATA) {
                                            setBinData(responsebody);
                                        }
                                        else if (request == REQUEST_GET_PICKLIST_DATA) {
                                            setPicklistListData(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_EMPTY_CRATE) {
                                            txt_scanned_empty_crate.setText(UIFuncs.toUpperTrim(txt_empty_crate));
                                            txt_empty_crate.setText("");
                                            txt_scan_msa_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_BIN) {
                                            txt_scanned_msa_bin.setText(UIFuncs.toUpperTrim(txt_scan_msa_bin));
                                            txt_scan_msa_bin.setText("");
                                            txt_scan_msa_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_CRATE) {
                                            clearFieldsForNextScan(responsebody);
                                        }else if (request == REQUEST_SAVE) {
                                            box.getBox("Success", responsebody.get("EX_TANUM").toString(), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    step2(1);
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