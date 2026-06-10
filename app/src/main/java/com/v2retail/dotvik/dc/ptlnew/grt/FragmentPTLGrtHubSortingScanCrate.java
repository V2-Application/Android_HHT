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
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PTL-GRT — Hub Sorting, Scan Crate.
 * <ul>
 *   <li>Crate validate: {@link Vars#ZWM_PTL_GRT_MSA_CRATE_VALIDATE}</li>
 *   <li>Hub tag save: {@link Vars#ZWM_PTL_HUB_ARTICLE_TAG_CRATE}</li>
 * </ul>
 */
public class FragmentPTLGrtHubSortingScanCrate extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentPTLGrtHubSortingScanCrate.class.getSimpleName();
    private static final String ACTION_BAR_TITLE = "HUB SORTING";
    private static final List<String> FLOOR_OPTIONS = Arrays.asList("0", "1", "2", "3", "4", "5");
    private static final int REQUEST_VALIDATE_CRATE = 5911;
    private static final int REQUEST_TAG_HUB = 5912;

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
    private EditText txtScanArticle;
    private EditText txtArticle;
    private EditText txtScanQty;
    private EditText txtProposedHub;
    private EditText txtScanHub;
    private Button btnBack;

    private boolean floorSelected = false;
    private String validatedCrate = "";
    private Map<String, JSONObject> etDataMap = new HashMap<>();
    private Map<String, JSONObject> eanDataMap = new HashMap<>();
    private Map<String, Double> scannedQtyByArticle = new HashMap<>();

    private String currentArticle = "";
    private JSONObject currentEtRow = null;
    private double currentMaxQty = 0;
    private double currentScannedQty = 0;

    public FragmentPTLGrtHubSortingScanCrate() {
    }

    public static FragmentPTLGrtHubSortingScanCrate newInstance() {
        return new FragmentPTLGrtHubSortingScanCrate();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ptl_grt_hub_sorting_scan_crate, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        ddFloor = root.findViewById(R.id.dd_ptl_grt_hub_sorting_scan_crate_floor);
        txtScanCrate = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan);
        txtCrate = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_crate);
        txtScanArticle = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan_article);
        txtArticle = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_article);
        txtScanQty = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan_qty);
        txtProposedHub = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_proposed_hub);
        txtScanHub = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan_hub);
        btnBack = root.findViewById(R.id.btn_ptl_grt_hub_sorting_scan_crate_back);

        setupFloorDropdown();
        addCrateScanEvents();
        addArticleScanEvents();
        addHubScanEvents();
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
                resetAfterFloorChange();
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
                    requestCrateValidate(scanned);
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
                    requestCrateValidate(value);
                }
            }
        });
    }

    private void addArticleScanEvents() {
        txtScanArticle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtScanArticle);
                if (!TextUtils.isEmpty(scanned)) {
                    validateArticleScan(scanned);
                }
                return true;
            }
            return false;
        });

        txtScanArticle.addTextChangedListener(new TextWatcher() {
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
                    validateArticleScan(value);
                }
            }
        });
    }

    private void addHubScanEvents() {
        txtScanHub.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtScanHub);
                if (!TextUtils.isEmpty(scanned)) {
                    requestHubTag(scanned);
                }
                return true;
            }
            return false;
        });

        txtScanHub.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 2;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    requestHubTag(value);
                }
            }
        });
    }

    private String getSelectedFloor() {
        Object selected = ddFloor.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private static String normalizeArticle(String article) {
        if (article == null) {
            return "";
        }
        return article.trim().toUpperCase(Locale.ROOT);
    }

    private static double parseQty(JSONObject row, String primaryKey, String fallbackKey, double defaultValue) {
        if (row == null) {
            return defaultValue;
        }
        String primary = row.optString(primaryKey, "").trim();
        if (!primary.isEmpty()) {
            return Util.convertStringToDouble(primary);
        }
        String fallback = row.optString(fallbackKey, "").trim();
        if (!fallback.isEmpty()) {
            return Util.convertStringToDouble(fallback);
        }
        return defaultValue;
    }

    private JSONObject findEtDataForArticle(String article) {
        String target = normalizeArticle(article);
        if (target.isEmpty()) {
            return null;
        }
        for (JSONObject row : etDataMap.values()) {
            String etArticle = normalizeArticle(row.optString("ARTICLE", ""));
            if (target.equals(etArticle)
                    || target.equals(normalizeArticle(UIFuncs.removeLeadingZeros(etArticle)))) {
                return row;
            }
        }
        return null;
    }

    private String resolveHubFromEtRow(JSONObject etRow) {
        if (etRow == null) {
            return "";
        }
        String hub = etRow.optString("HUB", "").trim();
        if (hub.isEmpty()) {
            hub = etRow.optString("PLT_REC_HUBZONE", "").trim();
        }
        return hub;
    }

    private void requestCrateValidate(String scannedCrate) {
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
            args.put("bapiname", Vars.ZWM_PTL_GRT_MSA_CRATE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_CRATE", scannedCrate);
            showProcessingAndSubmit(Vars.ZWM_PTL_GRT_MSA_CRATE_VALIDATE, REQUEST_VALIDATE_CRATE, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestCrateValidate", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void validateArticleScan(String barcode) {
        if (TextUtils.isEmpty(validatedCrate)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate Crate first.");
            txtScanArticle.setText("");
            txtScanCrate.requestFocus();
            return;
        }

        JSONObject eanRow = eanDataMap.get(barcode);
        if (eanRow == null) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "Scanned EAN is not available in EAN records.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        String article = eanRow.optString("ARTICLE", "").trim();
        if (article.isEmpty()) {
            article = eanRow.optString("MATNR", "").trim();
        }
        if (article.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "EAN record does not contain article/material.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        JSONObject etRow = findEtDataForArticle(article);
        if (etRow == null) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "Article " + UIFuncs.removeLeadingZeros(article) + " not found in ET records.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        String etArticle = etRow.optString("ARTICLE", article).trim();
        String articleKey = normalizeArticle(etArticle);

        if (!TextUtils.isEmpty(currentArticle)
                && !articleKey.equals(normalizeArticle(currentArticle))
                && currentScannedQty > 0
                && currentScannedQty < currentMaxQty) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Complete scanning for current article before scanning another.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        double packQty = parseQty(eanRow, "QUNANTITY", "QUANTITY", 1);
        if (packQty <= 0) {
            packQty = 1;
        }

        double maxQty = parseQty(etRow, "SCAN_QTY", "QTY", 0);
        if (maxQty <= 0) {
            maxQty = parseQty(etRow, "QTY", "QUANTITY", 0);
        }
        if (maxQty <= 0) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "No scan quantity defined for this article.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        double alreadyScanned = scannedQtyByArticle.containsKey(articleKey)
                ? scannedQtyByArticle.get(articleKey) : 0;
        if (alreadyScanned + packQty > maxQty) {
            UIFuncs.errorSound(con);
            box.getBox("Qty Exceeded", "Scan quantity cannot exceed " + Util.convertToDoubleString(String.valueOf(maxQty)));
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        alreadyScanned += packQty;
        scannedQtyByArticle.put(articleKey, alreadyScanned);

        currentArticle = etArticle;
        currentEtRow = etRow;
        currentMaxQty = maxQty;
        currentScannedQty = alreadyScanned;

        txtArticle.setText(UIFuncs.removeLeadingZeros(etArticle));
        txtScanQty.setText(Util.convertToDoubleString(String.valueOf(alreadyScanned))
                + " / " + Util.convertToDoubleString(String.valueOf(maxQty)));
        txtProposedHub.setText(resolveHubFromEtRow(etRow));
        txtScanArticle.setText("");

        if (alreadyScanned >= maxQty) {
            UIFuncs.disableInput(con, txtScanArticle);
            UIFuncs.enableInput(con, txtScanHub);
            txtScanHub.requestFocus();
        } else {
            txtScanArticle.requestFocus();
        }
    }

    private void requestHubTag(String scannedHub) {
        if (TextUtils.isEmpty(validatedCrate) || TextUtils.isEmpty(currentArticle)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please complete article scanning first.");
            txtScanHub.setText("");
            txtScanArticle.requestFocus();
            return;
        }
        if (currentScannedQty < currentMaxQty) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan full quantity before tagging HUB.");
            txtScanHub.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        String proposedHub = UIFuncs.toUpperTrim(txtProposedHub);
        if (!TextUtils.isEmpty(proposedHub)
                && !proposedHub.equalsIgnoreCase(scannedHub)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Scanned HUB does not match Purposed HUB (" + proposedHub + ").");
            txtScanHub.setText("");
            txtScanHub.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HUB_ARTICLE_TAG_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_SOURCE_CRATE", validatedCrate);

            JSONObject itRow = new JSONObject();
            itRow.put("MSA_CRATE", validatedCrate);
            itRow.put("ARTICLE", currentArticle);
            itRow.put("SCAN_QTY", Util.convertToDoubleString(String.valueOf(currentScannedQty)));
            itRow.put("FLOOR", getSelectedFloor());
            itRow.put("HUB", scannedHub);

            JSONArray itData = new JSONArray();
            itData.put(itRow);
            args.put("IT_DATA", itData);

            Log.d(TAG, "hub tag IT_DATA -> " + itRow);
            showProcessingAndSubmit(Vars.ZWM_PTL_HUB_ARTICLE_TAG_CRATE, REQUEST_TAG_HUB, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestHubTag", e);
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
                volleyErrorListener(request)) {
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

            if ("E".equals(type)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", message);
                if (request == REQUEST_VALIDATE_CRATE) {
                    clearAfterCrateValidateFailure();
                } else if (request == REQUEST_TAG_HUB) {
                    txtScanHub.setText("");
                    txtScanHub.requestFocus();
                }
                return;
            }

            if (!TextUtils.isEmpty(message)) {
                box.getBox("Success", message);
            }

            if (request == REQUEST_VALIDATE_CRATE) {
                handleCrateValidateSuccess(responsebody);
            } else if (request == REQUEST_TAG_HUB) {
                handleHubTagSuccess();
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleRfcResponse", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void handleCrateValidateSuccess(JSONObject responsebody) throws JSONException {
        validatedCrate = UIFuncs.toUpperTrim(txtScanCrate);
        txtCrate.setText(validatedCrate);
        txtScanCrate.setText("");
        UIFuncs.disableInput(con, txtScanCrate);

        resetArticleFields();
        etDataMap = new HashMap<>();
        eanDataMap = new HashMap<>();
        scannedQtyByArticle = new HashMap<>();

        if (responsebody.has("ET_DATA")) {
            JSONArray etDataArray = responsebody.getJSONArray("ET_DATA");
            int etStart = SapJsonRows.startIndex(etDataArray, "CRATE", "ARTICLE");
            for (int i = etStart; i < etDataArray.length(); i++) {
                JSONObject row = etDataArray.getJSONObject(i);
                if (SapJsonRows.isMetadataRow(row, "CRATE", "ARTICLE")) {
                    continue;
                }
                String article = row.optString("ARTICLE", "").trim();
                if (!article.isEmpty()) {
                    etDataMap.put(normalizeArticle(article), row);
                }
            }
        }

        if (responsebody.has("ET_EAN_DATA")) {
            JSONArray eanDataArray = responsebody.getJSONArray("ET_EAN_DATA");
            int eanStart = SapJsonRows.startIndex(eanDataArray, "EAN11", "ARTICLE");
            for (int i = eanStart; i < eanDataArray.length(); i++) {
                JSONObject row = eanDataArray.getJSONObject(i);
                if (SapJsonRows.isMetadataRow(row, "EAN11", "ARTICLE")) {
                    continue;
                }
                String ean = row.optString("EAN11", "").trim();
                if (!ean.isEmpty()) {
                    eanDataMap.put(ean, row);
                }
            }
        }

        if (etDataMap.isEmpty() || eanDataMap.isEmpty()) {
            box.getBox("No Records", "No article/EAN data returned for this crate.");
        } else {
            UIFuncs.enableInput(con, txtScanArticle);
            txtScanArticle.requestFocus();
        }
    }

    private void handleHubTagSuccess() {
        if (!TextUtils.isEmpty(currentArticle)) {
            scannedQtyByArticle.put(normalizeArticle(currentArticle), currentMaxQty);
        }
        resetArticleFields();
        UIFuncs.enableInput(con, txtScanArticle);
        txtScanArticle.requestFocus();
    }

    private void resetArticleFields() {
        currentArticle = "";
        currentEtRow = null;
        currentMaxQty = 0;
        currentScannedQty = 0;
        txtArticle.setText("");
        txtScanQty.setText("");
        txtProposedHub.setText("");
        txtScanArticle.setText("");
        txtScanHub.setText("");
        UIFuncs.disableInput(con, txtScanHub);
    }

    private void clearAfterCrateValidateFailure() {
        validatedCrate = "";
        txtCrate.setText("");
        txtScanCrate.setText("");
        UIFuncs.enableInput(con, txtScanCrate);
        txtScanCrate.requestFocus();
    }

    private void resetAfterFloorChange() {
        validatedCrate = "";
        etDataMap = new HashMap<>();
        eanDataMap = new HashMap<>();
        scannedQtyByArticle = new HashMap<>();
        txtCrate.setText("");
        txtScanCrate.setText("");
        resetArticleFields();
        UIFuncs.disableInput(con, txtScanArticle);
        UIFuncs.enableInput(con, txtScanCrate);
    }

    private void resetScreen() {
        resetAfterFloorChange();
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

    private Response.ErrorListener volleyErrorListener(int request) {
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
            box.getBox("Err", err);
            if (request == REQUEST_VALIDATE_CRATE) {
                clearAfterCrateValidateFailure();
            } else if (request == REQUEST_TAG_HUB) {
                txtScanHub.setText("");
                txtScanHub.requestFocus();
            }
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
        if (view.getId() == R.id.btn_ptl_grt_hub_sorting_scan_crate_back) {
            fm.popBackStack();
        }
    }
}
