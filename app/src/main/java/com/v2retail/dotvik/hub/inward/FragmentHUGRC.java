package com.v2retail.dotvik.hub.inward;

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
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.putwayinbin.FragmentHUToPallet;
import com.v2retail.dotvik.dc.putwayinbin.PalletHU;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.dotvik.hub.models.GRCHU;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentHUGRC extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_INVOICE = 1501;
    private static final int REQUEST_SAVE_HU = 1502;

    private static final String TAG = FragmentHUGRC.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    LinearLayout ll_screen1, ll_screen2;
    EditText txt_hub, txt_date, txt_invoce, txt_scan_hu, txt_ttl_hu, txt_scanned_hu, txt_pending_hu;
    Button btn_back, btn_next, btn_save;

    Map<String, GRCHU> hus = null;
    Map<String, GRCHU> extHus = null;
    Map<String, GRCHU> scannedHus = null;

    /** True while {@link Vars#ZWM_INV_GRC_HUB_SAVE} is in flight for a scanned HU. */
    private boolean huSaveInFlight = false;
    private String pendingSaveHuKey = null;

    public FragmentHUGRC() {
    }

    public static FragmentHUGRC newInstance() {
        return new FragmentHUGRC();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("Scan HU GRC Process");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.hub_inward_hu_grc, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        ll_screen1 = rootView.findViewById(R.id.ll_hub_inward_hu_grc_screen1);
        ll_screen2 = rootView.findViewById(R.id.ll_hub_inward_hu_grc_screen2);

        txt_hub = rootView.findViewById(R.id.txt_hub_inward_hu_grc_hub);
        txt_date = rootView.findViewById(R.id.txt_hub_inward_hu_grc_date);
        txt_invoce = rootView.findViewById(R.id.txt_hub_inward_hu_grc_invoice);
        txt_ttl_hu = rootView.findViewById(R.id.txt_hub_inward_hu_grc_ttl_hu);
        txt_scan_hu = rootView.findViewById(R.id.txt_hub_inward_hu_grc_scan_hu);
        txt_scanned_hu = rootView.findViewById(R.id.txt_hub_inward_hu_grc_scanned_hu);
        txt_pending_hu = rootView.findViewById(R.id.txt_hub_inward_hu_grc_pending_hu);

        btn_back = rootView.findViewById(R.id.btn_hub_inward_hu_grc_back);
        btn_next = rootView.findViewById(R.id.btn_hub_inward_hu_grc_next);
        btn_save = rootView.findViewById(R.id.btn_hub_inward_hu_grc_save);

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
            case R.id.btn_hub_inward_hu_grc_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_hub_inward_hu_grc_next:
                if(UIFuncs.toUpperTrim(txt_invoce).isEmpty()){
                    showError("Required", "Please provide invoice");
                    txt_invoce.requestFocus();
                    return;
                }
                validateInvoice();
                break;
            case R.id.btn_hub_inward_hu_grc_save:
                break;
        }
    }

    private void addInputEvents() {
        txt_invoce.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_invoce);
                    if (!value.isEmpty()) {
                        btn_next.performClick();
                        return true;
                    }
                }
                return false;
            }
        });

        txt_invoce.addTextChangedListener(new TextWatcher() {
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
        huSaveInFlight = false;
        pendingSaveHuKey = null;
        hus = new HashMap<>();
        extHus = new HashMap<>();
        scannedHus = new HashMap<>();
        step2();
        txt_hub.setText(WERKS);
        ll_screen1.setVisibility(View.VISIBLE);
        ll_screen2.setVisibility(View.GONE);
        btn_save.setVisibility(View.GONE);
        btn_next.setVisibility(View.VISIBLE);
        txt_date.setText("");
        txt_invoce.setText("");
        UIFuncs.enableInput(con, txt_invoce);
    }

    private void step2(){
        ll_screen1.setVisibility(View.GONE);
        ll_screen2.setVisibility(View.VISIBLE);
        txt_scan_hu.setText("");
        updateCounts();
        btn_save.setVisibility(View.GONE);
        btn_next.setVisibility(View.GONE);
        UIFuncs.enableInput(con, txt_scan_hu);
    }

    @SuppressLint("DefaultLocale")
    private void updateCounts(){
        txt_ttl_hu.setText(String.format("%d", hus.size()));
        txt_scanned_hu.setText(scannedHus.size() + "");
        txt_pending_hu.setText(String.format("%d",hus.size() - scannedHus.size()));
    }

    private void validateInvoice(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_INV_GRC_VALIDATION);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_VBELN", UIFuncs.toUpperTrim(txt_invoce));
            showProcessingAndSubmit(Vars.ZWM_INV_GRC_VALIDATION, REQUEST_VALIDATE_INVOICE, args);
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

    private static boolean isEtHuHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String id = row.optString("EXIDV", "").trim();
        String huno = row.optString("SAP_HU", "").trim();
        String invoice = row.optString("VBELN", "").trim();
        if ("EXIDV".equalsIgnoreCase(id) || "SAP_HU".equalsIgnoreCase(huno) || "VBELN".equalsIgnoreCase(invoice)) {
            return true;
        }
        return id.contains("External Handling Unit") || huno.contains("Handling Unit");
    }

    private static int etHuStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return 0;
        }
        return isEtHuHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
    }

    private void setData(JSONObject responsebody){
        try{
            hus = new HashMap<>();
            scannedHus = new HashMap<>();
            extHus = new HashMap<>();
            JSONArray arrEtHU = responsebody.getJSONArray("ET_HU");
            int start = etHuStartIndex(arrEtHU);
            for (int i = start; i < arrEtHU.length(); i++) {
                JSONObject row = arrEtHU.getJSONObject(i);
                if (isEtHuHeaderRow(row)) {
                    continue;
                }
                GRCHU etData = new Gson().fromJson(row.toString(), GRCHU.class);
                hus.put(UIFuncs.removeLeadingZeros(etData.getHuno()), etData);
                extHus.put(UIFuncs.removeLeadingZeros(etData.getId()), etData);
            }
            if (!hus.isEmpty()) {
                step2();
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
    }

    private void scanHU(String hu){
        if (huSaveInFlight) {
            return;
        }
        hu = UIFuncs.removeLeadingZeros(hu);
        boolean isValid = true;
        GRCHU etData = null;
        if(hu.isEmpty()){
            showError("Required", "Please scan HU");
            isValid = false;
        }
        if(scannedHus.containsKey(hu)){
            showError("Already Scanned", String.format("HU %s is already scanned",hu));
            isValid = false;
        }
        if(!hus.containsKey(hu)){
            if(!extHus.containsKey(hu)){
                showError("Invalid", String.format("Scanned HU %s is invalid",hu));
                isValid = false;
            }else{
                etData = extHus.get(hu);
            }
        }else{
            etData = hus.get(hu);
        }
        if(isValid && Objects.nonNull(etData)){
            saveValidatedHu(hu, etData);
            return;
        }
        txt_scan_hu.setText("");
        txt_scan_hu.requestFocus();
    }

    /** After local HU validation succeeds, persist that HU via {@link Vars#ZWM_INV_GRC_HUB_SAVE}. */
    private void saveValidatedHu(String hu, GRCHU etData) {
        try {
            huSaveInFlight = true;
            pendingSaveHuKey = hu;
            UIFuncs.disableInput(con, txt_scan_hu);

            JSONArray itHu = new JSONArray();
            itHu.put(new JSONObject(new Gson().toJson(etData)));

            JSONObject args = new JSONObject();
            args.put("bapiname", Vars.ZWM_INV_GRC_HUB_SAVE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_VBELN", UIFuncs.toUpperTrim(txt_invoce));
            args.put("IT_HU", itHu);
            showProcessingAndSubmit(Vars.ZWM_INV_GRC_HUB_SAVE, REQUEST_SAVE_HU, args);
        } catch (JSONException e) {
            resetHuSaveState();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void resetHuSaveState() {
        huSaveInFlight = false;
        pendingSaveHuKey = null;
        UIFuncs.enableInput(con, txt_scan_hu);
        txt_scan_hu.setText("");
        txt_scan_hu.requestFocus();
    }

    private void onHuSaved(JSONObject responsebody) {
        try {
            if (pendingSaveHuKey == null) {
                resetHuSaveState();
                return;
            }
            GRCHU saved = null;
            if (hus.containsKey(pendingSaveHuKey)) {
                saved = GRCHU.newInstance(hus.get(pendingSaveHuKey));
            } else if (extHus.containsKey(pendingSaveHuKey)) {
                saved = GRCHU.newInstance(extHus.get(pendingSaveHuKey));
            }
            if (saved != null) {
                scannedHus.put(pendingSaveHuKey, saved);
            }
            updateCounts();
            resetHuSaveState();

            if (scannedHus.size() >= hus.size() && hus.size() > 0) {
                JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                box.getBox("Success", returnobj.getString("MESSAGE"), (dialogInterface, i) -> clear());
            }
        } catch (JSONException e) {
            resetHuSaveState();
            box.getErrBox(e);
        }
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
                                        if (request == REQUEST_VALIDATE_INVOICE) {
                                            txt_invoce.setText("");
                                            txt_invoce.requestFocus();
                                            return;
                                        }
                                        if (request == REQUEST_SAVE_HU) {
                                            resetHuSaveState();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_INVOICE) {
                                            setData(responsebody);
                                            return;
                                        }
                                        if (request == REQUEST_SAVE_HU) {
                                            onHuSaved(responsebody);
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
                if (huSaveInFlight) {
                    resetHuSaveState();
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }
}