package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

public class Multi_Order_Picking_V32 extends Fragment implements View.OnClickListener  {
    private final String TAG = "Multi_Order_Picking_V32";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private Activity activity;

    private View rootView;

    private EditText textSourceCrate;
    private EditText textArticle;
    private EditText textPutwallZoneBin;
    private EditText textPreviousZone;
    private EditText textScannedCount;
    private Button button_save;

    JSONArray etSrcData = null;
    JSONArray etEanData = null;

    HashMap<String, String> vbeln_dbin_map = new HashMap<String, String>();
    JSONObject selectedOrderLine = null;

    int totalScannedCount = 0;

    ProgressDialog dialog = null;

    public Multi_Order_Picking_V32() {


    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_multi_order_picking_v32, container, false);
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

        textSourceCrate = rootView.findViewById(R.id.text_source_crate);
        textArticle = rootView.findViewById(R.id.text_article);
        textPutwallZoneBin = rootView.findViewById(R.id.text_putwall_zone_bin);
        textPreviousZone = rootView.findViewById(R.id.text_previous_zone);
        textScannedCount = rootView.findViewById(R.id.text_scanned_count);

        button_save = rootView.findViewById(R.id.button_save);

        handleClicks();
        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Multi Order Picking V32");
        }

        textSourceCrate.requestFocus();
    }

    void handleClicks() {
        button_save.setOnClickListener(this);
    }

    private void handlePicklistKeypadEditor() {

        textSourceCrate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String sourceCrate = textSourceCrate.getText().toString();
                    if(sourceCrate.length()>0) {
                         validateSourceCrate(sourceCrate);
                    }
                    return true;
                }
                return false;
            }
        });

        textSourceCrate.addTextChangedListener(new TextWatcher() {
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
                String sourceCrate = s.toString();
                if(sourceCrate.length()>0 && scannerReading)  {
                    validateSourceCrate(sourceCrate);
                }
            }
        });

        /*
        textSourceCrate.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {


                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310
                            || keyEvent.getScanCode() == 311
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String sourceCrate = textSourceCrate.getText().toString();
                        if(sourceCrate.length()>0) {
                            validateSourceCrate(sourceCrate);
                            return true;
                        }
                    }
                }

                return false;
            }
        });
         */


        textArticle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String article = textArticle.getText().toString();
                    if(article.length()>0) {
                        validateArticle(article);
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });

        textArticle.addTextChangedListener(new TextWatcher() {
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
        textArticle.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {


                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310
                            || keyEvent.getScanCode() == 311
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String article = textArticle.getText().toString();
                        if(article.length()>0) {
                            validateArticle(article);
                            return true;
                        }
                    }
                }

                return false;
            }
        });
        */

        textPutwallZoneBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String putwallZoneBin = textPutwallZoneBin.getText().toString();
                    if(putwallZoneBin.length()>0) {
                        validatePutwallZone(putwallZoneBin);
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });

        textPutwallZoneBin.addTextChangedListener(new TextWatcher() {
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
                String putwallZoneBin = s.toString();
                if(putwallZoneBin.length()>0 && scannerReading) {
                    validatePutwallZone(putwallZoneBin);
                }
            }
        });

        /*
        textPutwallZoneBin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310
                            || keyEvent.getScanCode() == 311
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String putwallZoneBin = textPutwallZoneBin.getText().toString();
                        if(putwallZoneBin.length()>0) {
                            validatePutwallZone(putwallZoneBin);
                            return true;
                        }
                    }
                }

                return false;
            }
        });
         */

    }


    void validateSourceCrate(final String sourceCrate) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendCrateJson(sourceCrate);
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

    void sendCrateJson(String sourceCrate) {
        String rfc = "ZECOM_PICK32_VALIDATE_CRATE";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_CRATE", sourceCrate.toUpperCase());

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
                                        textSourceCrate.setText("");
                                        textSourceCrate.requestFocus();
                                        return;
                                    } else {
                                        etEanData = responsebody.getJSONArray("ET_EAN_DATA");
                                        etSrcData = responsebody.getJSONArray("ET_SRCDATA");

                                        int totalCount = etSrcData.length() - 1;
                                        textScannedCount.setText("" + totalScannedCount + " / " + totalCount);

                                        textArticle.setText("");
                                        textArticle.requestFocus();
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

        if(etEanData == null) {

            AlertBox box = new AlertBox(getContext());
            box.getBox("", "Article data not available. Scan source crate again.");
            textSourceCrate.setText("");;
            textSourceCrate.requestFocus();
            return;
        }

        JSONObject eanNode = null;
        boolean found = false;
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
            textArticle.setText("");
            textArticle.requestFocus();
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

                            if(vbeln_dbin_map.get(orderNo)==null) {
                                textPutwallZoneBin.setText("");
                                textPreviousZone.setText("");
                                textPutwallZoneBin.requestFocus();
                            } else {
                                // this order already had previous line scanned to
                               // vbeln_dbin_map.get(orderNo)
                                textPutwallZoneBin.setText("");
                                textPreviousZone.setText(vbeln_dbin_map.get(orderNo));
                                textPutwallZoneBin.requestFocus();
                            }
                        }
                    } else {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", "Scanned article's Order not found: " + article);
                        selectedOrderLine = null;
                        textArticle.setText("");
                        textArticle.requestFocus();
                    }
                }
            }
        }
    }

    JSONObject findOrderLine(String matnr) {
        JSONObject retJson = null;
        for(int i=1;i<etSrcData.length(); i++) {
            try {
                JSONObject lineNode = etSrcData.getJSONObject(i);
                String saved = lineNode.optString("SCANNED", "");
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

    void validatePutwallZone(final String putwallZoneBin) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidatePutwallZoneBinJson(putwallZoneBin);
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

    void sendValidatePutwallZoneBinJson(String  putwallZoneBin) {

        if(selectedOrderLine==null) return;

        String rfc = "ZECOM_PICK32_VALIDATE_BIN";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);
            params.put("IM_BIN", putwallZoneBin);
            params.put("IM_VBELN", selectedOrderLine.getString("VBELN_VA"));
            params.put("IM_POSNR", selectedOrderLine.getString("POSNR"));

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

                                        String pastTaggedBin = responsebody.optString("EX_TAGGED_BIN", "");
                                        if(pastTaggedBin!=null && pastTaggedBin.length()>0) {
                                            // in this case, we need to scan the destination bin again
                                            // replace pastTaggedBin for this order

                                            // now pastTaggedBin is tagged with the order number
                                            vbeln_dbin_map.put(selectedOrderLine.getString("VBELN_VA"), pastTaggedBin);
                                            textPutwallZoneBin.setText(pastTaggedBin);
                                            textPutwallZoneBin.requestFocus();
                                        } else {
                                            textPutwallZoneBin.setText("");
                                            textPutwallZoneBin.requestFocus();
                                        }
                                        return;
                                    } else {
                                        String taggedBin = textPutwallZoneBin.getText().toString();

                                        selectedOrderLine.put("SCANNED", "X");
                                        selectedOrderLine.put("TAGGEDBIN", taggedBin);
                                        vbeln_dbin_map.put( selectedOrderLine.getString("VBELN_VA"), taggedBin);
                                        totalScannedCount = totalScannedCount + 1;

                                        int totalCount = (etSrcData.length() - 1);
                                        textScannedCount.setText("" + totalScannedCount + " / " + totalCount);

                                        textPutwallZoneBin.setText("");
                                        textArticle.setText("");
                                        textArticle.requestFocus();
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
    public void onClick(View view) {

        // saving the data
        CommonUtils.hideKeyboard(activity);
        switch (view.getId()){
            case R.id.button_save:
                // SCANNER_CURRENT_TYPE = SavePicklistFragment.SCANNER_TYPES.PICKLIST.name();
                // openScanner();
                savePick32();
                break;
        }
    }

    void savePick32() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendScannedPick32Json();
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

    void sendScannedPick32Json() {
        String rfc = "ZECOM_PICK32_SAVE";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER);

            JSONArray etData = new JSONArray();
            for(int i=1; i<etSrcData.length();i++) {
                JSONObject srcRow = etSrcData.getJSONObject(i);
                if(srcRow.optString("SCANNED")!=null && srcRow.optString("SCANNED").equals("X")) {
                    etData.put(srcRow);
                }
            }
            params.put("ET_DATA", etData);
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
                                        box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                getFragmentManager().popBackStack();
                                                // text_article.requestFocus();
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
