package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Locale;

public class GRTComboCrateScanning extends Fragment {

    private static final int REQUEST_VALIDATE_COMBO_CRATE = 613;
    private static final int REQUEST_VALIDATE_ZONE = 614;

    private static String ARG_ZONE = "zone";

    private static final String TAG = GRTComboCrateScanning.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText text_zone,text_crate;
    LinearLayout llCrate;
    String zone = "";
    public GRTComboCrateScanning() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Scanning");
        if(text_crate.getText().toString().length() > 0){
            text_crate.setText("");
            text_crate.requestFocus();
        }
    }

    public static GRTComboCrateScanning newInstance(String param1, String param2) {
        GRTComboCrateScanning fragment = new GRTComboCrateScanning();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            zone = getArguments().getString(ARG_ZONE);
        }
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_combo_crate_sorting, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        text_crate = rootView.findViewById(R.id.text_combo_crate_sort_scan_crate);
        text_zone = rootView.findViewById(R.id.text_combo_crate_sort_zone);
        llCrate = rootView.findViewById(R.id.ll_combo_crate_scan_crate);

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
        text_zone.addTextChangedListener(new TextWatcher() {
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
                String zone = s.toString().toUpperCase();
                if(zone.length()>0 && scannerReading) {
                    text_zone.selectAll();
                    validateZone(zone);
                }
            }
        });

        text_zone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String zone = text_zone.getText().toString().toUpperCase();
                    if(zone.length()>0) {
                        text_zone.selectAll();
                        validateZone(zone);
                        return true;
                    }
                }
                return false;
            }
        });
        text_zone.requestFocus();
        if(zone.length() > 0){
            text_zone.setText(zone);
            text_zone.setEnabled(false);
            llCrate.setVisibility(View.VISIBLE);
            text_crate.requestFocus();
        }
        return rootView;
    }
    private void validateZone(String zone) {
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_COMBO_ZONE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_ZONE", zone.toUpperCase().trim());

            showProcessingAndSubmit(Vars.GRT_COMBO_ZONE_VALIDATE,REQUEST_VALIDATE_ZONE,args);
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
    private void validateCrate(String crate) {
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_SIN_CRATE_COMBO_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());

            showProcessingAndSubmit(Vars.GRT_SIN_CRATE_COMBO_VALIDATE,REQUEST_VALIDATE_COMBO_CRATE,args);
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

    private void moveToNextScreen(String nature,String crate,JSONObject responsebody,String matnr){
        String zone = text_zone.getText().toString().toUpperCase(Locale.ROOT).trim();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    text_crate.setText("");
                    text_zone.setText("");
                    Bundle args=new Bundle();
                    Fragment fragment = null;
                    if(nature.toUpperCase(Locale.ROOT).equals("SIN")){
                        HashSet<String> zones = new HashSet<>();
                        zones.add(zone);
                        fragment =  new GRTSinglePickCrateZoneScan();
                        args.putSerializable("zonelist", zones);
                        args.putString("crate", crate);
                        args.putString("mode", "combo");
                        args.putString("scanmode", "SIN");
                        args.putString("etdata", responsebody.toString());
                        args.putString("matnr", matnr);
                        fragment.setArguments(args);
                    }
                    else{
                        fragment = new GRTComboCrateZoneSorting();
                        args.putString("crate",crate);
                        args.putString("zone",zone);
                        args.putString("etdata", responsebody.toString());
                        fragment.setArguments(args);
                    }
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.home, fragment, "grt_combo_picking_process_menu");
                    ft.addToBackStack("grt_combo_picking_process_menu");
                    ft.commit();
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 500);
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
                                        UIFuncs.errorSound(con);
                                        if (request == REQUEST_VALIDATE_ZONE) {
                                            text_zone.setText("");
                                            text_zone.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_COMBO_CRATE) {
                                            text_zone.setEnabled(false);
                                            text_crate.setText("");
                                            text_crate.requestFocus();
                                        }
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        if (request == REQUEST_VALIDATE_ZONE) {
                                            llCrate.setVisibility(View.VISIBLE);
                                            text_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_COMBO_CRATE) {
                                            moveToNextScreen(responsebody.get("EX_NATURE").toString(),text_crate.getText().toString().toUpperCase(Locale.ROOT).trim(),responsebody,responsebody.get("EX_MATNR").toString());
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