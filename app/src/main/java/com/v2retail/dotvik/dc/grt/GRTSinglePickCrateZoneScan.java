package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.v2retail.commons.SapJsonObjectRequest;
import com.bumptech.glide.util.Util;
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GRTSinglePickCrateZoneScan extends Fragment {

    private static final int REQUEST_VALIDATE_CRATE_ZONE = 5206;
    private static final int REQUEST_SUBMIT_CRATE_ZONE = 5207;
    private static final String TAG = GRTSinglePickCrateZoneScan.class.getName();

    private static final String ARG_ZONES = "zonelist";
    private static final String ARG_CRATE = "crate";
    private static final String ARG_MODE = "mode";
    private static final String SCAN_MODE = "scanmode";
    private static final String ARG_ETDATA = "etdata";
    private static final String ARG_MATNR = "matnr";

    private View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText text_zone,text_article,text_store,text_qty,text_sqty,text_scan_hu,text_proposed_hu;

    Button button_back,button_minus,button_next;

    List<CrateSortETData> etDatal = new ArrayList<>();

    int reqQty = 0;
    HashSet<String> argzones = new HashSet<>();
    String argcrate,argmode,matnr,scanmode;
    JSONObject etData = new JSONObject();
    public GRTSinglePickCrateZoneScan() {
        // Required empty public constructor
    }

    public static GRTSinglePickCrateZoneScan newInstance(String param1, String param2) {
        GRTSinglePickCrateZoneScan fragment = new GRTSinglePickCrateZoneScan();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            argzones = (HashSet<String>) getArguments().get(ARG_ZONES);
            argcrate = getArguments().getString(ARG_CRATE);
            argmode =  getArguments().getString(ARG_MODE);
            scanmode =  getArguments().getString(SCAN_MODE);
            Gson gson = new Gson();
            try {
                etData = new JSONObject(getArguments().getString(ARG_ETDATA));
            }catch (Exception exce){

            }
            matnr =  getArguments().getString(ARG_MATNR);
        }
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_single_pick_crate_scan_zone, container, false);
        if(argmode.equals("combo")) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Single Article Process");
        }
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        button_next = rootView.findViewById(R.id.btn_grt_single_crate_scan_zone_next);
        button_minus = rootView.findViewById(R.id.btn_grt_single_crate_scan_zone_minus);
        button_back = rootView.findViewById(R.id.btn_grt_single_crate_scan_zone_back);
        text_zone = rootView.findViewById(R.id.text_grt_single_crate_scan_zone);
        text_article = rootView.findViewById(R.id.text_grt_single_crate_scan_zone_article);

        text_store = rootView.findViewById(R.id.text_grt_single_crate_scan_zone_store);
        text_qty = rootView.findViewById(R.id.text_grt_single_crate_scan_zone_qty);
        text_sqty = rootView.findViewById(R.id.text_grt_single_crate_zone_scan_qty);
        text_proposed_hu = rootView.findViewById(R.id.text_grt_single_crate_proposed_hu);
        text_scan_hu = rootView.findViewById(R.id.text_grt_single_crate_scan_hu);

        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                box.getBox("Alert", "Do you want to go to Crate Scanning?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bundle args=new Bundle();
                        Fragment fragment = null;
                        fragment =  new GRTComboCrateScanning();
                        args.putString("zone", text_zone.getText().toString());
                        fragment.setArguments(args);
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.home, fragment, "grt_combo_picking_process_menu");
                        ft.commit();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
        });
        button_minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(text_sqty.getText() != null && text_sqty.getText().toString().length() > 0) {
                    int qty = Integer.parseInt(text_sqty.getText().toString());
                    qty -= 1;
                    if ((qty) < 0) {
                        return;
                    }
                    text_sqty.setText(qty+"");
                }
            }
        });
        button_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etDatal.remove(0);
                loadData();
            }
        });
        text_zone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  zone = text_zone.getText().toString().toUpperCase();
                    if(zone.length()>0) {
                        text_zone.selectAll();
                        validateZone(zone);
                        return true;
                    }
                }
                return false;
            }
        });
        text_zone.addTextChangedListener(new TextWatcher() {
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
                String zone = s.toString().toUpperCase();
                if(zone.length()>0 && scannerReading) {
                    text_zone.selectAll();
                    validateZone(zone);
                }
            }
        });

        text_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  shu = text_scan_hu.getText().toString().toUpperCase();
                    if(shu.length()>0) {
                        saveData();
                    }
                }
                return false;
            }
        });
        text_scan_hu.addTextChangedListener(new TextWatcher() {
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
                String shu = s.toString().toUpperCase();
                if(shu.length()>0 && scannerReading) {
                    saveData();
                }
            }
        });

        text_zone.requestFocus();
        if(argzones.size() > 0 && argmode.equals("combo")){
            text_zone.setText(argzones.stream().findFirst().get());
            text_zone.setEnabled(false);
            text_article.requestFocus();
        }
        if(matnr.length() > 0 ){
            text_article.setText(matnr);
            text_article.setEnabled(false);
            matnr = "";
        }
        if(etData.length() > 0){
            //SetPickData(etData);
        }
        return rootView;
    }
    private boolean checkIfAllowed(String zone){
        if(argzones.contains(zone))
            return true;
        else
            return false;

    }
    private void saveData(){
        if(text_sqty.getText() != null && text_sqty.getText().toString().length() > 0) {
            int qty = Integer.parseInt(text_sqty.getText().toString());
            String scanhu = text_scan_hu.getText().toString().toUpperCase(Locale.ROOT).trim();
            String phu = text_proposed_hu.getText().toString().toUpperCase(Locale.ROOT).trim();
            if(!scanhu.equals(phu)){
                UIFuncs.errorSound(con);
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid", "Invalid HU. Scan HU should match with Proposed HU");
                text_scan_hu.requestFocus();
                return;
            }
            if(qty < 0 || qty > reqQty){
                UIFuncs.errorSound(con);
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid", "Invalid Qty. Quantity should be either 0 or less than or equal to Total Qty");
                text_sqty.requestFocus();
            }
            else{
                JSONObject args = new JSONObject();
                try {
                    JSONArray arrEtData = new JSONArray();
                    JSONObject et_data = new JSONObject();
                    et_data.put("CRATE",argcrate);
                    et_data.put("ZONE",text_zone.getText().toString().trim().toUpperCase(Locale.ROOT));
                    et_data.put("DWERKS",text_store.getText().toString().toUpperCase(Locale.ROOT).trim());
                    et_data.put("MENGE",0.00);
                    et_data.put("E_MENGE",Double.parseDouble(text_sqty.getText().toString().trim()));
                    et_data.put("CONFIRMED","X");
                    et_data.put("MATNR",text_article.getText().toString().trim().toUpperCase(Locale.ROOT));
                    et_data.put("EX_HU",text_scan_hu.getText().toString().trim().toUpperCase(Locale.ROOT));
                    arrEtData.put(et_data);

                    args.put("bapiname", Vars.GRT_SINGLE_PICK_ZONE_CRATE_SIN_VALIDATE);
                    args.put("IM_USER", USER);
                    args.put("IM_ZONE", text_zone.getText().toString().trim().toUpperCase(Locale.ROOT));
                    args.put("IM_CRATE", argcrate);
                    args.put("ET_DATA", arrEtData);
                    showProcessingAndSubmit(Vars.GRT_SINGLE_PICK_ZONE_CRATE_SIN_VALIDATE, REQUEST_SUBMIT_CRATE_ZONE, args, null);

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
        else{
            UIFuncs.errorSound(con);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid", "Quantity you are submitting is invalid.");
            text_qty.requestFocus();
        }
    }
    private void validateZone(String zone){
        if(argmode.equals("combo") && !scanmode.equals("SIN")){
            return;
        }
        text_store.setText("");
        text_qty.setText("0");
        text_sqty.setText("0");
        etDatal = new ArrayList<>();
        zone = zone.toUpperCase(Locale.ROOT).trim();
        text_article.setText("");
        if(checkIfAllowed(zone)) {
            JSONObject args = new JSONObject();
            try {
                args.put("bapiname", Vars.GRT_SINGLE_PICK_ZONE_CRATE_SIN_VALIDATE);
                args.put("IM_USER", USER);
                args.put("IM_ZONE", zone);
                args.put("IM_CRATE", argcrate);
                etDatal = new ArrayList<>();
                showProcessingAndSubmit(Vars.GRT_SINGLE_PICK_ZONE_CRATE_SIN_VALIDATE, REQUEST_VALIDATE_CRATE_ZONE, args, null);

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
        else{
            UIFuncs.errorSound(con);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", "Not a valid Zone. Please scan valid Zone");
        }
    }
    public void showProcessingAndSubmit(String rfc, int request, JSONObject args, Map.Entry<Integer, CrateSortETData> targetEtData){

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
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                    } else {
                                        if(request == REQUEST_VALIDATE_CRATE_ZONE){
                                            SetPickData(responsebody);
                                        }
                                        else if(request == REQUEST_SUBMIT_CRATE_ZONE){
                                            etDatal.remove(0);
                                            SetPickData(responsebody);
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

    private void SetPickData(JSONObject responsebody) {
        try
        {
            etDatal = new ArrayList<>();
            JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_EAN_DATA.length();
            if(totalEtRecords > 0){
                text_article.setText(UIFuncs.removeLeadingZeros(responsebody.getString("EX_MATNR")));
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_EAN_RECORD  = ET_EAN_DATA.getJSONObject(recordIndex);
                    CrateSortETData etData = new CrateSortETData();
                    etData.setLgmenge(ET_EAN_RECORD.getDouble("MENGE"));
                    etData.setLgdwerks(ET_EAN_RECORD.getString("DWERKS"));
                    etData.setLgebeln(ET_EAN_RECORD.getString("EX_HU"));
                    etDatal.add(etData);
                }
            }
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    public void loadData() {
        text_store.setText("");
        text_qty.setText("0");
        text_sqty.setText("0");
        text_scan_hu.setText("");
        text_proposed_hu.setText("");
        reqQty = 0;
        if(etDatal.size() > 0){
            CrateSortETData etData = etDatal.get(0);
            text_store.setText(etData.getLgdwerks());
            text_qty.setText(((int)etData.getLgmenge())+"");
            text_sqty.setText(text_qty.getText().toString());
            text_proposed_hu.setText(UIFuncs.removeLeadingZeros(etData.getLgebeln()));
            reqQty = (int)etData.getLgmenge();
            text_scan_hu.requestFocus();
        }
    }
}