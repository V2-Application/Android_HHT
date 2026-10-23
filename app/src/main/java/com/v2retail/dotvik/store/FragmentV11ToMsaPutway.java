package com.v2retail.dotvik.store;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.v2retail.dotvik.ho.HoProcessSelectionActivity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Store screen for V11 to MSA putaway.
 * Scan Bin validates through {@link Vars#ZWM_SAMPLE_BIN_VAL}.
 * Scan Crate validates through {@link Vars#ZWM_SAMPLE_CRATE_VAL}.
 * Scan Ean validates through {@link Vars#ZWM_SAMPLE_ART_VAL}.
 * Save posts Putaway through {@link Vars#ZWM_SAPMLE_PUTWAY_V11_H01}
 * and RePutway through {@link Vars#ZWM_SAPMLE_RE_PUTWAY_V11_H01}.
 */
public class FragmentV11ToMsaPutway extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentV11ToMsaPutway";
    private static final String TITLE = "V11 TO MSA PUTAWAY";
    private static final int REQUEST_VALIDATE_BIN = 6101;
    private static final int REQUEST_VALIDATE_CRATE = 6102;
    private static final int REQUEST_SAVE = 6103;
    private static final int REQUEST_VALIDATE_ARTICLE = 6104;

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
    private RadioGroup rgMode;
    private RadioButton rbPutaway;
    private RadioButton rbRePutway;

    private String pendingBin = "";
    private String pendingCrate = "";
    private String pendingEan = "";
    private String currentMatnr = "";
    private final Map<String, ScanItem> scannedItems = new LinkedHashMap<>();
    private int activeRequest = 0;
    private boolean binValidationInProgress = false;
    private boolean binValidated = false;
    private boolean crateValidationInProgress = false;
    private boolean crateValidated = false;
    private boolean articleValidationInProgress = false;
    private boolean articleValidated = false;
    private boolean saveInProgress = false;

    public FragmentV11ToMsaPutway() {
    }

    public static FragmentV11ToMsaPutway newInstance() {
        return new FragmentV11ToMsaPutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_v11_to_msa_putway, container, false);
        con = getContext();
        fm = getParentFragmentManager();
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtBin = view.findViewById(R.id.txt_v11_msa_putway_bin);
        txtCrate = view.findViewById(R.id.txt_v11_msa_putway_crate);
        txtEan = view.findViewById(R.id.txt_v11_msa_putway_ean);
        txtArticle = view.findViewById(R.id.txt_v11_msa_putway_article);
        txtQty = view.findViewById(R.id.txt_v11_msa_putway_qty);
        rgMode = view.findViewById(R.id.rg_v11_msa_putway_mode);
        rbPutaway = view.findViewById(R.id.rb_v11_msa_putway_putaway);
        rbRePutway = view.findViewById(R.id.rb_v11_msa_putway_reputway);
        rbPutaway.setChecked(true);
        Button btnBack = view.findViewById(R.id.btn_v11_msa_putway_back);
        Button btnReset = view.findViewById(R.id.btn_v11_msa_putway_reset);
        Button btnSave = view.findViewById(R.id.btn_v11_msa_putway_save);
        btnBack.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        addBinEvents();
        addCrateEvents();
        addEanEvents();
        clearFields();
        txtBin.requestFocus();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof HoProcessSelectionActivity) {
            ((HoProcessSelectionActivity) getActivity()).setActionBarTitle(TITLE);
        } else if (getActivity() instanceof Home_Activity) {
            ((Home_Activity) getActivity()).setActionBarTitle(TITLE);
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
        if (id == R.id.btn_v11_msa_putway_back) {
            box.confirmBack(fm, getContext());
        } else if (id == R.id.btn_v11_msa_putway_reset) {
            clearFields();
            txtBin.requestFocus();
        } else if (id == R.id.btn_v11_msa_putway_save) {
            save();
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
            String saveRfc = selectedSaveRfc();
            args.put("bapiname", saveRfc);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_BIN", pendingBin);
            args.put("IM_CRATE", pendingCrate);
            args.put("ET_DATA", etData);
            setModeEnabled(false);
            showProcessingAndSubmit(saveRfc, REQUEST_SAVE, args);
        } catch (JSONException e) {
            saveInProgress = false;
            setModeEnabled(true);
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private String selectedSaveRfc() {
        if (rbRePutway != null && rbRePutway.isChecked()) {
            return Vars.ZWM_SAPMLE_RE_PUTWAY_V11_H01;
        }
        return Vars.ZWM_SAPMLE_PUTWAY_V11_H01;
    }

    private void setModeEnabled(boolean enabled) {
        if (rgMode == null) {
            return;
        }
        rgMode.setEnabled(enabled);
        for (int i = 0; i < rgMode.getChildCount(); i++) {
            rgMode.getChildAt(i).setEnabled(enabled);
        }
    }

    private void addBinEvents() {
        txtBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterKey = event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || enterKey) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txtBin);
                    if (!value.isEmpty()) {
                        triggerBinValidation(value);
                    }
                    return true;
                }
                return false;
            }
        });

        txtBin.addTextChangedListener(new TextWatcher() {
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
                    triggerBinValidation(value);
                }
            }
        });
    }

    private void triggerBinValidation(String bin) {
        if (binValidationInProgress || binValidated || !txtBin.isEnabled()) {
            return;
        }
        validateBin(bin);
    }

    private void validateBin(String bin) {
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin.");
            txtBin.requestFocus();
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

    private void addCrateEvents() {
        txtCrate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterKey = event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || enterKey) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txtCrate);
                    if (!value.isEmpty()) {
                        triggerCrateValidation(value);
                    }
                    return true;
                }
                return false;
            }
        });

        txtCrate.addTextChangedListener(new TextWatcher() {
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
                    triggerCrateValidation(value);
                }
            }
        });
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
        validateCrate(crate);
    }

    private void validateCrate(String crate) {
        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate.");
            txtCrate.requestFocus();
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

    private void onCrateValidated(String crate) {
        crateValidationInProgress = false;
        crateValidated = true;
        txtCrate.setText(crate);
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

    private void addEanEvents() {
        txtEan.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterKey = event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || enterKey) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txtEan);
                    if (!value.isEmpty()) {
                        triggerArticleValidation(value);
                    }
                    return true;
                }
                return false;
            }
        });

        txtEan.addTextChangedListener(new TextWatcher() {
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
                    triggerArticleValidation(value);
                }
            }
        });
    }

    private void triggerArticleValidation(String ean) {
        if (articleValidationInProgress || articleValidated || !txtEan.isEnabled()) {
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
        validateArticle(ean);
    }

    private void validateArticle(String ean) {
        if (TextUtils.isEmpty(ean)) {
            box.getBox("Alert", "Please scan Ean.");
            txtEan.requestFocus();
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

    private void onArticleValidated(String ean, JSONObject responsebody) {
        currentMatnr = responsebody.optString("EX_ART", "").trim();
        ScanItem item = scannedItems.get(ean);
        if (item == null) {
            item = new ScanItem(ean, currentMatnr);
            scannedItems.put(ean, item);
        }
        item.qty++;
        txtArticle.setText(currentMatnr);
        txtQty.setText(String.valueOf(item.qty));

        articleValidationInProgress = false;
        articleValidated = false;
        pendingEan = "";
        txtEan.setText("");
        txtEan.requestFocus();
    }

    private void onArticleRejected() {
        articleValidationInProgress = false;
        articleValidated = false;
        pendingEan = "";
        currentMatnr = "";
        txtEan.setText("");
        txtEan.setEnabled(true);
        txtEan.setBackgroundResource(R.drawable.bg_input);
        txtArticle.setText("");
        txtQty.setText("");
        txtEan.requestFocus();
    }

    private void onBinValidated(String bin) {
        binValidationInProgress = false;
        binValidated = true;
        txtBin.setText(bin);
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
                onCrateValidated(pendingCrate);
            } else if (request == REQUEST_VALIDATE_ARTICLE) {
                onArticleValidated(pendingEan, responsebody);
            } else if (request == REQUEST_SAVE) {
                saveInProgress = false;
                setModeEnabled(true);
                String message = returnobj.optString("MESSAGE", "");
                if (TextUtils.isEmpty(message)) {
                    message = "Saved successfully";
                }
                box.getBox("Success", message);
                clearFields();
                txtBin.requestFocus();
            } else {
                onBinValidated(pendingBin);
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
            setModeEnabled(true);
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
        currentMatnr = "";
        scannedItems.clear();
        activeRequest = 0;
        binValidationInProgress = false;
        binValidated = false;
        crateValidationInProgress = false;
        crateValidated = false;
        articleValidationInProgress = false;
        articleValidated = false;
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
        setModeEnabled(true);
    }

    private static class ScanItem {
        final String ean;
        final String matnr;
        int qty;

        ScanItem(String ean, String matnr) {
            this.ean = ean;
            this.matnr = matnr;
        }
    }
}
