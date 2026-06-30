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

import androidx.core.content.ContextCompat;
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
    EditText txt_store, txt_scan_hu, txt_hu_total_qty, txt_scan_barcode, txt_article, txt_article_type, txt_article_size;
    EditText txt_scan_qty, txt_trqty, txt_tqty, txt_taqty;

    String validatedHu = "";
    double huTotalQty = 0;

    // Article-level data for the validated HU, keyed by article (MATNR). Holds available qty/type/size/bin.
    Map<String, FloorBarcode> huArtDataMap = new HashMap<>();
    // EAN -> article mapping for the validated HU, keyed by scanned BARCODE. Holds MATNR + UMREZ.
    Map<String, FloorBarcode> eanArtDataMap = new HashMap<>();
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
        txt_hu_total_qty = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_hu_total_qty);
        txt_scan_barcode = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_scan_barcode);
        txt_article = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_article);
        txt_article_type = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_article_type);
        txt_article_size = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_article_size);
        txt_scan_qty = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_sqty);
        txt_trqty = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_trqty);
        txt_tqty = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_tqty);
        txt_taqty = rootView.findViewById(R.id.txt_direct_picking_v01_article_transfer_0001_taqty);

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
        huArtDataMap = new HashMap<>();
        eanArtDataMap = new HashMap<>();
        articleLookupMap = new HashMap<>();
        validatedHu = "";
        huTotalQty = 0;
        clearQtyFields();
        txt_store.setText(WERKS);
        txt_article.setText("");
        txt_article_type.setText("");
        txt_article_size.setText("");
        if (txt_hu_total_qty != null) txt_hu_total_qty.setText("");
        txt_scan_barcode.setText("");
        disableUnderlineInput(txt_scan_barcode);
        txt_scan_hu.setText("");
        enableUnderlineInput(txt_scan_hu);
        txt_scan_hu.requestFocus();
    }

    private void enableUnderlineInput(EditText view) {
        view.setBackground(ContextCompat.getDrawable(con, R.drawable.input_underline));
        view.setEnabled(true);
        view.requestFocus();
    }

    private void disableUnderlineInput(EditText view) {
        view.setBackground(ContextCompat.getDrawable(con, R.drawable.input_underline_disabled));
        view.setEnabled(false);
    }

    private void clearArticleMeta() {
        if (txt_article_type != null) txt_article_type.setText("");
        if (txt_article_size != null) txt_article_size.setText("");
        if (txt_scan_qty != null) txt_scan_qty.setText("");
        if (txt_trqty != null) txt_trqty.setText("");
        if (txt_tqty != null) txt_tqty.setText("");
    }

    private void clearQtyFields() {
        if (txt_scan_qty != null) txt_scan_qty.setText("");
        if (txt_trqty != null) txt_trqty.setText("");
        if (txt_tqty != null) txt_tqty.setText("");
        if (txt_taqty != null) txt_taqty.setText("");
    }

    private double getTotalScannedQty() {
        double total = 0;
        for (FloorBarcode art : huArtDataMap.values()) {
            total += Util.convertStringToDouble(art.getScanQty());
        }
        return total;
    }

    private void updateQtyFields(FloorBarcode art, double sqty, double totalQty) {
        txt_scan_qty.setText(Util.formatDouble(sqty));
        txt_tqty.setText(Util.formatDouble(totalQty));
        txt_trqty.setText(Util.formatDouble(totalQty - sqty));
        txt_taqty.setText(Util.formatDouble(getTotalScannedQty()));
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
        // Barcode validation is done entirely against the local maps populated on HU scan.
        updateQtyAfterScan(normalizeKey(barcode));
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
            huTotalQty = 0;
            if (txt_hu_total_qty != null) txt_hu_total_qty.setText("");
            txt_scan_hu.setText("");
            txt_scan_hu.requestFocus();
        });
    }

    private void setHuData(JSONObject responsebody) {
        String hu = UIFuncs.removeLeadingZeros(responsebody.optString("EX_HU", "").trim());
        if (hu.isEmpty()) {
            hu = UIFuncs.toUpperTrim(txt_scan_hu);
        }
        validatedHu = hu;

        huArtDataMap = new HashMap<>();
        eanArtDataMap = new HashMap<>();
        huTotalQty = 0;
        try {
            parseHuArtData(responsebody.optJSONArray("ET_HU_ART_DATA"));
            parseEanArtData(responsebody.optJSONArray("ET_EAN_ART_DATA"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (huArtDataMap.isEmpty()) {
            showHuError("No Data", "No article data returned for this HU");
            return;
        }

        txt_scan_hu.setText(hu);
        txt_hu_total_qty.setText(Util.formatDouble(huTotalQty));
        disableUnderlineInput(txt_scan_hu);
        enableUnderlineInput(txt_scan_barcode);
        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    // Parses ET_HU_ART_DATA rows (HU, QTY, MATERIAL, BIN) into huArtDataMap keyed by article (MATERIAL).
    // Available qty (VERME) is aggregated across rows of the same material; BIN is kept for the save payload.
    private void parseHuArtData(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return;
        }
        huTotalQty = 0;
        int start = SapJsonRows.startIndex(arr, "MATERIAL", "QTY", "HU");
        for (int i = start; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null || SapJsonRows.isMetadataRow(row, "MATERIAL", "QTY", "HU")) {
                continue;
            }
            String material = row.optString("MATERIAL", "").trim();
            String key = normalizeKey(material);
            if (key.isEmpty()) {
                continue;
            }
            double qty = Util.convertStringToDouble(row.optString("QTY", "0"));
            huTotalQty += qty;
            String bin = row.optString("BIN", "").trim();
            FloorBarcode art = huArtDataMap.get(key);
            if (art == null) {
                art = new FloorBarcode();
                art.setMatnr(material);
                art.setVerme(Util.formatDouble(qty));
                art.setScanQty("0");
                art.setLgpla(bin);
                huArtDataMap.put(key, art);
            } else {
                double total = Util.convertStringToDouble(art.getVerme()) + qty;
                art.setVerme(Util.formatDouble(total));
                if ((art.getLgpla() == null || art.getLgpla().trim().isEmpty()) && !bin.isEmpty()) {
                    art.setLgpla(bin);
                }
            }
        }
    }

    // Parses ET_EAN_ART_DATA rows (EAN, UMREZ, MATERIAL) into eanArtDataMap keyed by EAN.
    private void parseEanArtData(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return;
        }
        int start = SapJsonRows.startIndex(arr, "EAN", "MATERIAL", "UMREZ");
        for (int i = start; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null || SapJsonRows.isMetadataRow(row, "EAN", "MATERIAL", "UMREZ")) {
                continue;
            }
            String ean = row.optString("EAN", "").trim();
            String key = normalizeKey(ean);
            if (key.isEmpty()) {
                continue;
            }
            FloorBarcode mapping = new FloorBarcode();
            mapping.setBarcode(ean);
            mapping.setMatnr(row.optString("MATERIAL", "").trim());
            mapping.setUmrez(row.optString("UMREZ", "").trim());
            eanArtDataMap.put(key, mapping);
        }
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

        FloorBarcode art = huArtDataMap.get(aKey);
        if (art != null) {
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
        // Resolve the scanned EAN/barcode to an article using the local EAN map.
        FloorBarcode ean = eanArtDataMap.get(barcode);
        if (ean == null) {
            showScanError("Invalid", "Scanned Barcode is invalid and not part of this HU");
            return;
        }

        String matnr = normalizeKey(ean.getMatnr());
        FloorBarcode art = huArtDataMap.get(matnr);
        if (art == null) {
            showScanError("Invalid", "Article " + ean.getMatnr() + " is not available in this HU");
            return;
        }

        double sqty = Util.convertStringToDouble(art.getScanQty()) + 1;
        double rqty = Util.convertStringToDouble(art.getVerme());
        if (rqty > 0 && sqty > rqty) {
            box.getBox("Invalid", "Already scanned maximum allowed Qty " + Util.formatDouble(rqty), (dialog, which) -> {
                clearArticleMeta();
                txt_scan_barcode.setText("");
                txt_scan_barcode.requestFocus();
            });
            return;
        }

        art.setScanQty(Util.formatDouble(sqty));
        art.setBarcode(ean.getBarcode());

        txt_article.setText(art.getMatnr());
        if (art.getArtType() != null) {
            txt_article_type.setText(art.getArtType());
        }
        if (art.getArtSize() != null) {
            txt_article_size.setText(art.getArtSize());
        }
        updateQtyFields(art, sqty, rqty);

        // Fill article type/size from the lookup service if not provided in the HU data.
        if (art.getArtType() == null || art.getArtType().trim().isEmpty()
                || art.getArtSize() == null || art.getArtSize().trim().isEmpty()) {
            lookupArticle(art.getMatnr());
        }

        txt_scan_barcode.setText("");
        txt_scan_barcode.requestFocus();
    }

    private JSONArray getScanDataToSubmit() {
        try {
            JSONArray arrScanData = new JSONArray();
            for (Map.Entry<String, FloorBarcode> entry : huArtDataMap.entrySet()) {
                FloorBarcode art = entry.getValue();
                double sqty = Util.convertStringToDouble(art.getScanQty());
                if (sqty <= 0) {
                    continue;
                }
                JSONObject itDataJson = new JSONObject();
                // ET_DATA uses structure ZSDC_FLRBARCODE_ST
                itDataJson.put("BARCODE", art.getBarcode() != null ? art.getBarcode() : "");
                itDataJson.put("WERKS", WERKS);
                itDataJson.put("MATNR", art.getMatnr());
                itDataJson.put("LGPLA", art.getLgpla() != null ? art.getLgpla() : "");
                itDataJson.put("VERME", art.getVerme() != null ? art.getVerme() : "0");
                itDataJson.put("SCAN_QTY", Util.formatDouble(sqty));
                arrScanData.put(itDataJson);
            }
            return arrScanData;
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
        return null;
    }

    private void save() {
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave == null || dataToSave.length() == 0) {
            box.getBox("Invalid", "No records to submit. Please scan some barcodes");
            return;
        }
        try {
            args.put("bapiname", Vars.ZSDC_DIRECT_ART_V01_0001_RFC);
            args.put("IM_USER", USER);
            args.put("IM_STORE_CODE", WERKS);
            args.put("IM_HU", validatedHu);
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
                        UIFuncs.errorSound(getContext());
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", message);
                    } else if (request == REQUEST_VALIDATE_HU) {
                        setHuData(responsebody);
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
