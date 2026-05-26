package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.putaway.ETData_QcFailedCancel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EcommCancelQCPutaway#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EcommCancelQCPutaway extends Fragment implements View.OnClickListener {

    private static int VALIDATE_CRATE_REQUEST = 100;
    private static int VALIDATE_PLANT_REQUEST = 101;
    private static int SAVE_REQUEST = 102;

    private final String TAG = "EcommCancelQCPutaway";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private ProgressDialog dialog = null;

    private Activity activity;

    private View rootView;

    private EditText text_destination_crate;
    private EditText text_article_no;
    private EditText text_total_scan;

    private RadioButton radio_failed;
    private RadioButton radio_cancel;
    private RadioGroup radio_group;

    private Button button_save;
    private Button button_reset;

    private String validatedCrate;

    private Map<String, ETData_QcFailedCancel> ET_RECORDS = new HashMap<>();
    private Map<String, JSONObject> ET_EAN_RECORDS = new HashMap<>();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EcommCancelQCPutaway() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment fragment_ecmm_cancel_qc_putaway.
     */
    // TODO: Rename and change types and number of parameters
    public static EcommCancelQCPutaway newInstance(String param1, String param2) {
        EcommCancelQCPutaway fragment = new EcommCancelQCPutaway();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ecmm_cancel_qc_putaway, container, false);
        initView();
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    void initView() {
        activity = getActivity();

        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

        text_destination_crate = rootView.findViewById(R.id.text_destination_crate_qcputaway);
        text_article_no = rootView.findViewById(R.id.text_article_no_qcputaway);
        text_total_scan = rootView.findViewById(R.id.text_total_scan_qcputaway);

        radio_failed = rootView.findViewById(R.id.radio_qc_failed);
        radio_cancel = rootView.findViewById(R.id.radio_qc_cancel);
        radio_group = rootView.findViewById(R.id.radio_group_qcfailed);

        button_save = rootView.findViewById(R.id.button_save_qcputaway);
        button_reset = rootView.findViewById(R.id.button_reset_qcputaway);


        radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.radio_qc_failed:
                        if(radio_failed.isChecked()) {
                            validateDesitationPlant(Vars.PUTAWAY_MODE_QC_FAILED);
                        }
                        break;
                    case R.id.radio_qc_cancel:
                        if(radio_cancel.isChecked()) {
                            validateDesitationPlant(Vars.PUTAWAY_MODE_CANCEL);
                        }
                        break;
                    }
             }
         });

        handleClicks();

        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Cancel QC Putaway");
        }

        radio_failed.requestFocus();

    }



    void handleClicks() {
        button_save.setOnClickListener(this);
        button_reset.setOnClickListener(this);
    }


    private void handlePicklistKeypadEditor() {

        text_destination_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String crate = text_destination_crate.getText().toString();
                    if(crate.length()>0) {
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });

        text_destination_crate.addTextChangedListener(new TextWatcher() {
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
                String crate = s.toString();
                if(crate.length()>0) {
                    validateCrate(crate);
                }
            }
        });

        text_article_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String  articlebcode = text_article_no.getText().toString();
                    if(articlebcode.length()>0) {
                        validateArticleNoandScan(articlebcode);
                        return true;
                    }
                }
                return false;
            }
        });

        text_article_no.addTextChangedListener(new TextWatcher() {
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
                String articlebcode = s.toString();
                if(articlebcode.length()>0) {
                    validateArticleNoandScan(articlebcode);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {

        CommonUtils.hideKeyboard(activity);
        switch (view.getId()){
            case R.id.button_save_qcputaway:
                    validateFormAndSave();
                break;
            case R.id.button_reset_qcputaway:
                    resetFields();
                    text_destination_crate.setEnabled(true);
                    text_destination_crate.requestFocus();
                break;
        }
    }

    private void resetFields(){

        text_destination_crate.setText("");
        text_article_no.setText("");
        text_total_scan.setText("");
        text_destination_crate.setEnabled(false);
        text_article_no.setEnabled(false);
    }

    private void validateDesitationPlant(String mode){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.CANCELPUT_VALIDATE_PLANT);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_NATURE", mode);

            ET_RECORDS = new HashMap<>();
            ET_EAN_RECORDS = new HashMap<>();

            resetFields();

            showProcessingAndSubmit(Vars.CANCELPUT_VALIDATE_PLANT, VALIDATE_PLANT_REQUEST, args);

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

    private void validateCrate(String crate){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.CANCELPUT_VALIDATE_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());

            text_article_no.setText("");
            text_article_no.setEnabled(false);
            validatedCrate = crate.toUpperCase();

            showProcessingAndSubmit(Vars.CANCELPUT_VALIDATE_CRATE, VALIDATE_CRATE_REQUEST, args);
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

    private void validateFormAndSave() {
        String totalScan = text_total_scan.getText().toString().trim();

        if(totalScan.length() == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid Input","Total scan quantity is required should be greater than 0");
            return;
        }

        int totalQty = Integer.parseInt(totalScan);
        if(totalQty == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid Input","Total scan quantity should be greater than 0");
            return;
        }

        if(validatedCrate == null || validatedCrate.length() == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid Input","Valid crate value is required");
            return;
        }

        if(ET_RECORDS.size() == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid Input","Nothing to save. Invalid destination plant. Please try validating destination plant again");
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.CANCELPUT_SAVE);
            args.put("IM_CRATE", validatedCrate);
            args.put("IM_NATURE", radio_failed.isChecked() ? Vars.PUTAWAY_MODE_QC_FAILED : Vars.PUTAWAY_MODE_CANCEL);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);

            JSONArray etData = new JSONArray();

            for (ETData_QcFailedCancel ETData: ET_RECORDS.values()) {
                float scannedQty = Float.parseFloat(ETData.getScanqty());
                if( scannedQty >= 1) {
                    ETData.setScanqty(String.format("%.3f", scannedQty));
                    String ET_Data_JsonString = new Gson().toJson(ETData);
                    JSONObject ET_DATA = new JSONObject(ET_Data_JsonString);
                    etData.put(ET_DATA);
                }
            }

            if(etData.length() == 0){
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid Request", "Nothing to save. Please scan some articles");
                return;
            }

            args.put("IT_DATA", etData);
            showProcessingAndSubmit(Vars.CANCELPUT_SAVE,SAVE_REQUEST,args);
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

                                        if (request == VALIDATE_PLANT_REQUEST) {

                                            radio_failed.setChecked(false);
                                            radio_cancel.setChecked(false);
                                            radio_failed.requestFocus();

                                        } else if (request == VALIDATE_CRATE_REQUEST) {

                                            text_destination_crate.setText("");
                                            validatedCrate = "";

                                        }

                                        return;
                                    } else {

                                        if (request == VALIDATE_PLANT_REQUEST) {

                                            setETAndEANData(responsebody);

                                        } else if (request == VALIDATE_CRATE_REQUEST) {

                                            text_article_no.setEnabled(true);
                                            text_article_no.requestFocus();

                                        } else if (request == SAVE_REQUEST) {

                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    getFragmentManager().popBackStack();
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

    public void setETAndEANData(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    ETData_QcFailedCancel ET_DATA = new ETData_QcFailedCancel();

                    ET_DATA.setLgnum(ET_RECORD.getString("LGNUM"));
                    ET_DATA.setLgpla(ET_RECORD.getString("LGPLA"));
                    ET_DATA.setLgtyp(ET_RECORD.getString("LGTYP"));
                    ET_DATA.setMaktx(ET_RECORD.getString("MAKTX"));
                    ET_DATA.setMaterial(ET_RECORD.getString("MATERIAL"));
                    ET_DATA.setMatnr(ET_RECORD.getString("MATNR"));
                    ET_DATA.setMenge(ET_RECORD.getString("MENGE"));
                    ET_DATA.setScanqty(ET_RECORD.getString("SCANQTY"));
                    ET_DATA.setWerks(ET_RECORD.getString("WERKS"));

                    ET_RECORDS.put(ET_RECORD.getString("MATNR"),ET_DATA);
                }
            }
            JSONArray ET_EAN_DATA_ARRAY = responsebody.getJSONArray("ET_EAN_DATA");
            int totalEanRecords = ET_EAN_DATA_ARRAY.length() - 1;
            if(totalEanRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEanRecords; recordIndex++){
                    JSONObject ET_EAN_RECORD  = ET_EAN_DATA_ARRAY.getJSONObject(recordIndex+1);
                    ET_EAN_RECORDS.put(ET_EAN_RECORD.getString("EAN11"),ET_EAN_RECORD);
                }
            }
            text_destination_crate.setEnabled(true);
            text_destination_crate.requestFocus();
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void validateArticleNoandScan(String articlebcode) {
        try {
            //Find article by EAN-11 from ET_EAN_RECORDS Map
            JSONObject ET_EAN_RECORD = ET_EAN_RECORDS.get(articlebcode);

            if (ET_EAN_RECORD != null) {

                //If found fetch MATNR from the found record
                String matnr = ET_EAN_RECORD.getString("MATNR");

                //Find in ET_DATA Map by MATNR
                ETData_QcFailedCancel ET_DATA = ET_RECORDS.get(matnr);

                if (ET_DATA != null) {

                    //If found fetch MENGE from the found ET_DATA record
                    int allowedQty = (int)Float.parseFloat(ET_DATA.getMenge());
                    int scannedQty = (int)Float.parseFloat(ET_DATA.getScanqty());

                    String txtTotalScan = text_total_scan.getText().toString();
                    int qty = txtTotalScan.length() == 0 ? 0 : Integer.parseInt(txtTotalScan);

                    if(scannedQty == allowedQty){
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Abort", "Scan limit exceeded for " + articlebcode + ". Maximum scan allowed is: "+allowedQty);
                    }else{
                        scannedQty = scannedQty + 1;

                        String totQty = (qty+1)+"";
                        text_total_scan.setText(totQty);

                        ET_DATA.setScanqty(scannedQty+"");
                        ET_RECORDS.replace(matnr,ET_DATA);
                    }

                }else {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Invalid Input", "Material No :" + matnr + " not found.");
                }
            } else {
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid Input", "Article No :" + articlebcode + " not found.");
            }
            text_article_no.setText("");
            text_article_no.requestFocus();
        }
        catch (JSONException e) {
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