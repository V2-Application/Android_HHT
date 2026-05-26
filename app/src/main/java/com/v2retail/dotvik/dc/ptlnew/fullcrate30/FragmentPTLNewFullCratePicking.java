package com.v2retail.dotvik.dc.ptlnew.fullcrate30;

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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.BinCrateData;
import com.v2retail.dotvik.dc.ptlnew.ETPickData;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.dc.ptlnew.withoutpallate.FragmentPTLNewWithoutPallatePicking;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentPTLNewFullCratePicking extends Fragment implements View.OnClickListener {

    private static final int REQUEST_BIN_DATA = 1500;
    private static final int REQUEST_GET_PICKLIST_DATA = 1501;
    private static final int REQUEST_VALIDATE_MSA_BIN = 1502;
    private static final int REQUEST_VALIDATE_MSA_CRATE = 1503;

    private static final String TAG = FragmentPTLNewFullCratePicking.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    Button btn_back, btn_next;
    LinearLayout ll_screen_1, ll_screen_2;
    EditText txt_picklist, txt_floor, txt_section, txt_scan_bin;
    EditText txt_picklistno, txt_scanned_bin, txt_scan_msa_bin, txt_scanned_msa_bin, txt_scan_msa_crate, txt_scanned_msa_crate;
    TableLayout table_bin_crate;

    Map<String, BinCrateData> binDataMap = new LinkedHashMap<>();
    Map<String, PicklistData> picklistDataMap = new HashMap<>();

    int totalScanned = 0;

    public FragmentPTLNewFullCratePicking() {
        // Required empty public constructor
    }

    public static FragmentPTLNewFullCratePicking newInstance() {
        return new FragmentPTLNewFullCratePicking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL Picking - Full Crate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ptl_new_full_crate_picking, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        txt_scan_bin = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_scan_floor_bin);
        txt_floor = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_floor);
        txt_section = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_section);
        txt_picklist = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_picklist);

        txt_picklistno = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_picklistno);
        txt_scanned_bin = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_scanned_bin_tot_bin);
        txt_scan_msa_bin = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_msa_bin);
        txt_scanned_msa_bin = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_msa_bin_scanned);
        txt_scan_msa_crate = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_msa_crate);
        txt_scanned_msa_crate = rootView.findViewById(R.id.txt_ptl_new_picking_full_crate_msa_crate_scanned);

        table_bin_crate = rootView.findViewById(R.id.table_ptl_new_picking_full_crate_msa_bin_crate);

        ll_screen_1 = rootView.findViewById(R.id.ll_ptl_new_picking_full_crate_screen_1);
        ll_screen_2 = rootView.findViewById(R.id.ll_ptl_new_picking_full_crate_screen_2);

        btn_back =  rootView.findViewById(R.id.btn_ptl_new_picking_full_crate_back);
        btn_next =  rootView.findViewById(R.id.btn_ptl_new_picking_full_crate_next);

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);

        addInputEvents();
        clear();

        return rootView;
    }

    private void addInputEvents(){
        txt_scan_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_bin);
                    if (!value.isEmpty()) {
                        validateBin(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_bin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateBin(value);
                }
            }
        });
        txt_scan_msa_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_msa_crate);
                    if (!value.isEmpty()) {
                        validateMSACrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_msa_crate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateMSACrate(value);
                }
            }
        });
        txt_scan_msa_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_msa_bin);
                    if (!value.isEmpty()) {
                        validateMsaBin(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_msa_bin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateMsaBin(value);
                }
            }
        });
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_picking_full_crate_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_ptl_new_picking_full_crate_next:
                if(!UIFuncs.toUpperTrim(txt_picklist).isEmpty()){
                    getPicklistListData(UIFuncs.toUpperTrim(txt_picklist));
                }
                break;
        }
    }

    private void clear(){
        step2("");
        step1();
    }

    private void step1(){
        ll_screen_1.setVisibility(View.VISIBLE);
        ll_screen_2.setVisibility(View.GONE);
        txt_scan_bin.setText("");
        txt_section.setText("");
        txt_floor.setText("");
        txt_picklist.setText("");
        UIFuncs.enableInput(con, txt_scan_bin);
    }

    private void step2(String step){
        totalScanned = 0;
        ll_screen_1.setVisibility(View.GONE);
        ll_screen_2.setVisibility(View.VISIBLE);
        txt_scanned_bin.setText(totalScanned + " / " + picklistDataMap.size());
        txt_scan_msa_bin.setText("");
        txt_scanned_msa_bin.setText("");
        txt_scan_msa_crate.setText("");
        txt_scanned_msa_crate.setText("");
        txt_picklistno.setText("");
        UIFuncs.disableInput(con, txt_scanned_bin);
        populateBinCrateTable();
        txt_scan_msa_bin.requestFocus();
    }

    //Step 1
    private void validateBin(String bin){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZPTL_GET_DATA_FROM_BIN_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZPTL_GET_DATA_FROM_BIN_RFC, REQUEST_BIN_DATA, args);
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

    private void setBinData(JSONObject responsebody){
        try
        {
            binDataMap = new LinkedHashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    BinCrateData binCrateData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), BinCrateData.class);
                    binDataMap.put(binCrateData.getBin()+"-"+binCrateData.getCrate(), binCrateData);
                    if(recordIndex == 1){
                        txt_floor.setText(binCrateData.getFloor());
                        txt_section.setText(binCrateData.getSection());
                        txt_picklist.setText(binCrateData.getPicklist());
                    }
                }
            }
            if(!binDataMap.isEmpty()){
                btn_next.requestFocus();
            }else{
                box.getBox("Empty", "Picklist Data is Empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void getPicklistListData(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GET_TO_DETAILS);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_GET_TO_DETAILS, REQUEST_GET_PICKLIST_DATA, args);
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

    public void setPicklistListData(JSONObject responsebody) {
        try {
            picklistDataMap = new LinkedHashMap<>();
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if (totalEtRecords > 0) {
                for (int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++) {
                    PicklistData picklistData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), PicklistData.class);
                    picklistDataMap.put(picklistData.getBin() + "-" + picklistData.getCrate(), picklistData);
                }
            }
            if (!picklistDataMap.isEmpty()) {
                step2("");
                txt_picklistno.setText(UIFuncs.toUpperTrim(txt_picklist));
            } else {
                box.getBox("Empty", "Picklist Data is Empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void populateBinCrateTable() {
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 16;
        textSize = 15;

        table_bin_crate.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerBin = new TextView(getContext());
        TextView headerCrate = new TextView(getContext());
        headerSno.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        headerSno.setGravity(Gravity.CENTER);
        headerSno.setPadding(5,5,0,5);
        headerSno.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSno.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSno.setText(" # ");

        headerBin.setGravity(Gravity.CENTER);
        headerBin.setPadding(0,5,0,5);
        headerBin.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerBin.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerBin.setText("Bin");

        headerCrate.setGravity(Gravity.CENTER);
        headerCrate.setPadding(0,5,0,5);
        headerCrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerCrate.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerCrate.setText("Crate");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerSno);
        tr.addView(headerBin);
        tr.addView(headerCrate);
        table_bin_crate.addView(tr, trParams);

        int rowNum = 1;
        for (Map.Entry<String, PicklistData> picklistDataEntry :picklistDataMap.entrySet()) {
            PicklistData data = picklistDataEntry.getValue();
            TextView tvSno = new TextView(getContext());
            tvSno.setText(rowNum+"");
            tvSno.setTextSize(textSize);
            tvSno.setPadding(5,2,0,2);
            tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvBin = new TextView(getContext());
            tvBin.setText(data.getBin());
            tvBin.setTextSize(textSize);
            tvBin.setPadding(5,2,0,2);
            tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvCrate = new TextView(getContext());
            tvCrate.setText(data.getCrate());
            tvCrate.setTextSize(textSize);
            tvCrate.setPadding(5,2,0,2);
            tvCrate.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            tr = new TableRow(getContext());
            tr.setId(rowNum);
            tr.setPadding(0,0,0,0);
            tr.setLayoutParams(trParams);
            tr.addView(tvSno);
            tr.addView(tvBin);
            tr.addView(tvCrate);
            tr.setTag(data);
            table_bin_crate.addView(tr, trParams);
            rowNum++;
        }
    }

    //Step 2
    private void validateMsaBin(String bin){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_BIN_VALIDATE_V3);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZWM_PTL_BIN_VALIDATE_V3, REQUEST_VALIDATE_MSA_BIN, args);
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

    private void validateMSACrate(String value){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_MSA_CRATE_VALIDATE_V3);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", UIFuncs.toUpperTrim(txt_picklistno));
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_scanned_msa_bin));
            args.put("IM_MSA_CRATE", value);
            showProcessingAndSubmit(Vars.ZWM_PTL_MSA_CRATE_VALIDATE_V3, REQUEST_VALIDATE_MSA_CRATE, args);
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

    private void clearFieldsForNextScan(){
        totalScanned++;
        String key = UIFuncs.toUpperTrim(txt_scanned_msa_bin)+"-"+UIFuncs.toUpperTrim(txt_scan_msa_crate);
        picklistDataMap.remove(key);
        txt_scanned_msa_crate.setText(UIFuncs.toUpperTrim(txt_scan_msa_crate));
        txt_scan_msa_crate.setText("");
        txt_scanned_bin.setText(totalScanned + " / " + picklistDataMap.size());
        populateBinCrateTable();
        txt_scan_msa_bin.requestFocus();
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
                                        if (request == REQUEST_BIN_DATA) {
                                            txt_scan_bin.setText("");
                                            txt_scan_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_BIN) {
                                            txt_scan_msa_bin.setText("");
                                            txt_scan_msa_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_CRATE) {
                                            txt_scan_msa_crate.setText("");
                                            txt_scan_msa_crate.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_BIN_DATA) {
                                            setBinData(responsebody);
                                        }
                                        else if (request == REQUEST_GET_PICKLIST_DATA) {
                                            setPicklistListData(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_BIN) {
                                            txt_scanned_msa_bin.setText(UIFuncs.toUpperTrim(txt_scan_msa_bin));
                                            txt_scan_msa_bin.setText("");
                                            txt_scan_msa_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MSA_CRATE) {
                                            clearFieldsForNextScan();
                                        }
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