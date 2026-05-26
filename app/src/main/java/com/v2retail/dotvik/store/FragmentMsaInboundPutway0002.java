package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
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
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.putaway.ETDataStorePutway;
import com.v2retail.dotvik.modal.putaway.ETEanDataStorePutway;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Narayanan
 * @version 11.71
 * {@code Author: Narayanan, Revision: 1, Created: 30th Jul 2024, Modified: 30th Jul 2024}
 */
public class FragmentMsaInboundPutway0002 extends Fragment implements View.OnClickListener {
    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    String TAG = FragmentStoreDisplayInboundPutway.class.getName();
    private static final int REQUEST_VALIDATE_BIN = 5201;
    private static final int REQUEST_SAVE = 5202;
    String URL;
    String WERKS;
    String USER;
    private static String parent;
    Button btn_back, btn_reset, btn_next, btn_submit;
    EditText txt_store, txt_sloc, txt_scanbin, txt_bin, txt_ean, txt_article, txt_description, txt_sqty, txt_tsqty, txt_tqty;
    LinearLayout ll_screen2;
    String title;
    Map<String, Map<String, ETDataStorePutway>> etData = new HashMap<>();
    Map<String, Map<String, ETEanDataStorePutway>> etEanData = new HashMap<>();
    int totalScannedQty;

    public FragmentMsaInboundPutway0002() {
        // Required empty public constructor
    }

    public static FragmentMsaInboundPutway0002 newInstance(String breadcrumb) {
        FragmentMsaInboundPutway0002 fragment = new FragmentMsaInboundPutway0002();
        fragment.title = breadcrumb;
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .getSupportActionBar().setTitle(UIFuncs.getSmallTitle(title + " > Putway 0002"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getActivity().getSupportFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_msa_inbound_putway0002, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_store = view.findViewById(R.id.txt_msa_inbound_putway0002_store);
        txt_sloc = view.findViewById(R.id.txt_msa_inbound_putway0002_sloc);
        txt_scanbin = view.findViewById(R.id.txt_msa_inbound_putway0002_scanbin);
        txt_bin = view.findViewById(R.id.txt_msa_inbound_putway0002_bin);
        txt_ean = view.findViewById(R.id.txt_msa_inbound_putway0002_ean);
        txt_article = view.findViewById(R.id.txt_msa_inbound_putway0002_articleno);
        txt_description = view.findViewById(R.id.txt_msa_inbound_putway0002_description);
        txt_sqty = view.findViewById(R.id.txt_msa_inbound_putway0002_sqty);
        txt_tsqty = view.findViewById(R.id.txt_msa_inbound_putway0002_tsqty);
        txt_tqty = view.findViewById(R.id.txt_msa_inbound_putway0002_tqty);

        btn_back = view.findViewById(R.id.btn_msa_inbound_putway0002_back);
        btn_reset = view.findViewById(R.id.btn_msa_inbound_putway0002_reset);
        btn_next = view.findViewById(R.id.btn_msa_inbound_putway0002_next);
        btn_submit = view.findViewById(R.id.btn_msa_inbound_putway0002_submit);

        ll_screen2 = view.findViewById(R.id.ll_msa_inbound_putway0002_screen2);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_submit.setOnClickListener(this);

        txt_store.setText(WERKS);
        txt_sloc.setText("0001");

        clear();
        addInputEvents();
        step2();

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_msa_inbound_putway0002_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_msa_inbound_putway0002_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    step2();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_msa_inbound_putway0002_next:
                step2();
                break;
            case R.id.btn_msa_inbound_putway0002_submit:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
        txt_scanbin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scanbin);
                    if(value.length()>0) {
                        validateBin();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scanbin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if((before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if(value.length()>0 && scannerReading) {
                    validateBin();
                }
            }
        });
        txt_ean.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_ean);
                    if(value.length()>0) {
                        validateEan();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_ean.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if((before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if(value.length()>0 && scannerReading) {
                    validateEan();
                }
            }
        });
    }

    private void clear() {
        step2();
        ll_screen2.setVisibility(View.GONE);
        btn_reset.setVisibility(View.INVISIBLE);
        btn_next.setVisibility(View.VISIBLE);
        btn_submit.setVisibility(View.GONE);
    }

    private void step2(){
        etData = new HashMap<>();
        etEanData = new HashMap<>();
        ll_screen2.setVisibility(View.VISIBLE);
        btn_reset.setVisibility(View.VISIBLE);
        btn_next.setVisibility(View.GONE);
        btn_submit.setVisibility(View.VISIBLE);
        txt_scanbin.setText("");
        txt_bin.setText("");
        txt_ean.setText("");
        txt_article.setText("");
        txt_description.setText("");
        txt_sqty.setText("");
        txt_tsqty.setText("");
        txt_tqty.setText("");
        UIFuncs.disableInput(con, txt_ean);
        UIFuncs.enableInput(con, txt_scanbin);
    }

    private void showError(String title, String message){
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }
    private void validateBin() {
        String binno = UIFuncs.toUpperTrim(txt_scanbin);
        if(binAlreadyScanned(binno)){
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_BIN_001_VALIDATION);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_LGPLA", binno);
            showProcessingAndSubmit(Vars.ZWM_STORE_BIN_001_VALIDATION, REQUEST_VALIDATE_BIN, args);

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

    private void setData(JSONObject rsponse){
        try{
            JSONArray arrEtData = rsponse.getJSONArray("ET_DATA");
            JSONArray arrEtEanData = rsponse.getJSONArray("ET_EAN_DATA");
            String binno = UIFuncs.toUpperTrim(txt_scanbin);

            int arrlength = arrEtData.length();
            if(arrlength > 0){
                HashMap<String, ETDataStorePutway> mapEtData = new HashMap<>();
                for(int recordIndex = 1; recordIndex < arrlength; recordIndex++){
                    JSONObject ET_RECORD  = arrEtData.getJSONObject(recordIndex);
                    mapEtData.put(ET_RECORD.getString("MATNR"),ETDataStorePutway.newInstance(ET_RECORD,UIFuncs.toUpperTrim(txt_sloc),WERKS,binno));
                }
                etData.put(binno,mapEtData);
            }
            arrlength = arrEtEanData.length();
            if(arrlength > 0){
                HashMap<String, ETEanDataStorePutway> mapEtEanData = new HashMap<>();
                for(int recordIndex = 1; recordIndex < arrlength; recordIndex++){
                    JSONObject ET_EAN_RECORD  = arrEtEanData.getJSONObject(recordIndex);
                    mapEtEanData.put(ET_EAN_RECORD.getString("EAN11"),ETEanDataStorePutway.newInstance(ET_EAN_RECORD));
                }
                etEanData.put(binno,mapEtEanData);
            }
            resetBinScanFields(binno);
        }catch (Exception exce){
            box.getErrBox(exce);
        }
    }
    private boolean binAlreadyScanned(String binno){
        if(etEanData.containsKey(binno)){
            resetBinScanFields(binno);
            return true;
        }
        return false;
    }
    private void resetBinScanFields(String binno){
        txt_bin.setText(binno);
        txt_scanbin.setText("");
        txt_ean.setText("");
        txt_article.setText("");
        txt_description.setText("");
        calculateScanQtys();
        UIFuncs.enableInput(con, txt_ean);
    }
    private void validateEan(){
        String eanno = UIFuncs.toUpperTrim(txt_ean);
        String binno = UIFuncs.toUpperTrim(txt_bin);
        ETEanDataStorePutway eanRecord = searchEan(binno, eanno);
        if(eanRecord != null){
            if(etData.containsKey(binno)){
                String matnr = eanRecord.getMatnr();
                Map<String, ETDataStorePutway> mapEtData = etData.get(binno);
                if(mapEtData.containsKey(matnr)){
                    ETDataStorePutway etRecord = mapEtData.get(matnr);

                    int allowedQty = (int) Double.parseDouble(etRecord.getVerme1());
                    int matnrSqty = (int) Double.parseDouble(etRecord.getVerme());
                    int lotQty = (int) Double.parseDouble(eanRecord.getUmrez());

                    if(matnrSqty + lotQty > allowedQty){
                        showError("Not Allowed","Already scanned maximum allowed Qty");
                    }else{
                        totalScannedQty = totalScannedQty + lotQty;
                        etRecord.setVerme((matnrSqty + lotQty)+"");
                        txt_tsqty.setText(totalScannedQty+"");

                        txt_article.setText(etRecord.getMatnr());
                        txt_description.setText(etRecord.getMaktx());
                        calculateScanQtys();
                    }
                }else{
                    showError("Invalid Article","Article not found in ET Records");
                }
            }else{
                showError("Invalid Bin-ET","Scanned BIN is not valid. Please scan the BIN again");
            }
        }
        txt_ean.setText("");
        txt_ean.requestFocus();
    }
    private ETEanDataStorePutway searchEan(String binno, String eanno){
        if(etEanData.containsKey(binno)){
            Map<String,ETEanDataStorePutway> mapEanData = etEanData.get(binno);
            if(mapEanData.containsKey(eanno)){
                return mapEanData.get(eanno);
            }else{
                showError("Invalid EAN","Scanned EAN number not found in Bin Data");
            }
        }else{
            showError("Invalid Bin-EAN","Scanned BIN is not valid. Please scan the BIN again");
        }
        return null;
    }

    private void saveData() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_IROD_BIN_POST);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            JSONArray etScanData = getScanDataToSubmit();
            if(etScanData == null){
                return;
            }
            args.put("IT_DATA", etScanData);
            showProcessingAndSubmit(Vars.ZWM_STORE_IROD_BIN_POST, REQUEST_SAVE, args);

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
    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, Map<String, ETDataStorePutway>> binEtRecords : etData.entrySet()) {
                for (Map.Entry<String, ETDataStorePutway> etRecords : binEtRecords.getValue().entrySet()) {
                    ETDataStorePutway etRecord = etRecords.getValue();
                    int matnrScannedQty = (int) Double.parseDouble(etRecord.getVerme());
                    if(matnrScannedQty > 0){
                        String scanDataJsonString = new Gson().toJson(etRecord);
                        JSONObject itDataJson = new JSONObject(scanDataJsonString);
                        arrScanData.put(itDataJson);
                    }
                }
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some articles");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        txt_ean.requestFocus();
        return null;
    }

    private void calculateScanQtys(){
        int binSqty = 0;
        int totQty = 0;
        String binno = UIFuncs.toUpperTrim(txt_bin);
        for (Map.Entry<String, Map<String, ETDataStorePutway>> binEtRecords : etData.entrySet()) {
            String curBinno = binEtRecords.getKey();
            for (Map.Entry<String, ETDataStorePutway> etRecords : binEtRecords.getValue().entrySet()) {
                ETDataStorePutway etRecord = etRecords.getValue();
                int matnrScannedQty = (int) Double.parseDouble(etRecord.getVerme());
                totQty += ((int) Double.parseDouble(etRecord.getVerme1()));
                if(binno.equals(curBinno) && matnrScannedQty > 0){
                    binSqty += matnrScannedQty;
                }
            }
        }
        txt_sqty.setText(binSqty+"");
        txt_tqty.setText(totQty+"");
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
                                        if(request == REQUEST_VALIDATE_BIN){
                                            txt_scanbin.setText("");
                                            txt_scanbin.requestFocus();
                                        }
                                        if(request == REQUEST_SAVE){
                                            txt_ean.setText("");
                                            txt_ean.requestFocus();
                                        }
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                    } else {
                                        if(request == REQUEST_VALIDATE_BIN){
                                            setData(responsebody);
                                            return;
                                        }
                                        if(request == REQUEST_SAVE){
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("EX_TANUM"));
                                            step2();
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