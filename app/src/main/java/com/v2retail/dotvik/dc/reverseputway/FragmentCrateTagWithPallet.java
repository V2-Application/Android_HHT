package com.v2retail.dotvik.dc.reverseputway;

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
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.putwayinbin.FragmentHUToPallet;
import com.v2retail.dotvik.dc.putwayinbin.PalletHU;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentCrateTagWithPallet extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_PALLET = 1501;
    private static final int REQUEST_VALIDATE_CRATE = 1502;
    private static final int REQUEST_SAVE = 1503;

    private static final String TAG = FragmentCrateTagWithPallet.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_reset, btn_save;
    EditText txt_site, txt_sloc, txt_scan_crate, txt_crate, txt_scan_pallet, txt_pallet, txt_sqty, txt_tqty;

    double totalQty = 0;
    List<PalletCrate> palletCrates;
    public FragmentCrateTagWithPallet() {
        // Required empty public constructor
    }

    public static FragmentCrateTagWithPallet newInstance() {
        FragmentCrateTagWithPallet fragment = new FragmentCrateTagWithPallet();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("Crate Tagging With Pallet");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_crate_tag_with_pallet, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_site = rootView.findViewById(R.id.txt_grt_reverse_putway_site);
        txt_sloc = rootView.findViewById(R.id.txt_grt_reverse_putway_sloc);
        txt_scan_crate = rootView.findViewById(R.id.txt_grt_reverse_putway_scan_crate);
        txt_crate = rootView.findViewById(R.id.txt_grt_reverse_putway_crate);
        txt_scan_pallet = rootView.findViewById(R.id.txt_grt_reverse_putway_scan_pallet);
        txt_pallet = rootView.findViewById(R.id.txt_grt_reverse_putway_pallet);
        txt_sqty = rootView.findViewById(R.id.txt_grt_reverse_putway_sqty);
        txt_tqty = rootView.findViewById(R.id.txt_grt_reverse_putway_tqty);

        btn_back = rootView.findViewById(R.id.btn_grt_reverse_putway_back);
        btn_reset = rootView.findViewById(R.id.btn_grt_reverse_putway_reset);
        btn_save = rootView.findViewById(R.id.btn_grt_reverse_putway_save);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        txt_site.setText(WERKS);
        txt_sloc.setText("0001");

        palletCrates = new ArrayList<>();
        
        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_grt_reverse_putway_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_grt_reverse_putway_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_grt_reverse_putway_save:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
        txt_scan_pallet.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_pallet);
                    if (!value.isEmpty()) {
                        validatePallet(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_pallet.addTextChangedListener(new TextWatcher() {
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
                    validatePallet(value);
                }
            }
        });
        txt_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_crate);
                    if (!value.isEmpty()) {
                        validateCrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_crate.addTextChangedListener(new TextWatcher() {
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
                    validateCrate(value);
                }
            }
        });
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void clear() {
        palletCrates = new ArrayList<>();
        UIFuncs.disableInput(con, txt_scan_crate);
        btn_reset.setVisibility(View.INVISIBLE);
        btn_save.setVisibility(View.GONE);
        txt_scan_pallet.setText("");
        txt_pallet.setText("");
        txt_scan_crate.setText("");
        txt_crate.setText("");
        txt_sqty.setText("0");
        txt_tqty.setText(Util.formatDouble(totalQty));
        UIFuncs.enableInput(con, txt_scan_pallet);
    }
    private void validatePallet(String pallet){
        if(!palletCrates.isEmpty()){
            box.getBox("Please Save", "Please save already scanned data before scanning new Pallet");
            txt_scan_crate.setText("");
            txt_scan_crate.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PALATE_VALIDATION);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PALATE", pallet);
            showProcessingAndSubmit(Vars.ZWM_PALATE_VALIDATION, REQUEST_VALIDATE_PALLET, args);
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

    private void validateCrate(String crate){
        for (PalletCrate scannedRecord: palletCrates) {
            if(scannedRecord.getCrate().equalsIgnoreCase(crate)){
                box.getBox("Already Scanned", "Crate is already scanned against this Pallet");
                txt_scan_crate.setText("");
                txt_scan_crate.requestFocus();
                return;
            }
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_CRATE_VALIDATION);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PALATE", UIFuncs.toUpperTrim(txt_pallet));
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_CRATE_VALIDATION, REQUEST_VALIDATE_CRATE, args);
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

    private void setData(JSONObject response){
        try{
            JSONArray arrItSave = response.getJSONArray("IT_CRATE");
            int length = arrItSave.length();
            for(int i=1; i < length; i++){
                PalletCrate palletCrate = new Gson().fromJson(arrItSave.get(i).toString(), PalletCrate.class);
                palletCrates.add(palletCrate);
            }
            if(length > 1){
                double sqty = 1;
                totalQty = totalQty + sqty;
                txt_sqty.setText(Util.formatDouble(sqty));
                txt_tqty.setText(Util.formatDouble(totalQty));
                btn_save.setVisibility(View.VISIBLE);
                btn_reset.setVisibility(View.VISIBLE);
                txt_crate.setText(UIFuncs.toUpperTrim(txt_scan_crate));
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        txt_scan_crate.setText("");
        UIFuncs.enableInput(con, txt_scan_crate);
    }

    private void saveData(){
        if(palletCrates.isEmpty()){
            box.getBox("No Data Scanned", "No data scanned. Please scan some Crates");
            txt_scan_crate.setText("");
            txt_scan_crate.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if(dataToSave != null){
            try {
                args.put("bapiname", Vars.ZWM_SAVE_PAL_CRATE);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("ET_CRATE", dataToSave);
                showProcessingAndSubmit(Vars.ZWM_SAVE_PAL_CRATE, REQUEST_SAVE, args);
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
            for (PalletCrate etRecord : palletCrates) {
                String scanDataJsonString = new Gson().toJson(etRecord);
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
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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
                                        if (request == REQUEST_VALIDATE_PALLET) {
                                            txt_scan_pallet.setText("");
                                            txt_scan_pallet.requestFocus();
                                            return;
                                        }
                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            txt_scan_crate.setText("");
                                            txt_scan_crate.requestFocus();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_PALLET) {
                                            txt_pallet.setText(UIFuncs.toUpperTrim(txt_scan_pallet));
                                            txt_scan_pallet.setText("");
                                            UIFuncs.enableInput(con, txt_scan_crate);
                                            return;
                                        }
                                        if (request == REQUEST_VALIDATE_CRATE) {
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