package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

public class GRTTagHUStoreZone extends Fragment implements View.OnClickListener  {

    private static final int REQUEST_VALIDATE_STORE = 5203;
    private static final int REQUEST_VALIDATE_HU = 5204;

    private static final String TAG = GRTTagHUStoreZone.class.getName();

    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;
    Button reset,back;
    EditText txt_store,txt_hu,txt_zone;

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("Tag HU With Store Zone");
    }

    public GRTTagHUStoreZone() {
        // Required empty public constructor
    }

    public static GRTTagHUStoreZone newInstance(String param1, String param2) {
        GRTTagHUStoreZone fragment = new GRTTagHUStoreZone();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView =  inflater.inflate(R.layout.fragment_grt_tag_hu_store_zone, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        txt_store = rootView.findViewById(R.id.text_grt_single_tag_hu_store);
        txt_hu = rootView.findViewById(R.id.text_grt_single_tag_hu_hu);
        txt_zone = rootView.findViewById(R.id.text_grt_single_tag_hu_zone);
        reset = rootView.findViewById(R.id.btn_grt_single_tag_hu_reset);
        back = rootView.findViewById(R.id.btn_grt_single_tag_hu_back);

        reset.setOnClickListener(this);
        back.setOnClickListener(this);
        txt_store.requestFocus();
        txt_store.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  store = txt_store.getText().toString().toUpperCase().trim();
                    if(store.length()>0) {
                        txt_store.selectAll();
                        validateStore(store);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_store.addTextChangedListener(new TextWatcher() {
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
                String store = s.toString().toUpperCase().trim();
                if(store.length()>0 && scannerReading) {
                    txt_store.selectAll();
                    validateStore(store);
                }
            }
        });
        txt_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String hu = txt_hu.getText().toString().toUpperCase().trim();
                    if(hu.length()>0) {
                        txt_zone.setEnabled(true);
                        txt_zone.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_hu.addTextChangedListener(new TextWatcher() {
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
                String hu = s.toString().toUpperCase().trim();
                if(hu.length()>0 && scannerReading) {
                    txt_zone.setEnabled(true);
                    txt_zone.requestFocus();
                }
            }
        });
        txt_zone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  zone = txt_zone.getText().toString().toUpperCase().trim();
                    if(zone.length()>0) {
                        if(txt_hu.getText().toString().trim().length() == 0){
                            UIFuncs.errorSound(con);
                            AlertBox box = new AlertBox(getContext());
                            box.getBox("Missing", "Please Scan HU");
                            txt_hu.requestFocus();
                        }
                        else {
                            txt_zone.selectAll();
                            validateHU(txt_hu.getText().toString().trim().toUpperCase(Locale.ROOT),zone.trim().toUpperCase(Locale.ROOT));
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        txt_zone.addTextChangedListener(new TextWatcher() {
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
                String zone = s.toString().toUpperCase().trim();
                if(zone.length()>0 && scannerReading) {
                    if(txt_hu.getText().toString().trim().length() == 0){
                        UIFuncs.errorSound(con);
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Missing", "Please Scan HU");
                        txt_hu.requestFocus();
                    }
                    else {
                        txt_zone.selectAll();
                        validateHU(txt_hu.getText().toString().trim().toUpperCase(Locale.ROOT),zone.trim().toUpperCase(Locale.ROOT));
                    }
                }
            }
        });
        return rootView;
    }

    @Override
    public void onClick(View v) {
        AlertBox box = new AlertBox(getContext());
        switch (v.getId()){
            case R.id.btn_grt_single_tag_hu_reset:
                box.getBox("Alert", "Do you want to RESET.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TO DO
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                break;

            case R.id.btn_grt_single_tag_hu_back:
                box.getBox("Alert", "Do you want to GO BACK.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fm.popBackStack();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                break;
        }
    }
    private void validateStore(String store){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.GRT_SINGLE_PICK_TAG_HU_VALIDATE_STORE);
            args.put("IM_USER", USER);
            args.put("IM_STORE", txt_store.getText().toString().toUpperCase(Locale.ROOT).trim());
            showProcessingAndSubmit(Vars.GRT_SINGLE_PICK_TAG_HU_VALIDATE_STORE, REQUEST_VALIDATE_STORE, args,null);

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
    private void validateHU(String hu, String zone){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.GRT_SINGLE_PICK_TAG_HU_VALIDATE_HU_ZONE);
            args.put("IM_USER", USER);
            args.put("IM_STORE", txt_store.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("IM_HU", hu.trim());
            args.put("IM_ZONE", txt_zone.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("IM_HU_CLOSED", "Y");
            showProcessingAndSubmit(Vars.GRT_SINGLE_PICK_TAG_HU_VALIDATE_HU_ZONE, REQUEST_VALIDATE_HU, args,null);

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
                                        if(request == REQUEST_VALIDATE_STORE){
                                            txt_store.setText("");
                                            txt_store.setEnabled(true);
                                            txt_store.requestFocus();
                                        }
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                    } else {
                                        if(request == REQUEST_VALIDATE_STORE){
                                            txt_hu.setText("");
                                            txt_hu.setEnabled(true);
                                            txt_hu.requestFocus();
                                        }
                                        else if(request == REQUEST_VALIDATE_HU){
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));
                                            txt_hu.setText("");
                                            txt_hu.setEnabled(false);
                                            txt_zone.setText("");
                                            txt_zone.setEnabled(false);
                                            txt_store.setText("");
                                            txt_store.requestFocus();
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