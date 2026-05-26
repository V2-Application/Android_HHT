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
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FragmentPTLNewFullCrateHubStnCrateZoneMapping extends Fragment implements View.OnClickListener {
    private static final int REQUEST_STATIONS_LIST = 1500;
    private static final int REQUEST_VALIDATE_CRATE = 1501;

    private static final String TAG = FragmentPTLNewFullCrateHubStnCrateZoneMapping.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    FragmentActivity activity;

    List<String> stations = new ArrayList<String>();
    ArrayAdapter<String> stationsAdapter;
    List<String> zones = new ArrayList<String>();
    ArrayAdapter<String> zonesAdapter;

    boolean spinnerTouched = false;

    Spinner dd_station_list, dd_zone_list;
    Button btn_back;
    EditText txt_scan_crate, txt_crate;

    Map<String, Map<String, HubZoneCrate>> hubZoneDataMap = new LinkedHashMap<>();
    HubZoneCrate selectedHubZone = new HubZoneCrate();

    public FragmentPTLNewFullCrateHubStnCrateZoneMapping() {
        // Required empty public constructor
    }

    public static FragmentPTLNewFullCrateHubStnCrateZoneMapping newInstance() {
        return new FragmentPTLNewFullCrateHubStnCrateZoneMapping();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("HUB STN/Crate/Zone Mapping");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ptl_new_full_crate_hub_stn_crate_zone_mapping, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        txt_scan_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_station_crate_zone_map_scan_crate);
        txt_crate = rootView.findViewById(R.id.txt_ptl_new_full_crate_station_crate_zone_map_crate);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_full_crate_station_crate_zone_map_back);

        dd_station_list = rootView.findViewById(R.id.ptl_new_picking_full_crate_station_crate_zone_map_dd_stations);
        dd_station_list.setSelection(0);
        dd_zone_list = rootView.findViewById(R.id.ptl_new_picking_full_crate_station_crate_zone_map_dd_zones);
        dd_zone_list.setSelection(0);

        stationsAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, stations);
        stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_station_list.setAdapter(stationsAdapter);
        dd_station_list.setOnTouchListener((v, me) -> {spinnerTouched = true; v.performClick(); return false;});

        zonesAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, zones);
        zonesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_zone_list.setAdapter(zonesAdapter);
        dd_zone_list.setOnTouchListener((v, me) -> {spinnerTouched = true; v.performClick(); return false;});

        btn_back.setOnClickListener(this);

        clear();
        addInputEvents();


        return rootView;
    }

    private void addInputEvents(){
        dd_station_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    if(dd_station_list.getSelectedItem() != null){
                        String station = dd_station_list.getSelectedItem().toString();
                        if(!station.isEmpty()){
                            getZonesList(station);
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
                        if(!key.isEmpty()){
                            String hub = dd_station_list.getSelectedItem().toString();
                            Map<String, HubZoneCrate> hubZones = hubZoneDataMap.get(hub);
                            if(hubZones != null && !hubZones.isEmpty()){
                                selectedHubZone = hubZones.get(hub + "-" + key);
                                txt_scan_crate.setText("");
                                txt_crate.setText("");
                                UIFuncs.enableInput(con, txt_scan_crate);
                            }else{
                                box.getBox("Zone Not Available", "All zones are mapped with crate. Please select different station");
                            }
                        }
                    }
                    spinnerTouched = false;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        txt_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_crate);
                    if (!value.isEmpty()) {
                        validateCrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_crate.addTextChangedListener(new TextWatcher() {
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
                    validateCrate(value);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_full_crate_station_crate_zone_map_back:
                box.confirmBack(fm, con);
                break;
        }
    }

    private void clear(){
        stations.clear();
        zones.clear();
        hubZoneDataMap.clear();
        txt_scan_crate.setText("");
        txt_crate.setText("");
        UIFuncs.disableInput(con, txt_scan_crate);
        dd_zone_list.setEnabled(false);
        getStationsList();
    }

    private void getStationsList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HUBSTN_DATA_RFC_V3);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            showProcessingAndSubmit(Vars.ZWM_PTL_HUBSTN_DATA_RFC_V3, REQUEST_STATIONS_LIST, args);
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

    public void setStationsList(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                stations.clear();
                stations.add("Select");
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    HubZoneCrate hubZoneCrate = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(recordIndex).toString(), HubZoneCrate.class);
                    if(hubZoneDataMap.containsKey(hubZoneCrate.getHubStation())){
                        Map<String, HubZoneCrate> hubZoneMap = hubZoneDataMap.get(hubZoneCrate.getHubStation());
                        hubZoneMap.put(hubZoneCrate.getHubStation() + "-" + hubZoneCrate.getZoneCrate(), hubZoneCrate);
                    }else{
                        Map<String, HubZoneCrate> hubZoneMap = new LinkedHashMap<>();
                        hubZoneMap.put(hubZoneCrate.getHubStation() + "-" + hubZoneCrate.getZoneCrate(), hubZoneCrate);
                        hubZoneDataMap.put(hubZoneCrate.getHubStation(), hubZoneMap);
                        stations.add(hubZoneCrate.getHubStation());
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

    private void getZonesList(String station){
        Map<String, HubZoneCrate> zoneMap = hubZoneDataMap.get(station);
        zones.clear();
        zones.add("Select");
        for (Map.Entry<String, HubZoneCrate> zone: zoneMap.entrySet()) {
            HubZoneCrate zoneCrate = zone.getValue();
            if(zoneCrate.getCrate() == null || zone.getValue().getCrate().isEmpty()){
                zones.add(zone.getValue().getZoneCrate());
            }
        }
        ((BaseAdapter) dd_zone_list.getAdapter()).notifyDataSetChanged();
        dd_zone_list.setEnabled(true);
        dd_zone_list.invalidate();
        dd_zone_list.setSelection(0);
        dd_zone_list.requestFocus();
        if(zones.isEmpty()){
            box.getBox("Not Available", "All zones are mapped with crate. Please select different station");
        }
    }

    private void validateCrate(String crate){
        selectedHubZone.setCrate(crate);
        JSONObject args = new JSONObject();
        try {
            String rfc = Vars.ZWM_PTL_CRT_TAG_VAL_RFC_V3;
            args.put("bapiname", rfc);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);

            //Preparing IT_DATA
            JSONArray arrScanData = new JSONArray();
            String scanDataJsonString = new Gson().toJson(selectedHubZone);
            JSONObject itDataJson = new JSONObject(scanDataJsonString);
            arrScanData.put(itDataJson);

            args.put("IT_DATA", arrScanData);
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_CRATE, args);
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

    private void afterSave(JSONObject responsebody){
        txt_crate.setText(UIFuncs.toUpperTrim(txt_scan_crate));
        txt_scan_crate.setText("");
        UIFuncs.disableInput(con, txt_scan_crate);
        getZonesList(dd_zone_list.getSelectedItem().toString());
        selectedHubZone = new HubZoneCrate();
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
                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            txt_scan_crate.setText("");
                                            txt_scan_crate.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_STATIONS_LIST) {
                                            setStationsList(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_CRATE) {
                                            afterSave(responsebody);
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