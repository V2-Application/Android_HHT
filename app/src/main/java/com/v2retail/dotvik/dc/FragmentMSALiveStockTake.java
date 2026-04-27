package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.provider.Contacts;
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
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.livestock.LiveArticleQty;
import com.v2retail.dotvik.modal.livestock.LiveScanData;
import com.v2retail.dotvik.modal.livestock.LiveStockBinCrate;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FragmentMSALiveStockTake extends Fragment implements View.OnClickListener {

    private static final int REQUEST_GET_STOCK_ID = 1500;
    private static final int REQUEST_VALIDATE_STOCK_ID = 1501;
    private static final int REQUEST_LIVE_SCAN = 1502;
    private static final int REQUEST_SAVE = 1503;

    private static final String TAG = FragmentMSALiveStockTake.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_next, btn_submit;
    TextView tv_plant, tv_stock_take_id;
    EditText txt_tq, txt_sq, txt_pq;
    EditText txt_cur_binno, txt_cur_crate, txt_cur_article, txt_cur_sqty;
    EditText txt_scan_binno, txt_scan_crate, txt_scan_article, txt_scan_sqty;

    LinearLayout llStockTake, llNextScreen;
    TableLayout tableItems;

    boolean spinnerTouched = false;
    Spinner dd_stock_id_list;
    List<String> stockIds = new ArrayList<String>();
    ArrayAdapter<String> stockAdapter;

    Map<String, LiveStockBinCrate> liveStockList = new LinkedHashMap<>();
    Map<String, LiveScanData> scanData = new HashMap<>();
    LiveStockBinCrate currentData = null;

    int totalScanned = 0;

    public FragmentMSALiveStockTake() {
        // Required empty public constructor
    }

    public static FragmentMSALiveStockTake newInstance(String param1, String param2) {
        return new FragmentMSALiveStockTake();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("MSA Live Stock Take");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_msa_live_stock_take, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        FragmentActivity activity = getActivity();

        tv_stock_take_id = rootView.findViewById(R.id.tv_msa_live_stock_take_stock_id);
        tv_plant = rootView.findViewById(R.id.tv_msa_live_stock_take_plant);
        
        txt_tq = rootView.findViewById(R.id.txt_msa_live_stock_take_tq);
        txt_sq = rootView.findViewById(R.id.txt_msa_live_stock_take_sq);
        txt_pq = rootView.findViewById(R.id.txt_msa_live_stock_take_rq);
        
        txt_cur_binno = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_bin);
        txt_cur_crate = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_crate);
        txt_cur_article = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_article);
        txt_cur_sqty = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_sqty);
        
        txt_scan_binno = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_bin);
        txt_scan_crate = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_crate);
        txt_scan_article = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_article);
        txt_scan_sqty = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_sqty);
        
        llNextScreen = rootView.findViewById(R.id.ll_msa_live_stock_take_next_screen);
        llStockTake = rootView.findViewById(R.id.ll_msa_live_stock_take_stock_take);

        tableItems = rootView.findViewById(R.id.table_msa_live_stock_take_items);

        dd_stock_id_list = rootView.findViewById(R.id.dd_msa_live_stock_take_stock_take_id);
        dd_stock_id_list.setSelection(0);
        stockAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, stockIds);
        stockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_stock_id_list.setAdapter(stockAdapter);
        dd_stock_id_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});


        btn_back = rootView.findViewById(R.id.btn_msa_live_stock_take_back);
        btn_next = rootView.findViewById(R.id.btn_msa_live_stock_take_next);
        btn_submit = rootView.findViewById(R.id.btn_msa_live_stock_take_submit);

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_submit.setOnClickListener(this);

        clear(true);
        addInputEvents();
        return rootView;
    }


    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_msa_live_stock_take_back:
                    box.getBox("Alert", "Do you want to go back. Any unsaved progress will be lost", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clear(true);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                break;
            case R.id.btn_msa_live_stock_take_next:
                if(dd_stock_id_list.getSelectedItem() != null && !dd_stock_id_list.getSelectedItem().toString().isEmpty()  && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select")) {
                    validateStockTakeId(dd_stock_id_list.getSelectedItem().toString());
                }
                break;
            case R.id.btn_msa_live_stock_take_submit:
                if(!scanData.isEmpty()){
                    saveData();
                }else{
                    box.getBox("No Data", "Nothing to save, please scan some data");
                }
                break;
        }
    }
    private void addInputEvents() {
        dd_stock_id_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinnerTouched) {
                    btn_next.setVisibility(View.GONE);
                    if(dd_stock_id_list.getSelectedItem() != null && !dd_stock_id_list.getSelectedItem().toString().isEmpty()  && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select")) {
                        btn_next.setVisibility(View.VISIBLE);
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        txt_scan_binno.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_binno);
                    if (!value.isEmpty()) {
                        validateBinNo(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_binno.addTextChangedListener(new TextWatcher() {
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
                    validateBinNo(value);
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
                        txt_cur_crate.setText(value);
                        UIFuncs.enableInput(con, txt_scan_article);
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
                    txt_cur_crate.setText(value);
                    UIFuncs.enableInput(con, txt_scan_article);
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
                        validateArticle(value);
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
                    validateArticle(value);
                }
            }
        });
    }

    private void clear(boolean clearAll) {
        scanData = new HashMap<>();
        liveStockList = new HashMap<>();
        totalScanned = 0;
        step2();
        if(clearAll){
            llNextScreen.setVisibility(View.GONE);
            llStockTake.setVisibility(View.VISIBLE);
            btn_next.setVisibility(View.GONE);
            btn_submit.setVisibility(View.GONE);
            totalScanned = 0;
            UIFuncs.enableInput(con, dd_stock_id_list);
            getStockIDs();
        }else{
            validateStockTakeId(dd_stock_id_list.getSelectedItem().toString());
        }
    }

    private void step2(){
        tv_stock_take_id.setText("");
        tv_plant.setText(WERKS);
        txt_tq.setText("");
        txt_sq.setText("");
        txt_pq.setText("");
        txt_cur_crate.setText("");
        txt_cur_binno.setText("");
        txt_cur_article.setText("");
        txt_cur_sqty.setText("");
        txt_scan_binno.setText("");
        txt_scan_crate.setText("");
        txt_scan_article.setText("");
        txt_scan_sqty.setText("");
        tableItems.removeAllViews();
        llNextScreen.setVisibility(View.VISIBLE);
        llStockTake.setVisibility(View.GONE);
        btn_next.setVisibility(View.GONE);
        btn_submit.setVisibility(View.VISIBLE);
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno);
    }

    private void getStockIDs(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_STOCK_TAKE_ID);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.ZWM_GET_STOCK_TAKE_ID, REQUEST_GET_STOCK_ID, args);
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

    private void validateStockTakeId(String stockTakeId){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_STOCK_BIN);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_ST_ID", stockTakeId);
            showProcessingAndSubmit(Vars.ZWM_GET_STOCK_BIN, REQUEST_VALIDATE_STOCK_ID, args);
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

    private void populateStockIDs(JSONObject responsebody){
        try {
            stockIds.clear();
            stockIds.add("Select");
            JSONArray IT_DATA_ARRAY = responsebody.getJSONArray("IT_DATA");
            int length = IT_DATA_ARRAY.length();
            for(int i = 1; i < length; i++){
                stockIds.add(IT_DATA_ARRAY.getJSONObject(i).getString("ST_TAKE_ID"));
            }
            if(stockIds.size() > 0){
                ((BaseAdapter) dd_stock_id_list.getAdapter()).notifyDataSetChanged();
                dd_stock_id_list.setEnabled(true);
                dd_stock_id_list.invalidate();
                dd_stock_id_list.setSelection(0);
                dd_stock_id_list.requestFocus();
            }else{
                AlertBox box = new AlertBox(getContext());
                box.getBox("No Data", "Not Stock Take IDs Found.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear(true);
                    }
                });
            }
        }catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void setData(JSONObject responsebody){
        try {
            liveStockList = new LinkedHashMap<>();
            scanData = new HashMap<>();
            totalScanned = 0;
            JSONArray IT_DATA_ARRAY = responsebody.getJSONArray("IT_DATA");
            int length = IT_DATA_ARRAY.length();
            for(int i = 1; i < length; i++){
                LiveStockBinCrate data = new Gson().fromJson(IT_DATA_ARRAY.getJSONObject(i).toString(), LiveStockBinCrate.class);
                liveStockList.put(data.getBin(), data);
            }
            if(liveStockList.size() > 0){
                step2();
                tv_stock_take_id.setText(dd_stock_id_list.getSelectedItem().toString());
                txt_tq.setText(liveStockList.size()+"");
                txt_sq.setText("0");
                txt_tq.setText(UIFuncs.toUpperTrim(txt_tq));
                populateTableData();
            }else{
                AlertBox box = new AlertBox(getContext());
                box.getBox("No Data", "Picklist is empty.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear(true);
                    }
                });
            }
        }catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void validateBinNo(String binno){
        boolean binFound = false;
        boolean withCarate = false;
        for (Map.Entry<String, LiveStockBinCrate> dataEntry: liveStockList.entrySet()) {
            LiveStockBinCrate data = dataEntry.getValue();
            if(data.getBin().equalsIgnoreCase(binno) && !data.isPicked()){
                data.setPicked(true);
                currentData = LiveStockBinCrate.newInstance(data);
                binFound = true;
                if(data.getCrate() != null && !data.getCrate().isEmpty()){
                    withCarate = true;
                }
                totalScanned = totalScanned + 1;
                break;
            }
        }
        if(!binFound){
            txt_scan_binno.setText("");
            UIFuncs.errorSound(con);
            box.getBox("Invalid Bin", "Invalid BIN, please check below table for allowed BINs");
            txt_scan_binno.requestFocus();
        }else{
            //Here we can check for with crate and without crate logic if needed
            setLastScanedItem(withCarate);
        }
    }
    private void validateArticle(String article){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_LIVE_STOCK_SCANNING);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_STOCK_TAKE_ID", tv_stock_take_id.getText());
            args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_scan_crate));
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_cur_binno));
            args.put("IM_BARCODE", article);
            showProcessingAndSubmit(Vars.ZWM_LIVE_STOCK_SCANNING, REQUEST_LIVE_SCAN, args);
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

    private void updateScanStats(JSONObject responsebody){
        try {
            JSONObject EX_DATA = responsebody.getJSONObject("EX_DATA");

            LiveArticleQty data = new Gson().fromJson(EX_DATA.toString(), LiveArticleQty.class);

            LiveScanData existing;
            if(scanData.containsKey(data.getArticle())){
                existing = scanData.get(data.getArticle());
            }else{
                existing = LiveScanData.copyProperties(currentData);
                existing.setMaterial(data.getArticle());
                existing.setCrate(UIFuncs.toUpperTrim(txt_scan_crate));
                scanData.put(data.getArticle(), existing);
            }
            LiveScanData.updateScanQty(existing, data.getQty());

            double scanQty = Util.convertStringToDouble(txt_cur_sqty.getText().toString());
            double dataQty = Util.convertStringToDouble(data.getQty());
            scanQty = scanQty + dataQty;

            txt_cur_article.setText(UIFuncs.removeLeadingZeros(data.getArticle()));
            txt_cur_sqty.setText(Util.formatDouble(scanQty));
            txt_scan_sqty.setText(Util.formatDouble(dataQty));
        }catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
        txt_scan_article.setText("");
        txt_scan_article.requestFocus();
    }

    private void setLastScanedItem(boolean withCarate){
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_binno);

        txt_cur_binno.setText(currentData.getBin());
        txt_cur_crate.setText(currentData.getCrate());
        txt_cur_article.setText("");
        txt_cur_sqty.setText("0");

        txt_scan_crate.setText("");
        txt_scan_article.setText("");
        txt_scan_sqty.setText("0");

        if(!withCarate){
            UIFuncs.enableInput(con, txt_scan_article);
        }else{
            UIFuncs.enableInput(con, txt_scan_crate);
        }

        populateTableData();

        txt_tq.setText(liveStockList.size() + "");
        txt_sq.setText(totalScanned + "");
        txt_pq.setText((liveStockList.size() - totalScanned) + "");
    }

    private void populateTableData(){
        tableItems.removeAllViews();
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 16;
        textSize = 14;

        TextView headerBin = new TextView(getContext());
        TextView headerHuNo = new TextView(getContext());
        TextView headerExHuNo = new TextView(getContext());

        headerBin.setLayoutParams(new TableRow.LayoutParams(
                350,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        headerBin.setGravity(Gravity.CENTER);
        headerBin.setPadding(0,5,0,5);
        headerBin.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerBin.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerBin.setText("Bin");

        headerHuNo.setGravity(Gravity.CENTER);
        headerHuNo.setPadding(0,5,0,5);
        headerHuNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerHuNo.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerHuNo.setText("Crate");

        headerExHuNo.setGravity(Gravity.CENTER);
        headerExHuNo.setPadding(0,5,0,5);
        headerExHuNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerExHuNo.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerExHuNo.setText("Plant");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerBin);
        tr.addView(headerHuNo);
        tr.addView(headerExHuNo);
        tableItems.addView(tr, trParams);

        //Create Data Rows in Table
        int rowNum = 1;
        for (Map.Entry<String, LiveStockBinCrate> stockEntry : liveStockList.entrySet()) {
            LiveStockBinCrate data = stockEntry.getValue();
            if(!data.isPicked()){
                TextView tvBin = new TextView(getContext());
                tvBin.setText(data.getBin());
                tvBin.setTextSize(textSize);
                tvBin.setPadding(5,2,0,2);
                tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvHu = new TextView(getContext());
                tvHu.setText(data.getCrate());
                tvHu.setTextSize(textSize);
                tvHu.setPadding(5,2,0,2);
                tvHu.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvExHu = new TextView(getContext());
                tvExHu.setText(data.getPlant());
                tvExHu.setTextSize(textSize);
                tvExHu.setPadding(5,2,0,2);
                tvExHu.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                tr = new TableRow(getContext());
                tr.setId(rowNum);
                tr.setPadding(0,0,0,0);
                tr.setLayoutParams(trParams);
                tr.addView(tvBin);
                tr.addView(tvHu);
                tr.addView(tvExHu);
                tr.setTag(data);
                tableItems.addView(tr, trParams);
                rowNum++;
            }
        }
    }

    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, LiveScanData> dataEntry : scanData.entrySet()) {
                LiveScanData data = dataEntry.getValue();
                String scanDataJsonString = new Gson().toJson(data);
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some articles");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    private void saveData() {
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave != null) {
            try {
                args.put("bapiname", Vars.ZWM_STK_ADJ_MSA_BIN);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("IM_STOCK_TAKE_ID", USER);
                args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_cur_crate));
                args.put("IM_BIN", UIFuncs.toUpperTrim(txt_cur_binno));
                args.put("IM_DESKTOP", UIFuncs.toUpperTrim(txt_cur_crate).isEmpty() ? "" : "X");
                args.put("ET_SAVE", dataToSave);
                showProcessingAndSubmit(Vars.ZWM_STK_ADJ_MSA_BIN, REQUEST_SAVE, args);
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

    private void afterSave(){
        scanData = new HashMap<>();
        currentData = null;

        txt_cur_crate.setText("");
        txt_cur_binno.setText("");
        txt_cur_article.setText("");
        txt_cur_sqty.setText("0");

        txt_scan_binno.setText("");
        txt_scan_crate.setText("");
        txt_scan_article.setText("");
        txt_scan_sqty.setText("0");

        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno);
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
                                    } else {
                                        if (request == REQUEST_GET_STOCK_ID) {
                                            populateStockIDs(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_VALIDATE_STOCK_ID) {
                                            setData(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_LIVE_SCAN) {
                                            updateScanStats(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    afterSave();
                                                }
                                            });
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