package com.v2retail.dotvik.store;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.MardModal;
import com.v2retail.dotvik.modal.material.ETPACKMAT;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Scan_GRT_Display_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Scan_GRT_Display_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Scan_GRT_Display_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "source";
    private static final String ARG_PARAM2 = "dest";
    private static final String ARG_PARAM3 = "category";

    private static final int REQUEST_SAVE = 5401;
    private static final int REQUEST_PACK_DATA = 5402;
    private static final int REQUEST_STOCK_DATA = 5403;

    ArrayList<Integer> rows_index = new ArrayList<>();
    // TODO: Rename and change types of parameters
    private ArrayList<String> mParam1_list;
    private ArrayList<String> pac_mat_list = new ArrayList<>();
    ArrayList<ArrayList<String>> rows1;
    private String dest;
    private String source;
    private String category;
    private String scanner;
    private String TAG = Scan_GRT_Display_Fragment.class.getName();

    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;

    EditText date;
    FragmentManager fm;
    Button save;
    Button back;
    String[] arrBarQty;
    //version 11.81
    //Button barcode_scan;
    String URL = "";
    String WERKS = "";
    String USER = "";

    //version 11.81
    //TextView mResponseView;
    Spinner pack_mat_spinner;
    EditText total_sq_et;
    EditText bin_et;
    EditText article_available_stock_et;
    EditText barcode_art_et;
    EditText article_no_et;
    EditText asq_et;
    EditText tsq_et;
    int ScQty = 0;
    int TPOQty = 0;
    int ScanQty = 0;
    int OpenQty = 0;
    int sum = 0;
    String requester = "";
    private boolean requestInProgress = false;

    ArrayList<ArrayList<String>> dtEAN;
    ArrayList<ETPACKMAT> packMaterialRecords;
    Map<String, EtEanDataModel> etEanRecords;
    Map<String, MardModal> etMardRecords;
    Map<String, ScannedData> scannedData;
    Map<String, ScannedData> scannedEanData;

    ArrayList<String> dtBin;
    ArrayList<ArrayList<String>> dtMaterial;
    private OnFragmentInteractionListener mListener;

    public Scan_GRT_Display_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Scan GRT Display");
    }

    public static Scan_GRT_Display_Fragment newInstance(String param1, String param2) {
        Scan_GRT_Display_Fragment fragment = new Scan_GRT_Display_Fragment();
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
            source = getArguments().getString(ARG_PARAM1);
            dest = getArguments().getString(ARG_PARAM2);
            category = getArguments().getString(ARG_PARAM3);
            if (source != null) Log.d(TAG, "Source ->" + source);
            else Log.d(TAG, "Source ->empty");
            if (dest != null) Log.d(TAG, "des ->" + dest);
            else Log.d(TAG, "dest ->empty");
        }
        fm = getFragmentManager();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scan_grt_display, container, false);
        con = getContext();
        dialog = new ProgressDialog(con);
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

        back = (Button) view.findViewById(R.id.back);
        //version 11.81
        //barcode_scan = (Button) view.findViewById(R.id.barcode_scan); commented as view not present in layout xml
        save = (Button) view.findViewById(R.id.save);

        total_sq_et = (EditText) view.findViewById(R.id.total_scan_qty);
        article_no_et = (EditText) view.findViewById(R.id.article_no);
        tsq_et = (EditText) view.findViewById(R.id.tsq);
        asq_et = (EditText) view.findViewById(R.id.asq);
        pack_mat_spinner = (Spinner) view.findViewById(R.id.pack_mat_spinner);
        barcode_art_et = (EditText) view.findViewById(R.id.barcode_no);
        article_available_stock_et = (EditText) view.findViewById(R.id.article_available_stock);
        //version 11.81
        //mResponseView = (TextView) view.findViewById(R.id.response); commented as view not present in layout xml

        pack_mat_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ;
        back.setOnClickListener(this);
        //version 11.81
        //barcode_scan.setOnClickListener(this);
        save.setOnClickListener(this);
        barcode_art_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String value = UIFuncs.toUpperTrim(barcode_art_et);
                    if(!value.isEmpty()){
                        validateEan(value);
                    }
                }
                return false;
            }
        });

        barcode_art_et.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().toUpperCase();
                if(!value.isEmpty() && scannerReading) {
                    validateEan(value);
                }
            }
        });

        try {
            clearData();
            getPACMaterial();
        } catch (Exception e) {
            box.getErrBox(e);
        }

        return view;
    }
    private void clearData(){
        article_no_et.setText("");
        barcode_art_et.setText("");
        tsq_et.setText("");
        asq_et.setText("");
        total_sq_et.setText("");
        article_available_stock_et.setText("");
        etEanRecords = new HashMap<>();
        scannedData = new HashMap<>();
        etMardRecords = new HashMap<>();
        scannedEanData = new HashMap<>();
        barcode_art_et.requestFocus();
    }

    // Clears the Article/Barcode field and returns focus to it so the user can rescan.
    private void resetBarcodeInput() {
        if (barcode_art_et != null) {
            barcode_art_et.setText("");
            barcode_art_et.requestFocus();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.save:
                if(scannedData.size() > 0){
                    saveDataToServer();
                }else{
                    box.getBox("No Data", "No records to save. Please scan some articles");
                    clearData();
                    return;
                }
                break;
            case R.id.back:
                fm.popBackStack();
                break;
            case R.id.barcode_scan:
                barcode_art_et.setText("");
                scanner = "bar";
                if(CameraCheck.isCameraAvailable(con))
                    IntentIntegrator.forSupportFragment(Scan_GRT_Display_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
                break;
        }
    }

    private void getPACMaterial() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_PACKING_MATERIAL);
            args.put("IM_LGNUM", "V2R");
            showProcessingAndSubmit(Vars.ZWM_GET_PACKING_MATERIAL, REQUEST_PACK_DATA, args);
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
    /** Dev JSON RFC adapter returns data rows only; production SAP often prefixes tables with a header row at index 0. */
    private static boolean isPackMatHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String maktx = row.optString("MAKTX", "").trim();
        String matnr = row.optString("MATNR", "").trim();
        return "MAKTX".equalsIgnoreCase(maktx) || "MATNR".equalsIgnoreCase(matnr);
    }

    private static boolean isEanDataHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String matnr = row.optString("MATNR", "").trim();
        String ean11 = row.optString("EAN11", "").trim();
        return "MATNR".equalsIgnoreCase(matnr) || "EAN11".equalsIgnoreCase(ean11);
    }

    private static JSONObject parseReturnObject(JSONObject responsebody) throws JSONException {
        if (!responsebody.has("EX_RETURN")) {
            return null;
        }
        Object raw = responsebody.get("EX_RETURN");
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            JSONObject firstSuccess = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String type = row.optString("TYPE", "").trim();
                if ("E".equals(type) || "A".equals(type)) {
                    return row;
                }
                if (firstSuccess == null) {
                    firstSuccess = row;
                }
            }
            return firstSuccess;
        }
        return null;
    }

    private static boolean isSapErrorReturn(JSONObject returnobj) {
        if (returnobj == null) {
            return false;
        }
        String type = returnobj.optString("TYPE", "").trim();
        return "E".equals(type) || "A".equals(type);
    }

    private void populatePackMaterial(JSONObject responsebody){
        try{
            packMaterialRecords = new ArrayList<>();
            JSONArray arrPackMaterial = responsebody.getJSONArray("ET_PACK_MAT");
            int len = arrPackMaterial.length();
            for (int recordIndex = 0; recordIndex < len; recordIndex++) {
                JSONObject row = arrPackMaterial.getJSONObject(recordIndex);
                if (isPackMatHeaderRow(row)) {
                    continue;
                }
                packMaterialRecords.add(new Gson().fromJson(row.toString(), ETPACKMAT.class));
            }
            if(!packMaterialRecords.isEmpty()){
                ArrayList<String> materials = new ArrayList<>();
                materials.add("0");
                for (ETPACKMAT packMat: packMaterialRecords) {
                    materials.add(UIFuncs.removeLeadingZeros(packMat.getMATNR()));
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(con, android.R.layout.simple_spinner_item, materials);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pack_mat_spinner.setAdapter(dataAdapter);
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
    }

    @SuppressLint("SetTextI18n")
    private void validateEan(String ean){
        ean = ean != null ? ean.trim().toUpperCase() : "";
        if (ean.isEmpty()) {
            return;
        }
        EtEanDataModel eanModel = null;
        if(etEanRecords.containsKey(ean)){
            eanModel = etEanRecords.get(ean);
        }else{
            // Avoid firing a duplicate stock lookup while one is already running
            // (the barcode field has both an editor-action and a text-watcher listener
            // that can each trigger validateEan for the same scan).
            if(requestInProgress){
                return;
            }
            getStoreStockdata(ean);
            return;
        }
        String matnr = eanModel.getMATNR();
        ScannedData data = new ScannedData();
        ScannedData eanData = new ScannedData();

        data.setMaterial(matnr);
        if(scannedData.containsKey(matnr)){
            data = scannedData.get(matnr);
        }

        eanData.setMaterial(ean);
        if(scannedEanData.containsKey(ean)){
            eanData = scannedEanData.get(ean);
        }

        int articleScanQty = data.getQty();
        int eanScanQty = eanData.getQty();
        int requiredQty = (int) Double.parseDouble(etMardRecords.get(matnr).getLabst());
        //int requiredQty = Integer.parseInt(eanModel.getUMREZ());
        int unitQty =  Integer.parseInt(eanModel.getUMREN());
        if((articleScanQty + unitQty) > requiredQty){
            box.getBox("Not Allowed", "Already scanned maximum allowed quantity");
            barcode_art_et.setText("");
            barcode_art_et.requestFocus();
            return;
        }

        eanData.setQty(eanScanQty + unitQty);
        data.setQty(articleScanQty + unitQty);

        ScanQty = ScanQty + unitQty;
        total_sq_et.setText(ScanQty + "");
        article_available_stock_et.setText(requiredQty + "");
        asq_et.setText(eanData.getQty() + "");
        tsq_et.setText(data.getQty() + "");
        article_no_et.setText(matnr);

        scannedData.put(matnr, data);
        scannedEanData.put(ean, eanData);

        barcode_art_et.setText("");
        barcode_art_et.requestFocus();
    }
    private void getStoreStockdata(String ean) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_GET_STOCK);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGORT", source);
            args.put("IM_EAN11", ean);
            args.put("IM_STOCK_TAKE", "");
            showProcessingAndSubmit(Vars.ZWM_STORE_GET_STOCK, REQUEST_STOCK_DATA, args);
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
    private void setStockData(JSONObject responsebody){
        try{
            JSONArray arrEanData = responsebody.getJSONArray("ET_EAN_DATA");
            JSONObject mardData = responsebody.getJSONObject("EX_MARD");
            int len = arrEanData.length();
            for (int recordIndex = 0; recordIndex < len; recordIndex++) {
                JSONObject row = arrEanData.getJSONObject(recordIndex);
                if (isEanDataHeaderRow(row)) {
                    continue;
                }
                EtEanDataModel eanModel = new Gson().fromJson(row.toString(), EtEanDataModel.class);
                if (eanModel.getEAN11() == null || eanModel.getEAN11().trim().isEmpty()) {
                    continue;
                }
                etEanRecords.put(eanModel.getEAN11().trim().toUpperCase(), eanModel);
            }
            MardModal mardModal = MardModal.newInstance(mardData.getString("MATNR"), mardData.getString("LABST"));
            etMardRecords.put(mardModal.getMatnr(), mardModal);

            String scannedEan = UIFuncs.toUpperTrim(barcode_art_et);
            if (!scannedEan.isEmpty() && etEanRecords.containsKey(scannedEan)) {
                validateEan(scannedEan);
            } else {
                UIFuncs.errorSound(con);
                box.getBox("No Data", "No EAN data returned for scanned barcode.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        d.dismiss();
                        resetBarcodeInput();
                    }
                });
            }
        }catch (Exception exce){
            box.getErrBox(exce);
            resetBarcodeInput();
        }
    }

    private void saveDataToServer() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_GRT_FROM_DISP_AREA);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGORT_SRC", source);
            args.put("IM_LGORT_DEST", dest);
            args.put("IM_PACK_MAT", pack_mat_spinner.getSelectedItem().toString());
            args.put("IM_CATEGORY", category);

            int i = 0, j = 0;
            JSONArray arrItData = new JSONArray();
            for(Map.Entry<String, ScannedData> data: scannedData.entrySet()) {
                JSONObject itDataJson = getItDataJson(data);
                arrItData.put(itDataJson);
            }
            args.put("IT_DATA", arrItData);
            showProcessingAndSubmit(Vars.ZWM_STORE_GRT_FROM_DISP_AREA, REQUEST_SAVE, args);
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

    private static @NonNull JSONObject getItDataJson(Map.Entry<String, ScannedData> data) throws JSONException {
        JSONObject itDataJson = new JSONObject();
        itDataJson.put("MATERIAL", data.getValue().getMaterial());
        itDataJson.put("SCAN_QTY", data.getValue().getQty());
        itDataJson.put("WM_NO", "");
        itDataJson.put("PLANT", "");
        itDataJson.put("STOR_LOC", "");
        itDataJson.put("BATCH", "");
        itDataJson.put("CRATE", "");
        itDataJson.put("BIN", "");
        itDataJson.put("STORAGE_TYPE", "");
        itDataJson.put("MEINS", "");
        itDataJson.put("AVL_STOCK", "");
        itDataJson.put("OPEN_STOCK", "");
        itDataJson.put("PICNR", "");
        itDataJson.put("PICK_QTY", "");
        itDataJson.put("HU_NO", "");
        itDataJson.put("BARCODE", "");
        itDataJson.put("MATKL", "");
        itDataJson.put("WGBEZ", "");
        itDataJson.put("SONUM", "");
        itDataJson.put("DELNUM", "");
        itDataJson.put("POSNR", "");
        itDataJson.put("GNATURE", "");
        return itDataJson;
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

        // Dismiss any previously shown dialog so we never leave an orphaned
        // "Please wait..." dialog on screen when a request is triggered twice.
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }

        requestInProgress = true;
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
                    requestInProgress = false;
                    if (dialog != null) {
                        dialog.dismiss();
                        dialog = null;
                    }
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
                requestInProgress = false;
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {
                    UIFuncs.errorSound(con);
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            d.dismiss();
                            resetBarcodeInput();
                        }
                    });
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            d.dismiss();
                            resetBarcodeInput();
                        }
                    });
                    return;
                } else {
                    try {
                        JSONObject returnobj = parseReturnObject(responsebody);
                        if (isSapErrorReturn(returnobj)) {
                            UIFuncs.errorSound(getContext());
                            AlertBox box = new AlertBox(getContext());
                            String message = returnobj.optString("MESSAGE", "").trim();
                            box.getBox("Err", message.isEmpty() ? "SAP error" : message, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int which) {
                                    d.dismiss();
                                    resetBarcodeInput();
                                }
                            });
                        } else if (request == REQUEST_PACK_DATA) {
                            populatePackMaterial(responsebody);
                        } else if (request == REQUEST_STOCK_DATA) {
                            setStockData(responsebody);
                        } else if (request == REQUEST_SAVE) {
                            String message = returnobj != null
                                    ? returnobj.optString("MESSAGE", "Saved successfully.").trim()
                                    : "Saved successfully.";
                            if (message.isEmpty()) {
                                message = "Saved successfully.";
                            }
                            box.getBox("Success", message);
                            clearData();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        AlertBox box = new AlertBox(getContext());
                        box.getErrBox(e);
                        resetBarcodeInput();
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
            requestInProgress = false;
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

                requestInProgress = false;
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        d.dismiss();
                        resetBarcodeInput();
                    }
                });
            }
        };
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStackImmediate();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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

    class ScannedData {
        private String material;
        private int qty;

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }
    }
}
