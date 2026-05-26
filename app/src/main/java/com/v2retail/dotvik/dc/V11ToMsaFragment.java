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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
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
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link V11ToMsaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class V11ToMsaFragment extends Fragment implements View.OnClickListener {

    private EditText site,location,crate,scan_bin,stock_qty,article;
    private RadioButton with_crate,without_crate;
    private Button save,back,reset;
    private View view;
    private AlertBox box;
    private Activity activity;
    private ProgressDialog dialog;
    private String URL ="";
    private String WERKS ="";
    private String USER ="";
    private String TAG ="V11ToMsaFragment";
    private LinearLayout crateLayout;
    private JSONArray saveData;
    private String crateNum ="";
    private String loca="";
    private String scanB = "";
    private int sqSum = 0;
    private CheckBox unScan;
    FragmentManager fm;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public V11ToMsaFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment V11ToMsaFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static V11ToMsaFragment newInstance(String param1, String param2) {
        V11ToMsaFragment fragment = new V11ToMsaFragment();
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
        view = inflater.inflate(R.layout.fragment_v11_to_msa, container, false);

        site = view.findViewById(R.id.site);
        location = view.findViewById(R.id.location);

        with_crate = view.findViewById(R.id.with_crate);
        without_crate = view.findViewById(R.id.without_crate);
        scan_bin = view.findViewById(R.id.scan_bin);
        crate = view.findViewById(R.id.crate);
        stock_qty = view.findViewById(R.id.stock_qty);
        save = view.findViewById(R.id.save);
        back = view.findViewById(R.id.back);
        crateLayout = view.findViewById(R.id.crateLayout);
        article = view.findViewById(R.id.article);
        reset = view.findViewById(R.id.reset);
        unScan = view.findViewById(R.id.unScan);
        fm = getFragmentManager();

        save.setOnClickListener(this);
        reset.setOnClickListener(this);
        back.setOnClickListener(this);


        init();
        onEditListener();
        addTextChangeListeners();
        return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        saveData = new JSONArray();
        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);

        with_crate.setChecked(true);
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        URL = data.read("URL");

        if(!WERKS.isEmpty()) {
            Log.d(TAG, "WERKS->" + WERKS);
            site.setText(WERKS);
        }
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);


        loadLocationData();

        with_crate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    crateLayout.setVisibility(View.VISIBLE);
                    without_crate.setChecked(false);
                    crate.requestFocus();
                }

            }
        });

        without_crate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    crateLayout.setVisibility(View.GONE);
                    with_crate.setChecked(false);
                    scan_bin.requestFocus();
                }

            }
        });

    }

    private void onEditListener() {

        location.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String locationNumber = location.getText().toString();
                    if (locationNumber == null || locationNumber.length() < 0 || locationNumber.equals("")) {
                        box.getBox("Alert", "Scan Location !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(location.getWindowToken(), 0);
                            loadLocationData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });

        scan_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String scanBin = scan_bin.getText().toString();
                    if (scanBin == null || scanBin.length() < 0 || scanBin.equals("") ) {
                        box.getBox("Alert", "Scan Crate Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(scan_bin.getWindowToken(), 0);
                        loadScanBinData();
                        return true;
                    }
                }

                return false;
            }
        });


        crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String crateNo = crate.getText().toString();
                    if (crateNo == null || crateNo.length() < 0 || crateNo.equals("") ) {
                        box.getBox("Alert", "Scan Crate Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(crate.getWindowToken(), 0);
                        loadCrateData();
                        return true;
                    }
                }

                return false;
            }
        });
        article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String crateNo = crate.getText().toString();
                    if (crateNo == null || crateNo.length() < 0 || crateNo.equals("") ) {
                        box.getBox("Alert", "Scan Article Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(crate.getWindowToken(), 0);
                        loadArticleData();
                        return true;
                    }
                }

                return false;
            }
        });

    }
    private void addTextChangeListeners() {

        location.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned Location  : " +  locationNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadLocationData();
                        }
                    });
                }
            }
        });


        crate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String crateString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Crate Number : " +  crateString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadCrateData();
                        }
                    });
                }
            }
        });

        scan_bin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String barcodeString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Crate Number : " +  barcodeString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadScanBinData();
                        }
                    });
                }
            }
        });

        article.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String barcodeString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Article Number : " +  barcodeString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadArticleData();
                        }
                    });
                }
            }
        });
    }


    private void loadLocationData() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String s = site.getText().toString().trim();
        String l = location.getText().toString().trim();


        if (s == null || s.length() < 0 || s.equals("")) {
            box.getBox("Alert", "Please fill Site.");
            return;
        }

        if (l == null || l.length() < 0 || l.equals("")) {
            box.getBox("Alert", "Please fill Location.");
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateNumberJson(s,l);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void loadCrateData() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String s = site.getText().toString().trim();
        String c = crate.getText().toString().trim();


        if (s == null || s.length() < 0 || s.equals("")) {
            box.getBox("Alert", "Please fill Site.");
            return;
        }

        if (c == null || c.length() < 0 || c.equals("")) {
            box.getBox("Alert", "Please fill Crate.");
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    crateValidation(s,c);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void loadScanBinData() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String s = site.getText().toString().trim();
        String scanBin = scan_bin.getText().toString().trim();


        if (s == null || s.length() < 0 || s.equals("")) {
            box.getBox("Alert", "Please fill Site.");
            return;
        }

        if (scanBin == null || scanBin.length() < 0 || scanBin.equals("")) {
            box.getBox("Alert", "Please fill Scan Bin.");
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    scanBinValidation(s,scanBin);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void loadArticleData() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String s = site.getText().toString().trim();
        String a = article.getText().toString().trim();


        if (s == null || s.length() < 0 || s.equals("")) {
            box.getBox("Alert", "Scan Site First.");
            return;
        }

        if (a == null || a.length() < 0 || a.equals("")) {
            box.getBox("Alert", "Scan Article First.");
            return;
        }

        if (loca == null || loca.length() < 0 || loca.equals("")) {
            box.getBox("Alert", "Scan Location First.");
            return;
        }

        if (crateLayout.getVisibility()==View.VISIBLE) {
            if (crateNum == null || crateNum.length() < 0 || crateNum.equals("")) {
                box.getBox("Alert", "Scan Crate First.");
                return;
            }
        }

        if (scanB == null || scanB.length() < 0 || scanB.equals("")) {
            box.getBox("Alert", "Scan Bin First.");
            return;
        }




        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    articleData(s,a);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    void sendValidateNumberJson(String siteDc, String loc) {

        String rfc = "ZWM_RFC_VALIDATE_DC_SLOC";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", siteDc);
            params.put("IM_LGORT", loc);
            params.put("IM_V11", "X");




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

                                        location.setText("");
                                        location.requestFocus();
                                        return;
                                    } else {
                                        loca = loc;
                                        crate.requestFocus();
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

    void crateValidation(String s, String c){

        String rfc = "ZWM_RFC_STOCK_TAKE_CRATE_VALI";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", c);
            params.put("IM_SITE", s);




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

                                        crate.setText("");
                                        crate.requestFocus();
                                        return;
                                    } else {
                                        crateNum = c;
                                        scan_bin.requestFocus();
                                        with_crate.setEnabled(false);
                                        without_crate.setEnabled(false);
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

    void scanBinValidation(String s , String scanBin){

        String rfc = "ZWM_RFC_STOCK_TAKE_BIN_VALI";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_BIN", scanBin);
            params.put("IM_SITE", s);

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

                                        scan_bin.setText("");
                                        scan_bin.requestFocus();
                                        return;
                                    } else {

                                        scanB = scanBin;
                                        article.requestFocus();
                                        with_crate.setEnabled(false);
                                        without_crate.setEnabled(false);
                                        scan_bin.setEnabled(false);
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

    void articleData(String s, String a){

        String rfc = "ZWM_RFC_STOCK_TAKE_ARTI_VALI";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_BARCODE", a);
            params.put("IM_SITE", s);




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

                                        article.setText("");
                                        article.requestFocus();
                                        return;
                                    } else {
                                        JSONObject  data = responsebody.getJSONObject("EX_MARM");
                                        boolean flag = true;
                                        boolean unScanFlag = true;
                                        JSONObject  jsonObject = new JSONObject();
                                        for (int i = 0; i<saveData.length();i++){
                                            JSONObject jsonObject1 = saveData.getJSONObject(i);
                                            if (data.get("MATNR").equals(jsonObject1.getString("MATERIAL"))){

                                                if (unScan.isChecked()) {
                                                    if (Integer.valueOf(jsonObject1.getString("SCAN_QTY")) - Integer.valueOf(data.getString("UMREZ")) < 0) {
                                                        AlertBox box = new AlertBox(getContext());
                                                        box.getBox("Alert", "Removed Quantity is less then already scanned quantity.");
                                                        article.setText("");
                                                        article.requestFocus();
                                                        return;
                                                    }
                                                    String sq = jsonObject1.getString("SCAN_QTY");
                                                    jsonObject1.put("SCAN_QTY",String.valueOf(Integer.valueOf(sq)-Integer.valueOf(data.getString("UMREZ"))));
                                                    sqSum = sqSum - Integer.valueOf(data.getString("UMREZ"));
                                                    unScanFlag = false;

                                                }else{
                                                    String sq = jsonObject1.getString("SCAN_QTY");
                                                    jsonObject1.put("SCAN_QTY",String.valueOf(Integer.valueOf(sq)+Integer.valueOf(data.getString("UMREZ"))));
                                                    sqSum = sqSum + Integer.valueOf(data.getString("UMREZ"));
                                                    flag = false;
                                                }


                                            }

                                        }

                                        if (unScanFlag && unScan.isChecked()){
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Alert", "No Data Found for Removed Quantity - "+ a);
                                            article.setText("");
                                            article.requestFocus();
                                            return;
                                        }

                                        if (flag&& !unScan.isChecked()){

                                            jsonObject.put("WAREHOUSE",WERKS);
                                            jsonObject.put("SITE",s);
                                            jsonObject.put("SLOC",loca);
                                            if (!crateNum.equals("")) {
                                                jsonObject.put("CRATE", crateNum);
                                            }
                                            jsonObject.put("BIN",scanB);
                                            jsonObject.put("MATERIAL",data.get("MATNR"));
                                            jsonObject.put("SCAN_QTY",data.get("UMREZ"));
                                            saveData.put(jsonObject);
                                            sqSum = sqSum + Integer.valueOf(data.getString("UMREZ"));

                                        }

                                        Log.v(TAG,saveData.toString());

                                        stock_qty.setText(String.valueOf(sqSum));

                                        article.setText("");
                                        article.requestFocus();


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
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (saveData != null && saveData.length() < 0){

                    box.getBox("Alert", "Scan  All Details.");
                }

                Log.d(TAG, "payload sent to server ");

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

        String rfc = "ZWM_RFC_STOCK_TAKE_SAVE_V11";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IT_DATA", saveData);


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

                                        return;
                                    } else {

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {


                                                crateNum = "";
                                                sqSum = 0;
                                                scanB = "";
                                                saveData = new JSONArray();

                                                crate.setText("");
                                                scan_bin.setText("");
                                                article.setText("");
                                                stock_qty.setText("");
                                                scan_bin.setEnabled(true);
                                                with_crate.setChecked(true);
                                                with_crate.setEnabled(true);
                                                without_crate.setEnabled(true);
                                                if (crate.isEnabled()){
                                                    crate.requestFocus();
                                                }else {
                                                    scan_bin.requestFocus();
                                                }



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
                saveDataToServer();
                break;

            case R.id.reset:


                with_crate.setEnabled(true);
                with_crate.setChecked(true);
                without_crate.setEnabled(true);
                scan_bin.setText("");
                crate.setText("");
                article.setText("");
                scan_bin.setEnabled(true);
                stock_qty.setText("");
                saveData = new JSONArray();
                crateNum = "";
                sqSum = 0;
                scanB = "";
                if (crate.isEnabled()){
                    crate.requestFocus();
                }else {
                    scan_bin.requestFocus();
                }

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
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("V11 To MSA");
    }


}