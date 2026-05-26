package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.inputmethod.InputMethodManager;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.StockTakeProcessEtBinModel;
import com.v2retail.dotvik.modal.StockTakeProcessExHeaderModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Scan_Stock_take_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Scan_Stock_take_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Scan_Stock_take_Fragment extends Fragment implements View.OnClickListener,TextView.OnEditorActionListener, View.OnKeyListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    private static final String ARG_PARAM1 = "stocktakeBin";
    private static final String ARG_PARAM2 = "Crate";
    private static final String ARG_PARAM3 = "data";
    private static final String ARG_PARAM4 = "StockTakeDetails";
    String URL="";
    String WERKS="";
    String USER="";
    String requester = "";
    String scanner = "";
    int scanCount=0;
    List<StockTakeProcessEtBinModel> dtstocktakeBin;
    List<StockTakeProcessExHeaderModel> dtStockTakeDetails;

    // TODO: Rename and change types of parameters
    private String TAG = Scan_Stock_take_Fragment.class.getName();

    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button back;
    Button save;
    EditText bin_et;
    EditText scrate_et;
    EditText sq_et;
    EditText barcode_et;
    EditText tsq_et;
    EditText article_et;
    EditText asq_et;
    CheckBox unscanned_ch;
    private JSONArray saveJsonArray;

    private String binStr="";
    private String crateStr = "";

    private int asqSum = 0;
    private int sum = 0;

    private OnFragmentInteractionListener mListener;

    public Scan_Stock_take_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Scan Stock Take");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OutWardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static Scan_Stock_take_Fragment newInstance(String param1, String param2) {
        Scan_Stock_take_Fragment fragment = new Scan_Stock_take_Fragment();
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

            dtstocktakeBin = (List<StockTakeProcessEtBinModel>) getArguments().getSerializable(ARG_PARAM1);
            dtStockTakeDetails = (List<StockTakeProcessExHeaderModel>)  getArguments().getSerializable(ARG_PARAM4);

        }
        fm = getFragmentManager();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scan_stock_take_dc, container, false);
        con = getContext();
        box=new AlertBox(con);

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

        back = (Button) view.findViewById(R.id.back);
        save = (Button) view.findViewById(R.id.save);


        unscanned_ch = (CheckBox) view.findViewById(R.id.unscanned);

        bin_et = (EditText) view.findViewById(R.id.bin_no); //huno
        scrate_et = (EditText) view.findViewById(R.id.dest_crate);//pack
        barcode_et = (EditText) view.findViewById(R.id.barcode_no);//ts
        article_et = (EditText) view.findViewById(R.id.article_no);//to
        tsq_et = (EditText) view.findViewById(R.id.tsq);//tsq
        sq_et = (EditText) view.findViewById(R.id.sqc);//tsq
        asq_et = (EditText) view.findViewById(R.id.asq);//topenqty

        bin_et.requestFocus();

        saveJsonArray = new JSONArray();

        back.setOnClickListener(this);//btnExit
        save.setOnClickListener(this);//save

        onEditListener();
        addTextChangeListeners();

        return view;

    }

    private void onEditListener() {

        bin_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String binEt = bin_et.getText().toString();
                    if (binEt == null || binEt.length() < 0 || binEt.equals("")) {
                        box.getBox("Alert", "Scan stock bin !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin_et.getWindowToken(), 0);
                            loadBinData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });
        scrate_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String crate = scrate_et.getText().toString().toUpperCase().trim();
                    if(crate.length()>0) {
                        scrate_et.selectAll();
                        loadCrateData();
                        return true;
                    }
                }
                return false;
            }
        });
//        scrate_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
//                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    String crateScan = scrate_et.getText().toString();
//                    if (crateScan == null || crateScan.length() < 0 || crateScan.equals("")) {
//                        box.getBox("Alert", "Scan crate Scan !");
//                    } else {
//                        try {
//                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                            imm.hideSoftInputFromWindow(scrate_et.getWindowToken(), 0);
//                            loadCrateData();
//                        }catch (Exception e){
//                            box.getErrBox(e);
//                        }
//
//
//                        return true;
//                    }
//                }
//
//                return false;
//            }
//        });

        barcode_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String barcodeEt = barcode_et.getText().toString();
                    if (barcodeEt == null || barcodeEt.length() < 0 || barcodeEt.equals("")) {
                        box.getBox("Alert", "Scan barcode !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(barcode_et.getWindowToken(), 0);
                            loadBarcodeData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });


    }
    private void addTextChangeListeners() {

        bin_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 2) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String locationNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned bin_et  : " +  locationNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBinData();
                        }
                    });
                }
            }
        });
        scrate_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 2) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String locationNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned crate_scan  : " +  locationNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadCrateData();
                        }
                    });
                }
            }
        });
        barcode_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 2) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String locationNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned barcode  : " +  locationNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBarcodeData();
                        }
                    });
                }
            }
        });

    }
    private void loadBinData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String bin = bin_et.getText().toString().trim() ;

        if (bin == null || bin.length() < 0 || bin.equals("")) {
            box.getBox("Alert", "Scan bin !");
            dialog.dismiss();
            return;
        }


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    setBinData(bin);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }
    private void loadCrateData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String crate = scrate_et.getText().toString().trim() ;

        if (crate == null || crate.length() < 0 || crate.equals("")) {
            box.getBox("Alert", "Scan crate !");
            dialog.dismiss();
            return;
        }


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    validateCrate(crate);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }
    private void loadBarcodeData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String barcodeEt = barcode_et.getText().toString().trim() ;

        if (barcodeEt == null || barcodeEt.length() < 0 || barcodeEt.equals("")) {
            box.getBox("Alert", "Scan barcode !");
            dialog.dismiss();
            return;
        }


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    validateBarcode(barcodeEt);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }
    private void loadSaveData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        if (saveJsonArray == null || saveJsonArray.length() == 0) {
            box.getBox("Alert", "Scan Data !");
            dialog.dismiss();
            return;
        }


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    saveData();
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    private void setBinData(String bin) {
        boolean binfound=false;
        for (int i =0 ; i< dtstocktakeBin.size();i++){
            if (dtstocktakeBin.get(i).getLGPLA().equals(bin)){
                binfound = true;
                binStr = bin;
                break;
            }
        }

        if (binfound) {
            for (int j = 0; j < dtStockTakeDetails.size(); j++) {
                if (dtStockTakeDetails.get(j).getWITH_CRATE().equals("X")) {
                    bin_et.setEnabled(false);
                    scrate_et.requestFocus();
                } else {
                    scrate_et.setEnabled(false);
                    barcode_et.requestFocus();

                }
                break;
            }
        }else {
            box.getBox("Alert", "Invalid Bin No.");
            bin_et.setText("");
            bin_et.requestFocus();

        }
        dialog.dismiss();
    }
    private void validateCrate(String crate){
        String rfc = "ZWM_RFC_STOCK_TAKE_CRATE_VALI";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crate);
            params.put("IM_SITE", dtStockTakeDetails.get(0).getWERKS());

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        scrate_et.setText("");
                                        scrate_et.requestFocus();
                                        return;
                                    } else {

                                        barcode_et.requestFocus();
                                        crateStr = crate;

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
    private void validateBarcode(String barcodeEt){
        String rfc = "ZWM_RFC_STOCK_VALIDATE_BARCODE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_BARCODE", barcodeEt);
            params.put("IM_LGNUM", dtStockTakeDetails.get(0).getLGNUM());
            params.put("IM_LGTYP", dtStockTakeDetails.get(0).getLGTYP());
            params.put("IM_RFC", "X");

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
                        if (responsebody.has("EX_MESSAGE") && responsebody.get("EX_MESSAGE") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_MESSAGE");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        barcode_et.setText("");
                                        barcode_et.requestFocus();
                                        return;
                                    } else {

                                        boolean flag = true;
                                        boolean unScanFlag = true;

                                        JSONObject saveJsonObject = new JSONObject();
                                        JSONObject data = new JSONObject();
                                        JSONArray jsonArray = responsebody.getJSONArray("ET_EAN_DATA");
                                        for (int i =1;i<jsonArray.length();i++){
                                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                                            if (jsonObject.getString("EAN11").equals(barcodeEt)) {
                                                data = jsonArray.getJSONObject(i);
                                                for (int j =0 ;j< saveJsonArray.length();j++){
                                                    JSONObject jsonObject1 = saveJsonArray.getJSONObject(j);
                                                    if (jsonObject1.getString("MATNR").equals(jsonObject.getString("MATNR"))){
                                                        String sq = jsonObject1.getString("MENGE");
                                                        asqSum = Integer.valueOf(sq);
                                                        if (unscanned_ch.isChecked()){
                                                            if (asqSum- Integer.valueOf(jsonObject.getString("UMREZ") )<0){
                                                                AlertBox box = new AlertBox(getContext());
                                                                box.getBox("Alert", "Removed Quantity is less then already scanned quantity.");
                                                                barcode_et.setText("");
                                                                barcode_et.requestFocus();
                                                                return;
                                                            }
                                                            asqSum = asqSum - Integer.valueOf(jsonObject.getString("UMREZ"));
                                                            sum = sum - Integer.valueOf(data.getString("UMREZ"));
                                                            unScanFlag = false;
                                                            Log.v(TAG,"UScnaa Pressed");
                                                        }else {

                                                            asqSum = asqSum + Integer.valueOf(jsonObject.getString("UMREZ"));
                                                            sum = sum+ Integer.valueOf(data.getString("UMREZ"));
                                                        }
                                                        jsonObject1.put("MENGE", String.valueOf(asqSum));
                                                        article_et.setText(jsonObject1.getString("MATNR"));
                                                        flag = false;
                                                    }
                                                }
                                            }
                                        }

                                        if (unScanFlag && unscanned_ch.isChecked()){
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Alert", "No Data Found for Removed Quantity -"+ barcodeEt);
                                            barcode_et.setText("");
                                            barcode_et.requestFocus();
                                            return;
                                        }
                                        if (flag && !unscanned_ch.isChecked()){

                                            saveJsonObject.put("MANDT", dtStockTakeDetails.get(0).getMANDT());
                                            saveJsonObject.put("STOCK_TAKE", dtStockTakeDetails.get(0).getSTOCK_TAKE());
                                            saveJsonObject.put("WERKS", dtStockTakeDetails.get(0).getWERKS());
                                            saveJsonObject.put("LGNUM", dtStockTakeDetails.get(0).getLGNUM());
                                            saveJsonObject.put("LGTYP", dtStockTakeDetails.get(0).getLGTYP());
                                            saveJsonObject.put("LGPLA", binStr);
                                            saveJsonObject.put("MATNR", data.getString("MATNR"));
                                            saveJsonObject.put("MENGE", data.getString("UMREZ"));
                                            saveJsonObject.put("CRATE", crateStr);

                                            saveJsonArray.put(saveJsonObject);
                                            article_et.setText(data.getString("MATNR"));
                                            asqSum = Integer.valueOf(data.getString("UMREZ"));
                                            sum = sum+ Integer.valueOf(data.getString("UMREZ"));

                                        }



                                        asq_et.setText(String.valueOf(asqSum));

                                        sq_et.setText(String.valueOf(sum));
                                        tsq_et.setText(String.valueOf(sum));
                                        scrate_et.setEnabled(false);

                                        Log.v("SaveData",saveJsonArray.toString());
                                        barcode_et.setText("");
                                        barcode_et.requestFocus();



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

    private void saveData(){
        String rfc = "ZWM_RFC_STOCK_TAKE_SAVE_DATA";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_STOCK_TAKE", dtStockTakeDetails.get(0).getSTOCK_TAKE());
            params.put("IM_RFC", "X");
            params.put("IM_USER", USER);
            params.put("IT_ITEM", saveJsonArray);

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
                        if (responsebody.has("EX_MESSAGE") && responsebody.get("EX_MESSAGE") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_MESSAGE");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                crateStr = "";
                                                binStr = "";
                                                sum = 0;
                                                asqSum = 0;
                                                saveJsonArray = new JSONArray();
                                                bin_et.setText("");
                                                bin_et.setEnabled(true);
                                                bin_et.requestFocus();
                                                scrate_et.setText("");
                                                scrate_et.setEnabled(true);
                                                barcode_et.setText("");
                                                article_et.setText("");
                                                asq_et.setText("");
                                                sq_et.setText("");
                                                tsq_et.setText("");
                                                loadStockTakeIdData();

                                            }
                                        });

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


    private void loadStockTakeIdData(){

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String stockTakeId = dtStockTakeDetails.get(0).getSTOCK_TAKE();

        if (stockTakeId == null || stockTakeId.length() < 0 || stockTakeId.equals("")) {
            box.getBox("Alert", "Scan stock Take Id !");
            dialog.dismiss();
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateStockTakeIdJson(stockTakeId);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    private void sendValidateStockTakeIdJson(String stockTakeId) {

        String rfc = "ZWM_RFC_STOCK_TAKE_GET_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_STOCK_TAKE", stockTakeId);
            params.put("IM_RFC", "X");

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
                        if (responsebody.has("EX_MESSAGE") && responsebody.get("EX_MESSAGE") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_MESSAGE");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                fm.popBackStack();
                                            }
                                        });

                                        return;

                                    } else {

                                        dtStockTakeDetails.clear();
                                        dtstocktakeBin.clear();
                                        JSONObject jsonObject = responsebody.getJSONObject("EX_HEADER");

                                        String LGTYP = jsonObject.getString("LGTYP");
                                        String AETIM = jsonObject.getString("AETIM");
                                        String AENAM = jsonObject.getString("AENAM");
                                        String WITH_CRATE = jsonObject.getString("WITH_CRATE");
                                        String WERKS = jsonObject.getString("WERKS");
                                        String AEDAT = jsonObject.getString("AEDAT");
                                        String GBSTK = jsonObject.getString("GBSTK");
                                        String STOCK_TAKE = jsonObject.getString("STOCK_TAKE");
                                        String LGPLA_HIGH = jsonObject.getString("LGPLA_HIGH");
                                        String MANDT = jsonObject.getString("MANDT");
                                        String LGPLA_LOW = jsonObject.getString("LGPLA_LOW");
                                        String ERDAT = jsonObject.getString("ERDAT");
                                        String LGNUM = jsonObject.getString("LGNUM");
                                        String UZEIT = jsonObject.getString("UZEIT");
                                        String ERNAM = jsonObject.getString("ERNAM");

                                        dtStockTakeDetails.add(new StockTakeProcessExHeaderModel(LGTYP,AETIM,AENAM,WITH_CRATE,WERKS
                                                ,AEDAT,GBSTK
                                                ,STOCK_TAKE,LGPLA_HIGH,MANDT,LGPLA_LOW,ERDAT,LGNUM,UZEIT,ERNAM));

                                        JSONArray jsonArray = responsebody.getJSONArray("ET_BIN");
                                        for (int i = 1 ; i<jsonArray.length();i++){
                                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                                            String lgtyp = jsonObject1.getString("LGTYP");
                                            String mandt = jsonObject1.getString("MANDT");
                                            String werks = jsonObject1.getString("WERKS");
                                            String lvorm = jsonObject1.getString("LVORM");
                                            String erdat = jsonObject1.getString("ERDAT");
                                            String lgpla = jsonObject1.getString("LGPLA");
                                            String lgnum = jsonObject1.getString("LGNUM");
                                            String uzeit = jsonObject1.getString("UZEIT");
                                            String stock_take = jsonObject1.getString("STOCK_TAKE");
                                            String ernam = jsonObject1.getString("ERNAM");

                                            dtstocktakeBin.add(new StockTakeProcessEtBinModel(lgtyp,mandt,werks,lvorm,erdat,lgpla
                                                    ,lgnum,uzeit,stock_take,ernam));

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
    public void onClick(View view) {
        switch (view.getId())

        {
            case R.id.save:

                try {
                    loadSaveData();
                }catch (Exception e)
                {
                    box.getErrBox(e);
                }
                break;

            case R.id.back:


                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         fm.popBackStack();

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });
                break;

            case R.id.bin_scan:
                scanner="bin";
                if(CameraCheck.isCameraAvailable(con))
                    IntentIntegrator.forSupportFragment(Scan_Stock_take_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
                break;
            case R.id.bar_scan:
                scanner="bar";
                if(CameraCheck.isCameraAvailable(con))
                    IntentIntegrator.forSupportFragment(Scan_Stock_take_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
                break;
            case R.id.crate_scan:
                scanner="crate";
                if(CameraCheck.isCameraAvailable(con))
                    IntentIntegrator.forSupportFragment(Scan_Stock_take_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
                break;

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null ) {

            Log.d(TAG, TAG + " scanned result...");
            box.getBox("Scanner Err","Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);


        }


    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStack();
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

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        return false;
    }


    /*    * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
