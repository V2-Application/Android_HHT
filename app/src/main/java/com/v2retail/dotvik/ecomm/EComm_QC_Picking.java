package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EComm_QC_Picking  extends Fragment implements View.OnClickListener {

    private final String TAG = "EComm_QC_Picking";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private Activity activity;

    private View rootView;

    private Spinner spinnerZone;
    private Spinner spinnerPicklist;
    private EditText textPigionHoleBin;
    private EditText textScannedCount;
    private Button button_save;

    RecyclerView recycler_view = null;


    ProgressDialog dialog = null;

    ArrayList<String> zoneList = new ArrayList<String>();
    ArrayList<String> picklistList = new ArrayList<String>();
    JSONArray etData = null;

    String selectedZone = "";
    String selectedPicklist = "";

    HashMap<String, String> uniqueList = new HashMap<String, String>();
    JSONArray filteredPicklistPigeonHoles = null;

    PigeonHoleAdapter myAdapter = null;

    int totalScanCount = 0;
    int scanCount = 0;

    public EComm_QC_Picking() {


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ecomm_qc_picking, container, false);
        initView();

        return rootView;
    }

    void initView() {
        activity = getActivity();

        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

        spinnerZone = rootView.findViewById(R.id.id_spinner_zone);
        spinnerPicklist = rootView.findViewById(R.id.id_spinner_picklist);
        textPigionHoleBin = rootView.findViewById(R.id.text_pigeon_home_bin);
        textScannedCount = rootView.findViewById(R.id.text_scanned_count);
        recycler_view = rootView.findViewById(R.id.recycler_view);

        button_save = rootView.findViewById(R.id.button_save);

        handleClicks();
        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Ecomm QC Picking");
        }

        spinnerZone.requestFocus();

        zoneList.add("Select Zone");
        picklistList.add("Select Picklist");

        getZoneList();
    }

    void handleClicks() {
        button_save.setOnClickListener(this);
    }

    private void handlePicklistKeypadEditor() {

        spinnerZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position>0) {
                    selectedZone = zoneList.get(position);
                    getZonePickList(selectedZone);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerPicklist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position>0) {
                    selectedPicklist = picklistList.get(position);
                    spinnerPicklist.setEnabled(false);
                    spinnerZone.setEnabled(false);
                    filterPigeonHoles(selectedPicklist);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        textPigionHoleBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String putwallZoneBin = textPigionHoleBin.getText().toString();
                    if(putwallZoneBin.length()>0) {
                        validatePutwallZone(putwallZoneBin);
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });


        textPigionHoleBin.addTextChangedListener(new TextWatcher() {
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
                String putwallZoneBin = s.toString();
                if(putwallZoneBin.length()>0 && scannerReading) {
                    validatePutwallZone(putwallZoneBin);
                }
            }
        });


/*
        textPigionHoleBin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {


                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310
                            || keyEvent.getScanCode() == 311
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String putwallZoneBin = textPigionHoleBin.getText().toString();
                        if(putwallZoneBin.length()>0) {
                            validatePutwallZone(putwallZoneBin);
                            return true;
                        }
                    }
                }

                return false;
            }
        });

 */

    }

    void validatePutwallZone(String putwallZoneBin) {
        boolean retVal = false;
        for(int i=0; i<filteredPicklistPigeonHoles.length(); i++) {
            try {
                JSONObject json = filteredPicklistPigeonHoles.getJSONObject(i);
                String toMatch = json.optString("PUTWALL_BIN", "");
                if(toMatch.equals(putwallZoneBin)) {
                    String oldValue = json.optString("PICKED", "");
                    if(oldValue.equals("X")) {
                        // already scanned
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("", putwallZoneBin + " already scanned");
                    } else {
                        // matched
                        json.put("PICKED", "X");
                        retVal = true;
                        scanCount = scanCount + 1;
                        String displayStr =  scanCount + " / " + totalScanCount;
                        textScannedCount.setText(displayStr);
                        myAdapter.notifyItemChanged(i);
                    }
                    break;
                }
            } catch (JSONException jsone) {

            }
        }

        if(retVal == false) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("", putwallZoneBin + " not found");
        }
        textPigionHoleBin.setText("");
        textPigionHoleBin.requestFocus();
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.button_save) {
            // Save the scanned pigeon holes now
            savePicklist();
        }

    }

    void getZoneList() {

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendZoneRequestJson();
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
        
    }

    void sendZoneRequestJson() {
        String rfc = "ZECOM_GET_ZONE_LIST";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER.toUpperCase());

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        JSONArray jsonArray = responsebody.getJSONArray("ET_ZONE");
                                        if(jsonArray!=null && jsonArray.length()>0) {
                                            for(int i=1; i<jsonArray.length(); i++) {
                                                JSONObject json = jsonArray.getJSONObject(i);
                                                String active = json.getString("ACTIVE");  //X
                                                if(active.equals("X")) {
                                                    String zone = json.getString("ZECOM_ZONE");
                                                    zoneList.add(zone);
                                                }
                                            }

                                            String[] zlArray = new String[zoneList.size()];
                                            zlArray = zoneList.toArray(zlArray);
                                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(EComm_QC_Picking.this.getContext(), android.R.layout.simple_spinner_item, zlArray);
                                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                            spinnerZone.setAdapter(adapter);
                                        }
                                        return;
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);
        }

    }



    void getZonePickList(final String zone) {

        picklistList.clear();
        picklistList.add("Select Picklist");

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendZonePickListRequestJson(zone);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    void sendZonePickListRequestJson(String zone) {
        String rfc = "ZECOM_GET_ZONE_PICKLIST";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER.toUpperCase());
            params.put("IM_ZONE", zone.toUpperCase());

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        etData = responsebody.getJSONArray("ET_DATA");
                                        if(etData!=null && etData.length()>0) {
                                            for(int i=1; i<etData.length(); i++) {
                                                JSONObject json = etData.getJSONObject(i);
                                                String pickList = json.getString("PICKLISTNO");

                                                if(uniqueList.get(pickList)==null) {
                                                    uniqueList.put(pickList, pickList);
                                                }
                                            }
                                            picklistList.clear();

                                            picklistList.addAll(uniqueList.values());
                                            Collections.sort(picklistList);
                                            picklistList.add(0, "Select Picklist");

                                            String[] zlArray = new String[picklistList.size()];
                                            zlArray = picklistList.toArray(zlArray);
                                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(EComm_QC_Picking.this.getContext(), android.R.layout.simple_spinner_item, zlArray);
                                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                            spinnerPicklist.setAdapter(adapter);
                                        }
                                        return;
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);
        }

    }

    void filterPigeonHoles(String pickList) {
        filteredPicklistPigeonHoles = new JSONArray();
        totalScanCount = 0;
        for(int i=0; i<etData.length(); i++) {
            try {
                JSONObject json = etData.getJSONObject(i);
                String tobematch = json.getString("PICKLISTNO");
                if(tobematch.equals(pickList)) {
                    filteredPicklistPigeonHoles.put(json);
                    totalScanCount = totalScanCount + 1;
                }
            } catch (JSONException jsone) {

            }
        }
        scanCount = 0;
        textScannedCount.setText("0 / " + totalScanCount);
        textPigionHoleBin.setText("");
        textPigionHoleBin.requestFocus();

        myAdapter = new PigeonHoleAdapter();
        recycler_view.setAdapter(myAdapter);
        recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));
        myAdapter.notifyDataSetChanged();

    }


    void savePicklist() {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendScannedPickListJson();
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    void sendScannedPickListJson() {
        String rfc = "ZECOM_SAVE_ZONE_PICKING_DATA";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_USER", USER.toUpperCase());
            params.put("IM_ZONE", selectedZone);
            params.put("IM_PICKLISTNO", selectedPicklist);

            JSONArray itData = new JSONArray();
            for(int i=0; i<filteredPicklistPigeonHoles.length(); i++) {

                JSONObject json = filteredPicklistPigeonHoles.getJSONObject(i);
                if(json.getString("PICKED").equals("X")) {
                    itData.put(json);
                }
            }
            params.put("IT_DATA", itData);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

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
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        String stationName =  responsebody.getString("EX_STATIONNAME");
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Station: " + stationName, returnobj.getString("MESSAGE"));
                                        return;
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



    class PigeonHoleAdapter extends RecyclerView.Adapter<EComm_QC_Picking.ToViewHolder> {

        PigeonHoleAdapter() {

        }
        @Override
        public EComm_QC_Picking.ToViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_ecomm_tanum_display, parent, false);
            return new EComm_QC_Picking.ToViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(EComm_QC_Picking.ToViewHolder holder, int position) {
            try {
                holder.updateView(position, filteredPicklistPigeonHoles.getJSONObject(position));
            } catch(JSONException jsone) {

            }
        }

        @Override
        public int getItemCount() {
            if(filteredPicklistPigeonHoles!=null && filteredPicklistPigeonHoles.length()>0) {
                return filteredPicklistPigeonHoles.length();
            }

            return 0;
        }
    }

    class ToViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textBinNumber;
        TextView textStatus;

        public ToViewHolder(View itemView) {
            super(itemView);

            textBinNumber = itemView.findViewById(R.id.text_tanum);
            textStatus  = itemView.findViewById(R.id.text_erdat);
        }

        public void updateView(int position, JSONObject item) {

            try {
                String toMatch = item.getString("PUTWALL_BIN");
                textBinNumber.setText(toMatch);

                String oldValue = item.optString("PICKED", "");
                if(oldValue.equals("X")) {
                    textStatus.setText("X");
                } else {
                    textStatus.setText("");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onClick(View v) {
            // we need to go to the next screen
            Log.d(TAG, "onClick() view tag = " + v.getTag());
        }
    }


}
