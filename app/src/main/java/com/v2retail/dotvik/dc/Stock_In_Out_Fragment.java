package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Parcelable;
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
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtPoDataModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.EditTextDate;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Stock_In_Out_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Stock_In_Out_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
/**
 * @author Narayanan
 * @version 11.73
 * {@code Author: Narayanan, Revision: 2, Modified: 24th Aug 2024}
 * Changes: Capturing MIX_ALLOWED and storing the value into EtPoDataModel.MXALOW before passing it to Validate Crate Process
 */
public class Stock_In_Out_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final int REQUEST_SCREEN = 1023;
    private static final String ARG_PARAM1 = "bin_no";
    private static final String ARG_PARAM2 = "po_data";
    private static final String ARG_PARAM3 = "hhu_qty";
    private static final String ARG_PARAM4 = "ean";
    AlertBox box ;
    ProgressDialog dialog;
    String URL="";
    String WERKS="";
    String USER="";

    String requester = "";
    ArrayList<ArrayList<String>> dtPO;
    ArrayList<ArrayList<String>> dtEAN;
    ArrayList<String> dtPONO;
    // TODO: Rename and change types of parameters
    private ArrayList<String> hhu_qty_param;
    ArrayList<Integer> rows_index = new ArrayList<>();
    private String TAG = Stock_In_Out_Fragment.class.getName();

    Tables tables = new Tables();
    Context con;
    FragmentManager fm;

    Button back;
    Button reset;
    Button next;

    TextView mResponseView;
    TextView mDt;

    EditText po_et;
    EditText invoice_et;
    EditText ge_et;
    EditText bno_et;
    EditText bol_et;
    EditText ven_et;

    String scanner;
    List<EtEanDataModel> etEanDataModels;
    List<EtPoDataModel> etPoDataModels;
    private OnFragmentInteractionListener mListener;
    private int mode;
    public Stock_In_Out_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Crate Scan");
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
    public static Stock_In_Out_Fragment newInstance(String param1, String param2) {
        Stock_In_Out_Fragment fragment = new Stock_In_Out_Fragment();
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
        View view = inflater.inflate(R.layout.stock_in_out, container, false);
        con = getContext();
        box = new AlertBox(con);
        etEanDataModels = new ArrayList<>();
        etPoDataModels = new ArrayList<>();
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
        reset = (Button) view.findViewById(R.id.reset);
        next = (Button) view.findViewById(R.id.next);

        po_et = (EditText) view.findViewById(R.id.po); //pono_text
        ven_et = (EditText) view.findViewById(R.id.ven);//vendoreId
        ge_et = (EditText) view.findViewById(R.id.ge);//gateEntry
        bno_et = (EditText) view.findViewById(R.id.bno);//billNo
        bol_et = (EditText) view.findViewById(R.id.bol);//billLandin
        invoice_et = (EditText) view.findViewById(R.id.inv);//ven_inv



        mResponseView = (TextView) view.findViewById(R.id.response);
        mDt = (TextView) view.findViewById(R.id.dt);//date
        EditTextDate date = new EditTextDate(con);
        date.setDateOnView(mDt);
        po_et.requestFocus();


        back.setOnClickListener(this);//btnExit
        reset.setOnClickListener(this);//btnReset
        next.setOnClickListener(this);//btn_next

        addEditorListeners();

        addTextChangeListners();

        return view;

    }


    void addEditorListeners() {

        po_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String po = po_et.getText().toString().trim();
                    if (TextUtils.isEmpty(po_et.getText().toString().trim()) && po.equals("")) {
                        UIFuncs.blinkEffectOnError(con,po_et,true);
                        box.getBox("Alert", "Enter PO No!");
                        return true;
                    } else {
                        checkScreen(po);
                        invoice_et.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
        invoice_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;
                    checkData();
                    return true;
                }
                return false;
            }
        });
    }


    void addTextChangeListners() {

        po_et.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned poString : " +  poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkScreen(poString);
                        }
                    });
                }
            }
        });

        invoice_et.addTextChangedListener(new TextWatcher() {
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

                String poString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned poString : " +  poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkData();
                        }
                    });
                }
            }
        });

        ge_et.addTextChangedListener(new TextWatcher() {
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

                String ge = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned poString : " +  ge);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bno_et.requestFocus();
                        }
                    });
                }
            }
        });

        bno_et.addTextChangedListener(new TextWatcher() {
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

                String billnumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned billnumber : " +  billnumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bol_et.requestFocus();
                        }
                    });
                }
            }
        });

        bol_et.addTextChangedListener(new TextWatcher() {
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

                String bolString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned bolString : " +  bolString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            next.requestFocus();
                        }
                    });
                }
            }
        });
    }
    private void checkScreen(String po){
        mode = 0;
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.ZFMS_SCREEN);
            args.put("IM_EBELN", po);
            args.put("IM_USER", USER);

            showProcessingAndSubmit(Vars.ZFMS_SCREEN, REQUEST_SCREEN, args);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

    }
    private boolean checkData() {

        Log.d(TAG, "checkData() Enter");
        String ven = invoice_et.getText().toString().trim();

        if (TextUtils.isEmpty(invoice_et.getText().toString())&& ven.equals("")) {
            UIFuncs.blinkEffectOnError(con,invoice_et, true);
            box.getBox("Alert", "Enter Vendor Invoice No!");
            return false;
        } else {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(invoice_et.getWindowToken(), 0);

            try {
                requestDataFromServer();
            }catch (Exception e)
            {
                box.getErrBox(e);
            }
            return true;
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId())

        {
            case R.id.next:
                try {
                    saveDataToServer();
                }catch (Exception e)
                {
                    box.getErrBox(e);
                }
                break;

            case R.id.back:

                fm.popBackStack();
                break;

            case R.id.po_scan:

                po_et.setText("");
                scanner = "po";
                if(CameraCheck.isCameraAvailable(con))
                    IntentIntegrator.forSupportFragment(Stock_In_Out_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
                break;

            case R.id.reset:

                clear();
                break;

            case R.id.ok:
                if (checkData())
                    try {
                        requestDataFromServer();
                    }catch (Exception e)
                    {
                        box.getErrBox(e);
                    }

                break;
        }
    }

    private void requestDataFromServer() {

        if(dialog==null) {
            dialog = new ProgressDialog(getContext());
        }

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String addrec = null;
                String po = po_et.getText().toString().trim();
                String inv = invoice_et.getText().toString().trim();

                if (TextUtils.isEmpty(po_et.getText().toString())&& po.equals("")) {
                    UIFuncs.blinkEffectOnError(con,po_et,true);
                    box.getBox("Alert", "Please fill PO No.");
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    return;
                }

                if (TextUtils.isEmpty(invoice_et.getText().toString())&& inv.equals(""))
                {
                    UIFuncs.blinkEffectOnError(con,invoice_et,true);
                    box.getBox("Alert", "Please fill Invoice No.");
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    return;
                }

                addrec = po + "#" + inv;
                requester = "nitupd";

                String valueRequestPayload = "nitupd#" + addrec + "#<eol>";
                Log.d(TAG, "payload : " + valueRequestPayload);
                Log.d(TAG, "payload sent to server ");

                try {
//                   sendAndRequestResponse(valueRequestPayload);
                    getData(po,inv);
                }catch (Exception e)
                {
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    private void saveDataToServer(){
        if(dialog==null) {
            dialog = new ProgressDialog(getContext());
        }

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String addrec = null;

                String po = po_et.getText().toString().trim();
                String inv = invoice_et.getText().toString().trim();
                String ge = ge_et.getText().toString().trim();
                String bno = bno_et.getText().toString().trim();
                String bol = bol_et.getText().toString().trim();
                String tag = "";
                if (TextUtils.isEmpty( po_et.getText().toString())&& po.equals(""))
                {   tag = "PO No.";
                }

                if ( TextUtils.isEmpty( invoice_et.getText().toString())&&inv.equals(""))
                {   tag = "Invoice No";
                }

                if (TextUtils.isEmpty( ge_et.getText().toString())   &&ge.equals(""))
                {
                    tag = "Gate Entry";
                }
                if (TextUtils.isEmpty( bno_et.getText().toString())   &&bno.equals(""))
                {		tag = "Bill No";
                }

                if (TextUtils.isEmpty( bol_et.getText().toString()) &&bol.equals(""))
                {
                    tag = "Bill Landing";
                }

                if (!tag.equals(""))
                {
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }

                    UIFuncs.errorSound(con);
                    box.getBox("Alert", "Enter " + tag + " First");
                    return;
                }

                //nitrec#5900105838#2012#181200025784#222#1#222#<eol>
                addrec = po + "#" + inv + "#" + ge + "#" + bno + "#" + USER + "#" + bol;
                requester = "nitrec";
                String valueRequestPayload = "nitrec#" + addrec + "#<eol>";
                Log.d(TAG, "payload : " + valueRequestPayload);
                Log.d(TAG, "payload sent to server ");

                try {
//                   sendAndRequestResponse(valueRequestPayload);
                    saveData(po,inv,ge,bno,bol);
                }catch (Exception e)
                {
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    void saveData(String po,String inv,String ge, String bno,String bol){

        String rfc = "ZWM_PO_GET_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EBELN", po);
            params.put("IM_XBLNR", inv);
            params.put("IM_USER", USER);
            params.put("IM_GATE_ENTRY", ge);
            params.put("IM_FRBNR", bol);
            params.put("IM_BILL", bno);

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

//                                        gr.setText("");
//                                        gr.requestFocus();
                                        return;
                                    } else {
                                        JSONArray jsonArray = responsebody.getJSONArray("ET_PO_DATA");
                                        Log.v(TAG,"ET_PO_DATA--->"+jsonArray.toString());
                                        if (jsonArray.length()>1) {
                                            for (int i = 1; i < jsonArray.length(); i++) {
                                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                String MAT_DESC = jsonObject.getString("MAT_DESC");
                                                String SCAN_QTY = jsonObject.getString("SCAN_QTY");
                                                String UNIT = jsonObject.getString("UNIT");
                                                String OPEN_QTY = jsonObject.getString("OPEN_QTY");
                                                String GR_QTY = jsonObject.getString("GR_QTY");
                                                String MATERIAL = jsonObject.getString("MATERIAL");
                                                String PO_QTY = jsonObject.getString("PO_QTY");
                                                String CRATE = jsonObject.getString("CRATE");
                                                String MIX_ALLOW = jsonObject.getString("MIX_ALLOWED");
                                                etPoDataModels.add(new EtPoDataModel(MAT_DESC, SCAN_QTY, UNIT, OPEN_QTY, GR_QTY, MATERIAL, PO_QTY, CRATE, MIX_ALLOW));

                                            }
                                        }

                                        JSONArray etEanData = responsebody.getJSONArray("ET_EAN_DATA");
                                        Log.v(TAG,"ET_EAN_DATA--->"+etEanData.toString());
                                        if (etEanData.length()>1) {
                                            for (int i = 1; i < etEanData.length(); i++) {
                                                JSONObject jsonObject = etEanData.getJSONObject(i);
                                                String MANDT = jsonObject.getString("MANDT");
                                                String EANNR = jsonObject.getString("EANNR");
                                                String MATNR = jsonObject.getString("MATNR");
                                                String EAN11 = jsonObject.getString("EAN11");
                                                String UMREZ = jsonObject.getString("UMREZ");
                                                String MESRT = jsonObject.getString("MESRT");
                                                etEanDataModels.add(new EtEanDataModel(MANDT,EANNR,UMREZ,EAN11,MATNR,MESRT));

                                            }
                                        }


                                        Bundle args = new Bundle();
                                        args.putSerializable("pno", (Serializable) etPoDataModels);
                                        args.putSerializable("ean", (Serializable) etEanDataModels);
                                        args.putString("po_et",po_et.getText().toString());
                                        args.putString("invoice_et",invoice_et.getText().toString());
                                        args.putString("bno_et",bno_et.getText().toString());
                                        args.putString("ge_et",ge_et.getText().toString());
                                        args.putString("bol_et",bol_et.getText().toString());
                                        args.putInt("screen",mode);

                                        Fragment fragment = new ValidateCrate_Process_Fragment();
                                        fragment.setArguments(args);
                                        if (fragment != null) {
                                            clear();
                                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                                            ft.replace(R.id.home, fragment, "stock_in");
                                            ft.addToBackStack("stock_in");
                                            ft.commit();

                                        }

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


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {
            UIFuncs.errorSound(con);
            Log.d(TAG, TAG + " scanned result...");
            box.getBox("Scanner Err","Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null && !scanContent.equals("null")) {
                switch (scanner)

                {

                    case "po":
                        po_et.setText(scanContent);

                        Log.v(TAG, "bin code = " + scanContent);
                        invoice_et.requestFocus();
                        break;
                }

            } else {
                UIFuncs.errorSound(con);
                box.getBox("Scanner Err","No Content Received. Please Scan Again");

            }
        }


    }

    void getData(String po,String inv){

        String rfc = "ZWM_VALIDATE_PURCHASE_ORDER";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EBELN", po);
            params.put("IM_XBLNR", inv);

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
                                        invoice_et.setText("");
                                        invoice_et.requestFocus();

//                                        gr.setText("");
//                                        gr.requestFocus();
                                        return;
                                    } else {
                                        //YAHAN
                                        String bol = responsebody.getString("EX_FRBNR");

                                        if (!bol.equals(""))
                                            bol_et.setText(bol);
                                        else {
                                            bol_et.setText(invoice_et.getText().toString());
                                            bol_et.setEnabled(true);
                                            bol_et.setFocusableInTouchMode(true);
                                            bol_et.setFocusable(true);
                                        }

                                        String billNo = responsebody.getString("EX_FRBNR");
                                        if (!billNo.equals(""))
                                            bno_et.setText(billNo);
                                        else {
                                            bno_et.setText(invoice_et.getText().toString());
                                            bno_et.setEnabled(true);
                                            bno_et.setFocusableInTouchMode(true);
                                            bno_et.setFocusable(true);
                                        }

                                        String vendorId = responsebody.getString("EX_LIFNR");
                                        if (!vendorId.equals(""))
                                            ven_et.setText(vendorId);
                                        else {
                                            ven_et.setText("");
                                            ven_et.setEnabled(true);
                                            ven_et.setFocusableInTouchMode(true);
                                            ven_et.setFocusable(true);
                                        }
                                        String ge = responsebody.getString("EX_GATE_ENTRY");
                                        if (!ge.equals("")) {
                                            ge_et.setText(ge);
                                        } else {
                                            ge_et.setText("");
                                            ge_et.setEnabled(true);
                                            ge_et.setFocusableInTouchMode(true);
                                            ge_et.setFocusable(true);
                                        }

                                        // bno_et.requestFocus();
                                        Stock_In_Out_Fragment.this.ge_et.requestFocus();
                                        //bno_et.setText(inv);
                                        //bol_et.setText(inv);
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
                return DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
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
    private void loadDataFromServer(String response) {

        switch (requester) {
            case "next":
            case "nitrec":
                try {
                    next(response);
                }catch (Exception e)
                {
                    box.getErrBox(e);
                }
                break;

            case "nitupd":
                //response=response.substring(2,response.length());
                //S#R. K. CEILINGS PVT. LTD.#123#123#DH24181200025781
                //S#R. K. CEILINGS PVT. LTD.###
// Response:S#
// 000000001132034455#600-C_DNM_PLN/C_SKR_PLN-RN/M_EMB-262-1#5.000#0.000#0.000#5.000#EA#000000001132034517#1132034516-Gen-Art-6#5.000#0.000#0.000#5.000#EA#!
// 300#000000001132034455#81000136#1#E01#300#000000001132034517#81000215#1#E01#
                Log.v(TAG, "response from server = " + response);
                String myArr[] = response.split("#");


                switch (myArr.length) {
                    case 0:
                        UIFuncs.errorSound(con);
                        box.getBox("Alert", "Data Not Received Properly from Server");

                        break;
                    case 1:
                        if (!myArr[0].equals(""))
                        {

                            ven_et.setText(myArr[0]);

                            ge_et.setText("");
                            ge_et.setEnabled(true);
                            ge_et.setFocusableInTouchMode(true);
                            ge_et.setFocusable(true);

                            bol_et.setText("");
                            bol_et.setFocusableInTouchMode(true);
                            bol_et.setEnabled(true);
                            bol_et.setFocusable(true);

                            bno_et.setText("");
                            bno_et.setEnabled(true);
                            bno_et.setFocusableInTouchMode(true);
                            bno_et.setFocusable(true);
                        }
                        break;
                    case 2:
                        if (!myArr[0].equals("")) {
                            ven_et.setText(myArr[0]);
                            ven_et.setFocusableInTouchMode(true);
                            ven_et.setFocusable(true);

                        } else {
                            ven_et.setText("");
                            ven_et.setEnabled(true);
                            ven_et.setFocusableInTouchMode(true);
                            ven_et.setFocusable(true);
                        }
                        if (!myArr[1].equals("")) {
                            bol_et.setText(myArr[1]);

                        } else {
                            bol_et.setText("");
                            bol_et.setEnabled(true);
                            bol_et.setFocusableInTouchMode(true);
                            bol_et.setFocusable(true);

                        }
                        ge_et.setText("");
                        ge_et.setEnabled(true);
                        ge_et.setFocusableInTouchMode(true);
                        ge_et.setFocusable(true);

                        bno_et.setText("");
                        bno_et.setEnabled(true);
                        bno_et.setFocusableInTouchMode(true);
                        bno_et.setFocusable(true);

                        break;
                    case 3:
                        if (!myArr[0].equals("")) {
                            ven_et.setText(myArr[0]);

                        } else {
                            ven_et.setText("");
                            ven_et.setEnabled(true);
                            ven_et.setFocusableInTouchMode(true);
                            ven_et.setFocusable(true);
                        }
                        if (!myArr[3].equals("")) {
                            bno_et.setText(myArr[3]);
                            bno_et.setFocusableInTouchMode(true);
                            bno_et.setFocusable(true);

                        } else {
                            bno_et.setText("");
                            bno_et.setEnabled(true);
                            bno_et.setFocusableInTouchMode(true);
                            bno_et.setFocusable(true);
                        }
                        if (!myArr[2].equals("")) {
                            bol_et.setText(myArr[2]);

                        } else {
                            bol_et.setText("");
                            bol_et.setEnabled(true);
                            bol_et.setFocusableInTouchMode(true);
                            bol_et.setFocusable(true);

                        }
                        ge_et.setText("");
                        ge_et.setEnabled(true);
                        ge_et.setFocusableInTouchMode(true);
                        ge_et.setFocusable(true);


                        break;
                    case 4:
                        if (!myArr[3].equals(""))
                            ge_et.setText(myArr[3]);
                        else {
                            ge_et.setText("");
                            ge_et.setEnabled(true);
                            ge_et.setFocusableInTouchMode(true);
                            ge_et.setFocusable(true);
                        }
                        if (!myArr[2].equals(""))
                            bno_et.setText(myArr[2]);
                        else {
                            bno_et.setText("");
                            bno_et.setEnabled(true);
                            bno_et.setFocusableInTouchMode(true);
                            bno_et.setFocusable(true);
                        }
                        if (!myArr[1].equals(""))
                            bol_et.setText(myArr[1]);
                        else {
                            bol_et.setText("");
                            bol_et.setEnabled(true);
                            bol_et.setFocusableInTouchMode(true);
                            bol_et.setFocusable(true);
                        }
                        if (!myArr[0].equals(""))
                            ven_et.setText(myArr[0]);
                        else {
                            ven_et.setText("");
                            ven_et.setEnabled(true);
                            ven_et.setFocusableInTouchMode(true);
                            ven_et.setFocusable(true);
                        }
                        break;

                }

        }


    }

    private void next(String rcvdData) {

        String[] arrayRcvdData = rcvdData.split("!");
        String[] arrayPOData = arrayRcvdData[0].split("#");
        String[] arrayEanData = arrayRcvdData[1].split("#");
        dtPO = tables.getPOTAble("inout");
        for (int lk = 0; lk <= arrayPOData.length - 7; ) {
            String str1 = arrayPOData[lk] + "#"
                    + arrayPOData[lk + 1] + "#"
                    + arrayPOData[lk + 2] + "#"
                    + arrayPOData[lk + 3] + "#"
                    + arrayPOData[lk + 4] + "#"
                    + arrayPOData[lk + 5] + "#"
                    + arrayPOData[lk + 6];
            Log.d(TAG, "po: lk->" + lk + " :" + str1);

            dtPO.get(0).add(arrayPOData[lk]);
            dtPO.get(1).add(arrayPOData[lk + 1]);
            dtPO.get(2).add(arrayPOData[lk + 2]);
            dtPO.get(3).add(arrayPOData[lk + 3]);
            dtPO.get(4).add(arrayPOData[lk + 4]);
            dtPO.get(5).add(arrayPOData[lk + 5]);
            dtPO.get(6).add(arrayPOData[lk + 6]);
            lk = lk + 7;
        }
        Log.d(TAG, " po   :" + dtPO);
        dtEAN = tables.getEANTAble("");
        for (int lk = 0; lk <= arrayEanData.length - 5; ) {
            String str1 = arrayEanData[lk] + ","
                    + arrayEanData[lk + 1] + ","
                    + arrayEanData[lk + 2] + ","
                    + arrayEanData[lk + 3] + ","
                    + arrayEanData[lk + 4];

            Log.d(TAG, "ean: lk->" + lk + " :" + str1);

            dtEAN.get(0).add(arrayEanData[lk]);
            dtEAN.get(1).add(arrayEanData[lk + 1]);
            dtEAN.get(2).add(arrayEanData[lk + 2]);
            dtEAN.get(3).add(arrayEanData[lk + 3]);
            dtEAN.get(4).add(arrayEanData[lk + 4]);
            lk = lk + 5;
        }
        Log.d(TAG, " ean:" + dtEAN);
        dtPONO = tables.getPoNoTAble();
        dtPONO.add(po_et.getText().toString());
        dtPONO.add(invoice_et.getText().toString());
        dtPONO.add(bno_et.getText().toString());
        dtPONO.add(ge_et.getText().toString());
        dtPONO.add(bol_et.getText().toString());
        Log.d(TAG, " PONO   :" + dtPONO);

        Bundle args = new Bundle();
        args.putSerializable("pono", dtPONO);
        args.putSerializable("pno", dtPO);


        args.putSerializable("ean", dtEAN);


        Fragment fragment = new ValidateCrate_Process_Fragment();
        fragment.setArguments(args);
        if (fragment != null) {
            clear();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "stock_in");
            ft.addToBackStack("stock_in");
            ft.commit();

        }
    }

    private void clear() {

        po_et.setText("");
        bno_et.setText("");
        bol_et.setText("");
        ven_et.setText("");
        ge_et.setText("");
        invoice_et.setText("");
        po_et.requestFocus();

    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){

        if(dialog==null) {
            dialog = new ProgressDialog(getContext());
        }

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

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
                                if (type != null) {
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {

                                        if (request == REQUEST_SCREEN) {
                                            mode = Integer.parseInt(responsebody.getString("EX_SCREEN"));
                                            invoice_et.requestFocus();
                                        }
                                        return;
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
}
