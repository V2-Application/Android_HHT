package com.v2retail.dotvik.dc.ptlnew.withpallate;

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
import android.widget.Toast;

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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONException;
import org.json.JSONObject;

public class FragmentPTLNewHUCloseAndPrint extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_EXT_HU = 1501;

    private static final String TAG = FragmentPTLNewHUCloseAndPrint.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back;
    EditText txt_scan_ext_hu, txt_ext_hu, txt_printer;
    LinearLayout ll_printer;

    String process = null;
    String tvsprinter = null;
    SharedPreferencesData data;
    private boolean huRequestInFlight = false;

    public FragmentPTLNewHUCloseAndPrint() {
    }

    public static FragmentPTLNewHUCloseAndPrint newInstance(String process) {
        FragmentPTLNewHUCloseAndPrint fragment = new FragmentPTLNewHUCloseAndPrint();
        fragment.process = process;
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
        if("msa_binwise".equalsIgnoreCase(this.process)){
            ((Process_Selection_Activity) getActivity()).setActionBarTitle("External HU Print");
        }else{
            ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL-" + this.process);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_hu_close_and_print, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        data = new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        ll_printer = rootView.findViewById(R.id.ll_ptl_new_hu_close_tvs_printer);
        
        txt_scan_ext_hu = rootView.findViewById(R.id.txt_ptl_new_hu_close_scan_ext_hu);
        txt_ext_hu = rootView.findViewById(R.id.txt_ptl_new_hu_close_ext_hu);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_hu_close_back);

        btn_back.setOnClickListener(this);

        txt_scan_ext_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_ext_hu);
                    if (!value.isEmpty() && !huRequestInFlight) {
                        validateExtHU(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_ext_hu.addTextChangedListener(new TextWatcher() {
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
                if (!value.isEmpty() && scannerReading && !huRequestInFlight) {
                    scannerReading = false;
                    validateExtHU(value);
                }
            }
        });

        if(this.process.equalsIgnoreCase("HU Close")){
            UIFuncs.enableInput(con, txt_scan_ext_hu);
            ll_printer.setVisibility(View.GONE);
        }else{
            txt_printer = rootView.findViewById(R.id.txt_ptl_new_hu_close_printer);

            ll_printer.setVisibility(View.VISIBLE);

            String defaultPrinter = data.read(Vars.TVS_PRINTER);
            if(defaultPrinter != null && !defaultPrinter.isEmpty()){
                txt_printer.setText(defaultPrinter);
                validatePrinter(defaultPrinter, true);
            }else{
                txt_printer.requestFocus();
            }

            txt_printer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        UIFuncs.hideKeyboard(getActivity());
                        String value = UIFuncs.toUpperTrim(txt_printer);
                        if (value.length() > 0) {
                            validatePrinter(value, false);
                            return true;
                        }
                    }
                    return false;
                }
            });
            txt_printer.addTextChangedListener(new TextWatcher() {
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
                        validatePrinter(value, false);
                    }
                }
            });
        }
        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_hu_close_back:
                box.confirmBack(fm, con);
                break;
        }
    }

    private void validatePrinter(String printerName, boolean isSilentCheck){
        TSPLPrinter printerHelper = new TSPLPrinter(con);
        if(!printerHelper.findBluetoothPrinter(printerName, false)){
            if(!isSilentCheck){
                box.getBox("Not Paired", "Scanned printer ( "+ printerName +" ) is not paired with this device.");
            }
            this.tvsprinter = "xxxxxxxx";
            txt_printer.setText("");
            txt_printer.requestFocus();
            UIFuncs.disableInput(con, txt_scan_ext_hu);
            return;
        }
        data.write(Vars.TVS_PRINTER, printerName);
        this.tvsprinter = printerName;
        UIFuncs.enableInput(con, txt_scan_ext_hu);
    }

    private void validateExtHU(String hu){
        if (huRequestInFlight) {
            return;
        }
        huRequestInFlight = true;
        JSONObject args = new JSONObject();
        boolean isClose = this.process.equalsIgnoreCase("HU Close");
        try {
            String rfc = isClose ? Vars.ZWM_PTL_HU_VALIDATE_CLOSE : ("msa_binwise".equalsIgnoreCase(this.process) ? Vars.ZWM_PTL_TVS_HU_PRINT_2 : Vars.ZWM_PTL_TVS_HU_PRINT);
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            if(isClose){
                args.put("IM_PLANT", WERKS);
                args.put("IM_HU", hu);
                args.put("IM_HU_CLOSED", "X");
            }else{
                args.put("IM_EXIDV", hu);
            }
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_EXT_HU, args);
        } catch (JSONException e) {
            huRequestInFlight = false;
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

    private void printHU(JSONObject huObj){
        TSPLPrinter printer = new TSPLPrinter(getContext(), Vars.PTL_NEW_MODULE_HU_CLOSE);
        printer.sendPrintCommandToBluetoothPrinter(this.tvsprinter, huObj, "1");
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

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
                    huRequestInFlight = false;
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
                huRequestInFlight = false;
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
                                        if (request == REQUEST_VALIDATE_EXT_HU) {
                                            txt_scan_ext_hu.setText("");
                                            txt_ext_hu.setText("");
                                            txt_scan_ext_hu.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_EXT_HU) {
                                            txt_ext_hu.setText(UIFuncs.toUpperTrim(txt_scan_ext_hu));
                                            txt_scan_ext_hu.setText("");
                                            txt_scan_ext_hu.requestFocus();
                                            if(!process.equalsIgnoreCase("HU Close")){
                                                Toast.makeText(con, "Details sent to printer " + tvsprinter, Toast.LENGTH_SHORT).show();
                                                printHU(responsebody.getJSONObject("EX_HUDATA"));
                                            }
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
                huRequestInFlight = false;

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