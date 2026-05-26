package com.v2retail.dotvik.dc.putwayinbin;

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
import com.v2retail.dotvik.modal.putaway.ETDataStorePutway;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FragmentHUToPallet extends Fragment implements View.OnClickListener{

    private static final int REQUEST_VALIDATE_PALLET = 1501;
    private static final int REQUEST_VALIDATE_HU = 1502;
    private static final int REQUEST_SAVE = 1503;

    private static final String TAG = FragmentHUToPallet.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_reset, btn_save;
    EditText txt_site, txt_sloc, txt_scan_hu, txt_hu, txt_scan_pallet, txt_pallet, txt_sqty, txt_tqty;

    double totalQty = 0;
    List<PalletHU> palletHUS;

    public FragmentHUToPallet() {
        // Required empty public constructor
    }

    public static FragmentHUToPallet newInstance(String param1, String param2) {
        FragmentHUToPallet fragment = new FragmentHUToPallet();
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
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("HU Putway-HU To Pallet");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_pallet_to_hu, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_site = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_site);
        txt_sloc = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_sloc);
        txt_scan_hu = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_scan_hu);
        txt_hu = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_hu);
        txt_scan_pallet = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_scan_pallet);
        txt_pallet = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_pallet);
        txt_sqty = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_sqty);
        txt_tqty = rootView.findViewById(R.id.txt_hu_putway_pallet_to_hu_tqty);

        btn_back = rootView.findViewById(R.id.btn_hu_putway_pallet_to_hu_back);
        btn_reset = rootView.findViewById(R.id.btn_hu_putway_pallet_to_hu_reset);
        btn_save = rootView.findViewById(R.id.btn_hu_putway_pallet_to_hu_save);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        txt_site.setText(WERKS);
        txt_sloc.setText("0001");

        palletHUS = new ArrayList<>();

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_hu_putway_pallet_to_hu_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_hu_putway_pallet_to_hu_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_hu_putway_pallet_to_hu_save:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty()) {
                        validateHU(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_hu.addTextChangedListener(new TextWatcher() {
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
                    validateHU(value);
                }
            }
        });
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
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void clear() {
        palletHUS = new ArrayList<>();
        totalQty = 0;
        UIFuncs.disableInput(con, txt_scan_hu);
        btn_reset.setVisibility(View.INVISIBLE);
        btn_save.setVisibility(View.GONE);
        txt_scan_hu.setText("");
        txt_hu.setText("");
        txt_scan_pallet.setText("");
        txt_pallet.setText("");
        txt_sqty.setText("0");
        txt_tqty.setText(Util.formatDouble(totalQty));
        UIFuncs.enableInput(con, txt_scan_pallet);
    }

    private void validatePallet(String pallet){
        if(!palletHUS.isEmpty()){
            box.getBox("Please Save", "Please save already scanned data before scanning new Pallet");
            txt_scan_hu.setText("");
            txt_scan_hu.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_VALIDATE_PALATE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PALATE", pallet);
            showProcessingAndSubmit(Vars.ZWM_VALIDATE_PALATE, REQUEST_VALIDATE_PALLET, args);
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

    private void validateHU(String hu){
        for (PalletHU scannedRecord: palletHUS) {
            if(scannedRecord.getHu().equalsIgnoreCase(hu)){
                box.getBox("Already Scanned", "HU is already scanned against this Pallet");
                txt_scan_pallet.setText("");
                txt_scan_hu.setText("");
                txt_scan_hu.requestFocus();
                return;
            }
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_VALIDATE_PALATE_HU);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PALATE", UIFuncs.toUpperTrim(txt_pallet));
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZWM_VALIDATE_PALATE_HU, REQUEST_VALIDATE_HU, args);
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
            JSONArray arrItSave = response.getJSONArray("IT_SAVE");
            int length = arrItSave.length();
            for(int i=1; i < length; i++){
                PalletHU palletHU = new Gson().fromJson(arrItSave.get(i).toString(), PalletHU.class);
                palletHU.setHu(UIFuncs.removeLeadingZeros(palletHU.getHu()));
                palletHUS.add(palletHU);
            }
            if(length > 1){
                double sqty = 1;
                totalQty = totalQty + sqty;
                txt_sqty.setText(Util.formatDouble(sqty));
                txt_tqty.setText(Util.formatDouble(totalQty));
                btn_save.setVisibility(View.VISIBLE);
                btn_reset.setVisibility(View.VISIBLE);
                txt_hu.setText(UIFuncs.toUpperTrim(txt_scan_hu));
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        txt_scan_hu.setText("");
        UIFuncs.enableInput(con, txt_scan_hu);
    }

    private void saveData(){
        if(palletHUS.isEmpty()){
            box.getBox("No Data Scanned", "No data scanned. Please scan some HU");
            txt_scan_hu.setText("");
            txt_scan_hu.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if(dataToSave != null){
            try {
                args.put("bapiname", Vars.ZWM_PALATE_HU_SAVE);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("ET_SAVE", dataToSave);
                showProcessingAndSubmit(Vars.ZWM_PALATE_HU_SAVE, REQUEST_SAVE, args);
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
            for (PalletHU etRecord : palletHUS) {
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
                                        if (request == REQUEST_VALIDATE_HU) {
                                            txt_scan_hu.setText("");
                                            txt_scan_hu.requestFocus();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_PALLET) {
                                            txt_pallet.setText(UIFuncs.toUpperTrim(txt_scan_pallet));
                                            txt_scan_pallet.setText("");
                                            UIFuncs.enableInput(con, txt_scan_hu);
                                            return;
                                        }
                                        if (request == REQUEST_VALIDATE_HU) {
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