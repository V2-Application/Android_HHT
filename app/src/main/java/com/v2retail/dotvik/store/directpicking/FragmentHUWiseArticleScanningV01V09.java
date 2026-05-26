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
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
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

public class FragmentHUWiseArticleScanningV01V09 extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_HU = 1501;
    private static final int REQUEST_SAVE = 1502;

    private static final String TAG = FragmentHUWiseArticleScanningV01V09.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_save;
    CheckBox chk_empty_hu;
    EditText txt_scan_hu, txt_scanned_hu, txt_scan_barcode, txt_article, txt_floor, txt_bin, txt_division, txt_rqty, txt_sqty, txt_pqty, txt_tqty;

    HashMap<String, ArtEANData> eanDataMap = new HashMap<>();
    List<HUArtData> artDataList = new ArrayList<>();
    List<HUArtData> scannedData = new ArrayList<>();

    double totalScanned = 0;

    public FragmentHUWiseArticleScanningV01V09() {

    }

    public static FragmentHUWiseArticleScanningV01V09 newInstance() {
        return new FragmentHUWiseArticleScanningV01V09();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("HU Wise Article Scan V01 To V09");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_hu_wise_article_scanning_v01_v09, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        FragmentActivity activity = getActivity();

        txt_scan_hu = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_scan_hu);
        txt_scanned_hu = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_scanned_hu);
        txt_scan_barcode = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_scan_barcode);
        txt_article = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_article);
        txt_floor = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_floor);
        txt_bin = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_bin);
        txt_division = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_division);
        txt_rqty = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_rqty);
        txt_sqty = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_sqty);
        txt_pqty = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_pqty);
        txt_tqty = rootView.findViewById(R.id.txt_hu_wise_article_scan_v01_v09_tqty);

        chk_empty_hu = rootView.findViewById(R.id.chk_hu_wise_article_scan_v01_v09_empty_hu);

        btn_back = rootView.findViewById(R.id.btn_hu_wise_article_scan_v01_v09_back);
        btn_save = rootView.findViewById(R.id.btn_hu_wise_article_scan_v01_v09_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_hu_wise_article_scan_v01_v09_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_hu_wise_article_scan_v01_v09_save:
                String message = "YOU ARE SAVING WITHOUT EMPTY HU CHECK";
                if(chk_empty_hu.isChecked()){
                    message = "ARE YOU SURE YOU WANT TO SAVE";
                }
                box.getBox("Alert", message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        save();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                break;
        }
    }

    private void clear(){
        eanDataMap = new HashMap<>();
        artDataList = new ArrayList<>();
        scannedData = new ArrayList<>();
        txt_scan_hu.setText("");
        txt_scanned_hu.setText("");
        UIFuncs.disableInput(con, txt_scan_barcode);
        txt_scan_barcode.setText("");
        txt_article.setText("");
        txt_floor.setText("");
        txt_bin.setText("");
        txt_division.setText("");
        txt_rqty.setText("0");
        txt_sqty.setText("0");
        txt_pqty.setText("0");
        txt_tqty.setText("0");
        chk_empty_hu.setChecked(false);
        totalScanned = 0;
        UIFuncs.enableInput(con, txt_scan_hu);
    }

    private void addInputEvents(){
        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty()) {
                        validateHU(value);
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
        txt_scan_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_barcode);
                    if (!value.isEmpty()) {
                        validateBarcode(value);
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
                    validateBarcode(value);
                }
            }
        });
    }

    private void validateHU(String hu) {
        JSONObject args = new JSONObject();
        try {
            String rfc = Vars.ZSDC_DIRECT_HU_VAL_RFC;
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_HU, args);
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

    /**
     * SAP (production) often returns ET_*[0] as a field-description header row.
     * Dev JSON RFC adapter returns clean 0-indexed rows with no header — older code
     * always skipped index 0, so EAN/HU data was never loaded in dev.
     */
    private static boolean isHuArtHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String hu = row.optString("HU", "").trim();
        String material = row.optString("MATERIAL", "").trim();
        String floor = row.optString("FLOOR", "").trim();
        if ("HU".equalsIgnoreCase(hu) || "MATERIAL".equalsIgnoreCase(material) || "FLOOR".equalsIgnoreCase(floor)) {
            return true;
        }
        if (hu.contains("External Handling Unit")) {
            return true;
        }
        if (material.equalsIgnoreCase("Material Number")) {
            return true;
        }
        return floor.contains("Component of the Version");
    }

    private static boolean isEanArtHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String ean = row.optString("EAN", "").trim();
        String material = row.optString("MATERIAL", "").trim();
        String umrez = row.optString("UMREZ", "").trim();
        if ("EAN".equalsIgnoreCase(ean) || "MATERIAL".equalsIgnoreCase(material) || "UMREZ".equalsIgnoreCase(umrez)) {
            return true;
        }
        if (ean.contains("International Article Number")) {
            return true;
        }
        if (material.equalsIgnoreCase("Material Number")) {
            return true;
        }
        return umrez.contains("Numerator for Conversion");
    }

    private static int huArtDataStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return 0;
        }
        return isHuArtHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
    }

    private static int eanArtDataStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return 0;
        }
        return isEanArtHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
    }

    private void setData(JSONObject responsebody) {
        try
        {
            eanDataMap = new HashMap<>();
            artDataList = new ArrayList<>();
            scannedData = new ArrayList<>();
            totalScanned = 0;

            JSONArray ET_HU_ART_DATA_ARRAY = responsebody.optJSONArray("ET_HU_ART_DATA");
            JSONArray ET_EAN_ART_DATA_ARRAY = responsebody.optJSONArray("ET_EAN_ART_DATA");

            if (ET_HU_ART_DATA_ARRAY != null) {
                int start = huArtDataStartIndex(ET_HU_ART_DATA_ARRAY);
                for (int i = start; i < ET_HU_ART_DATA_ARRAY.length(); i++) {
                    JSONObject ET_RECORD = ET_HU_ART_DATA_ARRAY.getJSONObject(i);
                    if (isHuArtHeaderRow(ET_RECORD)) {
                        continue;
                    }
                    HUArtData artData = new Gson().fromJson(ET_RECORD.toString(), HUArtData.class);
                    if (artData == null || artData.getMaterial() == null || artData.getMaterial().trim().isEmpty()) {
                        continue;
                    }
                    artData.setSqty("0.00");
                    artData.setPqty("0.00");
                    artData.setTqty(artData.getQty());
                    artDataList.add(artData);
                }
            }

            if (ET_EAN_ART_DATA_ARRAY != null) {
                int start = eanArtDataStartIndex(ET_EAN_ART_DATA_ARRAY);
                for (int i = start; i < ET_EAN_ART_DATA_ARRAY.length(); i++) {
                    JSONObject EAN_RECORD = ET_EAN_ART_DATA_ARRAY.getJSONObject(i);
                    if (isEanArtHeaderRow(EAN_RECORD)) {
                        continue;
                    }
                    ArtEANData eanData = new Gson().fromJson(EAN_RECORD.toString(), ArtEANData.class);
                    if (eanData == null || eanData.getEan() == null || eanData.getEan().trim().isEmpty()) {
                        continue;
                    }
                    eanDataMap.put(eanData.getEan().trim().toUpperCase(), eanData);
                }
            }

            Log.d(TAG, "setData HU rows=" + artDataList.size() + " EAN keys=" + eanDataMap.size());
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
            txt_scan_hu.setText("");
            txt_scan_hu.requestFocus();
            return;
        }
        txt_scanned_hu.setText(UIFuncs.toUpperTrim(txt_scan_hu));
        txt_scan_hu.setText("");
        UIFuncs.enableInput(con, txt_scan_barcode);
    }

    private void validateBarcode(String barcode) {
        HUArtData artData = null;
        boolean isAllScanned = false;
        if(eanDataMap.containsKey(barcode)){

            //First check in scanned data.
            if(!scannedData.isEmpty()){
                for (HUArtData data : scannedData){
                    if(data.getEan().equals(barcode)){
                        double rqty = Util.convertStringToDouble(data.getQty());
                        double sqty = Util.convertStringToDouble(data.getSqty());
                        if(sqty < rqty){
                            artData = data;
                            break;
                        }
                        if(rqty <= (sqty + data.getUmrez())){
                            isAllScanned = true;
                        }
                    }
                }
            }

            //If not already scanned then pick from the HU Art Data
            if(artData == null){
                ArtEANData eanData = eanDataMap.get(barcode);
                String material = eanData.getMaterial();
                for (HUArtData data: artDataList) {
                    if(data.getMaterial().equals(material) && !data.isPicked()){
                        data.setPicked(true);
                        data.setEan(barcode);
                        data.setUmrez(Util.convertStringToDouble(eanData.getUmrez()));
                        artData = HUArtData.newInstance(data);
                        scannedData.add(artData);
                        break;
                    }
                }
            }

            if(artData != null){

                double rqty = Util.convertStringToDouble(artData.getQty());
                double sqty = Util.convertStringToDouble(artData.getSqty());
                double pqty;

                sqty = sqty + artData.getUmrez();

                if(sqty <= rqty){

                    pqty = rqty - sqty;
                    totalScanned = totalScanned + artData.getUmrez();

                    txt_article.setText(UIFuncs.removeLeadingZeros(artData.getMaterial()));
                    txt_rqty.setText(Util.formatDouble(rqty));
                    txt_sqty.setText(Util.formatDouble(sqty));
                    txt_pqty.setText(Util.formatDouble(pqty));
                    txt_tqty.setText(Util.formatDouble(totalScanned));

                    txt_floor.setText(artData.getFloor());
                    txt_bin.setText(artData.getBin());
                    txt_division.setText(artData.getDivision());

                    artData.setSqty(Util.formatDouble(sqty));
                    artData.setPqty(Util.formatDouble(pqty));

                    txt_scan_barcode.setText("");
                    UIFuncs.enableInput(con, txt_scan_barcode);

                }else{
                    box.getBox("Max Scanned", "No pending quantity for this article");
                }
            }else{
                if(isAllScanned){
                    box.getBox("Max Scanned", "No pending quantity for this article");
                }else{
                    box.getBox("Invalid Material", "Invalid material scanned. Material not present in HU Data");
                }
            }
        }else{
            box.getBox("Invalid Barcode", "Invalid barcode scanned. Barcode not present in EAN Data");
        }
        txt_scan_barcode.setText("");
        UIFuncs.enableInput(con, txt_scan_barcode);
    }

    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for(HUArtData artData : scannedData) {
                if(Util.convertStringToDouble(artData.getSqty()) > 0){
                    JSONObject itDataJson = new JSONObject(new Gson().toJson(artData));
                    arrScanData.put(itDataJson);
                }
            }
            return arrScanData;
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    private void save(){
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if(dataToSave == null){
            box.getBox("Invalid", "No records to submit. Please scan some barcodes");
            return;
        }
        try {
            args.put("bapiname", Vars.ZSDC_DIRECT_SAVE_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", UIFuncs.toUpperTrim(txt_scanned_hu));
            args.put("IM_HU_EMPTY", chk_empty_hu.isChecked() ? "X" : "");
            args.put("IT_DATA", dataToSave);
            showProcessingAndSubmit(Vars.ZSDC_DIRECT_SAVE_RFC, REQUEST_SAVE, args);
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
                                        if (request == REQUEST_VALIDATE_HU) {
                                            txt_scan_hu.setText("");
                                            txt_scan_hu.requestFocus();
                                        }
                                    } else {
                                        if(request == REQUEST_VALIDATE_HU){
                                            setData(responsebody);
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

    class ArtEANData {
        @SerializedName("MATERIAL")
        private String material;
        @SerializedName("EAN")
        private String ean;
        @SerializedName("UMREZ")
        private String umrez;

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public String getEan() {
            return ean;
        }

        public void setEan(String ean) {
            this.ean = ean;
        }

        public String getUmrez() {
            return umrez;
        }

        public void setUmrez(String umrez) {
            this.umrez = umrez;
        }
    }
}