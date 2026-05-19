package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.adapter.GRTPickListAdapter;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickList;
import com.v2retail.dotvik.modal.grt.cratepick.ETSection;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GRTCratePickingProcess#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRTCratePickingProcess extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final int REQUEST_SECTION_LIST = 200;
    private static final int REQUEST_PICK_LIST = 201;
    private static final String TAG = GRTCratePickingProcess.class.getName();

    private static String fragment_code="GRTCPP";

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

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

    List<String> sections = new ArrayList<String>();
    ArrayList<ETPickList> picklistitems = new ArrayList<>();
    ArrayAdapter<String> sectionAdapter;
    GRTPickListAdapter pickListAdapter;

    boolean spinnerTouched = false;
    Spinner section_list;
    ListView grt_pick_list;


    public GRTCratePickingProcess() {
        // Required empty public constructor
    }


    public static GRTCratePickingProcess newInstance(String param1, String param2) {
        GRTCratePickingProcess fragment = new GRTCratePickingProcess();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRT Crate Picking  ");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        fm = getParentFragmentManager();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_crate_picking_process, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        section_list = rootView.findViewById(R.id.dd_section);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("GRT Crate Picking");
        }

        sectionAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        section_list.setAdapter(sectionAdapter);
        section_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});
        section_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinnerTouched) {
                    pickListAdapter.clear();
                    if (section_list.getSelectedItemPosition() > 0) {
                        getPickList(section_list.getSelectedItem().toString());
                    }
                    spinnerTouched = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        pickListAdapter= new GRTPickListAdapter(getContext(),picklistitems);
        grt_pick_list = rootView.findViewById(R.id.grt_pick_list);
        grt_pick_list.setAdapter(pickListAdapter);
        getSectionList();
        return rootView;
    }

    @Override
    public void onClick(View view) {

    }

    //CUSTOM FUNCTIONS
    private void getSectionList(){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_CRATE_PICK_SECTION_LIST);
            args.put("IM_USER", USER);

            ET_SECTION = new HashMap<>();
            ET_EAN_RECORDS = new HashMap<>();

            showProcessingAndSubmit(Vars.GRT_CRATE_PICK_SECTION_LIST, REQUEST_SECTION_LIST, args);

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
    //GET PICK LIST
    private void getPickList(String section){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_CRATE_PICK_LIST);
            args.put("IM_USER", USER);
            args.put("IM_NATURE", "F");
            args.put("IM_SECTION", section);

            ET_SECTION = new HashMap<>();
            ET_EAN_RECORDS = new HashMap<>();
            showProcessingAndSubmit(Vars.GRT_CRATE_PICK_LIST, REQUEST_PICK_LIST, args);

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
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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
                                        return;
                                    } else {

                                        if (request == REQUEST_SECTION_LIST) {
                                            SetSectionETData(responsebody);
                                        }
                                        else if (request == REQUEST_PICK_LIST) {
                                            SetPickListETData(responsebody);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                        // Some gateway responses omit EX_RETURN or use a different shape; still parse tables.
                        if (request == REQUEST_SECTION_LIST && responsebody.has("ET_SECTION")
                                && responsebody.get("ET_SECTION") instanceof JSONArray) {
                            SetSectionETData(responsebody);
                            return;
                        }
                        if (request == REQUEST_PICK_LIST && responsebody.has("ET_DATA")
                                && responsebody.get("ET_DATA") instanceof JSONArray) {
                            SetPickListETData(responsebody);
                            return;
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

    /**
     * SAP sometimes returns a dummy/header row as ET_DATA[0]. Older code always skipped index 0,
     * which hid the list when only one real TO row existed (common for a single pending picklist).
     */
    private static boolean isGrtPickListHeaderRow(JSONObject row) {
        if (row == null) return true;
        String tanum = row.optString("TANUM", "").trim();
        String erdat = row.optString("ERDAT", "").trim();
        if ("TANUM".equalsIgnoreCase(tanum)) return true;
        if ("TRANSFER ORDER".equalsIgnoreCase(tanum) || "TO NUMBER".equalsIgnoreCase(tanum)) return true;
        if ("ERDAT".equalsIgnoreCase(erdat) && (tanum.isEmpty() || "TANUM".equalsIgnoreCase(tanum))) return true;
        return false;
    }

    private static int pickListDataStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() <= 1) return 0;
        return isGrtPickListHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
    }

    private void SetPickListETData(JSONObject responsebody) {
        try {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            picklistitems.clear();
            ET_PICK_LIST.clear();

            int start = pickListDataStartIndex(ET_DATA_ARRAY);
            String section = section_list.getSelectedItem() != null
                    ? section_list.getSelectedItem().toString() : "";

            for (int i = start; i < ET_DATA_ARRAY.length(); i++) {
                JSONObject ET_RECORD = ET_DATA_ARRAY.getJSONObject(i);
                if (isGrtPickListHeaderRow(ET_RECORD)) continue;
                String tanum = ET_RECORD.optString("TANUM", "").trim();
                if (tanum.isEmpty()) continue;

                ETPickList ET_DATA = new ETPickList();
                ET_DATA.setLgtanum(tanum);
                ET_DATA.setLgerdat(ET_RECORD.optString("ERDAT", ""));
                ET_DATA.setSection(section);
                ET_PICK_LIST.put(tanum, ET_DATA);
                picklistitems.add(ET_DATA);
            }

            ((BaseAdapter) grt_pick_list.getAdapter()).notifyDataSetChanged();
            if (picklistitems.isEmpty()) {
                new AlertBox(getContext()).getBox("No pick lists",
                        "No transfer orders found for this section.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private static boolean isGrtSectionHeaderRow(JSONObject row) {
        if (row == null) return true;
        String merge = row.optString("MERGE", "").trim();
        if ("MERGE".equalsIgnoreCase(merge) || "SECTION".equalsIgnoreCase(merge)) return true;
        return false;
    }

    private static int sectionDataStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() <= 1) return 0;
        return isGrtSectionHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
    }

    public void SetSectionETData(JSONObject responsebody) {
        try {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_SECTION");
            int start = sectionDataStartIndex(ET_DATA_ARRAY);
            sections.clear();
            sections.add("Select");
            for (int i = start; i < ET_DATA_ARRAY.length(); i++) {
                JSONObject ET_RECORD = ET_DATA_ARRAY.getJSONObject(i);
                if (isGrtSectionHeaderRow(ET_RECORD)) continue;
                String merge = ET_RECORD.optString("MERGE", "").trim();
                if (merge.isEmpty()) continue;
                ETSection ET_DATA = new ETSection();
                ET_DATA.setLgmandt(ET_RECORD.optString("MANDT", ""));
                ET_DATA.setLglgnum(ET_RECORD.optString("LGNUM", ""));
                ET_DATA.setLgzsection(ET_RECORD.optString("ZSECTION", ""));
                ET_DATA.setLgmerge(merge);
                ET_DATA.setLgbinmerge(ET_RECORD.optString("BIN_MERGE", ""));
                ET_SECTION.put(merge, ET_DATA);
                sections.add(merge);
            }
            if (sections.size() <= 1) {
                new AlertBox(getContext()).getBox("No sections", "No warehouse sections returned from server.");
                return;
            }
            ((BaseAdapter) section_list.getAdapter()).notifyDataSetChanged();
            section_list.invalidate();
            section_list.setSelection(0);
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}