package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.AppConstants;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class Order_Putway_in_Pigeon extends Fragment implements View.OnClickListener {

    private final String TAG = "Order_Putway_in_Pigeon";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private Activity activity;

    private View rootView;

    private EditText text_source_bin;
    private EditText text_crate;
    private EditText text_article;
    private EditText text_display_dbin;
    private EditText text_destination_bin;
    private EditText text_sales_order_no;
    private EditText text_tagged_bin;
    private Button button_save;


    JSONArray etSrcData = null;
    JSONArray etEanData = null;

    HashMap<String, String> vbeln_dbin_map = new HashMap<String, String>();
    JSONObject selectedOrderLine = null;

    private ProgressDialog dialog;

    public Order_Putway_in_Pigeon() {

    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_order_putway_in_pigeon, container, false);
        initView();
        return rootView;
    }

    void initView() {
        activity = getActivity();

        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

        text_source_bin = rootView.findViewById(R.id.text_source_bin);
        text_crate = rootView.findViewById(R.id.text_crate);
        text_article = rootView.findViewById(R.id.text_article);
        text_display_dbin = rootView.findViewById(R.id.text_display_dbin);
        text_destination_bin = rootView.findViewById(R.id.text_destination_bin);
        text_sales_order_no = rootView.findViewById(R.id.text_order_no);
        text_tagged_bin = rootView.findViewById(R.id.text_tagged_bin);

        button_save = rootView.findViewById(R.id.button_save);

        handleClicks();
        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Order Putway in Pigeon Hole");
        }
    }

    void handleClicks() {
        button_save.setOnClickListener(this);
    }

    private void handlePicklistKeypadEditor() {

        text_source_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String sourceBin = text_source_bin.getText().toString();
                    if(sourceBin.length()>0) {
                        validateSourceBin(sourceBin);
                    }
                    return true;
                }
                return false;
            }
        });

        text_source_bin.addTextChangedListener(new TextWatcher() {
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
                String sourceBin = s.toString();
                if(sourceBin.length()>0 && scannerReading) {
                    validateSourceBin(sourceBin);
                }
            }
        });
        /*
        text_source_bin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if ( (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036) && (
                            keyEvent.getScanCode() == 310
                                    || keyEvent.getScanCode() == 311
                                    || keyEvent.getScanCode() == 0)
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String sourceBin = text_source_bin.getText().toString();
                        if(sourceBin.length()>0) {
                            validateSourceBin(sourceBin);
                            return true;
                        }
                    }
                } else {
                    text_source_bin.setText("");
                }
                return false;
            }
        });
         */

        text_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String article = text_article.getText().toString();
                    if(article.length()>0) {
                        validateArticle(article);
                    }
                    return true;
                }
                return false;
            }
        });


        text_article.addTextChangedListener(new TextWatcher() {
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
                String article = s.toString();
                if(article.length()>0 && scannerReading) {
                    validateArticle(article);
                }
            }
        });

        /*
        text_article.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {

                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if ( (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036) && (
                            keyEvent.getScanCode() == 310
                                    || keyEvent.getScanCode() == 311
                                    || keyEvent.getScanCode() == 0)
                    )  {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String article = text_article.getText().toString();
                        if(article.length()>0) {
                            validateArticle(article);
                            return true;
                        }
                    }
                } else {
                    text_article.setText("");
                }
                return false;
            }
        });
         */

        text_destination_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String destBin = text_destination_bin.getText().toString();
                    if(destBin.length()>0) {
                        validateDestinationBin(destBin);
                    }
                    return true;
                }
                return false;
            }
        });

        text_destination_bin.addTextChangedListener(new TextWatcher() {
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
                String destBin = s.toString();
                if(destBin.length()>0 && scannerReading) {
                    validateDestinationBin(destBin);
                }
            }
        });

        /*
        text_destination_bin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if ( (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036) && (
                            keyEvent.getScanCode() == 310
                                    || keyEvent.getScanCode() == 311
                                    || keyEvent.getScanCode() == 0)
                    )  {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String destBin = text_destination_bin.getText().toString();
                        if(destBin.length()>0) {
                            validateDestinationBin(destBin);
                            return true;
                        }
                    }
                }
                return false;
            }
        });
         */

    }
    @Override
    public void onClick(View view) {

        CommonUtils.hideKeyboard(activity);
        switch (view.getId()){
            case R.id.button_save:
                // SCANNER_CURRENT_TYPE = SavePicklistFragment.SCANNER_TYPES.PICKLIST.name();
                // openScanner();
                break;
        }
    }


    void validateSourceBin(final String sourceBin) {
        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateSourceBinJson(sourceBin);
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

    void sendValidateSourceBinJson(String sourceBin) {
        String rfc = "ZECOM_PUTWALL_BIN_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_BIN", sourceBin);

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
             //    Log.d(TAG, "response ->" + responsebody);

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
                                        text_source_bin.setText("");
                                        text_source_bin.requestFocus();
                                        return;
                                    } else {

                                        etSrcData = responsebody.getJSONArray("ET_SRCDATA");
                                        etEanData = responsebody.getJSONArray("ET_EAN_DATA");

                                        Log.d(TAG, "response->ET_SRCDATA->" + etSrcData);
                                        Log.d(TAG, "response->ET_EAN_DATA->" + etEanData);

                                        text_article.setText("");
                                        text_article.requestFocus();
                                        // scan articles
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

    // validate order line here
    void validateArticle(String article) {
       // for(int i=0; et)
        JSONObject eanNode = null;
        boolean found = false;

        if(etEanData==null) return;

        for(int i=0; i<etEanData.length(); i++) {
            try {
                JSONObject tempNode = etEanData.getJSONObject(i);
                if(tempNode.getString("EAN11").equals(article)) {
                    found = true;
                    eanNode = tempNode;
                    break;
                }
            } catch(JSONException jsone) {

            }
        }

        selectedOrderLine = null;
        if(!found) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", "Scanned article not found: " + article);
            text_article.setText("");
            text_article.requestFocus();
        } else {
            // if found, we need to check if it belongs to same (vbeln, destination bin)
            // vbeln_dbin_map
            if(eanNode!=null) {
                String matnr = eanNode.optString("MATNR");
                if(matnr!=null && matnr.length()>0) {
                    JSONObject orderLine = findOrderLine(matnr);
                    if (orderLine != null) {
                        String orderNo = orderLine.optString("VBELN_VA");
                        if(orderNo!=null && orderNo.length()>0) {
                            // for processing and saving
                            selectedOrderLine = orderLine;
                            text_sales_order_no.setText(orderNo);

                            String taggedDestBin = vbeln_dbin_map.get(orderNo);
                            if(taggedDestBin!=null && taggedDestBin.length()>0) {
                                text_tagged_bin.setText(taggedDestBin);
                            } else {
                                text_tagged_bin.setText("");
                            }

                            text_destination_bin.setText("");
                            text_destination_bin.requestFocus();

                            getPutwallSOBin(orderNo);

                        }
                    } else {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", "Scanned article's Order not found: " + article);
                        selectedOrderLine = null;
                        text_article.setText("");
                        text_article.requestFocus();
                    }
                }
            }
        }
    }

    JSONObject findOrderLine(String matnr) {
        JSONObject retJson = null;
        for(int i=0;i<etSrcData.length(); i++) {
            try {
                JSONObject lineNode = etSrcData.getJSONObject(i);
                String saved = lineNode.optString("SAVED", "");
                if(saved.equals("")) {
                    // it is pending
                    String lineMatnr = lineNode.optString("MATNR");
                    if(lineMatnr!=null && lineMatnr.equals(matnr)) {
                        retJson = lineNode;
                        break;
                    }
                }
            } catch (JSONException jsone) {

            }
        }
        return retJson;
    }

    void validateDestinationBin(final String destBin) {

        String expecteDestinationBin = text_display_dbin.getText().toString();

        String sameDestinationBin = text_tagged_bin.getText().toString();

        // either sameDestination bin empty, or it must equal to scanned destBin
        if( destBin.equalsIgnoreCase(expecteDestinationBin)  &&   (sameDestinationBin.length()==0 ||  sameDestinationBin.equalsIgnoreCase(destBin))) {
            if(selectedOrderLine!=null) {
                dialog = new ProgressDialog(getContext());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sendSaveDestinationBinJson(destBin);
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
        } else {
            // Destination does not match same order destination bin.
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", "Scanned does not match same order destination bin.");

            text_destination_bin.setText("");
            text_destination_bin.requestFocus();
        }

    }

    void getPutwallSOBin(final String salesOrder) {
        // either sameDestination bin empty, or it must equal to scanned destBin
        if(salesOrder!=null && salesOrder.length()>0) {
            if(selectedOrderLine!=null) {
                dialog = new ProgressDialog(getContext());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        call_ZECOM_PUTWALL_GET_SO_BIN(salesOrder);
                    }
                }, 1000);
            }
        }
    }

    void call_ZECOM_PUTWALL_GET_SO_BIN(final String salesOrder) {
        final RequestQueue mRequestQueue;
        String rfc = "ZECOM_PUTWALL_GET_SO_BIN";
        JsonObjectRequest mJsonRequest = null;
        String url = Order_Putway_in_Pigeon.this.URL.substring(0, Order_Putway_in_Pigeon.this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);

        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_SALESORDER", salesOrder);
        } catch (JSONException jsone) {

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
                                if (type != null) {
                                    String message = returnobj.getString("MESSAGE");
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", message);
                                        return;
                                    } else {
                                        String destBinToDisplay = responsebody.getString("EX_BIN");
                                        text_display_dbin.setText(destBinToDisplay);

                                        String crateToDisplay = responsebody.getString("EX_CRATE");
                                        // TODO: Pradeep EX_CRATE
                                        text_crate.setText(crateToDisplay);
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

        // no retry policy on save
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( AppConstants.VOLLEY_TIMEOUT, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    void sendSaveDestinationBinJson(final String destBin) {
        String rfc = "ZECOM_PUTWALL_BIN_V33_SAVE";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            String posnr = selectedOrderLine.getString("POSNR");
            String orderNumber = selectedOrderLine.getString("VBELN_VA");

            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_SONUM", orderNumber);
            params.put("IM_SOITEM", posnr);
            params.put("IM_SRCBIN", text_source_bin.getText());
            params.put("IM_DESTBIN", destBin);
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
                                if (type != null) {
                                    String message = returnobj.getString("MESSAGE");
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", message);
                                        String pastTaggedBin = responsebody.optString("EX_TAGGED_BIN", "");
                                        if(pastTaggedBin!=null && pastTaggedBin.length()>0) {
                                            // in this case, we need to scan the destination bin again
                                            // replace pastTaggedBin for this order

                                            // now pastTaggedBin is tagged with the order number
                                             vbeln_dbin_map.put(text_sales_order_no.getText().toString(), pastTaggedBin);
                                             text_tagged_bin.setText(pastTaggedBin);
                                        }
                                        text_destination_bin.setText("");
                                        text_destination_bin.requestFocus();

                                        return;
                                    } else {
                                        String toNum = responsebody.getString("EX_TANUM");
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Success", "TO: " + toNum + ". " + message);

                                        // now destination bin is tagged with the order
                                        vbeln_dbin_map.put(text_sales_order_no.getText().toString(), text_destination_bin.getText().toString());

                                        // clear all fields except source bin
                                        text_article.setText("");
                                        text_destination_bin.setText("");
                                        text_sales_order_no.setText("");
                                        text_tagged_bin.setText("");
                                        // so that use can scan article again
                                        text_article.requestFocus();
                                        selectedOrderLine.put("SAVED", "true");
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

        // no retry policy on save
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( AppConstants.VOLLEY_TIMEOUT, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

}
