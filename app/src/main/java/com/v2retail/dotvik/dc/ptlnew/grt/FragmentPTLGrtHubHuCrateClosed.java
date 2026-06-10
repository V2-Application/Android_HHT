package com.v2retail.dotvik.dc.ptlnew.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * PTL-GRT — HUB HU/Crate Closed.
 * Crate close: {@link Vars#ZWM_PTL_GRT_CRATE_CLOSE} — IM_USER, IM_PLANT, IM_FLOOR, IM_CRATE → EX_RETURN.
 */
public class FragmentPTLGrtHubHuCrateClosed extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentPTLGrtHubHuCrateClosed.class.getSimpleName();
    private static final String ACTION_BAR_TITLE = "HUB SORTING";
    private static final List<String> FLOOR_OPTIONS = Arrays.asList("0", "1", "2", "3", "4", "5");
    private static final int REQUEST_CLOSE_CRATE = 5921;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private Spinner ddFloor;
    private EditText txtScanCrate;
    private EditText txtCrate;
    private TextView txtMessage;
    private Button btnBack;

    private boolean floorSelected = false;

    public FragmentPTLGrtHubHuCrateClosed() {
    }

    public static FragmentPTLGrtHubHuCrateClosed newInstance() {
        return new FragmentPTLGrtHubHuCrateClosed();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ptl_grt_hub_hu_crate_closed, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        ddFloor = root.findViewById(R.id.dd_ptl_grt_hub_hu_crate_closed_floor);
        txtScanCrate = root.findViewById(R.id.txt_ptl_grt_hub_hu_crate_closed_scan_crate);
        txtCrate = root.findViewById(R.id.txt_ptl_grt_hub_hu_crate_closed_crate);
        txtMessage = root.findViewById(R.id.txt_ptl_grt_hub_hu_crate_closed_message);
        btnBack = root.findViewById(R.id.btn_ptl_grt_hub_hu_crate_closed_back);

        setupFloorDropdown();
        addCrateScanEvents();
        btnBack.setOnClickListener(this);
        resetScreen();

        return root;
    }

    private void setupFloorDropdown() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_list_item_1,
                FLOOR_OPTIONS);
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ddFloor.setAdapter(floorAdapter);
        ddFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                floorSelected = true;
                UIFuncs.enableInput(con, txtScanCrate);
                txtCrate.setText("");
                txtScanCrate.setText("");
                txtMessage.setText("");
                txtScanCrate.requestFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                floorSelected = false;
                UIFuncs.disableInput(con, txtScanCrate);
            }
        });
    }

    private void addCrateScanEvents() {
        txtScanCrate.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtScanCrate);
                if (!TextUtils.isEmpty(scanned)) {
                    requestCrateClose(scanned);
                }
                return true;
            }
            return false;
        });

        txtScanCrate.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    requestCrateClose(value);
                }
            }
        });
    }

    private String getSelectedFloor() {
        Object selected = ddFloor.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void requestCrateClose(String scannedCrate) {
        if (TextUtils.isEmpty(scannedCrate)) {
            return;
        }
        if (!floorSelected || TextUtils.isEmpty(getSelectedFloor())) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please select Floor Number first.");
            txtScanCrate.setText("");
            ddFloor.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_GRT_CRATE_CLOSE);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_FLOOR", getSelectedFloor());
            args.put("IM_CRATE", scannedCrate);
            showProcessingAndSubmit(Vars.ZWM_PTL_GRT_CRATE_CLOSE, REQUEST_CLOSE_CRATE, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestCrateClose", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                dismissDialog();
                box.getErrBox(e);
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload -> " + params);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                responsebody -> {
                    dismissDialog();
                    Log.d(TAG, "response -> " + responsebody);

                    if (responsebody == null) {
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "No response from Server");
                    } else if (responsebody.length() == 0) {
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    } else {
                        handleRfcResponse(responsebody, request);
                    }
                },
                volleyErrorListener()) {
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
                Log.d(TAG, "Network response -> " + res);
                return res;
            }
        };

        jsonRequest.setRetryPolicy(new RetryPolicy() {
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
        queue.add(jsonRequest);
    }

    private void handleRfcResponse(JSONObject responsebody, int request) {
        try {
            if (!responsebody.has("EX_RETURN") || !(responsebody.get("EX_RETURN") instanceof JSONObject)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", "Invalid response (EX_RETURN missing)");
                return;
            }

            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
            String type = returnobj.optString("TYPE", "");
            String message = returnobj.optString("MESSAGE", "");
            displayMessage(message);

            if ("E".equals(type)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", message);
                clearScanFieldAndRefocus();
                return;
            }

            if (request == REQUEST_CLOSE_CRATE) {
                if (!TextUtils.isEmpty(message)) {
                    box.getBox("Success", message);
                }
                txtCrate.setText(UIFuncs.toUpperTrim(txtScanCrate));
                clearScanFieldAndRefocus();
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleRfcResponse", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void displayMessage(String message) {
        if (txtMessage != null) {
            txtMessage.setText(message == null ? "" : message);
        }
    }

    private void clearScanFieldAndRefocus() {
        txtScanCrate.setText("");
        txtScanCrate.requestFocus();
    }

    private void resetScreen() {
        displayMessage("");
        txtCrate.setText("");
        txtScanCrate.setText("");
        if (ddFloor.getAdapter() != null && ddFloor.getAdapter().getCount() > 0) {
            ddFloor.setSelection(0);
            floorSelected = true;
            UIFuncs.enableInput(con, txtScanCrate);
        } else {
            floorSelected = false;
            UIFuncs.disableInput(con, txtScanCrate);
        }
        txtScanCrate.post(() -> txtScanCrate.requestFocus());
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private Response.ErrorListener volleyErrorListener() {
        return error -> {
            Log.i(TAG, "Error :" + error);
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
            dismissDialog();
            UIFuncs.errorSound(con);
            displayMessage(err);
            box.getBox("Err", err);
            clearScanFieldAndRefocus();
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle(ACTION_BAR_TITLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_ptl_grt_hub_hu_crate_closed_back) {
            fm.popBackStack();
        }
    }
}
