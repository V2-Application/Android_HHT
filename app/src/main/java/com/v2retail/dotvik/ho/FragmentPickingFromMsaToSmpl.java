package com.v2retail.dotvik.ho;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HO screen for picking from MSA to sample.
 * Scan Bin validates through {@link Vars#ZWM_SAMPLE_BIN_VAL}.
 * Scan Crate validates through {@link Vars#ZWM_SAMPLE_CRATE_VAL}.
 * Scan Ean validates through {@link Vars#ZWM_SAMPLE_ART_VAL}; each validated Ean adds 1 to the scanned quantity.
 * Save posts through {@link Vars#ZWM_SAPMLE_PICKING_H01_V11}.
 */
public class FragmentPickingFromMsaToSmpl extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentPickMsaToSmpl";
    private static final String TITLE = "PICKING FROM MSA TO (SMPL)";
    private static final int REQUEST_VALIDATE_BIN = 6201;
    private static final int REQUEST_VALIDATE_CRATE = 6202;
    private static final int REQUEST_VALIDATE_ARTICLE = 6203;
    private static final int REQUEST_SAVE = 6204;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private Drawable defaultToolbarBackground;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText txtBin;
    private EditText txtCrate;
    private EditText txtEan;
    private EditText txtArticle;
    private EditText txtQty;
    private String pendingBin = "";
    private String pendingCrate = "";
    private String pendingEan = "";
    private final Map<String, ScanItem> scannedItems = new LinkedHashMap<>();
    private int activeRequest = 0;
    private boolean binValidationInProgress = false;
    private boolean binValidated = false;
    private boolean crateValidationInProgress = false;
    private boolean crateValidated = false;
    private boolean articleValidationInProgress = false;
    private boolean saveInProgress = false;

    public FragmentPickingFromMsaToSmpl() {
    }

    public static FragmentPickingFromMsaToSmpl newInstance() {
        return new FragmentPickingFromMsaToSmpl();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_picking_from_msa_to_smpl, container, false);
        con = getContext();
        fm = getParentFragmentManager();
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtBin = view.findViewById(R.id.txt_pick_msa_smpl_bin);
        txtCrate = view.findViewById(R.id.txt_pick_msa_smpl_crate);
        txtEan = view.findViewById(R.id.txt_pick_msa_smpl_ean);
        txtArticle = view.findViewById(R.id.txt_pick_msa_smpl_article);
        txtQty = view.findViewById(R.id.txt_pick_msa_smpl_qty);
        Button btnBack = view.findViewById(R.id.btn_pick_msa_smpl_back);
        Button btnReset = view.findViewById(R.id.btn_pick_msa_smpl_reset);
        Button btnSave = view.findViewById(R.id.btn_pick_msa_smpl_save);
        btnBack.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        addScanEvents(txtBin, new ScanHandler() {
            @Override
            public void onScan(String value) {
                triggerBinValidation(value);
            }
        });
        addScanEvents(txtCrate, new ScanHandler() {
            @Override
            public void onScan(String value) {
                triggerCrateValidation(value);
            }
        });
        addScanEvents(txtEan, new ScanHandler() {
            @Override
            public void onScan(String value) {
                triggerArticleValidation(value);
            }
        });
        clearFields();
        txtBin.requestFocus();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HoProcessSelectionActivity) {
            ((HoProcessSelectionActivity) getActivity()).setActionBarTitle(TITLE);
        }
        Toolbar toolbar = getActivity() != null ? getActivity().findViewById(R.id.toolbar) : null;
        if (toolbar != null) {
            defaultToolbarBackground = toolbar.getBackground();
            toolbar.setBackgroundColor(ContextCompat.getColor(con, R.color.hoRed));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Toolbar toolbar = getActivity() != null ? getActivity().findViewById(R.id.toolbar) : null;
        if (toolbar != null && defaultToolbarBackground != null) {
            toolbar.setBackground(defaultToolbarBackground);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_pick_msa_smpl_back) {
            box.confirmBack(fm, getContext());
        } else if (id == R.id.btn_pick_msa_smpl_reset) {
            clearFields();
            txtBin.requestFocus();
        } else if (id == R.id.btn_pick_msa_smpl_save) {
            save();
        }
    }

    private interface ScanHandler {
        void onScan(String value);
    }

    private void addScanEvents(final EditText field, final ScanHandler handler) {
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterKey = event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || enterKey) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(field);
                    if (!value.isEmpty()) {
                        handler.onScan(value);
                    }
                    return true;
                }
                return false;
            }
        });

        field.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().trim().toUpperCase();
                if (!value.isEmpty() && scannerReading) {
                    handler.onScan(value);
                }
            }
        });
    }

    private void triggerBinValidation(String bin) {
        if (binValidationInProgress || binValidated || !txtBin.isEnabled()) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            binValidationInProgress = true;
            pendingBin = bin;
            args.put("bapiname", Vars.ZWM_SAMPLE_BIN_VAL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_BIN", bin);
            showProcessingAndSubmit(Vars.ZWM_SAMPLE_BIN_VAL, REQUEST_VALIDATE_BIN, args);
        } catch (JSONException e) {
            binValidationInProgress = false;
            pendingBin = "";
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void triggerCrateValidation(String crate) {
        if (crateValidationInProgress || crateValidated || !txtCrate.isEnabled()) {
            return;
        }
        if (!binValidated) {
            box.getBox("Alert", "Please scan Bin first.");
            txtCrate.setText("");
            txtBin.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            crateValidationInProgress = true;
            pendingCrate = crate;
            args.put("bapiname", Vars.ZWM_SAMPLE_CRATE_VAL);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_CRATE", crate);
            showProcessingAndSubmit(Vars.ZWM_SAMPLE_CRATE_VAL, REQUEST_VALIDATE_CRATE, args);
        } catch (JSONException e) {
            crateValidationInProgress = false;
            pendingCrate = "";
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void triggerArticleValidation(String ean) {
        if (articleValidationInProgress || !txtEan.isEnabled()) {
            return;
        }
        if (!binValidated) {
            box.getBox("Alert", "Please scan Bin first.");
            txtEan.setText("");
            txtBin.requestFocus();
            return;
        }
        if (!crateValidated) {
            box.getBox("Alert", "Please scan Crate first.");
            txtEan.setText("");
            txtCrate.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            articleValidationInProgress = true;
            pendingEan = ean;
            args.put("bapiname", Vars.ZWM_SAMPLE_ART_VAL);
            args.put("IM_USER", USER);
            args.put("IM_EAN11", ean);
            showProcessingAndSubmit(Vars.ZWM_SAMPLE_ART_VAL, REQUEST_VALIDATE_ARTICLE, args);
        } catch (JSONException e) {
            articleValidationInProgress = false;
            pendingEan = "";
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void save() {
        if (saveInProgress || binValidationInProgress || crateValidationInProgress
                || articleValidationInProgress) {
            return;
        }
        if (!binValidated || TextUtils.isEmpty(pendingBin)) {
            box.getBox("Alert", "Please scan Bin.");
            txtBin.requestFocus();
            return;
        }
        if (!crateValidated || TextUtils.isEmpty(pendingCrate)) {
            box.getBox("Alert", "Please scan Crate.");
            txtCrate.requestFocus();
            return;
        }
        if (scannedItems.isEmpty()) {
            box.getBox("Alert", "Please scan at least one article.");
            txtEan.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            saveInProgress = true;
            JSONArray etData = new JSONArray();
            for (ScanItem item : scannedItems.values()) {
                JSONObject row = new JSONObject();
                row.put("EAN11", item.ean);
                row.put("MATNR", item.matnr);
                row.put("MENGE", item.qty);
                etData.put(row);
            }
            args.put("bapiname", Vars.ZWM_SAPMLE_PICKING_H01_V11);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_BIN", pendingBin);
            args.put("IM_CRATE", pendingCrate);
            args.put("ET_DATA", etData);
            showProcessingAndSubmit(Vars.ZWM_SAPMLE_PICKING_H01_V11, REQUEST_SAVE, args);
        } catch (JSONException e) {
            saveInProgress = false;
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onBinValidated() {
        binValidationInProgress = false;
        binValidated = true;
        txtBin.setText(pendingBin);
        txtBin.setEnabled(false);
        txtBin.setBackgroundResource(R.drawable.apsw_input_readonly);
        txtCrate.requestFocus();
    }

    private void onBinRejected() {
        binValidationInProgress = false;
        binValidated = false;
        pendingBin = "";
        txtBin.setText("");
        txtBin.setEnabled(true);
        txtBin.setBackgroundResource(R.drawable.bg_input);
        txtBin.requestFocus();
    }

    private void onCrateValidated() {
        crateValidationInProgress = false;
        crateValidated = true;
        txtCrate.setText(pendingCrate);
        txtCrate.setEnabled(false);
        txtCrate.setBackgroundResource(R.drawable.apsw_input_readonly);
        txtEan.requestFocus();
    }

    private void onCrateRejected() {
        crateValidationInProgress = false;
        crateValidated = false;
        pendingCrate = "";
        txtCrate.setText("");
        txtCrate.setEnabled(true);
        txtCrate.setBackgroundResource(R.drawable.bg_input);
        txtCrate.requestFocus();
    }

    private void onArticleValidated(JSONObject responsebody) {
        articleValidationInProgress = false;
        String matnr = responsebody.optString("EX_ART", "").trim();
        ScanItem item = scannedItems.get(pendingEan);
        if (item == null) {
            item = new ScanItem(pendingEan, matnr);
            scannedItems.put(pendingEan, item);
        }
        item.qty += 1;
        txtArticle.setText(matnr);
        txtQty.setText(formatQty(totalScannedQty()));
        pendingEan = "";
        txtEan.setText("");
        txtEan.requestFocus();
    }

    private void onArticleRejected() {
        articleValidationInProgress = false;
        pendingEan = "";
        txtEan.setText("");
        txtArticle.setText("");
        txtEan.requestFocus();
    }

    private double totalScannedQty() {
        double total = 0;
        for (ScanItem item : scannedItems.values()) {
            total += item.qty;
        }
        return total;
    }

    private String formatQty(double qty) {
        return qty == Math.floor(qty) ? String.valueOf((long) qty) : String.valueOf(qty);
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        activeRequest = request;
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    dismissDialog();
                    rejectActiveRequest();
                    box.getErrBox(e);
                }
            }
        }, 300);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        if (URL == null || !URL.contains("/")) {
            dismissDialog();
            rejectActiveRequest();
            box.getBox("Err", "Gateway URL is not configured");
            return;
        }

        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "payload -> " + args);

        RequestQueue requestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, args,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        dismissDialog();
                        Log.d(TAG, "response -> " + responsebody);
                        if (responsebody == null || responsebody.length() == 0) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from Server");
                            rejectActiveRequest();
                            return;
                        }
                        handleResponse(responsebody, request);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error: " + error);
                dismissDialog();
                UIFuncs.errorSound(con);
                box.getBox("Err", volleyErrorMessage(error));
                rejectActiveRequest();
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return args.toString().getBytes();
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
        requestQueue.add(jsonRequest);
    }

    private void handleResponse(JSONObject responsebody, int request) {
        try {
            if (!(responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", "No return from server");
                rejectActiveRequest();
                return;
            }
            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
            String type = returnobj.optString("TYPE", "");
            if ("E".equals(type) || "A".equals(type)) {
                UIFuncs.errorSound(con);
                String fallback;
                if (request == REQUEST_VALIDATE_CRATE) {
                    fallback = "Crate validation failed";
                } else if (request == REQUEST_VALIDATE_ARTICLE) {
                    fallback = "Article validation failed";
                } else if (request == REQUEST_SAVE) {
                    fallback = "Save failed";
                } else {
                    fallback = "Bin validation failed";
                }
                box.getBox("Err", returnobj.optString("MESSAGE", fallback));
                rejectActiveRequest();
                return;
            }
            if (request == REQUEST_VALIDATE_CRATE) {
                onCrateValidated();
            } else if (request == REQUEST_VALIDATE_ARTICLE) {
                onArticleValidated(responsebody);
            } else if (request == REQUEST_SAVE) {
                saveInProgress = false;
                String message = returnobj.optString("MESSAGE", "");
                if (TextUtils.isEmpty(message)) {
                    message = "Saved successfully";
                }
                box.getBox("Success", message);
                clearFields();
                txtBin.requestFocus();
            } else {
                onBinValidated();
            }
        } catch (JSONException e) {
            box.getErrBox(e);
            rejectActiveRequest();
        }
    }

    private void rejectActiveRequest() {
        if (activeRequest == REQUEST_VALIDATE_CRATE) {
            onCrateRejected();
        } else if (activeRequest == REQUEST_VALIDATE_ARTICLE) {
            onArticleRejected();
        } else if (activeRequest == REQUEST_SAVE) {
            saveInProgress = false;
        } else {
            onBinRejected();
        }
    }

    private String volleyErrorMessage(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return "Communication Error!";
        } else if (error instanceof AuthFailureError) {
            return "Authentication Error!";
        } else if (error instanceof ServerError) {
            return "Server Side Error!";
        } else if (error instanceof NetworkError) {
            return "Network Error!";
        } else if (error instanceof ParseError) {
            return "Parse Error!";
        }
        return error.toString();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void clearFields() {
        pendingBin = "";
        pendingCrate = "";
        pendingEan = "";
        scannedItems.clear();
        activeRequest = 0;
        binValidationInProgress = false;
        binValidated = false;
        crateValidationInProgress = false;
        crateValidated = false;
        articleValidationInProgress = false;
        saveInProgress = false;
        txtBin.setText("");
        txtBin.setEnabled(true);
        txtBin.setBackgroundResource(R.drawable.bg_input);
        txtCrate.setText("");
        txtCrate.setEnabled(true);
        txtCrate.setBackgroundResource(R.drawable.bg_input);
        txtEan.setText("");
        txtEan.setEnabled(true);
        txtEan.setBackgroundResource(R.drawable.bg_input);
        txtArticle.setText("");
        txtQty.setText("");
    }

    private static class ScanItem {
        final String ean;
        final String matnr;
        double qty;

        ScanItem(String ean, String matnr) {
            this.ean = ean;
            this.matnr = matnr;
        }
    }
}
