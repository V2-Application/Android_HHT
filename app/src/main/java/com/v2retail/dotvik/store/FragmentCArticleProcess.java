package com.v2retail.dotvik.store;

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
import android.widget.RadioButton;
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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.DiscountArticle;
import com.v2retail.dotvik.modal.picking.ETEan;
import com.v2retail.dotvik.modal.putaway.DiscountArticleScan;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FragmentCArticleProcess extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_BARCODE = 1501;
    private static final int REQUEST_SAVE = 1502;

    private static final String TAG = FragmentCArticleProcess.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_reset, btn_save;
    EditText txt_store_code, txt_scan_barcode, txt_article, txt_sqty, txt_tqty, txt_cur_discount, txt_prev_discount;
    RadioButton radio_0001,radio_0006;
    double totalQty = 0;
    Map<String, ETEan> eanDataMap = new HashMap<>();
    Map<String, DiscountArticle> discountDataMap = new HashMap<>();
    Map<String, DiscountArticleScan> discountArticleScanMap = new HashMap<>();
    /** Normalized EAN from last GET_EAN call — used to pick the row when ET_* has one line at index 0. */
    private String lastValidatedBarcode = "";

    public FragmentCArticleProcess() {
    }

    public static FragmentCArticleProcess newInstance() {
        return new FragmentCArticleProcess();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("C-Article Process");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_carticle_process, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_store_code = rootView.findViewById(R.id.txt_002msa_carticle_process_store_code);
        txt_scan_barcode = rootView.findViewById(R.id.txt_002msa_carticle_process_scan_barcode);
        txt_article = rootView.findViewById(R.id.txt_002msa_carticle_process_article);
        txt_cur_discount = rootView.findViewById(R.id.txt_002msa_carticle_process_cur_discount);
        txt_prev_discount = rootView.findViewById(R.id.txt_002msa_carticle_process_prev_discount);
        txt_sqty = rootView.findViewById(R.id.txt_002msa_carticle_process_sqty);
        txt_tqty = rootView.findViewById(R.id.txt_002msa_carticle_process_tsqty);

        btn_back = rootView.findViewById(R.id.btn_002msa_carticle_process_back);
        btn_reset = rootView.findViewById(R.id.btn_002msa_carticle_process_reset);
        btn_save = rootView.findViewById(R.id.btn_002msa_carticle_process_save);

        radio_0001 = rootView.findViewById(R.id.radio_002msa_carticle_process_0001);
        radio_0006 = rootView.findViewById(R.id.radio_002msa_carticle_process_0006);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        clear();
        addInputEvents();
        radio_0001.setChecked(true);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_002msa_carticle_process_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_002msa_carticle_process_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_002msa_carticle_process_save:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
        txt_scan_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_barcode);
                    if (!value.isEmpty()) {
                        validateBarcode(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_barcode.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateBarcode(value);
                }
            }
        });
    }

    private static String normalizeEanKey(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toUpperCase();
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void clear() {
        eanDataMap = new HashMap<>();
        discountDataMap = new HashMap<>();
        discountArticleScanMap = new HashMap<>();
        lastValidatedBarcode = "";
        totalQty = 0;
        btn_reset.setVisibility(View.INVISIBLE);
        btn_save.setVisibility(View.GONE);
        txt_scan_barcode.setText("");
        txt_store_code.setText(WERKS);
        txt_article.setText("");
        txt_cur_discount.setText("");
        txt_prev_discount.setText("");
        txt_sqty.setText("0");
        txt_tqty.setText(Util.formatDouble(totalQty));
        UIFuncs.enableInput(con, txt_scan_barcode);
    }

    private void validateBarcode(String barcode){
        String barcodeKey = normalizeEanKey(barcode);
        if(discountArticleScanMap.containsKey(barcodeKey)){
            DiscountArticleScan scannedArticle = discountArticleScanMap.get(barcodeKey);
            double sqty = Util.convertStringToDouble(scannedArticle.getSqnty()) + 1;
            totalQty = totalQty + 1;
            txt_sqty.setText(Util.formatDouble(sqty));
            txt_tqty.setText(Util.formatDouble(totalQty));
            scannedArticle.setSqnty(Util.formatDouble(sqty));
            ETEan eanRow = eanDataMap.get(barcodeKey);
            if (eanRow != null) {
                txt_article.setText(UIFuncs.removeLeadingZeros(eanRow.getLgmatnr()));
            }
            txt_cur_discount.setText(scannedArticle.getDisper());
            txt_prev_discount.setText(scannedArticle.getDisper1());
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
        }else{
            JSONObject args = new JSONObject();
            try {
                lastValidatedBarcode = barcodeKey;
                args.put("bapiname", Vars.ZSTORE_DISCOUNT_GET_EAN_DATA);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("IM_EAN", barcode);
                args.put("IM_0001", radio_0001.isChecked() ? "X" : "");
                args.put("IM_0006", radio_0006.isChecked() ? "X" : "");
                showProcessingAndSubmit(Vars.ZSTORE_DISCOUNT_GET_EAN_DATA, REQUEST_VALIDATE_BARCODE, args);
            } catch (JSONException e) {
                e.printStackTrace();
                UIFuncs.errorSound(con);
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getErrBox(e);
            }
        }
    }

    private void setData(JSONObject response){
        try{
            JSONArray arrEanData = response.getJSONArray("ET_EAN_DATA");
            JSONArray arrDiscData = response.getJSONArray("ET_DISCOUNT_DATA");
            int eanLength = arrEanData.length();
            int discLength = arrDiscData.length();

            for (int i = 0; i < eanLength; i++) {
                JSONObject row = arrEanData.optJSONObject(i);
                if (row == null || row.length() == 0) {
                    continue;
                }
                ETEan eanData = new Gson().fromJson(row.toString(), ETEan.class);
                if (eanData.getLgean11() == null || eanData.getLgean11().trim().isEmpty()) {
                    continue;
                }
                eanDataMap.put(normalizeEanKey(eanData.getLgean11()), eanData);
            }

            DiscountArticle discData = null;
            for (int i = 0; i < discLength; i++) {
                JSONObject row = arrDiscData.optJSONObject(i);
                if (row == null || row.length() == 0) {
                    continue;
                }
                DiscountArticle d = new Gson().fromJson(row.toString(), DiscountArticle.class);
                if (d.getEan11() == null || d.getEan11().trim().isEmpty()) {
                    continue;
                }
                discountDataMap.put(normalizeEanKey(d.getEan11()), d);
            }

            if (!lastValidatedBarcode.isEmpty()) {
                discData = discountDataMap.get(lastValidatedBarcode);
            }
            if (discData == null && !discountDataMap.isEmpty()) {
                discData = discountDataMap.values().iterator().next();
            }

            if (discData != null) {
                String startdate = Util.DateTime("yyyyMMdd", new Date());
                String starttime = Util.DateTime("HHmmss", new Date());

                double sqty = 1;
                totalQty = totalQty + sqty;
                txt_sqty.setText(Util.formatDouble(sqty));
                txt_tqty.setText(Util.formatDouble(totalQty));
                btn_save.setVisibility(View.VISIBLE);
                btn_reset.setVisibility(View.VISIBLE);
                txt_article.setText(UIFuncs.removeLeadingZeros(discData.getMatnr()));
                txt_cur_discount.setText(discData.getDisper());
                txt_prev_discount.setText(discData.getDisper1());

                DiscountArticleScan articleScan = new DiscountArticleScan();
                String eanKey = normalizeEanKey(discData.getEan11());
                articleScan.setEan11(discData.getEan11());
                articleScan.setErname(discData.getErname());
                articleScan.setHhtuser(USER);
                articleScan.setMatnr(discData.getMatnr());
                articleScan.setMandt(discData.getMandt());
                articleScan.setSqnty(Util.formatDouble(sqty));
                articleScan.setStktrf("X");
                articleScan.setWerks(WERKS);
                articleScan.setSdate(startdate);
                articleScan.setStime(starttime);
                articleScan.setDisper(discData.getDisper());
                articleScan.setDisper1(discData.getDisper1());
                discountArticleScanMap.put(eanKey, articleScan);
            }

        }catch (Exception exce){
            box.getErrBox(exce);
        }
        txt_scan_barcode.setText("");
        UIFuncs.enableInput(con, txt_scan_barcode);
    }

    private void saveData(){
        if(discountArticleScanMap.isEmpty()){
            box.getBox("No Data Scanned", "No data scanned. Please scan some HU");
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();

        if(dataToSave != null){
            try {
                args.put("bapiname", Vars.ZSTORE_DISCOUNT_SAVE_EAN_DATA);
                args.put("IM_PLANT", WERKS);
                args.put("IM_USER", USER);
                args.put("IM_0001", radio_0001.isChecked() ? "X":"");
                args.put("IM_0006", radio_0006.isChecked() ? "X":"");
                args.put("IT_DATA", dataToSave);
                showProcessingAndSubmit(Vars.ZSTORE_DISCOUNT_SAVE_EAN_DATA, REQUEST_SAVE, args);
            } catch (JSONException e) {
                e.printStackTrace();
                UIFuncs.errorSound(con);
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getErrBox(e);
            }
        }
    }

    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, DiscountArticleScan> articleScanEntry : discountArticleScanMap.entrySet()) {
                String scanDataJsonString = new Gson().toJson(articleScanEntry.getValue());
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some articles");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        return null;
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

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

    private void submitRequest(String rfc, int request, JSONObject args) {

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if (dialog != null) {
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
                                if (type != null) {
                                    if (type.equals("E")) {
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_BARCODE) {
                                            txt_scan_barcode.setText("");
                                            txt_scan_barcode.requestFocus();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_BARCODE) {
                                            setData(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));
                                            clear();
                                            return;
                                        }
                                    }
                                }
                                return;
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
            if (dialog != null) {
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

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }
}