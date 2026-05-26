package com.v2retail.dotvik.dc;

import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GRTScanning01Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRTScanning01Fragment extends Fragment implements View.OnClickListener {

    private EditText dc_site,location,crate,barcode,sq,tsq;
    private Activity activity;
    private String URL ="" ,WERKS = "" ,USER = "";
    private String TAG ="GRTScanning01Fragment";
    private View view;
    private ProgressDialog dialog = null;
    private Button back,reset,submit;
    private AlertBox box;
    private JSONObject jsonObject;
    private JSONArray jsonArray;
    private int sum = 0;
    private int totalSumQ = 0;
    private String locationStr ="";
    private String dc ="";
    private String crateStr ="";
    Context con;

    private CheckBox unScan;

    private FragmentManager fm;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GRTScanning01Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GRTScanning01Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GRTScanning01Fragment newInstance(String param1, String param2) {
        GRTScanning01Fragment fragment = new GRTScanning01Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_g_r_t_scanning01, container, false);
        con = getContext();
        dc_site = (EditText) view.findViewById(R.id.dc_site);
        location = (EditText) view.findViewById(R.id.location);
        crate = (EditText) view.findViewById(R.id.crate);
        barcode = (EditText) view.findViewById(R.id.barcode);
        sq = (EditText) view.findViewById(R.id.sq);
        tsq = (EditText) view.findViewById(R.id.tsq);
        unScan = view.findViewById(R.id.unScan);

        submit = view.findViewById(R.id.submit);
        reset = view.findViewById(R.id.reset);
        back = view.findViewById(R.id.back);
//        reverse = view.findViewById(R.id.reverse);

        submit.setOnClickListener(this);
        reset.setOnClickListener(this);
        back.setOnClickListener(this);
//        reverse.setOnClickListener(this);

        fm = getFragmentManager();

        init();
        onEditListener();
        addTextChangeListeners();
        return view;
    }

    private void init() {
        activity = getActivity();
        jsonObject = new JSONObject();
        jsonArray = new JSONArray();
        box = new AlertBox(activity);
        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);


        WERKS=data.read("WERKS");
        USER=data.read("USER");
        URL = data.read("URL");

        if(!WERKS.isEmpty()) {
            Log.d(TAG, "WERKS->" + WERKS);
            dc_site.setText(WERKS);
        }
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);

        location.requestFocus();
        loadLocationData();

    }

    private void onEditListener() {

        location.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String locationNumber = location.getText().toString();
                    if (locationNumber == null || locationNumber.length() < 0 || locationNumber.equals("")) {
                        UIFuncs.blinkEffectOnError(con,location,true);
                        box.getBox("Alert", "Scan Location !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(location.getWindowToken(), 0);
                            loadLocationData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });

        crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String crateNo = crate.getText().toString();
                    if (crateNo == null || crateNo.length() < 0 || crateNo.equals("") ) {
                        UIFuncs.blinkEffectOnError(con,crate, true);
                        box.getBox("Alert", "Scan Crate Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(crate.getWindowToken(), 0);
                        loadCrateData();
                        return true;
                    }
                }

                return false;
            }
        });


        barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String barcodeNo = barcode.getText().toString();
                    if (barcodeNo == null || barcodeNo.length() < 0 || barcodeNo.equals("") ) {
                        UIFuncs.blinkEffectOnError(con,barcode, true);
                        box.getBox("Alert", "Scan Barcode Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(barcode.getWindowToken(), 0);
                        loadBarcodeData();
                        return true;
                    }
                }

                return false;
            }
        });

    }
    private void addTextChangeListeners() {

        location.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 2) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String locationNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Location  : " +  locationNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadLocationData();
                        }
                    });
                }
            }
        });


        crate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String crateString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Crate Number : " +  crateString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadCrateData();
                        }
                    });
                }
            }
        });

        barcode.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String barcodeString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Barcode Number : " +  barcodeString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBarcodeData();
                        }
                    });
                }
            }
        });

    }

    private void loadLocationData() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String locationNumber = location.getText().toString();
        String werks = dc_site.getText().toString();
        if (werks == null || werks.length() < 0 || werks.equals("")) {
            UIFuncs.blinkEffectOnError(con,dc_site, true);
            box.getBox("Alert", "Scan DC Site No!");
            dialog.dismiss();
            return;
        }

        if (locationNumber == null || locationNumber.length() < 0 || locationNumber.equals("")) {
            UIFuncs.blinkEffectOnError(con,location, true);
            box.getBox("Alert", "Scan Location !");
            dialog.dismiss();
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateStoNumberJson(werks,locationNumber);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void loadCrateData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String crateNumber = crate.getText().toString();

                if (dc == null || dc.length() < 0 || dc.equals("")) {
                    UIFuncs.blinkEffectOnError(con,dc_site,true);
                    box.getBox("Alert", "Scan DC No!");
                    dialog.dismiss();
                    return;
                }

                if (crateNumber == null || crateNumber.length() < 0 || crateNumber.equals("")) {
                    UIFuncs.blinkEffectOnError(con,crate,true);
                    box.getBox("Alert", "Scan Crate No!");
                    dialog.dismiss();
                    return;
                }

                crateGetData(crateNumber,dc);
            }
        }, 1000);
    }

    private void loadBarcodeData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String crateNumber = crate.getText().toString();
                String barcodeNumber  = barcode.getText().toString();

                if (dc == null || dc.length() < 0 || dc.equals("")) {
                    UIFuncs.blinkEffectOnError(con,dc_site,true);
                    box.getBox("Alert", "Scan DC No!");
                    dialog.dismiss();
                    return;
                }
                if (crateNumber == null || crateNumber.length() < 0 || crateNumber.equals("")) {
                    UIFuncs.blinkEffectOnError(con,crate,true);
                    box.getBox("Alert", "Scan Crate No!");
                    crate.setText("");
                    dialog.dismiss();
                    return;
                }
                if (barcodeNumber == null ||barcodeNumber.trim().length() <= 0 || barcodeNumber.trim().equals("")) {
                    UIFuncs.blinkEffectOnError(con,barcode,true);
                    box.getBox("Alert", "Scan Barcode No!");
                    barcode.setText("");
                    dialog.dismiss();
                    return;
                }
                barcodeGetData(barcodeNumber,dc);
            }
        }, 1000);
    }


    private void sendValidateStoNumberJson(String werks ,String locationNumber ) {

        String rfc = "ZWM_RFC_VALIDATE_DC_SLOC";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_LGORT", locationNumber);
            params.put("IM_WERKS", werks);




        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        dc_site.setText("");
                                        dc_site.requestFocus();
                                        return;
                                    } else {
                                        crate.requestFocus();
                                        dc_site.setEnabled(false);
                                        location.setEnabled(false);
                                        locationStr = locationNumber;
                                        dc = werks;
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

    private void crateGetData(String crateNumber ,String werks){
        // Version 11.85
        //String rfc = "ZWM_RFC_VALIDATE_CRATE";
        String rfc = "ZWM_GRT_VALIDATE_CRATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNumber);
            params.put("IM_WERKS", werks);


        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));

                                        crate.setText("");
                                        crate.requestFocus();
                                        return;
                                    } else {
                                        barcode.requestFocus();
                                        crateStr = crateNumber;
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

    private void barcodeGetData( String barcodeNumber , String werks){

        String rfc = "ZWM_RFC_STORE_EAN_DATA_STK";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EAN11", barcodeNumber);
            params.put("IM_WERKS", werks);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));

                                        barcode.setText("");
                                        barcode.requestFocus();
                                        return;
                                    } else {

                                        boolean flag = true;
                                        boolean unScanFlag = true;

                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        JSONObject jsonObject = new JSONObject();
                                        Log.v("Data",ET_EAN_DATA.toString());
                                        for (int i =1;i<ET_EAN_DATA.length();i++){
                                            JSONObject data = ET_EAN_DATA.getJSONObject(i);
                                            for (int j=0;j< jsonArray.length();j++){
                                                JSONObject jsonObject1 = jsonArray.getJSONObject(j);
                                                if (jsonObject1.getString("MATERIAL").equals(data.get("MATNR"))){
                                                    String sq = jsonObject1.getString("SCAN_QTY");
                                                    sum = Integer.valueOf(sq);

                                                    if (unScan.isChecked()){
                                                        if (sum- Integer.valueOf(data.getString("UMREZ") )<0){
                                                            AlertBox box = new AlertBox(getContext());
                                                            box.getBox("Alert", "Removed Quantity is less then already scanned quantity.");
                                                            barcode.setText("");
                                                            barcode.requestFocus();
                                                            return;
                                                        }
                                                        sum =  sum - Integer.valueOf(data.getString("UMREZ"));
                                                        totalSumQ = totalSumQ - Integer.valueOf(data.getString("UMREZ"));
                                                        unScanFlag = false;
                                                    }else {
                                                        sum =  sum + Integer.valueOf(data.getString("UMREZ"));
                                                        totalSumQ = totalSumQ + Integer.valueOf(data.getString("UMREZ"));

                                                    }


                                                    jsonObject1.put("SCAN_QTY",String.valueOf(sum));

                                                    flag = false;
                                                }
                                            }

                                            if (unScanFlag && unScan.isChecked()){
                                                AlertBox box = new AlertBox(getContext());
                                                box.getBox("Alert", "No Data Found for Removed Quantity - "+ barcodeNumber);
                                                barcode.setText("");
                                                barcode.requestFocus();
                                                return;
                                            }

                                            if(flag && !unScan.isChecked()){

                                                jsonObject.put("MATERIAL",data.get("MATNR"));
                                                jsonObject.put("PLANT",werks);
                                                jsonObject.put("STOR_LOC",locationStr);
                                                jsonObject.put("CRATE",crateStr);
                                                jsonObject.put("SCAN_QTY",data.getString("UMREZ"));
                                                jsonObject.put("BARCODE",data.getString("EAN11"));
                                                jsonObject.put("MEINS","EA");

                                                sum = Integer.valueOf(data.getString("UMREZ"));
                                                totalSumQ = totalSumQ + Integer.valueOf(data.getString("UMREZ"));

                                                jsonArray.put(jsonObject);

                                            }

                                        }

                                        Log.v(TAG,jsonObject.toString());
                                        sq.setText(String.valueOf(sum));
                                        tsq.setText(String.valueOf(totalSumQ));


                                        Log.v(TAG,jsonArray.toString());
                                        crate.setEnabled(false);
                                        barcode.setText("");
                                        barcode.requestFocus();

//                                        setBarcode(werks,barcodeNumber);

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

    private void saveDataToServer(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String ds = dc_site.getText().toString().trim();
                String l = location.getText().toString().trim();
                String b = barcode.getText().toString().trim();
                String t = tsq.getText().toString().trim();
                String s = sq.getText().toString().trim();

                String tag = "";
                if (dc == null || dc.length() < 0 || dc.equals(""))
                {   tag = "DC Site.";
                }

                if (locationStr == null || locationStr.length() < 0 || locationStr.equals(""))
                {   tag = "Location";
                }

                if (crateStr == null || crateStr.length() < 0 || crateStr.equals(""))
                {
                    tag = "Crate";
                }
                if (!tag.equals(""))
                {
                    UIFuncs.errorSound(con);
                    dialog.dismiss();
                    box.getBox("Alert", "Scan " + tag + " First");
                    return;
                }

                Log.d(TAG, "payload sent to server ");

                try {
                    sendAndRequestResponse();
                }catch (Exception e)
                {    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    private void sendAndRequestResponse() {

        String rfc = "ZWM_GRT_SAVE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateStr);
            params.put("IM_USER", USER);
            params.put("IM_WERKS", dc);
            params.put("IT_DATA", jsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));

                                        return;
                                    } else {

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                crateStr = "";
                                                sum =0;
                                                totalSumQ = 0;
                                                jsonArray = new JSONArray();
                                                crate.setText("");
                                                barcode.setText("");
                                                sq.setText("");
                                                tsq.setText("");
                                                crate.setEnabled(true);
                                                dc_site.setEnabled(true);
                                                location.setEnabled(true);
                                                crate.requestFocus();
                                            }
                                        });
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

        // no retry policy on save
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.submit:

                saveDataToServer();
                break;

            case R.id.reset:

                crateStr = "";
                sum =0;
                totalSumQ = 0;
                jsonArray = new JSONArray();
                crate.setText("");
                barcode.setText("");
                sq.setText("");
                tsq.setText("");
                crate.setEnabled(true);
                dc_site.setEnabled(true);
                location.setEnabled(true);
                crate.requestFocus();

                break;

            case R.id.back:

                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         fm.popBackStack();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });


                break;


//            case R.id.reverse:
//
//
//                try {
//                    JSONObject jsonObject = jsonArray.getJSONObject(jsonArray.length()-1);
//                    String s = jsonObject.getString("SCAN_QTY");
//                    int b = Integer.valueOf(s)-1;
//                    if (b==0){
//                        jsonArray.remove(jsonArray.length()-1);
//                    }
//                    jsonObject.put("SCAN_QTY",String.valueOf(b));
//                    tsq.setText(String.valueOf(Integer.valueOf(tsq.getText().toString())-1));
//                    sq.setText(String.valueOf(Integer.valueOf(sq.getText().toString())-1));
//                    Log.v(TAG,jsonArray.toString());
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//
//
//                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRT Scanning (01)");
    }
}