package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.DataHelper;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.dotvik.modal.grt.putaway.PutawayETData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GRTReturnPutAway extends Fragment {

    private static final int REQUEST_VALIDATE_CRATE = 213;
    private static final int REQUEST_VALIDATE_EAN = 214;
    private static final int REQUEST_SAVE = 215;

    private static final String TAG = GRTReturnPutAway.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText text_crate;
    EditText text_barcode_scan;
    EditText text_article_no;
    EditText text_scanned_qty;

    Button button_save;

    LinearLayout ll_putaway_form;

    List<String> scannedBarcode = new ArrayList<>();

    Map<String, HUEANData> EAN_DATA = new HashMap<>();

    Map<String, PutawayETData> IT_DATA = new HashMap<>();

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRT Return Putaway");
    }

    public GRTReturnPutAway() {
        // Required empty public constructor
    }

    public static GRTReturnPutAway newInstance(String param1, String param2) {
        GRTReturnPutAway fragment = new GRTReturnPutAway();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_grt_return_put_away, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        text_crate = rootView.findViewById(R.id.text_ptl_return_putaway_scan_crate);
        text_barcode_scan = rootView.findViewById(R.id.text_grt_return_putaway_scan_barcode);
        text_article_no = rootView.findViewById(R.id.text_grt_return_putaway_article_no);
        text_scanned_qty = rootView.findViewById(R.id.text_grt_return_putaway_scanned_qty);

        ll_putaway_form = rootView.findViewById(R.id.ll_grt_return_putaway_form);

        text_crate.addTextChangedListener(new TextWatcher() {
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
                String crate = s.toString().toUpperCase();
                if(crate.length()>0 && scannerReading) {
                    text_crate.selectAll();
                    validateCrate(crate);
                }
            }
        });

        text_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String crate = text_crate.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        text_crate.selectAll();
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });

        text_barcode_scan.addTextChangedListener(new TextWatcher() {
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
                String articlebcode = s.toString().toUpperCase();
                if(articlebcode.length()>0 && scannerReading) {
                    text_scanned_qty.selectAll();
                    submitScannedArticle(articlebcode);
                }
            }
        });

        text_barcode_scan.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  articlebcode = text_barcode_scan.getText().toString().toUpperCase();
                    if(articlebcode.length()>0) {
                        text_barcode_scan.selectAll();
                        submitScannedArticle(articlebcode);
                        return true;
                    }
                }
                return false;
            }
        });

        text_scanned_qty.requestFocus();

        button_save = rootView.findViewById(R.id.button_save_grt_return_putaway);

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitETData();
            }
        });

        text_crate.requestFocus();

        return rootView;
    }

    private void submitETData() {
        try {
            JSONObject args = new JSONObject();
            JSONArray arrItData = new JSONArray();

            for (Map.Entry<String,PutawayETData> itData : IT_DATA.entrySet()) {
                String IT_Data_JsonString = new Gson().toJson(itData.getValue());
                JSONObject itDataJson = new JSONObject(IT_Data_JsonString);
                arrItData.put(itDataJson);
            }
            if(arrItData.length() == 0){
                box = new AlertBox(getContext());
                box.getBox("Err", "Nothing to save. 0 barcode scanned. Please reset this form and try again");
                return;
            }

            args.put("bapiname", Vars.GRT_PUTAWAY_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_NATURE", "R");
            args.put("IM_LGTYP", "");
            args.put("IM_CRATE", text_crate.getText());
            args.put("IT_DATA", arrItData);

            showProcessingAndSubmit(Vars.GRT_PUTAWAY_SAVE, REQUEST_SAVE, args);
        }catch(Exception exce){
            box = new AlertBox(getContext());
            box.getErrBox(exce);
        }
    }

    private void validateCrate(String crate) {
        JSONObject args = new JSONObject();
        try {
            EAN_DATA = new HashMap<>();
            IT_DATA = new HashMap<>();
            scannedBarcode = new ArrayList<>();

            args.put("bapiname", Vars.GRT_PUTAWAY_VALIDATE_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());

            showProcessingAndSubmit(Vars.GRT_PUTAWAY_VALIDATE_CRATE,REQUEST_VALIDATE_CRATE,args);
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

    private void submitScannedArticle(String articlebarcode) {
        text_article_no.setText("");
        articlebarcode = articlebarcode.trim();

        if(EAN_DATA.containsKey(articlebarcode.toUpperCase())){

            HUEANData eanData = EAN_DATA.get(articlebarcode.toUpperCase());
            PutawayETData etData = new PutawayETData();

            if(IT_DATA.containsKey(eanData.getLgmatnr())){

                etData = IT_DATA.get(eanData.getLgmatnr());
                etData.setLgscanqty(etData.getLgscanqty()+1);
                etData.setLgmenge(etData.getLgscanqty()+"");

            }else{

                etData.setLgmenge("1");
                etData.setLgscanqty(1);
                etData.setLgmatnr(eanData.getLgmatnr());
                etData.setLgwerks(WERKS);

            }
            IT_DATA.put(eanData.getLgmatnr(),etData);
            updateScannedList(eanData);
        }
        else {
            JSONObject args = new JSONObject();
            try {
                args.put("bapiname", Vars.GRT_PUTAWAY_VALIDATE_ARTICLE);
                args.put("IM_USER", USER);
                args.put("IM_EAN", articlebarcode.toUpperCase());

                showProcessingAndSubmit(Vars.GRT_PUTAWAY_VALIDATE_ARTICLE, REQUEST_VALIDATE_EAN, args);
            } catch (JSONException e) {
                e.printStackTrace();
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getErrBox(e);
            }
        }
    }
    private void updateEANData(JSONArray eanDataArr) {
        try {
            int totalEtRecords = eanDataArr.length() - 1;
            if (totalEtRecords > 0) {
                for (int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++) {
                    JSONObject EAN_RECORD = eanDataArr.getJSONObject(recordIndex + 1);
                    EAN_DATA.put(EAN_RECORD.getString("EAN11").toUpperCase(), DataHelper.initEANData(EAN_RECORD));
                }
                submitScannedArticle(text_barcode_scan.getText().toString());
            }
        }catch (JSONException e){
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void updateScannedList(HUEANData eanData) {
        text_barcode_scan.setText("");
        text_article_no.setText(UIFuncs.removeLeadingZeros(eanData.getLgmatnr()));
        scannedBarcode.add(eanData.getLgean11());
        text_scanned_qty.setText(scannedBarcode.size()+"");
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){

        dialog = new ProgressDialog(getContext());

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
                    dialog.dismiss();
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
                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            ll_putaway_form.setVisibility(View.VISIBLE);
                                            text_barcode_scan.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_EAN) {
                                            updateEANData(responsebody.getJSONArray("ET_EAN_DATA"));
                                        }
                                        else if (request == REQUEST_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));
                                            resetUI();
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

    private void resetUI() {

        text_barcode_scan.setText("");
        text_article_no.setText("");
        text_scanned_qty.setText("");
        EAN_DATA = new HashMap<>();
        IT_DATA = new HashMap<>();
        scannedBarcode = new ArrayList<>();
        text_barcode_scan.requestFocus();

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