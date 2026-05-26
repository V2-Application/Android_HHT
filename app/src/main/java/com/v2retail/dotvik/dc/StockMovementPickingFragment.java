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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StockMovementPickingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StockMovementPickingFragment extends Fragment implements View.OnClickListener {

    private EditText w_house,site,storage_loc,scan_bin,crate,stock_qty;
    private Button back,save;
    private Activity activity;
    private String URL ="";
    private String TAG ="StockMovementPickingFragment";
    private View view;
    private ProgressDialog dialog = null;
    private AlertBox box ;
    private RadioButton with_crate,without_crate;
    private LinearLayout crateLayout;
    private String crateOrWithoutCrate = "";
    private String USER = "";
    private String WERKS = "";
    private String process;
    private LinearLayout destBinLayout;
    private EditText destBin;
    private FragmentManager fm;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public StockMovementPickingFragment() {
        // Required empty public constructor
    }

    public StockMovementPickingFragment(String process){
        this.process = process;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OutwardPickingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StockMovementPickingFragment newInstance(String param1, String param2) {
        StockMovementPickingFragment fragment = new StockMovementPickingFragment();
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

        view = inflater.inflate(R.layout.fragment_outward_picking, container, false);

        w_house = view.findViewById(R.id.w_house);
        site = view.findViewById(R.id.site);
        storage_loc = view.findViewById(R.id.storage_loc);
        with_crate = view.findViewById(R.id.with_crate);
        without_crate = view.findViewById(R.id.without_crate);
        scan_bin = view.findViewById(R.id.scan_bin);
        crate = view.findViewById(R.id.crate);
        stock_qty = view.findViewById(R.id.stock_qty);
        save = view.findViewById(R.id.save);
        back = view.findViewById(R.id.back);
        crateLayout = view.findViewById(R.id.crateLayout);
        destBinLayout = view.findViewById(R.id.destBinLayout);
        destBin = view.findViewById(R.id.destBin);

        fm = getFragmentManager();

        save.setOnClickListener(this);
        back.setOnClickListener(this);
        if (process.equals("putway")){
            destBinLayout.setVisibility(View.VISIBLE);
            crateLayout.setVisibility(View.GONE);
        }

        init();

        onEditListener();
        addTextChangeListeners();
        return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        USER=data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);

        if(!WERKS.isEmpty()) {
            Log.d(TAG, "WERKS->" + WERKS);
            site.setText(WERKS);
        }
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);



        scan_bin.requestFocus();
        with_crate.setChecked(true);
        crateOrWithoutCrate = "A";

        with_crate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    crateLayout.setVisibility(View.VISIBLE);
                    without_crate.setChecked(false);
                    scan_bin.requestFocus();
                    crateOrWithoutCrate = "A";
                    if (process.equals("putway")){
                        destBinLayout.setVisibility(View.VISIBLE);
                        crateLayout.setVisibility(View.GONE);
                    }

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
                    crateOrWithoutCrate = "B";
                    if (process.equals("putway")){
                        destBinLayout.setVisibility(View.GONE);
                        crateLayout.setVisibility(View.GONE);
                    }

                }

            }
        });


    }

    private void onEditListener() {


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

        destBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String scanBin = scan_bin.getText().toString();
                    if (scanBin == null || scanBin.length() < 0 || scanBin.equals("") ) {
                        box.getBox("Alert", "Scan dest Bin !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(scan_bin.getWindowToken(), 0);
                        loadScanDestBinData();
                        return true;
                    }
                }

                return false;
            }
        });


    }
    private void addTextChangeListeners() {




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
        destBin.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "destBin : " +  barcodeString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadScanDestBinData();
                        }
                    });
                }
            }
        });

    }

    private void loadScanBinData() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String s = site.getText().toString().trim();
        String scanBin = scan_bin.getText().toString().trim();


        if (scanBin == null || scanBin.length() < 0 || scanBin.equals("")) {
            box.getBox("Alert", "Please fill Scan Bin.");
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (process.equals("putway")){
                        destBin.requestFocus();
                        dialog.dismiss();
                    }else {
                        scanBinValidation(scanBin);
                    }

                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }
    private void loadScanDestBinData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String scanDestBin = destBin.getText().toString().trim();

        if (scanDestBin == null || scanDestBin.length() < 0 || scanDestBin.equals("")) {
            box.getBox("Alert", "Please fill  Dest Bin.");
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    save.requestFocus();
                    dialog.dismiss();
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    void scanBinValidation(String scanBin){

        String rfc = "ZWM_RFC_STOCK_VALIDATE_V21";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("TYPE",crateOrWithoutCrate);
            jsonObject.put("BIN",scanBin);


            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_PARMS", jsonObject);

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

                                        with_crate.setEnabled(false);
                                        without_crate.setEnabled(false);
                                        site.setEnabled(false);
                                        storage_loc.setEnabled(false);

                                        crate.setText(responsebody.getString("EX_CRATE"));
                                        stock_qty.setText(responsebody.getString("EX_STOCKQTY"));
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

    private void saveDataToServer(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                String s = site.getText().toString().trim();
                String w = w_house.getText().toString().trim();
                String sb = scan_bin.getText().toString().trim();
                String sl = storage_loc.getText().toString().trim();
                String c = "";
                String sq = stock_qty.getText().toString().trim();
                String pickingOrPutway = "";


                String tag = "";
                if (TextUtils.isEmpty( site.getText().toString())&& s.equals(""))
                {   tag = "Site.";
                }

                if ( TextUtils.isEmpty( w_house.getText().toString())&&w.equals(""))
                {   tag = "W House";
                }

                if (TextUtils.isEmpty( scan_bin.getText().toString())   &&sb.equals(""))
                {
                    tag = "Scan Bin";
                }
                if (TextUtils.isEmpty( storage_loc.getText().toString())&& sl.equals(""))
                {		tag = "Storage";
                }



                if (process.equals("picking")){

                    pickingOrPutway = "A";
                }else {
                    pickingOrPutway = "B";
                    c = destBin.getText().toString().trim();
                    if (TextUtils.isEmpty( crate.getText().toString()) && c.equals("")){
                        tag = "Dest Bin";
                    }
                }


                if (!tag.equals(""))
                {
                    dialog.dismiss();
                    box.getBox("Alert", "Enter " + tag + " First");
                    return;
                }

                Log.d(TAG, "payload sent to server ");

                try {




                    sendAndRequestResponse(pickingOrPutway,s,sb,c);
                }catch (Exception e)
                {    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    void sendAndRequestResponse(String pickingOrPutway , String site , String scanBin , String crateStr ) {

        String rfc = "ZWM_RFC_STOCK_MOVEMENT_V21";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("PICK_PUTAWAY",pickingOrPutway);
            jsonObject.put("TYPE",crateOrWithoutCrate);
            jsonObject.put("PLANT",site);
            jsonObject.put("WAREHOUSE","V2R");
            jsonObject.put("LOCATION","0001");
            jsonObject.put("STORAGE_TYPE","E01");
            jsonObject.put("BIN",scanBin);
            jsonObject.put("DESTINATION_BIN",crateStr);

            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_PARMS", jsonObject);

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

                                                crate.setText("");
                                                scan_bin.setText("");
                                                with_crate.setChecked(true);
                                                with_crate.setEnabled(true);
                                                without_crate.setEnabled(true);
                                                scan_bin.requestFocus();
                                                if (process.equals("putway")){
                                                    destBin.setText("");
                                                }
                                                scan_bin.setEnabled(true);

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
    public void onResume() {
        super.onResume();
        if (process.equals("picking")) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Picking");
        }else {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Putaway");
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.save:
                saveDataToServer();
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

        }
    }
}