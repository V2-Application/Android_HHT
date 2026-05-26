package com.v2retail.dotvik.dc;

import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtLtbpModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GRCPutwayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRCPutwayFragment extends Fragment implements View.OnClickListener {

    private EditText gr,wHouse,bin,cur_bin,barcode,sq,rq,tsq,tpoq;
    private Activity activity;
    private String URL ="";
    private String USER ="";
    private String TAG ="GRCPutwayFragment";
    private View view;
    private ProgressDialog dialog = null;
    AlertBox box ;
    private int OpenQty = 0;
    private int sum = 0;
    private int tsqCheck = 0;
    private int qty = 0;
    boolean check ;
    private Context con;

    private Button submit,reset,back;
    private List<EtLtbpModel> ltbpModels;
    private List<EtEanDataModel> etEanDataModels;
    private JSONArray jsonArray;
    private String binNum ="";
    private int tqoqSum = 0;
    private FragmentManager fm;
    private String grNum;
    private CheckBox unScan;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GRCPutwayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GRCPutwayFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GRCPutwayFragment newInstance(String param1, String param2) {
        GRCPutwayFragment fragment = new GRCPutwayFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_g_r_c_putway, container, false);
        con = getContext();
        submit = view.findViewById(R.id.submit);
        reset = view.findViewById(R.id.reset);
        back = view.findViewById(R.id.back);
        gr = view.findViewById(R.id.gr);
        wHouse = view.findViewById(R.id.wHouse);
        cur_bin = view.findViewById(R.id.cur_bin);
        barcode = view.findViewById(R.id.barcode);
        sq = view.findViewById(R.id.sq);
        rq = view.findViewById(R.id.rq);
        tsq = view.findViewById(R.id.tsq);
        tpoq = view.findViewById(R.id.tpoq);
        bin = view.findViewById(R.id.bin);
        unScan = view.findViewById(R.id.unScan);

        submit.setOnClickListener(this);
        back.setOnClickListener(this);
        reset.setOnClickListener(this);
        init();
        onEditListener();
        addTextChangeListners();
        return view;
    }

    private void init() {
        activity = getActivity();
        ltbpModels = new ArrayList<>();
        etEanDataModels = new ArrayList<>();
        fm = getFragmentManager();
        jsonArray = new JSONArray();
        wHouse.setEnabled(false);
        sq.setEnabled(false);
        rq.setEnabled(false);
        tsq.setEnabled(false);
        tpoq.setEnabled(false);
        cur_bin.setEnabled(false);
        bin.setEnabled(false);
        barcode.setEnabled(false);
        gr.requestFocus();
        box = new AlertBox(activity);
        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);
    }
    private void onEditListener() {

        gr.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String grNumber = gr.getText().toString();
                    if (grNumber == null || grNumber.length() < 0 || grNumber.equals("")) {
                        box.getBox("Alert", "Scan Gr No!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(gr.getWindowToken(), 0);
                            loadGrData();
                            Log.v("dialog","loadGrData--- scanner");
                            return true;
                        }catch (Exception e){
                            box.getErrBox(e);
                        }

                    }
                }

                return false;
            }
        });

        bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String binNumber = bin.getText().toString();
                    if (binNumber == null || binNumber.length() < 0 || binNumber.equals("") ) {
                        box.getBox("Alert", "Enter Bin Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(bin.getWindowToken(), 0);
                        loadBinData();
                        return true;
                    }
                }

                return false;
            }
        });

        barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String barcodeNumber = barcode.getText().toString();
                    if (barcodeNumber == null || barcodeNumber.length() < 0 || barcodeNumber.equals("") ) {
                        box.getBox("Alert", "Scan Barcode !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin.getWindowToken(), 0);
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

    void addTextChangeListners() {

        gr.addTextChangedListener(new TextWatcher() {
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

                String grNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Gr : " +  grNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadGrData();
                            Log.v("dialog","loadGrData --- textWatcher");
                        }
                    });
                }
            }
        });


        bin.addTextChangedListener(new TextWatcher() {
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

                String poString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Bin : " +  poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBinData();
                        }
                    });
                }
            }
        });

        barcode.addTextChangedListener(new TextWatcher() {
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

                String barcode = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Barcode : " +  barcode);

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

    private void loadGrData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        Log.v("dialog","dialog show");
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String grNumber = gr.getText().toString();
                if (grNumber == null || grNumber.length() < 0 || grNumber.equals("")) {
                    UIFuncs.blinkEffectOnError(con,gr,true);
                    box.getBox("Alert", "Scan Gr No!");
                    dialog.dismiss();
                    return;
                }
                etEanDataModels.clear();
                ltbpModels.clear();
                getGrNumberData(grNumber);
            }
        }, 1500);
    }

    private void loadBinData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        String wH = wHouse.getText().toString();
        String binNumber = bin.getText().toString();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (binNumber == null || binNumber.length() < 0 || binNumber.equals("") ) {
                    UIFuncs.blinkEffectOnError(con,bin,true);
                    box.getBox("Alert", "Please Enter Bin Number");
                    dialog.dismiss();
                    return;
                }

                getBinNumberData(binNumber,wH);

            }
        }, 1000);
    }

    private void loadBarcodeData(){


        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String barcodeNumber = barcode.getText().toString();
                if (barcodeNumber == null || barcodeNumber.length() < 0 || barcodeNumber.equals("") ) {
                    UIFuncs.blinkEffectOnError(con,barcode,true);
                    box.getBox("Alert", "Please Enter Barcode ");
                    dialog.dismiss();
                    return;
                }

                getBarCodeData(barcodeNumber);

            }
        }, 1000);
    }


    private void getBarCodeData(String barcodeNumber) {

        String MATNR ="";
        String UMREZ ="";
        String EANNR ="";
        boolean flag= false;
        boolean flag1 = true;
        int ERFMG;
        for (int i =0;i<etEanDataModels.size();i++){

            if (etEanDataModels.get(i).getEAN11().equals(barcodeNumber)){
                MATNR = etEanDataModels.get(i).getMATNR();
                UMREZ = etEanDataModels.get(i).getUMREZ();
                EANNR = etEanDataModels.get(i).getEANNR();
                flag =true;
            }
        }

        if (flag){
            for (int i =0;i<ltbpModels.size();i++){
                if (ltbpModels.get(i).getMATNR().equals(MATNR)){
                    rq.setText(ltbpModels.get(i).getMENGE());
                    OpenQty = Double.valueOf(ltbpModels.get(i).getMENGE()).intValue();
                    ERFMG = Double.valueOf(ltbpModels.get(i).getERFMG()).intValue();
                    bin.setEnabled(false);
                    flag1 = false;
                    boolean check =true;
                    boolean unScanFlag = true;
                    for (int a=0;a<jsonArray.length();a++){
                        try {
                            JSONObject jsonObject1 = jsonArray.getJSONObject(a);
                            String m = jsonObject1.getString("MATNR");
                            if (m.equals(ltbpModels.get(i).getMATNR())) {

                                if (unScan.isChecked()) {
                                    if (Integer.valueOf(jsonObject1.getString("MENGE")) - Integer.valueOf(UMREZ) < 0) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", "Removed Quantity is less then already scanned quantity.");
                                        barcode.setText("");
                                        barcode.requestFocus();
                                        dialog.dismiss();
                                        return;
                                    }
                                    String sq = jsonObject1.getString("MENGE");
                                    sum = Integer.valueOf(sq);
                                    jsonObject1.put("MENGE", String.valueOf(Integer.valueOf(sq) - Integer.valueOf(UMREZ)));
                                    unScanFlag = false;

                                }else {
                                    String sq = jsonObject1.getString("MENGE");
                                    sum =Integer.valueOf(sq);
                                    if (sum<ERFMG) {
                                        jsonObject1.put("MENGE", String.valueOf(Integer.valueOf(sq) + Integer.valueOf(UMREZ)));
                                    }
                                }

                                check = false;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    if (unScanFlag && unScan.isChecked()){
                        UIFuncs.errorSound(con);
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Alert", "No Data Found for Removed Quantity - "+ barcodeNumber);
                        barcode.setText("");
                        barcode.requestFocus();
                        dialog.dismiss();
                        return;
                    }

                    if (check&&!unScan.isChecked())
                        sum = 0;
                    if (unScan.isChecked()){
                        sum =  sum - Double.valueOf(UMREZ).intValue();
                    }else {
                        sum =  sum + Double.valueOf(UMREZ).intValue();
                    }

                    if (sum <=  ERFMG){
                        if (unScan.isChecked()){
                            tsqCheck = tsqCheck - Double.valueOf(UMREZ).intValue();
                        }else {
                            tsqCheck = tsqCheck + Double.valueOf(UMREZ).intValue();
                        }

                        Log.v(TAG, String.valueOf(tsqCheck));
                        tsq.setText(String.valueOf(tsqCheck));
                        if (sum==0&&!unScan.isChecked()){
                            sq.setText(String.valueOf(Double.valueOf(UMREZ).intValue()));
                        }else {
                            sq.setText(String.valueOf(sum));
                        }



                        try {
                            if (check&&!unScan.isChecked()){
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("MATNR",ltbpModels.get(i).getMATNR());
                                jsonObject.put("MENGE",String.valueOf(Integer.valueOf(UMREZ)));
                                jsonObject.put("LGPLA",binNum);
                                jsonArray.put(jsonObject);
                            }
                            Log.v(TAG,"Save Data-->"+jsonArray.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (flag1){
                            UIFuncs.errorSound(con);
                            box.getBox("Alert", "Material " + MATNR + " not found in Po");
                            barcode.setText("");
                            barcode.requestFocus();
                        }

                    } else {
                        UIFuncs.errorSound(con);
                        box.getBox("Alert", "Scanned Qty can't be greater than Open Qty ");

                        submit.requestFocus();

                    }

                }
            }
        }else {
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Invalid Barcode");
            barcode.setText("");
            barcode.requestFocus();
        }
        barcode.setText("");
        dialog.dismiss();

    }

    void getGrNumberData(String grNumber) {

        String rfc = "ZWM_GR_GET_DETAILS";
        int year = Calendar.getInstance().get(Calendar.YEAR);
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_MBLNR", grNumber);
            params.put("IM_MJAHR", year);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                Log.v("dialog","dialog dismiss");
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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        Log.v(TAG,"EX_RETURN"+returnobj.toString());
                                        gr.setText("");
                                        gr.requestFocus();
                                        return;
                                    } else {
                                        bin.setEnabled(true);
                                        barcode.setEnabled(false);
                                        bin.requestFocus();
                                        bin.setText("");
                                        grNum = grNumber;
                                        JSONArray ET_MSEG_DATA = responsebody.getJSONArray("ET_MSEG_DATA");
                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        String EX_LGNUM = responsebody.getString("EX_LGNUM");
                                        wHouse.setText(EX_LGNUM);
                                        for (int i=0;i<ET_MSEG_DATA.length();i++){
                                            String MATNR = ET_MSEG_DATA.getJSONObject(i).getString("MATNR");
                                            String ERFMG = ET_MSEG_DATA.getJSONObject(i).getString("ERFMG");
                                            String MENGE = ET_MSEG_DATA.getJSONObject(i).getString("MENGE");

                                            for (int j = 0; j <ERFMG.length();j++){
                                                if (ERFMG.charAt(j)>='0'&& ERFMG.charAt(j)<='9')
                                                    check = true;
                                                else
                                                    check = false;
                                            }
                                            if (check) {
                                                Log.v(TAG+" 1-----",String.valueOf(tqoqSum));
                                                tqoqSum = tqoqSum + Double.valueOf(ERFMG).intValue();
                                            }
                                            Log.v(TAG+" 2-----",String.valueOf(tqoqSum));
                                            ltbpModels.add(new EtLtbpModel(MATNR,ERFMG,MENGE));
                                        }
                                        for (int i=0;i<ET_EAN_DATA.length();i++){
                                            String MATNR = ET_EAN_DATA.getJSONObject(i).getString("MATNR");
                                            String UMREZ = ET_EAN_DATA.getJSONObject(i).getString("UMREZ");
                                            String MANDT = ET_EAN_DATA.getJSONObject(i).getString("MANDT");
                                            String EAN11 = ET_EAN_DATA.getJSONObject(i).getString("EAN11");
                                            etEanDataModels.add(new EtEanDataModel(MATNR,UMREZ,MANDT,EAN11));
                                        }
                                        tpoq.setText(String.valueOf(tqoqSum));
                                        tqoqSum =0;
                                        Log.v("ETMSEGData-->",ET_MSEG_DATA.toString());
                                        Log.v("EANData-->",ET_EAN_DATA.toString());
                                        return;
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

    private void saveDataToServer(){
        dialog  = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (grNum!=""&&jsonArray.length()<=0) {
                    UIFuncs.errorSound(con);
                    box.getBox("Alert", "Scan All Details");
                    dialog.dismiss();
                    return;

                }

                try {
                    sendAndRequestResponse();
                }catch (Exception e)
                {    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    void sendAndRequestResponse() {

        String rfc = "ZWM_TO_CREATE_FROM_GR_DATA";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_MBLNR", grNum);
            params.put("IM_MJAHR", Calendar.getInstance().get(Calendar.YEAR));
            params.put("IM_USER", USER);
            params.put("IT_DATA", jsonArray);

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));

                                        return;
                                    } else {
                                        tqoqSum = 0;
                                        tsqCheck = 0;
                                        OpenQty = 0;
                                        sum = 0;
                                        ltbpModels.clear();
                                        etEanDataModels.clear();
                                        jsonArray = new JSONArray();
                                        getGrNumberData(grNum);

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("", returnobj.getString("MESSAGE"));
                                        bin.setEnabled(true);
                                        bin.setText("");
                                        bin.requestFocus();
                                        cur_bin.setText("");
                                        barcode.setText("");
                                        barcode.setEnabled(false);
                                        sq.setText("");
                                        rq.setText("");
                                        tsq.setText("");
                                        tsq.setText("");
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

        // no retry policy on save
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    void getBinNumberData(String binNumber,String wH){

        String rfc = "ZWM_GET_BIN_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_LGNUM", wH);
            params.put("IM_LGPLA", binNumber);

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));

                                        bin.setText("");
                                        bin.requestFocus();
                                        return;
                                    } else {
                                        barcode.setEnabled(true);
                                        cur_bin.setText(binNumber);
                                        barcode.requestFocus();
                                        binNum = binNumber;

                                        return;
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
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.reset:

                clear();
                break;

            case R.id.submit:

                saveDataToServer();
                break;

            case R.id.back:
                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // fm.popBackStack();
                        //  ApplicationController.getInstance().refreshObservable().notifyObservers();

                        fm.popBackStack();

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });

        }
    }

    private void clear() {

        gr.setText("");
        wHouse.setText("");
        cur_bin.setText("");
        barcode.setText("");
        sq.setText("");
        rq.setText("");
        tsq.setText("");
        tpoq.setText("");
        bin.setText("");
        gr.requestFocus();
        wHouse.setEnabled(false);
        cur_bin.setEnabled(false);
        barcode.setEnabled(false);
        sq.setEnabled(false);
        rq.setEnabled(false);
        tsq.setEnabled(false);
        tpoq.setEnabled(false);
        bin.setEnabled(false);
        jsonArray = new JSONArray();
        tqoqSum = 0;
        tsqCheck = 0;
        OpenQty = 0;
        sum = 0;

    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRC Putway");
    }
}