package com.v2retail.dotvik.hub.outward;

import android.annotation.SuppressLint;
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
import android.widget.CheckBox;
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
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.putwayinbin.FragmentHUToPallet;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.dotvik.hub.models.GRCHU;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FragmentDispatchFromHUBHUWise extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_STORE = 1501;
    private static final int REQUEST_SAVE = 1502;

    private static final String TAG = FragmentDispatchFromHUBHUWise.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    LinearLayout ll_screen1, ll_screen2;
    EditText txt_store, txt_vehicle, txt_ttl_hu, txt_scan_hu, txt_scanned_hu, txt_pending_hu;
    CheckBox chk_pgi, chk_pgi_inv;
    Button btn_back, btn_next, btn_save;

    Map<String, GRCHU> hus = null;
    Map<String, GRCHU> scannedHus = null;

    public FragmentDispatchFromHUBHUWise() {
    }

    public static FragmentDispatchFromHUBHUWise newInstance() {
        return new FragmentDispatchFromHUBHUWise();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("Dispatch From HUB");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.hub_outward_dispatch_from_hub_hu_wise, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        ll_screen1 = rootView.findViewById(R.id.ll_hub_outward_disp_hub_huwise_screen1);
        ll_screen2 = rootView.findViewById(R.id.ll_hub_outward_disp_hub_huwise_screen2);

        txt_store = rootView.findViewById(R.id.txt_hub_outward_disp_hub_huwise_store);
        txt_vehicle = rootView.findViewById(R.id.txt_hub_outward_disp_hub_huwise_vehicle);
        txt_ttl_hu = rootView.findViewById(R.id.txt_hub_outward_disp_hub_huwise_ttl_hu);
        txt_scan_hu = rootView.findViewById(R.id.txt_hub_outward_disp_hub_huwise_scan_hu);
        txt_scanned_hu = rootView.findViewById(R.id.txt_hub_outward_disp_hub_huwise_scanned_hu);
        txt_pending_hu = rootView.findViewById(R.id.txt_hub_outward_disp_hub_huwise_pending_hu);
        chk_pgi = rootView.findViewById(R.id.chk_hub_outward_disp_hub_huwise_pgi);
        chk_pgi_inv = rootView.findViewById(R.id.chk_hub_outward_disp_hub_huwise_pgi_inv);

        btn_back = rootView.findViewById(R.id.btn_hub_outward_disp_hub_huwise_back);
        btn_next = rootView.findViewById(R.id.btn_hub_outward_disp_hub_huwise_next);
        btn_save = rootView.findViewById(R.id.btn_hub_outward_disp_hub_huwise_save);

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_hub_outward_disp_hub_huwise_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_hub_outward_disp_hub_huwise_next:
                if(UIFuncs.toUpperTrim(txt_store).isEmpty()){
                    showError("Required", "Please provide Store");
                    txt_store.requestFocus();
                    return;
                }
                validateStore();
                break;
            case R.id.btn_hub_outward_disp_hub_huwise_save:
                save();
                break;
        }
    }

    private void addInputEvents() {
        txt_store.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_store);
                    if (!value.isEmpty()) {
                        btn_next.performClick();
                        return true;
                    }
                }
                return false;
            }
        });

        txt_store.addTextChangedListener(new TextWatcher() {
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
                    btn_next.performClick();
                }
            }
        });
        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty()) {
                        scanHU(value);
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
                    scanHU(value);
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
        hus = new HashMap<>();
        scannedHus = new HashMap<>();
        step2();
        txt_store.setText("");
        ll_screen1.setVisibility(View.VISIBLE);
        ll_screen2.setVisibility(View.GONE);
        btn_save.setVisibility(View.GONE);
        btn_next.setVisibility(View.VISIBLE);
        UIFuncs.enableInput(con, txt_store);
    }

    private void step2(){
        ll_screen1.setVisibility(View.GONE);
        ll_screen2.setVisibility(View.VISIBLE);
        txt_vehicle.setText("");
        txt_scan_hu.setText("");
        updateCounts();
        btn_save.setVisibility(View.VISIBLE);
        btn_next.setVisibility(View.GONE);
        UIFuncs.enableInput(con, txt_vehicle);
    }

    @SuppressLint("DefaultLocale")
    private void updateCounts(){
        txt_ttl_hu.setText(String.format("%d", hus.size()));
        txt_scanned_hu.setText(scannedHus.size() + "");
        txt_pending_hu.setText(String.format("%d",hus.size() - scannedHus.size()));
    }

    private void validateStore(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZHUB_HU_VALIDATION_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", UIFuncs.toUpperTrim(txt_store));
            showProcessingAndSubmit(Vars.ZHUB_HU_VALIDATION_RFC, REQUEST_VALIDATE_STORE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getErrBox(e);
        }
    }

    private void setData(JSONObject responsebody){
        try{
            hus = new HashMap<>();
            scannedHus = new HashMap<>();
            JSONArray arrEtHU = responsebody.getJSONArray("IT_EXIDV");
            int length = arrEtHU.length();
            for(int i=0; i < length; i++){
                GRCHU etData = new Gson().fromJson(arrEtHU.get(i).toString(), GRCHU.class);
                hus.put(UIFuncs.removeLeadingZeros(etData.getHuno()), etData);
            }
            if(length > 0){
                step2();
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
    }

    private void scanHU(String hu){
        hu = UIFuncs.removeLeadingZeros(hu);
        boolean isValid = true;
        if(hu.isEmpty()){
            showError("Required", "Please scan HU");
            isValid = false;
        }
        if(scannedHus.containsKey(hu)){
            showError("Already Scanned", String.format("HU %s is already scanned",hu));
            isValid = false;
        }
        if(!hus.containsKey(hu)){
            showError("Invalid", String.format("Scanned HU %s is invalid",hu));
            isValid = false;
        }
        if(isValid){
            GRCHU etData = hus.get(hu);
            scannedHus.put(hu, GRCHU.newInstance(etData));
            updateCounts();
        }
        txt_scan_hu.setText("");
        txt_scan_hu.requestFocus();
    }

    private void save(){
        if(scannedHus.isEmpty()){
            box.getBox("No Data Scanned", "No data scanned. Please scan some HU");
            txt_scan_hu.setText("");
            txt_scan_hu.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if(dataToSave != null){
            try {
                args.put("bapiname", Vars.ZDIS_HU_DC_HUB_PRO_RFC);
                args.put("IM_PGI", chk_pgi.isChecked() ? "X":"");
                args.put("IM_INV_PGI", chk_pgi_inv.isChecked() ? "X":"");
                args.put("IM_VBELN", UIFuncs.toUpperTrim(txt_store));
                args.put("GT_DATA", dataToSave);
                showProcessingAndSubmit(Vars.ZDIS_HU_DC_HUB_PRO_RFC, REQUEST_SAVE, args);
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
            for (Map.Entry<String, GRCHU> etEntry : scannedHus.entrySet()) {
                String scanDataJsonString = new Gson().toJson(etEntry.getValue());
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                itDataJson.put("PLANT", UIFuncs.toUpperTrim(txt_store));
                itDataJson.put("HHTUSER", USER);
                itDataJson.put("VEHICLE_NO", UIFuncs.toUpperTrim(txt_vehicle));
                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some HU");
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
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
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
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_STORE) {
                                            txt_store.setText("");
                                            txt_store.requestFocus();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_STORE) {
                                            setData(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_SAVE) {
                                            box.getBox("Success", returnobj.getString("MESSAGE"), (dialogInterface, i) -> {
                                                clear();
                                            });
                                            return;
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