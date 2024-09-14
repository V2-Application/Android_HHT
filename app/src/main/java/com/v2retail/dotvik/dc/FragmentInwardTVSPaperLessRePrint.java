package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.v2retail.dotvik.store.FragmentStoreDisplayInternalIRODToIRODTransfer;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Narayanan
 * @version 11.79
 * {@code Author: Narayanan, Revision: 1, Created: 12th Sep 2024, Modified: 12th Sep 2024}
 */
public class FragmentInwardTVSPaperLessRePrint extends Fragment implements View.OnClickListener {

    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    String TAG = FragmentInwardTVSPaperLessRePrint.class.getName();
    private static final int REQUEST_SAVE = 5403;
    String URL;
    String WERKS;
    String USER;
    private static String parent;
    Button btn_back, btn_reset;
    EditText txt_printer, txt_hu;
    SharedPreferencesData data;
    String title;
    String tvsprinter;

    public FragmentInwardTVSPaperLessRePrint() {
        // Required empty public constructor
    }

    public static FragmentInwardTVSPaperLessRePrint newInstance(String breadcrumb) {
        FragmentInwardTVSPaperLessRePrint fragment = new FragmentInwardTVSPaperLessRePrint();
        fragment.title  = breadcrumb;
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .getSupportActionBar().setTitle(UIFuncs.getSmallTitle(title + " > RE-PRINT"));
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
        view = inflater.inflate(R.layout.fragment_inward_tvs_paperless_reprint, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_printer = view.findViewById(R.id.txt_outward_tvs_paperless_reprint_printer);
        txt_hu = view.findViewById(R.id.txt_outward_tvs_paperless_reprint_huno);

        btn_back = view.findViewById(R.id.btn_outward_tvs_paperless_reprint_back);
        btn_reset = view.findViewById(R.id.btn_outward_tvs_paperless_reprint_reset);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);

        clear();
        addInputEvents();

        TSPLPrinter printerHelper = new TSPLPrinter(con);
        String defaultrPrinter = data.read(Vars.TVS_PRINTER);
        if(defaultrPrinter != null && defaultrPrinter.length() > 0){
            if(printerHelper.findBluetoothPrinter(defaultrPrinter, false)){
                String printerName = data.read(Vars.TVS_PRINTER);
                this.tvsprinter = printerName;
                txt_printer.setText(printerName);
                validatePrinter(printerName);
            }
        }

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_outward_tvs_paperless_reprint_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_outward_tvs_paperless_reprint_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
        }
    }

    private void addInputEvents() {
        txt_printer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_printer);
                    if (value.length() > 0) {
                        validatePrinter(value);
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
                    validatePrinter(value);
                }
            }
        });

        txt_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_hu);
                    if (value.length() > 0) {
                        validateHU(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_hu.addTextChangedListener(new TextWatcher() {
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
                    validateHU(value);
                }
            }
        });
    }

    private void clear() {
        txt_printer.setText(data.read(Vars.TVS_PRINTER));
        txt_hu.setText("");
        UIFuncs.disableInput(con, txt_hu);
        txt_printer.requestFocus();
    }

    private void validatePrinter(String printerName){
        TSPLPrinter printerHelper = new TSPLPrinter(con);
        if(!printerHelper.findBluetoothPrinter(printerName, false)){
            box.getBox("Not Paired", "Scanned printer ( "+ printerName +" ) is not paired with this device.");
            this.tvsprinter = "xxxxxxxx";
            txt_printer.setText("");
            txt_printer.requestFocus();
            UIFuncs.disableInput(con, txt_hu);
            return;
        }
        data.write(Vars.TVS_PRINTER, printerName);
        this.tvsprinter = printerName;
        txt_hu.setText("");
        UIFuncs.enableInput(con, txt_hu);
    }

    private void validateHU(String huno) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PRINT_HU_TVS);
            args.put("IM_USER", USER);
            args.put("IM_EXIDV", huno);
            showProcessingAndSubmit(Vars.ZWM_PRINT_HU_TVS, REQUEST_SAVE, args);
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

    private void printHu(JSONObject huObj) {
        TSPLPrinter printer = new TSPLPrinter(getContext());
        printer.sendPrintCommandToBluetoothPrinter(this.tvsprinter, huObj);
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
                                        if (request == REQUEST_SAVE) {
                                            txt_hu.setText("");
                                            txt_hu.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_SAVE) {
                                            txt_hu.setText("");
                                            txt_hu.requestFocus();
                                            Toast.makeText(con, "Details sent to printer "+tvsprinter, Toast.LENGTH_SHORT).show();
                                            printHu(responsebody.getJSONObject("EX_HUDATA"));
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