package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Article_Sale_Scan_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Article_Sale_Scan_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Article_Sale_Scan_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "type";
    private static final String ARG_PARAM2 = "pickType";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String TAG = Article_Sale_Scan_Fragment.class.getName();
    ProgressDialog dialog;
    AlertBox box;
    Context con;
    Button search;
    Button back;
    Button scan;
    Button rescan;
    FragmentManager fm;
    String URL = "";
    String WERKS = "";
    String USER = "";

    EditText barcode_et;
    EditText article_no_et;
    TextView mResponseView;


    private OnFragmentInteractionListener mListener;


    public Article_Sale_Scan_Fragment() {
        // Required empty public constructor
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
    public static Article_Sale_Scan_Fragment newInstance(String param1, String param2) {
        Article_Sale_Scan_Fragment fragment = new Article_Sale_Scan_Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Article Scan");
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

        Log.d(TAG, TAG + " created");

        View view = inflater.inflate(R.layout.stock_in_gondala, container, false);
        con = getContext();
        dialog = new ProgressDialog(con);
        box = new AlertBox(con);
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

        barcode_et = (EditText) view.findViewById(R.id.barcode);
        mResponseView = (TextView) view.findViewById(R.id.response);


        back = (Button) view.findViewById(R.id.back);
        scan = (Button) view.findViewById(R.id.bar_scan);
        rescan = (Button) view.findViewById(R.id.rescan);
        search = (Button) view.findViewById(R.id.search);

        scan.setOnClickListener(this);
        rescan.setOnClickListener(this);
        back.setOnClickListener(this);
        search.setOnClickListener(this);


        barcode_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    String code = barcode_et.getText().toString().trim();
                    if (TextUtils.isEmpty( barcode_et.getText().toString()) && code.equals("")) {

                        box.getBox("Alert", "Enter Barcode No!");
                        return true;
                    } else {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(barcode_et.getWindowToken(), 0);

                        search.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
        barcode_et.requestFocus();
        return view;
    }


    @Override
    public void onClick(View view) {


        Log.d(TAG, TAG + " button clicked");
        switch (view.getId()) {

            case R.id.bar_scan:

                Log.d(TAG, TAG + " Scanning...");
                if(CameraCheck.isCameraAvailable(con))
                IntentIntegrator.forSupportFragment(Article_Sale_Scan_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();

                break;
            case R.id.search:
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(barcode_et.getWindowToken(), 0);

                try {
                    String code = barcode_et.getText().toString().trim();
                    if (TextUtils.isEmpty( barcode_et.getText().toString()) && code.equals("")) {

                        box.getBox("Alert", "Enter Barcode No!");
                        return ;
                    } else loadData();
                } catch (Exception e) {
                    box.getErrBox(e);
                }
                break;
            case R.id.back:
                fm.popBackStack();
                break;
            case R.id.rescan:
                barcode_et.setText("");
                Log.d(TAG, TAG + " Re-Scanning...");
                break;
        }
    }

    private void loadData() {

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {



                try {
                    if (mParam1!=null) {
                        if (mParam1.equals("generic"))
                            sendJSONGenericRequest();
                        else if (mParam1.equals("single"))
                            sendJSONSingleRequestResponse();
                    }else
                    {
                        dialog.dismiss();
                        box.getBox("MSG","single/generic not founnd");
                    }

                }catch (Exception e)
                {
                    dialog.dismiss();
                    box.getErrBox(e);
                }
            }
        }, 2000);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, TAG + " scanned result...");
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {

            box.getBox("Scanner Err", "Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null) {
                barcode_et.setText(scanContent);
            } else {
                box.getBox("Scanner Err", "No Content Received. Please Scan Again");

            }
        }


    }
    private void sendJSONSingleRequestResponse( ) {
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        URL=URL.substring(0,URL.lastIndexOf("/"));
        URL +="/noacljsonrfcadaptor?bapiname=ZWM_APP_ARTICLE_SALES_DETAIL&aclclientid=android";
        Log.d(TAG,"URL_>"+URL);
        final JSONObject params = new JSONObject();
        try {
            params.put("IM_EAN", barcode_et.getText().toString().trim() );
            params.put("bapiname", "ZWM_APP_ARTICLE_SALES_DETAIL" );
            params.put("IM_WERKS", WERKS);
            params.put("IM_GEN", " ");
        } catch (JSONException e) {
            e.printStackTrace();
            dialog.dismiss();
            box.getErrBox(e);
        }
        Log.d(TAG, "payload ->" + params.toString());

        //{"bapiname":"ZWM_APP_ARTICLE_DETAILS","IM_EAN":"14116718", "IM_GEN": "X", "IM_LGNUM":"SDC", "IM_WERKS":"HD22"}
        //RequestQueue initialized
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        mJsonRequest = new SapJsonObjectRequest( Request.Method.POST, URL, params,new Response.Listener<JSONObject>() {


            @Override
            public void onResponse(JSONObject responsebody)
            {


                dialog.dismiss();
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {

                    box.getBox("Err", "No response from Server");

                } else if (responsebody.equals("")||responsebody.equals("null")) {
                    dialog.dismiss();
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");


                } else {
                    try {
                        Log.d("tag", "  response ->" + responsebody.toString());
                        JSONArray responsearray=responsebody.getJSONArray("ET_DATA");
                       // JSONObject response=responsebody.getJSONObject("ES_DATA");
                        JSONObject ES_RETURN=responsebody.getJSONObject("ES_RETURN");
                        if(ES_RETURN!=null)
                        {
                            String type=ES_RETURN.getString("TYPE");
                            if (type!=null)
                                if(type.equals("E"))
                                {
                                    box.getBox("Err",ES_RETURN.getString("MESSAGE"));
                                    return;
                                }
                        }
                        JSONObject response=null;
                        if(responsearray!=null)
                        {        if(responsearray.length()>1) {
                            response = responsearray.getJSONObject(1);
                              }
                              else {
                            box.getBox("Err","No Data Found For barcode "+barcode_et.getText().toString());
                            return;
                        }
                        }
                        if(response!=null)
                        {
                            Bundle args = new Bundle();
                            args.putString("IM_EAN",barcode_et.getText().toString().trim());
                            args.putString("MATNR", response.getString("MATNR"));
                            args.putString("YTD_NVAL", response.getString("YTD_NVAL"));
                            args.putString("YTD_QTY", response.getString("YTD_QTY"));
                            args.putString("TD_QTY", response.getString("TD_QTY"));
                            args.putString("TD_NVAL", response.getString("TD_NVAL"));
                            args.putString("MTD_NVAL", response.getString("MTD_NVAL"));
                            args.putString("MTD_QTY", response.getString("MTD_QTY"));

                            Fragment fragment = new Article_Sales_Fragment();
                            fragment.setArguments(args);
                            if (fragment != null) {
                                barcode_et.setText("");
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                ft.replace(R.id.home, fragment, "Article_SCan");
                                ft.addToBackStack("Article_SCan");
                                ft.commit();
                            }
                        }





                    } catch (JSONException e) {
                        e.printStackTrace();
                        box.getErrBox(e);
                    }
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
                box.getBox("Err", err);
            }
        }) {
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
                Log.d(TAG,"Network response -> "+res.toString());

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
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mJsonRequest);
        Log.d(TAG,"jsonRequest getUrl ->"+ mJsonRequest.getUrl());
        Log.d(TAG,"jsonRequest getBodyContentType->"+ mJsonRequest.getBodyContentType());
        Log.d(TAG,"jsonRequest getBody->"+ mJsonRequest.getBody().toString() );
        Log.d(TAG,"jsonRequest getMethod->"+ mJsonRequest.getMethod() );
        try {
            Log.d(TAG,"jsonRequest getHeaders->"+ mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            dialog.dismiss();
            box.getErrBox(authFailureError);

        }

    }
    private void sendJSONGenericRequest( ) {
        final RequestQueue mRequestQueue;
          JsonObjectRequest mJsonRequest = null;
        URL=URL.substring(0,URL.lastIndexOf("/"));
        URL +="/noacljsonrfcadaptor?bapiname=ZWM_APP_ARTICLE_SALES_DETAIL&aclclientid=android";
        Log.d(TAG,"URL_>"+URL);
        final JSONObject params = new JSONObject();
        try {
            params.put("IM_EAN", barcode_et.getText().toString().trim() );
            params.put("bapiname", "ZWM_APP_ARTICLE_SALES_DETAIL" );
            params.put("IM_WERKS", WERKS);
            params.put("IM_GEN", "X"); 
        } catch (JSONException e) {
            e.printStackTrace();
            dialog.dismiss();
            box.getErrBox(e);
        }
        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        mJsonRequest = new SapJsonObjectRequest( Request.Method.POST, URL, params,new Response.Listener<JSONObject>() {


            @Override
            public void onResponse(JSONObject responsebody)
            {


                dialog.dismiss();
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {

                    box.getBox("Err", "No response from Server");

                } else if (responsebody.equals("")||responsebody.equals("null")) {
                    dialog.dismiss();
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");


                } else {
                    try {
                        Log.d("tag", "  response ->" + responsebody.toString());
                               JSONArray responsearray=responsebody.getJSONArray("ET_DATA");
                             //   JSONObject response=responsebody.getJSONObject("ES_DATA");
                                JSONObject returnobj=responsebody.getJSONObject("ES_RETURN");
                                if(returnobj!=null)
                                {
                                    String type=returnobj.getString("TYPE");
                                    if (type!=null)
                                    if(type.equals("E"))
                                    {
                                          box.getBox("Err",returnobj.getString("MESSAGE"));
                                          return;
                                    }
                                }
                                ArrayList<HashMap<String,String>> dataList=new ArrayList<HashMap<String, String>>()  ;
                                HashMap<String,String> args=null;
                        if(responsearray!=null)
                                {
                                    if(responsearray.length()>1)
                                    {
                                        for (int i = 1; i <= responsearray.length() - 1; i++) {
                                            JSONObject response = responsearray.getJSONObject(i);
                                            args = new HashMap<String, String>();
                                            args.put("IM_EAN", barcode_et.getText().toString().trim());
                                            args.put("MATNR", response.getString("MATNR"));
                                            args.put("YTD_NVAL", response.getString("YTD_NVAL"));
                                            args.put("YTD_QTY", response.getString("YTD_QTY"));
                                            args.put("TD_QTY", response.getString("TD_QTY"));
                                            args.put("TD_NVAL", response.getString("TD_NVAL"));
                                            args.put("MTD_NVAL", response.getString("MTD_NVAL"));
                                            args.put("MTD_QTY", response.getString("MTD_QTY"));
                                            args.put("MAKTX", response.getString("MAKTX"));

                                            dataList.add(args);
                                            Log.d("tag", "  datalist ->" + dataList);
                                        }

                                        Bundle data = new Bundle();
                                        data.putSerializable("data", dataList);

                                        Fragment fragment = new Article_Sales_Variant_Fragment();
                                        fragment.setArguments(data);
                                        if (fragment != null) {
                                            barcode_et.setText("");
                                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                                            ft.replace(R.id.home, fragment, "Article_SCan");
                                            ft.addToBackStack("Article_SCan");
                                            ft.commit();
                                        }
                                    }
                                    else{
                                    box.getBox("Err","No Data Found For barcode "+barcode_et.getText().toString());
                                    }
                                }





                    } catch (JSONException e) {
                        e.printStackTrace();
                        box.getErrBox(e);
                    }
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
                box.getBox("Err", err);
            }
        }) {
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
                Log.d(TAG,"Network response -> "+res.toString());

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
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mJsonRequest);
        Log.d(TAG,"jsonRequest getUrl ->"+ mJsonRequest.getUrl());
        Log.d(TAG,"jsonRequest getBodyContentType->"+ mJsonRequest.getBodyContentType());
        Log.d(TAG,"jsonRequest getBody->"+ mJsonRequest.getBody().toString() );
        Log.d(TAG,"jsonRequest getMethod->"+ mJsonRequest.getMethod() );
        try {
            Log.d(TAG,"jsonRequest getHeaders->"+ mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            dialog.dismiss();
            box.getErrBox(authFailureError);

        }

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

                if (response == null) {
                    dialog.dismiss();
                    box.getBox("Err", "No response from Server");

                } else if (response.equals("")) {
                    dialog.dismiss();
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");


                } else if (response.charAt(0) == ('S')) {
                    response = response.substring(2, response.length());
                    String data[] = response.split("#");
                    Bundle args = new Bundle();
                    args.putStringArray("data", data);
                    dialog.dismiss();
                    Fragment fragment = new Article_Detail_Fragment();
                    fragment.setArguments(args);
                    if (fragment != null) {
                        barcode_et.setText("");

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.home, fragment, "Article_SCan");
                        ft.addToBackStack("Article_SCan");
                        ft.commit();
                    }
                    //fm.popBackStack();

                    Log.d("tag", "  response ->" + response);

                } else if (response.charAt(0) == ('E')) {
                    Log.d(TAG, " Response is Failure -> Invalid Invoice :" + response);

                    response = response.substring(2, response.length());
                    dialog.dismiss();
                    box.getBox("Err", response);


                } else {

                    Log.d(TAG, " Response is unknown :" + response);
                    dialog.dismiss();
                    box.getBox("Err", response);


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
                box.getBox("Err", err);
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
        mStringRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mStringRequest);

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
