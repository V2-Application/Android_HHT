package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.PickListModel;
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
 * Use the {@link StoreBinConsolidationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreBinConsolidationFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private EditText storeEt,pickListEt,dateEt,hu_no,bin_no,barcode_et,article_no,sqEt,bsqEt;
    private Button back,save,next;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";
    private Context con;
    private FragmentManager fm;
    private AlertBox box;
    private static final String TAG = StoreBinConsolidationFragment.class.getName();
    private ProgressDialog dialog;
    private int sum = 0;
    private List<EtEanDataModel> etEanDataModelList;
    private List<PickListModel> pickListModels;
    private JSONArray saveData;
    private int totalScanQty =0;
    boolean processThisScan = false;

    public StoreBinConsolidationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StoreBinConsolidationFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StoreBinConsolidationFragment newInstance(String param1, String param2) {
        StoreBinConsolidationFragment fragment = new StoreBinConsolidationFragment();
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_store_bin_consolidation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    private void init(View view){
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
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

        storeEt = view.findViewById(R.id.storeEt);
        pickListEt = view.findViewById(R.id.pickListEt);
        dateEt = view.findViewById(R.id.dateEt);
        hu_no = view.findViewById(R.id.hu_no);
        bin_no = view.findViewById(R.id.bin_no);
        barcode_et = view.findViewById(R.id.barcode_et);
        article_no = view.findViewById(R.id.article_no);
        bsqEt = view.findViewById(R.id.bsqEt);
        sqEt = view.findViewById(R.id.sqEt);
        back = view.findViewById(R.id.back);
        save = view.findViewById(R.id.save);
        next = view.findViewById(R.id.next);
        EditTextDate editTextDate = new EditTextDate(getContext());
        editTextDate.setDateOnView(dateEt);
        pickListEt.requestFocus();
        if (WERKS != null || !WERKS.equals("") || WERKS.length() > 0) {
            storeEt.setText(WERKS);
        }else {
            box.getBox("Err", "Invalid Store Name!");
            return;
        }

        next.setOnClickListener(this);
        save.setOnClickListener(this);
        back.setOnClickListener(this);


        onEditListener();
        addTextChangeListeners();
    }
    private void onEditListener() {

        pickListEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = pickListEt.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan invoice Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(pickListEt.getWindowToken(), 0);
                            loadPickListData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });
        hu_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = hu_no.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan invoice Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(hu_no.getWindowToken(), 0);
                            scanHuData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });
        bin_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = bin_no.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan invoice Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin_no.getWindowToken(), 0);
                            scanBinData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });
        barcode_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = bin_no.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan invoice Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin_no.getWindowToken(), 0);
                            scanArticleData();
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
    void addTextChangeListeners() {

        pickListEt.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned crate Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadPickListData();

                        }
                    });
                }
            }
        });

        hu_no.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned crate Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanHuData();
                        }
                    });
                }
            }
        });

        bin_no.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned crate Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanBinData();
                        }
                    });
                }
            }
        });

        barcode_et.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned crate Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanArticleData();
                        }
                    });
                }
            }
        });


    }

    private void loadPickListData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    String store = pickListEt.getText().toString();
                    if (store == null || store.equals("") || store.length()==0){
                        box.getBox("Error", "Start Again , Store Name not Found");
                        dialog.dismiss();
                        return;
                    }
                    etEanDataModelList = new ArrayList<>();
                    pickListModels = new ArrayList<>();
                    saveData = new JSONArray();

                    pickListApi(store);


                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }
    private void scanHuData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    String hu = hu_no.getText().toString();
                    if (hu == null || hu.equals("") || hu.length()==0){
                        box.getBox("Error", "Fill Hu Number");
                        dialog.dismiss();
                        return;
                    }
                    huValidateApi(hu);


                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }
    private void scanBinData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    String hu = bin_no.getText().toString();
                    if (hu == null || hu.equals("") || hu.length()==0){
                        box.getBox("Error", "Scan Bin");
                        dialog.dismiss();
                        return;
                    }
                    boolean flag = false;
                    for (int i=0;i<pickListModels.size();i++){
                        if (hu.equals(pickListModels.get(i).getBIN())){
                            flag = true;
                            break;
                        }
                    }

                    if (flag){
                        barcode_et.setEnabled(true);
                        barcode_et.requestFocus();
                    }else {
                        box.getBox("Alert","Incorrect Bin");
                        bin_no.requestFocus();
                        bin_no.setText("");

                    }

                    dialog.dismiss();

                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 500);
    }
    private void scanArticleData(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    String hu = barcode_et.getText().toString();
                    if (hu == null || hu.equals("") || hu.length()==0){
                        box.getBox("Error", "Scan Article");
                        dialog.dismiss();
                        return;
                    }


                    getArticleData(hu);

                    dialog.dismiss();

                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 500);
    }
    private void saveDataLoader(){
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    if (saveData.length()<=0){
                        box.getBox("Alert", "First Scan Data!");
                        dialog.dismiss();
                        return;
                    }


                    saveDataApi();

                    dialog.dismiss();

                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 500);
    }

    private void pickListApi(String pickListNumber){
        String rfc = "ZWM_STORE_BIN_LIST_VALIDATION";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_PICNR", pickListNumber);
            params.put("IM_LGORT", "0002");
            params.put("IM_LGNUM", "SDC");

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
                                        pickListEt.setText("");
                                        pickListEt.requestFocus();
                                        return;
                                    } else {

                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        JSONArray ET_PICKLIST = responsebody.getJSONArray("ET_PICKLIST");
                                        for (int i =1;i<ET_EAN_DATA.length();i++){
                                            JSONObject jsonObject = ET_EAN_DATA.getJSONObject(i);
                                            String MANDT = jsonObject.getString("MANDT");
                                            String MATNR = jsonObject.getString("MATNR");
                                            String UMREN = jsonObject.getString("UMREN");
                                            String MEINH = jsonObject.getString("MEINH");
                                            String EAN11 = jsonObject.getString("EAN11");
                                            String UMREZ = jsonObject.getString("UMREZ");
                                            String EANNR = jsonObject.getString("EANNR");

                                            etEanDataModelList.add(new EtEanDataModel(MANDT,MATNR,UMREZ,UMREN,EANNR,MEINH,EAN11));
                                        }
                                        for (int i=1;i<ET_PICKLIST.length();i++){
                                            JSONObject jsonObject = ET_PICKLIST.getJSONObject(i);
                                            String PLANT = jsonObject.getString("PLANT");
                                            String BIN = jsonObject.getString("BIN");
                                            String STOR_LOC = jsonObject.getString("STOR_LOC");
                                            String CRATE = jsonObject.getString("CRATE");
                                            String AVL_STOCK = jsonObject.getString("AVL_STOCK");
                                            String MATERIAL = jsonObject.getString("MATERIAL");
                                            String STORAGE_TYPE = jsonObject.getString("STORAGE_TYPE");

                                            pickListModels.add(new PickListModel(PLANT,BIN,STOR_LOC,CRATE,AVL_STOCK,MATERIAL,STORAGE_TYPE));
                                        }

                                        hu_no.setEnabled(true);
                                        hu_no.requestFocus();
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
    private void huValidateApi(String huNumber){
        String rfc = "ZWM_STORE_HU_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_PICNR", pickListEt.getText().toString());
            params.put("IM_EXIDV", huNumber);
            params.put("IM_LGNUM", "SDC");

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
                                        hu_no.setText("");
                                        hu_no.requestFocus();
                                        return;
                                    } else {


                                        hu_no.setEnabled(false);
                                        bin_no.setEnabled(true);
                                        bin_no.requestFocus();
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

    private void getArticleData(String article){
        String MATNR = "";
        String UMREZ = "";
        String avlQty = "";
        boolean check = true;
        boolean eanCheck = true;
        boolean avlCheck = false;
        for (int i = 0 ;i<etEanDataModelList.size();i++){
            if (article.equals(etEanDataModelList.get(i).getEAN11())){
                MATNR = etEanDataModelList.get(i).getMATNR();
                UMREZ = etEanDataModelList.get(i).getUMREZ();
                eanCheck = false;
                break;
            }
        }
        if (eanCheck){
            box.getBox("Alert", "Article not in BIN, Do you want to process?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogI, int which) {
                    try {
                        dialog = new ProgressDialog(getContext());
                        dialog.setMessage("Please wait...");
                        dialog.setCancelable(false);
                        dialog.show();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                try {

                                    if (article == null || article.equals("") || article.length()==0){
                                        box.getBox("Error", "Scan Article");
                                        dialog.dismiss();
                                        return;
                                    }


                                    getArticle(article);

                                    dialog.dismiss();

                                } catch (Exception e) {
                                    box.getErrBox(e);
                                }

                            }
                        }, 500);



                    }catch (Exception e){
                        e.printStackTrace();
                    }


                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    barcode_et.setText("");
                    barcode_et.requestFocus();
                }
            });
            dialog.dismiss();
            return;
        }

        String bin = bin_no.getText().toString();
        boolean flag = true;
        for (int i = 0;i<pickListModels.size();i++){
            if (MATNR.equals(pickListModels.get(i).getMATERIAL())
                    &&bin.equals(pickListModels.get(i).getBIN())){
                avlQty = pickListModels.get(i).getAVL_STOCK();
                flag = false;
                break;
            }
        }
        if (flag){
            avlQty ="0";
        }
        try {

        for (int j = 0; j < saveData.length(); j++) {
            JSONObject jsonObject = null;
                jsonObject = saveData.getJSONObject(j);
                if (jsonObject.get("MATERIAL").equals(MATNR)&&jsonObject.getString("BIN").equals(bin)) {
                    String sq = jsonObject.getString("SCAN_QTY");
                    sum = Integer.valueOf(sq);
                    sum = sum + Integer.valueOf(UMREZ);
                    if (sum <= Double.valueOf(avlQty).intValue()) {
                        jsonObject.put("SCAN_QTY", String.valueOf(Integer.valueOf(sq) + Integer.valueOf(UMREZ)));
                    }else {
                        avlCheck = true;
                    }
                    Log.v("SaveData",saveData.toString());

                    check = false;
                    break;
                }
        }
        if (avlCheck){
            String matr = MATNR;
            String rmrez = UMREZ;
            check = false;
            box.getBox("Alert", "Bin has no stock, Do you want to process.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        for (int j = 0; j < saveData.length(); j++) {
                            JSONObject jsonObject = null;
                            jsonObject = saveData.getJSONObject(j);
                            if (jsonObject.get("MATERIAL").equals(matr)&&jsonObject.getString("BIN").equals(bin)) {
                                String sq = jsonObject.getString("SCAN_QTY");
                                sum = Integer.valueOf(sq);
                                sum = sum + Integer.valueOf(rmrez);
                                jsonObject.put("SCAN_QTY", String.valueOf(Integer.valueOf(sq) + Integer.valueOf(rmrez)));

                                totalScanQty = totalScanQty + Integer.valueOf(Integer.valueOf(rmrez));
                                sqEt.setText(String.valueOf(totalScanQty));
                                bsqEt.setText(String.valueOf(sum));
                                article_no.setText(matr);
                                Log.v("SaveData",saveData.toString());
                                break;
                            }
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }


                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.dismiss();
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (check) {
            sum = 0;
            sum = sum + Integer.parseInt(UMREZ);
        }

        if (sum <= Double.valueOf(avlQty).intValue()) {
            totalScanQty = totalScanQty + Integer.valueOf(Integer.valueOf(UMREZ));
            sqEt.setText(String.valueOf(totalScanQty));
            bsqEt.setText(String.valueOf(sum));
            article_no.setText(MATNR);

            if (check) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("MATERIAL", MATNR);
                    jsonObject.put("SCAN_QTY", UMREZ);
                    jsonObject.put("BIN", bin);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                saveData.put(jsonObject);
                Log.v("SaveData",saveData.toString());
            }

        } else {

            if (!avlCheck){
                String matr = MATNR;
                String rmrez = UMREZ;
                boolean c = check;
                box.getBox("Alert", "Bin has no stock, Do you want to process.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            totalScanQty = totalScanQty + Integer.valueOf(Integer.valueOf(rmrez));
                            sqEt.setText(String.valueOf(totalScanQty));
                            bsqEt.setText(String.valueOf(sum));
                            article_no.setText(matr);

                            if (c) {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("MATERIAL", matr);
                                    jsonObject.put("SCAN_QTY", rmrez);
                                    jsonObject.put("BIN", bin);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                saveData.put(jsonObject);
                            }
                            Log.v("SaveData",saveData.toString());

                        }catch (Exception e){
                            e.printStackTrace();
                        }


                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
            }
//            box.getBox("Alert", "Scanned Qty can't be greater than Open Qty!");
            dialog.dismiss();
        }

        barcode_et.setText("");
        barcode_et.requestFocus();

    }

    private void getArticle(String article){
        String rfc = "ZWM_RFC_STORE_EAN_DATA_STK";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_EAN11", article);

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
                                        barcode_et.setText("");
                                        barcode_et.requestFocus();
                                        return;
                                    } else {
                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        for (int i =1;i<ET_EAN_DATA.length();i++){
                                            JSONObject jsonObject = ET_EAN_DATA.getJSONObject(i);
                                            String MANDT = jsonObject.getString("MANDT");
                                            String MATNR = jsonObject.getString("MATNR");
                                            String UMREN = jsonObject.getString("UMREN");
                                            String MEINH = jsonObject.getString("MEINH");
                                            String EAN11 = jsonObject.getString("EAN11");
                                            String UMREZ = jsonObject.getString("UMREZ");
                                            String EANNR = jsonObject.getString("EANNR");

                                            etEanDataModelList.add(new EtEanDataModel(MANDT,MATNR,UMREZ,UMREN,EANNR,MEINH,EAN11));
                                            if (i==ET_EAN_DATA.length()-1){
                                                scanArticleData();
                                            }
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
    private void saveDataApi(){
        String rfc = "ZWM_STORE_BIN_CON_PICKING_HU";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_LGNUM", "SDC");
            params.put("IM_USER", USER);
            params.put("IM_EXIDV", hu_no.getText().toString());
            params.put("IM_PICNR", pickListEt.getText().toString());
            params.put("IT_DATA", saveData);


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
                                                hu_no.setText("");
                                                hu_no.setEnabled(true);
                                                bin_no.setText("");
                                                barcode_et.setText("");
                                                article_no.setText("");
                                                bsqEt.setText("");
                                                sqEt.setText("");
                                                saveData = new JSONArray();
                                                hu_no.requestFocus();
                                                totalScanQty =0;

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.next:
                loadPickListData();
                break;
            case R.id.save:
                saveDataLoader();
                break;

            case R.id.back:
                fm.popBackStack();
                break;
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
        ((Home_Activity) getActivity())
                .setActionBarTitle("Store Bin Consolidation");
    }

}