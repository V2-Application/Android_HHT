package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.google.gson.JsonObject;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtFinalModel;
import com.v2retail.dotvik.modal.EtLquaModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link V01HuPickingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class V01HuPickingFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Button save,back;
    private EditText huNo,pickListNo,bin_no,barcode_no,article_no,destination_bin,huQ,tsq;
    private ProgressDialog dialog;
    private AlertBox box;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";
    private String TAG = V01HuPickingFragment.class.getName();
    private List<EtEanDataModel> etEanDataModelList;
    private List<EtFinalModel> etFinalModels;
    private JSONArray saveHu;
    private int totalScanQty=0;
    private JSONArray IMT_LQUA,IMT_HU_STATUS;
    private FragmentManager fragmentManager;
    public V01HuPickingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment V01HuPickingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static V01HuPickingFragment newInstance(String param1, String param2) {
        V01HuPickingFragment fragment = new V01HuPickingFragment();
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        fragmentManager = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_v01_hu_picking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init(view);
    }

    private void init(View view) {

        huNo = view.findViewById(R.id.huNo);
        pickListNo = view.findViewById(R.id.pickListNo);
        bin_no = view.findViewById(R.id.bin_no);
        barcode_no = view.findViewById(R.id.barcode_no);
        article_no = view.findViewById(R.id.article_no);
        destination_bin = view.findViewById(R.id.destination_bin);
        huQ = view.findViewById(R.id.huQ);
        tsq = view.findViewById(R.id.tsq);
        back = view.findViewById(R.id.back);
        save = view.findViewById(R.id.save);
        save.setOnClickListener(this);
        back.setOnClickListener(this);
        huNo.requestFocus();
        addEditorListeners();
        addTextChangeListners();
        box = new AlertBox(getContext());
        dialog = new ProgressDialog(getContext());
        SharedPreferencesData data = new SharedPreferencesData(getContext());
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

    }

    void addEditorListeners() {

        huNo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = huNo.getText().toString();
                    if (!(crate.equals("") || crate.length() < 0 || crate.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(huNo.getWindowToken(), 0);
                            loadHuNo();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }
                return false;
            }
        });

        bin_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = bin_no.getText().toString();
                    if (!(crate.equals("") || crate.length() < 0 || crate.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin_no.getWindowToken(), 0);
                            loadBin();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }
                return false;
            }
        });
        barcode_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = barcode_no.getText().toString();
                    if (!(crate.equals("") || crate.length() < 0 || crate.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(barcode_no.getWindowToken(), 0);
                            loadBarcode();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }
                return false;
            }
        });


    }


    void addTextChangeListners() {

        huNo.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String poString = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned poString : " + poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadHuNo();
                        }
                    });
                }
            }
        });

        bin_no.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String poString = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned poString : " + poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBin();
                        }
                    });
                }
            }
        });
        barcode_no.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String poString = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned poString : " + poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBarcode();
                        }
                    });
                }
            }
        });



    }
    private void loadHuNo() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String bin = huNo.getText().toString();

                if (bin.equals("")||bin.isEmpty()){
                    box.getBox("Alert","First Scan Hu!");
                    dialog.dismiss();
                    return;
                }
                try {
                    getHuDataApi(bin);
                } catch (Exception e) {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }

    private void loadBin(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String bin = bin_no.getText().toString();
                if (bin.equals("")||bin.isEmpty()){
                    box.getBox("Alert","First Scan Bin!");
                    dialog.dismiss();
                    return;
                }
                try {
                    validateBin(bin);
                } catch (Exception e) {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }

    private void loadBarcode(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String bin = barcode_no.getText().toString();

                if (bin.equals("")||bin.isEmpty()){
                    box.getBox("Alert","First Scan Bin!");
                    dialog.dismiss();
                    return;
                }
                loadSaveData(bin);
                try {
                } catch (Exception e) {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }
    private void loadSaveData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String bin = barcode_no.getText().toString();

                if (saveHu==null||saveHu.length()<=0){
                    box.getBox("Alert","First Scan Data!");
                    dialog.dismiss();
                    return;
                }
                saveData();
                try {
                } catch (Exception e) {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }
    private void getHuDataApi(String hu){

        String rfc = "ZRFC_SDC_PUT31";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_SITE", WERKS);
            params.put("IM_HU", hu );



        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

        Log.d(TAG, "payload ->" + params.toString());
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();

                }
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    huNo.requestFocus();
                    huNo.setText("");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        etEanDataModelList = new ArrayList<>();
                                        etFinalModels = new ArrayList<>();
                                        saveHu = new JSONArray();
                                        IMT_HU_STATUS = new JSONArray();
                                        IMT_LQUA = new JSONArray();
                                        JSONArray ET_LQUA = responsebody.getJSONArray("ET_LQUA");
                                        JSONArray ET_FINAL = responsebody.getJSONArray("ET_FINAL");
                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        for (int i = 1;i<ET_LQUA.length();i++){
                                            JSONObject saveData = new JSONObject();
                                            JSONObject jsonObject = ET_LQUA.getJSONObject(i);
                                            String LQNUM = jsonObject.getString("LQNUM");
                                            String WERKS = jsonObject.getString("WERKS");
                                            String LGPLA = jsonObject.getString("LGPLA");
                                            String LGNUM = jsonObject.getString("LGNUM");
                                            String MATNR = jsonObject.getString("MATNR");

                                            saveData.put("LGNUM",LGNUM);
                                            saveData.put("LQNUM",LQNUM);
                                            saveData.put("MATNR",MATNR);
                                            saveData.put("WERKS",WERKS);
                                            saveData.put("LGPLA",LGPLA);
                                            IMT_LQUA.put(saveData);
                                        }
                                        Log.d("IMT_LQUA",IMT_LQUA.toString());
                                          for (int i = 1;i<ET_FINAL.length();i++){
                                              JSONObject jsonObject = ET_FINAL.getJSONObject(i);
                                              JSONObject saveData = new JSONObject();
                                              String PICNR = jsonObject.getString("PICNR");
                                              String HU_NO = jsonObject.getString("HU_NO");
                                              String WERKS = jsonObject.getString("WERKS");
                                              String MATNR = jsonObject.getString("MATNR");
                                              String HU_QTY = jsonObject.getString("HU_QTY");
                                              String BIN_QTY = jsonObject.getString("BIN_QTY");
                                              String SCAN_QTY = jsonObject.getString("SCAN_QTY");
                                              String REQ_0008 = jsonObject.getString("REQ_0008");
                                              String PICKLIST_QTY = jsonObject.getString("PICKLIST_QTY");
                                              etFinalModels.add(new EtFinalModel(SCAN_QTY,REQ_0008,HU_QTY,WERKS,PICNR,PICKLIST_QTY,BIN_QTY,HU_NO,MATNR));
                                              saveData.put("PICNR",PICNR);
                                              saveData.put("HU_NO",HU_NO);
                                              saveData.put("WERKS",WERKS);
                                              saveData.put("MATNR",MATNR);
                                              saveData.put("HU_QTY",HU_QTY);
                                              saveData.put("PICKLIST_QTY",PICKLIST_QTY);
                                              saveData.put("SCAN_QTY",SCAN_QTY);
                                              saveData.put("REQ_0008",REQ_0008);
                                              saveData.put("BIN_QTY",BIN_QTY);

                                              IMT_HU_STATUS.put(saveData);

                                        }
                                          Log.v("IMT_HU_STATUS",IMT_HU_STATUS.toString());
                                          int huCount=0;
                                          for (int i = 1;i<ET_EAN_DATA.length();i++){
                                              JSONObject jsonObject = ET_EAN_DATA.getJSONObject(i);
                                              String UMREN = jsonObject.getString("UMREN");
                                              String EAN11 = jsonObject.getString("EAN11");
                                              String UMREZ = jsonObject.getString("UMREZ");
                                              String MATNR = jsonObject.getString("MATNR");

                                              etEanDataModelList.add(new EtEanDataModel(MATNR,UMREZ,UMREN,EAN11));
                                            huCount = huCount+Double.valueOf(etFinalModels.get(0).getHU_QTY()).intValue();
                                          }

                                          pickListNo.setText(etFinalModels.get(0).getPICNR());
                                          huQ.setText(String.valueOf(huCount));
                                          bin_no.requestFocus();


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



        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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
    private void validateBin(String bin){

        String rfc = "ZRFC_SDC_PUT31_BIN_VALIDATION";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_SITE", WERKS);
            params.put("IM_LGPLA", bin );



        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

        Log.d(TAG, "payload ->" + params.toString());
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        bin_no.requestFocus();
                                        bin_no.setText("");
                                        return;
                                    } else {
                                          barcode_no.requestFocus();


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



        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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

    private void loadSaveData(String barcode){
        boolean isEan = true;
        String matnr="";
        int eanPackQty = 0;
        dialog.dismiss();
        for(int i =0;i<etEanDataModelList.size();i++){
            if (barcode.equals(etEanDataModelList.get(i).getEAN11())){
                article_no.setText(etEanDataModelList.get(i).getMATNR());
                matnr = etEanDataModelList.get(i).getMATNR();
                eanPackQty = Double.valueOf(etEanDataModelList.get(i).getUMREZ()).intValue();
                isEan = false;
                break;
            }
        }

        if (isEan){
            box.getBox("Err","Scanned barcode not found in EAN RECORDS");
            barcode_no.requestFocus();
            barcode_no.setText("");
            return;
        }
        boolean isFinal = true;
        int pickRem = 0;
        int scanQty = 0;
        int huQty = 0;
        int binRem = 0;
        boolean isAdjusted = false;
        for (int i=0;i<etFinalModels.size();i++){
            if (matnr.equals(etFinalModels.get(i).getMATNR())){
                isFinal = false;

                scanQty = Double.valueOf(etFinalModels.get(i).getSCAN_QTY()).intValue();
                huQty = Double.valueOf(etFinalModels.get(i).getHU_QTY()).intValue();
                pickRem = Double.valueOf(etFinalModels.get(i).getPICKLIST_QTY()).intValue();
                binRem =  Double.valueOf(etFinalModels.get(i).getBIN_QTY()).intValue();
                pickListNo.setText(etFinalModels.get(i).getPICNR());
            }
        }

        if (isEan){
            box.getBox("Err","Article not found in PICKLIST RECORDS");
            barcode_no.requestFocus();
            barcode_no.setText("");
            return;

        }

        totalScanQty++;
//        if (huQty<totalScanQty){
        if (huQty - scanQty==0){
            box.getBox("Alert","You have already scanned maximum allowed quantities for Article : "+matnr);
            barcode_no.setText("");
            barcode_no.requestFocus();
            return;
        }

        if (pickRem >= eanPackQty){
            pickRem = pickRem - eanPackQty;
            destination_bin.setText("0008");
            for (int i=0;i<etFinalModels.size();i++){
                if (matnr.equals(etFinalModels.get(i).getMATNR())){
                    etFinalModels.get(i).setPICKLIST_QTY(String.valueOf(pickRem));
                }
            }
            isAdjusted = true;
        }else if (binRem >= eanPackQty) {
            binRem = binRem - eanPackQty;
            for (int i=0;i<etFinalModels.size();i++){
                if (matnr.equals(etFinalModels.get(i).getMATNR())){
                    etFinalModels.get(i).setBIN_QTY(String.valueOf(binRem));
                }
            }
            destination_bin.setText(bin_no.getText().toString());
            isAdjusted = true;
        }
        if (!isAdjusted)
        {
            box.getBox("Alert","Quantity of this EAN cannot be adjusted in either PICKLIST or BIN.\n -->Remaining PICKLIST QTY = " + pickRem + "\n-->Remaining BIN QTY = " + binRem);
            barcode_no.setText("");
            barcode_no.requestFocus();
            return;
        }
        scanQty = scanQty + eanPackQty;
//        tsq.setText(String.valueOf(scanQty));
        for (int i=0;i<etFinalModels.size();i++){
            if (matnr.equals(etFinalModels.get(i).getMATNR())){
                etFinalModels.get(i).setSCAN_QTY(String.valueOf(scanQty));
                etFinalModels.get(i).setREQ_0008(String.valueOf(huQty - scanQty));
            }
        }
        int count=0;
        for (int i=0;i<etFinalModels.size();i++){
            count = count+Double.valueOf(etFinalModels.get(i).getSCAN_QTY()).intValue();
        }


        tsq.setText(String.valueOf(count));

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("HU",huNo.getText().toString());
            jsonObject.put("ITEM_NO","000000");
            jsonObject.put("ARTICLE",matnr);
            jsonObject.put("PSTNG_DATE",new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            jsonObject.put("PLANT",WERKS);
            jsonObject.put("STGE_LOC",destination_bin.getText().toString().equals("0008")?"0008":"");
            jsonObject.put("SCAN_QTY",eanPackQty);
            jsonObject.put("REM_QTY",0);
            jsonObject.put("BIN",destination_bin.getText().toString().equals("0008")?"":destination_bin.getText().toString());
            jsonObject.put("EAN11",barcode_no.getText().toString());
            saveHu.put(jsonObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.v("Save",saveHu.toString());

        barcode_no.requestFocus();
        barcode_no.setText("");


    }

    private void saveData(){

        String rfc = "ZWM_HUPUT31_SAVE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_PICNR", pickListNo.getText().toString() );
            params.put("IT_HUSAVE", saveHu);
            params.put("IMT_LQUA", IMT_LQUA);
            params.put("IMT_HU_STATUS", IMT_HU_STATUS);


        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

        Log.d(TAG, "payload ->" + params.toString());
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();

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
                        if (responsebody.has("EX_TO_RETURN") && responsebody.get("EX_TO_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_TO_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        totalScanQty = 0;
                                        saveHu = new JSONArray();
                                        tsq.setText("");
                                        huNo.setText("");
                                        pickListNo.setText("");
                                        bin_no.setText("");
                                        barcode_no.setText("");
                                        article_no.setText("");
                                        destination_bin.setText("");
                                        huQ.setText("");
                                        huNo.requestFocus();
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", returnobj.getString("MESSAGE"));

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



        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.save:
                    loadSaveData();
                break;

            case R.id.back:
                fragmentManager.popBackStack();
                break;
        }
    }
}