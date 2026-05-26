package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.StockTakeProcessEtBinModel;
import com.v2retail.dotvik.modal.StockTakeProcessExHeaderModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Stock_Take_Process_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Stock_Take_Process_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Stock_Take_Process_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "bin_list";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = Stock_Take_Process_Fragment.class.getName();

    // TODO: Rename and change types of parameters
    private ArrayList<String> mParam1;
    private String mParam2;

    Tables tables = new Tables();
    Context con;
    FragmentManager fm;
    Button next;
    Button back;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;
    TextView mResponseView;
    EditText stock_take_id;
    EditText storage_id;
    EditText bin_from;
    EditText site;
    EditText warehouse_no;
    EditText bin_to;


    String requester = "";
    String scanner = "";

    List<StockTakeProcessEtBinModel> etBinModelList;
    List<StockTakeProcessExHeaderModel> headerModelList;
    ArrayList<ArrayList<String>> dtStockBinPO;
    ArrayList<ArrayList<String>> dtCrate;
    ArrayList<String> data;
    private OnFragmentInteractionListener mListener;

    public Stock_Take_Process_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Stock Take  ");
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
    public static Stock_Take_Process_Fragment newInstance(String param1, String param2) {
        Stock_Take_Process_Fragment fragment = new Stock_Take_Process_Fragment();
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
        View view = inflater.inflate(R.layout.stock_take_dc, container, false);


        back = (Button) view.findViewById(R.id.back);
        next = (Button) view.findViewById(R.id.next);

        stock_take_id = (EditText) view.findViewById(R.id.stock_id);
        storage_id = (EditText) view.findViewById(R.id.storage_type);
        bin_from = (EditText) view.findViewById(R.id.bin_from);
        bin_to = (EditText) view.findViewById(R.id.bin_to);
        site = (EditText) view.findViewById(R.id.site);
        warehouse_no = (EditText) view.findViewById(R.id.warehouse_no);

       // mResponseView = (TextView) view.findViewById(R.id.response);

        init();

        return view;
    }

    private void init(){

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
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


        etBinModelList = new ArrayList<>();
        headerModelList = new ArrayList<>();

        back.setOnClickListener(this);

        next.setOnClickListener(this);
        stock_take_id.requestFocus();
        onEditListener();
        addTextChangeListeners();


    }

    private void onEditListener() {

        stock_take_id.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String stockTakeId = stock_take_id.getText().toString();
                    if (stockTakeId == null || stockTakeId.length() < 0 || stockTakeId.equals("")) {
                        box.getBox("Alert", "Scan stock Take Id !");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(stock_take_id.getWindowToken(), 0);
                            loadStockTakeIdData();
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

        stock_take_id.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned stock_take_id  : " +  locationNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadStockTakeIdData();
                        }
                    });
                }
            }
        });

    }

    private void loadStockTakeIdData(){

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String stockTakeId = stock_take_id.getText().toString();

        if (stockTakeId == null || stockTakeId.length() < 0 || stockTakeId.equals("")) {
            box.getBox("Alert", "Scan stock Take Id !");
            dialog.dismiss();
            return;
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateStockTakeIdJson(stockTakeId);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    private void sendValidateStockTakeIdJson(String stockTakeId) {

        String rfc = "ZWM_RFC_STOCK_TAKE_GET_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_STOCK_TAKE", stockTakeId);
            params.put("IM_RFC", "X");

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
                        if (responsebody.has("EX_MESSAGE") && responsebody.get("EX_MESSAGE") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_MESSAGE");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        stock_take_id.setText("");
                                        stock_take_id.requestFocus();
                                        storage_id.setText("");
                                        bin_from.setText("");
                                        bin_to.setText("");
                                        site.setText("");
                                        warehouse_no.setText("");
                                        etBinModelList.clear();
                                        headerModelList.clear();

                                        return;

                                    } else {

                                        JSONObject jsonObject = responsebody.getJSONObject("EX_HEADER");
                                        storage_id.setText(jsonObject.getString("LGTYP"));
                                        bin_from.setText(jsonObject.getString("LGPLA_LOW"));
                                        bin_to.setText(jsonObject.getString("LGPLA_HIGH"));
                                        site.setText(jsonObject.getString("WERKS"));
                                        warehouse_no.setText(jsonObject.getString("LGNUM"));

                                        String LGTYP = jsonObject.getString("LGTYP");
                                        String AETIM = jsonObject.getString("AETIM");
                                        String AENAM = jsonObject.getString("AENAM");
                                        String WITH_CRATE = jsonObject.getString("WITH_CRATE");
                                        String WERKS = jsonObject.getString("WERKS");
                                        String AEDAT = jsonObject.getString("AEDAT");
                                        String GBSTK = jsonObject.getString("GBSTK");
                                        String STOCK_TAKE = jsonObject.getString("STOCK_TAKE");
                                        String LGPLA_HIGH = jsonObject.getString("LGPLA_HIGH");
                                        String MANDT = jsonObject.getString("MANDT");
                                        String LGPLA_LOW = jsonObject.getString("LGPLA_LOW");
                                        String ERDAT = jsonObject.getString("ERDAT");
                                        String LGNUM = jsonObject.getString("LGNUM");
                                        String UZEIT = jsonObject.getString("UZEIT");
                                        String ERNAM = jsonObject.getString("ERNAM");

                                        headerModelList.add(new StockTakeProcessExHeaderModel(LGTYP,AETIM,AENAM,WITH_CRATE,WERKS
                                                ,AEDAT,GBSTK
                                                ,STOCK_TAKE,LGPLA_HIGH,MANDT,LGPLA_LOW,ERDAT,LGNUM,UZEIT,ERNAM));

                                        JSONArray jsonArray = responsebody.getJSONArray("ET_BIN");
                                        for (int i = 1 ; i<jsonArray.length();i++){
                                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                                            String lgtyp = jsonObject1.getString("LGTYP");
                                            String mandt = jsonObject1.getString("MANDT");
                                            String werks = jsonObject1.getString("WERKS");
                                            String lvorm = jsonObject1.getString("LVORM");
                                            String erdat = jsonObject1.getString("ERDAT");
                                            String lgpla = jsonObject1.getString("LGPLA");
                                            String lgnum = jsonObject1.getString("LGNUM");
                                            String uzeit = jsonObject1.getString("UZEIT");
                                            String stock_take = jsonObject1.getString("STOCK_TAKE");
                                            String ernam = jsonObject1.getString("ERNAM");

                                            etBinModelList.add(new StockTakeProcessEtBinModel(lgtyp,mandt,werks,lvorm,erdat,lgpla
                                                    ,lgnum,uzeit,stock_take,ernam));

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
            case R.id.next:


                try {
                    nextScreen();
                }catch (Exception e)
                {
                    box.getErrBox(e);
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

    private void nextScreen() {
        String stocktakeId=stock_take_id.getText().toString().trim();
        String siteno=site.getText().toString().trim();
        String warehouse=warehouse_no.getText().toString().trim();
        data=new ArrayList<>();
        if(!stocktakeId.equals("")&& !siteno.equals("")&& !warehouse.equals(""))
        {
            data.add(stocktakeId);
            data.add(siteno);
            data.add(warehouse);
            data.add(storage_id.getText().toString().trim());

        }
        else {
            box.getBox("Alert","Please Enter All The details Before Proceeding");
            return;
        }
        if(etBinModelList!=null && headerModelList!= null && etBinModelList.isEmpty() && headerModelList.isEmpty())
        {
            box.getBox("Alert","No Data Received From Server");
            return;
        }
        Bundle args=new Bundle();
        args.putSerializable("stocktakeBin", (Serializable) etBinModelList);
        args.putSerializable("Crate", dtCrate);
        args.putSerializable("data", data);
        args.putSerializable("StockTakeDetails", (Serializable)headerModelList);
        Fragment fragment = new Scan_Stock_take_Fragment();
        fragment.setArguments(args);
        clear();
        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "Scan_Stock");
            ft.addToBackStack("Scan_Stock");
            ft.commit();

        }

    }

    private void clear() {
        stock_take_id.setText("");
        storage_id.setText("");
        bin_to.setText("");
        bin_from.setText("");
        site.setText("");
        warehouse_no.setText("");
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

                    case "bar":

                        Log.v(TAG, "bin code = " + scanContent);
                        stock_take_id.setText(scanContent);

                        try {

                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }

                        break;
                }

            } else {
                box.getBox("Scanner Err","No Content Received. Please Scan Again");

            }
        }


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
