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
    private static final int REQUEST_GET_PICKLIST_DATA = 2002;
    private static final int REQUEST_GET_PACKING = 2003;
    private static final int REQUEST_VALIDATE_EXHU = 2004;
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
    String lastScannedCategory = "";
    Map<String, ScannedGrtLine> scannedLines = new HashMap<>();
    /** MATNR → picklist article row from ET_DATA. */
    Map<String, PicklistArticleLine> picklistArticlesByMatnr = new HashMap<>();
    /** EAN11 or MATNR scan key → EAN row from ET_EAN_DATA. */
    Map<String, EanRecord> eanByScanCode = new HashMap<>();
    /** Destination plant from EX_RDC after picklist data load. */
    String picklistRdcPlant = "";
    /** True after ZWM_ST_GRT_EXHU_VALIDATION succeeds for the current External HU scan. */
    boolean externalHuValidated = false;

    private static class PicklistArticleLine {
        String picklistNo;
        String source;
        String majCat;
        String size1;
        String floor;
        String bgt;
        String matnr;
        String matkl;
    }

    private static class EanRecord {
        String matnr;
        String ean11;
        double umrez;
    }

    private static class ScannedGrtLine {
        String category;
        String matnr;
        String ean11;
        String matkl;
        String size1;
        String floor;
        String bgt;
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
                resetPicklistScanState();
                applyPlantForSelectedPicklist();
                String picklist = getSelectedPicklist();
                if (!picklist.isEmpty()) {
                    loadPicklistData(picklist);
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
        if (picklistRdcPlant != null && !picklistRdcPlant.isEmpty()) {
            txtFdesPlant.setText(picklistRdcPlant);
            return;
        }
        String fDesSite = picklistDesSite.get(picklist);
        txtFdesPlant.setText(fDesSite != null ? fDesSite : "");
    }

    private void resetPicklistScanState() {
        picklistArticlesByMatnr = new HashMap<>();
        eanByScanCode = new HashMap<>();
        picklistRdcPlant = "";
        scannedQtyByCategory = new HashMap<>();
        scannedLines = new HashMap<>();
        lastScannedCategory = "";
        externalHuValidated = false;
        txtScanQty.setText("0");
        txtArticle.setText("");
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
        resetPicklistScanState();
        externalHuValidated = false;
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
                    String value = UIFuncs.toUpperTrim(txtExternalHu);
                    if (!value.isEmpty()) {
                        validateExternalHu(value);
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
                externalHuValidated = false;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateExternalHu(value);
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
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Select Picklist", "Please select a Picklist No. before scanning an article.");
            resetArticleInput();
            return;
        }
        if (!externalHuValidated || UIFuncs.toUpperTrim(txtExternalHu).isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate External HU before article.");
            resetExternalHuInput();
            resetArticleInput();
            return;
        }
        if (picklistArticlesByMatnr.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Picklist data not loaded. Reselect picklist and try again.");
            resetArticleInput();
            return;
        }

        EanRecord eanRec = resolveEanRecord(article);
        if (eanRec == null || eanRec.matnr == null || eanRec.matnr.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Article not allowed for this picklist.");
            resetArticleInput();
            return;
        }

        PicklistArticleLine articleLine = findArticleLine(eanRec.matnr);
        if (articleLine == null) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Article not found in picklist data.");
            resetArticleInput();
            return;
        }

        String category = articleLine.majCat != null ? articleLine.majCat.trim() : "";
        if (category.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Could not determine the article category.");
            resetArticleInput();
            return;
        }

        Map<String, Double> catMap = picklistCategoryPend.get(picklist);
        Double pend = (catMap != null) ? catMap.get(category.toUpperCase()) : null;
        if (pend == null) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "No pending quantity found for category " + category
                    + " in picklist " + picklist + ".");
            resetArticleInput();
            return;
        }

        double umrez = eanRec.umrez > 0 ? eanRec.umrez : 1;
        double categoryScanned = getCategoryScannedQty(category);
        double proposedCategoryQty = categoryScanned + umrez;
        if (proposedCategoryQty > pend) {
            UIFuncs.errorSound(con);
            box.getBox("Limit Reached", "Scanned quantity cannot exceed pending quantity ("
                    + Util.formatDouble(pend) + ") for category " + category + ".");
            resetArticleInput();
            return;
        }

        String matnrKey = articleLine.matnr.toUpperCase();
        ScannedGrtLine line = scannedLines.get(matnrKey);
        if (line == null) {
            line = new ScannedGrtLine();
            line.matnr = articleLine.matnr;
            line.category = category;
            line.ean11 = eanRec.ean11;
            line.matkl = articleLine.matkl;
            line.size1 = articleLine.size1;
            line.floor = articleLine.floor;
            line.bgt = articleLine.bgt;
            line.scanQty = 0;
        }
        line.scanQty += umrez;
        scannedLines.put(matnrKey, line);

        scannedQtyByCategory.put(category.toUpperCase(), proposedCategoryQty);
        lastScannedCategory = category;

        if (!articleLine.matnr.isEmpty()) {
            txtArticle.setText(UIFuncs.removeLeadingZeros(articleLine.matnr));
        } else if (eanRec.ean11 != null && !eanRec.ean11.isEmpty()) {
            txtArticle.setText(eanRec.ean11);
        }
        txtScanQty.setText(Util.formatDouble(proposedCategoryQty));
        resetArticleInput();
    }

    private double getCategoryScannedQty(String category) {
        if (category == null || category.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (ScannedGrtLine line : scannedLines.values()) {
            if (line.category != null && line.category.equalsIgnoreCase(category)) {
                total += line.scanQty;
            }
        }
        return total;
    }

    private EanRecord resolveEanRecord(String scan) {
        if (scan == null) {
            return null;
        }
        String upper = scan.trim().toUpperCase();
        if (upper.isEmpty()) {
            return null;
        }
        EanRecord hit = eanByScanCode.get(upper);
        if (hit != null) {
            return hit;
        }
        String noZeros = UIFuncs.removeLeadingZeros(upper);
        if (!noZeros.isEmpty()) {
            hit = eanByScanCode.get(noZeros);
            if (hit != null) {
                return hit;
            }
        }
        PicklistArticleLine line = findArticleLine(upper);
        if (line == null) {
            return null;
        }
        EanRecord fallback = new EanRecord();
        fallback.matnr = line.matnr;
        fallback.ean11 = "";
        fallback.umrez = 1;
        return fallback;
    }

    private PicklistArticleLine findArticleLine(String matnrOrScan) {
        if (matnrOrScan == null || matnrOrScan.isEmpty()) {
            return null;
        }
        String upper = matnrOrScan.trim().toUpperCase();
        PicklistArticleLine direct = picklistArticlesByMatnr.get(upper);
        if (direct != null) {
            return direct;
        }
        String noZeros = UIFuncs.removeLeadingZeros(upper);
        for (Map.Entry<String, PicklistArticleLine> entry : picklistArticlesByMatnr.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(upper)
                    || UIFuncs.removeLeadingZeros(key).equalsIgnoreCase(noZeros)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isPicklistDataHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String matnr = row.optString("MATNR", "").trim();
        String picklist = row.optString("PICKLIST_NO", "").trim();
        return "MATNR".equalsIgnoreCase(matnr) || "PICKLIST_NO".equalsIgnoreCase(picklist);
    }

    private static boolean isEanDataHeaderRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        String matnr = row.optString("MATNR", "").trim();
        String ean11 = row.optString("EAN11", "").trim();
        return "MATNR".equalsIgnoreCase(matnr) || "EAN11".equalsIgnoreCase(ean11);
    }

    private void indexEanRecord(EanRecord record) {
        if (record == null || record.matnr == null || record.matnr.isEmpty()) {
            return;
        }
        eanByScanCode.put(record.matnr.toUpperCase(), record);
        String nz = UIFuncs.removeLeadingZeros(record.matnr);
        if (!nz.isEmpty()) {
            eanByScanCode.put(nz.toUpperCase(), record);
        }
        if (record.ean11 != null && !record.ean11.isEmpty()) {
            eanByScanCode.put(record.ean11.toUpperCase(), record);
        }
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

        String werksDes = picklistRdcPlant;
        if (werksDes == null || werksDes.isEmpty()) {
            werksDes = picklistDesSite.get(picklist);
        }
        if (werksDes == null || werksDes.isEmpty()) {
            werksDes = UIFuncs.toUpperTrim(txtFdesPlant);
        }
        if (werksDes.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please select Picklist with destination plant.");
            return;
        }

        if (!externalHuValidated || UIFuncs.toUpperTrim(txtExternalHu).isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate External HU before save.");
            resetExternalHuInput();
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
            args.put("bapiname", Vars.ZWM_ST_GRT_HU_CREATION_SAVE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGORT_SRC", selectedSource);
            args.put("IM_WERKS_DES", werksDes);
            args.put("IM_USER", USER);
            args.put("IM_PACK_MAT", packMat);
            args.put("IM_CATEGORY", category);
            args.put("IT_DATA", arrItData);
            showProcessingAndSubmit(Vars.ZWM_ST_GRT_HU_CREATION_SAVE, REQUEST_SAVE, args);
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
            String picklist = getSelectedPicklist();
            for (ScannedGrtLine line : scannedLines.values()) {
                if (line.scanQty <= 0 || line.matnr == null || line.matnr.isEmpty()) {
                    continue;
                }
                JSONObject itDataJson = new JSONObject();
                itDataJson.put("MATERIAL", line.matnr);
                itDataJson.put("SCAN_QTY", line.scanQty);
                itDataJson.put("WM_NO", "");
                itDataJson.put("PLANT", WERKS);
                itDataJson.put("STOR_LOC", selectedSource);
                itDataJson.put("BATCH", "");
                itDataJson.put("CRATE", "");
                itDataJson.put("BIN", "");
                itDataJson.put("STORAGE_TYPE", "");
                itDataJson.put("MEINS", "EA");
                itDataJson.put("AVL_STOCK", "");
                itDataJson.put("OPEN_STOCK", "");
                itDataJson.put("PICNR", picklist);
                itDataJson.put("PICK_QTY", "");
                itDataJson.put("HU_NO", huNo);
                itDataJson.put("BARCODE", line.ean11 != null ? line.ean11 : "");
                itDataJson.put("MATKL", line.matkl != null ? line.matkl : "");
                itDataJson.put("WGBEZ", line.size1 != null ? line.size1 : "");
                itDataJson.put("SONUM", "");
                itDataJson.put("DELNUM", "");
                itDataJson.put("POSNR", "");
                itDataJson.put("GNATURE", line.category != null ? line.category : "");
                arrItData.put(itDataJson);
            }
            return arrItData;
        } catch (JSONException e) {
            box.getErrBox(e);
            return null;
        }
    }

    private void validateExternalHu(String hu) {
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Select Picklist", "Please select a Picklist No. before scanning External HU.");
            resetExternalHuInput();
            return;
        }
        if (hu == null || hu.isEmpty()) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_EXHU_VALIDATION);
            args.put("IM_PLANT", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZWM_ST_GRT_EXHU_VALIDATION, REQUEST_VALIDATE_EXHU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void resetExternalHuInput() {
        externalHuValidated = false;
        txtExternalHu.setText("");
        txtExternalHu.requestFocus();
        UIFuncs.enableInput(con, txtExternalHu);
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
        String destPlant = picklistRdcPlant;
        if (destPlant == null || destPlant.isEmpty()) {
            destPlant = picklistDesSite.get(picklist);
        }
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

    private void loadPicklistData(String picklistNo) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_GET_PICKLIST_DATA);
            args.put("IM_PLANT", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", picklistNo);
            showProcessingAndSubmit(Vars.ZWM_ST_GRT_GET_PICKLIST_DATA, REQUEST_GET_PICKLIST_DATA, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void bindPicklistData(JSONObject response) {
        try {
            picklistArticlesByMatnr = new HashMap<>();
            eanByScanCode = new HashMap<>();

            String exRdc = response.optString("EX_RDC", "").trim();
            if (!exRdc.isEmpty() && !"null".equalsIgnoreCase(exRdc)) {
                picklistRdcPlant = exRdc;
                applyPlantForSelectedPicklist();
            }

            JSONArray etData = response.optJSONArray("ET_DATA");
            if (etData != null) {
                for (int i = 0; i < etData.length(); i++) {
                    JSONObject row = etData.optJSONObject(i);
                    if (row == null || isPicklistDataHeaderRow(row)) {
                        continue;
                    }
                    String matnr = row.optString("MATNR", "").trim();
                    if (matnr.isEmpty()) {
                        continue;
                    }
                    PicklistArticleLine line = new PicklistArticleLine();
                    line.picklistNo = row.optString("PICKLIST_NO", "").trim();
                    line.source = row.optString("SOUR", row.optString("SOURCE", WERKS)).trim();
                    line.majCat = row.optString("MAJ_CAT", "").trim();
                    line.size1 = row.optString("SIZE1", "").trim();
                    line.floor = row.optString("FLOOR", "").trim();
                    line.bgt = row.optString("BGT", "").trim();
                    line.matnr = matnr;
                    line.matkl = row.optString("MATKL", "").trim();
                    picklistArticlesByMatnr.put(matnr.toUpperCase(), line);
                }
            }

            JSONArray etEanData = response.optJSONArray("ET_EAN_DATA");
            if (etEanData != null) {
                for (int i = 0; i < etEanData.length(); i++) {
                    JSONObject row = etEanData.optJSONObject(i);
                    if (row == null || isEanDataHeaderRow(row)) {
                        continue;
                    }
                    String matnr = row.optString("MATNR", "").trim();
                    if (matnr.isEmpty()) {
                        continue;
                    }
                    EanRecord record = new EanRecord();
                    record.matnr = matnr;
                    record.ean11 = row.optString("EAN11", "").trim();
                    record.umrez = Util.convertStringToDouble(row.optString("UMREZ", "1"));
                    if (record.umrez <= 0) {
                        record.umrez = 1;
                    }
                    indexEanRecord(record);
                }
            }

            if (picklistArticlesByMatnr.isEmpty()) {
                UIFuncs.errorSound(con);
                box.getBox("No Data", "No picklist article data returned.");
            }
        } catch (Exception exce) {
            box.getErrBox(exce);
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

    /** Picklist/packing/save use EX_RETURN (object or table). */
    private static JSONObject getReturnObject(JSONObject response, int request) throws JSONException {
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
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                } else {
                    try {
                        JSONObject returnobj = getReturnObject(responsebody, request);

                        if (returnobj != null && "E".equals(returnobj.optString("TYPE"))) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", returnobj.optString("MESSAGE"));
                            if (request == REQUEST_VALIDATE_EXHU) {
                                resetExternalHuInput();
                            }
                        } else if (request == REQUEST_VALIDATE_EXHU) {
                            externalHuValidated = true;
                            focusArticleField();
                        } else if (request == REQUEST_GET_PICKLIST) {
                            bindPicklistNumbers(responsebody);
                        } else if (request == REQUEST_GET_PICKLIST_DATA) {
                            bindPicklistData(responsebody);
                        } else if (request == REQUEST_GET_PACKING) {
                            bindPackingMaterials(responsebody);
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
                        box.getErrBox(e);
                    }
                }

                if (dismissLoading) {
                    finishRequest();
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
                finishRequest();
                box.getBox("Err", err);
            }
        };
    }
}
