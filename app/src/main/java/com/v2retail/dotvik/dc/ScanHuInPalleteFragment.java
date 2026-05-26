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
import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScanHuInPalleteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScanHuInPalleteFragment extends Fragment implements View.OnClickListener {

    private EditText pallete,scan_hu,scan_hu_to,destination_stored,scanned_hu_count, batch_no;
    private Activity activity;
    private String URL ="";
    private String TAG ="ScanHuInPalleteFragment";
    private View view;
    private ProgressDialog dialog = null;
    private AlertBox box ;
    private Button submit,back,reset;
    private int huCount = 0;
    private String destinationStore = "";
    private JSONArray jsonArray;
    private String pNo="";
    private FragmentManager fm;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ScanHuInPalleteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ScanHuInPalleteFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ScanHuInPalleteFragment newInstance(String param1, String param2) {
        ScanHuInPalleteFragment fragment = new ScanHuInPalleteFragment();
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
        view = inflater.inflate(R.layout.fragment_scan_hu_in_pallete, container, false);
        pallete = view.findViewById(R.id.pallete);
        scan_hu = view.findViewById(R.id.scan_hu);
        scan_hu_to = view.findViewById(R.id.scan_hu_to);
        batch_no = view.findViewById(R.id.batch_no);
        destination_stored = view.findViewById(R.id.destination_stored);
        scanned_hu_count = view.findViewById(R.id.scanned_hu_count);
        back = view.findViewById(R.id.back);
        submit = view.findViewById(R.id.submit);
        reset = view.findViewById(R.id.reset);

        back.setOnClickListener(this);
        submit.setOnClickListener(this);
        reset.setOnClickListener(this);
        fm = getFragmentManager();
        init();
        onEditListener();
        addTextChangeListeners();
        return view;
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        scan_hu_to.setEnabled(false);
        destination_stored.setEnabled(false);
        scanned_hu_count.setEnabled(false);
        scan_hu.setEnabled(false);
        pallete.requestFocus();
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

        scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String scanHu = scan_hu.getText().toString();
                    if (scanHu == null || scanHu.length() < 0 || scanHu.equals("") ) {
                        box.getBox("Alert", "Enter Bin Number !");
                    } else {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(scan_hu.getWindowToken(), 0);
                        loadHuData();
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


        scan_hu.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scan HU : " +  poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadHuData();
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
                jsonArray = new JSONArray();
                huCount = 0;
                destinationStore = "";
                palleteGetData(palleteNumber);
            }
        }, 1000);
    }

    void loadHuData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String huNumber = scan_hu.getText().toString();
                if (huNumber == null || huNumber.length() < 0 || huNumber.equals("")) {
                    box.getBox("Alert", "Scan HU No!");
                    dialog.dismiss();
                    return;
                }
                scanHuGetData(huNumber);
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
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("PALETTE",palleteNumber);
            jsonObject.put("INDICATOR",1);
            Log.v(TAG,jsonObject.toString());
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
                                        pNo = palleteNumber;
                                        scan_hu.setEnabled(true);
                                        scan_hu.requestFocus();
                                        JSONObject EX_DATA = responsebody.getJSONObject("EX_DATA");
                                        scanned_hu_count.setText(EX_DATA.getString("HUCOUNT"));
                                        destination_stored.setText(EX_DATA.getString("WERKS"));
                                        destinationStore =  EX_DATA.getString("WERKS");
                                        huCount = Integer.valueOf(EX_DATA.getString("HUCOUNT"));
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

    void scanHuGetData(String huNumber) {

        String rfc = "ZWM_CLA_HU_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("EXIDV",huNumber);
            jsonObject.put("INDICATOR",1);
            params.put("bapiname", rfc);
            params.put("IM_PARMS", jsonObject);
            params.put("IM_ABATCH", UIFuncs.toUpperTrim(batch_no));

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
                                        scan_hu.setText("");
                                        scan_hu.requestFocus();
                                        return;
                                    } else {
                                        scan_hu.setText("");
                                        scan_hu.requestFocus();
                                        JSONObject EX_DATA = responsebody.getJSONObject("EX_DATA");
                                        if (destinationStore.equals("")) {
                                            destinationStore = EX_DATA.getString("WERKS");
                                        }
                                        if (destinationStore.equals(EX_DATA.getString("WERKS"))) {
                                            for (int i=0;i<jsonArray.length();i++){
                                                if (jsonArray.getJSONObject(i).getString("EXIDV").equals(huNumber)){
                                                    AlertBox box = new AlertBox(getContext());
                                                    box.getBox("Err","HU already scanned.");
                                                    return;
                                                }
                                            }
                                            JSONObject data = new JSONObject();
                                            data.put("EXIDV",huNumber);
                                            data.put("WERKS",EX_DATA.getString("WERKS"));
                                            data.put("PALETTE",pNo);

                                            jsonArray.put(data);
                                            huCount = huCount + 1;
                                            destination_stored.setText(EX_DATA.getString("WERKS"));
                                            scanned_hu_count.setText(String.valueOf(huCount));
                                            scan_hu_to.setText(huNumber);
                                            batch_no.setText(EX_DATA.getString("ABATCH"));

                                        }else {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Err", "Destination store is different then previous HU.");
                                        }
                                    }
                                Log.v(TAG,jsonArray.toString());
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

                if (jsonArray.length()<1) {
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

        String rfc = "ZWM_CLA_HU_PALETTE_SAVE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_DATA", jsonArray);

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

                                        scan_hu_to.setText("");
                                        destination_stored.setText("");
                                        scanned_hu_count.setText("");
                                        pallete.setText("");
                                        scan_hu.setText("");
                                        pallete.requestFocus();
                                        jsonArray = new JSONArray();
                                        destinationStore = "";
                                        huCount = 0;
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






    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Hu Scan In Pallete");
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
                pallete.setText("");
                scan_hu.setText("");
                scan_hu_to.setText("");
                destination_stored.setText("");
                scanned_hu_count.setText("");
                pallete.requestFocus();
                scan_hu.setEnabled(true);
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

}