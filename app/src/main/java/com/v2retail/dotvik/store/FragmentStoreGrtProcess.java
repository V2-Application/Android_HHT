package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.text.Editable;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.google.gson.Gson;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.material.ETPACKMAT;
import com.v2retail.util.AlertBox;
import com.v2retail.util.PlantNames;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;
import com.v2retail.util.Util;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Store GRT Process screen.
 */
public class FragmentStoreGrtProcess extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentStoreGrtProcess.class.getName();

    private static final String SOURCE_0001 = "0001";
    private static final String SOURCE_0006 = "0006";

    private static final String PICKLIST_HINT = "Select Picklist No.";
    private static final String PACKING_HINT = "Select Packing Material";

    private static final String LGNUM = "V2R";

    private static final int REQUEST_GET_PICKLIST = 2001;
    private static final int REQUEST_VALIDATE_ARTICLE = 2002;
    private static final int REQUEST_GET_PACKING = 2003;
    private static final int REQUEST_GET_STOCK = 2004;
    private static final int REQUEST_SAVE = 2005;

    View rootView;
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    int activeRequests = 0;
    FragmentManager fm;

    String URL = "";
    String WERKS = "";
    String USER = "";
    String tvsPrinter = "";

    LinearLayout source0001, source0006;
    Spinner spinnerPicklistNo, spinnerPackingMaterial;
    EditText txtExternalHu, txtFdesPlant, txtArticle, txtScanQty;
    Button btnCancel, btnReset, btnSubmit;

    String selectedSource = SOURCE_0001;

    // picklistNo -> (MAJ_CAT -> pending qty)
    Map<String, Map<String, Double>> picklistCategoryPend = new HashMap<>();
    // picklistNo -> F_DES_SITE (destination plant, IM_WERKS_DES)
    Map<String, String> picklistDesSite = new HashMap<>();
    // picklistNo -> D_HUB (destination PTL hub)
    Map<String, String> picklistDesHub = new HashMap<>();
    // picklistNo -> D_NAME (destination name)
    Map<String, String> picklistDesName = new HashMap<>();
    /** Source site name from RFC export ES_S_NAME (NAME1). */
    String sourceSiteName = "";
    // MAJ_CAT (for the currently selected picklist) -> scanned qty so far
    Map<String, Double> scannedQtyByCategory = new HashMap<>();
    // packing material records (index aligns with spinner items, minus the hint at position 0)
    List<ETPACKMAT> packMaterialRecords = new ArrayList<>();
    String lastScannedMatnr = "";
    String lastScannedEan11 = "";
    String lastScannedCategory = "";
    Map<String, ScannedGrtLine> scannedLines = new HashMap<>();
    boolean isArticleValidating = false;

    private static class ScannedGrtLine {
        String category;
        String matnr;
        double scanQty;
    }

    public FragmentStoreGrtProcess() {
    }

    public static FragmentStoreGrtProcess newInstance() {
        return new FragmentStoreGrtProcess();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Store GRT Process");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_store_grt_process, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        tvsPrinter = data.read(Vars.TVS_PRINTER);
        PlantNames.load(URL);
        resolveTvsPrinter(data);

        source0001 = rootView.findViewById(R.id.store_grt_source_0001);
        source0006 = rootView.findViewById(R.id.store_grt_source_0006);

        spinnerPicklistNo = rootView.findViewById(R.id.store_grt_picklist_no);
        spinnerPackingMaterial = rootView.findViewById(R.id.store_grt_packing_material);
        txtExternalHu = rootView.findViewById(R.id.store_grt_external_hu);
        txtFdesPlant = rootView.findViewById(R.id.store_grt_fdes_plant);
        txtArticle = rootView.findViewById(R.id.store_grt_article);
        txtScanQty = rootView.findViewById(R.id.store_grt_scan_qty);

        btnCancel = rootView.findViewById(R.id.store_grt_btn_cancel);
        btnReset = rootView.findViewById(R.id.store_grt_btn_reset);
        btnSubmit = rootView.findViewById(R.id.store_grt_btn_submit);

        source0001.setOnClickListener(this);
        source0006.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);

        setupDropdowns();
        addPicklistSelectionListener();
        addInputEvents();
        selectSource(SOURCE_0001);
        clear();

        loadPicklistNumbers();
        loadPackingMaterials();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.store_grt_source_0001:
                selectSource(SOURCE_0001);
                break;
            case R.id.store_grt_source_0006:
                selectSource(SOURCE_0006);
                break;
            case R.id.store_grt_btn_cancel:
                box.confirmBack(fm, con);
                break;
            case R.id.store_grt_btn_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.store_grt_btn_submit:
                saveData();
                break;
        }
    }

    private void selectSource(String source) {
        selectedSource = source;
        boolean is0001 = SOURCE_0001.equals(source);
        source0001.setBackgroundResource(is0001
                ? R.drawable.bg_grt_source_selected
                : R.drawable.bg_grt_source_unselected);
        source0006.setBackgroundResource(is0001
                ? R.drawable.bg_grt_source_unselected
                : R.drawable.bg_grt_source_selected);
    }

    private void setupDropdowns() {
        List<String> picklistItems = new ArrayList<>();
        picklistItems.add(PICKLIST_HINT);
        setSpinnerData(spinnerPicklistNo, picklistItems);

        List<String> packingItems = new ArrayList<>();
        packingItems.add(PACKING_HINT);
        setSpinnerData(spinnerPackingMaterial, packingItems);
    }

    private void setSpinnerData(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(con,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void addPicklistSelectionListener() {
        spinnerPicklistNo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyPlantForSelectedPicklist();
                if (!getSelectedPicklist().isEmpty()) {
                    txtExternalHu.requestFocus();
                    UIFuncs.enableInput(con, txtExternalHu);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                txtFdesPlant.setText("");
            }
        });
    }

    private void applyPlantForSelectedPicklist() {
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            txtFdesPlant.setText("");
            return;
        }
        txtFdesPlant.setText(formatDestinationPlantDisplay(picklist));
    }

    /** Plant field: F_DES_SITE with optional D_HUB / D_NAME from picklist RFC. */
    private String formatDestinationPlantDisplay(String picklistNo) {
        String plant = picklistDesSite.get(picklistNo);
        if (plant == null) {
            plant = "";
        }
        String hub = picklistDesHub.get(picklistNo);
        String name = picklistDesName.get(picklistNo);
        if (name != null && !name.isEmpty()) {
            if (!plant.isEmpty()) {
                return plant + " - " + name;
            }
            if (hub != null && !hub.isEmpty()) {
                return hub + " - " + name;
            }
            return name;
        }
        if (hub != null && !hub.isEmpty()) {
            if (!plant.isEmpty() && !plant.equalsIgnoreCase(hub)) {
                return plant + " / " + hub;
            }
            return hub;
        }
        return plant;
    }

    /** Reads ES_S_NAME / NAME1 export (flat string or structure). */
    private static String readSapNameExport(JSONObject response, String key) {
        if (response == null || !response.has(key)) {
            return "";
        }
        try {
            Object raw = response.get(key);
            if (raw instanceof JSONObject) {
                JSONObject obj = (JSONObject) raw;
                String name = obj.optString("NAME1", obj.optString("NAME", "")).trim();
                return "null".equalsIgnoreCase(name) ? "" : name;
            }
            String text = String.valueOf(raw).trim();
            return text.isEmpty() || "null".equalsIgnoreCase(text) ? "" : text;
        } catch (JSONException e) {
            return response.optString(key, "").trim();
        }
    }

    private void clear() {
        if (spinnerPicklistNo.getAdapter() != null && spinnerPicklistNo.getAdapter().getCount() > 0) {
            spinnerPicklistNo.setSelection(0);
        }
        if (spinnerPackingMaterial.getAdapter() != null && spinnerPackingMaterial.getAdapter().getCount() > 0) {
            spinnerPackingMaterial.setSelection(0);
        }
        scannedQtyByCategory = new HashMap<>();
        scannedLines = new HashMap<>();
        isArticleValidating = false;
        lastScannedMatnr = "";
        lastScannedEan11 = "";
        lastScannedCategory = "";
        txtExternalHu.setText("");
        txtFdesPlant.setText("");
        txtArticle.setText("");
        txtScanQty.setText("0");
    }

    private void addInputEvents() {
        txtExternalHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    if (!UIFuncs.toUpperTrim(txtExternalHu).isEmpty()) {
                        focusArticleField();
                        return true;
                    }
                }
                return false;
            }
        });
        txtExternalHu.addTextChangedListener(new TextWatcher() {
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
                    focusArticleField();
                }
            }
        });

        txtArticle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txtArticle);
                    if (!value.isEmpty()) {
                        validateArticle(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txtArticle.addTextChangedListener(new TextWatcher() {
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
                    validateArticle(value);
                }
            }
        });
    }

    private String getSelectedPicklist() {
        Object selected = spinnerPicklistNo.getSelectedItem();
        if (selected == null) {
            return "";
        }
        String value = selected.toString().trim();
        return PICKLIST_HINT.equals(value) ? "" : value;
    }

    private void validateArticle(String article) {
        if (isArticleValidating) {
            return;
        }
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Select Picklist", "Please select a Picklist No. before scanning an article.");
            txtArticle.setText("");
            txtArticle.requestFocus();
            return;
        }

        isArticleValidating = true;
        UIFuncs.disableInput(con, txtArticle);
        getStockData(article);
    }

    private void finishArticleValidation() {
        isArticleValidating = false;
        finishRequest();
    }

    private void getStockData(String ean11) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_GET_STOCK);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGORT", selectedSource);
            args.put("IM_EAN11", ean11);
            args.put("IM_STOCK_TAKE", "X");
            showProcessingAndSubmit(Vars.ZWM_STORE_GET_STOCK, REQUEST_GET_STOCK, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void handleStockData(JSONObject response) {
        try {
            JSONArray arrEanData = response.optJSONArray("ET_EAN_DATA");
            if (arrEanData == null || arrEanData.length() == 0) {
                UIFuncs.errorSound(con);
                box.getBox("Err", "No article data found for this scan.");
                finishArticleValidation();
                resetArticleInput();
                return;
            }

            String ean11 = "";
            String matnr = "";
            for (int i = 0; i < arrEanData.length(); i++) {
                JSONObject row = arrEanData.optJSONObject(i);
                if (row == null || isEanHeaderRow(row)) {
                    continue;
                }
                ean11 = row.optString("EAN11", "").trim();
                matnr = row.optString("MATNR", "").trim();
                if (!ean11.isEmpty() || !matnr.isEmpty()) {
                    break;
                }
            }

            if (ean11.isEmpty() && matnr.isEmpty()) {
                UIFuncs.errorSound(con);
                box.getBox("Err", "Invalid article scan.");
                finishArticleValidation();
                resetArticleInput();
                return;
            }

            lastScannedEan11 = ean11;
            lastScannedMatnr = matnr;

            if (!lastScannedMatnr.isEmpty()) {
                txtArticle.setText(UIFuncs.removeLeadingZeros(lastScannedMatnr));
            } else if (!lastScannedEan11.isEmpty()) {
                txtArticle.setText(lastScannedEan11);
            }

            requestMajCatValidation();
        } catch (Exception exce) {
            box.getErrBox(exce);
            finishArticleValidation();
            resetArticleInput();
        }
    }

    private String getArticleIdentifierForValidation() {
        if (!lastScannedEan11.isEmpty()) {
            return lastScannedEan11;
        }
        if (!lastScannedMatnr.isEmpty()) {
            return UIFuncs.removeLeadingZeros(lastScannedMatnr);
        }
        return "";
    }

    private void requestMajCatValidation() {
        String picklist = getSelectedPicklist();
        String articleId = getArticleIdentifierForValidation();
        if (picklist.isEmpty() || articleId.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Unable to validate article.");
            finishArticleValidation();
            resetArticleInput();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_MAJ_CAT_VAL_RFC);
            args.put("IM_EANNR", articleId);
            args.put("IM_PICKLIST", picklist);
            chainSubmitRequest(Vars.ZWM_ST_GRT_MAJ_CAT_VAL_RFC, REQUEST_VALIDATE_ARTICLE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            finishArticleValidation();
            box.getErrBox(e);
        }
    }

    /** Chains after stock RFC — reuses active loading state, no extra delay. */
    private void chainSubmitRequest(String rfc, int request, JSONObject args) {
        try {
            submitRequest(rfc, request, args);
        } catch (Exception e) {
            finishArticleValidation();
            box.getErrBox(e);
        }
    }

    private static boolean isEanHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String matnr = row.optString("MATNR", "").trim();
        String ean11 = row.optString("EAN11", "").trim();
        return "MATNR".equalsIgnoreCase(matnr) || "EAN11".equalsIgnoreCase(ean11);
    }

    private static String categoryFromReturn(JSONObject returnobj) {
        if (returnobj == null) {
            return "";
        }
        String category = returnobj.optString("MESSAGE_V1", "").trim();
        if (!category.isEmpty()) {
            return category;
        }
        String message = returnobj.optString("MESSAGE", "").trim();
        int idx = message.lastIndexOf(':');
        if (idx >= 0 && idx < message.length() - 1) {
            return message.substring(idx + 1).trim();
        }
        return message;
    }

    private void handleArticleCategory(String category) {
        category = category != null ? category.trim() : "";
        if (category.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Could not determine the article category.");
            finishArticleValidation();
            resetArticleInput();
            return;
        }

        String picklist = getSelectedPicklist();
        Map<String, Double> catMap = picklistCategoryPend.get(picklist);
        Double pend = (catMap != null) ? catMap.get(category.toUpperCase()) : null;
        if (pend == null) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "No pending quantity found for category " + category
                    + " in picklist " + picklist + ".");
            finishArticleValidation();
            resetArticleInput();
            return;
        }

        double current = scannedQtyByCategory.containsKey(category.toUpperCase())
                ? scannedQtyByCategory.get(category.toUpperCase()) : 0;
        double proposed = current + 1;

        if (proposed > pend) {
            UIFuncs.errorSound(con);
            box.getBox("Limit Reached", "Scanned quantity cannot exceed pending quantity ("
                    + Util.formatDouble(pend) + ") for category " + category + ".");
            scannedQtyByCategory.put(category.toUpperCase(), pend);
            txtScanQty.setText(Util.formatDouble(pend));
            finishArticleValidation();
            resetArticleInput();
            return;
        }

        scannedQtyByCategory.put(category.toUpperCase(), proposed);
        lastScannedCategory = category;

        ScannedGrtLine line = scannedLines.get(category.toUpperCase());
        if (line == null) {
            line = new ScannedGrtLine();
            line.category = category;
        }
        if (!lastScannedMatnr.isEmpty()) {
            line.matnr = lastScannedMatnr;
        }
        line.scanQty = proposed;
        scannedLines.put(category.toUpperCase(), line);

        txtScanQty.setText(Util.formatDouble(proposed));
        finishArticleValidation();
        resetArticleInput();
    }

    private String getSelectedPackMatNr() {
        int index = spinnerPackingMaterial.getSelectedItemPosition();
        if (index <= 0 || index - 1 >= packMaterialRecords.size()) {
            return "";
        }
        ETPACKMAT pack = packMaterialRecords.get(index - 1);
        return pack != null && pack.getMATNR() != null ? pack.getMATNR().trim() : "";
    }

    private void saveData() {
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please select a Picklist No.");
            return;
        }

        String packMat = getSelectedPackMatNr();
        if (packMat.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please select Packing Material.");
            return;
        }

        String werksDes = picklistDesSite.get(picklist);
        if (werksDes == null || werksDes.isEmpty()) {
            werksDes = UIFuncs.toUpperTrim(txtFdesPlant);
        }
        if (werksDes.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please select Picklist with destination plant.");
            return;
        }

        if (scannedLines.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan at least one article before save.");
            txtArticle.requestFocus();
            return;
        }

        String category = lastScannedCategory;
        if (category.isEmpty()) {
            for (ScannedGrtLine line : scannedLines.values()) {
                if (line.category != null && !line.category.isEmpty()) {
                    category = line.category;
                    break;
                }
            }
        }
        if (category.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "No category found for scanned articles.");
            return;
        }

        JSONArray arrItData = buildItData();
        if (arrItData == null || arrItData.length() == 0) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "No scan data to submit.");
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_GRT_FROM_DISP_AREA);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGORT_SRC", selectedSource);
            args.put("IM_WERKS_DES", werksDes);
            args.put("IM_USER", USER);
            args.put("IM_PACK_MAT", packMat);
            args.put("IM_CATEGORY", category);
            args.put("IT_DATA", arrItData);
            showProcessingAndSubmit(Vars.ZWM_STORE_GRT_FROM_DISP_AREA, REQUEST_SAVE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private JSONArray buildItData() {
        try {
            JSONArray arrItData = new JSONArray();
            String huNo = UIFuncs.toUpperTrim(txtExternalHu);
            for (ScannedGrtLine line : scannedLines.values()) {
                if (line.scanQty <= 0 || line.matnr == null || line.matnr.isEmpty()) {
                    continue;
                }
                JSONObject itDataJson = new JSONObject();
                itDataJson.put("MATERIAL", line.matnr);
                itDataJson.put("SCAN_QTY", line.scanQty);
                itDataJson.put("WM_NO", "");
                itDataJson.put("PLANT", "");
                itDataJson.put("STOR_LOC", "");
                itDataJson.put("BATCH", "");
                itDataJson.put("CRATE", "");
                itDataJson.put("BIN", "");
                itDataJson.put("STORAGE_TYPE", "");
                itDataJson.put("MEINS", "");
                itDataJson.put("AVL_STOCK", "");
                itDataJson.put("OPEN_STOCK", "");
                itDataJson.put("PICNR", "");
                itDataJson.put("PICK_QTY", "");
                itDataJson.put("HU_NO", huNo);
                itDataJson.put("BARCODE", "");
                itDataJson.put("MATKL", "");
                itDataJson.put("WGBEZ", "");
                itDataJson.put("SONUM", "");
                itDataJson.put("DELNUM", "");
                itDataJson.put("POSNR", "");
                itDataJson.put("GNATURE", "");
                arrItData.put(itDataJson);
            }
            return arrItData;
        } catch (JSONException e) {
            box.getErrBox(e);
            return null;
        }
    }

    private void focusArticleField() {
        txtArticle.requestFocus();
        UIFuncs.enableInput(con, txtArticle);
    }

    private void resetArticleInput() {
        txtArticle.setText("");
        focusArticleField();
    }

    private void resolveTvsPrinter(SharedPreferencesData data) {
        resolveTvsPrinterForPrint(data);
    }

    /** Resolves paired TVS printer name (exact match, then 4B-2033* prefix). */
    private boolean resolveTvsPrinterForPrint(SharedPreferencesData data) {
        TSPLPrinter helper = new TSPLPrinter(con);
        if (tvsPrinter != null && !tvsPrinter.isEmpty() && helper.findBluetoothPrinter(tvsPrinter, false)) {
            tvsPrinter = helper.getPrinterName();
            if (tvsPrinter != null && !tvsPrinter.isEmpty()) {
                data.write(Vars.TVS_PRINTER, tvsPrinter);
            }
            return true;
        }
        if (helper.findBluetoothPrinter("4B-2033", true)) {
            tvsPrinter = helper.getPrinterName();
            if (tvsPrinter != null && !tvsPrinter.isEmpty()) {
                data.write(Vars.TVS_PRINTER, tvsPrinter);
            }
            return true;
        }
        return false;
    }

    private String extractHuFromSaveResponse(JSONObject response, JSONObject returnobj) {
        if (response != null) {
            String hu = readHuExport(response);
            if (!hu.isEmpty()) {
                return hu;
            }
        }
        if (returnobj != null) {
            String hu = readHuExport(returnobj);
            if (!hu.isEmpty()) {
                return hu;
            }
            hu = parseHuFromText(returnobj.optString("MESSAGE_V1", ""));
            if (!hu.isEmpty()) {
                return hu;
            }
            hu = parseHuFromText(returnobj.optString("MESSAGE", ""));
            if (!hu.isEmpty()) {
                return hu;
            }
        }
        if (response != null && response.has("ET_RETURN")) {
            try {
                Object raw = response.get("ET_RETURN");
                if (raw instanceof JSONArray) {
                    JSONArray arr = (JSONArray) raw;
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject row = arr.optJSONObject(i);
                        if (row == null) {
                            continue;
                        }
                        String hu = readHuExport(row);
                        if (!hu.isEmpty()) {
                            return hu;
                        }
                        hu = parseHuFromText(row.optString("MESSAGE_V1", ""));
                        if (!hu.isEmpty()) {
                            return hu;
                        }
                        hu = parseHuFromText(row.optString("MESSAGE", ""));
                        if (!hu.isEmpty()) {
                            return hu;
                        }
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        return "";
    }

    /** Reads HU from flat or nested SAP export (e.g. EX_HU → EXIDV). */
    private static String readHuExport(JSONObject obj) {
        if (obj == null) {
            return "";
        }
        String[] keys = {"EX_HU", "SAP_HU", "HU_NO", "EXIDV", "EX_HU_NO", "EXIDV_NEW", "HU"};
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            try {
                Object raw = obj.get(key);
                if (raw instanceof JSONObject) {
                    JSONObject nested = (JSONObject) raw;
                    String nestedHu = nested.optString("EXIDV",
                            nested.optString("SAP_HU", nested.optString("HU", ""))).trim();
                    if (!nestedHu.isEmpty()) {
                        return nestedHu;
                    }
                } else {
                    String text = String.valueOf(raw).trim();
                    if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                        return text;
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        return "";
    }

    private static String parseHuFromText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher matcher = Pattern.compile("HU\\s+([0-9A-Z]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private double getTotalScanQty() {
        double total = 0;
        for (ScannedGrtLine line : scannedLines.values()) {
            total += line.scanQty;
        }
        return total;
    }

    private void printGrtHuLabel(String huNo) {
        if (huNo == null || huNo.isEmpty()) {
            Log.w(TAG, "No HU number for label print.");
            return;
        }
        SharedPreferencesData data = new SharedPreferencesData(con);
        if (!resolveTvsPrinterForPrint(data)) {
            Log.w(TAG, "TVS printer not found; skipping label print.");
            return;
        }
        String picklist = getSelectedPicklist();
        String destPlant = picklistDesSite.get(picklist);
        if (destPlant == null) {
            destPlant = UIFuncs.toUpperTrim(txtFdesPlant);
        }
        String destHub = picklistDesHub.get(picklist);
        if (destHub == null) {
            destHub = "";
        }
        String destName = picklistDesName.get(picklist);
        if (destName == null) {
            destName = "";
        }
        String qty = Util.formatDouble(getTotalScanQty());
        String dateTime = Util.DateTime("dd.MM.yyyy HH:mm:ss", new Date());

        final String printerName = tvsPrinter;
        final String hu = huNo;
        final String srcCode = WERKS;
        final String srcName = sourceSiteName;
        final String dest = destPlant != null ? destPlant : "";
        final String hub = destHub;
        final String name = destName;
        final String qtyVal = qty;
        final String dt = dateTime;
        new Thread(() -> {
            TSPLPrinter printer = new TSPLPrinter(con, Vars.STORE_GRT_PROCESS);
            boolean printed = printer.sendStoreGrtPrintCommand(
                    printerName, hu, srcCode, srcName, dest, hub, name, qtyVal, dt);
            if (!printed) {
                Log.w(TAG, "Store GRT label print failed for HU: " + hu);
            } else {
                Log.d(TAG, "Store GRT label printed for HU: " + hu);
            }
        }).start();
    }

    private void loadPicklistNumbers() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_PICKLIST_RFC);
            args.put("SOURCE_SITE", WERKS);
            showProcessingAndSubmit(Vars.ZWM_ST_GRT_PICKLIST_RFC, REQUEST_GET_PICKLIST, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void bindPicklistNumbers(JSONObject response) {
        try {
            JSONArray arr = response.optJSONArray("ET_PICKLIST_NO");
            Set<String> picklistNos = new LinkedHashSet<>();
            picklistCategoryPend = new HashMap<>();
            picklistDesSite = new HashMap<>();
            picklistDesHub = new HashMap<>();
            picklistDesName = new HashMap<>();
            sourceSiteName = readSapNameExport(response, "ES_S_NAME");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row == null || isHeaderRow(row)) {
                        continue;
                    }
                    String picklistNo = row.optString("PICKLIST_NO", "").trim();
                    if (picklistNo.isEmpty()) {
                        continue;
                    }
                    picklistNos.add(picklistNo);

                    String fDesSite = row.optString("F_DES_SITE", "").trim();
                    if (!fDesSite.isEmpty()) {
                        picklistDesSite.put(picklistNo, fDesSite);
                    }
                    String dHub = row.optString("D_HUB", "").trim();
                    if (!dHub.isEmpty()) {
                        picklistDesHub.put(picklistNo, dHub);
                    }
                    String dName = row.optString("D_NAME", "").trim();
                    if (!dName.isEmpty()) {
                        picklistDesName.put(picklistNo, dName);
                    }

                    String majCat = row.optString("MAJ_CAT", "").trim();
                    String pendQty = row.optString("PEND_QTY", row.optString("PEND", "0"));
                    double pend = Util.convertStringToDouble(pendQty);
                    if (!majCat.isEmpty()) {
                        Map<String, Double> catMap = picklistCategoryPend.get(picklistNo);
                        if (catMap == null) {
                            catMap = new HashMap<>();
                            picklistCategoryPend.put(picklistNo, catMap);
                        }
                        catMap.put(majCat.toUpperCase(), pend);
                    }
                }
            }

            List<String> items = new ArrayList<>();
            items.add(PICKLIST_HINT);
            items.addAll(picklistNos);
            setSpinnerData(spinnerPicklistNo, items);
            applyPlantForSelectedPicklist();

            if (picklistNos.isEmpty()) {
                box.getBox("No Data", "No picklist found for this site.");
            }
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
    }

    private void loadPackingMaterials() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_PACKING_MATERIAL);
            args.put("IM_LGNUM", LGNUM);
            showProcessingAndSubmit(Vars.ZWM_GET_PACKING_MATERIAL, REQUEST_GET_PACKING, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void bindPackingMaterials(JSONObject response) {
        try {
            JSONArray arr = response.optJSONArray("ET_PACK_MAT");
            packMaterialRecords = new ArrayList<>();
            List<String> items = new ArrayList<>();
            items.add(PACKING_HINT);

            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row == null || isPackHeaderRow(row)) {
                        continue;
                    }
                    ETPACKMAT pack = new Gson().fromJson(row.toString(), ETPACKMAT.class);
                    if (pack == null) {
                        continue;
                    }
                    String maktx = pack.getMAKTX() != null ? pack.getMAKTX().trim() : "";
                    if (maktx.isEmpty()) {
                        continue;
                    }
                    packMaterialRecords.add(pack);
                    items.add(maktx);
                }
            }

            setSpinnerData(spinnerPackingMaterial, items);
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
    }

    private static boolean isPackHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String maktx = row.optString("MAKTX", "").trim();
        String matnr = row.optString("MATNR", "").trim();
        return "MAKTX".equalsIgnoreCase(maktx) || "MATNR".equalsIgnoreCase(matnr);
    }

    /**
     * Dev JSON RFC adapter returns data rows only; production SAP often prefixes
     * result tables with a column-name template row at index 0.
     */
    private static boolean isHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        return "PICKLIST_NO".equalsIgnoreCase(row.optString("PICKLIST_NO", "").trim());
    }

    /** Article validation uses ER_RETURN; picklist/packing/save use EX_RETURN (object or table). */
    private static JSONObject getReturnObject(JSONObject response, int request) throws JSONException {
        if (request == REQUEST_VALIDATE_ARTICLE) {
            JSONObject er = parseReturnNode(response, "ER_RETURN");
            if (er != null) {
                return er;
            }
        }
        JSONObject ex = parseReturnNode(response, "EX_RETURN");
        if (ex != null) {
            return ex;
        }
        return parseReturnNode(response, "ER_RETURN");
    }

    private static JSONObject parseReturnNode(JSONObject response, String key) throws JSONException {
        if (!response.has(key)) {
            return null;
        }
        Object raw = response.get(key);
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

    private void dismissDialog() {
        activeRequests = 0;
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    /** Decrement the in-flight request count and dismiss the dialog only when none remain. */
    private void finishRequest() {
        activeRequests--;
        if (activeRequests <= 0) {
            dismissDialog();
        }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        activeRequests++;
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(getContext());
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();
        }

        try {
            submitRequest(rfc, request, args);
        } catch (Exception e) {
            finishRequest();
            box.getErrBox(e);
        }
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject responsebody) {
                boolean dismissLoading = true;
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {
                    UIFuncs.errorSound(con);
                    box.getBox("Err", "No response from Server");
                    if (request == REQUEST_VALIDATE_ARTICLE || request == REQUEST_GET_STOCK) {
                        resetArticleInput();
                    }
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    if (request == REQUEST_VALIDATE_ARTICLE || request == REQUEST_GET_STOCK) {
                        resetArticleInput();
                    }
                } else {
                    try {
                        JSONObject returnobj = getReturnObject(responsebody, request);

                        if (returnobj != null && "E".equals(returnobj.optString("TYPE"))) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", returnobj.optString("MESSAGE"));
                            if (request == REQUEST_VALIDATE_ARTICLE || request == REQUEST_GET_STOCK) {
                                finishArticleValidation();
                                resetArticleInput();
                            }
                        } else if (request == REQUEST_GET_PICKLIST) {
                            bindPicklistNumbers(responsebody);
                        } else if (request == REQUEST_GET_PACKING) {
                            bindPackingMaterials(responsebody);
                        } else if (request == REQUEST_GET_STOCK) {
                            dismissLoading = false;
                            handleStockData(responsebody);
                        } else if (request == REQUEST_VALIDATE_ARTICLE) {
                            dismissLoading = false;
                            handleArticleCategory(categoryFromReturn(returnobj));
                        } else if (request == REQUEST_SAVE) {
                            String successMsg = returnobj != null
                                    ? returnobj.optString("MESSAGE", "Saved successfully.")
                                    : "Saved successfully.";
                            String huNo = extractHuFromSaveResponse(responsebody, returnobj);
                            if (!huNo.isEmpty()) {
                                printGrtHuLabel(huNo);
                            }
                            box.getBox("Success", successMsg);
                            clear();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (request == REQUEST_VALIDATE_ARTICLE || request == REQUEST_GET_STOCK) {
                            finishArticleValidation();
                            resetArticleInput();
                        }
                        box.getErrBox(e);
                    }
                }

                if (dismissLoading) {
                    if (request == REQUEST_VALIDATE_ARTICLE || request == REQUEST_GET_STOCK) {
                        finishArticleValidation();
                        if (request == REQUEST_GET_STOCK) {
                            resetArticleInput();
                        }
                    } else {
                        finishRequest();
                    }
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
            public void retry(VolleyError error) throws VolleyError {
            }
        });
        mRequestQueue.add(mJsonRequest);
    }

    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
                } else {
                    err = error.toString();
                }
                if (isArticleValidating) {
                    finishArticleValidation();
                    resetArticleInput();
                } else {
                    finishRequest();
                }
                box.getBox("Err", err);
            }
        };
    }
}
