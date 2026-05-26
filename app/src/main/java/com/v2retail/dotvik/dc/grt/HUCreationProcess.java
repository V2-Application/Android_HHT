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
import com.v2retail.commons.DataHelper;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.dotvik.modal.grt.createhu.HUETData;
import com.v2retail.dotvik.modal.grt.createhu.HUITData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HUCreationProcess#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HUCreationProcess extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int REQUEST_HU_VALIDATE = 206;
    private static final int REQUEST_HU_SAVE = 207;
    private static final String TAG = HUCreationProcess.class.getName();

    // TODO: Rename and change types of parameters
    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;

    LinearLayout ll_form;
    EditText text_scan_hu,text_store_code, text_scan_barcode,text_article_no,text_total_scanned;
    Button button_reset,button_save;

    private Map<String, HUEANData> ET_EAN_DATA = new HashMap<>();
    private Map<String, HUETData> ET_DATA = new HashMap<>();
    private List<HUITData> IT_DATA = new ArrayList<>();
    private List<String> scannedMatnr = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("HU Creation");
    }

    public HUCreationProcess() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HUCreationProcess.
     */
    // TODO: Rename and change types and number of parameters
    public static HUCreationProcess newInstance(String param1, String param2) {
        HUCreationProcess fragment = new HUCreationProcess();
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
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_hu_creation_process, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        ll_form = rootView.findViewById(R.id.ll_grt_crate_hu_form);
        text_scan_hu = rootView.findViewById(R.id.text_grt_hu_scan_hu);
        text_total_scanned = rootView.findViewById(R.id.text_grt_hu_total_scanned_qty);
        text_store_code = rootView.findViewById(R.id.text_grt_hu_store_code);
        text_article_no = rootView.findViewById(R.id.text_grt_hu_scanned_article_no);
        text_scan_barcode = rootView.findViewById(R.id.text_grt_hu_scan_barcode);
        button_reset = rootView.findViewById(R.id.button_reset_grt_hu_form);
        button_save = rootView.findViewById(R.id.button_save_grt_hu_form);
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitItData();
            }
        });
        button_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetForm();
            }
        });
        text_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  hu = text_scan_hu.getText().toString().toUpperCase();
                    if(hu.length()>0) {
                        text_scan_hu.selectAll();
                        validateHU(hu);
                        return true;
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
                String hu = s.toString().toUpperCase();
                if(hu.length()>0 && scannerReading) {
                    text_scan_hu.selectAll();
                    validateHU(hu);
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
                        validateScannedBarcode(barcode);
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
                    validateScannedBarcode(barcode);
                }
            }
        });

        text_scan_hu.requestFocus();
        return rootView;
    }

    private void resetForm() {
        text_scan_hu.setEnabled(true);
        text_total_scanned.setText("0");
        text_store_code.setText("");
        text_article_no.setText("");
        text_scan_barcode.setText("");
        ll_form.setVisibility(View.INVISIBLE);
        button_reset.setVisibility(View.INVISIBLE);

        ET_EAN_DATA = new HashMap<>();
        ET_DATA = new HashMap<>();
        IT_DATA = new ArrayList<>();
        scannedMatnr = new ArrayList<>();

        text_scan_hu.requestFocus();
        text_scan_hu.selectAll();
    }

    //CUSTOM USER METHODS
    private void validateHU(String hu) {
        JSONObject args = new JSONObject();
        try {
            text_scan_barcode.setText("");
            text_store_code.setText("");
            text_article_no.setText("");
            text_total_scanned.setText("0");

            args.put("bapiname", Vars.GRT_HU_PICK_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_EXT_HU", hu);

            ET_EAN_DATA = new HashMap<>();
            ET_DATA = new HashMap<>();
            IT_DATA = new ArrayList<>();
            scannedMatnr = new ArrayList<>();

            showProcessingAndSubmit(Vars.GRT_HU_PICK_VALIDATE, REQUEST_HU_VALIDATE, args);

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
    private String removeLeadingZeros(String str)
    {
        String regex = "^0+(?!$)";
        return str.replaceAll(regex, "");
    }
    private void validateScannedBarcode(String barcode) {
        String matnrToSearch = "";
        text_article_no.setText(matnrToSearch);
        if(ET_EAN_DATA.containsKey(barcode)){
            matnrToSearch = ET_EAN_DATA.get(barcode).getLgmatnr();
            text_article_no.setText(removeLeadingZeros(matnrToSearch));
            if(ET_DATA.containsKey(matnrToSearch)){
                HUETData etData = ET_DATA.get(matnrToSearch);
                HUITData itData = new HUITData();
                itData.setLgmandt("");
                itData.setLgexidv(text_scan_hu.getText().toString());
                itData.setLgposnr(000001);
                itData.setLglgnum(etData.getLglgnum());
                itData.setLgerdat("");
                itData.setLgwerks(text_store_code.getText().toString());
                itData.setLgmatnr(matnrToSearch);
                itData.setLgmenge(1);
                itData.setLgebeln("");
                itData.setLgvbeln("");
                itData.setLgtanum(0);
                itData.setLgsaphu("");
                itData.setLgerror("");
                itData.setLgmessage("");
                itData.setLgcomplete("");
                itData.setLgernam("");
                itData.setLgerzet("");
                IT_DATA.add(itData);
                text_total_scanned.setText(IT_DATA.size()+"");
                text_scan_barcode.setText("");
                text_scan_barcode.requestFocus();
            }else{
                UIFuncs.blinkEffectOnError(con,text_scan_barcode,true);
                box = new AlertBox(getContext());
                box.getBox("Err", "Article No "+matnrToSearch+" does not exist in ET DATA");
            }
        }else{
            UIFuncs.blinkEffectOnError(con,text_scan_barcode,true);
            box = new AlertBox(getContext());
            box.getBox("Err", "Code "+barcode+" does not exist in EAN DATA");
        }
    }
    private void submitItData() {
        try {
            JSONObject args = new JSONObject();
            JSONArray arrItData = new JSONArray();

            for (HUITData itData : IT_DATA) {
                String IT_Data_JsonString = new Gson().toJson(itData);
                JSONObject itDataJson = new JSONObject(IT_Data_JsonString);
                arrItData.put(itDataJson);
            }
            if(arrItData.length() == 0){
                box = new AlertBox(getContext());
                box.getBox("Err", "Nothing to save. 0 barcode scanned. Please reset this form and try again");
                return;
            }

            args.put("bapiname", Vars.GRT_HU_PICK_SAVE);
            args.put("IM_USER", USER);
            args.put("IT_DATA", arrItData);

            showProcessingAndSubmit(Vars.GRT_HU_PICK_SAVE, REQUEST_HU_SAVE, args);
        }catch(Exception exce){
            box = new AlertBox(getContext());
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
                                        return;
                                    } else {

                                        if (request == REQUEST_HU_VALIDATE) {
                                            if(type.equals("S")) {
                                                text_store_code.setText(responsebody.getString("EX_STORECODE"));
                                                setETAndEANData(responsebody);
                                            }else{
                                                AlertBox box = new AlertBox(getContext());
                                                box.getBox("Err", "EAN data does not contain allowed barcodes");
                                                return;
                                            }
                                        }
                                        else if (request == REQUEST_HU_SAVE) {
                                            if(type.equals("S")) {
                                                AlertBox box = new AlertBox(getContext());
                                                box.getBox("Success", returnobj.getString("MESSAGE"));
                                                updateRecordsAndUI(responsebody);
                                            }else{
                                                AlertBox box = new AlertBox(getContext());
                                                box.getBox("Err Code: "+type, returnobj.getString("MESSAGE"));
                                                return;
                                            }
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

    private void updateRecordsAndUI(JSONObject responsebody) {
        text_scan_hu.setText("");
        resetForm();
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
                    HUETData ETData = new HUETData();
                    ETData.setLglgnum(ET_RECORD.getString("LGNUM"));
                    ETData.setLglqnum(ET_RECORD.getInt("LQNUM"));
                    ETData.setLgmatnr(ET_RECORD.getString("MATNR"));
                    ETData.setLgwerks(ET_RECORD.getString("WERKS"));
                    ETData.setLglgort(ET_RECORD.getString("LGORT"));
                    ETData.setLglgtyp(ET_RECORD.getString("LGTYP"));
                    ETData.setLglgpla(ET_RECORD.getString("LGPLA"));
                    ETData.setLgverme(ET_RECORD.getDouble("VERME"));
                    ET_DATA.put(ET_RECORD.getString("MATNR"),ETData);
                }
            }
            if(totalEanRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEanRecords; recordIndex++){
                    JSONObject EAN_RECORD  = ET_EAN_DATA_ARRAY.getJSONObject(recordIndex+1);
                    if(EAN_RECORD.getString("EAN11").trim().length() == 0){
                        continue;
                    }
                    ET_EAN_DATA.put(EAN_RECORD.getString("EAN11"), DataHelper.initEANData(EAN_RECORD));
                }
                if(totalEanRecords > 0 && totalEtRecords > 0){
                    ll_form.setVisibility(View.VISIBLE);
                    button_reset.setVisibility(View.VISIBLE);
                    text_scan_hu.setEnabled(false);
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