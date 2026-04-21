package com.v2retail.dotvik.store.directpicking;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.dc.ptlnew.ZoneStation;
import com.v2retail.dotvik.dc.ptlnew.withoutpallate.FragmentPTLNewWithoutPallatePutwayStorewise;
import com.v2retail.dotvik.modal.FloorBarcode;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentDirectPickingV01ArticlePutway0001 extends Fragment implements View.OnClickListener {

    private static final int REQUEST_FLOOR_LIST = 1500;
    private static final int REQUEST_VALIDATE_BARCODE = 1501;
    private static final int REQUEST_SAVE = 1502;

    private static final String TAG = FragmentPTLNewWithoutPallatePutwayStorewise.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    List<String> floors = new ArrayList<String>();
    ArrayAdapter<String> floorAdapter;

    boolean spinnerTouched = false;

    Spinner dd_floor_list;

    Button btn_back, btn_save;
    EditText txt_store, txt_scan_barcode, txt_article, txt_article_type, txt_scan_qty;

    Map<String, FloorBarcode> barcodeDataMap = new HashMap<>();

    public FragmentDirectPickingV01ArticlePutway0001() {
        // Required empty public constructor
    }

    public static FragmentDirectPickingV01ArticlePutway0001 newInstance() {
        return new FragmentDirectPickingV01ArticlePutway0001();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Article Putway To 0001(V09 To 0001)");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_direct_picking_v01_article_putway0001, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        FragmentActivity activity = getActivity();

        dd_floor_list = rootView.findViewById(R.id.dd_direct_picking_v01_article_putway_0001_floor);
        dd_floor_list.setSelection(0);

        txt_store = rootView.findViewById(R.id.txt_direct_picking_v01_article_putway_0001_store);
        txt_scan_barcode = rootView.findViewById(R.id.txt_direct_picking_v01_article_putway_0001_scan_barcode);
        txt_article = rootView.findViewById(R.id.txt_direct_picking_v01_article_putway_0001_article);
        txt_article_type = rootView.findViewById(R.id.txt_direct_picking_v01_article_putway_0001_article_type);
        txt_scan_qty = rootView.findViewById(R.id.txt_direct_picking_v01_article_putway_0001_sqty);

        btn_back = rootView.findViewById(R.id.btn_direct_picking_v01_article_putway_0001_back);
        btn_save = rootView.findViewById(R.id.btn_direct_picking_v01_article_putway_0001_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        floorAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, floors);
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_floor_list.setAdapter(floorAdapter);
        dd_floor_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_direct_picking_v01_article_putway_0001_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_direct_picking_v01_article_putway_0001_save:
                save();
                break;
        }
    }

    private void clear(){
        barcodeDataMap = new HashMap<>();
        txt_scan_qty.setText("");
        UIFuncs.disableInput(con, txt_scan_barcode);
        txt_store.setText(WERKS);
        txt_article.setText("");
        txt_article_type.setText("");
        txt_scan_qty.setText("");
        txt_scan_barcode.setText("");
        getFLoorList();
    }

    private void addInputEvents(){
        dd_floor_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinnerTouched) {
                    if(dd_floor_list.getSelectedItem() != null && dd_floor_list.getSelectedItemPosition() > 0 && !dd_floor_list.getSelectedItem().toString().isEmpty()){
                        UIFuncs.enableInput(con, txt_scan_barcode);
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        txt_scan_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_barcode);
                    if (!value.isEmpty()) {
                        if(dd_floor_list.getSelectedItem() !=null && dd_floor_list.getSelectedItemPosition() > 0 && !dd_floor_list.getSelectedItem().toString().isEmpty()){
                            validateBarcode(value, dd_floor_list.getSelectedItem().toString());
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_barcode.addTextChangedListener(new TextWatcher() {
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
                    if(dd_floor_list.getSelectedItem() !=null && !dd_floor_list.getSelectedItem().toString().isEmpty()){
                        validateBarcode(value, dd_floor_list.getSelectedItem().toString());
                    }
                }
            }
        });

    }

    private void getFLoorList(){
        JSONObject args = new JSONObject();
        try {
            String rfc = Vars.ZSDC_DIRECT_FLR_RFC;
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            showProcessingAndSubmit(rfc, REQUEST_FLOOR_LIST, args);
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

    public void setFloorList(JSONObject responsebody){
        try
        {
            JSONArray IT_DATA_ARRAY = responsebody.getJSONArray("IT_DATA");
            int totalItRecords = IT_DATA_ARRAY.length() - 1;
            if(totalItRecords > 0){
                floors.clear();
                floors.add("Select Floor");
                for(int recordIndex = 0; recordIndex < totalItRecords; recordIndex++){
                    JSONObject IT_RECORD  = IT_DATA_ARRAY.getJSONObject(recordIndex+1);
                    floors.add(IT_RECORD.getString("FLOOR"));
                }
                ((BaseAdapter) dd_floor_list.getAdapter()).notifyDataSetChanged();
                dd_floor_list.setEnabled(true);
                dd_floor_list.invalidate();
                dd_floor_list.setSelection(0);
                dd_floor_list.requestFocus();
            }else{
                box.getBox("No Floors Found", "No records returned by the server");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void validateBarcode(String barcode, String floor){
        if(barcodeDataMap.containsKey(barcode))
        {
            updateQtyAfterScan(UIFuncs.toUpperTrim(txt_scan_barcode));
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
        }else{
            JSONObject args = new JSONObject();
            try {
                String rfc = Vars.ZSDC_DIRECT_ART_VAL_BARCOD_RFC;
                args.put("bapiname", rfc);
                args.put("IM_USER", USER);
                args.put("IM_STORE_CODE", WERKS);
                args.put("IM_FLOOR", floor);
                args.put("IM_BARCODE", barcode);
                showProcessingAndSubmit(rfc, REQUEST_VALIDATE_BARCODE, args);
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
    }

    public void setBarcodeData(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    FloorBarcode barcodeData = new Gson().fromJson(ET_RECORD.toString(), FloorBarcode.class);
                    barcodeDataMap.put(barcodeData.getBarcode(), barcodeData);
                }
                updateQtyAfterScan(UIFuncs.toUpperTrim(txt_scan_barcode));
            }else{
                box.getBox("No Records", "No records returned by the server");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private void updateQtyAfterScan(String barcode){
        FloorBarcode barcodeData = null;
        if(barcodeDataMap.containsKey(barcode)){
            barcodeData = barcodeDataMap.get(barcode);
            double sqty  = Util.convertStringToDouble(barcodeData.getScanQty());
            double rqty  = Util.convertStringToDouble(barcodeData.getVerme());
            // Use UMREZ from RFC response — pack conversion ratio per barcode scan
            // e.g. UMREZ=4 means one scan = 4 units (not 1)
            double umrez = Util.convertStringToDouble(barcodeData.getUmrez());
            if (umrez <= 0) umrez = 1; // safety fallback if SAP returns 0
            sqty = sqty + umrez;
            if(sqty > rqty){
                box.getBox("Invalid", "Already scanned maximum allowed Qty " + rqty);
                return;
            }
            txt_article.setText(barcodeData.getMatnr());
            txt_scan_qty.setText(Util.formatDouble(sqty));
            barcodeData.setScanQty(Util.formatDouble(sqty));
            return;
        }else{
            box.getBox("Invalid", "Scanned Barcode is invalid and not available in Records");
        }
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }


    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for(Map.Entry<String, FloorBarcode> floorBarcodeEntry: barcodeDataMap.entrySet()) {
                String scanDataJsonString = new Gson().toJson(floorBarcodeEntry.getValue());
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                arrScanData.put(itDataJson);
            }
            return arrScanData;
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    private void save(){
        if(barcodeDataMap.size() == 0){
            box.getBox("Invalid", "No records to submit. Please scan some barcodes");
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if(dataToSave != null){
            try {
                args.put("bapiname", Vars.ZSDC_DIRECT_ART_VAL1_SAVE1_RFC);
                args.put("IM_USER", USER);
                args.put("IM_STORE_CODE", WERKS);
                args.put("ET_DATA", dataToSave);
                showProcessingAndSubmit(Vars.ZSDC_DIRECT_ART_VAL1_SAVE1_RFC, REQUEST_SAVE, args);
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
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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
                                        if (request == REQUEST_VALIDATE_BARCODE) {
                                            txt_scan_barcode.setText("");
                                            txt_scan_barcode.requestFocus();
                                        }
                                    } else {
                                        if(request == REQUEST_FLOOR_LIST){
                                            setFloorList(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_BARCODE) {
                                            setBarcodeData(responsebody);
                                            return;
                                        }
                                        else if (request == REQUEST_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));
                                            clear();
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
}