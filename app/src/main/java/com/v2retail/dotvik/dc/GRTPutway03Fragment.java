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
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GRTPutway03Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRTPutway03Fragment extends Fragment implements View.OnClickListener {

    private EditText dc_site,crate,destination_bin;
    private Activity activity;
    private String URL ="";
    private String TAG ="GRTPutWay03Fragment";
    private View view;
    private ProgressDialog dialog = null;
    private Button back,reset,submit;
    private AlertBox box;
    private String WERKS ="";
    private String USER ="";
    private String dcS ="";
    private String crateNum ="";
    private String destBin = "";
    private FragmentManager fm;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GRTPutway03Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GRTPutway03Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GRTPutway03Fragment newInstance(String param1, String param2) {
        GRTPutway03Fragment fragment = new GRTPutway03Fragment();
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
        view = inflater.inflate(R.layout.fragment_g_r_t_putway03, container, false);

        dc_site = (EditText) view.findViewById(R.id.dc_site);
        crate = (EditText) view.findViewById(R.id.crate);
        destination_bin = (EditText) view.findViewById(R.id.destination_bin);
        back =  view.findViewById(R.id.back);
        reset =  view.findViewById(R.id.reset);
        submit =  view.findViewById(R.id.submit);

        fm = getFragmentManager();

        init();
        onEditListener();
        addTextChangeListeners();
        return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData data = new SharedPreferencesData(activity);

        submit.setOnClickListener(this);
        back.setOnClickListener(this);
        reset.setOnClickListener(this);
        URL = data.read("URL");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);

        WERKS=data.read("WERKS");
        USER=data.read("USER");
        URL = data.read("URL");

        if(!WERKS.isEmpty()) {
            Log.d(TAG, "WERKS->" + WERKS);
            dc_site.setText(WERKS);
        }
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);

        dc_site.requestFocus();

    }

    private void onEditListener() {

        dc_site.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String dcSite = dc_site.getText().toString();
                    if (dcSite == null || dcSite.length() < 0 || dcSite.equals("")) {
                        box.getBox("Alert", "Scan DC Site !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(dc_site.getWindowToken(), 0);
                            loadDcSiteData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


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


        destination_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String destinationBin = destination_bin.getText().toString();
                    if (destinationBin == null || destinationBin.length() < 0 || destinationBin.equals("") ) {
                        box.getBox("Alert", "Scan Destination Bin  !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(destination_bin.getWindowToken(), 0);
                        loadDestinationBinData();
                        return true;
                    }
                }

                return false;
            }
        });

    }
    private void addTextChangeListeners() {

        dc_site.addTextChangedListener(new TextWatcher() {
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

                String dc = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Dc Site : " +  dc);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadDestinationBinData();
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

        destination_bin.addTextChangedListener(new TextWatcher() {
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

                String destinationBin = s.toString();
                if(scannerReading) {
                    Log.d(TAG, " Destination Bin : " +  destinationBin);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadDestinationBinData();
                        }
                    });
                }
            }
        });

    }

    private void loadDcSiteData(){

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    crate.requestFocus();
                    dialog.dismiss();
                }catch (Exception e)
                {    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    private void loadCrateData(){

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {

                    destination_bin.requestFocus();
                    dialog.dismiss();
                }catch (Exception e)
                {    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    private void loadDestinationBinData(){

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String crateNumber = crate.getText().toString();
        String destinationBin = destination_bin.getText().toString();
        String werks = dc_site.getText().toString();
        if (werks == null || werks.length() < 0 || werks.equals("")) {
            box.getBox("Alert", "Scan DC Site No!");
            dialog.dismiss();
            return;
        }

        if (crateNumber == null || crateNumber.length() < 0 || crateNumber.equals("")) {
            box.getBox("Alert", "Scan Crate !");
            dialog.dismiss();
            return;
        }
        if (destinationBin == null || destinationBin.length() < 0 || destinationBin.equals("")) {
            box.getBox("Alert", "Scan Destination Bin !");
            dialog.dismiss();
            return;
        }




        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    binGetData(werks,crateNumber,destinationBin);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    void binGetData(String werks, String crateNumber, String destinationBin){

        String rfc = "ZWM_RFC_GRT_PUTWAY_CRATE_VAL";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNumber);
            params.put("IM_LGPLA", destinationBin);
            params.put("IM_WERKS", werks);




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
                                        destination_bin.setText("");
                                        crate.requestFocus();
                                        return;
                                    } else {

                                        submit.requestFocus();
                                        destBin = destinationBin;
                                        crateNum = crateNumber;
                                        dcS = werks;

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


                String tag = "";
                if (dcS == null || dcS.length() < 0 || dcS.equals(""))
                {   tag = "Dc Site.";
                }

                if (crateNum == null || crateNum.length() < 0 || crateNum.equals(""))
                {   tag = "Create";
                }

                if (destBin == null || destBin.length() < 0 || destBin.equals(""))
                {
                    tag = "Destination Bin";
                }



                if (!tag.equals(""))
                {
                    dialog.dismiss();
                    box.getBox("Alert", "Scan " + tag + " First");
                    return;
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

        String rfc = "ZWM_RFC_GRT_PUTWAY_POST";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNum);
            params.put("IM_LGPLA", destBin);
            params.put("IM_USER", USER);
            params.put("IM_WERKS", dcS);

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
                                                destination_bin.setText("");
                                                dc_site.requestFocus();
                                                crateNum = "";
                                                destBin = "";
                                                dcS = "";
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

            case R.id.submit:
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

            case R.id.reset:
                crate.setText("");
                destination_bin.setText("");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRT Putway (03)");
    }
}