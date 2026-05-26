package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.picking.ETEan;
import com.v2retail.dotvik.modal.picking.ETFinal;
import com.v2retail.dotvik.modal.picking.HUSave;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PickingWithConsolidationFragment extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_PICKLIST = 312;
    private static final int REQUEST_VALIDATE_SOURCE_BIN = 313;
    private static final int REQUEST_VALIDATE_SCAN_BIN = 314;
    private static final int REQUEST_PICK_BIN_SAVE= 315;

    private static final String TAG = PickingWithConsolidationFragment.class.getName();

    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    View rootView;

    EditText text_store;
    EditText text_picklistno;
    EditText text_sourcebin;
    EditText text_scanbin;
    EditText text_barcode;
    EditText text_article_no;
    EditText text_destination;
    EditText text_binqty;
    EditText text_total_scanned_qty;

    Button button_save;
    Button button_refresh;

    LinearLayout formLayout;

    private Map<String,ETFinal> ET_FINAL_DATA =  new HashMap<>();
    private JSONArray ET_LQUA_DATA;
    private Map<String,ETEan> ET_EAN_DATA =  new HashMap<>();
    private Map<String, HUSave> ET_HU_SAVE =  new HashMap<>();
    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Picking With Consolidation");
    }

    public PickingWithConsolidationFragment() {

    }

    public static PickingWithConsolidationFragment newInstance(String param1, String param2) {
        PickingWithConsolidationFragment fragment = new PickingWithConsolidationFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_picking_with_consolidation, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        formLayout = rootView.findViewById(R.id.text_pickwithcon_form);

        text_store = rootView.findViewById(R.id.text_pickwithcon_store);
        text_picklistno = rootView.findViewById(R.id.text_pickwithcon_picklist);
        text_sourcebin = rootView.findViewById(R.id.text_pickwithcon_source_bin);
        text_scanbin = rootView.findViewById(R.id.text_pickwithcon_scan_bin);
        text_barcode = rootView.findViewById(R.id.text_pickwithcon_barcode);
        text_article_no = rootView.findViewById(R.id.text_pickwithcon_article_no);
        text_destination = rootView.findViewById(R.id.text_pickwithcon_destination);
        text_binqty = rootView.findViewById(R.id.text_pickwithcon_bin_qty);
        text_total_scanned_qty = rootView.findViewById(R.id.text_pickwithcon_tot_scan_qty);

        text_picklistno.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  picklistNo = text_picklistno.getText().toString().toUpperCase();
                    text_picklistno.setText(picklistNo);
                    if(picklistNo.length()>0) {
                        text_picklistno.selectAll();
                        validatePicklistNo(picklistNo);
                        return true;
                    }
                }
                return false;
            }
        });
        text_picklistno.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String picklistNo = s.toString().toUpperCase();
                if(picklistNo.length()>0 && scannerReading) {
                    text_picklistno.setText(picklistNo);
                    text_picklistno.selectAll();
                    validatePicklistNo(picklistNo);
                }
            }
        });

        text_sourcebin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String sourceBin = text_sourcebin.getText().toString().toUpperCase();
                    text_sourcebin.setText(sourceBin);
                    if(sourceBin.length()>0) {
                        text_sourcebin.selectAll();
                        validateSourceBin(sourceBin);
                        return true;
                    }
                }
                return false;
            }
        });
        text_sourcebin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String sourceBin = s.toString().toUpperCase();
                if(sourceBin.length()>0 && scannerReading) {
                    text_sourcebin.setText(sourceBin);
                    text_sourcebin.selectAll();
                    validateSourceBin(sourceBin);
                }
            }
        });

        text_scanbin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String scanBin = text_scanbin.getText().toString().toUpperCase();
                    text_scanbin.setText(scanBin);
                    if(scanBin.length()>0) {
                        text_scanbin.selectAll();
                        validateScanBin(scanBin);
                        return true;
                    }
                }
                return false;
            }
        });
        text_scanbin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String scanBin = s.toString().toUpperCase();
                if(scanBin.length()>0 && scannerReading) {
                    text_scanbin.setText(scanBin);
                    text_scanbin.selectAll();
                    validateScanBin(scanBin);
                }
            }
        });

        text_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String barcode = text_barcode.getText().toString().toUpperCase();
                    text_barcode.setText(barcode);
                    if(barcode.length()>0) {
                        text_barcode.selectAll();
                        validateBarcode(barcode);
                        return true;
                    }
                }
                return false;
            }
        });
        text_barcode.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String barcode = s.toString().toUpperCase();
                if(barcode.length()>0 && scannerReading) {
                    text_barcode.setText(barcode);
                    text_barcode.selectAll();
                    validateBarcode(barcode);
                }
            }
        });

        button_save = rootView.findViewById(R.id.button_pickwithcon_save);
        button_refresh = rootView.findViewById(R.id.button_pickwithcon_reset);
        button_save.setOnClickListener(this);
        text_store.setText(WERKS);
        text_total_scanned_qty.setText("0");
        text_picklistno.requestFocus();
        return rootView;
    }
    private void resetData(){
        text_picklistno.setText("");

        text_sourcebin.setText("");
        text_sourcebin.setEnabled(false);

        text_scanbin.setText("");
        text_scanbin.setEnabled(false);

        text_barcode.setText("");
        text_barcode.setEnabled(false);

        text_article_no.setText("");
        text_destination.setText("");
        text_binqty.setText("");
        text_total_scanned_qty.setText("0");

        button_refresh.setVisibility(View.INVISIBLE);
        button_save.setVisibility(View.INVISIBLE);
        formLayout.setVisibility(View.INVISIBLE);
        ET_FINAL_DATA = new HashMap<>();
        ET_LQUA_DATA = null;
        ET_EAN_DATA = new HashMap<>();
        ET_HU_SAVE = new HashMap<>();
        text_picklistno.requestFocus();
    }
    @Override
    public void onClick(View v) {
        CommonUtils.hideKeyboard(getActivity());
        switch (v.getId()){
            case R.id.button_pickwithcon_save:
                saveData();
                break;
            case R.id.button_pickwithcon_reset:
                resetData();
                break;
        }
    }

    private void validatePicklistNo(String picklistno){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.PICKING_WITH_CONS_VALIDATE_PICKLIST);
            args.put("IM_WERKS", WERKS);
            args.put("IM_PICNR", picklistno.toUpperCase());
            showProcessingAndSubmit(Vars.PICKING_WITH_CONS_VALIDATE_PICKLIST,REQUEST_VALIDATE_PICKLIST,args);
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
    private void validateSourceBin(String sourcebin){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.PICKING_WITH_CONS_VALIDATE_SOURCE_BIN);
            args.put("IM_WERKS", WERKS);
            args.put("IM_PICNR", text_picklistno.getText().toString().toUpperCase());
            args.put("IM_BIN", sourcebin.toUpperCase());
            ET_FINAL_DATA = new HashMap<>();
            ET_LQUA_DATA = null;
            ET_EAN_DATA = new HashMap<>();
            ET_HU_SAVE = new HashMap<>();
            showProcessingAndSubmit(Vars.PICKING_WITH_CONS_VALIDATE_SOURCE_BIN,REQUEST_VALIDATE_SOURCE_BIN,args);
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
    private void validateScanBin(String scanbin){
        if(scanbin.toUpperCase().equals(text_sourcebin.getText().toString().toUpperCase())){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid", "Scan Bin cannot be same as Source Bin");
            text_scanbin.setText("");
            text_scanbin.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.PICKING_WITH_CONS_VALIDATE_SCAN_BIN);
            args.put("IM_SITE", WERKS);
            args.put("IM_LGPLA", scanbin.toUpperCase());
            args.put("IM_BIN", text_sourcebin.getText().toString().toUpperCase());
            args.put("IM_PICNR", text_picklistno.getText().toString().toUpperCase());

            showProcessingAndSubmit(Vars.PICKING_WITH_CONS_VALIDATE_SCAN_BIN,REQUEST_VALIDATE_SCAN_BIN,args);
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
    private void validateBarcode(String barcode){
        if(text_scanbin.getText().toString().trim().length() == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid", "First Scan Bin");
            text_barcode.setText("");
            text_scanbin.requestFocus();
            return;
        }
        String matnr = "";
        float eanPackQty = 0;
        ETEan eanData = ET_EAN_DATA.get(barcode);
        if(eanData != null){

            matnr = eanData.getLgmatnr();
            eanPackQty = eanData.getLgumrez();
            text_article_no.setText(matnr);

            float pickRem = 0;
            float scanQty = 0;
            float huQty = 0;
            float binRem = 0;
            float binQty = 0;
            boolean isAdjusted = false;

            ETFinal finalData = ET_FINAL_DATA.get(matnr);
            if(finalData != null){
                scanQty = finalData.getLgscanqty();
                huQty = finalData.getLghuqty();
                pickRem = finalData.getLgrempick();
                binRem = finalData.getLgrembin();

                if(huQty - scanQty == 0){
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Not Found", "You have already scanned maximum allowed quantity for article "+finalData.getLgmatnr());
                    text_barcode.setText("");
                    text_barcode.requestFocus();
                    return;
                }

                if(pickRem >= eanPackQty){
                    pickRem = pickRem - eanPackQty;
                    text_destination.setText("0008");
                    finalData.setLgrempick(pickRem);
                    isAdjusted = true;
                }
                else if(binRem >= eanPackQty){
                    binRem = binRem - eanPackQty;
                    text_destination.setText(text_scanbin.getText().toString().toUpperCase());
                    finalData.setLgrembin(binRem);
                    isAdjusted = true;
                }

                if(!isAdjusted){
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Invalid", "Quantity of this EAN cannot be adjusted in either PICKLIST or BIN.\r\n -->Remaining PICKLIST QTY = " + pickRem + " \r\n -->Remaining BIN QTY = " + binRem);
                    text_barcode.setText("");
                    text_barcode.requestFocus();
                    return;
                }

                scanQty = scanQty + eanPackQty;
                finalData.setLqreq008(huQty - scanQty);
                finalData.setLgscanqty(scanQty);

                ET_FINAL_DATA.put(matnr,finalData);

                float totScanQty = text_total_scanned_qty.getText().toString().length() == 0 ? 0 : Integer.parseInt(text_total_scanned_qty.getText().toString());
                totScanQty = totScanQty + eanPackQty;
                text_total_scanned_qty.setText(totScanQty+"");

                String destination = text_destination.getText().toString().toUpperCase().trim();
                HUSave existingSave = ET_HU_SAVE.get(matnr+"-"+destination);
                scanQty = 0;
                if(existingSave != null){
                    scanQty = Integer.parseInt(existingSave.getLgscanqty());
                }else{
                    existingSave = new HUSave();
                }

                existingSave.setLghu(finalData.getLghuno());
                existingSave.setLgitemno("000000");
                existingSave.setLgarticle(matnr);
                existingSave.setLgpostingdate("00000000");
                existingSave.setLgplant(WERKS);
                existingSave.setLgstorageloc(destination.equals("0008") ? "0008" : "");
                existingSave.setLgscanqty((scanQty + eanPackQty)+"");
                existingSave.setLgremqty("0");
                existingSave.setLgbin(destination.equals("0008") ? "" : destination);
                existingSave.setLgean11(text_barcode.getText().toString().trim().toUpperCase());
                ET_HU_SAVE.put(matnr+"-"+destination,existingSave);
                text_barcode.setText("");

            }else{
                AlertBox box = new AlertBox(getContext());
                box.getBox("Not Found", "Article not found in PICKLIST RECORDS");
                text_barcode.setText("");
                text_barcode.requestFocus();
                return;
            }
        }else{
            AlertBox box = new AlertBox(getContext());
            box.getBox("Not Found", "Invalid Barcode");
            text_barcode.setText("");
            text_barcode.requestFocus();
            return;
        }
    }
    private void saveData(){
        try {
            JSONObject args = new JSONObject();
            JSONArray arrFinalData = new JSONArray();
            JSONArray arrSaveData = new JSONArray();

            for (Map.Entry<String,ETFinal> finalData : ET_FINAL_DATA.entrySet()) {
                ETFinal etFinal = finalData.getValue();
                String Final_Data_JsonString = new Gson().toJson(etFinal);
                JSONObject finalDataJson = new JSONObject(Final_Data_JsonString);
                arrFinalData.put(finalDataJson);
            }

            for (Map.Entry<String,HUSave> saveData : ET_HU_SAVE.entrySet()) {
                HUSave etSave = saveData.getValue();
                String Save_Data_JsonString = new Gson().toJson(etSave);
                JSONObject saveDataJson = new JSONObject(Save_Data_JsonString);
                arrSaveData.put(saveDataJson);
            }

            if(arrSaveData.length() == 0){
                box = new AlertBox(getContext());
                box.getBox("Invalid", "Nothing to save. 0 barcode scanned. Scanned Qty. should be greater than 0");
                return;
            }

            args.put("bapiname", Vars.PICKING_WITH_CONS_SAVE_PICK_DATA);
            args.put("IM_SITE", WERKS);
            args.put("IM_PICNR", text_picklistno.getText().toString().trim());
            args.put("IM_BIN", text_sourcebin.getText().toString().toUpperCase().trim());
            args.put("IM_COMPLETE_FLAG", "X");
            args.put("IMT_LQUA", ET_LQUA_DATA);
            args.put("IMT_HU_STATUS", arrFinalData);
            args.put("IT_HUSAVE", arrSaveData);
            showProcessingAndSubmit(Vars.PICKING_WITH_CONS_SAVE_PICK_DATA, REQUEST_PICK_BIN_SAVE, args);

        }catch(Exception exce){
            box = new AlertBox(getContext());
            box.getErrBox(exce);
        }
    }
    private void SetDataAfterSourceBinValidate(JSONObject responsebody) {
        try
        {
            JSONArray ET_FINAL_ARRAY = responsebody.getJSONArray("ET_FINAL");
            JSONArray ET_LQUA_ARRAY = responsebody.getJSONArray("ET_LQUA");
            JSONArray ET_EAN_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");

            int totalEtRecords = ET_FINAL_ARRAY.length()-1;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_FINAL_RECORD  = ET_FINAL_ARRAY.getJSONObject(recordIndex+1);
                    ETFinal record = new ETFinal();
                    record.setLgpicnr(ET_FINAL_RECORD.getString("PICNR"));
                    record.setLghuno(ET_FINAL_RECORD.getString("HU_NO"));
                    record.setLgwerks(ET_FINAL_RECORD.getString("WERKS"));
                    record.setLgmatnr(ET_FINAL_RECORD.getString("MATNR"));
                    record.setLghuqty(ET_FINAL_RECORD.getInt("HU_QTY"));
                    record.setLgpicklistqty(ET_FINAL_RECORD.getInt("PICKLIST_QTY"));
                    record.setLgscanqty(ET_FINAL_RECORD.getInt("SCAN_QTY"));
                    record.setLqreq008(ET_FINAL_RECORD.getInt("REQ_008"));
                    record.setLgbinqty(ET_FINAL_RECORD.getInt("BIN_QTY"));
                    record.setLgbin(ET_FINAL_RECORD.getString("BIN"));
                    record.setLgrempick(record.getLgpicklistqty());
                    record.setLgrembin(record.getLgbinqty());
                    ET_FINAL_DATA.put(record.getLgmatnr(), record);
                }
            }
            totalEtRecords = ET_LQUA_ARRAY.length()-1;
            if(totalEtRecords > 0){
                ET_LQUA_DATA = ET_LQUA_ARRAY;
            }
            totalEtRecords = ET_EAN_ARRAY.length()-1;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_EAN_RECORD  = ET_FINAL_ARRAY.getJSONObject(recordIndex+1);
                    ETEan record = new ETEan();
                    record.setLgumren(ET_EAN_RECORD.getInt("UMREN"));
                    record.setLgmeinh(ET_EAN_RECORD.getString("MEINH"));
                    record.setLgean11(ET_EAN_RECORD.getString("EAN11"));
                    record.setLgumrez(ET_EAN_RECORD.getInt("UMREZ"));
                    record.setLgmatnr(ET_EAN_RECORD.getString("MATNR"));
                    ET_EAN_DATA.put(record.getLgean11(),record);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
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

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

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
                                        if (request == REQUEST_VALIDATE_PICKLIST){
                                            text_picklistno.setText("");
                                        }
                                        if (request == REQUEST_VALIDATE_SOURCE_BIN){
                                            text_sourcebin.setText("");
                                        }
                                        return;
                                    }
                                    else{
                                        if (request == REQUEST_VALIDATE_PICKLIST){
                                            text_picklistno.setEnabled(false);
                                            text_sourcebin.setEnabled(true);
                                            button_refresh.setVisibility(View.VISIBLE);
                                            text_sourcebin.requestFocus();
                                        }
                                        else if(request == REQUEST_VALIDATE_SOURCE_BIN){
                                            text_sourcebin.setEnabled(false);
                                            formLayout.setVisibility(View.VISIBLE);
                                            SetDataAfterSourceBinValidate(responsebody);
                                            text_scanbin.setEnabled(true);
                                            text_scanbin.requestFocus();
                                        }
                                        else if(request == REQUEST_VALIDATE_SCAN_BIN){
                                            text_barcode.setEnabled(true);
                                            text_barcode.requestFocus();
                                        }
                                        else if (request == REQUEST_PICK_BIN_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    resetData();
                                                }
                                            });
                                        }
                                        return;
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