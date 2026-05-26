package com.v2retail.dotvik.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.scanner.ScannerActivity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PostDataOffiineFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PostDataOffiineFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private EditText site;
    private EditText employeeId;
    private EditText irod;
    private EditText barcode;
    private EditText barcodeScanned;
    private EditText scanQty;
    private EditText totalScanQty;
    private Button save;

    ActivityResultLauncher<Intent> scannerActivity;

    AlertBox box;
    ProgressDialog dialog;
    String URL="";
    String WERKS="";
    String USER="";
    private final String TAG = getClass().getName();
    private JSONArray jsonArray;
    private int sum = 0;
    private int totalSum = 0;
    private Button barcodeScan,irodScan;
    private String scanned;
    private String selectedSize = "";
    private ToggleButton selectedChild = null;
    private Context con;
    public PostDataOffiineFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PostDataOffiineFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PostDataOffiineFragment newInstance(String param1, String param2) {
        PostDataOffiineFragment fragment = new PostDataOffiineFragment();
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
        con = getContext();
        box = new AlertBox(getContext());
        dialog=new ProgressDialog(getContext());
        SharedPreferencesData data=new SharedPreferencesData(getContext());
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        if(!URL.isEmpty())
            Log.d(TAG,"URL->"+URL);
        if(!WERKS.isEmpty())
            Log.d(TAG,"WERKS->"+WERKS);
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);

        jsonArray = new JSONArray();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_data_offiine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        site = view.findViewById(R.id.site);
        employeeId = view.findViewById(R.id.employeeId);
        irod = view.findViewById(R.id.irod);
        barcode = view.findViewById(R.id.barcode);
        barcodeScanned = view.findViewById(R.id.barcodeScanned);
        scanQty = view.findViewById(R.id.scanQty);
        totalScanQty = view.findViewById(R.id.totalScanQty);
        save = view.findViewById(R.id.save);
        barcodeScan = view.findViewById(R.id.barcodeScan);
        irodScan = view.findViewById(R.id.irodScan);
        save.setOnClickListener(this);
        barcodeScan.setOnClickListener(this);
        irodScan.setOnClickListener(this);

        ((ToggleButton) view.findViewById(R.id.btn_size_ml)).setOnClickListener(this);
        ((ToggleButton) view.findViewById(R.id.btn_size_rl)).setOnClickListener(this);
        ((ToggleButton) view.findViewById(R.id.btn_size_m)).setOnClickListener(this);
        ((ToggleButton) view.findViewById(R.id.btn_size_c)).setOnClickListener(this);
        ((ToggleButton) view.findViewById(R.id.btn_size_v2s)).setOnClickListener(this);
        addEditorListeners();
        employeeId.requestFocus();
        site.setText(WERKS);
        scannerActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if(data.getStringExtra("ScanType").equals("irodScan")) {
                        irod.setText(data.getStringExtra("ScannedData"));
                        if (TextUtils.isEmpty(data.getStringExtra("ScannedData"))) {
                            box.getBox("Alert", "Enter Irod No!");
                            return;
                        }
                        loadIrodData();
                    }else{
                        barcodeScanned.setText(data.getStringExtra("ScannedData"));
                        barcode.setText(data.getStringExtra("ScannedData"));
                        if (TextUtils.isEmpty(data.getStringExtra("ScannedData"))) {
                            box.getBox("Alert", "Enter Barcode No!");
                            return;
                        }
                        loadBarcodeData();
                    }
                }
            }
        });
    }

    private void onToggle(View view) {
        RadioGroup rGroup = ((RadioGroup)view.getParent());
        selectedSize = "";
        for (int j = 0; j < rGroup.getChildCount(); j++) {
            final ToggleButton child = (ToggleButton) rGroup.getChildAt(j);
            if(child.getId() == view.getId()){
                child.setChecked(true);
                child.setTextColor(getResources().getColor(R.color.stock_gen));
                selectedChild = child;
            }else{
                child.setChecked(false);
                child.setTextColor(Color.BLACK);
            }
        }
    }

    void addEditorListeners() {

        site.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = site.getText().toString();
                    if (!(crate.equals("") || crate.length() < 0 || crate.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(site.getWindowToken(), 0);
                            loadSiteData();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        UIFuncs.errorSound(con);
                        box.getBox("Alert!!", "Please provide site");
                    }
                }
                return false;
            }
        });
        employeeId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String article = employeeId.getText().toString();
                    if (!(article.equals("") || article.length() < 0 || article.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(employeeId.getWindowToken(), 0);
                            loadEmployeeData();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        UIFuncs.errorSound(con);
                        box.getBox("Alert!!", "Provide Employee ID");
                    }
                }
                return false;
            }
        });

        barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  ean = barcode.getText().toString().toUpperCase();
                    if(ean.length()>0) {
                        loadBarcodeData();
                        return true;
                    }
                }
                return false;
                /*
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String article = employeeId.getText().toString();
                    if (!(article.equals("") || article.length() < 0 || article.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(employeeId.getWindowToken(), 0);
                            loadBarcodeData();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        UIFuncs.errorSound(con);
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }*/
            }
        });

        irod.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String article = irod.getText().toString();
                    if (!(article.equals("") || article.length() < 0 || article.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(employeeId.getWindowToken(), 0);
                            loadIrodData();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        UIFuncs.errorSound(con);
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }
                return false;
            }
        });

    }

    void addTextChangeListners() {
        site.addTextChangedListener(new TextWatcher() {
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
                            loadSiteData();
                        }
                    });
                }
            }
        });

        employeeId.addTextChangedListener(new TextWatcher() {
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
                            loadEmployeeData();
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
                            loadBarcodeData();
                        }
                    });
                }
            }
        });
        irod.addTextChangedListener(new TextWatcher() {
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
                            loadIrodData();
                        }
                    });
                }
            }
        });
    }

    private void loadIrodData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(irod.getText().toString().trim())&& irod.equals("")) {
                    UIFuncs.errorSound(con);
                    box.getBox("Alert", "Please Enter Site");
                    dialog.dismiss();
                    return;

                }
                try {
                    irod.setEnabled(false);
                    barcode.requestFocus();
                    dialog.dismiss();
                }catch (Exception e)
                {   dialog.dismiss();
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }

    private void loadSiteData(){
        employeeId.requestFocus();
        dialog.dismiss();
    }
    private void loadEmployeeData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String siteStr = site.getText().toString();
                String empStr = employeeId.getText().toString();
                if (TextUtils.isEmpty(site.getText().toString().trim())&& site.equals("")) {
                    UIFuncs.errorSound(con);
                    box.getBox("Alert", "Please Enter Site");
                    dialog.dismiss();
                    return;

                }
                if(empStr==null|| empStr.equals("")||empStr.isEmpty()){
                    UIFuncs.errorSound(con);
                    box.getBox("Alert", "Please Enter Employee Id");
                    dialog.dismiss();
                    return;
                }

                try {
                    site.setEnabled(false);
                    employeeId.setEnabled(false);
                    irod.requestFocus();
                    dialog.dismiss();

                }catch (Exception e)
                {   dialog.dismiss();
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

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(barcode.getText().toString().trim())&& barcode.equals("")) {
                    UIFuncs.blinkEffectOnError(con,barcode,true);
                    box.getBox("Alert", "Please Enter Barcode");
                    dialog.dismiss();
                    return;
                }
                if(selectedChild == null){
                    UIFuncs.errorSound(con);
                    box.getBox("Alert", "Please Select Size");
                    dialog.dismiss();
                    barcode.setText("");
                    barcode.requestFocus();
                    return;
                }
                String childText = selectedChild.getText().toString();
                try {
                    if(childText.equals("M")){
                        dialog.dismiss();
                        setSize(selectedChild);
                    }
                    else{
                        selectedSize = "";
                        scanBarcode(barcode.getText().toString());
                    }

                }catch (Exception e)
                {   dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    private void setSize(ToggleButton child){

        final EditText txtSize = new EditText(getContext());
        txtSize.setHint("Enter Size");
        AlertDialog inputDialog = new AlertDialog.Builder(getContext())
                .setTitle("Size")
                .setMessage("")
                .setView(txtSize)
                .setPositiveButton("Confirm",null)
                .setNegativeButton("Cancel",null).create();
        inputDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = ((AlertDialog) inputDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                Button buttonNeg = ((AlertDialog) inputDialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String size = txtSize.getText().toString().toUpperCase().trim();
                        if(size.length() == 0){
                            UIFuncs.errorSound(con);
                            box.getBox("Alert", "Invalid size. Please provide size");
                        }
                        else {
                            selectedSize = size;
                            scanBarcode(barcode.getText().toString());
                            inputDialog.dismiss();
                        }
                    }
                });
                buttonNeg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        barcode.setText("");
                        barcode.requestFocus();
                    }
                });
            }
        });
        inputDialog.show();
    }

    private void scanBarcode(String barcodeStr){

        barcodeScanned.setText(barcodeStr);
        boolean check = true;
        for (int i = 0; i<jsonArray.length();i++){
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if (jsonObject.get("ARTICLE").equals(barcodeStr) && jsonObject.get("ART_TYPE").equals(selectedChild.getText()) && jsonObject.get("ZSIZE").equals(selectedSize)){
                    String sq = jsonObject.getString("QUANTITY");
                    sum = Integer.valueOf(sq);
                    jsonObject.put("QUANTITY", String.valueOf(Integer.valueOf(sq) +1));
                    check = false;
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
        if (check){
            sum = 0;

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("EMP_CODE",employeeId.getText().toString());
                jsonObject.put("SITE",WERKS);
                jsonObject.put("GANDOLA",irod.getText().toString());
                jsonObject.put("ARTICLE",barcodeStr);
                jsonObject.put("QUANTITY",1);
                jsonObject.put("ART_TYPE",selectedChild.getText());
                jsonObject.put("ZSIZE",selectedSize);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        Log.v("data",jsonArray.toString());
        sum = sum+1;
        totalSum = totalSum+1;
        totalScanQty.setText(String.valueOf(totalSum));
        scanQty.setText(String.valueOf(sum));
        barcode.setText("");
        barcode.requestFocus();
        dialog.dismiss();

    }



    private void saveData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {


                if (TextUtils.isEmpty(site.getText().toString().trim())&& site.equals("")) {
                    box.getBox("Alert", "Please Enter Site");
                    dialog.dismiss();
                    return;

                }
                if (jsonArray.length()<=0){
                    box.getBox("Alert", "Please Scan All Data");
                    dialog.dismiss();
                    return;
                }

                try {
                    sendToServer();
                }catch (Exception e)
                {   dialog.dismiss();
                    box.getErrBox(e);
                }

            }
        }, 2000);
    }
    private void sendToServer(){

        String rfc = "ZWM_STORE_PUSHTOSAP_0001_IROD";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", employeeId.getText().toString());
            params.put("ET_DATA", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
            return;
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
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                irod.setEnabled(true);
                                                irod.setText("");
                                                barcode.setText("");
                                                barcodeScanned.setText("");
                                                totalScanQty.setText("");
                                                scanQty.setText("");
                                                sum = 0;
                                                totalSum = 0;
                                                selectedSize = "";
                                                selectedChild.setChecked(false);
                                                selectedChild.setTextColor(Color.BLACK);
                                                selectedChild = null;
                                                jsonArray = new JSONArray();
                                                irod.requestFocus();
                                            }
                                        });
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

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){

            case R.id.save:
                saveData();
                break;

            case R.id.irodScan:
                scanned ="irodScan";
                intent = new Intent(PostDataOffiineFragment.this.getActivity(),ScannerActivity.class);
                intent.putExtra("ScanType",scanned);
                scannerActivity.launch(intent);

                //Calling Zxing library scanner intent
                /*if (CameraCheck.isCameraAvailable(getContext()))
                    IntentIntegrator.forSupportFragment(PostDataOffiineFragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).setDesiredBarcodeFormats(Vars.PRODUCT_CODE_TYPES).initiateScan();*/

                break;

            case R.id.barcodeScan:
                scanned = "barcode";
                intent = new Intent(PostDataOffiineFragment.this.getActivity(),ScannerActivity.class);
                intent.putExtra("ScanType",scanned);
                scannerActivity.launch(intent);

                //Calling Zxing library scanner intent
                /*if (CameraCheck.isCameraAvailable(getContext()))
                    IntentIntegrator.forSupportFragment(PostDataOffiineFragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).setDesiredBarcodeFormats(Vars.PRODUCT_CODE_TYPES).initiateScan();*/

                break;

            case R.id.btn_size_ml:
                onToggle(v);
                break;

            case R.id.btn_size_rl:
                onToggle(v);
                break;

            case R.id.btn_size_m:
                onToggle(v);
                break;

            case R.id.btn_size_c:
                onToggle(v);
                break;

            case R.id.btn_size_v2s:
                onToggle(v);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Code Deprecated as additional Scanner Activity is Introduced
        //Removed the usage of Zxing library from this module for now
        /*Log.d(TAG, TAG + " scanned result...");
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {

            box.getBox("Scanner Err", "Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);
            Log.v(TAG, "Scanned = " + scanned);

            if (scanContent != null) {

                switch (scanned){
                    case "irodScan":
                        irod.setText(scanContent);
//                        code = scanContent;
                        if (TextUtils.isEmpty(scanContent)) {

                            box.getBox("Alert", "Enter Irod No!");

                            return;
                        }

                        loadIrodData();
                        break;

                    case "barcode":
                        barcode.setText(scanContent);
//                        code = scanContent;
                        if (TextUtils.isEmpty(scanContent)) {
                            box.getBox("Alert", "Enter Barcode No!");
                            return;
                        }
                        loadBarcodeData();
                        break;
                }




            } else {
                box.getBox("Scanner Err", "No Content Received. Please Scan Again");

            }
        }
*/

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