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
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EmptyBinFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EmptyBinFragment extends Fragment implements View.OnClickListener {

    private View view;
    private Activity activity;
    private String URL ="";
    private String TAG = "EmptyBinFragment";
    private ProgressDialog dialog = null;
    private EditText wHouse,crate,bin,quantity,scanned_bin;
    private Button back,submit;
    private AlertBox box;
    private String crateNumber ="";
    private String binNum ="";
    private JSONArray jsonArraySaveData;
    String WERKS="";
    String USER="";
    FragmentManager fm;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EmptyBinFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EmptyBinFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EmptyBinFragment newInstance(String param1, String param2) {
        EmptyBinFragment fragment = new EmptyBinFragment();
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
        view = inflater.inflate(R.layout.fragment_empty_bin, container, false);
        wHouse = view.findViewById(R.id.wHouse);
        crate = view.findViewById(R.id.crate);
        bin = view.findViewById(R.id.bin);
        quantity = view.findViewById(R.id.quantity);
        scanned_bin = view.findViewById(R.id.scanned_bin);
        back = view.findViewById(R.id.back);
        submit = view.findViewById(R.id.submit);
        init();
        onEditListener();
        addTextChangeListeners();
        return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        jsonArraySaveData = new JSONArray();
        crate.requestFocus();
        SharedPreferencesData data = new SharedPreferencesData(activity);
        fm = getFragmentManager();
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        URL = data.read("URL");

        if(!URL.isEmpty())
            Log.d(TAG,"URL->"+URL);
        if(!WERKS.isEmpty()) {
            Log.d(TAG, "WERKS->" + WERKS);
            wHouse.setText(WERKS);
        }
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);

        submit.setOnClickListener(this);
        back.setOnClickListener(this);

    }

    private void onEditListener() {

        bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String binNumber = bin.getText().toString();
                    if (binNumber == null || binNumber.length() < 0 || binNumber.equals("")) {
                        box.getBox("Alert", "Scan Bin No!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin.getWindowToken(), 0);
                            loadBinData();
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
                        box.getBox("Alert", "Enter Crate Number !");
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





    }
    void addTextChangeListeners() {

        crate.addTextChangedListener(new TextWatcher() {
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

                String crateNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned crate Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadCrateData();
                        }
                    });
                }
            }
        });


        bin.addTextChangedListener(new TextWatcher() {
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

                String binString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Bin Number : " +  binString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBinData();
                        }
                    });
                }
            }
        });






    }

    void loadCrateData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String crateNumber = crate.getText().toString();
                String werks = wHouse.getText().toString();
                String imLgnum ="";

                if (crateNumber == null || crateNumber.length() < 0 || crateNumber.equals("")) {
                    box.getBox("Alert", "Scan Crate No!");
                    dialog.dismiss();
                    return;
                }
                if (werks.equals("DH24")) {
                    imLgnum = "V2R";
                }else {
                    imLgnum = "V2B";
                }

                crateGetData(crateNumber,imLgnum,werks);
            }
        }, 1000);
    }
    void loadBinData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String binNumber = bin.getText().toString();
                String werks = wHouse.getText().toString();
                String imLgnum ="";

                if (crateNumber == null || crateNumber.length() < 0 || crateNumber.equals("")){
                    box.getBox("Alert", "Scan Crate No!");
                    dialog.dismiss();
                    return;
                }

                if (binNumber == null || binNumber.length() < 0 || binNumber.equals("")) {
                    box.getBox("Alert", "Scan Bin No!");
                    dialog.dismiss();
                    return;
                }
                if (werks.equals("DH24")) {
                    imLgnum = "V2R";
                }else {
                    imLgnum = "V2B";
                }

                binGetData(binNumber,imLgnum,werks);
            }
        }, 1000);
    }

    void crateGetData(String crateNum,String imLgnum,String werks) {

        String rfc = "ZWM_VALI_CRATE_EMPTYBIN";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNum);
            params.put("IM_LGNUM", imLgnum);
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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        crate.setText("");
                                        crate.requestFocus();
                                        crateNumber = "";
                                        return;
                                    } else {
                                        bin.setText("");
                                        bin.requestFocus();
                                        crateNumber = crateNum;
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

    void binGetData(String binNumber,String imLgnum,String werks) {

        String rfc = "ZWM_VALIDATE_EMPTY_BIN";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNumber);
            params.put("IM_LGPLA", binNumber);
            params.put("IM_LGNUM", imLgnum);
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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        bin.setText("");
                                        scanned_bin.setText("");
                                        bin.requestFocus();
                                        binNum = "";
                                        return;
                                    } else {
                                        scanned_bin.setText(binNumber);
                                        binNum = binNumber;
                                        JSONArray jsonArray = responsebody.getJSONArray("IT_DATA");
                                        JSONObject jsonObject = new JSONObject();
                                        for (int i=1;i<jsonArray.length();i++){
                                            String SCAN_QTY = jsonArray.getJSONObject(i).getString("SCAN_QTY");
                                            String BIN_TYPE = jsonArray.getJSONObject(i).getString("BIN_TYPE");
                                            String WAREHOUSE = jsonArray.getJSONObject(i).getString("WAREHOUSE");
                                            String BIN = jsonArray.getJSONObject(i).getString("BIN");
                                            String CRATE = jsonArray.getJSONObject(i).getString("CRATE");

                                            quantity.setText(SCAN_QTY);

                                            jsonObject.put("SCAN_QTY",SCAN_QTY);
                                            jsonObject.put("WAREHOUSE",WAREHOUSE);
                                            jsonObject.put("BIN",BIN);
                                            jsonObject.put("CRATE",CRATE);
                                            jsonArraySaveData.put(jsonObject);

                                        }

                                        Log.v(TAG,jsonArraySaveData.toString());
                                        bin.setText("");

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

    private void saveDataToServer(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String werks = wHouse.getText().toString();
                String imLgnum ="";

                    if (jsonArraySaveData.length()<1) {
                        box.getBox("Alert", "Scan All Details");
                        dialog.dismiss();
                        return;
                    }
                Log.d(TAG, "payload sent to server ");
                if (werks.equals("DH24")) {
                    imLgnum = "V2R";
                }else {
                    imLgnum = "V2B";
                }

                try {
                    sendAndRequestResponse(imLgnum,werks);
                }catch (Exception e)
                {    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }
    void sendAndRequestResponse(String imLgnum,String werks) {

        String rfc = "ZWM_SAVE_EMPTY_BIN";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_LGNUM", imLgnum);
            params.put("IM_USER", USER);
            params.put("IM_WERKS", werks);
            params.put("IT_DATA", jsonArraySaveData);



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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));

                                        return;
                                    } else {

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"),new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                crate.setText("");
                                                bin.setText("");
                                                quantity.setText("");
                                                scanned_bin.setText("");
                                                crateNumber = "";
                                                binNum = "";
                                                crate.requestFocus();
                                                jsonArraySaveData = new JSONArray();

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
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Empty Bin");
    }
}