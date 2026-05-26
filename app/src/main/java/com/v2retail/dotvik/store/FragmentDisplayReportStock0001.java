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
 * @version 11.82
 * {@code Author: Narayanan, Revision: 1, Created: 20th Sep 2024, Modified: 20th Sep 2024}
 */
public class FragmentDisplayReportStock0001 extends Fragment implements View.OnClickListener {

    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    String TAG = FragmentDisplayReportStock0001.class.getName();
    private static final int REQUEST_VALIDATE_BIN = 5201;
    private static final int REQUEST_SAVE = 5202;
    String URL;
    String WERKS;
    String USER;
    private static String parent;
    Button btn_back, btn_prev, btn_next;
    EditText txt_store, txt_sloc, txt_scanbin, txt_bin;
    LinearLayout ll_screen2;
    String title;

    public FragmentDisplayReportStock0001() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .getSupportActionBar().setTitle(UIFuncs.getSmallTitle(title + " > Stock 0001"));
    }

    public static FragmentDisplayReportStock0001 newInstance(String breadcrumb) {
        FragmentDisplayReportStock0001 fragment = new FragmentDisplayReportStock0001();
        fragment.title = breadcrumb;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getActivity().getSupportFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_display_report_stock0001, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_store = view.findViewById(R.id.txt_display_report_stock0001_store);
        txt_sloc = view.findViewById(R.id.txt_display_report_stock0001_sloc);
        txt_scanbin = view.findViewById(R.id.txt_display_report_stock0001_scanbin);
        txt_bin = view.findViewById(R.id.txt_display_report_stock0001_bin);

        btn_back = view.findViewById(R.id.btn_display_report_stock0001_back);
        btn_prev = view.findViewById(R.id.btn_display_report_stock0001_previous);
        btn_next = view.findViewById(R.id.btn_display_report_stock0001_next);

        ll_screen2 = view.findViewById(R.id.ll_msa_inbound_putway0002_screen2);

        btn_back.setOnClickListener(this);
        btn_prev.setOnClickListener(this);
        btn_next.setOnClickListener(this);

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
            case R.id.btn_display_report_stock0001_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_display_report_stock0001_previous:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {

                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_display_report_stock0001_next:

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
    }

    private void clear() {
        step2();
        //ll_screen2.setVisibility(View.GONE);
    }

    private void step2(){
//        ll_screen2.setVisibility(View.VISIBLE);
        txt_scanbin.setText("");
        txt_bin.setText("");
        UIFuncs.enableInput(con, txt_scanbin);
    }

    private void showError(String title, String message){
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void validateBin() {
        String binno = UIFuncs.toUpperTrim(txt_scanbin);
        JSONObject args = new JSONObject();
        try {
            JSONObject IM_DATA = new JSONObject();
            IM_DATA.put("WERKS",WERKS);
            IM_DATA.put("USER",USER);
            IM_DATA.put("LGPLA",binno);
            args.put("bapiname", Vars.ZWM_STK_IROD_GANDOLA_0001);
            args.put("IM_DATA", IM_DATA);
            showProcessingAndSubmit(Vars.ZWM_STK_IROD_GANDOLA_0001, REQUEST_VALIDATE_BIN, args);

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

            int arrlength = arrEtData.length();
            if(arrlength > 0){
            }
            arrlength = arrEtEanData.length();
            if(arrlength > 0){
            }

        }catch (Exception exce){
            box.getErrBox(exce);
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
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                    } else {
                                        if(request == REQUEST_VALIDATE_BIN){
                                            setData(responsebody);
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