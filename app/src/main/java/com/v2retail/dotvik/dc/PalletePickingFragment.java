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
 * Use the {@link PalletePickingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PalletePickingFragment extends Fragment implements View.OnClickListener {


    private EditText pallete,cla_bin;
    private Activity activity;
    private String URL ="";
    private String TAG ="PalletePickingFragment";
    private View view;
    private ProgressDialog dialog = null;
    AlertBox box ;
    private Button submit,back;
    private String pallateNu ="";
    private String claNum = "";
    FragmentManager fm;



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PalletePickingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PalletePickingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PalletePickingFragment newInstance(String param1, String param2) {
        PalletePickingFragment fragment = new PalletePickingFragment();
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

        view = inflater.inflate(R.layout.fragment_pallete_picking, container, false);

        pallete = view.findViewById(R.id.pallete);
        cla_bin = view.findViewById(R.id.cla_bin);
        submit = view.findViewById(R.id.submit);
        back = view.findViewById(R.id.back);

        back.setOnClickListener(this);
        submit.setOnClickListener(this);
        init();
        onEditListener();
        addTextChangeListeners();

        return view;
    }
    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        pallete.requestFocus();
        fm = getFragmentManager();
        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);

    }
    private void onEditListener() {

        pallete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String palleteNumber = pallete.getText().toString();
                    if (palleteNumber == null || palleteNumber.length() < 0 || palleteNumber.equals("")) {
                        box.getBox("Alert", "Scan Pallete No!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(pallete.getWindowToken(), 0);
                            loadPalleteData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });

        cla_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String claBin = cla_bin.getText().toString();
                    if (claBin == null || claBin.length() < 0 || claBin.equals("") ) {
                        box.getBox("Alert", "Enter CLA Bin Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(cla_bin.getWindowToken(), 0);
                        loadClaBinData();
                        return true;
                    }
                }

                return false;
            }
        });





    }
    void addTextChangeListeners() {

        pallete.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned Pallete Number : " +  grNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadPalleteData();
                        }
                    });
                }
            }
        });


        cla_bin.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scan CLA Bin : " +  poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadClaBinData();
                        }
                    });
                }
            }
        });






    }


    void loadPalleteData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String palleteNumber = pallete.getText().toString();
                if (palleteNumber == null || palleteNumber.length() < 0 || palleteNumber.equals("")) {
                    box.getBox("Alert", "Scan Pallete No!");
                    dialog.dismiss();
                    return;
                }
                palleteGetData(palleteNumber);
            }
        }, 1000);
    }
    void loadClaBinData(){

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String claBinNumber = cla_bin.getText().toString();
                if (claBinNumber == null || claBinNumber.length() < 0 || claBinNumber.equals("")) {
                    box.getBox("Alert", "Scan Cla Bin No!");
                    dialog.dismiss();
                    return;
                }
                claBinGetData(claBinNumber);
            }
        }, 1000);
    }

    void palleteGetData(String palleteNumber) {

        String rfc = "ZWM_CLA_PALETTE_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("PALETTE",palleteNumber);
            jsonObject.put("INDICATOR","3");

            params.put("bapiname", rfc);
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
                                        pallete.setText("");
                                        pallete.requestFocus();

                                        return;
                                    } else {
                                        pallateNu = palleteNumber;
                                        cla_bin.requestFocus();
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


    void claBinGetData(String claBinNumber) {

        String rfc = "ZWM_CLA_BIN_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("PALETTE",pallateNu);
            jsonObject.put("CLABIN",claBinNumber);
            jsonObject.put("INDICATOR","2");

            params.put("bapiname", rfc);
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
                                        cla_bin.setText("");
                                        cla_bin.requestFocus();
                                        claNum = "";

                                        return;
                                    } else {

                                        claNum = claBinNumber;
                                        submit.requestFocus();

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
                if (pallateNu == null || pallateNu.length() < 0 || pallateNu.equals(""))
                {   tag = "Pallete.";
                }

                if ( claNum == null || claNum.length() < 0 || claNum.equals(""))
                {   tag = "CLA Bin";
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

        String rfc = "ZWM_CLA_PALETTE_BIN_TAG_SAVE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("PALETTE",pallateNu);
            jsonObject.put("CLABIN",claNum);
            jsonObject.put("INDICATOR","2");

            params.put("bapiname", rfc);
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

                                                pallete.setText("");
                                                cla_bin.setText("");
                                                pallateNu = "";
                                                claNum = "";
                                                pallete.requestFocus();

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

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Pallete Picking");
    }

}