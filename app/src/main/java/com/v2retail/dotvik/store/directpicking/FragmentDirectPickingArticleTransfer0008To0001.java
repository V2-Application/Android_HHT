package com.v2retail.dotvik.store.directpicking;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.FloorBarcode;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FragmentDirectPickingArticleTransfer0008To0001 extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_BARCODE = 1601;
    private static final int REQUEST_SAVE = 1602;

    private static final String TAG = FragmentDirectPickingArticleTransfer0008To0001.class.getName();

    View rootView;
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    boolean requestInFlight = false;
    ProgressDialog articleLookupDialog;
    FragmentManager fm;

    Button btn_back, btn_save;
    EditText txt_store, txt_scan_barcode, txt_article, txt_article_type, txt_article_size;
    EditText txt_tsq, txt_tq;

    // Scan totals keyed by barcode. Holds SCAN_QTY vs allowed TOTAL_QTY (VERME) for that barcode.
    Map<String, FloorBarcode> barcodeScanMap = new HashMap<>();
    // Barcodes already validated by ZSDC_DIRECT_BARCODE_VAL_RFC this session (EAN/EAN2 → article).
    Map<String, FloorBarcode> barcodeValMap = new HashMap<>();
    Map<String, String[]> articleLookupMap = new HashMap<>();
    String lastScannedBarcode = "";

    public FragmentDirectPickingArticleTransfer0008To0001() {
    }

    public static FragmentDirectPickingArticleTransfer0008To0001 newInstance() {
        return new FragmentDirectPickingArticleTransfer0008To0001();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Article Transfer from (0008 TO 0001)");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_direct_picking_article_transfer0008_0001, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_store);
        txt_scan_barcode = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_scan_barcode);
        txt_article = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_article);
        txt_article_type = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_article_type);
        txt_article_size = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_article_size);
        txt_tsq = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_tsq);
        txt_tq = rootView.findViewById(R.id.txt_direct_picking_article_transfer_0008_0001_tq);

        btn_back = rootView.findViewById(R.id.btn_direct_picking_article_transfer_0008_0001_back);
        btn_save = rootView.findViewById(R.id.btn_direct_picking_article_transfer_0008_0001_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_direct_picking_article_transfer_0008_0001_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_direct_picking_article_transfer_0008_0001_save:
                save();
                break;
        }
    }

    private void clear() {
        barcodeScanMap = new HashMap<>();
        barcodeValMap = new HashMap<>();
        articleLookupMap = new HashMap<>();
        lastScannedBarcode = "";
        txt_store.setText(WERKS);
        txt_article.setText("");
        txt_article_type.setText("");
        txt_article_size.setText("");
        if (txt_tsq != null) txt_tsq.setText("");
        if (txt_tq != null) txt_tq.setText("");
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private void clearArticleMeta() {
        if (txt_article_type != null) txt_article_type.setText("");
        if (txt_article_size != null) txt_article_size.setText("");
        if (txt_tsq != null) txt_tsq.setText("");
        if (txt_tq != null) txt_tq.setText("");
    }

    private void showScanError(String title, String message) {
        UIFuncs.errorSound(getContext());
        AlertBox ab = new AlertBox(getContext());
        ab.getBox(title, message, (dialog, which) -> {
            clearArticleMeta();
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
        });
    }

    private void showBarcodeQtyExceeded() {
        UIFuncs.errorSound(getContext());
        AlertBox ab = new AlertBox(getContext());
        ab.getBox("Invalid", "Barcode quantity exceeded.", (dialog, which) -> {
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
        });
    }

    private double getTotalScannedQty() {
        double total = 0;
        for (FloorBarcode art : barcodeScanMap.values()) {
            total += Util.convertStringToDouble(art.getScanQty());
        }
        return total;
    }

    private String barcodeScanKey(FloorBarcode mapping, String scannedBarcode) {
        String canonical = mapping != null ? normalizeKey(mapping.getBarcode()) : "";
        if (!canonical.isEmpty()) {
            return canonical;
        }
        return normalizeKey(scannedBarcode);
    }

    private void addInputEvents() {
        txt_scan_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_barcode);
                    if (!value.isEmpty()) {
                        validateBarcode(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_barcode.addTextChangedListener(new TextWatcher() {
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
                    validateBarcode(value);
                }
            }
        });
    }

    public void validateBarcode(String barcode) {
        if (requestInFlight) {
            return;
        }
        String key = normalizeKey(barcode);
        if (key.isEmpty()) {
            return;
        }
        // Repeat scans of an already-validated barcode increment qty locally.
        if (barcodeValMap.containsKey(key)) {
            updateQtyAfterScan(key);
            return;
        }
        JSONObject args = new JSONObject();
        try {
            lastScannedBarcode = barcode;
            requestInFlight = true;
            String rfc = Vars.ZSDC_DIRECT_BARCODE_VAL_RFC;
            args.put("bapiname", rfc);
            args.put("IM_WERKS", WERKS);
            args.put("IM_BARCODE", barcode);
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_BARCODE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            requestInFlight = false;
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private static String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase();
    }

    private JSONArray getExDataArray(JSONObject responsebody) {
        if (responsebody == null || !responsebody.has("EX_DATA")) {
            return null;
        }
        Object raw = responsebody.opt("EX_DATA");
        if (raw instanceof JSONArray) {
            return (JSONArray) raw;
        }
        if (raw instanceof JSONObject) {
            JSONArray arr = new JSONArray();
            arr.put(raw);
            return arr;
        }
        return null;
    }

    private JSONObject findBarcodeExDataRow(JSONObject responsebody, String scannedBarcode) {
        JSONArray arr = getExDataArray(responsebody);
        if (arr == null || arr.length() == 0) {
            return null;
        }
        String scanKey = normalizeKey(scannedBarcode);
        JSONObject firstData = null;
        int start;
        try {
            start = SapJsonRows.startIndex(arr, "EAN", "ARTICLE", "EAN2");
        } catch (JSONException e) {
            start = 0;
        }
        for (int i = start; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null || SapJsonRows.isMetadataRow(row, "EAN", "ARTICLE", "EAN2")) {
                continue;
            }
            if (firstData == null) {
                firstData = row;
            }
            String ean = normalizeKey(row.optString("EAN", ""));
            String ean2 = normalizeKey(row.optString("EAN2", ""));
            if (!scanKey.isEmpty() && (scanKey.equals(ean) || scanKey.equals(ean2))) {
                return row;
            }
        }
        return firstData;
    }

    private void cacheBarcodeMapping(String key, FloorBarcode mapping) {
        String normalized = normalizeKey(key);
        if (!normalized.isEmpty() && mapping != null) {
            barcodeValMap.put(normalized, mapping);
        }
    }

    private void setBarcodeData(JSONObject responsebody) {
        String scannedBarcode = lastScannedBarcode;
        if (scannedBarcode == null || scannedBarcode.trim().isEmpty()) {
            scannedBarcode = UIFuncs.toUpperTrim(txt_scan_barcode);
        }
        JSONObject row = findBarcodeExDataRow(responsebody, scannedBarcode);
        if (row == null) {
            showScanError("Invalid", "No article data returned for this barcode");
            return;
        }

        String ean = row.optString("EAN", "").trim();
        String ean2 = row.optString("EAN2", "").trim();
        String article = row.optString("ARTICLE", "").trim();
        String totalQty = row.optString("TOTAL_QTY", "0").trim();
        if (article.isEmpty()) {
            showScanError("Invalid", "Article not returned for this barcode");
            return;
        }

        FloorBarcode mapping = new FloorBarcode();
        if (!ean.isEmpty()) {
            mapping.setBarcode(ean);
        } else if (!ean2.isEmpty()) {
            mapping.setBarcode(ean2);
        } else {
            mapping.setBarcode(scannedBarcode);
        }
        mapping.setMatnr(article);
        mapping.setVerme(totalQty);
        mapping.setUmrez("1");

        cacheBarcodeMapping(scannedBarcode, mapping);
        cacheBarcodeMapping(ean, mapping);
        cacheBarcodeMapping(ean2, mapping);

        updateQtyAfterScan(normalizeKey(scannedBarcode));
    }

    private void showArticleLookupLoading() {
        try {
            if (getContext() == null) return;
            if (articleLookupDialog != null && articleLookupDialog.isShowing()) return;
            articleLookupDialog = new ProgressDialog(getContext());
            articleLookupDialog.setMessage("Please wait...");
            articleLookupDialog.setCancelable(false);
            articleLookupDialog.show();
        } catch (Exception ignored) {
        }
    }

    private void hideArticleLookupLoading() {
        try {
            if (articleLookupDialog != null) {
                if (articleLookupDialog.isShowing()) {
                    articleLookupDialog.dismiss();
                }
                articleLookupDialog = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void lookupArticle(String articleNo) {
        String articleKey = normalizeKey(articleNo);
        if (articleKey.isEmpty()) {
            return;
        }

        if (articleLookupMap.containsKey(articleKey)) {
            applyArticleLookup(articleKey);
            return;
        }

        try {
            showArticleLookupLoading();

            JSONObject params = new JSONObject();
            params.put("store", WERKS);
            params.put("article", articleNo.trim());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    Vars.ARTICLE_LOOKUP_URL,
                    params,
                    response -> {
                        hideArticleLookupLoading();
                        if (response != null && response.optBoolean("status", false)) {
                            String articleType = response.optString("article_type", "").trim();
                            String articleSize = mapArticleSize(response.optString("article_size", "").trim());
                            articleLookupMap.put(articleKey, new String[]{articleType, articleSize});
                            applyArticleLookup(articleKey);
                        }
                    },
                    error -> hideArticleLookupLoading()
            );
            request.setRetryPolicy(new DefaultRetryPolicy(
                    50000,
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            ApplicationController.getInstance().getRequestQueue().add(request);
        } catch (JSONException e) {
            e.printStackTrace();
            hideArticleLookupLoading();
        }
    }

    private static String mapArticleSize(String articleSize) {
        if (articleSize == null) return "";
        String s = articleSize.trim();
        if (s.equalsIgnoreCase("NORMAL") || s.equalsIgnoreCase("S")) {
            return "NS";
        }
        return s;
    }

    private void applyArticleLookup(String articleKey) {
        String aKey = normalizeKey(articleKey);
        String[] values = articleLookupMap.get(aKey);
        if (values == null) {
            return;
        }

        if (values.length > 1) {
            values[1] = mapArticleSize(values[1]);
        }

        for (FloorBarcode art : barcodeScanMap.values()) {
            if (art == null || !aKey.equals(normalizeKey(art.getMatnr()))) {
                continue;
            }
            if (values[0] != null && !values[0].isEmpty()) art.setArtType(values[0]);
            if (values[1] != null && !values[1].isEmpty()) art.setArtSize(values[1]);
        }

        String currentArticle = normalizeKey(UIFuncs.toUpperTrim(txt_article));
        if (!currentArticle.isEmpty() && currentArticle.equals(aKey)) {
            if (values[0] != null && !values[0].isEmpty()) txt_article_type.setText(values[0]);
            if (values[1] != null && !values[1].isEmpty()) txt_article_size.setText(values[1]);
        }
    }

    private static JSONObject parseReturnObject(JSONObject responsebody) throws JSONException {
        if (!responsebody.has("EX_RETURN")) {
            return null;
        }
        Object raw = responsebody.get("EX_RETURN");
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            JSONObject firstSuccess = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String type = row.optString("TYPE", "").trim();
                if ("E".equals(type) || "A".equals(type)) {
                    return row;
                }
                if (firstSuccess == null) {
                    firstSuccess = row;
                }
            }
            return firstSuccess;
        }
        return null;
    }

    private static boolean isSapErrorReturn(JSONObject returnobj) {
        if (returnobj == null) {
            return false;
        }
        String type = returnobj.optString("TYPE", "").trim();
        return "E".equals(type) || "A".equals(type);
    }

    private void updateQtyAfterScan(String barcode) {
        FloorBarcode ean = barcodeValMap.get(barcode);
        if (ean == null) {
            showScanError("Invalid", "Scanned Barcode is invalid");
            return;
        }

        String scanKey = barcodeScanKey(ean, barcode);
        FloorBarcode art = barcodeScanMap.get(scanKey);
        if (art == null) {
            art = new FloorBarcode();
            art.setMatnr(ean.getMatnr());
            art.setVerme(ean.getVerme());
            art.setScanQty("0");
            art.setUmrez(ean.getUmrez());
            art.setBarcode(ean.getBarcode());
            barcodeScanMap.put(scanKey, art);
        }

        double nextQty = Util.convertStringToDouble(art.getScanQty()) + 1;
        double allowedQty = Util.convertStringToDouble(art.getVerme());
        if (nextQty > allowedQty) {
            showBarcodeQtyExceeded();
            return;
        }

        art.setScanQty(Util.formatDouble(nextQty));
        art.setBarcode(ean.getBarcode());
        art.setUmrez(ean.getUmrez());
        art.setWerks(WERKS);

        txt_article.setText(art.getMatnr());
        if (art.getArtType() != null) {
            txt_article_type.setText(art.getArtType());
        }
        if (art.getArtSize() != null) {
            txt_article_size.setText(art.getArtSize());
        }
        // T.S.Q. = times this barcode was scanned; T.Q. = total scanned across all barcodes.
        txt_tsq.setText(Util.formatDouble(nextQty));
        txt_tq.setText(Util.formatDouble(getTotalScannedQty()));

        if (art.getArtType() == null || art.getArtType().trim().isEmpty()
                || art.getArtSize() == null || art.getArtSize().trim().isEmpty()) {
            lookupArticle(art.getMatnr());
        }

        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private boolean hasExceededBarcodeQty() {
        for (FloorBarcode art : barcodeScanMap.values()) {
            double sqty = Util.convertStringToDouble(art.getScanQty());
            double allowedQty = Util.convertStringToDouble(art.getVerme());
            if (sqty > allowedQty) {
                return true;
            }
        }
        return false;
    }

    private JSONArray getScanDataToSubmit() {
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, FloorBarcode> entry : barcodeScanMap.entrySet()) {
                FloorBarcode art = entry.getValue();
                double sqty = Util.convertStringToDouble(art.getScanQty());
                if (sqty <= 0) {
                    continue;
                }
                JSONObject itDataJson = new JSONObject();
                itDataJson.put("BARCODE", art.getBarcode() != null ? art.getBarcode() : "");
                itDataJson.put("UMREZ", art.getUmrez() != null && !art.getUmrez().trim().isEmpty() ? art.getUmrez() : "1");
                itDataJson.put("WERKS", WERKS);
                itDataJson.put("MATNR", art.getMatnr());
                itDataJson.put("LGPLA", art.getLgpla() != null ? art.getLgpla() : "");
                itDataJson.put("VERME", art.getVerme() != null ? art.getVerme() : "0");
                itDataJson.put("FLOOR_BIN", art.getFloorBin() != null ? art.getFloorBin() : "");
                itDataJson.put("SCAN_QTY", Util.formatDouble(sqty));
                itDataJson.put("ART_TYPE", art.getArtType() != null ? art.getArtType() : "");
                arrScanData.put(itDataJson);
            }
            return arrScanData;
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
        return null;
    }

    private void save() {
        if (hasExceededBarcodeQty()) {
            showBarcodeQtyExceeded();
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave == null || dataToSave.length() == 0) {
            box.getBox("Invalid", "No records to submit. Please scan some barcodes");
            return;
        }
        try {
            args.put("bapiname", Vars.ZSDC_DIRECT_ART_0008_0001_RFC);
            args.put("IM_USER", USER);
            args.put("IM_STORE_CODE", WERKS);
            args.put("ET_DATA", dataToSave);
            showProcessingAndSubmit(Vars.ZSDC_DIRECT_ART_0008_0001_RFC, REQUEST_SAVE, args);
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
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }

        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                requestInFlight = true;
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                requestInFlight = false;
                dialog.dismiss();
                AlertBox box = new AlertBox(getContext());
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
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, responsebody -> {
            requestInFlight = false;
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
                    JSONObject returnobj = parseReturnObject(responsebody);
                    if (isSapErrorReturn(returnobj)) {
                        String message = returnobj.optString("MESSAGE", "").trim();
                        if (message.isEmpty()) {
                            message = "SAP error";
                        }
                        if (request == REQUEST_VALIDATE_BARCODE) {
                            showScanError("Err", message);
                            return;
                        }
                        UIFuncs.errorSound(getContext());
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", message);
                    } else if (request == REQUEST_VALIDATE_BARCODE) {
                        setBarcodeData(responsebody);
                    } else if (request == REQUEST_SAVE) {
                        String message = returnobj != null
                                ? returnobj.optString("MESSAGE", "Saved successfully.").trim()
                                : "Saved successfully.";
                        if (message.isEmpty()) {
                            message = "Saved successfully.";
                        }
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Success", message, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clear();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
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
                return super.parseNetworkResponse(response);
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
            public void retry(VolleyError error) {
            }
        });
        mRequestQueue.add(mJsonRequest);
    }

    Response.ErrorListener volleyErrorListener() {
        return error -> {
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
            requestInFlight = false;
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", err);
        };
    }
}
