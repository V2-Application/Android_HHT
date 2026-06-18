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
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
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

public class FragmentDirectPickingV01ArticleTransfer0001 extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_HU = 1600;
    private static final int REQUEST_VALIDATE_BARCODE = 1601;
    private static final int REQUEST_SAVE = 1602;

    private static final String TAG = FragmentDirectPickingV01ArticleTransfer0001.class.getName();

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
    EditText txt_store, txt_scan_hu, txt_scan_barcode, txt_article, txt_article_type, txt_article_size, txt_scan_qty;

    String validatedHu = "";

    Map<String, FloorBarcode> barcodeDataMap = new HashMap<>();
    Map<String, String[]> articleLookupMap = new HashMap<>();

    public FragmentDirectPickingV01ArticleTransfer0001() {
    }

    public static FragmentDirectPickingV01ArticleTransfer0001 newInstance() {
        return new FragmentDirectPickingV01ArticleTransfer0001();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Article Transfer from (V01 To 0001)");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_direct_picking_v01_article_transfer0001, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_store);
        txt_scan_hu = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_scan_hu);
        txt_scan_barcode = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_scan_barcode);
        txt_article = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_article);
        txt_article_type = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_article_type);
        txt_article_size = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_article_size);
        txt_scan_qty = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_sqty);

        btn_back = rootView.findViewById(R.id.btn_direct_picking_v01_article_transfer_0001_back);
        btn_save = rootView.findViewById(R.id.btn_direct_picking_v01_article_transfer_0001_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_direct_picking_v01_article_transfer_0001_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_direct_picking_v01_article_transfer_0001_save:
                save();
                break;
        }
    }

    private void clear() {
        barcodeDataMap = new HashMap<>();
        articleLookupMap = new HashMap<>();
        validatedHu = "";
        txt_scan_qty.setText("");
        txt_store.setText(WERKS);
        txt_article.setText("");
        txt_article_type.setText("");
        txt_article_size.setText("");
        txt_scan_qty.setText("");
        txt_scan_barcode.setText("");
        UIFuncs.disableInput(con, txt_scan_barcode);
        txt_scan_hu.setText("");
        UIFuncs.enableInput(con, txt_scan_hu);
        txt_scan_hu.requestFocus();
    }

    private void clearArticleMeta() {
        if (txt_article_type != null) txt_article_type.setText("");
        if (txt_article_size != null) txt_article_size.setText("");
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

    private void addInputEvents() {
        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty()) {
                        validateHu(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_hu.addTextChangedListener(new TextWatcher() {
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
                    validateHu(value);
                }
            }
        });
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
        if (barcodeDataMap.containsKey(barcode)) {
            updateQtyAfterScan(UIFuncs.toUpperTrim(txt_scan_barcode));
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
        } else {
            JSONObject args = new JSONObject();
            try {
                String rfc = Vars.ZSDC_DIRECT_ARTICLE_VAL_RFC;
                args.put("bapiname", rfc);
                args.put("IM_USER", USER);
                args.put("IM_STORE_CODE", WERKS);
                args.put("IM_BARCODE", barcode);
                args.put("IM_HU", validatedHu);
                showProcessingAndSubmit(rfc, REQUEST_VALIDATE_BARCODE, args);
            } catch (JSONException e) {
                e.printStackTrace();
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                requestInFlight = false;
                AlertBox box = new AlertBox(getContext());
                box.getErrBox(e);
            }
        }
    }

    public void validateHu(String hu) {
        if (requestInFlight) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            String rfc = Vars.ZSDC_DIRECT_HU_VALIDATE_RFC;
            args.put("bapiname", rfc);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(rfc, REQUEST_VALIDATE_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            requestInFlight = false;
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void showHuError(String title, String message) {
        UIFuncs.errorSound(getContext());
        AlertBox ab = new AlertBox(getContext());
        ab.getBox(title, message, (dialog, which) -> {
            txt_scan_hu.setText("");
            txt_scan_hu.requestFocus();
        });
    }

    private void setHuData(JSONObject responsebody) {
        String hu = responsebody.optString("EX_HU", "").trim();
        if (hu.isEmpty()) {
            hu = UIFuncs.toUpperTrim(txt_scan_hu);
        }
        validatedHu = hu;
        txt_scan_hu.setText(hu);
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.enableInput(con, txt_scan_barcode);
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private static String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase();
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

    private void lookupArticle(String articleNo, String barcodeKey) {
        String articleKey = normalizeKey(articleNo);
        String bKey = normalizeKey(barcodeKey);
        if (articleKey.isEmpty()) {
            return;
        }

        if (articleLookupMap.containsKey(articleKey)) {
            applyArticleLookup(articleKey, bKey);
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
                            applyArticleLookup(articleKey, bKey);
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

    private void applyArticleLookup(String articleKey, String barcodeKey) {
        String aKey = normalizeKey(articleKey);
        String bKey = normalizeKey(barcodeKey);
        String[] values = articleLookupMap.get(aKey);
        if (values == null) {
            return;
        }

        if (values.length > 1) {
            values[1] = mapArticleSize(values[1]);
        }

        FloorBarcode barcodeData = barcodeDataMap.get(bKey);
        if (barcodeData != null) {
            if (values[0] != null && !values[0].isEmpty()) barcodeData.setArtType(values[0]);
            if (values[1] != null && !values[1].isEmpty()) barcodeData.setArtSize(values[1]);
        }

        String currentArticle = normalizeKey(UIFuncs.toUpperTrim(txt_article));
        if (!currentArticle.isEmpty() && currentArticle.equals(aKey)) {
            if (values[0] != null && !values[0].isEmpty()) txt_article_type.setText(values[0]);
            if (values[1] != null && !values[1].isEmpty()) txt_article_size.setText(values[1]);
        }
    }

    private static boolean isBarcodeEtDataHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String barcode = row.optString("BARCODE", "").trim();
        String matnr = row.optString("MATNR", "").trim();
        String umrez = row.optString("UMREZ", "").trim();
        if ("BARCODE".equalsIgnoreCase(barcode) || "MATNR".equalsIgnoreCase(matnr) || "UMREZ".equalsIgnoreCase(umrez)) {
            return true;
        }
        if (barcode.contains("International Article Number")) {
            return true;
        }
        if (matnr.equalsIgnoreCase("Material Number")) {
            return true;
        }
        return umrez.contains("Numerator for Conversion");
    }

    private static int barcodeEtDataStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return 0;
        }
        return isBarcodeEtDataHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
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

    private boolean addBarcodeRecord(JSONObject row) {
        if (row == null || isBarcodeEtDataHeaderRow(row)) {
            return false;
        }
        FloorBarcode barcodeData = new Gson().fromJson(row.toString(), FloorBarcode.class);
        if (barcodeData == null || barcodeData.getBarcode() == null || barcodeData.getBarcode().trim().isEmpty()) {
            return false;
        }
        barcodeDataMap.put(barcodeData.getBarcode().trim().toUpperCase(), barcodeData);
        return true;
    }

    public void setBarcodeData(JSONObject responsebody) {
        try {
            boolean anyRecord = false;
            JSONArray etDataArray = responsebody.optJSONArray("ET_DATA");
            if (etDataArray != null && etDataArray.length() > 0) {
                int start = barcodeEtDataStartIndex(etDataArray);
                for (int i = start; i < etDataArray.length(); i++) {
                    if (addBarcodeRecord(etDataArray.getJSONObject(i))) {
                        anyRecord = true;
                    }
                }
            }
            if (!anyRecord && responsebody.has("EX_BARCODE")
                    && responsebody.get("EX_BARCODE") instanceof JSONObject) {
                anyRecord = addBarcodeRecord(responsebody.getJSONObject("EX_BARCODE"));
            }
            if (anyRecord) {
                String scannedBarcode = UIFuncs.toUpperTrim(txt_scan_barcode);
                FloorBarcode scannedData = barcodeDataMap.get(normalizeKey(scannedBarcode));
                if (scannedData != null) {
                    lookupArticle(scannedData.getMatnr(), scannedBarcode);
                }
                updateQtyAfterScan(scannedBarcode);
            } else {
                box.getBox("No Records", "No records returned by the server", (dialog, which) -> {
                    clearArticleMeta();
                    txt_scan_barcode.setText("");
                    txt_scan_barcode.requestFocus();
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private void updateQtyAfterScan(String barcode) {
        if (barcodeDataMap.containsKey(barcode)) {
            FloorBarcode barcodeData = barcodeDataMap.get(barcode);
            double sqty = Util.convertStringToDouble(barcodeData.getScanQty());
            double rqty = Util.convertStringToDouble(barcodeData.getVerme());
            double umrez = Util.convertStringToDouble(barcodeData.getUmrez());
            if (umrez <= 0) umrez = 1;
            sqty = sqty + umrez;
            if (sqty > rqty) {
                box.getBox("Invalid", "Already scanned maximum allowed Qty " + rqty, (dialog, which) -> {
                    clearArticleMeta();
                    txt_scan_barcode.setText("");
                    txt_scan_barcode.requestFocus();
                });
                return;
            }
            txt_article.setText(barcodeData.getMatnr());
            if (barcodeData.getArtType() != null) {
                txt_article_type.setText(barcodeData.getArtType());
            }
            if (barcodeData.getArtSize() != null) {
                txt_article_size.setText(barcodeData.getArtSize());
            }
            txt_scan_qty.setText(Util.formatDouble(sqty));
            barcodeData.setScanQty(Util.formatDouble(sqty));
            return;
        }
        box.getBox("Invalid", "Scanned Barcode is invalid and not available in Records", (dialog, which) -> {
            clearArticleMeta();
            txt_scan_barcode.setText("");
            txt_scan_barcode.requestFocus();
        });
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private JSONArray getScanDataToSubmit() {
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, FloorBarcode> floorBarcodeEntry : barcodeDataMap.entrySet()) {
                String scanDataJsonString = new Gson().toJson(floorBarcodeEntry.getValue());
                JSONObject itDataJson = new JSONObject(scanDataJsonString);
                arrScanData.put(itDataJson);
            }
            return arrScanData;
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
        return null;
    }

    private void save() {
        if (barcodeDataMap.size() == 0) {
            box.getBox("Invalid", "No records to submit. Please scan some barcodes");
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave != null) {
            try {
                args.put("bapiname", Vars.ZSDC_DIRECT_ART_V01_0001_RFC);
                args.put("IM_USER", USER);
                args.put("IM_STORE_CODE", WERKS);
                args.put("ET_DATA", dataToSave);
                showProcessingAndSubmit(Vars.ZSDC_DIRECT_ART_V01_0001_RFC, REQUEST_SAVE, args);
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
                        if (request == REQUEST_VALIDATE_HU) {
                            showHuError("Err", message);
                            return;
                        }
                        if (request == REQUEST_VALIDATE_BARCODE) {
                            showScanError("Err", message);
                            return;
                        }
                        UIFuncs.errorSound(getContext());
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", message);
                    } else if (request == REQUEST_VALIDATE_HU) {
                        setHuData(responsebody);
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
