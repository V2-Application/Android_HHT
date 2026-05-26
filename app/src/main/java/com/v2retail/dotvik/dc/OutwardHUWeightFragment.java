package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OutwardHUWeightFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OutwardHUWeightFragment extends Fragment {

    private static final String TAG = OutwardHUWeightFragment.class.getName();

    private static final int REQUEST_SCAN_HU = 6203;
    private static final int REQUEST_SUBMIT_WEIGHT = 6204;
    private static final int REQUEST_SAVE = 6205;

    Context con;
    FragmentManager fm;
    AlertBox box ;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    EditText txt_scan_hu,txt_scanned_hu,txt_budget_wgt,txt_actual_wgt,txt_variance;
    Button btn_save;
    LinearLayout llsection;
    private OutwardHUWeightFragment.OnFragmentInteractionListener mListener;

    public OutwardHUWeightFragment() {
        // Required empty public constructor
    }
    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("HU Weight Calculation");
    }
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OutwardHUWeightFragment.OnFragmentInteractionListener) {
            mListener = (OutwardHUWeightFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    public static OutwardHUWeightFragment newInstance(String param1, String param2) {
        OutwardHUWeightFragment fragment = new OutwardHUWeightFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_outward_hu_weight, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        if(!URL.isEmpty())
            Log.d(TAG,"URL->"+URL);
        if(!WERKS.isEmpty())
            Log.d(TAG,"WERKS->"+WERKS);
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);

        txt_scan_hu = view.findViewById(R.id.text_outward_hu_weight_scan_hu);
        txt_scanned_hu = view.findViewById(R.id.text_outward_hu_weight_scanned_hu);
        txt_budget_wgt = view.findViewById(R.id.text_outward_hu_weight_budget_weight);
        txt_actual_wgt = view.findViewById(R.id.text_outward_hu_weight_actual_weight);
        txt_variance = view.findViewById(R.id.text_outward_hu_weight_variance);

        btn_save = view.findViewById(R.id.button_outward_hu_weight_save);

        llsection = view.findViewById(R.id.ll_outward_hu_weight);

        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String scanhu = txt_scan_hu.getText().toString().toUpperCase().trim();
                    if(scanhu.length()>0) {
                        txt_scan_hu.selectAll();
                        submitHU(scanhu);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_hu.addTextChangedListener(new TextWatcher() {
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
                String scanhu = s.toString().toUpperCase().trim();
                if(scanhu.length()>0 && scannerReading) {
                    txt_scan_hu.selectAll();
                    submitHU(scanhu);
                }
            }
        });

        txt_actual_wgt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String weight = txt_actual_wgt.getText().toString().toUpperCase().trim();
                    if(weight.length()>0) {
                        txt_actual_wgt.selectAll();
                        submitWeight(weight);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_actual_wgt.addTextChangedListener(new TextWatcher() {
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
                String weight = s.toString().toUpperCase().trim();
                if(weight.length()>0 && scannerReading) {
                    txt_actual_wgt.selectAll();
                    submitWeight(weight);
                }
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveForm();
            }
        });

        llsection.setVisibility(View.INVISIBLE);
        txt_scan_hu.requestFocus();
        return view;
    }
    private void resetForm(){
        txt_scan_hu.setEnabled(true);
        txt_scan_hu.setBackgroundColor(getResources().getColor(R.color.white));
        txt_actual_wgt.setEnabled(true);
        txt_actual_wgt.setBackgroundColor(getResources().getColor(R.color.white));
        llsection.setVisibility(View.INVISIBLE);
        txt_scanned_hu.setText("");
        txt_budget_wgt.setText("");
        txt_variance.setText("");
        txt_scan_hu.setText("");
        txt_actual_wgt.setText("");
        txt_scan_hu.requestFocus();
    }
    private void submitHU(String hu){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZFM_HU_WGT);
            args.put("IM_USER", USER);
            args.put("IM_SCAN_HU", hu.toUpperCase(Locale.ROOT).trim());
            showProcessingAndSubmit(Vars.ZFM_HU_WGT, REQUEST_SCAN_HU, args);

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
    private void submitWeight(String weight){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZFM_HU_WGT);
            args.put("IM_USER", USER);
            args.put("IM_SCAN_HU", txt_scan_hu.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("IM_ACTUAL_WT", weight);
            showProcessingAndSubmit(Vars.ZFM_HU_WGT, REQUEST_SUBMIT_WEIGHT, args);

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
    private void saveForm(){
        if(txt_scan_hu.getText().toString().trim().length() <= 4){
            UIFuncs.errorSound(con);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Missing", "Please scan valid HU");
            txt_scan_hu.requestFocus();
            return;
        }
        if(txt_actual_wgt.getText().toString().trim().length() == 0){
            UIFuncs.errorSound(con);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Missing", "Please provide actual weight");
            txt_actual_wgt.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZFM_HU_WGT_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_SCAN_HU", txt_scan_hu.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("IM_ACTUAL_WT", txt_actual_wgt.getText().toString().toUpperCase(Locale.ROOT).trim());
            showProcessingAndSubmit(Vars.ZFM_HU_WGT_SAVE, REQUEST_SAVE, args);

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
                                        if(request == REQUEST_SCAN_HU){
                                            txt_scan_hu.setText("");
                                            txt_scan_hu.requestFocus();
                                        }
                                        else if(request == REQUEST_SUBMIT_WEIGHT){
                                            txt_actual_wgt.setText("");
                                            txt_actual_wgt.requestFocus();
                                        }
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                    } else {
                                        if(request == REQUEST_SCAN_HU){
                                            txt_scan_hu.setEnabled(false);
                                            txt_scan_hu.setBackgroundColor(getResources().getColor(R.color.viewBg));
                                            llsection.setVisibility(View.VISIBLE);
                                            txt_scanned_hu.setText(UIFuncs.removeLeadingZeros(responsebody.getString("EX_SCANED_HU")));
                                            txt_budget_wgt.setText(responsebody.getString("EX_BUDGET_WT"));
                                            txt_actual_wgt.requestFocus();
                                        }
                                        else if(request == REQUEST_SUBMIT_WEIGHT){
                                            txt_actual_wgt.setEnabled(false);
                                            txt_actual_wgt.setBackgroundColor(getResources().getColor(R.color.viewBg));
                                            txt_variance.setText(responsebody.getString("EX_VARIANCE"));
                                            btn_save.requestFocus();
                                        }
                                        else if(request == REQUEST_SAVE){
                                            box.getBox("Success",returnobj.getString("MESSAGE"),new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    resetForm();
                                                }
                                            });
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