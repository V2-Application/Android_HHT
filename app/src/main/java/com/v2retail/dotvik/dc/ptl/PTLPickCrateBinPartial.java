package com.v2retail.dotvik.dc.ptl;

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
import com.v2retail.dotvik.modal.grt.cratepick.ETPickData;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickSaveData;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PTLPickCrateBinPartial#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PTLPickCrateBinPartial extends Fragment implements View.OnClickListener {

    private static final int REQUEST_PARTIAL_SAVE_DATA = 305;
    private static final int REQUEST_VALIDATE_CRATE = 3051;


    private static final String TAG = PTLPickCrateBinPartial.class.getName();

    private static final String ARG_PICKLISTNO = "picklistno";
    private static final String ARG_PICK_DATA = "pickdata";
    private static final String ARG_ET_EAN_DATA = "eandata";

    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    View rootView;

    EditText text_picklist_no;
    EditText text_remaining_qty;
    EditText text_crate;
    EditText text_bin;
    EditText text_article_no;
    EditText text_scan_barcode;
    EditText text_total_scanned_qty;

    Button button_save;

    private Map<String, HUEANData> ET_EAN_DATA = new HashMap<>();
    private ETPickData pickData = null;
    private List<ETPickSaveData> IT_DATA = new ArrayList<>();
    private String mPicklistno;

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL Scan Article (Partial)");
    }

    public PTLPickCrateBinPartial() {

    }
    public static PTLPickCrateBinPartial newInstance(String param1, String param2) {
        PTLPickCrateBinPartial fragment = new PTLPickCrateBinPartial();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pickData = (ETPickData) getArguments().get(ARG_PICK_DATA);
            ET_EAN_DATA = (HashMap<String, HUEANData>) getArguments().get(ARG_ET_EAN_DATA);
            mPicklistno = getArguments().getString(ARG_PICKLISTNO);
        }
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_ptl_pick_crate_bin_partial, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        text_picklist_no = rootView.findViewById(R.id.text_ptl_p_picklist_no);
        text_remaining_qty = rootView.findViewById(R.id.text_ptl_p_remaining_qty);
        text_crate = rootView.findViewById(R.id.text_ptl_p_crate);
        text_bin = rootView.findViewById(R.id.text_ptl_p_bin);
        text_article_no = rootView.findViewById(R.id.text_ptl_p_article_no);
        text_scan_barcode = rootView.findViewById(R.id.text_ptl_p_scan_barcode);
        text_total_scanned_qty = rootView.findViewById(R.id.text_ptl_p_scanned_qty);

        text_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  crate = text_crate.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        text_crate.selectAll();
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });
        text_crate.addTextChangedListener(new TextWatcher() {
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
                    text_crate.selectAll();
                    validateCrate(crate);
                }
            }
        });

        text_scan_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  barcode = text_scan_barcode.getText().toString().toUpperCase();
                    if(barcode.length()>0) {
                        text_scan_barcode.selectAll();
                        findMatnrAndMoveToITData(barcode);
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
                String barcode = s.toString().toUpperCase();
                if(barcode.length()>0 && scannerReading) {
                    text_scan_barcode.selectAll();
                    findMatnrAndMoveToITData(barcode);
                }
            }
        });
        button_save = rootView.findViewById(R.id.button_save_ptl_p_form);
        button_save.setOnClickListener(this);
        text_picklist_no.setText(mPicklistno);
        setTextFieldsData();
        return rootView;
    }

    //CUSTOM USER FUNCTIONS
    private void setTextFieldsData(){
        text_bin.setText(pickData.getLgbin());
        text_article_no.setText(UIFuncs.removeLeadingZeros(pickData.getLgmatnr()));
        text_remaining_qty.setText(getRemainingQty()+"");
        text_crate.requestFocus();
    }
    private int getRemainingQty(){
        int remainingQty = 0;
        for (Map.Entry<String, HUEANData> eanData:ET_EAN_DATA.entrySet()) {
            if(eanData.getValue().getLgmatnr().equals(pickData.getLgmatnr())){
                remainingQty++;
            }
        }
        return remainingQty;
    }
    private void validateCrate(String crate) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.PTL_VALIDATE_CRATE_FOR_PICKDATA);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());
            args.put("IM_NATURE", "P");
            pickData.setLgcrate("");
            showProcessingAndSubmit(Vars.PTL_VALIDATE_CRATE_FOR_PICKDATA,REQUEST_VALIDATE_CRATE,args);
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
    private void findMatnrAndMoveToITData(String barcode) {

        if(pickData.getLgcrate().trim().length()==0){
            box = new AlertBox(getContext());
            box.getBox("Invalid", "Crate value is empty. Please re-validate the crate");
            text_crate.selectAll();
            text_crate.requestFocus();
            return;
        }

        HUEANData foundEan  = ET_EAN_DATA.get(barcode);
        if(foundEan != null){
            String matnr =foundEan.getLgmatnr();
            if(pickData.getLgmatnr().equals(matnr)){
                ETPickSaveData itData = new ETPickSaveData();
                itData.setLgmaterial(pickData.getLgmaterial());
                itData.setLgbin(pickData.getLgbin());
                itData.setLgplant(WERKS);
                itData.setLgbarcode(barcode);
                itData.setLgpickqty(1);
                itData.setLgscanqty(1);

                IT_DATA.add(itData);
                ET_EAN_DATA.remove(barcode);
                int remainingQty = getRemainingQty();
                text_remaining_qty.setText(remainingQty+"");
                text_total_scanned_qty.setText(IT_DATA.size()+"");
                text_scan_barcode.setText("");
                button_save.setVisibility(View.VISIBLE);
            }
            else{
                UIFuncs.blinkEffectOnError(con,text_scan_barcode,true);
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid", "Article No "+foundEan.getLgmatnr()+" not found in PICK DATA");
                text_scan_barcode.setText("");
                return;
            }
        }else{
            UIFuncs.blinkEffectOnError(con,text_scan_barcode,true);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid", "Code "+barcode+" not found in EAN DATA");
            text_scan_barcode.setText("");
        }
    }

    private void submitScannedPickedData() {

        try {
            JSONObject args = new JSONObject();
            JSONArray arrItData = new JSONArray();

            for (ETPickSaveData itData : IT_DATA) {
                itData.setLgcrate(pickData.getLgcrate());
                String IT_Data_JsonString = new Gson().toJson(itData);
                JSONObject itDataJson = new JSONObject(IT_Data_JsonString);
                arrItData.put(itDataJson);
            }
            if(arrItData.length() == 0){
                box = new AlertBox(getContext());
                box.getBox("Invalid", "Nothing to save. 0 barcode scanned. Scanned Qty. should be greater than 0");
                return;
            }

            args.put("bapiname", Vars.PTL_SAVE_PICK_DATA);
            args.put("IM_USER", USER);
            args.put("IM_NATURE", "P");
            args.put("IM_TEST", "");
            args.put("IM_TANUM", mPicklistno);
            args.put("IT_DATA", arrItData);

            showProcessingAndSubmit(Vars.PTL_SAVE_PICK_DATA, REQUEST_PARTIAL_SAVE_DATA, args);
        }catch(Exception exce){
            box = new AlertBox(getContext());
            box.getErrBox(exce);
        }
    }

    @Override
    public void onClick(View view) {
        CommonUtils.hideKeyboard(getActivity());
        switch (view.getId()){
            case R.id.button_save_ptl_p_form:
                submitScannedPickedData();
                break;
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
                                        if (request == REQUEST_VALIDATE_CRATE){
                                            text_crate.setText("");
                                        }
                                        return;
                                    }
                                    else{
                                        if (request == REQUEST_VALIDATE_CRATE){
                                            pickData.setLgcrate(params.getString("IM_CRATE"));
                                            text_scan_barcode.requestFocus();
                                        }
                                        else if (request == REQUEST_PARTIAL_SAVE_DATA) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    fm.popBackStack();
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