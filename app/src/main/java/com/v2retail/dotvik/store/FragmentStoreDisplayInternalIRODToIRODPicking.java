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
import com.v2retail.dotvik.modal.putaway.ETDataStorePutway;
import com.v2retail.dotvik.modal.putaway.ETEanDataStorePutway;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Narayanan
 * @version 11.73
 * {@code Author: Narayanan, Revision: 1, Created: 16th Aug 2024, Modified: 16th Aug 2024}
 */
public class FragmentStoreDisplayInternalIRODToIRODPicking extends Fragment implements View.OnClickListener {

    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    String TAG = FragmentStoreDisplayInternalIRODToIRODPicking.class.getName();
    private static final int REQUEST_VALIDATE_IROD = 5401;
    private static final int REQUEST_VALIDATE_EAN = 5402;
    private static final int REQUEST_SAVE = 5403;
    String URL;
    String WERKS;
    String USER;
    private static String parent;
    Button btn_back, btn_reset, btn_next, btn_save;
    EditText txt_store, txt_sloc, txt_irod, txt_ean, txt_article, txt_description, txt_sqty, txt_tqty;
    LinearLayout ll_screen2;
    String title;
    Map<String, ETDataStorePutway> etData = new HashMap<>();
    int totalScannedQty;
    public FragmentStoreDisplayInternalIRODToIRODPicking() {
        // Required empty public constructor
    }

    public static FragmentStoreDisplayInternalIRODToIRODPicking newInstance(String breadcrumb) {
        FragmentStoreDisplayInternalIRODToIRODPicking fragment = new FragmentStoreDisplayInternalIRODToIRODPicking();
        fragment.title  = breadcrumb;
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .getSupportActionBar().setTitle(UIFuncs.getSmallTitle(title + " > PICKING"));
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
        view = inflater.inflate(R.layout.fragment_store_display_internal_irod_to_irod_picking, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_store);
        txt_sloc = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_sloc);
        txt_irod = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_irod);
        txt_ean = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_ean);
        txt_article = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_articleno);
        txt_description = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_description);
        txt_sqty = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_sqty);
        txt_tqty = view.findViewById(R.id.txt_disp_internal_irod_to_irod_picking_tqty);

        btn_back = view.findViewById(R.id.btn_disp_internal_irod_to_irod_picking_back);
        btn_reset = view.findViewById(R.id.btn_disp_internal_irod_to_irod_picking_reset);
        btn_next = view.findViewById(R.id.btn_disp_internal_irod_to_irod_picking_next);
        btn_save = view.findViewById(R.id.btn_disp_internal_irod_to_irod_picking_save);

        ll_screen2 = view.findViewById(R.id.ll_disp_internal_irod_to_irod_picking_screen2);

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
            case R.id.btn_disp_internal_irod_to_irod_picking_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_disp_internal_irod_to_irod_picking_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    step2();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_disp_internal_irod_to_irod_picking_next:
                step2();
                break;
            case R.id.btn_disp_internal_irod_to_irod_picking_save:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
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
        txt_ean.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_ean);
                    if (value.length() > 0) {
                        validateArticle();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_ean.addTextChangedListener(new TextWatcher() {
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
                    validateArticle();
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
        etData = new HashMap<>();
        totalScannedQty = 0;
        ll_screen2.setVisibility(View.VISIBLE);
        btn_reset.setVisibility(View.VISIBLE);
        btn_next.setVisibility(View.GONE);
        btn_save.setVisibility(View.VISIBLE);
        txt_irod.setText("");
        txt_ean.setText("");
        txt_article.setText("");
        txt_description.setText("");
        txt_sqty.setText("");
        txt_tqty.setText("");
        UIFuncs.disableInput(con, txt_ean);
        UIFuncs.enableInput(con, txt_irod);
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void validateIrod() {
        String irod = UIFuncs.toUpperTrim(txt_irod);
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_IROD_PICK_VALIDATE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_IROD", irod);
            showProcessingAndSubmit(Vars.ZWM_STORE_IROD_PICK_VALIDATE, REQUEST_VALIDATE_IROD, args);
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

    private void validateArticle() {
        String ean = UIFuncs.toUpperTrim(txt_ean);
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_IROD_EAN_VALIDATE);
            args.put("IM_EAN", ean);
            showProcessingAndSubmit(Vars.ZWM_STORE_IROD_EAN_VALIDATE, REQUEST_VALIDATE_EAN, args);

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

    private void setData(JSONObject rsponse) {
        try {
            JSONArray arrEtEanData = rsponse.getJSONArray("ET_EAN_DATA");
            int arrlength = arrEtEanData.length();
            if(arrlength > 0){
                JSONObject ET_EAN_RECORD  = arrEtEanData.getJSONObject(1);
                ETEanDataStorePutway eanRecord = ETEanDataStorePutway.newInstance(ET_EAN_RECORD);
                String matnr = eanRecord.getMatnr();
                ETDataStorePutway etRecord = new ETDataStorePutway();

                if(etData.containsKey(matnr)){
                    etRecord = etData.get(matnr);
                }else{
                    etRecord.setVerme("0");
                    etRecord.setLgnum("SDC");
                    etRecord.setMatnr(matnr);
                    etRecord.setIrod(UIFuncs.toUpperTrim(txt_irod));
                }

                int matnrSqty = (int) Double.parseDouble(etRecord.getVerme());
                int lotQty = (int) Double.parseDouble(eanRecord.getUmrez());

                matnrSqty = matnrSqty + lotQty;
                totalScannedQty += lotQty;

                txt_article.setText(matnr);
                txt_sqty.setText(matnrSqty + "");

                etRecord.setVerme(matnrSqty + "");

                etData.put(matnr, etRecord);
            }
            txt_description.setText(rsponse.getString("EX_MAKTX"));
            txt_tqty.setText(totalScannedQty + "");
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
        txt_ean.setText("");
        UIFuncs.enableInput(con, txt_ean);
    }

    private void saveData() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_IROD_PICK);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGNUM", "SDC");
            args.put("IM_USER", USER);
            JSONArray etScanData = getScanDataToSubmit();
            if(etScanData == null){
                return;
            }
            args.put("IT_DATA", etScanData);
            showProcessingAndSubmit(Vars.ZWM_STORE_IROD_PICK, REQUEST_SAVE, args);

        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, ETDataStorePutway> etRecords : etData.entrySet()) {
                ETDataStorePutway etRecord = etRecords.getValue();
                int matnrScannedQty = (int) Double.parseDouble(etRecord.getVerme());
                if(matnrScannedQty > 0){
                    String scanDataJsonString = new Gson().toJson(etRecord);
                    JSONObject itDataJson = new JSONObject(scanDataJsonString);
                    arrScanData.put(itDataJson);
                }
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some articles");
            }else{
                return arrScanData;
            }
        }catch (Exception exce){
            box.getErrBox(exce);
        }
        txt_ean.requestFocus();
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
                                        if (request == REQUEST_VALIDATE_IROD) {
                                            txt_irod.setText("");
                                            txt_irod.requestFocus();
                                        }
                                        if (request == REQUEST_VALIDATE_EAN) {
                                            txt_ean.setText("");
                                            txt_ean.requestFocus();
                                            return;
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_IROD) {
                                            UIFuncs.disableInput(con, txt_irod);
                                            txt_ean.setText("");
                                            UIFuncs.enableInput(con, txt_ean);
                                        }
                                        if (request == REQUEST_VALIDATE_EAN) {
                                            setData(responsebody);
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