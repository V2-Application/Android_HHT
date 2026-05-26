package com.v2retail.dotvik.dc.ptl;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

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
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.adapter.GRTPickListAdapter;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickList;
import com.v2retail.dotvik.modal.grt.cratepick.ETSection;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PTLPicking#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PTLPicking extends Fragment {

    private final int REQUEST_SECTION_LIST = 301;
    private final int REQUEST_PICK_LIST = 302;

    private static final String TAG = PTLPicking.class.getName();

    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;

    private Map<String, ETSection> ET_SECTION = new HashMap<>();
    private Map<String, ETPickList> ET_PICK_LIST = new HashMap<>();
    private Map<String, JSONObject> ET_EAN_RECORDS = new HashMap<>();
    ArrayList<ETPickList> picklistitems = new ArrayList<>();

    List<String> sections = new ArrayList<String>();
    ArrayAdapter<String> sectionAdapter;
    GRTPickListAdapter pickListAdapter;

    Spinner dd_section_list;
    ListView ptl_pick_list;
    RadioButton radio_full,radio_partial;
    RadioGroup picklist_option;

    boolean spinnerTouched = false;

    public PTLPicking() {
    }
    public static PTLPicking newInstance(String param1, String param2) {
        PTLPicking fragment = new PTLPicking();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_picking, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("PTL Picking");
        }

        dd_section_list = rootView.findViewById(R.id.ptl_picking_dd_section);
        dd_section_list.setSelection(0);
        ptl_pick_list = rootView.findViewById(R.id.ptl_pick_list);
        radio_full = rootView.findViewById(R.id.ptl_picking_radio_full);
        radio_partial = rootView.findViewById(R.id.ptl_picking_radio_partial);
        picklist_option  = rootView.findViewById(R.id.ptl_picking_radio_group);

        sectionAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_section_list.setAdapter(sectionAdapter);
        dd_section_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        dd_section_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    pickListAdapter.clear();
                    if (dd_section_list.getSelectedItemPosition() > 0 && (radio_full.isChecked() || radio_partial.isChecked())) {
                        getPickList(dd_section_list.getSelectedItem().toString(),(radio_full.isChecked() ? Vars.PTL_PICK_FULL_CRATE : Vars.PTL_PICK_PARTIAL));
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        picklist_option.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.ptl_picking_radio_full:
                        if (radio_full.isChecked() && dd_section_list.getSelectedItemPosition() > 0) {
                            getPickList(dd_section_list.getSelectedItem().toString(), Vars.PTL_PICK_FULL_CRATE);
                        }
                        break;
                    case R.id.ptl_picking_radio_partial:
                        if (radio_partial.isChecked() && dd_section_list.getSelectedItemPosition() > 0) {
                            getPickList(dd_section_list.getSelectedItem().toString(), Vars.PTL_PICK_PARTIAL);
                        }
                        break;
                }
            }
        });

        pickListAdapter= new GRTPickListAdapter(getContext(),picklistitems);
        ptl_pick_list.setAdapter(pickListAdapter);
        if(sectionAdapter.getCount() == 0) {
            getSectionList();
        }

        return rootView;
    }

    private void getSectionList(){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.PTL_PICK_SECTION_LIST);
            args.put("IM_USER", USER);

            ET_SECTION = new HashMap<>();
            ET_EAN_RECORDS = new HashMap<>();
            showProcessingAndSubmit(Vars.PTL_PICK_SECTION_LIST, REQUEST_SECTION_LIST, args);

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
    private void getPickList(String section,String listMode){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.PTL_GET_PICK_LIST);
            args.put("IM_USER", USER);
            args.put("IM_NATURE", listMode);
            args.put("IM_SECTION", section);
            args.put("IM_DATE", new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));

            ET_SECTION = new HashMap<>();
            ET_EAN_RECORDS = new HashMap<>();

            showProcessingAndSubmit(Vars.PTL_GET_PICK_LIST, REQUEST_PICK_LIST, args);

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
                                    if (type.equals("S")) {
                                        if (request == REQUEST_SECTION_LIST) {
                                            SetSectionETData(responsebody);
                                        }
                                        else if (request == REQUEST_PICK_LIST) {
                                            SetPickListETData(responsebody);
                                        }
                                        return;
                                    } else {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
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

    public void SetSectionETData(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_SECTION");
            int totalEtRecords = ET_DATA_ARRAY.length() - 1;
            if(totalEtRecords > 0){
                sections.clear();
                sections.add("Select");
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    ETSection ET_DATA = new ETSection();
                    ET_DATA.setLgmandt(ET_RECORD.getString("MANDT"));
                    ET_DATA.setLglgnum(ET_RECORD.getString("LGNUM"));
                    ET_DATA.setLgzsection(ET_RECORD.getString("ZSECTION"));
                    ET_DATA.setLgmerge(ET_RECORD.getString("MERGE"));
                    ET_DATA.setLgbinmerge(ET_RECORD.getString("BIN_MERGE"));
                    ET_SECTION.put(ET_RECORD.getString("MERGE"),ET_DATA);
                    sections.add(ET_RECORD.getString("MERGE"));
                }
                ((BaseAdapter) dd_section_list.getAdapter()).notifyDataSetChanged();
                dd_section_list.invalidate();
                dd_section_list.setSelection(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    private void SetPickListETData(JSONObject responsebody) {
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length()-1;
            if(totalEtRecords > 0){
                picklistitems.clear();
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    ETPickList ET_DATA = new ETPickList();
                    ET_DATA.setLgtanum(ET_RECORD.getString("TANUM"));
                    ET_DATA.setLgerdat(ET_RECORD.getString("ERDAT"));
                    ET_DATA.setSection (dd_section_list.getSelectedItem().toString());
                    ET_DATA.setPicktype(radio_full.isChecked() ? Vars.PTL_PICK_FULL_CRATE : Vars.PTL_PICK_PARTIAL);
                    ET_DATA.setPtl(true);
                    ET_PICK_LIST.put(ET_RECORD.getString("TANUM"),ET_DATA);
                    picklistitems.add(ET_DATA);
                }
                ((BaseAdapter) ptl_pick_list.getAdapter()).notifyDataSetChanged();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
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