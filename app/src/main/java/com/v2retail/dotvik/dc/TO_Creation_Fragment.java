package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtBinModel;
import com.v2retail.dotvik.modal.EtCreateModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TO_Creation_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TO_Creation_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TO_Creation_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "bin_no";
    private static final String ARG_PARAM2 = "po_data";
    private static final String ARG_PARAM3 = "hhu_qty";
    private static final String ARG_PARAM4 = "ean";
    AlertBox box;
    ProgressDialog dialog;
    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();
    Context con;
    String requester = "";
    ArrayList<ArrayList<String>> dtCrate;
    JSONArray jsonArray = new JSONArray();


    // TODO: Rename and change types of parameters
    private ArrayList<String> hhu_qty_param;
    ArrayList<Integer> rows_index = new ArrayList<>();
    private String TAG = TO_Creation_Fragment.class.getName();


    FragmentManager fm;

    Button back;

    TextView mResponseView;
    TextView mDt;

    EditText po_et;
    EditText crate_et;
    EditText dest_bin_et;
    EditText total_Scanned_et;
    EditText crate_qty_et;
    EditText total_crate_qty_et;
    EditText gr_no;
    String Gr = "";
    String scanner;
    int TScanned = 0;
    String CurBin = "";
    String CurCrate = "";
    String Crate = "";
    int scanCount =0;
    private OnFragmentInteractionListener mListener;
    List<EtBinModel> etBinModels;
    List<EtCreateModel> etCreateModels;

    public TO_Creation_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("To Creation");
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
    public static TO_Creation_Fragment newInstance(String param1, String param2) {
        TO_Creation_Fragment fragment = new TO_Creation_Fragment();
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

        }
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.to_creation, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog=new ProgressDialog(con);
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


        po_et = (EditText) view.findViewById(R.id.po); //pono_text
        crate_et = (EditText) view.findViewById(R.id.crate_no);//vendoreId
        dest_bin_et = (EditText) view.findViewById(R.id.dest_bin);//gateEntry
        total_Scanned_et = (EditText) view.findViewById(R.id.ts);//billNo
        crate_qty_et = (EditText) view.findViewById(R.id.cq);//billLandin
        total_crate_qty_et = (EditText) view.findViewById(R.id.tcq);//ven_inv
        gr_no = (EditText) view.findViewById(R.id.gr_no);//ven_inv

        etBinModels = new ArrayList<>();
        etCreateModels = new ArrayList<>();

        addEditorListeners();
        addTextChangeListners();

        mResponseView = (TextView) view.findViewById(R.id.response);

        back.setOnClickListener(this); //btnExit

        dtCrate = tables.getCrateTable();

        gr_no.requestFocus();

        return view;

    }


    void addEditorListeners() {
        crate_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = crate_et.getText().toString();
                    if (crate == null || crate.length() < 0 || crate.equals("")) {
                        UIFuncs.blinkEffectOnError(con,crate_et,true);
                        box.getBox("Alert", "Enter Crate No!");
                        return true;
                    } else {
                        try {
                            loadCrateDate();
                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }

                        dest_bin_et.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });

        dest_bin_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String bin = dest_bin_et.getText().toString();
                    if (bin == null || bin.length() < 0 || bin.equals("")) {
                        UIFuncs.blinkEffectOnError(con,dest_bin_et,true);
                        box.getBox("Alert", "Enter Destination Bin No!");
                        return true;
                    } else {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(dest_bin_et.getWindowToken(), 0);

                        dialog.setMessage("Please wait...");
                        dialog.setCancelable(false);
                        dialog.show();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {

                                    loadDestData();
                                }catch (Exception e)
                                {
                                    box.getErrBox(e);
                                }
                            }
                        }, 1000);

                        return true;
                    }
                }

                return false;
            }
        });

        gr_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String gr = gr_no.getText().toString();
                    if (gr == null || gr.length() < 0 || gr.equals("")) {
                        UIFuncs.blinkEffectOnError(con,gr_no,true);
                        box.getBox("Alert", "Enter Gr No!");
                    } else {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(gr_no.getWindowToken(), 0);

                        try {
                            dialog.setMessage("Please wait...");
                            dialog.setCancelable(false);
                            dialog.show();

                            loadGrData(gr);
                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }
                    }
                    return true;
                }

                return false;
            }
        });

    }


    void addTextChangeListners() {

        crate_et.addTextChangedListener(new TextWatcher() {
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

                String crate = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned crate : " + crate);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadCrateDate();
                        }
                    });
                }
            }
        });


        dest_bin_et.addTextChangedListener(new TextWatcher() {
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

                String destBin = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned BIN : " + destBin);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("Please wait...");
                            dialog.setCancelable(false);
                            dialog.show();

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        loadDestData();
                                    }catch (Exception e)
                                    {
                                        box.getErrBox(e);
                                    }
                                }
                            }, 1000);
                        }
                    });
                }
            }
        });


        gr_no.addTextChangedListener(new TextWatcher() {
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

                String grNumber = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned GR Numer : " + grNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadGrData(grNumber);
                        }
                    });
                }
            }
        });

    }

    void loadGrData(String gr){
        Gr =gr;
        String rfc = "ZWM_GET_GRC_BINS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_MBLNR", gr);

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

                                        gr_no.setText("");
                                        gr_no.requestFocus();
//                                        gr.setText("");
//                                        gr.requestFocus();
                                        return;
                                    } else {

                                        JSONArray etBin = responsebody.getJSONArray("ET_BINS");
                                        JSONArray etCrate = responsebody.getJSONArray("ET_CRATE");

                                        // we need to start ffrom 1 index, and - 0 is meta information
                                        for (int i=1;i<etBin.length();i++){
                                            String LGTYP = etBin.getJSONObject(i).getString("LGTYP");
                                            String MANDT = etBin.getJSONObject(i).getString("MANDT");
                                            String MJAHR = etBin.getJSONObject(i).getString("MJAHR");
                                            String LOCKED = etBin.getJSONObject(i).getString("LOCKED");
                                            String MBLNR = etBin.getJSONObject(i).getString("MBLNR");
                                            String LGPLA = etBin.getJSONObject(i).getString("LGPLA");
                                            String LGNUM = etBin.getJSONObject(i).getString("LGNUM");

                                            etBinModels.add(new EtBinModel(LGTYP,MANDT,MJAHR,LOCKED,MBLNR,LGPLA,LGNUM));

                                        }

                                        // we need to start ffrom 1 index, and - 0 is meta information
                                        for (int i=1;i<etCrate.length();i++){

                                            String MANDT = etCrate.getJSONObject(i).getString("MANDT");
                                            String CRATE = etCrate.getJSONObject(i).getString("CRATE");

                                            etCreateModels.add(new EtCreateModel(CRATE,MANDT));
                                        }

                                        gr_no.setEnabled(false);

                                        crate_et.requestFocus();
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


    private void loadDestData() {
        String bin = dest_bin_et.getText().toString();
        int flag =0;
        String Str55 = "";

        if (bin == null || bin.length() < 0 || bin.equals("")) {
            UIFuncs.blinkEffectOnError(con,dest_bin_et,true);
            box.getBox("Alert", "Enter Destination Bin First!");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        JSONArray IT_DATA = new JSONArray();
        Log.v(TAG,jsonArray.toString()+"------"+ jsonArray.length());

        for (int i=0;i<etBinModels.size();i++){
            if (bin.equals(etBinModels.get(i).getLGPLA())){
                flag = 1;
                scanCount = scanCount+1;
                total_Scanned_et.setText(String.valueOf(scanCount));
                try {
                    jsonObject.put("LGPLA", bin);
                    jsonObject.put("CRATE",Crate);
                }catch (Exception e){
                    box.getErrBox(e);
                }
                jsonArray = IT_DATA.put(jsonObject);
                Log.v(TAG,jsonArray.toString());
                break;
            }
        }
        if (flag==0){
            UIFuncs.errorSound(con);
            box.getBox("Alert", "Invalid Destination");
            dest_bin_et.setText("");
            dest_bin_et.requestFocus();
        }
        dialog.dismiss();

        sendDataToServer();

    }

    private void loadCrateDate() {

        final String crate = crate_et.getText().toString().trim();

        if ( TextUtils.isEmpty(crate_et.getText().toString().trim())|| crate.equals("") ) {
            UIFuncs.blinkEffectOnError(con,crate_et,true);
            box.getBox("Alert", "Enter Crate First!");
            return;
        }

        if (dtCrate != null &&dtCrate.size()>0 && dtCrate.get(0).size()>0) {

            if (dtCrate.get(0).contains(crate.toUpperCase())) {
                UIFuncs.errorSound(con);
                box.getBox("Alert", "Already Scanned");
                crate_et.setText("");
                dest_bin_et.setText("");
                crate_et.requestFocus();
                return;
            }
        } else
            {
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.show();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        String addrec = crate;
                        String valueRequestPayload = "validatecrateto#" + addrec + "#<eol>";
                        Log.d(TAG, "payload->:" + valueRequestPayload);
                        requester = "crate";
                       try {
//                           sendAndRequestResponse(valueRequestPayload);
                           loadCrate(crate);
                       }catch (Exception e)
                       {
                           box.getErrBox(e);
                       }
                    }
                }, 2000);

        }
    }

    void loadCrate(String crate){
        int flag =0;
        for (int i = 0;i<etCreateModels.size();i++){

            if (crate.equals(etCreateModels.get(i).getCRATE())){
                dest_bin_et.requestFocus();
                Crate = crate;
                flag=1;
                dialog.dismiss();
                break;
            }
        }

        if (flag==0){
            dialog.dismiss();
            Crate = "";
            UIFuncs.errorSound(con);
            box.getBox("Alert","Invalid Crate against the GR " + Gr);
            crate_et.setText("");
            dest_bin_et.setText("");
            crate_et.requestFocus();
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId())

        {
            case R.id.crate_to:

                try {
                    sendDataToServer();
                }catch (Exception e)
                {
                    box.getErrBox(e);
                }
                break;

            case R.id.back:

                fm.popBackStack();
                break;
        }
    }

    private void sendDataToServer() {
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String msg = null;
                StringBuilder Stuz = new StringBuilder();
                String addrec = null;
                int i = 0, j = 0;
                int len=0;
                int lenItem=0;

                try {
//                    sendAndRequestResponse(valueRequestPayload);
                    if (!Gr.equals("")&& jsonArray.length()>0){
                        saveData();
                    }else {
                        dialog.dismiss();
                        UIFuncs.errorSound(con);
                        box.getBox("Alert","No crate data Found. Please Scan Data first");
                    }

                }catch (Exception e)
                {
                    box.getErrBox(e); }
            }
        }, 2000);

    }

    void saveData(){

        String rfc = "ZWM_TO_CREATE_FROM_SCAN_DATA";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_MBLNR",Gr);
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
                                        dest_bin_et.setText("");
                                        dest_bin_et.requestFocus();
                                        return;
                                    } else {
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                                        builder1.setMessage(returnobj.getString("MESSAGE"));
                                        builder1.setCancelable(false);
                                        builder1.setPositiveButton(
                                                "OK",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        // fm.popBackStack();

                                                        dest_bin_et.setText("");
                                                        crate_et.setText("");
                                                        crate_et.requestFocus();
                                                    }
                                                });
                                        builder1.show();

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {
            Log.d(TAG, TAG + " scanned result...");
            box.getBox("Scanner Err","Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null) {
                switch (scanner)

                {

                    case "crate":
                        crate_et.setText(scanContent);
                        try {
                            loadCrateDate();
                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }

                        Log.v(TAG, "crate code = " + scanContent);

                        break;
                    case "dest":
                        dest_bin_et.setText(scanContent);

                        try {
                            loadDestData();
                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }
                        Log.v(TAG, "dest code = " + scanContent);

                        break;
                }

            } else {
                UIFuncs.errorSound(con);
               box.getBox("Scanner Err","No Content Received. Please Scan Again");

            }
        }
    }

    private void sendAndRequestResponse(final String requestBody) {
        final RequestQueue mRequestQueue;
        StringRequest mStringRequest;

        //RequestQueue initialized
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        mStringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {


            @Override
            public void onResponse(String response) {

                response = response.substring(9, response.length());

                Log.d(TAG, "response ->" + response);

                if (response == null)
                {
                    dialog.dismiss();
                    box.getBox("Err","No response from Server");

                }
                if (response.equals(""))
                {
                    dialog.dismiss();
                    box.getBox("Err","Empty response from Server/ Unable to connect");

                }
                else if (response.charAt(0) == ('S'))
                {
                    dialog.dismiss();
                    response = response.substring(2, response.length());

                    Log.d("tag", "  response ->" + response);
                    if (requester.equals("crate"))
                        setCrateData(response);
                    if (requester.equals("save"))
                    {

                         box.getBox("Response", response);
                        crate_et.setText("");
                        crate_qty_et.setText("");
                        dest_bin_et.setText("");
                        po_et.setText("");
                        total_crate_qty_et.setText("");
                        total_Scanned_et.setText("");

                        if (dtCrate.get(0).size() > 0)
                        {
                            dtCrate.get(0).clear();
                            dtCrate.get(1).clear();
                        }

                        crate_et.requestFocus();
                    }
                }
                else if (response.charAt(0) == ('E'))
                {
                    Log.d(TAG, " Response is Failure ->   :" + response);
                    dialog.dismiss();
                    response = response.substring(2, response.length());

                    if (requester.equals("crate")) {

                        box.getBox("Response", response);
                        crate_et.setText("");
                        dest_bin_et.setText("");
                        crate_et.requestFocus();
                    }
                    if (requester.equals("save"))
                    {
                        box.getBox("Response", response);
                        crate_et.setText("");
                        po_et.setText("");
                        dest_bin_et.setText("");
                        total_crate_qty_et.setText("");
                        crate_qty_et.setText("");
                        crate_et.requestFocus();
                    }

                }
                else
                {
                    dialog.dismiss();
                    Log.d(TAG, " Response is unknown :" + response);
                    box.getBox("Err",response);

                }
            }
        }, new Response.ErrorListener() {
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

                dialog.dismiss();
                box.getBox("Err",err);

            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {

                Response<String> res = super.parseNetworkResponse(response);
                String data = res.result;
                Log.i(TAG, data);

                return res;
            }


        };
        mStringRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(mStringRequest);

    }

    private void setCrateData(String response) {
        dialog.dismiss();
        String[] myArr = response.split("#");


        CurBin = myArr[0];
        CurCrate = crate_et.getText().toString();
        po_et.setText(myArr[1]);
        crate_qty_et.setText(myArr[2]);
        total_crate_qty_et.setText(myArr[3]);
        dest_bin_et.requestFocus();
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
