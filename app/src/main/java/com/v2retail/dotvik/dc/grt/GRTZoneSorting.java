package com.v2retail.dotvik.dc.grt;

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
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class GRTZoneSorting extends Fragment {

    private static final int REQUEST_VALIDATE_ZONE_CRATE = 208;
    private static final int REQUEST_VALIDATE_MSA_CRATE = 2081;
    private static final int REQUEST_VALIDATE_EMPTY_CRATE = 2082;
    private static final int REQUEST_SAVE_ZONE_SORT = 209;
    private static final int REQUEST_MSA_SAVE_ZONE_SORT = 2091;
    private static final String TAG = GRTZoneSorting.class.getName();
    private static final String CRATE = "crate";
    private static final String ZONE = "zone";
    private static final String MODE = "mode";

    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;

    LinearLayout ll_form,ll_scan_crate,ll_msa_crate;
    EditText text_scan_crate,text_scan_barcode,text_article_no,text_store_bin,text_zone_zone,text_msacrate_scan;
    Button button_reset,button_empty;
    private Map<String, CrateSortETEANData> ET_EAN_DATA = new HashMap<>();
    private Map<Integer, CrateSortETData> ET_DATA = new HashMap<>();
    private List<String> scannedMatnr = new ArrayList<>();
    String crate = "",zone = "",mode = "";

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("GRT Zone Sorting");
    }

    public GRTZoneSorting() {
    }
    public static GRTZoneSorting newInstance(String param1, String param2) {
        GRTZoneSorting fragment = new GRTZoneSorting();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            crate = getArguments().getString(CRATE);
            mode = getArguments().getString(MODE);
            zone = getArguments().getString(ZONE);
        }
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_zone_sorting, container, false);
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Mix Article Process");
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        text_msacrate_scan = rootView.findViewById(R.id.text_grt_zone_msacrate_scan);
        text_zone_zone = rootView.findViewById(R.id.text_grt_zone_zone);
        text_scan_barcode = rootView.findViewById(R.id.text_grt_zone_scan_barcode);
        text_article_no = rootView.findViewById(R.id.text_grt_zone_article_no);
        text_store_bin = rootView.findViewById(R.id.text_grt_zone_sugg_store_bin);
        text_scan_crate = rootView.findViewById(R.id.text_grt_zone_crate_scan);
        button_empty = rootView.findViewById(R.id.button_empty_grt_zone_sort_form);
        ll_form = rootView.findViewById(R.id.ll_grt_zone_crate_sort_form);
        ll_scan_crate = rootView.findViewById(R.id.ll_grt_zone_sort_form_container);
        ll_msa_crate = rootView.findViewById(R.id.ll_grt_zone_sort_msa_form_container);

        text_scan_crate.setText(this.crate);

        button_empty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emptyPickedCrate();
            }
        });

        text_msacrate_scan.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  msacrate = text_msacrate_scan.getText().toString().toUpperCase().trim();
                    if(msacrate.length()>0) {
                        text_msacrate_scan.selectAll();
                        validateMsaCrate(msacrate);
                        return true;
                    }
                }
                return false;
            }
        });
        text_msacrate_scan.addTextChangedListener(new TextWatcher() {
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
                String msacrate = s.toString().toUpperCase().trim();
                if(msacrate.length()>0 && scannerReading) {
                    text_msacrate_scan.selectAll();
                    validateMsaCrate(msacrate);
                }
            }
        });

        text_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  crate = text_scan_crate.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        text_scan_crate.selectAll();
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });
        text_scan_crate.addTextChangedListener(new TextWatcher() {
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
                String crate = s.toString().toUpperCase();
                if(crate.length()>0 && scannerReading) {
                    text_scan_crate.selectAll();
                    validateCrate(crate);
                }
            }
        });

        text_scan_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  articlebcode = text_scan_barcode.getText().toString().toUpperCase();
                    if(articlebcode.length()>0) {
                        text_scan_barcode.selectAll();
                        updateDataAndSave(articlebcode);
                        return true;
                    }
                }
                return false;
            }
        });
        text_scan_barcode.addTextChangedListener(new TextWatcher() {
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
                String articlebcode = s.toString().toUpperCase();
                if(articlebcode.length()>0 && scannerReading) {
                    text_scan_barcode.selectAll();
                    updateDataAndSave(articlebcode);
                }
            }
        });

        text_msacrate_scan.requestFocus();

        if(this.crate.length() > 0){
            ll_scan_crate.setVisibility(View.VISIBLE);
            text_scan_crate.setEnabled(false);
            this.crate = "";
        }

        return rootView;
    }
    private void resetForm(){
        text_scan_barcode.setText("");
        text_article_no.setText("");
        text_store_bin.setText("");
        text_zone_zone.setText("");
        text_scan_crate.setText("");
        text_msacrate_scan.setText("");

        button_empty.setVisibility(View.INVISIBLE);
        text_scan_crate.setEnabled(false);
        ll_form.setVisibility(View.INVISIBLE);
        ll_scan_crate.setVisibility(View.INVISIBLE);

        text_msacrate_scan.setEnabled(true);
        ET_EAN_DATA = new HashMap<>();
        ET_DATA = new HashMap<>();
        scannedMatnr = new ArrayList<>();
        text_msacrate_scan.requestFocus();
    }
    private void emptyPickedCrate(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.GRT_ZONE_SORT_EMPTY_PICKED_CRATE);
            args.put("IM_CRATE", text_scan_crate.getText().toString().trim().toUpperCase());
            args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.GRT_ZONE_SORT_EMPTY_PICKED_CRATE, REQUEST_VALIDATE_EMPTY_CRATE, args,null);

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
    private void validateMsaCrate(String msacrate) {
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_ZONE_SORT_VALIDATE_MSACRATE);
            args.put("IM_MSA_CRATE", msacrate);
            args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.GRT_ZONE_SORT_VALIDATE_MSACRATE, REQUEST_VALIDATE_MSA_CRATE, args,null);

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
    private void validateCrate(String crate) {
        JSONObject args = new JSONObject();
        try {
            text_scan_barcode.setText("");
            text_article_no.setText("");
            text_store_bin.setText("");

            args.put("bapiname", Vars.GRT_ZONE_SORT_VALIDATE_CRATE);
            args.put("IM_CRATE", crate);
            args.put("IM_USER", USER);

            ET_EAN_DATA = new HashMap<>();
            ET_DATA = new HashMap<>();
            scannedMatnr = new ArrayList<>();

            showProcessingAndSubmit(Vars.GRT_ZONE_SORT_VALIDATE_CRATE, REQUEST_VALIDATE_ZONE_CRATE, args,null);

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

    private void updateDataAndSave(String articlebcode) {
        Map.Entry<Integer,CrateSortETData> etDataToSubmit = null;
        CrateSortETEANData scannedBarcode = ET_EAN_DATA.get(articlebcode);
        if(scannedBarcode != null){
            String matnr = scannedBarcode.getLgmatnr();
            for (Map.Entry<Integer,CrateSortETData>  etData:ET_DATA.entrySet()) {
                if(etData.getValue().getLgmatnr().equals(matnr) && (etData.getValue().getLgzmapped() == null || etData.getValue().getLgzmapped().equals(""))) {
                    etDataToSubmit=etData;
                    text_article_no.setText(UIFuncs.removeLeadingZeros(etData.getValue().getLgmatnr()));
                    text_store_bin.setText(etData.getValue().getLgdwerks());
                    String zonecrate = etData.getValue().getLgzonecrate();
                    if(zonecrate == null || zonecrate.trim().length() == 0){
                        text_zone_zone.setText(text_msacrate_scan.getText().toString().trim().toUpperCase(Locale.ROOT));
                    }else{
                        text_zone_zone.setText(zonecrate.trim().toUpperCase(Locale.ROOT));
                    }
                    break;
                }
            }
            if(etDataToSubmit !=null){
                saveEtData(etDataToSubmit);
            }else{
                text_zone_zone.setText(text_msacrate_scan.getText().toString().trim().toUpperCase(Locale.ROOT));
                validateMSASave(null);
                return;
            }
        }else{
            text_zone_zone.setText(text_msacrate_scan.getText().toString().trim().toUpperCase(Locale.ROOT));
            validateMSASave(null);
            /*UIFuncs.blinkEffectOnError(con,text_scan_barcode,true);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", "Reverse Putway");
            text_scan_barcode.setText("");
            text_article_no.setText("");
            text_store_bin.setText("");*/
        }
    }
    private void saveEtData(Map.Entry<Integer,CrateSortETData> targetEtData){
        JSONObject args = new JSONObject();
        CrateSortETData etData = targetEtData.getValue();
        etData.setLgzmapped("X");
        if(etData.getLgzonecrate() == null || etData.getLgzonecrate().trim().length() == 0){
            etData.setLgzonecrate(text_msacrate_scan.getText().toString().trim().toUpperCase(Locale.ROOT));
        }
        try {
            String ET_Data_JsonString = new Gson().toJson(etData);
            if(ET_Data_JsonString.length() == 0){
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid Request", "Nothing to save. Please scan some articles");
                return;
            }
            JSONObject ET_DATA = new JSONObject(ET_Data_JsonString);
            args.put("bapiname", Vars.GRT_ZONE_SORT_CRATE_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_DATA", ET_DATA);
            showProcessingAndSubmit(Vars.GRT_ZONE_SORT_CRATE_SAVE, REQUEST_SAVE_ZONE_SORT, args,targetEtData);

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
    private void validateMSASave(Map.Entry<Integer,CrateSortETData> targetEtData) {
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_ZONE_SORT_VALIDATE_MSACRATE_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_MSA_CRATE", text_zone_zone.getText().toString().trim().toUpperCase());
            args.put("IM_MATNR", text_scan_barcode.getText().toString().trim().toUpperCase());
            args.put("IM_CRATE", text_scan_crate.getText().toString().trim().toUpperCase());

            showProcessingAndSubmit(Vars.GRT_ZONE_SORT_VALIDATE_MSACRATE_SAVE, REQUEST_MSA_SAVE_ZONE_SORT, args,targetEtData);

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
    public void showProcessingAndSubmit(String rfc, int request, JSONObject args, Map.Entry<Integer,CrateSortETData> targetEtData){

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args,targetEtData);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }
    private void submitRequest(String rfc, int request, JSONObject args, Map.Entry<Integer,CrateSortETData> targetEtData){

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
                                        if (request == REQUEST_SAVE_ZONE_SORT) {
                                            validateMSASave(targetEtData);
                                        }
                                        else if (request == REQUEST_MSA_SAVE_ZONE_SORT){
                                            text_scan_barcode.setText("");
                                            text_article_no.setText("");
                                            text_store_bin.setText("");
                                            text_zone_zone.setText("");
                                        }
                                        if(request != REQUEST_SAVE_ZONE_SORT){
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Err", returnobj.getString("MESSAGE"));
                                        }
                                        return;
                                    } else {
                                        if (request == REQUEST_VALIDATE_MSA_CRATE) {
                                            //ll_msa_crate.setVisibility(View.GONE);
                                            text_msacrate_scan.setEnabled(false);
                                            ll_scan_crate.setVisibility(View.VISIBLE);
                                            text_scan_crate.setEnabled(true);
                                            text_scan_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_ZONE_CRATE) {
                                            setETAndEANData(responsebody);
                                        }
                                        else if (request == REQUEST_SAVE_ZONE_SORT) {
                                            updateRecordsAndUI(type,targetEtData);
                                        }
                                        else if (request == REQUEST_MSA_SAVE_ZONE_SORT){
                                            text_scan_barcode.setText("");
                                            text_article_no.setText(responsebody.getString("EX_MATNR"));
                                            text_store_bin.setText("DH24");
                                            text_zone_zone.setText(text_msacrate_scan.getText().toString().trim());
                                        }
                                        else if (request == REQUEST_VALIDATE_EMPTY_CRATE) {
                                            AlertBox box = new AlertBox(getContext());
                                            if(responsebody.has("EX_TANUM")){
                                                box.getBox("Success", responsebody.getString("EX_TANUM")+" data saved successfully");
                                            }
                                            else {
                                                box.getBox("Success", returnobj.getString("Data saved successfully"));
                                            }
                                            resetForm();
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

    private void updateRecordsAndUI(String type, Map.Entry<Integer,CrateSortETData> targetEtData) {
        if(type.toUpperCase().equals("S")){
            CrateSortETData etData = targetEtData.getValue();
            etData.setLgzmapped("X");
            ET_DATA.put(targetEtData.getKey(),etData);
            scannedMatnr.add(etData.getLgmatnr());
            text_scan_barcode.setText("");
            text_scan_barcode.requestFocus();
        }else{
            UIFuncs.blinkEffectOnError(con,text_scan_barcode,true);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Not Allowed","Response Type - "+type+": Article "+targetEtData.getValue().getLgmatnr()+" is not allowed");
        }
    }

    private void setETAndEANData(JSONObject responsebody) {
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            JSONArray ET_EAN_DATA_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length()-1;
            int totalEanRecords = ET_EAN_DATA_ARRAY.length()-1;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    CrateSortETData ETData = new CrateSortETData();
                    ETData.setLgmandt(ET_RECORD.getString("MANDT"));
                    ETData.setLgwave(ET_RECORD.getString("WAVE"));
                    ETData.setLgtanum(ET_RECORD.getInt("TANUM"));
                    ETData.setLgitemno(ET_RECORD.getInt("ITEMNO"));
                    ETData.setLgebeln(ET_RECORD.getString("EBELN"));
                    ETData.setLgebelp(ET_RECORD.getInt("EBELP"));
                    ETData.setLgetype(ET_RECORD.getString("ETYPE"));
                    ETData.setLgzonesec(ET_RECORD.getString("ZONE_SEC"));
                    ETData.setLgvbelnvl(ET_RECORD.getString("VBELN_VL"));
                    ETData.setLgdwerks(ET_RECORD.getString("DWERKS"));
                    ETData.setLgmatnr(ET_RECORD.getString("MATNR"));
                    ETData.setLgmenge(ET_RECORD.getDouble("MENGE"));
                    ETData.setLglgtyp(ET_RECORD.getString("LGTYP"));
                    ETData.setLglgpla(ET_RECORD.getString("LGPLA"));
                    ETData.setLgcrate(ET_RECORD.getString("CRATE"));
                    ETData.setLgcomplete(ET_RECORD.getString("COMPLETE"));
                    ETData.setLglvorm(ET_RECORD.getString("LVORM"));
                    ETData.setLgpnature(ET_RECORD.getString("PNATURE"));
                    ETData.setLgpicked(ET_RECORD.getString("PICKED"));
                    ETData.setLgzonecrate(ET_RECORD.getString("ZONE_CRATE"));
                    ETData.setLgzmapped(ET_RECORD.getString("ZMAPPED"));
                    ETData.setLgmapped(ET_RECORD.getString("MAPPED"));
                    ETData.setLgerror(ET_RECORD.getString("ERROR"));
                    ETData.setLgmessage(ET_RECORD.getString("MESSAGE"));
                    ETData.setLgiscancelled(ET_RECORD.getString("ISCANCELLED"));
                    ETData.setLgcanceltanum(ET_RECORD.getInt("CANCEL_TANUM"));
                    ETData.setLgcancellgnum(ET_RECORD.getString("CANCEL_LGNUM"));
                    ETData.setLgerdat(ET_RECORD.getString("ERDAT"));
                    ETData.setLgernam(ET_RECORD.getString("ERNAM"));
                    ETData.setLgerzet(ET_RECORD.getString("ERZET"));
                    ET_DATA.put(recordIndex+1,ETData);
                }
            }
            if(totalEanRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEanRecords; recordIndex++){
                    JSONObject EAN_RECORD  = ET_EAN_DATA_ARRAY.getJSONObject(recordIndex+1);
                    CrateSortETEANData EANData = new CrateSortETEANData();
                    EANData.setLgmandt(EAN_RECORD.getString("MANDT"));
                    EANData.setLgmatnr(EAN_RECORD.getString("MATNR"));
                    EANData.setLgmeinh(EAN_RECORD.getString("MEINH"));
                    EANData.setLglfnum(EAN_RECORD.getString("LFNUM"));
                    EANData.setLgean11(EAN_RECORD.getString("EAN11"));
                    EANData.setLgeantp(EAN_RECORD.getString("EANTP"));
                    EANData.setLghpean(EAN_RECORD.getString("HPEAN"));
                    EANData.setLgsgtcatv(EAN_RECORD.getString("SGT_CATV"));
                    EANData.setLgsttpecsergtin(EAN_RECORD.getString("/STTPEC/SER_GTIN"));
                    ET_EAN_DATA.put(EAN_RECORD.getString("EAN11"),EANData);
                }
                if(totalEanRecords > 0 && totalEtRecords > 0){
                    ll_form.setVisibility(View.VISIBLE);
                    //button_reset.setVisibility(View.VISIBLE);
                    button_empty.setVisibility(View.VISIBLE);
                    text_scan_crate.setEnabled(false);
                    text_scan_barcode.requestFocus();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
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