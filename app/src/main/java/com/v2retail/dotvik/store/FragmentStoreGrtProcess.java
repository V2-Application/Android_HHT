package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.material.ETPACKMAT;
import com.v2retail.util.AlertBox;
import com.v2retail.util.PlantNames;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;
import com.v2retail.util.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Store GRT Process screen.
 */
public class FragmentStoreGrtProcess extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentStoreGrtProcess.class.getName();

    private static final String PICKLIST_HINT = "Select Picklist No.";
    private static final String PACKING_HINT = "Select Packing Material";

    private static final String LGNUM = "V2R";
    private static final String SOURCE_0001 = "0001";
    private static final String SOURCE_0006 = "0006";

    private static final int REQUEST_GET_PICK_DATA = 2002;
    private static final int REQUEST_GET_PACKING = 2003;
    private static final int REQUEST_VALIDATE_EXHU = 2004;
    private static final int REQUEST_SAVE = 2005;
    private static final int REQUEST_GRT_ST_SAVE = 2006;
    private static final String VOLLEY_TAG = "FragmentStoreGrtProcess";

    View rootView;
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    int activeRequests = 0;
    FragmentManager fm;
    volatile boolean viewDestroyed = false;
    volatile boolean suppressPicklistSelection = false;
    boolean pickDataLoading = false;
    boolean packingMaterialsLoaded = false;
    ArrayAdapter<String> picklistSpinnerAdapter;
    ArrayAdapter<String> packingSpinnerAdapter;

    String URL = "";
    String WERKS = "";
    String USER = "";
    String tvsPrinter = "";

    LinearLayout source0001, source0006;
    /** Selected GRT source storage location; picklists and articles are filtered to this LGORT. */
    String selectedSource = SOURCE_0001;
    /** Last ZWM_ST_GRT_PICK_DATA response, re-bound when the source S.Loc changes. */
    JSONObject lastPickDataResponse;
    Spinner spinnerPicklistNo, spinnerPackingMaterial;
    EditText txtPickQty, txtPackQty, txtExternalHu, txtFdesPlant, txtArticle, txtScanQty;
    CheckBox chkPrintDHub;
    Button btnCancel, btnReset, btnSubmit;

    // picklistNo -> MAJ_CAT (IM_CATEGORY on save)
    Map<String, String> picklistMajCat = new HashMap<>();
    // picklistNo -> F_DES_SITE (destination plant, IM_WERKS_DES)
    Map<String, String> picklistDesSite = new HashMap<>();
    // picklistNo -> D_HUB (destination PTL hub)
    Map<String, String> picklistDesHub = new HashMap<>();
    // picklistNo -> D_NAME (destination name)
    Map<String, String> picklistDesName = new HashMap<>();
    /** Source site name from RFC export ES_S_NAME (NAME1). */
    String sourceSiteName = "";
    // packing material records (index aligns with spinner items, minus the hint at position 0)
    List<ETPACKMAT> packMaterialRecords = new ArrayList<>();
    Map<String, ScannedGrtLine> scannedLines = new HashMap<>();
    /** picklistNo → picked articles from ZWM_ST_GRT_PICK_DATA (IM_DATA) for the source site. */
    Map<String, PicklistPickData> pickDataByPicklist = new LinkedHashMap<>();
    /** MATNR / EAN11 scan key → picked article row of the selected picklist. */
    Map<String, PickedArticleLine> pickedByScanCode = new HashMap<>();
    /** True after ZWM_ST_GRT_EXHU_VALIDATION succeeds for the current External HU scan. */
    boolean externalHuValidated = false;
    /** HU and message from ZWM_ST_GRT_HU_CREATION_SAVE, shown after ZWM_GRT_ST_SAVE completes. */
    String savedHuNo = "";
    String savedHuMessage = "";

    private static class PickedArticleLine {
        String lgort;
        String matnr;
        String ean11;
        double pickQty;
        double packQty;
    }

    private static class PicklistPickData {
        final Map<String, PickedArticleLine> byScanCode = new HashMap<>();
        int articleCount;
        double pickQty;
        double packQty;
    }

    static class PicklistArticleLine {
        String picklistNo;
        String source;
        String majCat;
        String size1;
        String floor;
        String bgt;
        String matnr;
        String matkl;
    }

    static class EanRecord {
        String matnr;
        String ean11;
        double umrez;
    }

    private static class ScannedGrtLine {
        String matnr;
        String ean11;
        String lgort;
        double scanQty;
    }

    static class PicklistDataParseResult {
        final Map<String, PicklistArticleLine> articlesByMatnr = new HashMap<>(262144);
        final Map<String, EanRecord> eanByScanCode = new HashMap<>(524288);
        String rdcPlant = "";
        String errorType = "";
        String errorMessage = "";
        long parseMs;
        int httpStatus;
        int responseBytes;
        long networkMs;
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

        source0001 = rootView.findViewById(R.id.store_grt_source_0001);
        source0006 = rootView.findViewById(R.id.store_grt_source_0006);
        spinnerPicklistNo = rootView.findViewById(R.id.store_grt_picklist_no);
        spinnerPackingMaterial = rootView.findViewById(R.id.store_grt_packing_material);
        txtPickQty = rootView.findViewById(R.id.store_grt_pick_qty);
        txtPackQty = rootView.findViewById(R.id.store_grt_pack_qty);
        txtExternalHu = rootView.findViewById(R.id.store_grt_external_hu);
        txtFdesPlant = rootView.findViewById(R.id.store_grt_fdes_plant);
        txtArticle = rootView.findViewById(R.id.store_grt_article);
        txtScanQty = rootView.findViewById(R.id.store_grt_scan_qty);
        chkPrintDHub = rootView.findViewById(R.id.store_grt_print_d_hub);

        btnCancel = rootView.findViewById(R.id.store_grt_btn_cancel);
        btnReset = rootView.findViewById(R.id.store_grt_btn_reset);
        btnSubmit = rootView.findViewById(R.id.store_grt_btn_submit);

        source0001.setOnClickListener(this);
        source0006.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);

        updateSourceCards();
        setupDropdowns();
        addPicklistSelectionListener();
        addInputEvents();
        clear();

        viewDestroyed = false;
        packingMaterialsLoaded = false;

        loadPickData();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        viewDestroyed = true;
        if (spinnerPicklistNo != null) {
            spinnerPicklistNo.setOnItemSelectedListener(null);
        }
        pickDataLoading = false;
        ApplicationController.getInstance().cancelPendingRequests(VOLLEY_TAG);
        dismissDialogSafely();
        pickedByScanCode = new HashMap<>();
        pickDataByPicklist = new LinkedHashMap<>();
        lastPickDataResponse = null;
        super.onDestroyView();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.store_grt_source_0001:
                onSourceClicked(SOURCE_0001);
                break;
            case R.id.store_grt_source_0006:
                onSourceClicked(SOURCE_0006);
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

    private void onSourceClicked(String source) {
        if (source.equals(selectedSource)) {
            return;
        }
        if (!scannedLines.isEmpty()) {
            box.getBox("Confirm", "Scanned articles will be cleared. Change GRT source to S.Loc "
                    + source + "?", (dialogInterface, i) -> applySource(source), (dialogInterface, i) -> {
            });
            return;
        }
        applySource(source);
    }

    private void applySource(String source) {
        selectedSource = source;
        updateSourceCards();
        clear();
        if (lastPickDataResponse != null) {
            bindPickData(lastPickDataResponse);
        }
    }

    private void updateSourceCards() {
        boolean is0001 = SOURCE_0001.equals(selectedSource);
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
        picklistSpinnerAdapter = new ArrayAdapter<>(con,
                android.R.layout.simple_spinner_item, picklistItems);
        picklistSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPicklistNo.setAdapter(picklistSpinnerAdapter);

        List<String> packingItems = new ArrayList<>();
        packingItems.add(PACKING_HINT);
        packingSpinnerAdapter = new ArrayAdapter<>(con,
                android.R.layout.simple_spinner_item, packingItems);
        packingSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPackingMaterial.setAdapter(packingSpinnerAdapter);
    }

    private void updatePicklistSpinnerItems(List<String> items) {
        if (picklistSpinnerAdapter == null) {
            setSpinnerData(spinnerPicklistNo, items);
            return;
        }
        suppressPicklistSelection = true;
        try {
            picklistSpinnerAdapter.clear();
            picklistSpinnerAdapter.addAll(items);
            picklistSpinnerAdapter.notifyDataSetChanged();
            spinnerPicklistNo.setSelection(0);
        } finally {
            suppressPicklistSelection = false;
        }
    }

    private void updatePackingSpinnerItems(List<String> items) {
        if (packingSpinnerAdapter == null) {
            setSpinnerData(spinnerPackingMaterial, items);
            return;
        }
        packingSpinnerAdapter.clear();
        packingSpinnerAdapter.addAll(items);
        packingSpinnerAdapter.notifyDataSetChanged();
        spinnerPackingMaterial.setSelection(0);
    }

    private void setPicklistSpinnerSelection(int index) {
        suppressPicklistSelection = true;
        try {
            spinnerPicklistNo.setSelection(index);
        } finally {
            suppressPicklistSelection = false;
        }
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
                if (suppressPicklistSelection) {
                    return;
                }
                resetPicklistScanState();
                applyPlantForSelectedPicklist();
                applyPickDataForSelectedPicklist();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                txtFdesPlant.setText("");
                showPickPackQty(null);
            }
        });
    }

    private void applyPickDataForSelectedPicklist() {
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            showPickPackQty(null);
            return;
        }
        PicklistPickData pickData = pickDataByPicklist.get(picklist);
        showPickPackQty(pickData);
        if (pickData == null || pickData.articleCount == 0) {
            UIFuncs.errorSound(con);
            box.getBox("No Data", "No picked articles found for picklist " + picklist + ".");
            return;
        }
        if (pickData.pickQty <= pickData.packQty) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Picklist " + picklist + " is already packed.\nPick QTY ("
                    + Util.formatDouble(pickData.pickQty) + ") must be greater than Pack QTY ("
                    + Util.formatDouble(pickData.packQty) + ").");
            setPicklistSpinnerSelection(0);
            txtFdesPlant.setText("");
            showPickPackQty(null);
            return;
        }
        pickedByScanCode = pickData.byScanCode;
        txtExternalHu.requestFocus();
        UIFuncs.enableInput(con, txtExternalHu);
    }

    private void showPickPackQty(PicklistPickData pickData) {
        txtPickQty.setText(pickData != null ? Util.formatDouble(pickData.pickQty) : "");
        txtPackQty.setText(pickData != null ? Util.formatDouble(pickData.packQty) : "");
    }

    private void applyPlantForSelectedPicklist() {
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            txtFdesPlant.setText("");
            return;
        }
        String fDesSite = picklistDesSite.get(picklist);
        txtFdesPlant.setText(fDesSite != null ? fDesSite : "");
    }

    private void resetPicklistScanState() {
        pickedByScanCode = new HashMap<>();
        scannedLines = new HashMap<>();
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
            setPicklistSpinnerSelection(0);
        }
        if (spinnerPackingMaterial.getAdapter() != null && spinnerPackingMaterial.getAdapter().getCount() > 0) {
            spinnerPackingMaterial.setSelection(0);
        }
        resetPicklistScanState();
        externalHuValidated = false;
        txtExternalHu.setText("");
        txtFdesPlant.setText("");
        showPickPackQty(null);
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
        if (pickedByScanCode.isEmpty()) {
            UIFuncs.errorSound(con);
            if (pickDataLoading) {
                box.getBox("Please wait", "Picked data is still loading. Scan article after loading completes.");
            } else {
                box.getBox("Err", "No picked data for this picklist. Reselect picklist and try again.");
            }
            resetArticleInput();
            return;
        }

        PickedArticleLine picked = findPickedLine(article);
        if (picked == null) {
            Log.w(TAG, "Article not in picked data scan=" + article);
            UIFuncs.errorSound(con);
            box.getBox("Err", "Article not picked against this picklist.");
            resetArticleInput();
            return;
        }

        String matnrKey = picked.matnr.toUpperCase();
        ScannedGrtLine line = scannedLines.get(matnrKey);
        String huLgort = currentScanLgort();
        if (line == null && !huLgort.isEmpty() && !huLgort.equalsIgnoreCase(picked.lgort)) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Article is picked from storage location " + picked.lgort
                    + ". Current HU has articles from " + huLgort + ".");
            resetArticleInput();
            return;
        }

        double alreadyScanned = line != null ? line.scanQty : 0;
        double available = picked.pickQty - picked.packQty;
        double proposedQty = alreadyScanned + 1;
        if (proposedQty > available) {
            UIFuncs.errorSound(con);
            box.getBox("Limit Reached", "Scanned quantity cannot exceed Pick QTY - Pack QTY ("
                    + Util.formatDouble(picked.pickQty) + " - " + Util.formatDouble(picked.packQty)
                    + " = " + Util.formatDouble(Math.max(available, 0)) + ") for article "
                    + UIFuncs.removeLeadingZeros(picked.matnr) + ".");
            resetArticleInput();
            return;
        }

        if (line == null) {
            line = new ScannedGrtLine();
            line.matnr = picked.matnr;
            line.ean11 = picked.ean11;
            line.lgort = picked.lgort;
            line.scanQty = 0;
        }
        line.scanQty = proposedQty;
        scannedLines.put(matnrKey, line);

        txtScanQty.setText(Util.formatDouble(getTotalScanQty()));
        Log.d(TAG, "Article scan matnr=" + picked.matnr + " lgort=" + picked.lgort
                + " qty=" + proposedQty + " available=" + available);
        resetArticleInput();
    }

    /** LGORT of articles already scanned into the current HU, or empty when none yet. */
    private String currentScanLgort() {
        for (ScannedGrtLine line : scannedLines.values()) {
            if (line.lgort != null && !line.lgort.isEmpty()) {
                return line.lgort;
            }
        }
        return "";
    }

    private PickedArticleLine findPickedLine(String scan) {
        if (scan == null) {
            return null;
        }
        String upper = scan.trim().toUpperCase();
        if (upper.isEmpty()) {
            return null;
        }
        PickedArticleLine hit = pickedByScanCode.get(upper);
        if (hit != null) {
            return hit;
        }
        String noZeros = UIFuncs.removeLeadingZeros(upper);
        return noZeros.isEmpty() ? null : pickedByScanCode.get(noZeros.toUpperCase());
    }

    private static void indexEanRecord(Map<String, EanRecord> target, EanRecord record) {
        if (target == null || record == null || record.matnr == null || record.matnr.isEmpty()) {
            return;
        }
        target.put(record.matnr.toUpperCase(), record);
        String nz = UIFuncs.removeLeadingZeros(record.matnr);
        if (!nz.isEmpty()) {
            target.put(nz.toUpperCase(), record);
        }
        if (record.ean11 != null && !record.ean11.isEmpty()) {
            target.put(record.ean11.toUpperCase(), record);
            String eanNz = UIFuncs.removeLeadingZeros(record.ean11);
            if (!eanNz.isEmpty()) {
                target.put(eanNz.toUpperCase(), record);
            }
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

        String werksDes = picklistDesSite.get(picklist);
        if (werksDes == null || werksDes.isEmpty()) {
            werksDes = UIFuncs.toUpperTrim(txtFdesPlant);
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

        String category = picklistMajCat.get(picklist);
        if (category == null) {
            category = "";
        }

        String lgortSrc = currentScanLgort();
        if (lgortSrc.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "No storage location found for scanned articles.");
            return;
        }

        JSONArray arrItData = buildItData(category);
        if (arrItData == null || arrItData.length() == 0) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "No scan data to submit.");
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_HU_CREATION_SAVE);
            args.put("IM_WERKS", WERKS);
            args.put("IM_LGORT_SRC", lgortSrc);
            args.put("IM_WERKS_DES", werksDes);
            args.put("IM_USER", USER);
            args.put("IM_PACK_MAT", packMat);
            args.put("IM_CATEGORY", picklist);
            args.put("IT_DATA", arrItData);
            showProcessingAndSubmit(Vars.ZWM_ST_GRT_HU_CREATION_SAVE, REQUEST_SAVE, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private JSONArray buildItData(String category) {
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
                itDataJson.put("STOR_LOC", line.lgort != null ? line.lgort : "");
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
                itDataJson.put("MATKL", "");
                itDataJson.put("WGBEZ", "");
                itDataJson.put("SONUM", "");
                itDataJson.put("DELNUM", "");
                itDataJson.put("POSNR", "");
                itDataJson.put("GNATURE", category);
                arrItData.put(itDataJson);
            }
            return arrItData;
        } catch (JSONException e) {
            box.getErrBox(e);
            return null;
        }
    }

    private JSONObject buildGrtStSaveArgs(String sapHu) throws JSONException {
        String picklist = getSelectedPicklist();
        JSONArray imData = new JSONArray();
        for (ScannedGrtLine line : scannedLines.values()) {
            if (line.scanQty <= 0 || line.matnr == null || line.matnr.isEmpty()) {
                continue;
            }
            JSONObject row = new JSONObject();
            row.put("PICKLIST_NO", picklist);
            row.put("LGORT", line.lgort != null ? line.lgort : "");
            row.put("MATNR", line.matnr);
            row.put("PACK_QTY", line.scanQty);
            row.put("SAP_HU", sapHu);
            imData.put(row);
        }
        JSONObject args = new JSONObject();
        args.put("bapiname", Vars.ZWM_GRT_ST_SAVE);
        args.put("IM_DATA", imData);
        return args;
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

    /** Resolves paired printer name (saved, then TVS 4B-2033*, then Newland PP310). */
    private boolean resolveTvsPrinterForPrint(SharedPreferencesData data) {
        TSPLPrinter helper = new TSPLPrinter(con);
        if (helper.findSavedOrKnownLabelPrinter(tvsPrinter != null && !tvsPrinter.isEmpty() ? tvsPrinter : data.read(Vars.TVS_PRINTER))) {
            tvsPrinter = helper.getPrinterName();
            if (tvsPrinter != null && !tvsPrinter.isEmpty()) {
                data.write(Vars.TVS_PRINTER, tvsPrinter);
            }
            return true;
        }
        return false;
    }

    private void ensurePackingMaterialsLoaded() {
        if (!packingMaterialsLoaded && !viewDestroyed && isAdded()) {
            loadPackingMaterials();
        }
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
        final boolean printDestHub = chkPrintDHub != null && chkPrintDHub.isChecked();
        new Thread(() -> {
            TSPLPrinter printer = new TSPLPrinter(con, Vars.STORE_GRT_PROCESS);
            boolean printed = printer.sendStoreGrtPrintCommand(
                    printerName, hu, srcCode, srcName, dest, hub, name, qtyVal, dt, printDestHub);
            if (!printed) {
                Log.w(TAG, "Store GRT label print failed for HU: " + hu);
            } else {
                Log.d(TAG, "Store GRT label printed for HU: " + hu);
            }
        }).start();
    }

    private void loadPickData() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_PICK_DATA);
            args.put("IM_PICK", "");
            args.put("SOURCE_SITE", WERKS);
            pickDataLoading = true;
            showProcessingAndSubmit(Vars.ZWM_ST_GRT_PICK_DATA, REQUEST_GET_PICK_DATA, args);
        } catch (JSONException e) {
            pickDataLoading = false;
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    /** Groups IM_DATA rows by PICKLIST_NO, binds the picklist dropdown and caches pick/pack qty per article. */
    private void bindPickData(JSONObject response) {
        lastPickDataResponse = response;
        Map<String, PicklistPickData> byPicklist = new LinkedHashMap<>();
        Map<String, Map<String, PickedArticleLine>> articlesByPicklist = new HashMap<>();
        Map<String, String> majCat = new HashMap<>();
        Map<String, String> desSite = new HashMap<>();
        Map<String, String> desHub = new HashMap<>();
        Map<String, String> desName = new HashMap<>();
        JSONArray arr = response.optJSONArray("IM_DATA");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row == null || isHeaderRow(row)) {
                    continue;
                }
                String picklistNo = row.optString("PICKLIST_NO", "").trim();
                String matnr = row.optString("MATNR", "").trim();
                if (picklistNo.isEmpty() || matnr.isEmpty() || "MATNR".equalsIgnoreCase(matnr)) {
                    continue;
                }
                String lgort = row.optString("LGORT", "").trim();
                if (!lgort.isEmpty() && !lgort.equals(selectedSource)) {
                    continue;
                }
                PicklistPickData pickData = byPicklist.get(picklistNo);
                if (pickData == null) {
                    pickData = new PicklistPickData();
                    byPicklist.put(picklistNo, pickData);
                    articlesByPicklist.put(picklistNo, new HashMap<>());
                }
                Map<String, PickedArticleLine> byMatnr = articlesByPicklist.get(picklistNo);
                String key = matnr.toUpperCase();
                PickedArticleLine line = byMatnr.get(key);
                if (line == null) {
                    line = new PickedArticleLine();
                    line.matnr = matnr;
                    line.lgort = lgort.isEmpty() ? selectedSource : lgort;
                    line.ean11 = "";
                    byMatnr.put(key, line);
                    putScanKey(pickData.byScanCode, matnr, line);
                    pickData.articleCount++;
                }
                double pickQty = Util.convertStringToDouble(row.optString("PICK_QTY", "0"));
                double packQty = Util.convertStringToDouble(row.optString("PACK_QTY", "0"));
                line.pickQty += pickQty;
                line.packQty += packQty;
                pickData.pickQty += pickQty;
                pickData.packQty += packQty;
                String ean11 = row.optString("EAN11", "").trim();
                if (!ean11.isEmpty()) {
                    putScanKey(pickData.byScanCode, ean11, line);
                    if (line.ean11.isEmpty()) {
                        line.ean11 = ean11;
                    }
                }
                putIfPresent(majCat, picklistNo, row.optString("MAJ_CAT", ""));
                putIfPresent(desSite, picklistNo, row.optString("F_DES_SITE", ""));
                putIfPresent(desHub, picklistNo, row.optString("D_HUB", ""));
                putIfPresent(desName, picklistNo, row.optString("D_NAME", ""));
            }
        }
        pickDataByPicklist = byPicklist;
        picklistMajCat = majCat;
        picklistDesSite = desSite;
        picklistDesHub = desHub;
        picklistDesName = desName;
        String sourceName = readSapNameExport(response, "ES_S_NAME");
        if (!sourceName.isEmpty()) {
            sourceSiteName = sourceName;
        }

        List<String> items = new ArrayList<>();
        items.add(PICKLIST_HINT);
        items.addAll(byPicklist.keySet());
        updatePicklistSpinnerItems(items);
        resetPicklistScanState();
        txtFdesPlant.setText("");
        showPickPackQty(null);
        Log.d(TAG, "ZWM_ST_GRT_PICK_DATA done source=" + selectedSource + " picklists=" + byPicklist.size());
        if (byPicklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("No Data", "No picklist found for S.Loc " + selectedSource + ".");
        }
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        String trimmed = value != null ? value.trim() : "";
        if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed) && !target.containsKey(key)) {
            target.put(key, trimmed);
        }
    }

    private static void putScanKey(Map<String, PickedArticleLine> target, String code, PickedArticleLine line) {
        target.put(code.toUpperCase(), line);
        String noZeros = UIFuncs.removeLeadingZeros(code);
        if (!noZeros.isEmpty()) {
            target.put(noZeros.toUpperCase(), line);
        }
    }

    static PicklistDataParseResult parseGetPicklistData(byte[] body, String werks) throws IOException {
        return parsePicklistDataBytes(body, werks);
    }

    private static PicklistDataParseResult parsePicklistDataBytes(byte[] body, String werks) throws IOException {
        long started = SystemClock.elapsedRealtime();
        PicklistDataParseResult result = new PicklistDataParseResult();
        int offset = 0;
        if (body.length >= 3 && (body[0] & 0xFF) == 0xEF && (body[1] & 0xFF) == 0xBB && (body[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        JsonReader reader = new JsonReader(new InputStreamReader(
                new ByteArrayInputStream(body, offset, body.length - offset), StandardCharsets.UTF_8));
        reader.setLenient(true);
        try {
            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                parsePicklistDataObject(reader, result, werks);
            } else {
                reader.skipValue();
            }
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
        result.parseMs = SystemClock.elapsedRealtime() - started;
        Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA parse ms=" + result.parseMs
                + " bytes=" + body.length
                + " articles=" + result.articlesByMatnr.size()
                + " eans=" + result.eanByScanCode.size()
                + " rdc=" + result.rdcPlant
                + " type=" + result.errorType);
        return result;
    }

    private static void parsePicklistDataObject(JsonReader reader, PicklistDataParseResult result, String werks)
            throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (key == null) {
                reader.skipValue();
                continue;
            }
            String ukey = key.toUpperCase(Locale.ROOT);
            JsonToken token = reader.peek();
            if ("DATA".equals(ukey) && token == JsonToken.BEGIN_OBJECT) {
                parsePicklistDataObject(reader, result, werks);
            } else if ("ET_DATA".equals(ukey)) {
                parseEtDataTable(reader, result, werks);
            } else if ("ET_EAN_DATA".equals(ukey)) {
                parseEtEanTable(reader, result);
            } else if ("EX_RDC".equals(ukey)) {
                String rdc = readJsonAsString(reader).trim();
                if (!rdc.isEmpty() && !"null".equalsIgnoreCase(rdc)) {
                    result.rdcPlant = rdc;
                }
            } else if ("EX_RETURN".equals(ukey) || "ER_RETURN".equals(ukey)) {
                readReturnInto(reader, result);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static void parseEtDataTable(JsonReader reader, PicklistDataParseResult result, String werks)
            throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) {
                readEtDataRow(reader, result, werks);
            }
            reader.endArray();
            return;
        }
        if (token == JsonToken.BEGIN_OBJECT) {
            reader.beginObject();
            boolean nested = false;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("item".equalsIgnoreCase(name) || "results".equalsIgnoreCase(name)) {
                    nested = true;
                    parseEtDataTable(reader, result, werks);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            if (!nested) {
                return;
            }
            return;
        }
        reader.skipValue();
    }

    private static void readEtDataRow(JsonReader reader, PicklistDataParseResult result, String werks)
            throws IOException {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue();
            return;
        }
        PicklistArticleLine line = new PicklistArticleLine();
        line.picklistNo = "";
        line.source = "";
        line.majCat = "";
        line.size1 = "";
        line.floor = "";
        line.bgt = "";
        line.matnr = "";
        line.matkl = "";
        reader.beginObject();
        while (reader.hasNext()) {
            String uk = reader.nextName().toUpperCase(Locale.ROOT);
            if ("MATNR".equals(uk)) {
                line.matnr = readJsonAsString(reader).trim();
            } else if ("PICKLIST_NO".equals(uk)) {
                line.picklistNo = readJsonAsString(reader).trim();
            } else if ("SOUR".equals(uk) || "SOURCE".equals(uk)) {
                line.source = readJsonAsString(reader).trim();
            } else if ("MAJ_CAT".equals(uk)) {
                line.majCat = readJsonAsString(reader).trim();
            } else if ("SIZE1".equals(uk)) {
                line.size1 = readJsonAsString(reader).trim();
            } else if ("FLOOR".equals(uk)) {
                line.floor = readJsonAsString(reader).trim();
            } else if ("BGT".equals(uk)) {
                line.bgt = readJsonAsString(reader).trim();
            } else if ("MATKL".equals(uk)) {
                line.matkl = readJsonAsString(reader).trim();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (line.matnr.isEmpty() || "MATNR".equalsIgnoreCase(line.matnr)
                || "PICKLIST_NO".equalsIgnoreCase(line.picklistNo)) {
            return;
        }
        if (line.source.isEmpty()) {
            line.source = werks != null ? werks : "";
        }
        result.articlesByMatnr.put(line.matnr.toUpperCase(Locale.ROOT), line);
        String noZeros = UIFuncs.removeLeadingZeros(line.matnr);
        if (!noZeros.isEmpty()) {
            result.articlesByMatnr.putIfAbsent(noZeros.toUpperCase(Locale.ROOT), line);
        }
    }

    private static void parseEtEanTable(JsonReader reader, PicklistDataParseResult result) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) {
                readEtEanRow(reader, result);
            }
            reader.endArray();
            return;
        }
        if (token == JsonToken.BEGIN_OBJECT) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("item".equalsIgnoreCase(name) || "results".equalsIgnoreCase(name)) {
                    parseEtEanTable(reader, result);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return;
        }
        reader.skipValue();
    }

    private static void readEtEanRow(JsonReader reader, PicklistDataParseResult result) throws IOException {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue();
            return;
        }
        String matnr = "";
        String ean11 = "";
        String umrez = "1";
        reader.beginObject();
        while (reader.hasNext()) {
            String uk = reader.nextName().toUpperCase(Locale.ROOT);
            if ("MATNR".equals(uk)) {
                matnr = readJsonAsString(reader).trim();
            } else if ("EAN11".equals(uk)) {
                ean11 = readJsonAsString(reader).trim();
            } else if ("UMREZ".equals(uk)) {
                umrez = readJsonAsString(reader).trim();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (matnr.isEmpty() || "MATNR".equalsIgnoreCase(matnr) || "EAN11".equalsIgnoreCase(ean11)) {
            return;
        }
        EanRecord record = new EanRecord();
        record.matnr = matnr;
        record.ean11 = ean11;
        record.umrez = Util.convertStringToDouble(umrez);
        if (record.umrez <= 0) {
            record.umrez = 1;
        }
        indexEanRecord(result.eanByScanCode, record);
    }

    private static void readReturnInto(JsonReader reader, PicklistDataParseResult result) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) {
                applyReturnObject(reader, result);
            }
            reader.endArray();
            return;
        }
        if (token == JsonToken.BEGIN_OBJECT) {
            applyReturnObject(reader, result);
            return;
        }
        reader.skipValue();
    }

    private static void applyReturnObject(JsonReader reader, PicklistDataParseResult result) throws IOException {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue();
            return;
        }
        String type = "";
        String message = "";
        reader.beginObject();
        while (reader.hasNext()) {
            String uk = reader.nextName().toUpperCase(Locale.ROOT);
            if ("TYPE".equals(uk)) {
                type = readJsonAsString(reader).trim();
            } else if ("MESSAGE".equals(uk)) {
                message = readJsonAsString(reader).trim();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (type.isEmpty() || "TYPE".equalsIgnoreCase(type)) {
            return;
        }
        if ("E".equals(type) || "A".equals(type) || result.errorType.isEmpty()) {
            result.errorType = type;
            result.errorMessage = message;
        }
    }

    private static String readJsonAsString(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            return "";
        }
        if (token == JsonToken.BOOLEAN) {
            return Boolean.toString(reader.nextBoolean());
        }
        if (token == JsonToken.BEGIN_OBJECT || token == JsonToken.BEGIN_ARRAY) {
            reader.skipValue();
            return "";
        }
        return reader.nextString();
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
                    String maktx = row.optString("MAKTX", "").trim();
                    if (maktx.isEmpty()) {
                        continue;
                    }
                    ETPACKMAT pack = new ETPACKMAT();
                    pack.setMATNR(row.optString("MATNR", "").trim());
                    pack.setMAKTX(maktx);
                    packMaterialRecords.add(pack);
                    items.add(maktx);
                }
            }

            updatePackingSpinnerItems(items);
            packingMaterialsLoaded = true;
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
        if ("PICKLIST_NO".equalsIgnoreCase(row.optString("PICKLIST_NO", "").trim())) {
            return true;
        }
        // Template row may carry field labels ("Pick List No.", "Quantity") instead of technical names.
        return !isNumericOrEmpty(row.optString("PICK_QTY", ""))
                || !isNumericOrEmpty(row.optString("PACK_QTY", ""));
    }

    private static boolean isNumericOrEmpty(String value) {
        String trimmed = value != null ? value.trim() : "";
        if (trimmed.isEmpty()) {
            return true;
        }
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

    private void dismissDialogSafely() {
        activeRequests = 0;
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        dialog = null;
    }

    private void dismissDialog() {
        dismissDialogSafely();
    }

    /** Decrement the in-flight request count and dismiss the dialog only when none remain. */
    private void finishRequest() {
        if (viewDestroyed) {
            return;
        }
        activeRequests--;
        if (activeRequests <= 0) {
            dismissDialogSafely();
        }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        if (viewDestroyed || !isAdded()) {
            return;
        }
        showLoadingDialog();
        try {
            submitRequest(rfc, request, args);
        } catch (Exception e) {
            finishRequest();
            box.getErrBox(e);
        }
    }

    private void showLoadingDialog() {
        if (viewDestroyed || !isAdded()) {
            return;
        }
        activeRequests++;
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(getContext());
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "RFC request -> " + rfc + " payload=" + params);

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject responsebody) {
                Log.d(TAG, "RFC response -> " + rfc + " body=" + responsebody);
                if (request == REQUEST_GET_PICK_DATA) {
                    pickDataLoading = false;
                }

                if (viewDestroyed || !isAdded()) {
                    finishRequest();
                    return;
                }

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
                            if (request == REQUEST_GRT_ST_SAVE) {
                                box.getBox("Err", "HU " + savedHuNo
                                        + " created, but picklist update failed: "
                                        + returnobj.optString("MESSAGE"));
                            } else {
                                box.getBox("Err", returnobj.optString("MESSAGE"));
                            }
                            if (request == REQUEST_VALIDATE_EXHU) {
                                resetExternalHuInput();
                            }
                        } else if (request == REQUEST_VALIDATE_EXHU) {
                            externalHuValidated = true;
                            focusArticleField();
                        } else if (request == REQUEST_GET_PICK_DATA) {
                            bindPickData(responsebody);
                        } else if (request == REQUEST_GET_PACKING) {
                            bindPackingMaterials(responsebody);
                        } else if (request == REQUEST_SAVE) {
                            String successMsg = returnobj != null
                                    ? returnobj.optString("MESSAGE", "Saved successfully.")
                                    : "Saved successfully.";
                            String huNo = extractHuFromSaveResponse(responsebody, returnobj);
                            if (huNo.isEmpty()) {
                                box.getBox("Success", successMsg
                                        + "\nSAP HU not returned, picklist not updated.");
                                clear();
                            } else {
                                printGrtHuLabel(huNo);
                                JSONObject grtStArgs = buildGrtStSaveArgs(huNo);
                                clear();
                                savedHuNo = huNo;
                                savedHuMessage = successMsg;
                                showProcessingAndSubmit(Vars.ZWM_GRT_ST_SAVE, REQUEST_GRT_ST_SAVE, grtStArgs);
                            }
                        } else if (request == REQUEST_GRT_ST_SAVE) {
                            box.getBox("Success", savedHuMessage);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        box.getErrBox(e);
                    }
                }

                if (request == REQUEST_GET_PICK_DATA) {
                    ensurePackingMaterialsLoaded();
                } else if (request == REQUEST_GRT_ST_SAVE) {
                    loadPickData();
                }
                finishRequest();
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
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy(50000, 0, 1.0f));
        mJsonRequest.setShouldCache(false);
        mJsonRequest.setTag(VOLLEY_TAG);
        mRequestQueue.add(mJsonRequest);
    }

    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pickDataLoading = false;
                if (viewDestroyed || !isAdded()) {
                    finishRequest();
                    return;
                }
                Log.e(TAG, "RFC error -> " + error.toString(), error);
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
