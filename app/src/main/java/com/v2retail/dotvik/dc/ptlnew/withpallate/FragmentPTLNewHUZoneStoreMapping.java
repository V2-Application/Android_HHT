package com.v2retail.dotvik.dc.ptlnew.withpallate;

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

public class FragmentPTLNewHUZoneStoreMapping extends Fragment implements View.OnClickListener {

    private static final int REQUEST_ZONE_LIST = 1500;
    private static final int REQUEST_VALIDATE_STORE = 1501;
    private static final int REQUEST_VALIDATE_EXT_HU = 1502;
    private static final int REQUEST_GET_HUB_STATIONS = 1503;

    private static final String TAG = FragmentPTLNewHUZoneStoreMapping.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    List<String> zones = new ArrayList<String>();
    ArrayAdapter<String> zoneAdapter;
    List<String> stations = new ArrayList<String>();
    ArrayAdapter<String> stationsAdapter;
    Map<String, Map<String, ZoneStation>> hubZoneDataMap = new LinkedHashMap<>();

    boolean spinnerTouched = false;

    Spinner dd_zone_list, dd_station_list;
    Button btn_back;
    EditText txt_scan_store, txt_store, txt_scan_ext_hu, txt_ext_hu;
    LinearLayout ll_stations;

    String mode = null;
    public FragmentPTLNewHUZoneStoreMapping() {
    }

    public static FragmentPTLNewHUZoneStoreMapping newInstance(String mode) {
        FragmentPTLNewHUZoneStoreMapping fragment = new FragmentPTLNewHUZoneStoreMapping();
        fragment.mode = mode;
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
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL HU/Zone/Store Mapping");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_hu_zone_store_mapping, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        FragmentActivity activity = getActivity();

        ll_stations  = rootView.findViewById(R.id.ll_ptl_new_hu_zone_store_mapping_dd_stations);

        dd_station_list = rootView.findViewById(R.id.ptl_new_hu_zone_store_mapping_dd_stations);
        dd_station_list.setSelection(0);
        dd_zone_list = rootView.findViewById(R.id.ptl_new_hu_zone_store_mapping_dd_zone);
        dd_zone_list.setSelection(0);

        txt_scan_store = rootView.findViewById(R.id.txt_ptl_new_hu_zone_store_mapping_scan_store);
        txt_store = rootView.findViewById(R.id.txt_ptl_new_hu_zone_store_mapping_store);
        txt_scan_ext_hu = rootView.findViewById(R.id.txt_ptl_new_hu_zone_store_mapping_scan_ext_hu);
        txt_ext_hu = rootView.findViewById(R.id.txt_ptl_new_hu_zone_store_mapping_ext_hu);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_hu_zone_store_mapping_back);

        zoneAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, zones);
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_zone_list.setAdapter(zoneAdapter);
        dd_zone_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        if("full-crate".equalsIgnoreCase(this.mode)){
            stationsAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, stations);
            stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dd_station_list.setAdapter(stationsAdapter);
            dd_station_list.setOnTouchListener((v, me) -> {spinnerTouched = true; v.performClick(); return false;});
            ll_stations.setVisibility(View.VISIBLE);
        }

        btn_back.setOnClickListener(this);

        addInputEvents();
        clear();

        return rootView;
    }

    private void getStationsList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GET_ZONE_STATION_V3);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            showProcessingAndSubmit(Vars.ZWM_PTL_GET_ZONE_STATION_V3, REQUEST_GET_HUB_STATIONS, args);
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
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_ZONE");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ptl_new_hu_zone_store_mapping_back:
                box.confirmBack(fm, con);
                break;
        }
    }

    private void addInputEvents() {
        dd_zone_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinnerTouched) {
                    if(dd_zone_list.getSelectedItem() != null && !dd_zone_list.getSelectedItem().toString().isEmpty()) {
                        UIFuncs.enableInput(con, txt_scan_store);
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

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

        txt_scan_store.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_store);
                    if (!value.isEmpty()) {
                        validateStore(value);
                        return true;
                    }
                }
                return false;
            }
        });

        txt_scan_store.addTextChangedListener(new TextWatcher() {
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
                    validateStore(value);
                }
            }
        });

        txt_scan_ext_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_ext_hu);
                    if (!value.isEmpty()) {
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
                if (!value.isEmpty() && scannerReading) {
                    validateExtHU(value);
                }
            }
        });
    }

    private void clear(){
        UIFuncs.disableInput(con, txt_scan_ext_hu);
        dd_zone_list.setEnabled(false);
        UIFuncs.disableInput(con, txt_scan_store);
        txt_scan_ext_hu.setText("");
        txt_ext_hu.setText("");
        txt_scan_store.setText("");
        txt_store.setText("");
        if(!"full-crate".equalsIgnoreCase(this.mode)){
            getZoneList();
        }else{
            dd_zone_list.setEnabled(false);
            getStationsList();
        }
    }

    private void getZoneList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GET_ZONE);
            args.put("IM_USER", USER);
            args.put("IM_ZONE", null);
            showProcessingAndSubmit(Vars.ZWM_PTL_GET_ZONE, REQUEST_ZONE_LIST, args);
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

    private void validateStore(String value){
        JSONObject args = new JSONObject();
        try {
            String rfc = "full-crate".equalsIgnoreCase(this.mode) ? Vars.ZWM_PTL_VALIDATE_STORE_V3 : Vars.ZWM_PTL_VALIDATE_STORE;
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_ZONE", dd_zone_list.getSelectedItem().toString());
            args.put("IM_STORE", value);
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_STORE , args);
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

    private void validateExtHU(String value){
        JSONObject args = new JSONObject();
        try {
            String rfc = "full-crate".equalsIgnoreCase(this.mode) ? Vars.ZWM_PTL_VALIDATE_HU_V3 : Vars.ZWM_PTL_VALIDATE_STORE;
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_ZONE", dd_zone_list.getSelectedItem().toString());
            args.put("IM_STORE", UIFuncs.toUpperTrim(txt_store));
            args.put("IM_HU", value);
            if("full-crate".equalsIgnoreCase(this.mode)){
                args.put("IM_ZONE_ST", dd_station_list.getSelectedItem().toString());
            }
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_EXT_HU, args);
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
                    zones.add(ET_RECORD.getString("ZONE"));
                }
                ((BaseAdapter) dd_zone_list.getAdapter()).notifyDataSetChanged();
                dd_zone_list.setEnabled(true);
                dd_zone_list.invalidate();
                dd_zone_list.setSelection(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
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
                                if (type != null) {
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_STORE) {
                                            txt_scan_store.setText("");
                                            txt_scan_store.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_EXT_HU) {
                                            txt_scan_ext_hu.setText("");
                                            txt_scan_ext_hu.requestFocus();
                                        }
                                    } else {
                                        if(request == REQUEST_GET_HUB_STATIONS){
                                            setStationsList(responsebody);
                                        }
                                        else if(request == REQUEST_ZONE_LIST){
                                            setZoneList(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_STORE) {
                                            txt_store.setText(UIFuncs.toUpperTrim(txt_scan_store));
                                            txt_scan_store.setText("");
                                            txt_scan_ext_hu.setText("");
                                            txt_ext_hu.setText("");
                                            UIFuncs.enableInput(con, txt_scan_ext_hu);
                                        }
                                        else if (request == REQUEST_VALIDATE_EXT_HU) {
                                            txt_ext_hu.setText(UIFuncs.toUpperTrim(txt_scan_ext_hu));
                                            txt_scan_ext_hu.setText("");
                                            UIFuncs.enableInput(con, txt_scan_store);
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