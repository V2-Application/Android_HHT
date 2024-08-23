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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.irod.BinTag;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Narayanan
 * @version 11.72
 * {@code Author: Narayanan, Revision: 1, Created: 16th Aug 2024, Modified: 16th Aug 2024}
 */
public class FragmentStoreDisplayInternalTaggingIROD extends Fragment  implements View.OnClickListener {

    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    String TAG = FragmentStoreDisplayInternalTaggingIROD.class.getName();
    private static final int REQUEST_VALIDATE_BIN = 5401;
    private static final int REQUEST_VALIDATE_IROD = 5402;
    private static final int REQUEST_SAVE = 5403;
    String URL;
    String WERKS;
    String USER;
    private static String parent;
    Button btn_back, btn_reset, btn_next, btn_save;
    EditText txt_store, txt_sloc, txt_bin, txt_irod, txt_scanned_irod;
    LinearLayout ll_screen2;
    String title;
    Map<String, List<BinTag>> irods;
    public FragmentStoreDisplayInternalTaggingIROD() {
        // Required empty public constructor
    }

    public static FragmentStoreDisplayInternalTaggingIROD newInstance(String breadcrumb) {
        FragmentStoreDisplayInternalTaggingIROD fragment = new FragmentStoreDisplayInternalTaggingIROD();
        fragment.title = breadcrumb;
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .getSupportActionBar().setTitle(UIFuncs.getSmallTitle(title + " > TAGGING IROD WITH GANDOLA"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getActivity().getSupportFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_store_display_internal_tagging_irod, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = view.findViewById(R.id.txt_disp_internal_tag_irod_store);
        txt_sloc = view.findViewById(R.id.txt_disp_internal_tag_irod_sloc);
        txt_irod = view.findViewById(R.id.txt_disp_internal_tag_irod_scan_irod);
        txt_bin = view.findViewById(R.id.txt_disp_internal_tag_irod_scan_bin);
        txt_scanned_irod = view.findViewById(R.id.txt_disp_internal_tag_irod_scanned_irod);

        btn_back = view.findViewById(R.id.txt_disp_internal_tag_irod_back);
        btn_reset = view.findViewById(R.id.txt_disp_internal_tag_irod_reset);
        btn_next = view.findViewById(R.id.txt_disp_internal_tag_irod_next);
        btn_save = view.findViewById(R.id.txt_disp_internal_tag_irod_save);

        ll_screen2 = view.findViewById(R.id.ll_disp_art_irod_screen2);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        txt_store.setText(WERKS);
        txt_sloc.setText("0001");

        clear();
        addInputEvents();
        step2();

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txt_disp_internal_tag_irod_back:
                box.confirmBack(fm, con);
                break;
            case R.id.txt_disp_internal_tag_irod_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    step2();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.txt_disp_internal_tag_irod_next:
                step2();
                break;
            case R.id.txt_disp_internal_tag_irod_save:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
        txt_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_bin);
                    if (value.length() > 0) {
                        validateBin();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_bin.addTextChangedListener(new TextWatcher() {
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
                if (value.length() > 0 && scannerReading) {
                    validateBin();
                }
            }
        });

        txt_irod.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_irod);
                    if (value.length() > 0) {
                        validateIrod();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_irod.addTextChangedListener(new TextWatcher() {
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
                if (value.length() > 0 && scannerReading) {
                    validateIrod();
                }
            }
        });
    }

    private void clear() {
        step2();
        ll_screen2.setVisibility(View.GONE);
        btn_reset.setVisibility(View.INVISIBLE);
        btn_next.setVisibility(View.VISIBLE);
        btn_save.setVisibility(View.GONE);
    }

    private void step2() {
        irods = new HashMap<>();
        ll_screen2.setVisibility(View.VISIBLE);
        btn_reset.setVisibility(View.VISIBLE);
        btn_next.setVisibility(View.GONE);
        btn_save.setVisibility(View.VISIBLE);
        txt_irod.setText("");
        txt_bin.setText("");
        UIFuncs.disableInput(con, txt_irod);
        UIFuncs.enableInput(con, txt_bin);
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void validateBin() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_GANDOLA_VALIDATE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_LGNUM","SDC");
            args.put("IM_LGPLA", UIFuncs.toUpperTrim(txt_bin));
            showProcessingAndSubmit(Vars.ZWM_STORE_GANDOLA_VALIDATE, REQUEST_VALIDATE_BIN, args);

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

    private void validateIrod() {
        String scannedirod = UIFuncs.toUpperTrim(txt_irod);
        for (Map.Entry<String, List<BinTag>> irod: irods.entrySet()) {
            String binno = irod.getKey();
            for (BinTag data:irod.getValue()) {
                if(scannedirod.equals(data.getIrod())){
                    showError("Already Scanned", "IROD "+scannedirod+" is already tagged with BIN "+binno);
                    txt_irod.setText("");
                    txt_irod.requestFocus();
                    return;
                }
            }
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_IROD_VALIDATE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_LGNUM","SDC");
            args.put("IM_LGPLA", UIFuncs.toUpperTrim(txt_bin));
            args.put("IM_IROD", scannedirod);
            showProcessingAndSubmit(Vars.ZWM_STORE_IROD_VALIDATE, REQUEST_VALIDATE_IROD, args);

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

    private void setData() {
        try {
            String binno = UIFuncs.toUpperTrim(txt_bin);
            List<BinTag> binrecords = new ArrayList<>();
            if(irods.containsKey(binno)){
                binrecords = irods.get(binno);
            }
            String irod = UIFuncs.toUpperTrim(txt_irod);
            binrecords.add(new BinTag(WERKS, "SDC", "", irod, binno));
            irods.put(binno, binrecords);
            txt_scanned_irod.setText(irod);
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
        txt_irod.setText("");
        UIFuncs.enableInput(con, txt_irod);
    }

    private void saveData() {
        JSONArray itdata = getScanDataToSubmit();
        if(itdata != null){
            JSONObject args = new JSONObject();
            try {

                args.put("bapiname", Vars.ZWM_STORE_IROD_TAG);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("IM_LGNUM","SDC");
                args.put("IT_DATA", itdata);
                showProcessingAndSubmit(Vars.ZWM_STORE_IROD_TAG, REQUEST_SAVE, args);

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
            for (Map.Entry<String, List<BinTag>> irod : irods.entrySet()) {
                List<BinTag> records = irod.getValue();
                for (BinTag data: records) {
                    String scanDataJsonString = new Gson().toJson(data);
                    JSONObject itDataJson = new JSONObject(scanDataJsonString);
                    arrScanData.put(itDataJson);
                }
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some IROD");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        txt_irod.requestFocus();
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
                                        if (request == REQUEST_VALIDATE_BIN) {
                                            step2();
                                        }
                                        if (request == REQUEST_VALIDATE_IROD) {
                                            txt_irod.setText("");
                                            txt_irod.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_BIN) {
                                            UIFuncs.disableInput(con, txt_bin);
                                            UIFuncs.enableInput(con, txt_irod);
                                            return;
                                        }
                                        if (request == REQUEST_VALIDATE_IROD) {
                                            setData();
                                            return;
                                        }
                                        if (request == REQUEST_SAVE) {
                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));
                                            step2();
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