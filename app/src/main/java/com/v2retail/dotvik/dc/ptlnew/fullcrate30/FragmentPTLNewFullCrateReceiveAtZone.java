package com.v2retail.dotvik.dc.ptlnew.fullcrate30;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.v2retail.dotvik.dc.ptlnew.HubZoneCrate;
import com.v2retail.dotvik.dc.ptlnew.ZoneStation;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FragmentPTLNewFullCrateReceiveAtZone extends Fragment implements View.OnClickListener {

    private static final int REQUEST_ZONE_LIST= 1500;
    private static final int REQUEST_VALIDATE_ZONE_PALLATE = 1501;

    private static final String TAG = FragmentPTLNewFullCrateReceiveAtZone.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    Button btn_back;
    EditText txt_scan_pallate, txt_pallate, txt_scanned_pallete, txt_total_crate;

    List<String> zones = new ArrayList<String>();
    ArrayAdapter<String> zonesAdapter;
    List<String> stations = new ArrayList<String>();
    ArrayAdapter<String> stationsAdapter;

    Map<String, Map<String, ZoneStation>> hubZoneDataMap = new LinkedHashMap<>();

    boolean spinnerTouched = false;

    Spinner dd_zone_list, dd_station_list;

    LinearLayout ll_stations;

    int scannedPallete = 0;

    String title = "Receive at Zone";

    boolean isProcess40;

    public FragmentPTLNewFullCrateReceiveAtZone() {
        // Required empty public constructor
    }

    public static FragmentPTLNewFullCrateReceiveAtZone newInstance(String title) {
        FragmentPTLNewFullCrateReceiveAtZone newInstance = new FragmentPTLNewFullCrateReceiveAtZone();
        newInstance.title = title;
        newInstance.isProcess40 = title.equalsIgnoreCase("PTL - Receive at Zone");
        return newInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle(this.title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ptl_new_full_crate_receive_at_zone, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        ll_stations  = rootView.findViewById(R.id.ll_ptl_new_picking_full_crate_receive_at_zone_dd_zone_stations);

        dd_station_list = rootView.findViewById(R.id.ptl_new_picking_full_crate_receive_at_zone_dd_zone_stations);
        dd_station_list.setSelection(0);

        txt_scan_pallate = rootView.findViewById(R.id.txt_ptl_new_full_crate_receive_at_zone_scan_pallate);
        txt_pallate = rootView.findViewById(R.id.txt_ptl_new_full_crate_receive_at_zone_pallete);
        txt_scanned_pallete = rootView.findViewById(R.id.txt_ptl_new_full_crate_receive_at_zone_pallete_scanned);
        txt_total_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_receive_at_zone_crate_count);

        dd_zone_list = rootView.findViewById(R.id.ptl_new_picking_full_crate_receive_at_zone_dd_zones);
        dd_zone_list.setSelection(0);
        zonesAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, zones);
        zonesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_zone_list.setAdapter(zonesAdapter);
        dd_zone_list.setOnTouchListener((v, me) -> {spinnerTouched = true; v.performClick(); return false;});

        btn_back = rootView.findViewById(R.id.btn_ptl_new_full_crate_receive_at_zone_back);

        if(isProcess40){
            stationsAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, stations);
            stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dd_station_list.setAdapter(stationsAdapter);
            dd_station_list.setOnTouchListener((v, me) -> {spinnerTouched = true; v.performClick(); return false;});
            ll_stations.setVisibility(View.VISIBLE);
        }

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_full_crate_receive_at_zone_back:
                box.confirmBack(fm, con);
                break;
        }
    }

    private void addInputEvents(){
        btn_back.setOnClickListener(this);
        dd_station_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinnerTouched) {
                    if(dd_station_list.getSelectedItem() != null && !dd_station_list.getSelectedItem().toString().isEmpty()){
                        Map<String, ZoneStation> zoneMap = hubZoneDataMap.get(dd_station_list.getSelectedItem().toString());
                        zones.clear();
                        zones.add("Select");
                        for (Map.Entry<String, ZoneStation> zone: zoneMap.entrySet()) {
                            ZoneStation zoneCrate = zone.getValue();
                            zones.add(zoneCrate.getZone());
                        }
                        ((BaseAdapter) dd_zone_list.getAdapter()).notifyDataSetChanged();
                        dd_zone_list.setEnabled(true);
                        dd_zone_list.invalidate();
                        dd_zone_list.setSelection(0);
                        dd_zone_list.requestFocus();
                        if(zones.isEmpty()){
                            box.getBox("Not Available", "All zones are mapped with crate. Please select different station");
                        }else{
                            dd_zone_list.setEnabled(true);
                        }
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        dd_zone_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    if(dd_zone_list.getSelectedItem() != null){
                        String key = dd_zone_list.getSelectedItem().toString();
                        if(!key.isEmpty() && !key.equalsIgnoreCase("Select")){
                            UIFuncs.enableInput(con, txt_scan_pallate);
                        }else{
                            txt_scan_pallate.setText("");
                            UIFuncs.disableInput(con, txt_scan_pallate);
                        }
                    }
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        txt_scan_pallate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_pallate);
                    if (!value.isEmpty()) {
                        if(dd_zone_list.getSelectedItem() !=null && !dd_zone_list.getSelectedItem().toString().isEmpty()){
                            UIFuncs.enableInput(con, txt_scan_pallate);
                        }else{

                        }
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_pallate.addTextChangedListener(new TextWatcher() {
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
                    if(dd_zone_list.getSelectedItem() !=null && !dd_zone_list.getSelectedItem().toString().isEmpty()){
                        validateZonePallate();
                    }
                }
            }
        });
    }

    private void clear(){
        zones.clear();
        stations.clear();
        hubZoneDataMap = new LinkedHashMap<>();
        if(isProcess40){
            ll_stations.setVisibility(View.VISIBLE);
        }else{
            ll_stations.setVisibility(View.GONE);
        }
        scannedPallete = 0;
        txt_scan_pallate.setText("");
        txt_pallate.setText("");
        txt_scanned_pallete.setText("");
        txt_total_crate.setText("");
        UIFuncs.disableInput(con, txt_scan_pallate);
        getZoneList();
    }

    private void getZoneList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GET_ZONE_STATION_V3);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            showProcessingAndSubmit(Vars.ZWM_PTL_GET_ZONE_STATION_V3, REQUEST_ZONE_LIST, args);
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

    public void setZoneOrStationList(JSONObject responsebody){
        if(isProcess40){
            setStationList(responsebody);
        }else{
            setZoneList(responsebody);
        }
    }

    public void setZoneList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_ZONE");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                zones.clear();
                zones.add("Select");
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    if(!zones.contains(ET_RECORD.getString("ZZONE"))){
                        zones.add(ET_RECORD.getString("ZZONE"));
                    }
                }
                ((BaseAdapter) dd_zone_list.getAdapter()).notifyDataSetChanged();
                dd_zone_list.setEnabled(true);
                dd_zone_list.invalidate();
                dd_zone_list.setSelection(0);
                dd_zone_list.requestFocus();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void setStationList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_ZONE");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                zones.clear();
                stations.clear();
                stations.add("Select");
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    ZoneStation hubZoneCrate = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), ZoneStation.class);
                    if(hubZoneDataMap.containsKey(hubZoneCrate.getZoneStation())){
                        Map<String, ZoneStation> hubZoneMap = hubZoneDataMap.get(hubZoneCrate.getZoneStation());
                        hubZoneMap.put(hubZoneCrate.getZoneStation() + "-" + hubZoneCrate.getZone(), hubZoneCrate);
                    }else{
                        Map<String, ZoneStation> hubZoneMap = new LinkedHashMap<>();
                        hubZoneMap.put(hubZoneCrate.getZoneStation() + "-" + hubZoneCrate.getZone(), hubZoneCrate);
                        hubZoneDataMap.put(hubZoneCrate.getZoneStation(), hubZoneMap);
                        stations.add(hubZoneCrate.getZoneStation());
                    }
                }

                ((BaseAdapter) dd_station_list.getAdapter()).notifyDataSetChanged();
                dd_station_list.setEnabled(true);
                dd_station_list.invalidate();
                dd_station_list.setSelection(0);
                dd_station_list.requestFocus();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void validateZonePallate(){
        JSONObject args = new JSONObject();
        try {
            String rfc = isProcess40 ? Vars.ZWM_PTL_PLT_REC_AT_ZONE_SRT_V4 : Vars.ZWM_PTL_PLT_REC_AT_ZONE_SRT_V3;
            args.put("bapiname", rfc);
            args.put("IM_PLANT", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PALETTE", UIFuncs.toUpperTrim(txt_scan_pallate));
            if(isProcess40){
                args.put("IM_ZONE_STATION", dd_station_list.getSelectedItem().toString());
                args.put("IM_ZONE", dd_zone_list.getSelectedItem().toString());
            }else{
                args.put("IM_ZONE_STATION", dd_zone_list.getSelectedItem().toString());
            }
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_ZONE_PALLATE, args);
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
                                        if (request == REQUEST_VALIDATE_ZONE_PALLATE) {
                                            txt_scan_pallate.setText("");
                                            txt_scan_pallate.requestFocus();
                                        }
                                    } else {
                                        if(request == REQUEST_ZONE_LIST){
                                            setZoneOrStationList(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_ZONE_PALLATE) {
                                            txt_pallate.setText(UIFuncs.toUpperTrim(txt_scan_pallate));
                                            txt_scan_pallate.setText("");
                                            scannedPallete++;
                                            txt_scanned_pallete.setText(scannedPallete + "");
                                            txt_total_crate.setText(UIFuncs.removeLeadingZeros(responsebody.getString("EX_COUNT")));
                                            txt_scan_pallate.requestFocus();
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