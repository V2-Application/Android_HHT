package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
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
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.putwayinbin.FragmentDirectBin;
import com.v2retail.dotvik.dc.putwayinbin.PalletHU;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentHuPickingFromMSA#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentHuPickingFromMSA extends  Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_PICKLIST = 1501;
    private static final int REQUEST_SAVE = 1502;

    private static final String TAG = FragmentHuPickingFromMSA.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_next, btn_submit;
    EditText txt_picklistno, txt_tq, txt_sq, txt_rq, txt_cur_binno, txt_cur_huno, txt_cur_sqty, txt_scan_binno, txt_scan_huno, txt_scan_sqty;
    TextView tv_picklistno;

    LinearLayout llPicklistNo,llNextScreen;
    TableLayout tableItems;

    int totalScanned = 0;
    int totalQty = 0;

    List<PicklistData> picklistData;
    List<PicklistData> scannedData;
    PicklistData currentPickData;

    public FragmentHuPickingFromMSA() {
        // Required empty public constructor
    }

    public static FragmentHuPickingFromMSA newInstance(String param1, String param2) {
        FragmentHuPickingFromMSA fragment = new FragmentHuPickingFromMSA();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("HU Picking From MSA-Scanning");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_hu_picking_from_msa, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_picklistno = rootView.findViewById(R.id.txt_hu_picking_from_msa_picklistno);
        tv_picklistno = rootView.findViewById(R.id.tv_hu_picking_from_msa_picklistno);
        txt_tq = rootView.findViewById(R.id.txt_hu_picking_from_msa_tq);
        txt_sq = rootView.findViewById(R.id.txt_hu_picking_from_msa_sq);
        txt_rq = rootView.findViewById(R.id.txt_hu_picking_from_msa_rq);
        txt_cur_binno = rootView.findViewById(R.id.txt_hu_picking_from_msa_curr_bin);
        txt_cur_huno = rootView.findViewById(R.id.txt_hu_picking_from_msa_curr_hu);
        txt_cur_sqty = rootView.findViewById(R.id.txt_hu_picking_from_msa_curr_sqty);
        txt_scan_binno = rootView.findViewById(R.id.txt_hu_picking_from_msa_scan_bin);
        txt_scan_huno = rootView.findViewById(R.id.txt_hu_picking_from_msa_scan_hu);
        txt_scan_sqty = rootView.findViewById(R.id.txt_hu_picking_from_msa_scan_sqty);

        llPicklistNo = rootView.findViewById(R.id.ll_hu_picking_from_msa_picklistno);
        llNextScreen = rootView.findViewById(R.id.ll_hu_picking_from_msa_next_screen);

        tableItems = rootView.findViewById(R.id.table_hu_picking_from_msa_items);

        btn_back = rootView.findViewById(R.id.btn_hu_picking_from_msa_back);
        btn_next = rootView.findViewById(R.id.btn_hu_picking_from_msa_next);
        btn_submit = rootView.findViewById(R.id.btn_hu_picking_from_msa_submit);

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_submit.setOnClickListener(this);

        clear(true);
        addInputEvents();

        return rootView;
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_hu_picking_from_msa_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_hu_picking_from_msa_next:
                if(!UIFuncs.toUpperTrim(txt_picklistno).isEmpty()){
                    validatePicklistNo(UIFuncs.toUpperTrim(txt_picklistno));
                }
                break;
            case R.id.btn_hu_picking_from_msa_submit:
                if(!scannedData.isEmpty()){
                    saveData();
                }else{
                    box.getBox("No Data", "Nothing to save, please scan some data");
                }
                break;
        }
    }
    private void addInputEvents() {
        txt_picklistno.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_picklistno);
                    if (!value.isEmpty()) {
                        validatePicklistNo(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_picklistno.addTextChangedListener(new TextWatcher() {
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
                    validatePicklistNo(value);
                }
            }
        });
        txt_scan_binno.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_binno);
                    if (!value.isEmpty()) {
                        validateBinNo(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_binno.addTextChangedListener(new TextWatcher() {
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
                    validateBinNo(value);
                }
            }
        });
        txt_scan_huno.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_huno);
                    if (!value.isEmpty()) {
                        validateHU(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_huno.addTextChangedListener(new TextWatcher() {
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
                    validateHU(value);
                }
            }
        });
    }

    private void clear(boolean clearAll) {
        picklistData = new ArrayList<>();
        scannedData = new ArrayList<>();
        step2();
        if(clearAll){
            llPicklistNo.setVisibility(View.VISIBLE);
            llNextScreen.setVisibility(View.GONE);
            UIFuncs.enableInput(con, txt_picklistno);
            btn_next.setVisibility(View.VISIBLE);
            btn_submit.setVisibility(View.GONE);
            totalScanned = 0;
            txt_picklistno.setText("");
            tv_picklistno.setText("");
            UIFuncs.enableInput(con, txt_picklistno);
        }else{
            validatePicklistNo(UIFuncs.toUpperTrim(txt_picklistno));
        }
    }

    private void step2(){
        tv_picklistno.setText(UIFuncs.toUpperTrim(txt_picklistno));
        txt_tq.setText("");
        txt_sq.setText("");
        txt_rq.setText("");
        txt_cur_huno.setText("");
        txt_cur_binno.setText("");
        txt_cur_sqty.setText("");
        txt_scan_binno.setText("");
        txt_scan_huno.setText("");
        txt_scan_sqty.setText("");
        tableItems.removeAllViews();
        llPicklistNo.setVisibility(View.GONE);
        llNextScreen.setVisibility(View.VISIBLE);
        btn_next.setVisibility(View.GONE);
        btn_submit.setVisibility(View.VISIBLE);
        UIFuncs.disableInput(con, txt_scan_huno);
        UIFuncs.enableInput(con, txt_scan_binno);
    }

    private void validatePicklistNo(String picklistno){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PICKLIST_PPPN);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", picklistno);
            showProcessingAndSubmit(Vars.ZWM_HU_VALIDATION_PUT, REQUEST_VALIDATE_PICKLIST, args);
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

    private void setData(JSONObject responsebody){
        try {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int length = ET_DATA_ARRAY.length();
            for(int i = 1; i < length; i++){
                PicklistData data = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(i).toString(), PicklistData.class);
                data.setHuno(UIFuncs.removeLeadingZeros(data.getHuno()));
                picklistData.add(data);
            }
            if(picklistData.size() > 0){
                totalScanned = 0;
                totalQty = picklistData.size();
                step2();
                populateTableData();
            }else{
                AlertBox box = new AlertBox(getContext());
                box.getBox("No Data", "Picklist is empty.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear(true);
                    }
                });
            }
        }catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void updateScanStats(){
        txt_tq.setText(totalQty + "");
        txt_sq.setText(totalScanned + "");
        txt_rq.setText((totalQty - totalScanned) + "");
    }

    private void setLastScanedItem(){
        txt_cur_binno.setText("");
        txt_cur_huno.setText("");
        txt_cur_sqty.setText("");
        UIFuncs.disableInput(con, txt_scan_binno);
        UIFuncs.disableInput(con, txt_scan_huno);
        txt_cur_binno.setText(currentPickData.getBinno());
        txt_cur_huno.setText(UIFuncs.toUpperTrim(txt_scan_huno));
        txt_cur_sqty.setText("1");
        txt_scan_binno.setText("");
        txt_scan_huno.setText("");
        txt_scan_sqty.setText("");
        updateScanStats();
        if(!picklistData.isEmpty()){
            scannedData.add(currentPickData);
            removeItem();
        }
    }

    private void removeItem(){
        Iterator<PicklistData> iterator = picklistData.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(currentPickData)) {
                iterator.remove();
                break;
            }
        }
        currentPickData = new PicklistData();
        populateTableData();
        UIFuncs.enableInput(con, txt_scan_binno);
    }

    private void populateTableData(){
        tableItems.removeAllViews();
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 16;
        textSize = 14;

        TextView headerBin = new TextView(getContext());
        TextView headerHuNo = new TextView(getContext());
        TextView headerExHuNo = new TextView(getContext());

        headerBin.setLayoutParams(new TableRow.LayoutParams(
                350,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        headerBin.setGravity(Gravity.CENTER);
        headerBin.setPadding(0,5,0,5);
        headerBin.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerBin.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerBin.setText("Bin");

        headerHuNo.setGravity(Gravity.CENTER);
        headerHuNo.setPadding(0,5,0,5);
        headerHuNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerHuNo.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerHuNo.setText("HU No");

        headerExHuNo.setGravity(Gravity.CENTER);
        headerExHuNo.setPadding(0,5,0,5);
        headerExHuNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerExHuNo.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerExHuNo.setText("EX HU");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerBin);
        tr.addView(headerHuNo);
        tr.addView(headerExHuNo);
        tableItems.addView(tr, trParams);

        //Create Data Rows in Table
        int rowNum = 1;
        for (PicklistData data :picklistData) {
            TextView tvBin = new TextView(getContext());
            tvBin.setText(data.getBinno());
            tvBin.setTextSize(textSize);
            tvBin.setPadding(5,2,0,2);
            tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvHu = new TextView(getContext());
            tvHu.setText(data.getHuno());
            tvHu.setTextSize(textSize);
            tvHu.setPadding(5,2,0,2);
            tvHu.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvExHu = new TextView(getContext());
            tvExHu.setText(data.getExHu());
            tvExHu.setTextSize(textSize);
            tvExHu.setPadding(5,2,0,2);
            tvExHu.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            tr = new TableRow(getContext());
            tr.setId(rowNum);
            tr.setPadding(0,0,0,0);
            tr.setLayoutParams(trParams);
            tr.addView(tvBin);
            tr.addView(tvHu);
            tr.addView(tvExHu);
            tr.setTag(data);
            tableItems.addView(tr, trParams);
            rowNum++;
        }
    }

    private void validateBinNo(String binno){
        boolean binFound = false;
        for (PicklistData data: picklistData) {
            if(data.getBinno().equalsIgnoreCase(binno)){
                UIFuncs.enableInput(con, txt_scan_huno);
                binFound = true;
                break;
            }
        }
        if(!binFound){
            txt_scan_binno.setText("");
            UIFuncs.errorSound(con);
            box.getBox("Invalid Bin", "Invalid BIN, please check below table for allowed BINs");
            txt_scan_binno.requestFocus();
        }
    }

    private void validateHU(String huno){
        boolean huFound = false;
        for (PicklistData data: picklistData) {
            if(data.binno.equalsIgnoreCase(UIFuncs.toUpperTrim(txt_scan_binno)) && (data.getHuno().equalsIgnoreCase(huno) || data.getExHu().equalsIgnoreCase(huno))){
                txt_scan_sqty.setText("1");
                totalScanned += 1;
                currentPickData = new PicklistData().newInstance(data);
                huFound = true;
                setLastScanedItem();
                break;
            }
        }
        if(!huFound){
            txt_scan_huno.setText("");
            UIFuncs.errorSound(con);
            box.getBox("Invalid HU", "Invalid HU, please check below table for allowed HUs");
            txt_scan_huno.requestFocus();
        }
    }

    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (PicklistData data : scannedData) {
                String scanDataJsonString = new Gson().toJson(data);
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some articles");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    private void saveData() {
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave != null) {
            try {
                args.put("bapiname", Vars.ZWM_PICK_SAVE_RFC);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("ET_SAVE", dataToSave);
                showProcessingAndSubmit(Vars.ZWM_PICK_SAVE_RFC, REQUEST_SAVE, args);
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

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

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

    private void submitRequest(String rfc, int request, JSONObject args) {

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
                if (dialog != null) {
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
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_PICKLIST) {
                                            txt_picklistno.setText("");
                                            txt_picklistno.requestFocus();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_PICKLIST) {
                                            setData(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    clear(true);
                                                }
                                            });
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
            if (dialog != null) {
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

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }

    class PicklistData{
        @SerializedName("PICKLIST_NO")
        private String picklistno;
        @SerializedName("SAP_HU")
        private String huno;
        @SerializedName("STORAGE_BIN")
        private String binno;
        @SerializedName("DEST_STORE")
        private String dest;
        @SerializedName("EX_HU")
        private String exHu;

        public PicklistData(){

        }
        public PicklistData newInstance(PicklistData source) {
            PicklistData data = new PicklistData();
            data.setHuno(source.getHuno());
            data.setBinno(source.getBinno());
            data.setDest(source.getDest());
            data.setPicklistno(source.getPicklistno());
            data.setExHu(source.getExHu());
            return data;
        }
        public String getPicklistno() {
            return picklistno;
        }

        public void setPicklistno(String picklistno) {
            this.picklistno = picklistno;
        }

        public String getHuno() {
            return huno;
        }

        public void setHuno(String huno) {
            this.huno = huno;
        }

        public String getBinno() {
            return binno;
        }

        public void setBinno(String binno) {
            this.binno = binno;
        }

        public String getDest() {
            return dest;
        }

        public void setDest(String dest) {
            this.dest = dest;
        }

        public String getExHu() {
            return exHu;
        }

        public void setExHu(String exHu) {
            this.exHu = exHu;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            PicklistData data = (PicklistData) obj;
            return binno.equals(data.binno) && huno.equals(data.huno) && exHu.equals(data.exHu);
        }
    }
}