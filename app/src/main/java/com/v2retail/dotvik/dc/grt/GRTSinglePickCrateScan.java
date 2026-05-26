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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class GRTSinglePickCrateScan extends Fragment {

    private static final int REQUEST_VALIDATE_CRATE = 5205;
    private static final String PROCESS = "process";
    private static final String CRATE = "crate";
    private static final String MODE = "mode";
    private static final String ZONE = "zone";
    private static final String TAG = GRTSinglePickCrateScan.class.getName();

    private View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText text_crate;
    Button button_next;
    TableLayout table_zone_list;

    HashSet<String> zones = new HashSet<>();

    public GRTSinglePickCrateScan() {
        // Required empty public constructor
    }
    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Crate Scan");
        text_crate.setText("");
        text_crate.requestFocus();
    }

    public static GRTSinglePickCrateScan newInstance(String param1, String param2) {
        GRTSinglePickCrateScan fragment = new GRTSinglePickCrateScan();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_single_pick_crate_scan, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        table_zone_list = rootView.findViewById(R.id.table_grt_single_pick_zone_list);
        button_next = rootView.findViewById(R.id.btn_grt_single_crate_scan_next);
        text_crate = rootView.findViewById(R.id.text_grt_single_crate_scan);

        button_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args=new Bundle();
                args.putSerializable("zonelist", zones);
                args.putString("crate", text_crate.getText().toString().toUpperCase(Locale.ROOT).trim());
                args.putString("mode", "single");
                args.putString("etdata", "");
                args.putString("matnr", "");
                Fragment fragment =  new GRTSinglePickCrateZoneScan();
                fragment.setArguments(args);
                text_crate.setText("");
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.home, fragment, "Grt_Single_Pick_Crate_Scan");
                ft.addToBackStack("Grt_Single_Pick_Crate_Scan");
                ft.commit();
            }
        });
        text_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  crate = text_crate.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        text_crate.selectAll();
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });
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
        button_next.setEnabled(false);
        text_crate.requestFocus();

        return rootView;
    }

    private void validateCrate(String crate){
        JSONObject args = new JSONObject();
        try {
            button_next.setEnabled(false);
            args.put("bapiname", Vars.GRT_SINGLE_PICK_CRATE_SIN_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase(Locale.ROOT).trim());
            zones = new HashSet<>();
            showProcessingAndSubmit(Vars.GRT_SINGLE_PICK_CRATE_SIN_VALIDATE, REQUEST_VALIDATE_CRATE, args,null);

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
    public void showProcessingAndSubmit(String rfc, int request, JSONObject args, Map.Entry<Integer, CrateSortETData> targetEtData){

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args,targetEtData);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }
    private void submitRequest(String rfc, int request, JSONObject args, Map.Entry<Integer,CrateSortETData> targetEtData){

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
                if(dialog!=null) {
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
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if(request == REQUEST_VALIDATE_CRATE){
                                            text_crate.setText("");
                                            text_crate.requestFocus();
                                        }
                                    } else {
                                        if(request == REQUEST_VALIDATE_CRATE){
                                            SetPickData(responsebody);
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

    private void SetPickData(JSONObject responsebody) {
        try
        {
            JSONArray ET_DATA = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA.getJSONObject(recordIndex);
                    zones.add(ET_RECORD.getString("ZONE_CRATE"));
                }
            }
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    public void loadData() {

        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 20;
        textSize = 15;

        int rows = zones.size();
        table_zone_list.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerZone = new TextView(getContext());

        headerSno.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        headerSno.setGravity(Gravity.CENTER);
        headerSno.setPadding(5,5,0,5);
        headerSno.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSno.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSno.setText("S.No");

        headerZone.setGravity(Gravity.CENTER);
        headerZone.setPadding(0,5,0,5);
        headerZone.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerZone.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerZone.setText("Zone");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerSno);
        tr.addView(headerZone);

        table_zone_list.addView(tr, trParams);

        //Create Data Rows in Table
        int rowNum = 1;
        if(rows > 0) {
            button_next.setEnabled(true);
            Iterator<String> zoneIterator = zones.iterator();
            while (zoneIterator.hasNext()) {
                String zone = zoneIterator.next();
                TextView tvSno = new TextView(getContext());
                tvSno.setText(rowNum + "");
                tvSno.setTextSize(textSize);
                tvSno.setPadding(5, 2, 0, 2);
                tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvZone = new TextView(getContext());
                tvZone.setText(zone);
                tvZone.setTextSize(textSize);
                tvZone.setPadding(5, 2, 0, 2);
                tvZone.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                tr = new TableRow(getContext());
                tr.setId(rowNum);
                tr.setPadding(0, 0, 0, 0);
                tr.setLayoutParams(trParams);
                tr.addView(tvSno);
                tr.addView(tvZone);
                tr.setTag(zone);
                table_zone_list.addView(tr, trParams);
                rowNum++;
            }
        }
    }
}