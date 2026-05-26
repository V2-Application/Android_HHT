package com.v2retail.dotvik.dc.ptlnew.withpallate;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
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
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * PTL — Receive at FLR STA. User scans pallet → {@link Vars#ZWM_PTL_PLT_VAL_AT_ZONE_FL} returns
 * HUB, crate count, and hub station list / default station to bind on screen → {@link Vars#ZWM_PTL_PLT_REC_AT_ZONE_FL} saves.
 */
public class FragmentPTLNewReceiveAtZone extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_PALLET = 1501;
    private static final int REQUEST_SAVE = 1502;

    private static final String TAG = FragmentPTLNewReceiveAtZone.class.getName();

    View rootView;
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    List<String> stations = new ArrayList<>();
    ArrayAdapter<String> stationsAdapter;

    boolean spinnerTouched = false;
    /** True while updating the hub-station spinner from the validate RFC (avoid treating as user change). */
    boolean bindingStationFromServer = false;

    Spinner dd_hub_station;
    Button btn_back;
    Button btn_save;
    EditText txt_scan_pallate;
    EditText txt_pallate;
    EditText txt_hub;
    EditText txt_crate_in_pallate;

    /** Pallet confirmed by validate RFC; required for SAVE. */
    String validatedPalette = "";

    public FragmentPTLNewReceiveAtZone() {
    }

    public static FragmentPTLNewReceiveAtZone newInstance() {
        return new FragmentPTLNewReceiveAtZone();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("PTL- Receive at FLR STA");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_receive_at_zone, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        FragmentActivity activity = getActivity();

        dd_hub_station = rootView.findViewById(R.id.ptl_new_receive_at_zone_dd_hub_station);
        dd_hub_station.setSelection(0);

        txt_scan_pallate = rootView.findViewById(R.id.txt_ptl_new_receive_at_zone_scan_pallate);
        txt_pallate = rootView.findViewById(R.id.txt_ptl_new_receive_at_zone_pallate);
        txt_hub = rootView.findViewById(R.id.txt_ptl_new_receive_at_zone_hub);
        txt_crate_in_pallate = rootView.findViewById(R.id.txt_ptl_new_receive_at_zone_crate_in_pallate);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_receive_at_zone_back);
        btn_save = rootView.findViewById(R.id.btn_ptl_new_receive_at_zone_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        stationsAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, stations);
        stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_hub_station.setAdapter(stationsAdapter);
        dd_hub_station.setOnTouchListener((v, me) -> {
            spinnerTouched = true;
            v.performClick();
            return false;
        });

        dd_hub_station.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (bindingStationFromServer) {
                    return;
                }
                // Do NOT clear fields on hub-station selection.
                // User requested: clear only after Save success → OK click.
                spinnerTouched = false;
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
                        validatePalette(value);
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
                    validatePalette(value);
                }
            }
        });

        stations.clear();
        stations.add("Select");
        stationsAdapter.notifyDataSetChanged();
        dd_hub_station.setSelection(0);
        UIFuncs.enableInput(con, txt_scan_pallate);
        return rootView;
    }

    private boolean hasHubStationSelected() {
        if (dd_hub_station.getSelectedItem() == null) {
            return false;
        }
        String st = dd_hub_station.getSelectedItem().toString().trim();
        return !st.isEmpty() && !"Select".equalsIgnoreCase(st);
    }

    private void clearValidationFields() {
        validatedPalette = "";
        txt_scan_pallate.setText("");
        txt_pallate.setText("");
        txt_hub.setText("");
        txt_crate_in_pallate.setText("");
        bindingStationFromServer = true;
        stations.clear();
        stations.add("Select");
        stationsAdapter.notifyDataSetChanged();
        dd_hub_station.setSelection(0);
        bindingStationFromServer = false;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_ptl_new_receive_at_zone_back) {
            box.confirmBack(fm, con);
        } else if (id == R.id.btn_ptl_new_receive_at_zone_save) {
            saveReceiveAtFlrSta();
        }
    }

    /**
     * True when {@code stn} is not a real station (SAP/JSON adaptor header noise).
     */
    private static boolean isInvalidHubStationToken(String stn) {
        if (stn == null || stn.isEmpty()) {
            return true;
        }
        // Header/metadata row often exposes this literal; not a floor station.
        if ("ZZONE_STATION".equalsIgnoreCase(stn)) {
            return true;
        }
        return false;
    }

    /**
     * Hub on an ET_ST_ZONE / ET_DATA row, when SAP sends it (optional).
     */
    private static String hubFromTableRow(JSONObject row) {
        return row.optString("HUB", "").trim();
    }

    /**
     * First index to read for SAP ET_* arrays: index 0 is often a template/header row
     * (see {@link com.v2retail.dotvik.dc.ptlnew.fullcrate30.FragmentPTLNewFullCrateReceiveAtHubStation#setStationsList}).
     */
    private static int firstDataRowIndex(JSONArray et) {
        try {
            return com.v2retail.commons.SapJsonRows.startIndex(et);
        } catch (org.json.JSONException e) {
            return 0;
        }
    }

    /**
     * Fills HUB Station spinner from validate RFC.
     *
     * RFC table (per SAP): ET_ST_ZONE-ZONE_STATION
     * Fallbacks: ET_DATA-HUB_STN, exports EX_HUBSTN / EX_HUB_STN / ZONE_STATION.
     */
    private void bindHubStationFromValidateResponse(JSONObject responsebody) throws JSONException {
        List<String> list = new ArrayList<>();
        list.add("Select");

        String exHub = responsebody.optString("EX_HUB", "").trim();

        String preferred = responsebody.optString("EX_HUBSTN", "").trim();
        if (preferred.isEmpty()) {
            preferred = responsebody.optString("EX_HUB_STN", "").trim();
        }
        if (preferred.isEmpty()) {
            preferred = responsebody.optString("ZONE_STATION", "").trim();
        }
        if (isInvalidHubStationToken(preferred)) {
            preferred = "";
        }

        if (responsebody.has("ET_ST_ZONE") && responsebody.get("ET_ST_ZONE") instanceof JSONArray) {
            JSONArray et = responsebody.getJSONArray("ET_ST_ZONE");
            for (int i = firstDataRowIndex(et); i < et.length(); i++) {
                JSONObject row = et.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                if (!exHub.isEmpty()) {
                    String rowHub = hubFromTableRow(row);
                    if (!rowHub.isEmpty() && !exHub.equalsIgnoreCase(rowHub)) {
                        continue;
                    }
                }
                String stn = row.optString("ZONE_STATION", "").trim();
                if (!isInvalidHubStationToken(stn) && !list.contains(stn)) {
                    list.add(stn);
                }
            }
        }

        if (responsebody.has("ET_DATA") && responsebody.get("ET_DATA") instanceof JSONArray) {
            JSONArray et = responsebody.getJSONArray("ET_DATA");
            for (int i = firstDataRowIndex(et); i < et.length(); i++) {
                JSONObject row = et.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                if (!exHub.isEmpty()) {
                    String rowHub = hubFromTableRow(row);
                    if (!rowHub.isEmpty() && !exHub.equalsIgnoreCase(rowHub)) {
                        continue;
                    }
                }
                String stn = row.optString("HUB_STN", "").trim();
                if (stn.isEmpty()) {
                    stn = row.optString("HUBSTN", "").trim();
                }
                if (!isInvalidHubStationToken(stn) && !list.contains(stn)) {
                    list.add(stn);
                }
            }
        }

        if (!preferred.isEmpty() && !list.contains(preferred)) {
            list.add(preferred);
        }

        bindingStationFromServer = true;
        stations.clear();
        stations.addAll(list);
        stationsAdapter.notifyDataSetChanged();
        dd_hub_station.setEnabled(true);

        int selectIdx = 0;
        if (!preferred.isEmpty()) {
            int idx = stations.indexOf(preferred);
            if (idx >= 0) {
                selectIdx = idx;
            }
        } else if (stations.size() == 2) {
            selectIdx = 1;
        }
        dd_hub_station.setSelection(selectIdx);
        bindingStationFromServer = false;
    }

    private void validatePalette(String palette) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_PLT_VAL_AT_ZONE_FL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_PALETTE", palette);
            showProcessingAndSubmit(Vars.ZWM_PTL_PLT_VAL_AT_ZONE_FL, REQUEST_VALIDATE_PALLET, args);
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

    private void saveReceiveAtFlrSta() {
        if (TextUtils.isEmpty(validatedPalette)) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Validate pallet by scanning first");
            return;
        }
        if (!hasHubStationSelected()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Select HUB Station");
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_PLT_REC_AT_ZONE_FL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_PALETTE", validatedPalette);
            // Selected ZONE_STATION from dropdown
            args.put("IM_ZONE_STATION", dd_hub_station.getSelectedItem().toString());
            args.put("IM_HUB", WERKS);
            showProcessingAndSubmit(Vars.ZWM_PTL_PLT_REC_AT_ZONE_FL, REQUEST_SAVE, args);
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
        JsonObjectRequest mJsonRequest;
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
                                        if (request == REQUEST_VALIDATE_PALLET) {
                                            clearValidationFields();
                                            txt_scan_pallate.requestFocus();
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_PALLET) {
                                            validatedPalette = UIFuncs.toUpperTrim(txt_scan_pallate);
                                            txt_pallate.setText(validatedPalette);
                                            txt_hub.setText(responsebody.optString("EX_HUB", "").trim());
                                            String countRaw = "";
                                            if (responsebody.has("EX_COUNT")) {
                                                Object c = responsebody.get("EX_COUNT");
                                                countRaw = c != null ? String.valueOf(c) : "";
                                            }
                                            txt_crate_in_pallate.setText(UIFuncs.removeLeadingZeros(countRaw));
                                            try {
                                                bindHubStationFromValidateResponse(responsebody);
                                            } catch (JSONException e) {
                                                Log.e(TAG, "bindHubStationFromValidateResponse", e);
                                                bindingStationFromServer = false;
                                            }
                                            txt_scan_pallate.setText("");
                                            txt_scan_pallate.requestFocus();
                                        } else if (request == REQUEST_SAVE) {
                                            box.getBox("Ok", returnobj.optString("MESSAGE", "Saved"), (d, w) -> {
                                                clearValidationFields();
                                                txt_scan_pallate.requestFocus();
                                            });
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
                String err;

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
