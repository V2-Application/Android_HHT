package com.v2retail.dotvik.dc.ptlnew.ptl40;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * PTL 4.0 — Proposed Hub/Station. Scan pallet → {@link Vars#ZWM_PTL_PLT_VAL_AT_ZONE_FL}
 * returns HUB (EX_HUB) for display only.
 */
public class FragmentPTLNewProcess40ProposedHubStation extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_PALLET = 1501;
    private static final String TAG = FragmentPTLNewProcess40ProposedHubStation.class.getName();

    View rootView;
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back;
    EditText txt_scan_pallate;
    EditText txt_hub;

    public FragmentPTLNewProcess40ProposedHubStation() {
    }

    public static FragmentPTLNewProcess40ProposedHubStation newInstance() {
        return new FragmentPTLNewProcess40ProposedHubStation();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("Proposed Hub/Station");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_process40_proposed_hub_station, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_scan_pallate = rootView.findViewById(R.id.txt_ptl_new_proposed_hub_station_scan_pallate);
        txt_hub = rootView.findViewById(R.id.txt_ptl_new_proposed_hub_station_hub);
        btn_back = rootView.findViewById(R.id.btn_ptl_new_proposed_hub_station_back);

        btn_back.setOnClickListener(this);
        addScanInputEvents();
        clearFields();
        UIFuncs.enableInput(con, txt_scan_pallate);
        txt_scan_pallate.requestFocus();
        return rootView;
    }

    private void addScanInputEvents() {
        txt_scan_pallate.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_pallate);
                if (!value.isEmpty()) {
                    validatePalette(value);
                    return true;
                }
            }
            return false;
        });
        txt_scan_pallate.addTextChangedListener(new TextWatcher() {
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
                    validatePalette(value);
                }
            }
        });
    }

    private void clearFields() {
        txt_hub.setText("");
    }

    /** Clear prior HUB before validating a new pallet scan. */
    private void prepareForNewPaletteScan(String palette) {
        clearFields();
        txt_scan_pallate.setText(palette);
    }

    private void validatePalette(String palette) {
        prepareForNewPaletteScan(palette);
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
            dismissDialog();
            box.getErrBox(e);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_ptl_new_proposed_hub_station_back) {
            box.confirmBack(fm, con);
        }
    }

    private String extractHubFromResponse(JSONObject responsebody) {
        String hub = responsebody.optString("EX_HUB", "").trim();
        if (hub.isEmpty()) {
            hub = responsebody.optString("HUB", "").trim();
        }
        return hub;
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                dismissDialog();
                box.getErrBox(e);
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        final RequestQueue mRequestQueue;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload ->" + params);

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                responsebody -> {
                    dismissDialog();
                    Log.d(TAG, "response ->" + responsebody);

                    if (responsebody == null || responsebody.equals("") || responsebody.equals("null")
                            || responsebody.equals("{}")) {
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "Unable to Connect Server/ Empty Response");
                        return;
                    }
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            String type = returnobj.optString("TYPE", "");
                            if ("E".equals(type)) {
                                UIFuncs.errorSound(con);
                                box.getBox("Err", returnobj.optString("MESSAGE", "Error"));
                                clearFields();
                                txt_scan_pallate.requestFocus();
                            } else if (request == REQUEST_VALIDATE_PALLET) {
                                txt_hub.setText(extractHubFromResponse(responsebody));
                                txt_scan_pallate.setText("");
                                txt_scan_pallate.requestFocus();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
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
                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res);
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
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    Response.ErrorListener volleyErrorListener() {
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
            box.getBox("Err", err);
        };
    }
}
