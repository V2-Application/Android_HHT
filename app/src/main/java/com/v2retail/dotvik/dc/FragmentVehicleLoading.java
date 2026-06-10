package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentVehicleLoading extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentVehicleLoading.class.getSimpleName();
    private static final int REQUEST_TRANSPORTER_LIST = 1600;
    private static final int REQUEST_HUB_STORE_LIST = 1601;
    private static final int REQUEST_HU_SELECTION = 1602;

    public static final String ARG_PLANT = "plant";
    public static final String ARG_VEHICLE_NO = "vehicle_no";
    public static final String ARG_DESTINATION = "destination";
    public static final String ARG_HUB = "hub";
    public static final String ARG_STORE = "store";
    public static final String ARG_TRANSPORTER = "transporter";
    public static final String ARG_TRANSPORTER_NAME = "transporter_name";
    public static final String ARG_DRIVER_NAME = "driver_name";
    public static final String ARG_DRIVER_MOB = "driver_mob";
    public static final String ARG_SEAL_NO = "seal_no";
    public static final String ARG_HU_LIST = "hu_list";

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";
    private String lastValidatedHub = "";

    private EditText txtPlant;
    private EditText txtVehicleNo;
    private EditText txtHub;
    private EditText txtStore;
    private EditText txtDriverName;
    private EditText txtDriverMob;
    private EditText txtSealNo;
    private RadioGroup rgDestination;
    private RadioButton rbHub;
    private RadioButton rbStore;
    private Spinner spTransporter;
    private TextView tvTransporterName;
    private Button btnBack;
    private Button btnNext;

    private final List<TransporterItem> transporterItems = new ArrayList<>();
    private ArrayAdapter<TransporterItem> transporterAdapter;

    public FragmentVehicleLoading() {
    }

    public static FragmentVehicleLoading newInstance() {
        return new FragmentVehicleLoading();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Vehicle Loading");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_vehicle_loading, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtPlant = rootView.findViewById(R.id.txt_vehicle_loading_plant);
        txtVehicleNo = rootView.findViewById(R.id.txt_vehicle_loading_vehicle_no);
        txtHub = rootView.findViewById(R.id.txt_vehicle_loading_hub);
        txtStore = rootView.findViewById(R.id.txt_vehicle_loading_store);
        txtDriverName = rootView.findViewById(R.id.txt_vehicle_loading_driver_name);
        txtDriverMob = rootView.findViewById(R.id.txt_vehicle_loading_driver_mob);
        txtSealNo = rootView.findViewById(R.id.txt_vehicle_loading_seal_no);
        rgDestination = rootView.findViewById(R.id.rg_vehicle_loading_destination);
        rbHub = rootView.findViewById(R.id.rb_vehicle_loading_hub);
        rbStore = rootView.findViewById(R.id.rb_vehicle_loading_store);
        spTransporter = rootView.findViewById(R.id.sp_vehicle_loading_transporter);
        tvTransporterName = rootView.findViewById(R.id.tv_vehicle_loading_transporter_name);
        btnBack = rootView.findViewById(R.id.btn_vehicle_loading_back);
        btnNext = rootView.findViewById(R.id.btn_vehicle_loading_next);

        btnBack.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        txtPlant.setText(WERKS);
        setupTransporterSpinner();
        setupDestinationToggle();
        setupHubInputEvents();
        loadTransporterList();
        txtVehicleNo.requestFocus();

        return rootView;
    }

    private void setupTransporterSpinner() {
        transporterItems.clear();
        transporterItems.add(TransporterItem.placeholder());
        transporterAdapter = new ArrayAdapter<TransporterItem>(
                con, android.R.layout.simple_spinner_item, transporterItems) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                setLabel(view, transporterItems.get(position));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                setLabel(view, transporterItems.get(position));
                return view;
            }

            private void setLabel(View view, TransporterItem item) {
                if (view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setText(item.getVendorLabel());
                }
            }
        };
        transporterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTransporter.setAdapter(transporterAdapter);
        spTransporter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTransporterName(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateTransporterName(0);
            }
        });
    }

    private void updateTransporterName(int position) {
        if (position < 0 || position >= transporterItems.size()) {
            tvTransporterName.setVisibility(View.GONE);
            tvTransporterName.setText("");
            return;
        }
        TransporterItem item = transporterItems.get(position);
        if (item.isPlaceholder() || item.name.isEmpty()) {
            tvTransporterName.setVisibility(View.GONE);
            tvTransporterName.setText("");
        } else {
            tvTransporterName.setText(item.name);
            tvTransporterName.setVisibility(View.VISIBLE);
        }
    }

    private void setupDestinationToggle() {
        updateDestinationFields();
        rgDestination.setOnCheckedChangeListener((group, checkedId) -> updateDestinationFields());
    }

    private void updateDestinationFields() {
        if (rbHub.isChecked()) {
            UIFuncs.enableInput(con, txtHub);
            UIFuncs.disableInput(con, txtStore);
            txtHub.setText("");
            txtStore.setText("");
            lastValidatedHub = "";
            txtStore.setBackgroundResource(R.drawable.border_disabled_input);
        } else {
            UIFuncs.disableInput(con, txtHub);
            UIFuncs.enableInput(con, txtStore);
            txtHub.setText("");
            txtStore.setText("");
            lastValidatedHub = "";
            txtStore.setBackgroundResource(R.drawable.border);
        }
    }

    private void setupHubInputEvents() {
        txtHub.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                UIFuncs.hideKeyboard(getActivity());
                String hub = UIFuncs.toUpperTrim(txtHub);
                if (!hub.isEmpty() && isHubDestination()) {
                    loadHubStoreList(hub);
                    return true;
                }
            }
            return false;
        });

        txtHub.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hub = s.toString().toUpperCase().trim();
                if (!hub.equals(lastValidatedHub)) {
                    txtStore.setText("");
                }
                if (!hub.isEmpty() && scannerReading && isHubDestination()) {
                    loadHubStoreList(hub);
                }
            }
        });
    }

    private void loadHubStoreList(String hub) {
        hub = hub.toUpperCase().trim();
        if (hub.isEmpty() || !isHubDestination()) {
            return;
        }
        if (hub.equals(lastValidatedHub) && !UIFuncs.toUpperTrim(txtStore).isEmpty()) {
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_HUBWISE_STORE_LIST_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HUB", hub);
            showProcessingAndSubmit(Vars.ZWM_HUBWISE_STORE_LIST_RFC, REQUEST_HUB_STORE_LIST, args);
        } catch (JSONException e) {
            box.getErrBox(e);
        }
    }

    private void setHubStoreData(JSONObject responseBody, String hub) {
        try {
            List<String> stores = new ArrayList<>();
            if (responseBody.has("ET_STORES")
                    && responseBody.get("ET_STORES") instanceof JSONArray) {
                JSONArray arr = responseBody.getJSONArray("ET_STORES");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.getJSONObject(i);
                    String storeCode = row.optString("PLAN", "").trim();
                    if (storeCode.isEmpty()) {
                        storeCode = row.optString("PLANT", "").trim();
                    }
                    if (!storeCode.isEmpty()) {
                        stores.add(storeCode);
                    }
                }
            }

            if (stores.isEmpty()) {
                lastValidatedHub = "";
                txtStore.setText("");
                showError("No Data", "No stores found for Hub " + hub);
                txtHub.requestFocus();
                return;
            }

            StringBuilder storeText = new StringBuilder();
            for (int i = 0; i < stores.size(); i++) {
                if (i > 0) {
                    storeText.append(", ");
                }
                storeText.append(stores.get(i));
            }

            lastValidatedHub = hub;
            txtStore.setText(storeText.toString());
            txtStore.setBackgroundResource(R.drawable.border_disabled_input);
        } catch (Exception e) {
            lastValidatedHub = "";
            txtStore.setText("");
            box.getErrBox(e);
        }
    }

    private boolean isEtErrorSuccess(JSONObject responseBody) throws JSONException {
        if (!responseBody.has("ET_ERROR")) {
            return true;
        }
        Object etError = responseBody.get("ET_ERROR");
        if (etError instanceof JSONObject) {
            String errorNumber = ((JSONObject) etError).optString("NUMBER", "").trim();
            return errorNumber.isEmpty() || "000".equals(errorNumber) || "0".equals(errorNumber);
        }
        String errorCode = String.valueOf(etError).trim();
        return errorCode.isEmpty() || "000".equals(errorCode) || "0".equals(errorCode);
    }

    private String getEtErrorMessage(JSONObject responseBody) throws JSONException {
        if (responseBody.has("ET_ERROR") && responseBody.get("ET_ERROR") instanceof JSONObject) {
            JSONObject etError = responseBody.getJSONObject("ET_ERROR");
            String message = etError.optString("MESSAGE", "").trim();
            if (!message.isEmpty()) {
                return message;
            }
            String number = etError.optString("NUMBER", "").trim();
            if (!number.isEmpty()) {
                return "Error: " + number;
            }
        }
        return "Unable to load store list.";
    }

    private void loadTransporterList() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_TRANSPORTER_DETAILS_RFC);
            showProcessingAndSubmit(Vars.ZWM_TRANSPORTER_DETAILS_RFC, REQUEST_TRANSPORTER_LIST, args);
        } catch (JSONException e) {
            box.getErrBox(e);
        }
    }

    private void setTransporterData(JSONObject responseBody) {
        try {
            transporterItems.clear();
            transporterItems.add(TransporterItem.placeholder());

            if (responseBody.has("ET_TRANSPORT_DET")
                    && responseBody.get("ET_TRANSPORT_DET") instanceof JSONArray) {
                JSONArray arr = responseBody.getJSONArray("ET_TRANSPORT_DET");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.getJSONObject(i);
                    String vendor = UIFuncs.removeLeadingZeros(row.optString("VENDOR", "").trim());
                    String name = row.optString("NAME1", "").trim();
                    if (!vendor.isEmpty()) {
                        transporterItems.add(new TransporterItem(vendor, name));
                    }
                }
            }

            transporterAdapter.notifyDataSetChanged();
            spTransporter.setSelection(0);
            updateTransporterName(0);
        } catch (Exception e) {
            box.getErrBox(e);
        }
    }

    private boolean isHubDestination() {
        return rbHub.isChecked();
    }

    private TransporterItem getSelectedTransporterItem() {
        int position = spTransporter.getSelectedItemPosition();
        if (position < 0 || position >= transporterItems.size()) {
            return TransporterItem.placeholder();
        }
        return transporterItems.get(position);
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        box.getBox(title, message);
    }

    private boolean validateForm() {
        if (UIFuncs.toUpperTrim(txtVehicleNo).isEmpty()) {
            showError("Required", "Please enter Vehicle No.");
            txtVehicleNo.requestFocus();
            return false;
        }
        if (isHubDestination()) {
            String hub = UIFuncs.toUpperTrim(txtHub);
            if (hub.isEmpty()) {
                showError("Required", "Please enter Hub.");
                txtHub.requestFocus();
                return false;
            }
            if (!hub.equals(lastValidatedHub) || UIFuncs.toUpperTrim(txtStore).isEmpty()) {
                loadHubStoreList(hub);
                showError("Required", "Please wait for store list to load for the entered Hub.");
                return false;
            }
        } else if (UIFuncs.toUpperTrim(txtStore).isEmpty()) {
            showError("Required", "Please enter Store.");
            txtStore.requestFocus();
            return false;
        }
        if (getSelectedTransporterItem().isPlaceholder()) {
            showError("Required", "Please select Transporter.");
            spTransporter.requestFocus();
            return false;
        }
        if (UIFuncs.toUpperTrim(txtDriverName).isEmpty()) {
            showError("Required", "Please enter Driver Name.");
            txtDriverName.requestFocus();
            return false;
        }
        if (UIFuncs.toUpperTrim(txtDriverMob).isEmpty()) {
            showError("Required", "Please enter Driver Mobile No.");
            txtDriverMob.requestFocus();
            return false;
        }
        if (UIFuncs.toUpperTrim(txtSealNo).isEmpty()) {
            showError("Required", "Please enter Seal No.");
            txtSealNo.requestFocus();
            return false;
        }
        return true;
    }

    private void submitHuSelection() {
        TransporterItem transporter = getSelectedTransporterItem();
        JSONObject args = new JSONObject();
        try {
            boolean hubDest = isHubDestination();
            args.put("bapiname", Vars.ZWM_HU_SELECTION_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_VEH", UIFuncs.toUpperTrim(txtVehicleNo));
            args.put("IM_TRANSPORT_CODE", transporter.vendor);
            args.put("IM_SEAL_NO", UIFuncs.toUpperTrim(txtSealNo));
            args.put("IM_DRIVER_NAME", UIFuncs.toUpperTrim(txtDriverName));
            args.put("IM_DRIVER_MOB", UIFuncs.toUpperTrim(txtDriverMob));
            args.put("IM_HUB_FLAG", hubDest ? "X" : "");
            args.put("IM_STORE_FLAG", hubDest ? "0" : "X");
            args.put("IM_HUB", hubDest ? UIFuncs.toUpperTrim(txtHub) : "");
            args.put("IM_GRP", "");
            args.put("STORE_LIST", buildStoreListArray());
            showProcessingAndSubmit(Vars.ZWM_HU_SELECTION_RFC, REQUEST_HU_SELECTION, args);
        } catch (JSONException e) {
            box.getErrBox(e);
        }
    }

    private JSONArray buildStoreListArray() throws JSONException {
        JSONArray storeList = new JSONArray();
        String storeText = UIFuncs.toUpperTrim(txtStore);
        if (storeText.isEmpty()) {
            return storeList;
        }
        String[] stores = storeText.split(",");
        for (String store : stores) {
            String code = store.trim();
            if (!code.isEmpty()) {
                JSONObject row = new JSONObject();
                row.put("STORE", code);
                storeList.put(row);
            }
        }
        return storeList;
    }

    private void openScanScreen(JSONArray huList) {
        TransporterItem transporter = getSelectedTransporterItem();
        Bundle args = new Bundle();
        args.putString(ARG_PLANT, UIFuncs.toUpperTrim(txtPlant));
        args.putString(ARG_VEHICLE_NO, UIFuncs.toUpperTrim(txtVehicleNo));
        args.putString(ARG_DESTINATION, isHubDestination() ? "HUB" : "STORE");
        args.putString(ARG_HUB, UIFuncs.toUpperTrim(txtHub));
        args.putString(ARG_STORE, UIFuncs.toUpperTrim(txtStore));
        args.putString(ARG_TRANSPORTER, transporter.vendor);
        args.putString(ARG_TRANSPORTER_NAME, transporter.name);
        args.putString(ARG_DRIVER_NAME, UIFuncs.toUpperTrim(txtDriverName));
        args.putString(ARG_DRIVER_MOB, UIFuncs.toUpperTrim(txtDriverMob));
        args.putString(ARG_SEAL_NO, UIFuncs.toUpperTrim(txtSealNo));
        args.putString(ARG_HU_LIST, huList.toString());

        Fragment fragment = FragmentVehicleLoadingScan.newInstance(args);
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(this);
        ft.add(R.id.home, fragment);
        ft.addToBackStack("vehicle_loading_scan");
        ft.commit();
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                box.getErrBox(e);
            }
        }, 500);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        final JSONObject params = args;

        Log.d(TAG, "payload -> " + params);

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, responseBody -> {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Log.d(TAG, "response -> " + responseBody);

            if (responseBody == null || responseBody.length() == 0) {
                UIFuncs.errorSound(con);
                box.getBox("Err", "No response from Server");
                return;
            }

            try {
                if (!isEtErrorSuccess(responseBody)) {
                    UIFuncs.errorSound(con);
                    if (request == REQUEST_HUB_STORE_LIST) {
                        lastValidatedHub = "";
                        txtStore.setText("");
                        txtHub.requestFocus();
                    }
                    box.getBox("Err", getEtErrorMessage(responseBody));
                    return;
                }

                if (responseBody.has("EX_RETURN") && responseBody.get("EX_RETURN") instanceof JSONObject) {
                    JSONObject returnObj = responseBody.getJSONObject("EX_RETURN");
                    if ("E".equals(returnObj.optString("TYPE", ""))) {
                        UIFuncs.errorSound(con);
                        if (request == REQUEST_HUB_STORE_LIST) {
                            lastValidatedHub = "";
                            txtStore.setText("");
                            txtHub.requestFocus();
                        }
                        box.getBox("Err", returnObj.optString("MESSAGE", "Error"));
                        return;
                    }
                }

                if (request == REQUEST_TRANSPORTER_LIST) {
                    setTransporterData(responseBody);
                } else if (request == REQUEST_HUB_STORE_LIST) {
                    setHubStoreData(responseBody, args.optString("IM_HUB", ""));
                } else if (request == REQUEST_HU_SELECTION) {
                    JSONArray huList = responseBody.optJSONArray("ET_HULIST");
                    if (huList == null || huList.length() == 0) {
                        UIFuncs.errorSound(con);
                        box.getBox("No Data", "No HU found for the given selection.");
                        return;
                    }
                    openScanScreen(huList);
                }
            } catch (JSONException e) {
                box.getErrBox(e);
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
                return super.parseNetworkResponse(response);
            }
        };

        mRequestQueue.add(mJsonRequest);
    }

    private Response.ErrorListener volleyErrorListener() {
        return error -> {
            Log.i(TAG, "Error: " + error);
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
            } else {
                err = error.toString();
            }

            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            box.getBox("Err", err);
        };
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_vehicle_loading_back) {
            box.confirmBack(fm, con);
        } else if (view.getId() == R.id.btn_vehicle_loading_next) {
            if (validateForm()) {
                submitHuSelection();
            }
        }
    }

    static class TransporterItem {
        final String vendor;
        final String name;

        TransporterItem(String vendor, String name) {
            this.vendor = vendor == null ? "" : vendor;
            this.name = name == null ? "" : name;
        }

        static TransporterItem placeholder() {
            return new TransporterItem("", "SELECT");
        }

        boolean isPlaceholder() {
            return vendor.isEmpty();
        }

        String getVendorLabel() {
            return isPlaceholder() ? "SELECT" : vendor;
        }
    }
}
