package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentScanHuArticle extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentScanHuArticle.class.getSimpleName();
    private static final int REQUEST_SCAN_HU = 1;
    private static final int REQUEST_SAVE_HU = 2;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText txtScanHu;
    private EditText txtScanArticle;
    private EditText txtMat;
    private EditText txtHuQty;
    private EditText txtScanQty;
    private EditText txtDiffQty;
    private TableLayout tableItems;
    private TextView tvNoData;
    private Button btnBack;
    private Button btnSave;

    private final List<JSONObject> articleRows = new ArrayList<>();
    private final List<JSONObject> eanRows = new ArrayList<>();
    private String validatedHu = "";
    private boolean requestInFlight = false;

    public FragmentScanHuArticle() {
    }

    public static FragmentScanHuArticle newInstance() {
        return new FragmentScanHuArticle();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle("Scan Article");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan_hu_article, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtScanHu = rootView.findViewById(R.id.txt_scan_hu_article_scan_hu);
        txtScanArticle = rootView.findViewById(R.id.txt_scan_hu_article_scan_article);
        txtMat = rootView.findViewById(R.id.txt_scan_hu_article_mat);
        txtHuQty = rootView.findViewById(R.id.txt_scan_hu_article_hu_qty);
        txtScanQty = rootView.findViewById(R.id.txt_scan_hu_article_scan_qty);
        txtDiffQty = rootView.findViewById(R.id.txt_scan_hu_article_diff_qty);
        tableItems = rootView.findViewById(R.id.table_scan_hu_article_items);
        tvNoData = rootView.findViewById(R.id.tv_scan_hu_article_no_data);
        btnBack = rootView.findViewById(R.id.btn_scan_hu_article_back);
        btnSave = rootView.findViewById(R.id.btn_scan_hu_article_save);

        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        addScanHuEvents();
        addScanArticleEvents();

        txtScanHu.requestFocus();
        return rootView;
    }

    private void addScanHuEvents() {
        txtScanHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enterDown) {
                    requestScanHu(valueOf(txtScanHu));
                    return true;
                }
                return false;
            }
        });

        txtScanHu.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hu = s.toString().trim().toUpperCase(Locale.ROOT);
                if (!hu.isEmpty() && scannerReading) {
                    requestScanHu(hu);
                }
            }
        });
    }

    private void addScanArticleEvents() {
        txtScanArticle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enterDown) {
                    onArticleScanned(valueOf(txtScanArticle));
                    return true;
                }
                return false;
            }
        });

        txtScanArticle.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String article = s.toString().trim().toUpperCase(Locale.ROOT);
                if (!article.isEmpty() && scannerReading) {
                    onArticleScanned(article);
                }
            }
        });
    }

    private void requestScanHu(String scannedHu) {
        if (requestInFlight) {
            return;
        }
        if (TextUtils.isEmpty(scannedHu)) {
            box.getBox("Alert", "Scan HU Number!");
            txtScanHu.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_SCAN_HU);
            args.put("IM_HU", scannedHu);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            showProcessingAndSubmit(Vars.ZWM_SCAN_HU, REQUEST_SCAN_HU, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestScanHu", e);
            box.getErrBox(e);
        }
    }

    private void onArticleScanned(String scannedEan) {
        if (TextUtils.isEmpty(validatedHu) || articleRows.isEmpty()) {
            box.getBox("Alert", "Scan HU Number first!");
            txtScanArticle.setText("");
            txtScanHu.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(scannedEan)) {
            box.getBox("Alert", "Scan Article!");
            txtScanArticle.requestFocus();
            return;
        }

        JSONObject eanRow = findEanRow(scannedEan);
        if (eanRow == null) {
            box.getBox("Err", "Invalid Article / EAN for this HU.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        String matnr = eanRow.optString("MATNR", "").trim();
        double umrez = parseQty(eanRow.optString("UMREZ", "1"));
        if (umrez <= 0) {
            umrez = 1;
        }

        JSONObject articleRow = findArticleRow(matnr);
        if (articleRow == null) {
            box.getBox("Err", "Article not found in HU list.");
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
            return;
        }

        try {
            double huQty = parseQty(articleRow.optString("HU_QTY", "0"));
            double scanQty = parseQty(articleRow.optString("SCAN_QTY", "0"));
            double newScanQty = scanQty + umrez;

            if (newScanQty > huQty + 0.0001) {
                box.getBox("Err", "Scan quantity exceeds HU quantity.");
                txtScanArticle.setText("");
                txtScanArticle.requestFocus();
                return;
            }

            double diffQty = huQty - newScanQty;
            articleRow.put("SCAN_QTY", normalizeMenge(String.valueOf(newScanQty)));
            articleRow.put("DIFF_QTY", (int) Math.round(diffQty));

            bindArticleFields(articleRow);
            refreshArticleTable();

            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
        } catch (JSONException e) {
            Log.e(TAG, "onArticleScanned", e);
            box.getErrBox(e);
        }
    }

    private JSONObject findEanRow(String scannedEan) {
        String key = scannedEan == null ? "" : scannedEan.trim().toUpperCase(Locale.ROOT);
        String keyNoZero = stripLeadingZeros(key);
        for (JSONObject row : eanRows) {
            if (row == null) {
                continue;
            }
            String ean11 = row.optString("EAN11", "").trim().toUpperCase(Locale.ROOT);
            if (ean11.isEmpty()) {
                continue;
            }
            if (ean11.equals(key) || stripLeadingZeros(ean11).equals(keyNoZero)) {
                return row;
            }
        }
        return null;
    }

    private JSONObject findArticleRow(String matnr) {
        String key = stripLeadingZeros(matnr);
        for (JSONObject row : articleRows) {
            if (row == null) {
                continue;
            }
            if (stripLeadingZeros(row.optString("MATNR", "")).equalsIgnoreCase(key)) {
                return row;
            }
        }
        return null;
    }

    private void bindArticleFields(JSONObject articleRow) {
        if (articleRow == null) {
            clearDetailFields();
            return;
        }
        txtMat.setText(formatMatnrDisplay(articleRow.optString("MATNR", "")));
        txtHuQty.setText(formatQty(articleRow.optString("HU_QTY", "0")));
        txtScanQty.setText(formatQty(articleRow.optString("SCAN_QTY", "0")));
        txtDiffQty.setText(String.valueOf(normalizeDiffQty(articleRow.optString("DIFF_QTY", "0"))));
    }

    private void refreshArticleTable() {
        clearTableDataRows();
        for (JSONObject row : articleRows) {
            if (row != null) {
                addArticleTableRow(row);
            }
        }
        if (tvNoData != null) {
            tvNoData.setVisibility(articleRows.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void requestSaveHu() {
        if (requestInFlight) {
            return;
        }
        String hu = !TextUtils.isEmpty(validatedHu) ? validatedHu : valueOf(txtScanHu);
        if (TextUtils.isEmpty(hu)) {
            box.getBox("Alert", "Scan HU Number!");
            txtScanHu.requestFocus();
            return;
        }
        if (articleRows.isEmpty()) {
            box.getBox("Alert", "No data to save.");
            return;
        }

        double totalHuQty = 0;
        double totalScanQty = 0;
        for (JSONObject row : articleRows) {
            if (row == null) {
                continue;
            }
            totalHuQty += parseQty(row.optString("HU_QTY", "0"));
            totalScanQty += parseQty(row.optString("SCAN_QTY", "0"));
        }

        // Save RFC only after at least one article has been scanned.
        if (totalScanQty <= 0) {
            box.getBox("Alert", "Scan Article first!");
            txtScanArticle.requestFocus();
            return;
        }

        // Fully scanned → call Save RFC directly.
        if (Math.abs(totalHuQty - totalScanQty) <= 0.0001) {
            submitSaveHu(hu);
            return;
        }

        // Partial scan → confirm before Save RFC.
        String message = "HU quantity is " + formatQty(String.valueOf(totalHuQty))
                + ", but only " + formatQty(String.valueOf(totalScanQty))
                + " articles have been scanned. Do you want to continue?";
        box.getBox(
                "Confirm",
                message,
                (dialogInterface, which) -> submitSaveHu(hu),
                (dialogInterface, which) -> {
                    // Cancel: stay on screen, do not call Save RFC.
                    txtScanArticle.requestFocus();
                });
    }

    /** Calls ZWM_SAVE_HU only after explicit Save/OK confirmation. */
    private void submitSaveHu(String hu) {
        if (requestInFlight) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            JSONArray imArticles = buildImArticles();
            args.put("bapiname", Vars.ZWM_SAVE_HU);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", hu);
            args.put("IM_ARTICLES", imArticles);
            showProcessingAndSubmit(Vars.ZWM_SAVE_HU, REQUEST_SAVE_HU, args);
        } catch (JSONException e) {
            Log.e(TAG, "submitSaveHu", e);
            box.getErrBox(e);
        }
    }

    private JSONArray buildImArticles() throws JSONException {
        JSONArray arr = new JSONArray();
        for (JSONObject src : articleRows) {
            if (src == null) {
                continue;
            }
            // Send only articles that were actually scanned.
            double scanQty = parseQty(src.optString("SCAN_QTY", "0"));
            if (scanQty <= 0) {
                continue;
            }
            JSONObject row = new JSONObject();
            row.put("MATNR", src.optString("MATNR", "").trim());
            row.put("HU_QTY", normalizeMenge(src.optString("HU_QTY", "0")));
            row.put("SCAN_QTY", normalizeMenge(src.optString("SCAN_QTY", "0")));
            row.put("DIFF_QTY", normalizeDiffQty(src.optString("DIFF_QTY", "0")));
            arr.put(row);
        }
        return arr;
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        requestInFlight = true;
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    requestInFlight = false;
                    dismissDialog();
                    box.getErrBox(e);
                }
            }
        }, 500);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        final JSONObject params = args;
        Log.d(TAG, rfc + " payload -> " + params);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        requestInFlight = false;
                        dismissDialog();
                        Log.d(TAG, rfc + " response -> " + responsebody);

                        if (responsebody == null) {
                            box.getBox("Err", "No response from Server");
                        } else if (responsebody.length() == 0) {
                            box.getBox("Err", "Unable to Connect Server/ Empty Response");
                        } else if (request == REQUEST_SCAN_HU) {
                            handleScanHuResponse(responsebody);
                        } else if (request == REQUEST_SAVE_HU) {
                            handleSaveHuResponse(responsebody);
                        }
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

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonRequest);
    }

    private void handleScanHuResponse(JSONObject responsebody) {
        try {
            if (isErrorReturn(responsebody)) {
                String message = getReturnMessage(responsebody, "HU validation failed.");
                clearHuData();
                validatedHu = "";
                txtScanHu.setText("");
                txtScanHu.requestFocus();
                box.getBox("Err", message);
                return;
            }

            articleRows.clear();
            eanRows.clear();
            clearDetailFields();
            clearTableDataRows();

            JSONArray articles = responsebody.optJSONArray("ET_ATICLES");
            if (articles == null) {
                articles = new JSONArray();
            }
            int articleStart = SapJsonRows.startIndex(articles, "MATNR", "HU_QTY", "SCAN_QTY", "DIFF_QTY");
            for (int i = articleStart; i < articles.length(); i++) {
                JSONObject row = articles.optJSONObject(i);
                if (row == null || SapJsonRows.isMetadataRow(row, "MATNR", "HU_QTY", "SCAN_QTY", "DIFF_QTY")) {
                    continue;
                }
                String matnr = row.optString("MATNR", "").trim();
                if (matnr.isEmpty()) {
                    continue;
                }
                articleRows.add(row);
                addArticleTableRow(row);
            }

            JSONArray eans = responsebody.optJSONArray("ET_EAN");
            if (eans == null) {
                eans = new JSONArray();
            }
            int eanStart = SapJsonRows.startIndex(eans, "MATNR", "EAN11", "UMREZ");
            for (int i = eanStart; i < eans.length(); i++) {
                JSONObject row = eans.optJSONObject(i);
                if (row == null || SapJsonRows.isMetadataRow(row, "MATNR", "EAN11")) {
                    continue;
                }
                String ean11 = row.optString("EAN11", "").trim();
                if (ean11.isEmpty()) {
                    continue;
                }
                eanRows.add(row);
            }

            if (articleRows.isEmpty()) {
                validatedHu = "";
                tvNoData.setVisibility(View.VISIBLE);
                box.getBox("Alert", "No article data found for this HU.");
                txtScanHu.requestFocus();
                return;
            }

            validatedHu = valueOf(txtScanHu);
            tvNoData.setVisibility(View.GONE);
            txtScanHu.setText(validatedHu);
            txtScanArticle.setText("");
            txtScanArticle.requestFocus();
        } catch (Exception e) {
            Log.e(TAG, "handleScanHuResponse", e);
            box.getErrBox(e);
        }
    }

    private void handleSaveHuResponse(JSONObject responsebody) {
        try {
            if (isErrorReturn(responsebody)) {
                String message = getReturnMessage(responsebody, "Save failed.");
                box.getBox("Err", message);
                return;
            }

            String message = getReturnMessage(responsebody, "Saved successfully.");
            box.getBox("Ok", message, (d, w) -> resetAfterSave());
        } catch (Exception e) {
            Log.e(TAG, "handleSaveHuResponse", e);
            box.getErrBox(e);
        }
    }

    private boolean isErrorReturn(JSONObject responsebody) throws JSONException {
        JSONObject returnObj = extractReturnObject(responsebody);
        if (returnObj == null) {
            return false;
        }
        String type = returnObj.optString("TYPE", "").trim();
        return "E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type);
    }

    private String getReturnMessage(JSONObject responsebody, String fallback) throws JSONException {
        JSONObject returnObj = extractReturnObject(responsebody);
        if (returnObj == null) {
            return fallback;
        }
        String message = returnObj.optString("MESSAGE", "").trim();
        return TextUtils.isEmpty(message) ? fallback : message;
    }

    private JSONObject extractReturnObject(JSONObject responsebody) throws JSONException {
        if (responsebody == null) {
            return null;
        }

        JSONObject fromEtError = extractEtError(responsebody);
        if (fromEtError != null) {
            return fromEtError;
        }

        JSONObject esReturn = responsebody.optJSONObject("ES_RETURN");
        if (esReturn != null) {
            return esReturn;
        }

        JSONObject exReturn = responsebody.optJSONObject("EX_RETURN");
        if (exReturn != null) {
            return exReturn;
        }
        return null;
    }

    private JSONObject extractEtError(JSONObject responsebody) throws JSONException {
        if (responsebody == null || !responsebody.has("ET_ERROR")) {
            return null;
        }
        Object etError = responsebody.get("ET_ERROR");
        if (etError instanceof JSONObject) {
            return (JSONObject) etError;
        }
        if (etError instanceof JSONArray) {
            JSONArray arr = (JSONArray) etError;
            int start = SapJsonRows.startIndex(arr, "TYPE", "MESSAGE", "NUMBER");
            for (int i = start; i < arr.length(); i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row == null || SapJsonRows.isMetadataRow(row, "TYPE", "MESSAGE", "NUMBER")) {
                    continue;
                }
                return row;
            }
        }
        return null;
    }

    private void addArticleTableRow(JSONObject row) {
        TableRow tr = new TableRow(con);

        tr.addView(makeCell(formatMatnrDisplay(row.optString("MATNR", "")), 5f));
        tr.addView(makeCell(formatQty(row.optString("HU_QTY", "0")), 1f));
        tr.addView(makeCell(formatQty(row.optString("SCAN_QTY", "0")), 1f));
        tr.addView(makeCell(row.optString("DIFF_QTY", "0"), 1f));

        tableItems.addView(tr);
    }

    private TextView makeCell(String text, float weight) {
        TextView tv = new TextView(con);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                0, TableRow.LayoutParams.MATCH_PARENT, weight);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(6, 8, 6, 8);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(13);
        tv.setSingleLine(true);
        tv.setBackground(con.getResources().getDrawable(R.drawable.table_cell_border));
        tv.setText(text == null ? "" : text);
        return tv;
    }

    /** Display MATNR with space before last 3 digits, e.g. 1130000518001 → 1130000518 001 */
    private String formatMatnrDisplay(String matnr) {
        String value = stripLeadingZeros(matnr);
        if (value.length() > 3) {
            return value.substring(0, value.length() - 3) + " " + value.substring(value.length() - 3);
        }
        return value;
    }

    private void clearTableDataRows() {
        if (tableItems == null) {
            return;
        }
        tableItems.removeAllViews();
    }

    private void clearDetailFields() {
        txtMat.setText("");
        txtHuQty.setText("");
        txtScanQty.setText("");
        txtDiffQty.setText("");
    }

    private void clearHuData() {
        articleRows.clear();
        eanRows.clear();
        clearDetailFields();
        clearTableDataRows();
        if (tvNoData != null) {
            tvNoData.setVisibility(View.VISIBLE);
        }
    }

    private void resetAfterSave() {
        validatedHu = "";
        clearHuData();
        txtScanHu.setText("");
        txtScanArticle.setText("");
        txtScanHu.requestFocus();
    }

    private Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestInFlight = false;
                dismissDialog();
                Log.e(TAG, "RFC error", error);

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    box.getBox("Err", "Network timeout. Please try again.");
                } else if (error instanceof NetworkError) {
                    box.getBox("Err", "Network error. Please check connection.");
                } else if (error instanceof ServerError) {
                    box.getBox("Err", "Server error. Please try again.");
                } else if (error instanceof ParseError) {
                    box.getBox("Err", "Invalid server response.");
                } else {
                    box.getBox("Err", "Unable to process request.");
                }
            }
        };
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private String valueOf(EditText field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim().toUpperCase(Locale.ROOT);
    }

    private String stripLeadingZeros(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceFirst("^0+(?!$)", "");
    }

    private String formatQty(String qty) {
        if (qty == null || qty.trim().isEmpty()) {
            return "0";
        }
        try {
            double d = Double.parseDouble(qty.trim());
            if (d == Math.rint(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        } catch (NumberFormatException e) {
            return qty.trim();
        }
    }

    private double parseQty(String qty) {
        if (qty == null || qty.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(qty.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String normalizeMenge(String qty) {
        if (qty == null || qty.trim().isEmpty()) {
            return "0.000";
        }
        try {
            double d = Double.parseDouble(qty.trim());
            return String.format(Locale.US, "%.3f", d);
        } catch (NumberFormatException e) {
            return qty.trim();
        }
    }

    private int normalizeDiffQty(String qty) {
        if (qty == null || qty.trim().isEmpty()) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(qty.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_scan_hu_article_back) {
            if (fm != null) {
                fm.popBackStack();
            }
        } else if (view.getId() == R.id.btn_scan_hu_article_save) {
            requestSaveHu();
        }
    }
}
