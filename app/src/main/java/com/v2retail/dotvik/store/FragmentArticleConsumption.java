package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import java.nio.charset.StandardCharsets;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class FragmentArticleConsumption extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentArticleConsumption.class.getName();

    View rootView;
    Context con;
    AlertBox box;
    FragmentManager fm;
    ProgressDialog dialog;

    String URL = "";
    String WERKS = "";
    String USER = "";

    // Conversion factor (EA per PAC) for the resolved article. Auto-populated from material.
    double packSize = 1;
    double stkQty = 0;
    String scannedEan11 = "";
    String sapMatnr = "";
    String validatedPackMeins = "PAK";
    boolean barcodeValidateInProgress = false;
    boolean saveInProgress = false;

    private static final String DEFAULT_LGORT = "0001";

    EditText txt_store, txt_barcode, txt_article, txt_scan_qty;
    TextView txt_scanned_barcode;
    LinearLayout btn_single, btn_pack;
    TextView txt_scan_qty_uom, txt_pack_label, txt_uom_note, txt_total_qty;
    boolean isSingleUom = true;
    Button btn_back, btn_save, btn_qty_minus, btn_qty_plus;

    public FragmentArticleConsumption() {
    }

    public static FragmentArticleConsumption newInstance() {
        return new FragmentArticleConsumption();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Article Consumption");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_article_consumption, container, false);

        con = getContext();
        box = new AlertBox(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = rootView.findViewById(R.id.txt_article_consumption_store);
        txt_barcode = rootView.findViewById(R.id.txt_article_consumption_barcode);
        txt_scanned_barcode = rootView.findViewById(R.id.txt_article_consumption_scanned_barcode);
        txt_article = rootView.findViewById(R.id.txt_article_consumption_article);
        txt_scan_qty = rootView.findViewById(R.id.txt_article_consumption_scan_qty);
        txt_scan_qty_uom = rootView.findViewById(R.id.txt_article_consumption_scan_qty_uom);
        btn_qty_minus = rootView.findViewById(R.id.btn_article_consumption_scan_qty_minus);
        btn_qty_plus = rootView.findViewById(R.id.btn_article_consumption_scan_qty_plus);
        btn_single = rootView.findViewById(R.id.btn_article_consumption_single);
        btn_pack = rootView.findViewById(R.id.btn_article_consumption_pack);
        txt_pack_label = rootView.findViewById(R.id.txt_article_consumption_pack_label);
        txt_uom_note = rootView.findViewById(R.id.txt_article_consumption_uom_note);
        txt_total_qty = rootView.findViewById(R.id.txt_article_consumption_total_qty);

        btn_back = rootView.findViewById(R.id.btn_article_consumption_back);
        btn_save = rootView.findViewById(R.id.btn_article_consumption_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        btn_single.setOnClickListener(this);
        btn_pack.setOnClickListener(this);
        btn_qty_minus.setOnClickListener(this);
        btn_qty_plus.setOnClickListener(this);

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_article_consumption_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_article_consumption_save:
                save();
                break;
            case R.id.btn_article_consumption_single:
                selectUom(true);
                break;
            case R.id.btn_article_consumption_pack:
                selectUom(false);
                break;
            case R.id.btn_article_consumption_scan_qty_minus:
                adjustScanQty(-1);
                break;
            case R.id.btn_article_consumption_scan_qty_plus:
                adjustScanQty(1);
                break;
        }
    }

    private void selectUom(boolean single) {
        isSingleUom = single;
        btn_single.setBackgroundResource(single
                ? R.drawable.bg_uom_option_selected
                : R.drawable.bg_uom_option_unselected);
        btn_pack.setBackgroundResource(single
                ? R.drawable.bg_uom_option_unselected
                : R.drawable.bg_uom_option_selected);
        updateScanQtyUomLabel();
        recalcTotal();
        enforceStockLimit(true);
    }

    private void updateScanQtyUomLabel() {
        txt_scan_qty_uom.setText(isSingleUom ? "(in EA)" : "(in PAC)");
    }

    private void setQtyControlsEnabled(boolean enabled) {
        btn_qty_minus.setEnabled(enabled);
        btn_qty_plus.setEnabled(enabled);
        txt_scan_qty.setEnabled(enabled);
        txt_scan_qty.setFocusable(enabled);
        txt_scan_qty.setFocusableInTouchMode(enabled);
    }

    private int getScanQty() {
        return (int) Util.convertStringToDouble(UIFuncs.toUpperTrim(txt_scan_qty));
    }

    private void setScanQty(int qty) {
        txt_scan_qty.setText(String.valueOf(Math.max(0, qty)));
        recalcTotal();
    }

    private void normalizeScanQtyInput() {
        if (sapMatnr.isEmpty()) {
            return;
        }
        int qty = getScanQty();
        if (qty < 1) {
            setScanQty(1);
        } else {
            enforceStockLimit(true);
            recalcTotal();
        }
    }

    private void adjustScanQty(int delta) {
        if (sapMatnr.isEmpty()) {
            return;
        }
        int qty = getScanQty() + delta;
        int max = getMaxScanQty();
        if (qty > max) {
            showInsufficientStock("Max allowed: " + max + (isSingleUom ? " EA" : " PAC") + ".");
            return;
        }
        if (qty < 1) {
            qty = 1;
        }
        setScanQty(qty);
    }

    private void clear() {
        packSize = 1;
        stkQty = 0;
        scannedEan11 = "";
        sapMatnr = "";
        validatedPackMeins = "PAK";
        txt_store.setText(WERKS);
        txt_barcode.setText("");
        updateScannedBarcodeDisplay();
        txt_article.setText("");
        setScanQty(0);
        setQtyControlsEnabled(false);
        selectUom(true);
        updatePackLabel();
        UIFuncs.enableInput(con, txt_barcode);
        txt_barcode.requestFocus();
    }

    private void updatePackLabel() {
        if (packSize > 1) {
            txt_pack_label.setText("PAC = " + Util.formatDouble(packSize) + " EA");
        } else {
            txt_pack_label.setText("PAC");
        }
    }

    private double currentFactor() {
        return isSingleUom ? 1 : packSize;
    }

    /** SAP MEINS for save — use PAK (not PAC) for pack per material master. */
    private String meinsForSave() {
        return isSingleUom ? "EA" : validatedPackMeins;
    }

    private static String sapMenge3(double qty) {
        return String.format(Locale.US, "%.3f", qty);
    }

    private static String toSapMatnr(String matnr) {
        if (matnr == null) {
            return "";
        }
        String value = matnr.trim();
        if (value.isEmpty()) {
            return "";
        }
        while (value.length() < 18) {
            value = "0" + value;
        }
        return value;
    }

    /** Same gateway path as other store RFCs (GRC Putaway, HU Gate Entry). */
    private String buildRfcUrl(String rfc) {
        return GatewayUrls.noAclJsonRfcUrl(URL, rfc);
    }

    private double getTotalScanQtyEa() {
        return getScanQty() * currentFactor();
    }

    private int getMaxScanQty() {
        if (stkQty <= 0 || currentFactor() <= 0) {
            return 0;
        }
        return (int) Math.floor(stkQty / currentFactor());
    }

    private boolean isStockSufficient() {
        return stkQty >= getTotalScanQtyEa();
    }

    private void showInsufficientStock(String detail) {
        UIFuncs.errorSound(con);
        box.getBox("Invalid", "Qty exceeds available stock (" + Util.formatDouble(stkQty) + " EA)."
                + (detail.isEmpty() ? "" : "\n" + detail));
    }

    private void enforceStockLimit(boolean showMessage) {
        if (sapMatnr.isEmpty()) {
            return;
        }
        int max = getMaxScanQty();
        int qty = getScanQty();
        if (max < 1) {
            if (qty > 0) {
                setScanQty(0);
                if (showMessage) {
                    showInsufficientStock("No stock available.");
                }
            }
            return;
        }
        if (qty > max) {
            setScanQty(max);
            if (showMessage) {
                showInsufficientStock("Max allowed: " + max + (isSingleUom ? " EA" : " PAC") + ".");
            }
        }
    }

    private void recalcTotal() {
        int qty = getScanQty();
        double total = qty * currentFactor();
        txt_total_qty.setText(Util.formatDouble(total) + " EA");
    }

    private void addInputEvents() {
        txt_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    processBarcodeScan();
                    return true;
                }
                return false;
            }
        });

        txt_barcode.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 2;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (scannerReading) {
                    processBarcodeScan();
                }
            }
        });

        txt_scan_qty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recalcTotal();
                enforceStockLimit(false);
            }
        });

        txt_scan_qty.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    normalizeScanQtyInput();
                }
            }
        });

        txt_scan_qty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    normalizeScanQtyInput();
                    return true;
                }
                return false;
            }
        });
    }

    private void processBarcodeScan() {
        String barcode = UIFuncs.toUpperTrim(txt_barcode);
        if (barcode.isEmpty()) {
            return;
        }
        onBarcodeScanned(barcode);
    }

    private void onBarcodeScanned(String barcode) {
        if (barcodeValidateInProgress) {
            return;
        }

        String url = buildRfcUrl(Vars.ZRFC_ART_CONS_VALIDATE_EAN);
        if (url.isEmpty()) {
            box.getBox("Err", "Server URL missing. Please log in again.");
            return;
        }

        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZRFC_ART_CONS_VALIDATE_EAN);
            params.put("IV_EAN", barcode);
            params.put("IV_WERKS", WERKS);
        } catch (JSONException e) {
            box.getErrBox(e);
            return;
        }

        showProgress("Validating barcode...");
        barcodeValidateInProgress = true;
        UIFuncs.disableInput(con, txt_barcode);

        Log.d(TAG, "validate url -> " + url);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest request = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissProgress();
                        barcodeValidateInProgress = false;
                        UIFuncs.enableInput(con, txt_barcode);
                        Log.d(TAG, "validate response -> " + response);

                        if (response == null || response.length() == 0) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from server.");
                            resetBarcodeInput();
                            return;
                        }

                        try {
                            applyValidateEanResponse(response);
                        } catch (JSONException e) {
                            box.getErrBox(e);
                            resetBarcodeInput();
                        }
                    }
                }, validateErrorListener()) {
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
                return super.parseNetworkResponse(withoutValueXmwPrefix(response));
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
        Log.d(TAG, "validate payload -> " + params);
    }

    private void applyValidateEanResponse(JSONObject response) throws JSONException {
        if (!isRfcSuccess(response)) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", getRfcMessage(response, "Invalid barcode."));
            resetBarcodeInput();
            return;
        }

        String matnrRaw = response.optString("EV_MATNR", "").trim();
        String matnr = UIFuncs.removeLeadingZeros(matnrRaw);
        String meins = response.optString("EV_MEINS", "").trim().toUpperCase();
        double qty = Util.convertStringToDouble(response.optString("EV_QTY", "0"));
        stkQty = Util.convertStringToDouble(response.optString("EV_STK_QTY", "0"));

        scannedEan11 = UIFuncs.toUpperTrim(txt_barcode);
        sapMatnr = matnrRaw.isEmpty() ? toSapMatnr(matnr) : matnrRaw;
        txt_article.setText(matnr);
        packSize = qty > 0 ? qty : 1;
        updatePackLabel();
        setScanQty(0);
        applyUomFromMeins(meins);
        int maxScanQty = getMaxScanQty();
        if (maxScanQty < 1) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "No stock available. Available: " + Util.formatDouble(stkQty) + " EA");
            resetBarcodeInput();
            return;
        }
        setScanQty(1);
        setQtyControlsEnabled(true);
        txt_barcode.setText("");
        UIFuncs.enableInput(con, txt_barcode);
        txt_barcode.requestFocus();
        updateScannedBarcodeDisplay();
    }

    private void updateScannedBarcodeDisplay() {
        if (scannedEan11 == null || scannedEan11.isEmpty()) {
            txt_scanned_barcode.setVisibility(View.GONE);
            txt_scanned_barcode.setText("");
        } else {
            txt_scanned_barcode.setVisibility(View.VISIBLE);
            txt_scanned_barcode.setText(scannedEan11);
        }
    }

    private void applyUomFromMeins(String meins) {
        String value = meins == null ? "" : meins.trim().toUpperCase(Locale.ROOT);
        if ("PAC".equals(value) || "PAK".equals(value)) {
            validatedPackMeins = "PAK";
            selectUom(false);
        } else if ("EA".equals(value)) {
            selectUom(true);
        } else {
            selectUom(true);
        }
    }

    private static int getEvSubrc(JSONObject response) {
        if (response == null || !response.has("EV_SUBRC")) {
            return -1;
        }
        String subrc = response.optString("EV_SUBRC", "").trim();
        if (subrc.isEmpty()) {
            return response.optInt("EV_SUBRC", -1);
        }
        try {
            return Integer.parseInt(subrc);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isRfcSuccess(JSONObject response) {
        if (response == null) {
            return false;
        }
        if (response.has("EV_SUBRC")) {
            return getEvSubrc(response) == 0;
        }
        return true;
    }

    private static String getRfcMessage(JSONObject response, String fallback) {
        String message = response.optString("EV_MSG", "").trim();
        if (!message.isEmpty()) {
            return message;
        }
        return fallback;
    }

    private void resetBarcodeInput() {
        txt_barcode.setText("");
        txt_article.setText("");
        scannedEan11 = "";
        sapMatnr = "";
        stkQty = 0;
        validatedPackMeins = "PAK";
        packSize = 1;
        updatePackLabel();
        setScanQty(0);
        setQtyControlsEnabled(false);
        selectUom(true);
        updateScannedBarcodeDisplay();
        txt_barcode.requestFocus();
    }

    private Response.ErrorListener validateErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgress();
                barcodeValidateInProgress = false;
                UIFuncs.enableInput(con, txt_barcode);
                UIFuncs.errorSound(con);
                Log.e(TAG, "validate error -> " + error);
                box.getBox("Err", parseVolleyError(error));
                txt_barcode.requestFocus();
            }
        };
    }

    private Response.ErrorListener saveErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgress();
                saveInProgress = false;
                btn_save.setEnabled(true);
                UIFuncs.errorSound(con);
                Log.e(TAG, "save error -> " + error);
                box.getBox("Err", parseVolleyError(error));
            }
        };
    }

    private static NetworkResponse withoutValueXmwPrefix(NetworkResponse response) {
        if (response == null || response.data == null || response.data.length == 0) {
            return response;
        }
        try {
            String raw = new String(response.data, StandardCharsets.UTF_8).trim();
            if (raw.startsWith("Response:")) {
                byte[] stripped = raw.substring("Response:".length()).trim()
                        .getBytes(StandardCharsets.UTF_8);
                return new NetworkResponse(
                        response.statusCode,
                        stripped,
                        response.headers,
                        response.notModified,
                        response.networkTimeMs);
            }
        } catch (Exception ignored) {
        }
        return response;
    }

    private String parseVolleyError(VolleyError error) {
        if (error instanceof TimeoutError) {
            return "Request timed out. Please try again.";
        }
        if (error instanceof NoConnectionError) {
            return "No network connection.";
        }
        if (error instanceof NetworkError) {
            return "Network error. Check connection and try again.";
        }
        if (error instanceof ServerError) {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                String body = new String(error.networkResponse.data, StandardCharsets.UTF_8).trim();
                if (body.startsWith("Response:")) {
                    body = body.substring("Response:".length()).trim();
                }
                if (!body.isEmpty()) {
                    int max = Math.min(200, body.length());
                    return "Server error: " + body.substring(0, max);
                }
            }
            return "Server error. RFC may not be available on gateway.";
        }
        if (error instanceof ParseError) {
            return "Invalid response from server.";
        }
        return "Unable to connect to server.";
    }

    private void showProgress(String message) {
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(getContext());
            dialog.setCancelable(false);
        }
        dialog.setMessage(message);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void save() {
        if (saveInProgress) {
            return;
        }

        normalizeScanQtyInput();
        String article = UIFuncs.toUpperTrim(txt_article);
        double qty = getScanQty();

        if (article.isEmpty() || sapMatnr.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "Please scan a barcode first");
            return;
        }
        if (qty <= 0) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "Please enter a valid scan qty");
            return;
        }
        if (!isStockSufficient()) {
            showInsufficientStock("Total scan qty: " + Util.formatDouble(getTotalScanQtyEa()) + " EA.");
            return;
        }

        String url = buildRfcUrl(Vars.ZRFC_ART_CONS_POST);
        if (url.isEmpty()) {
            box.getBox("Err", "Server URL missing. Please log in again.");
            return;
        }

        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", Vars.ZRFC_ART_CONS_POST);
            params.put("IV_WERKS", WERKS);
            params.put("IV_LGORT", DEFAULT_LGORT);

            JSONObject scanItem = new JSONObject();
            scanItem.put("EAN11", scannedEan11);
            scanItem.put("MATNR", toSapMatnr(sapMatnr));
            scanItem.put("SCAN_QTY", sapMenge3(qty));
            scanItem.put("MEINS", meinsForSave());

            JSONArray itScanItems = new JSONArray();
            itScanItems.put(scanItem);
            params.put("IT_SCAN_ITEMS", itScanItems);
        } catch (JSONException e) {
            box.getErrBox(e);
            return;
        }

        showProgress("Saving consumption...");
        saveInProgress = true;
        btn_save.setEnabled(false);
        Log.d(TAG, "save url -> " + url);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest request = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        dismissProgress();
                        saveInProgress = false;
                        btn_save.setEnabled(true);
                        Log.d(TAG, "save response -> " + response);

                        if (response == null || response.length() == 0) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from server.");
                            return;
                        }

                        try {
                            applyPostResponse(response);
                        } catch (JSONException e) {
                            box.getErrBox(e);
                        }
                    }
                }, saveErrorListener()) {
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
                return super.parseNetworkResponse(withoutValueXmwPrefix(response));
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
        Log.d(TAG, "save payload -> " + params);
    }

    private void applyPostResponse(JSONObject response) throws JSONException {
        if (!isRfcSuccess(response)) {
            UIFuncs.errorSound(con);
            box.getBox("Error", getRfcMessage(response, "Save failed."));
            return;
        }

        String mblnr = response.optString("EV_MBLNR", "").trim();
        String mjahr = response.optString("EV_MJAHR", "").trim();
        String msg = getRfcMessage(response, "Consumption saved successfully.");
        if (!mblnr.isEmpty()) {
            msg = msg + "\nDoc: " + UIFuncs.removeLeadingZeros(mblnr);
            if (!mjahr.isEmpty()) {
                msg = msg + " / " + mjahr;
            }
        }

        box.getBox("Success", msg, (dialog, which) -> clear());
    }
}
