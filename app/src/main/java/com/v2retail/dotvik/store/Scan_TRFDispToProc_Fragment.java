package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Scan_TRFDispToProc_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Scan_TRFDispToProc_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Scan_TRFDispToProc_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "source";
    private static final String ARG_PARAM2 = "dest";

    // TODO: Rename and change types of parameters
    private ArrayList<String> mParam1_list;
    private ArrayList<String> pac_mat_list = new ArrayList<>();
    private String mParam2;
    private String mParam1;
    private String scanner;

    String[] arrBarQty;

    ArrayList<ArrayList<String>> rows1;
    private String TAG = Scan_TRFDispToProc_Fragment.class.getName();
    AlertBox box;
    ProgressDialog dialog;
    Tables tables = new Tables();
    Context con;
    EditText date;
    String URL = "";
    String WERKS = "";
    String USER = "";

    FragmentManager fm;
    Button save;
    Button back;
    Button bin_scan;
    TextView mResponseView;
    EditText total_sq_et;
    EditText article_available_stock_et;
    EditText barcode_art_et;
    EditText article_no_et;
    EditText asq_et;
    EditText tsq_et;
    int ScQty = 0;
    int TPOQty = 0;
    int ScanQty = 0;
    int OpenQty = 0;
    String requester = "";
    int sum = 0;
    private JSONArray jsonArray;
    private int totalScanQty =0;

    private OnFragmentInteractionListener mListener;

    public Scan_TRFDispToProc_Fragment() {
        // Required empty public constructor
    }


    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Scan TRF Display To Process ");
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
    public static Scan_TRFDispToProc_Fragment newInstance(String param1, String param2) {
        Scan_TRFDispToProc_Fragment fragment = new Scan_TRFDispToProc_Fragment();
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
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scan_sts_wwm, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        jsonArray = new JSONArray();
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);


        back = (Button) view.findViewById(R.id.back);
        save = (Button) view.findViewById(R.id.save);

        total_sq_et = (EditText) view.findViewById(R.id.total_scan_qty);
        article_no_et = (EditText) view.findViewById(R.id.article_no);
        tsq_et = (EditText) view.findViewById(R.id.tsq);
        asq_et = (EditText) view.findViewById(R.id.asq);
        barcode_art_et = (EditText) view.findViewById(R.id.barcode_no);
        article_available_stock_et = (EditText) view.findViewById(R.id.article_available_stock);
        mResponseView = (TextView) view.findViewById(R.id.response);

        back.setOnClickListener(this);
        save.setOnClickListener(this);
        barcode_art_et.requestFocus();

        //getPACMaterial();
        addEditorListeners();
        addTextChangeListners();
        return view;
    }

    void addEditorListeners() {

        barcode_art_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = barcode_art_et.getText().toString();
                    if (!(crate.equals("") || crate.length() < 0 || crate.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(barcode_art_et.getWindowToken(), 0);
                            loadBarcode();
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }
                return false;
            }
        });


    }

    void addTextChangeListners() {
        barcode_art_et.addTextChangedListener(new TextWatcher() {
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
                            loadBarcode();
                        }
                    });
                }
            }
        });

    }

    private void loadBarcode(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (TextUtils.isEmpty(barcode_art_et.getText().toString().trim())&& barcode_art_et.equals("")) {
                    box.getBox("Alert", "Please Enter Barcode");
                    dialog.dismiss();
                    return;

                }
                try {


                    getBarcodeData(barcode_art_et.getText().toString());


                }catch (Exception e)
                {   dialog.dismiss();
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }


    private void getBarcodeData(String barcode){

        String rfc = "ZWM_STORE_GET_STOCK";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_EAN11", barcode);
            params.put("IM_LGORT", mParam1 );


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

                                        JSONObject EX_MARD = responsebody.getJSONObject("EX_MARD");
                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        String openQty = EX_MARD.getString("LABST");
                                        article_available_stock_et.setText(openQty);
                                        boolean check = true;

                                        for (int i =1 ;i<ET_EAN_DATA.length();i++){
                                            JSONObject jsonObject1 = ET_EAN_DATA.getJSONObject(i);
                                            for (int j =0 ; j<jsonArray.length();j++){
                                                JSONObject jsonObject = jsonArray.getJSONObject(j);
                                                if (jsonObject.get("MATERIAL").equals(jsonObject1.getString("MATNR"))){
                                                    String sq = jsonObject.getString("SCAN_QTY");
                                                    sum = Integer.valueOf(sq);
                                                    sum =  sum + Integer.valueOf(jsonObject1.getString("UMREZ"));
                                                    if (sum<=Double.valueOf(openQty).intValue()) {
                                                        jsonObject.put("SCAN_QTY", String.valueOf(Integer.valueOf(sq) + Integer.valueOf(jsonObject1.getString("UMREZ"))));
                                                    }
                                                    check = false;
                                                    break;
                                                }
                                            }
                                            if (check){
                                                sum =0;
                                                sum =  sum + Integer.valueOf(jsonObject1.getString("UMREZ"));
                                            }

                                            if (sum<=Double.valueOf(openQty).intValue()){
                                                totalScanQty = totalScanQty+ Integer.valueOf(Integer.valueOf(jsonObject1.getString("UMREZ")));
                                                total_sq_et.setText(String.valueOf(totalScanQty));
                                                asq_et.setText(String.valueOf(sum));
                                                tsq_et.setText(String.valueOf(sum));

                                                if (check){
                                                    JSONObject jsonObject = new JSONObject();
                                                    jsonObject.put("MATERIAL",jsonObject1.getString("MATNR"));
                                                    jsonObject.put("SCAN_QTY",jsonObject1.getString("UMREZ"));
                                                    jsonArray.put(jsonObject);
                                                }

                                            }else {
                                                box.getBox("Alert", "Scanned Qty can't be greater than Open Qty!");

                                            }
                                            Log.v("JsonData",jsonArray.toString());
                                            barcode_art_et.setText("");
                                            barcode_art_et.requestFocus();
                                            break;
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
    public void onClick(View view) {
        switch (view.getId())

        {
            case R.id.save:
                requester = "save";
                dialog = new ProgressDialog(getContext());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.show();

                if (jsonArray.length()<=0) {
                    box.getBox("Alert", "Scan All Details");
                    dialog.dismiss();
                    return;
                }
                if (mParam1==null ||mParam1.equals("")||mParam1.isEmpty()){
                    box.getBox("Alert", "Fill Source Sloc");
                    dialog.dismiss();
                    return;
                }
                if (mParam2==null ||mParam2.equals("")||mParam2.isEmpty()){
                    box.getBox("Alert", "Fill Dest Sloc");
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

                break;

            case R.id.back:

                fm.popBackStack();
                break;


        }
    }

    private void saveData(){

        String rfc = "ZWM_STORE_TRANSFER_SLOC_TO_SLO";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_LGORT_SRC", mParam1);
            params.put("IM_LGORT_DEST", mParam2 );
            params.put("IM_USER", USER );
            params.put("IT_DATA", jsonArray );



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
                                        box.getBox("Err", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                article_no_et.setText("");
                                                article_available_stock_et.setText("");
                                                asq_et.setText("");
                                                tsq_et.setText("");
                                                total_sq_et.setText("");
                                                sum = 0;
                                                totalScanQty = 0;
                                                barcode_art_et.requestFocus();

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {

            Log.d(TAG, TAG + " scanned result...");
            box.getBox("Scanner Err", "Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null) {


                barcode_art_et.setText(scanContent);
                //bar_scan.setText("Rescan");
                Log.v(TAG, "bar code = " + scanContent);

                try {
                } catch (Exception e) {
                    box.getErrBox(e);
                }


            } else {
                box.getBox("Scanner Err", "No Content Received. Please Scan Again");

            }
        }


    }


    private void networkCall() {
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {

                } catch (Exception e) {
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }









    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStackImmediate();
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

    /**
     * This interface must be implemented by activities that contain this
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
