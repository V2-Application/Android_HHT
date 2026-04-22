package com.v2retail.dotvik.hub.inward;

import android.app.ProgressDialog;
import android.content.Context;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * FragmentHUStockReview — HU Stock Review at Hub
 *
 * RFC: ZWM_HU_STOCK_REV_RFC
 *   IV_WERKS  — Plant (auto-filled from device SharedPreferences)
 *   IV_HU     — Handling Unit number (barcode scanned)
 *   IV_LGTYP  — Storage Type (entered)
 *   ES_RETURN — BAPIRET2 return structure
 *
 * Branch: Android_HHT_Dev | Added: 2026-04-21
 */
public class FragmentHUStockReview extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentHUStockReview.class.getName();
    private static final int REQUEST_HU_STOCK_REVIEW = 1601;

    View rootView;
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_werks;   // read-only display of plant
    EditText txt_hu;
    EditText txt_lgtyp;
    TextView txt_result_type;
    TextView txt_result_message;
    Button btn_check;
    Button btn_clear;

    public FragmentHUStockReview() {}

    public static FragmentHUStockReview newInstance() {
        return new FragmentHUStockReview();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("HU Stock Review");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_hub_stock_review, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        // Bind views
        txt_werks          = rootView.findViewById(R.id.txt_hub_stock_review_werks);
        txt_hu             = rootView.findViewById(R.id.txt_hub_stock_review_hu);
        txt_lgtyp          = rootView.findViewById(R.id.txt_hub_stock_review_lgtyp);
        txt_result_type    = rootView.findViewById(R.id.txt_hub_stock_review_result_type);
        txt_result_message = rootView.findViewById(R.id.txt_hub_stock_review_result_message);
        btn_check          = rootView.findViewById(R.id.btn_hub_stock_review_check);
        btn_clear          = rootView.findViewById(R.id.btn_hub_stock_review_clear);

        btn_check.setOnClickListener(this);
        btn_clear.setOnClickListener(this);

        // Populate read-only plant field
        txt_werks.setText(WERKS);

        addInputEvents();
        clearScreen();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_hub_stock_review_check) {
            checkHUStock();
        } else if (id == R.id.btn_hub_stock_review_clear) {
            clearScreen();
        }
    }

    private void addInputEvents() {
        // HU field: on scanner input advance to LGTYP
        txt_hu.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty() && scannerReading) {
                    txt_lgtyp.requestFocus();
                }
            }
        });
        txt_hu.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                txt_lgtyp.requestFocus();
                return true;
            }
            return false;
        });

        // LGTYP field: on scanner input auto-submit
        txt_lgtyp.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 2;
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty() && scannerReading) {
                    btn_check.performClick();
                }
            }
        });
        txt_lgtyp.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                btn_check.performClick();
                return true;
            }
            return false;
        });
    }

    private void clearScreen() {
        txt_hu.setText("");
        txt_lgtyp.setText("");
        txt_result_type.setText("");
        txt_result_message.setText("");
        txt_result_type.setVisibility(View.GONE);
        txt_result_message.setVisibility(View.GONE);
        UIFuncs.enableInput(con, txt_hu);
        txt_hu.requestFocus();
    }

    private void checkHUStock() {
        String hu    = UIFuncs.toUpperTrim(txt_hu);
        String lgtyp = UIFuncs.toUpperTrim(txt_lgtyp);

        if (hu.isEmpty()) {
            showError("Required", "Please scan or enter HU number");
            txt_hu.requestFocus();
            return;
        }
        if (lgtyp.isEmpty()) {
            showError("Required", "Please enter Storage Type (LGTYP)");
            txt_lgtyp.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_HU_STOCK_REV_RFC);
            args.put("IV_WERKS", WERKS);
            args.put("IV_HU",    hu);
            args.put("IV_LGTYP", lgtyp);
            showProcessingAndSubmit(Vars.ZWM_HU_STOCK_REV_RFC, REQUEST_HU_STOCK_REVIEW, args);
        } catch (JSONException e) {
            e.printStackTrace();
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void handleResponse(JSONObject response) {
        try {
            txt_result_type.setVisibility(View.VISIBLE);
            txt_result_message.setVisibility(View.VISIBLE);

            if (response.has("ES_RETURN") && response.get("ES_RETURN") instanceof JSONObject) {
                JSONObject ret = response.getJSONObject("ES_RETURN");
                String type    = ret.optString("TYPE", "");
                String message = ret.optString("MESSAGE", "No message returned");

                txt_result_type.setText("TYPE: " + type);
                txt_result_message.setText(message);

                if ("E".equals(type)) {
                    UIFuncs.errorSound(con);
                    txt_result_type.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    txt_result_message.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if ("S".equals(type)) {
                    txt_result_type.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    txt_result_message.setTextColor(getResources().getColor(android.R.color.black));
                } else {
                    txt_result_type.setTextColor(getResources().getColor(android.R.color.black));
                    txt_result_message.setTextColor(getResources().getColor(android.R.color.black));
                }
            } else {
                txt_result_type.setText("");
                txt_result_message.setText("No return data received from SAP");
                txt_result_message.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            box.getErrBox(e);
        }
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        new AlertBox(getContext()).getBox(title, message);
    }

    private void dismissDialog() {
        if (dialog != null) { dialog.dismiss(); dialog = null; }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Checking HU stock...");
        dialog.setCancelable(false);
        dialog.show();
        new Handler().postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                dismissDialog();
                box.getErrBox(e);
            }
        }, 500);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        final JSONObject params = args;
        Log.d(TAG, "payload -> " + params.toString());

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, params,
                response -> {
                    dismissDialog();
                    Log.d(TAG, "response -> " + response);
                    if (response == null) {
                        UIFuncs.errorSound(con);
                        box.getBox("Error", "No response from server");
                    } else {
                        handleResponse(response);
                    }
                },
                volleyErrorListener()
        ) {
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody()             { return params.toString().getBytes(); }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse r) {
                return super.parseNetworkResponse(r);
            }
        };

        req.setRetryPolicy(new RetryPolicy() {
            @Override public int getCurrentTimeout()    { return 50000; }
            @Override public int getCurrentRetryCount() { return 1; }
            @Override public void retry(VolleyError e) throws VolleyError {}
        });
        queue.add(req);
    }

    Response.ErrorListener volleyErrorListener() {
        return error -> {
            Log.i(TAG, "Error: " + error);
            String err;
            if      (error instanceof TimeoutError || error instanceof NoConnectionError) err = "Communication Error!";
            else if (error instanceof AuthFailureError) err = "Authentication Error!";
            else if (error instanceof ServerError)      err = "Server Side Error!";
            else if (error instanceof NetworkError)     err = "Network Error!";
            else if (error instanceof ParseError)       err = "Parse Error!";
            else                                        err = error.toString();
            dismissDialog();
            new AlertBox(getContext()).getBox("Error", err);
        };
    }
}
