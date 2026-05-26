package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.v2retail.dotvik.modal.EtBinModel;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtPoDataModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.EditTextDate;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FullHuPutwayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FullHuPutwayFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Button back,next,reset;
    private EditText storeEt,dateEt,huNoEt,binEt;
    private String URL,WERKS,USER;
    private String TAG = FullHuPutwayFragment.class.getName();
    private AlertBox box;
    private ProgressDialog dialog;
    private List<EtBinModel> etBinModels;
    private FragmentManager fm;
    public FullHuPutwayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FullHuPutwayFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FullHuPutwayFragment newInstance(String param1, String param2) {
        FullHuPutwayFragment fragment = new FullHuPutwayFragment();
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
        return inflater.inflate(R.layout.fragment_full_hu_putway, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init(view);

    }

    private void init(View view){
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
        next = view.findViewById(R.id.next);
        reset = view.findViewById(R.id.reset);
        back = view.findViewById(R.id.back);
        storeEt = view.findViewById(R.id.store_name);
        dateEt = view.findViewById(R.id.date);
        huNoEt = view.findViewById(R.id.hu_no);
        binEt = view.findViewById(R.id.bin);

        EditTextDate editTextDate = new EditTextDate(getContext());
        editTextDate.setDateOnView(dateEt);

        etBinModels = new ArrayList<>();
        fm = getFragmentManager();

        storeEt.setText(WERKS);

        back.setOnClickListener(this);
        next.setOnClickListener(this);
        reset.setOnClickListener(this);

        huNoEt.requestFocus();

        onEditListener();
        addTextChangeListeners();


    }
    private void onEditListener() {

        huNoEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = huNoEt.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan HU Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(huNoEt.getWindowToken(), 0);
                            loadHuData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });
        binEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = binEt.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan HU Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(binEt.getWindowToken(), 0);
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

    }
    private void addTextChangeListeners() {

        huNoEt.addTextChangedListener(new TextWatcher() {
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

                String crateNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Hu Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadHuData();
                        }
                    });
                }
            }
        });

        binEt.addTextChangedListener(new TextWatcher() {
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

                String crateNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Hu Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBinData();
                        }
                    });
                }
            }
        });

    }

    private void loadHuData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String huNum = huNoEt.getText().toString();
        if (huNum==null|| huNum.equals("")||huNum.isEmpty()){
            box.getBox("Err","Scan Hu Number!");
            dialog.dismiss();
            return;
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    huScan(huNum);

                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }

    private void loadBinData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String huNum = huNoEt.getText().toString();
        String binNum = binEt.getText().toString();
        if (huNum.equals("")||huNum.isEmpty()){
            box.getBox("Alert","First Scan Hu Number!");
            dialog.dismiss();
            return;
        }
        if (binNum.equals("")||binNum.isEmpty()){
            box.getBox("Err","Scan Bin Number!");
            dialog.dismiss();
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    scanBin(binNum);
                } catch (Exception e) {
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

        String huNum = huNoEt.getText().toString();
        String binNum = binEt.getText().toString();
        if (huNum.equals("")||huNum.isEmpty()){
            box.getBox("Err","First Scan Hu Number!");
            dialog.dismiss();
            return;
        }
        if (binNum.equals("")||binNum.isEmpty()){
            box.getBox("Err","First Bin Number!");
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
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }
    private void huScan(String huNu) {


        String rfc = "ZWM_STORE_HU_GET_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EXIDV",huNu);
            params.put("IM_WERKS", WERKS);


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
                                        huNoEt.setText("");
                                        huNoEt.requestFocus();
                                        return;
                                    } else {

                                        JSONArray ET_LAGP = responsebody.getJSONArray("ET_LAGP");
                                        for (int i =1;i<ET_LAGP.length();i++){
                                            JSONObject jsonObject = ET_LAGP.getJSONObject(i);
                                            String LGPLA = jsonObject.getString("LGPLA");
                                            etBinModels.add(new EtBinModel(LGPLA));
                                        }

                                        binEt.requestFocus();
                                        huNoEt.setEnabled(false);
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

    private void scanBin(String bin){
        boolean flag = true;
        for (int i=0;i<etBinModels.size();i++){
            if (etBinModels.get(i).getLGPLA().equals(bin)){
                flag = false;
                break;
            }
        }

        if (flag){
            box.getBox("Alert","Incorrect Bin!");
            binEt.setText("");
            binEt.requestFocus();
            dialog.dismiss();

        }else {
            dialog.dismiss();
            loadSaveData();

        }


    }
    private void saveData() {


        String rfc = "ZWM_STORE_FLOOR_PUTWAY_HU";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_HU",huNoEt.getText().toString());
            params.put("IM_LGPLA",binEt.getText().toString());



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
                                        binEt.setText("");
                                        binEt.requestFocus();
                                        return;
                                    } else {

                                        binEt.requestFocus();
                                        binEt.setText("");
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

    private void reset(){

        huNoEt.setEnabled(true);
        huNoEt.setText("");
        huNoEt.requestFocus();
        binEt.setText("");
        etBinModels.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Full HU Putway");
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.next:
                loadSaveData();
                break;

            case R.id.reset:
                reset();
                break;

            case R.id.back:
                fm.popBackStack();
                break;
        }
    }
}