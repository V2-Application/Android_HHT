package com.v2retail.dotvik.dc.ptlnew.grt;

import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.BackPressHandler;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PTL-GRT — Hub Sorting, Scan Crate.
 * <ul>
 *   <li>MSA Crate validate: {@link Vars#ZWM_PTL_GRT_MSA_CRATE_VALIDATE}</li>
 *   <li>MSA REV Crate validate: {@link Vars#GRT_PUTAWAY_VALIDATE_CRATE}</li>
 *   <li>Matched articles: {@link Vars#ZWM_PTL_HUB_ARTICLE_TAG_CRATE} after HUB crate scan.</li>
 *   <li>Unmatched articles are cached and saved via {@link Vars#ZGRTRET_SAVE_CRATE_DETAILS}.</li>
 * </ul>
 */
public class FragmentPTLGrtHubSortingScanCrate extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentPTLGrtHubSortingScanCrate.class.getSimpleName();
    private static final String ACTION_BAR_TITLE = "HUB SORTING";
    private static final List<String> FLOOR_OPTIONS = Arrays.asList("0", "1", "2", "3", "4", "5");
    private static final int REQUEST_VALIDATE_CRATE = 5911;
    private static final int REQUEST_TAG_HUB = 5912;
    private static final int REQUEST_VALIDATE_REV_CRATE = 5913;
    private static final int REQUEST_SAVE_CACHE = 5914;
    private static final int REQUEST_EMPTY_SHORT_CLOSE = 5915;
    private static final String LOCAL_PREFS = "ptl_grt_hub_sorting";
    private static final String LOCAL_SESSION = "pending_session";

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private TextView ddFloor;
    private EditText txtScanCrate;
    private EditText txtCrate;
    private EditText txtEmptyCrateScan;
    private EditText txtEmptyCrate;
    private EditText txtScanArticle;
    private EditText txtArticle;
    private EditText txtProposedHub;
    private EditText txtHubMapCrate;
    private Button btnBack;
    private Button btnEmpty;
    private Button btnSave;

    private boolean floorSelected = false;
    private String validatedCrate = "";
    private String emptyCrate = "";
    private String hubMapCrate = "";
    private Map<String, JSONObject> etDataMap = new HashMap<>();
    private Map<String, JSONObject> eanDataMap = new HashMap<>();
    private Map<String, Double> scannedQtyByArticle = new HashMap<>();
    private JSONArray referenceEtData = new JSONArray();
    private JSONArray referenceEanData = new JSONArray();
    private JSONArray pendingScans = new JSONArray();
    private SharedPreferences localPreferences;

    private String currentArticle = "";
    private JSONObject currentEtRow = null;
    private double currentMaxQty = 0;
    private double currentScannedQty = 0;
    private double currentScanQty = 0;
    private boolean autoTagInProgress = false;
    /** Guards against the scanner double-firing (text watcher + trailing Enter). */
    private boolean crateValidateInProgress = false;
    private boolean revCrateValidateInProgress = false;
    /** Value submitted for validation — do not re-read EditText on success (fast re-scan appends). */
    private String pendingCrateScan = "";
    private String pendingRevCrateScan = "";
    private String lastArticleScanValue = "";
    private long lastArticleScanAtMs = 0;

    public FragmentPTLGrtHubSortingScanCrate() {
    }

    public static FragmentPTLGrtHubSortingScanCrate newInstance() {
        return new FragmentPTLGrtHubSortingScanCrate();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
        requireActivity().getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (pendingScans.length() > 0) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Unsaved Data")
                                    .setMessage("Save the locally stored scans before leaving this process.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            BackPressHandler.confirmCloseProcess(fm, requireContext());
                        }
                    }
                });
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
        localPreferences = con.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE);

        ddFloor = root.findViewById(R.id.dd_ptl_grt_hub_sorting_scan_crate_floor);
        txtScanCrate = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan);
        txtCrate = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_crate);
        txtEmptyCrateScan = root.findViewById(R.id.txt_ptl_grt_hub_sorting_empty_crate_scan);
        txtEmptyCrate = root.findViewById(R.id.txt_ptl_grt_hub_sorting_empty_crate);
        txtScanArticle = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan_article);
        txtArticle = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_article);
        txtProposedHub = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_proposed_hub);
        txtHubMapCrate = root.findViewById(R.id.txt_ptl_grt_hub_sorting_scan_crate_scan_hub);
        btnBack = root.findViewById(R.id.btn_ptl_grt_hub_sorting_scan_crate_back);
        btnEmpty = root.findViewById(R.id.btn_ptl_grt_hub_sorting_scan_crate_empty);
        btnSave = root.findViewById(R.id.btn_ptl_grt_hub_sorting_scan_crate_save);

        setupFloorDropdown();
        addCrateScanEvents();
        addEmptyCrateScanEvents();
        addArticleScanEvents();
        addHubMapCrateEvents();
        btnBack.setOnClickListener(this);
        btnEmpty.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        // Always open HUB SORTING as a fresh page.
        clearLocalSession();
        resetAfterFloorChange();
        ddFloor.setEnabled(true);
        ddFloor.setText(FLOOR_OPTIONS.get(0));
        floorSelected = true;
        UIFuncs.enableInput(con, txtScanCrate);
        updateActionButtons();
        txtScanCrate.post(() -> txtScanCrate.requestFocus());

        return root;
    }

    @Override
    public void onPause() {
        persistLocalSession();
        super.onPause();
    }

    private void setupFloorDropdown() {
        ddFloor.setText(FLOOR_OPTIONS.get(0));
        floorSelected = true;
        ddFloor.setClickable(true);
        ddFloor.setFocusable(true);
        ddFloor.setEnabled(true);
        ddFloor.setOnClickListener(v -> showFloorPicker());
    }

    private void showFloorPicker() {
        if (!isAdded()) {
            return;
        }
        // Lock floor only after article scans are stored locally.
        if (pendingScans != null && pendingScans.length() > 0) {
            box.getBox("Floor Locked",
                    "Save or clear scanned articles before changing FLOOR.");
            return;
        }

        final String[] floors = FLOOR_OPTIONS.toArray(new String[0]);
        int checked = FLOOR_OPTIONS.indexOf(getSelectedFloor());
        if (checked < 0) {
            checked = 0;
        }

        new AlertDialog.Builder(requireActivity())
                .setTitle("Select Floor")
                .setSingleChoiceItems(floors, checked, (dialog, which) -> {
                    String previous = getSelectedFloor();
                    String selected = FLOOR_OPTIONS.get(which);
                    dialog.dismiss();
                    if (selected.equals(previous)) {
                        return;
                    }
                    applyFloorSelection(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyFloorSelection(String selected) {
        Runnable apply = () -> {
            ddFloor.setText(selected);
            floorSelected = true;
            ddFloor.setEnabled(true);
            resetAfterFloorChange();
            clearLocalSession();
            UIFuncs.enableInput(con, txtScanCrate);
            txtScanCrate.requestFocus();
            updateActionButtons();
        };

        boolean hasProgress = !TextUtils.isEmpty(validatedCrate)
                || !TextUtils.isEmpty(emptyCrate)
                || !TextUtils.isEmpty(hubMapCrate);
        if (!hasProgress) {
            apply.run();
            return;
        }

        new AlertDialog.Builder(requireActivity())
                .setTitle("Change Floor")
                .setMessage("Changing FLOOR will clear current crate data. Continue?")
                .setPositiveButton("Yes", (d, w) -> apply.run())
                .setNegativeButton("No", null)
                .show();
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

    private void addEmptyCrateScanEvents() {
        txtEmptyCrateScan.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtEmptyCrateScan);
                if (!TextUtils.isEmpty(scanned)) {
                    requestRevCrateValidate(scanned);
                }
                return true;
            }
            return false;
        });

        txtEmptyCrateScan.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().toUpperCase(Locale.ROOT).trim();
                if (!value.isEmpty() && scannerReading) {
                    requestRevCrateValidate(value);
                }
            }
        });
    }

    private void requestRevCrateValidate(String scannedCrate) {
        if (TextUtils.isEmpty(scannedCrate)) {
            return;
        }
        if (revCrateValidateInProgress) {
            Log.d(TAG, "requestRevCrateValidate skipped — validation already in progress");
            txtEmptyCrateScan.post(() -> {
                if (txtEmptyCrateScan != null) {
                    txtEmptyCrateScan.setText("");
                }
            });
            return;
        }
        if (TextUtils.isEmpty(validatedCrate)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate MSA Crate first.");
            txtEmptyCrateScan.setText("");
            txtScanCrate.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            revCrateValidateInProgress = true;
            pendingRevCrateScan = scannedCrate;
            txtEmptyCrateScan.post(() -> {
                if (txtEmptyCrateScan != null) {
                    txtEmptyCrateScan.setText("");
                }
            });
            args.put("bapiname", Vars.GRT_PUTAWAY_VALIDATE_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", scannedCrate);
            showProcessingAndSubmit(Vars.GRT_PUTAWAY_VALIDATE_CRATE, REQUEST_VALIDATE_REV_CRATE, args);
        } catch (JSONException e) {
            revCrateValidateInProgress = false;
            pendingRevCrateScan = "";
            Log.e(TAG, "requestRevCrateValidate", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void addArticleScanEvents() {
        txtScanArticle.setInputType(InputType.TYPE_CLASS_NUMBER);
        txtScanArticle.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    if (source == null || start >= end) {
                        return null;
                    }
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) {
                            showBottomToast("scan EAN / Article only");
                            if (dest != null && dest.length() > 0) {
                                txtScanArticle.post(() -> {
                                    if (txtScanArticle != null) {
                                        txtScanArticle.setText("");
                                    }
                                });
                            }
                            return "";
                        }
                    }
                    return null;
                }
        });

        txtScanArticle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtScanArticle);
                if (!TextUtils.isEmpty(scanned)) {
                    if (!isNumericOnly(scanned)) {
                        showBottomToast("scan EAN / Article only");
                        txtScanArticle.setText("");
                        return true;
                    }
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
                String value = s.toString().trim();
                if (!value.isEmpty() && scannerReading) {
                    if (!isNumericOnly(value)) {
                        showBottomToast("scan EAN / Article only");
                        txtScanArticle.setText("");
                        return;
                    }
                    validateArticleScan(value);
                }
            }
        });
    }

    private boolean isNumericOnly(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void addHubMapCrateEvents() {
        txtHubMapCrate.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                captureHubMapCrate(UIFuncs.toUpperTrim(txtHubMapCrate));
                return true;
            }
            return false;
        });

        txtHubMapCrate.addTextChangedListener(new TextWatcher() {
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
                    captureHubMapCrate(value);
                }
            }
        });
    }

    private void captureHubMapCrate(String scannedCrate) {
        if (TextUtils.isEmpty(emptyCrate) || TextUtils.isEmpty(scannedCrate)) {
            return;
        }
        if (TextUtils.isEmpty(currentArticle) || currentEtRow == null || autoTagInProgress) {
            return;
        }
        hubMapCrate = scannedCrate;
        txtHubMapCrate.setText(hubMapCrate);
        requestMatchedArticleTag(scannedCrate);
    }

    private String getSelectedFloor() {
        CharSequence selected = ddFloor.getText();
        return selected == null ? "" : selected.toString().trim();
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

    private static double resolveMaxQty(JSONObject etRow) {
        if (etRow == null) {
            return 0;
        }
        // Prefer required/open qty. SCAN_QTY is usually already-scanned amount, not the limit.
        double qty = parseQty(etRow, "QUANTITY", "QTY", 0);
        if (qty <= 0) {
            qty = parseQty(etRow, "OPEN_QTY", "PO_QTY", 0);
        }
        if (qty <= 0) {
            qty = parseQty(etRow, "MENGE", "SCAN_QTY", 0);
        }
        return qty;
    }

    /**
     * Sums the quantity from every ET_DATA row for the article. SAP can return
     * the same article on multiple rows, so using only etDataMap would lose rows.
     */
    private double resolveArticleTotalQty(String article) {
        String target = normalizeArticle(article);
        String targetNoZeros = normalizeArticle(UIFuncs.removeLeadingZeros(article));
        double total = 0;
        for (int i = 0; i < referenceEtData.length(); i++) {
            JSONObject row = referenceEtData.optJSONObject(i);
            if (row == null || SapJsonRows.isMetadataRow(row, "CRATE", "ARTICLE")) {
                continue;
            }
            String rowArticle = normalizeArticle(row.optString("ARTICLE", ""));
            String rowMatnr = normalizeArticle(row.optString("MATNR", ""));
            if (target.equals(rowArticle)
                    || target.equals(rowMatnr)
                    || targetNoZeros.equals(normalizeArticle(UIFuncs.removeLeadingZeros(rowArticle)))
                    || targetNoZeros.equals(normalizeArticle(UIFuncs.removeLeadingZeros(rowMatnr)))) {
                total += resolveMaxQty(row);
            }
        }
        return total;
    }

    private static double resolvePackQty(JSONObject eanRow) {
        // ZZEAN_DATA uses QUNANTITY (SAP spelling).
        double packQty = parseQty(eanRow, "QUNANTITY", "UMREZ", 1);
        if (packQty <= 0) {
            packQty = parseQty(eanRow, "QUANTITY", "", 1);
        }
        return packQty > 0 ? packQty : 1;
    }

    private JSONObject findEanRow(String barcode) {
        if (TextUtils.isEmpty(barcode)) {
            return null;
        }
        String key = barcode.trim().toUpperCase(Locale.ROOT);
        JSONObject row = eanDataMap.get(key);
        if (row != null) {
            return row;
        }
        for (Map.Entry<String, JSONObject> entry : eanDataMap.entrySet()) {
            if (key.equals(entry.getKey().trim().toUpperCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return null;
    }
    private JSONObject findEtDataForArticle(String article) {
        String target = normalizeArticle(article);
        if (target.isEmpty()) {
            return null;
        }
        JSONObject direct = etDataMap.get(target);
        if (direct != null) {
            return direct;
        }
        String targetNoZeros = normalizeArticle(UIFuncs.removeLeadingZeros(article));
        for (JSONObject row : etDataMap.values()) {
            String etArticle = normalizeArticle(row.optString("ARTICLE", ""));
            String etMatnr = normalizeArticle(row.optString("MATNR", ""));
            if (target.equals(etArticle)
                    || target.equals(etMatnr)
                    || targetNoZeros.equals(normalizeArticle(UIFuncs.removeLeadingZeros(etArticle)))
                    || targetNoZeros.equals(normalizeArticle(UIFuncs.removeLeadingZeros(etMatnr)))) {
                return row;
            }
        }
        return null;
    }

    private String resolveHubFromEtRow(JSONObject etRow) {
        if (etRow == null) {
            return "";
        }
        // Purposed HUB field binds ZONE_CRATE from ET_DATA (not HUB).
        String[] hubKeys = {"ZONE_CRATE", "PLT_REC_HUBZONE", "HUB_STN", "HUBSTN", "HUB_ZONE", "HUBZONE"};
        for (String key : hubKeys) {
            String hub = etRow.optString(key, "").trim();
            if (!hub.isEmpty()) {
                return hub;
            }
        }
        return "";
    }

    private void requestCrateValidate(String scannedCrate) {
        if (TextUtils.isEmpty(scannedCrate)) {
            return;
        }
        if (crateValidateInProgress) {
            Log.d(TAG, "requestCrateValidate skipped — validation already in progress");
            // Discard extra scan chars that wedge-append while the first validate is running.
            txtScanCrate.post(() -> {
                if (txtScanCrate != null) {
                    txtScanCrate.setText("");
                }
            });
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
            crateValidateInProgress = true;
            pendingCrateScan = scannedCrate;
            // Clear after this TextWatcher cycle so a fast second scan cannot append.
            txtScanCrate.post(() -> {
                if (txtScanCrate != null) {
                    txtScanCrate.setText("");
                }
            });
            args.put("bapiname", Vars.ZWM_PTL_GRT_MSA_CRATE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_CRATE", scannedCrate);
            showProcessingAndSubmit(Vars.ZWM_PTL_GRT_MSA_CRATE_VALIDATE, REQUEST_VALIDATE_CRATE, args);
        } catch (JSONException e) {
            crateValidateInProgress = false;
            pendingCrateScan = "";
            Log.e(TAG, "requestCrateValidate", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void validateArticleScan(String barcode) {
        if (TextUtils.isEmpty(barcode)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (barcode.equalsIgnoreCase(lastArticleScanValue) && (now - lastArticleScanAtMs) < 1200) {
            return;
        }
        lastArticleScanValue = barcode;
        lastArticleScanAtMs = now;

        if (TextUtils.isEmpty(validatedCrate) || TextUtils.isEmpty(emptyCrate)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan MSA Crate and MSA REV Crate first.");
            txtScanArticle.setText("");
            if (TextUtils.isEmpty(validatedCrate)) {
                txtScanCrate.requestFocus();
            } else {
                txtEmptyCrateScan.requestFocus();
            }
            return;
        }

        JSONObject eanRow = findEanRow(barcode);
        String article = barcode;
        if (eanRow != null) {
            article = eanRow.optString("ARTICLE", "").trim();
            if (article.isEmpty()) {
                article = eanRow.optString("MATNR", "").trim();
            }
        }
        if (article.isEmpty()) {
            article = barcode;
        }

        JSONObject etRow = findEtDataForArticle(article);
        if (etRow == null) {
            etRow = findEtDataForArticle(barcode);
        }

        double packQty = eanRow == null ? 1 : resolvePackQty(eanRow);
        String etArticle = article;
        if (etRow != null) {
            etArticle = etRow.optString("ARTICLE", article).trim();
            if (etArticle.isEmpty()) {
                etArticle = etRow.optString("MATNR", article).trim();
            }
        }
        String articleKey = normalizeArticle(etArticle);
        double maxQty = etRow == null ? 0 : resolveArticleTotalQty(etArticle);
        double alreadyScanned = scannedQtyByArticle.containsKey(articleKey)
                ? scannedQtyByArticle.get(articleKey) : 0;
        boolean hasOpenQuantity = etRow != null && maxQty > 0 && alreadyScanned < maxQty;

        // No open quantity (missing ET_DATA / qty finished) → logged-in HUB + local cache.
        if (!hasOpenQuantity) {
            storeArticleInCache(etArticle, barcode, packQty);
            return;
        }

        // Has open quantity → HUB Crate Scan, then auto RFC (not cached).
        currentArticle = etArticle;
        currentEtRow = etRow;
        currentMaxQty = maxQty;
        currentScannedQty = alreadyScanned + packQty;
        currentScanQty = packQty;

        txtArticle.setText(UIFuncs.removeLeadingZeros(etArticle));
        String proposedHub = resolveHubFromEtRow(etRow);
        txtProposedHub.setText(proposedHub);
        hubMapCrate = "";
        txtHubMapCrate.setText("");
        UIFuncs.enableInput(con, txtHubMapCrate);
        txtScanArticle.setText("");
        txtHubMapCrate.requestFocus();
    }

    /** Fallback Proposed HUB when article is not in ET_DATA / has no open qty: logged-in plant (WERKS). */
    private String getUnmatchedProposedHub() {
        return TextUtils.isEmpty(WERKS) ? "" : WERKS.trim();
    }

    private void storeArticleInCache(String article, String ean, double quantity) {
        UIFuncs.errorSound(con);
        String displayArticle = TextUtils.isEmpty(article) ? ean : article;
        String proposedHub = getUnmatchedProposedHub();
        txtArticle.setText(UIFuncs.removeLeadingZeros(displayArticle));
        txtProposedHub.setText(proposedHub);
        appendPendingScan(displayArticle, ean, quantity > 0 ? quantity : 1, proposedHub, false);
        UIFuncs.disableInput(con, txtHubMapCrate);
        txtHubMapCrate.setText("");
        hubMapCrate = "";
        showBottomToast("No open quantity. Stored with Proposed HUB " + proposedHub);
        txtScanArticle.setText("");
        txtScanArticle.requestFocus();
    }

    private void confirmUnmatchedArticle(final String barcode) {
        storeArticleInCache(barcode, barcode, 1);
    }

    private void showBottomToast(String message) {
        if (con == null || TextUtils.isEmpty(message)) {
            return;
        }
        Toast toast = Toast.makeText(con, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.show();
    }

    private void requestMatchedArticleTag(String scannedHubCrate) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HUB_ARTICLE_TAG_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_SOURCE_CRATE", validatedCrate);

            JSONObject row = buildMatchedArticleTagRow(scannedHubCrate);
            JSONArray itData = new JSONArray();
            itData.put(row);
            args.put("IT_DATA", itData);

            autoTagInProgress = true;
            UIFuncs.disableInput(con, txtHubMapCrate);
            Log.d(TAG, "matched article tag payload -> " + args);
            showProcessingAndSubmit(
                    Vars.ZWM_PTL_HUB_ARTICLE_TAG_CRATE,
                    REQUEST_TAG_HUB,
                    args);
        } catch (JSONException e) {
            autoTagInProgress = false;
            Log.e(TAG, "requestMatchedArticleTag", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private JSONObject buildMatchedArticleTagRow(String scannedHubCrate) throws JSONException {
        JSONObject row = new JSONObject();
        copyIfPresent(currentEtRow, row, "PICKLIST");
        copyIfPresent(currentEtRow, row, "BIN");
        copyIfPresent(currentEtRow, row, "ETYPE");
        copyIfPresent(currentEtRow, row, "WAVE");
        copyIfPresent(currentEtRow, row, "TANUM");
        copyIfPresent(currentEtRow, row, "PALATE");
        copyIfPresent(currentEtRow, row, "STORE");
        copyIfPresent(currentEtRow, row, "ITEMNO");
        copyIfPresent(currentEtRow, row, "EBELN");
        copyIfPresent(currentEtRow, row, "EBELP");
        copyIfPresent(currentEtRow, row, "TAG");
        copyIfPresent(currentEtRow, row, "HU");
        copyIfPresent(currentEtRow, row, "ZONE");
        copyIfPresent(currentEtRow, row, "MATKL");
        copyIfPresent(currentEtRow, row, "DIVISION");

        String quantity = currentEtRow.optString("QUANTITY", "").trim();
        if (quantity.isEmpty()) {
            quantity = currentEtRow.optString("QTY", "").trim();
        }
        if (quantity.isEmpty()) {
            quantity = Util.convertToDoubleString(String.valueOf(currentMaxQty));
        }

        row.put("MSA_CRATE", emptyCrate);
        row.put("ARTICLE", currentArticle);
        row.put("QUANTITY", quantity);
        row.put("CRATE", scannedHubCrate);
        row.put("PLANT", WERKS);
        row.put("SCAN_QTY", Util.convertToDoubleString(String.valueOf(currentScanQty)));
        row.put("ZONE_STATION", UIFuncs.toUpperTrim(txtProposedHub));
        row.put("FLOOR", getSelectedFloor());
        row.put("HUB", UIFuncs.toUpperTrim(txtProposedHub));
        return row;
    }

    private static void copyIfPresent(JSONObject source, JSONObject target, String key)
            throws JSONException {
        if (source != null && source.has(key) && !source.isNull(key)) {
            target.put(key, source.get(key));
        }
    }

    private void appendPendingScan(String article, String ean, double quantity,
                                   String proposedHub, boolean matched) {
        JSONObject row = new JSONObject();
        try {
            row.put("MSA_CRATE", validatedCrate);
            row.put("EMPTY_CRATE", emptyCrate);
            row.put("ARTICLE", article);
            row.put("EAN11", ean);
            row.put("SCAN_QTY", Util.convertToDoubleString(String.valueOf(quantity)));
            row.put("FLOOR", getSelectedFloor());
            row.put("HUB", proposedHub);
            row.put("HUB_MAP_CRATE", hubMapCrate);
            row.put("_MATCHED", matched);
            pendingScans.put(row);
            persistLocalSession();
            updateActionButtons();
        } catch (JSONException e) {
            Log.e(TAG, "appendPendingScan", e);
            box.getErrBox(e);
        }
    }

    private void applyHubMapCrateToPendingRows() {
        for (int i = 0; i < pendingScans.length(); i++) {
            JSONObject row = pendingScans.optJSONObject(i);
            if (row != null) {
                try {
                    row.put("HUB_MAP_CRATE", hubMapCrate);
                    row.put("EMPTY_CRATE", emptyCrate);
                    row.put("MSA_CRATE", validatedCrate);
                } catch (JSONException e) {
                    Log.e(TAG, "applyHubMapCrateToPendingRows", e);
                }
            }
        }
    }

    private void savePendingScans() {
        if (pendingScans.length() == 0) {
            box.getBox("Validation", "Please scan at least one Article/EAN.");
            txtScanArticle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(validatedCrate)) {
            box.getBox("Validation", "Please scan MSA Crate first.");
            txtScanCrate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(emptyCrate)) {
            box.getBox("Validation", "Please scan MSA REV Crate first.");
            txtEmptyCrateScan.requestFocus();
            return;
        }
        applyHubMapCrateToPendingRows();
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZGRTRET_SAVE_CRATE_DETAILS);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_CRATE", emptyCrate);
            args.put("IM_MSA_CRATE", validatedCrate);
            args.put("IM_NATURE", "GRT");
            args.put("IT_DATA", buildSaveItData());

            Log.d(TAG, "save crate details payload -> " + args);
            showProcessingAndSubmit(Vars.ZGRTRET_SAVE_CRATE_DETAILS, REQUEST_SAVE_CACHE, args);
        } catch (JSONException e) {
            Log.e(TAG, "savePendingScans", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    /**
     * Builds {@code IT_DATA} rows for {@code ZECOM_CANCEL_QC_PUTAWAY_ST}.
     * One row per cached scan — duplicate scans of the same article are
     * kept as separate records (no aggregation).
     */
    private JSONArray buildSaveItData() throws JSONException {
        JSONArray itData = new JSONArray();
        for (int i = 0; i < pendingScans.length(); i++) {
            JSONObject storedRow = pendingScans.getJSONObject(i);
            String ean = storedRow.optString("EAN11", "").trim();
            if (ean.isEmpty()) {
                ean = storedRow.optString("ARTICLE", "").trim();
            }
            double scanQty = Util.convertStringToDouble(storedRow.optString("SCAN_QTY", "0"));
            String hub = storedRow.optString("HUB", "").trim();
            String lgpla = !TextUtils.isEmpty(hubMapCrate) ? hubMapCrate : hub;

            JSONObject requestRow = new JSONObject();
            requestRow.put("LGNUM", "");
            requestRow.put("LGPLA", lgpla);
            requestRow.put("MATNR", "");
            requestRow.put("WERKS", WERKS);
            requestRow.put("MENGE", "");
            requestRow.put("LGTYP", "");
            requestRow.put("MAKTX", "");
            requestRow.put("MATERIAL", "");
            requestRow.put("SCANQTY", Util.convertToDoubleString(String.valueOf(scanQty)));
            requestRow.put("EAN11", ean);
            itData.put(requestRow);
        }
        return itData;
    }

    private void requestEmptyShortClose() {
        String msaCrate = validatedCrate;
        if (TextUtils.isEmpty(msaCrate)) {
            msaCrate = UIFuncs.toUpperTrim(txtCrate);
        }
        if (TextUtils.isEmpty(msaCrate)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate MSA Crate first.");
            txtScanCrate.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZMM_V24_SHORT_CL_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_MSA_CRATE", msaCrate);
            showProcessingAndSubmit(Vars.ZMM_V24_SHORT_CL_RFC, REQUEST_EMPTY_SHORT_CLOSE, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestEmptyShortClose", e);
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
                        releaseRequestGuard(request);
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "No response from Server");
                    } else if (responsebody.length() == 0) {
                        releaseRequestGuard(request);
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

    private void releaseRequestGuard(int request) {
        if (request == REQUEST_VALIDATE_CRATE) {
            crateValidateInProgress = false;
        } else if (request == REQUEST_VALIDATE_REV_CRATE) {
            revCrateValidateInProgress = false;
        }
    }

    private void handleRfcResponse(JSONObject responsebody, int request) {
        releaseRequestGuard(request);
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
                } else if (request == REQUEST_VALIDATE_REV_CRATE) {
                    clearAfterRevCrateValidateFailure();
                } else if (request == REQUEST_TAG_HUB) {
                    autoTagInProgress = false;
                    UIFuncs.enableInput(con, txtHubMapCrate);
                    txtHubMapCrate.requestFocus();
                }
                return;
            }

            if (!TextUtils.isEmpty(message) && request != REQUEST_VALIDATE_REV_CRATE) {
                box.getBox("Success", message);
            }

            if (request == REQUEST_VALIDATE_CRATE) {
                handleCrateValidateSuccess(responsebody);
            } else if (request == REQUEST_VALIDATE_REV_CRATE) {
                handleRevCrateValidateSuccess();
            } else if (request == REQUEST_TAG_HUB) {
                handleHubTagSuccess();
            } else if (request == REQUEST_SAVE_CACHE) {
                handleCacheSaveSuccess();
            } else if (request == REQUEST_EMPTY_SHORT_CLOSE) {
                handleEmptyShortCloseSuccess();
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleRfcResponse", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void handleCrateValidateSuccess(JSONObject responsebody) throws JSONException {
        // Use the crate that was submitted — EditText may have been cleared or
        // received extra chars from a fast second scan while the API was in flight.
        validatedCrate = pendingCrateScan;
        if (TextUtils.isEmpty(validatedCrate)) {
            validatedCrate = UIFuncs.toUpperTrim(txtScanCrate);
        }
        txtCrate.setText(validatedCrate);
        txtScanCrate.setText("");
        pendingCrateScan = "";
        UIFuncs.disableInput(con, txtScanCrate);

        resetArticleFields();
        etDataMap = new HashMap<>();
        eanDataMap = new HashMap<>();
        scannedQtyByArticle = new HashMap<>();
        referenceEtData = responsebody.optJSONArray("ET_DATA");
        referenceEanData = responsebody.optJSONArray("ET_EAN_DATA");
        if (referenceEtData == null) {
            referenceEtData = new JSONArray();
        }
        if (referenceEanData == null) {
            referenceEanData = new JSONArray();
        }

        if (referenceEtData.length() > 0) {
            int etStart = SapJsonRows.startIndex(referenceEtData, "CRATE", "ARTICLE");
            for (int i = etStart; i < referenceEtData.length(); i++) {
                JSONObject row = referenceEtData.getJSONObject(i);
                if (SapJsonRows.isMetadataRow(row, "CRATE", "ARTICLE")) {
                    continue;
                }
                String article = row.optString("ARTICLE", "").trim();
                String matnr = row.optString("MATNR", "").trim();
                if (!article.isEmpty()) {
                    etDataMap.put(normalizeArticle(article), row);
                }
                if (!matnr.isEmpty()) {
                    etDataMap.put(normalizeArticle(matnr), row);
                }
            }
        }

        if (referenceEanData.length() > 0) {
            int eanStart = SapJsonRows.startIndex(referenceEanData, "EAN11", "ARTICLE");
            for (int i = eanStart; i < referenceEanData.length(); i++) {
                JSONObject row = referenceEanData.getJSONObject(i);
                if (SapJsonRows.isMetadataRow(row, "EAN11", "ARTICLE")) {
                    continue;
                }
                String ean = row.optString("EAN11", "").trim().toUpperCase(Locale.ROOT);
                if (!ean.isEmpty()) {
                    eanDataMap.put(ean, row);
                }
            }
        }

        // Keep floor selectable so user can change it before article scanning.
        ddFloor.setEnabled(true);
        UIFuncs.enableInput(con, txtEmptyCrateScan);
        txtEmptyCrateScan.requestFocus();
        persistLocalSession();
        updateActionButtons();
        if (etDataMap.isEmpty() && eanDataMap.isEmpty()) {
            box.getBox("No Records",
                    "No article/EAN data returned. Unmatched scans can still be stored after confirmation.");
        }
    }

    private void handleRevCrateValidateSuccess() {
        emptyCrate = pendingRevCrateScan;
        if (TextUtils.isEmpty(emptyCrate)) {
            emptyCrate = UIFuncs.toUpperTrim(txtEmptyCrateScan);
        }
        txtEmptyCrate.setText(emptyCrate);
        txtEmptyCrateScan.setText("");
        pendingRevCrateScan = "";
        UIFuncs.disableInput(con, txtEmptyCrateScan);
        UIFuncs.enableInput(con, txtScanArticle);
        UIFuncs.enableInput(con, txtHubMapCrate);
        persistLocalSession();
        updateActionButtons();
        txtScanArticle.requestFocus();
    }

    private void handleHubTagSuccess() {
        scannedQtyByArticle.put(normalizeArticle(currentArticle), currentScannedQty);
        persistLocalSession();
        autoTagInProgress = false;
        hubMapCrate = "";
        txtHubMapCrate.setText("");
        UIFuncs.disableInput(con, txtHubMapCrate);
        resetArticleFields();
        UIFuncs.enableInput(con, txtScanArticle);
        txtScanArticle.post(() -> txtScanArticle.requestFocus());
    }

    private void handleCacheSaveSuccess() {
        clearLocalSession();
        resetScreen();
    }

    private void handleEmptyShortCloseSuccess() {
        resetScreen();
    }

    private void resetArticleFields() {
        currentArticle = "";
        currentEtRow = null;
        currentMaxQty = 0;
        currentScannedQty = 0;
        currentScanQty = 0;
        txtArticle.setText("");
        txtProposedHub.setText("");
        txtScanArticle.setText("");
    }

    private void clearAfterCrateValidateFailure() {
        validatedCrate = "";
        pendingCrateScan = "";
        txtCrate.setText("");
        txtScanCrate.setText("");
        UIFuncs.enableInput(con, txtScanCrate);
        updateActionButtons();
        txtScanCrate.requestFocus();
    }

    private void clearAfterRevCrateValidateFailure() {
        emptyCrate = "";
        pendingRevCrateScan = "";
        txtEmptyCrate.setText("");
        txtEmptyCrateScan.setText("");
        UIFuncs.enableInput(con, txtEmptyCrateScan);
        updateActionButtons();
        txtEmptyCrateScan.requestFocus();
    }

    private void resetAfterFloorChange() {
        validatedCrate = "";
        emptyCrate = "";
        hubMapCrate = "";
        pendingCrateScan = "";
        pendingRevCrateScan = "";
        pendingScans = new JSONArray();
        referenceEtData = new JSONArray();
        referenceEanData = new JSONArray();
        etDataMap = new HashMap<>();
        eanDataMap = new HashMap<>();
        scannedQtyByArticle = new HashMap<>();
        txtCrate.setText("");
        txtScanCrate.setText("");
        txtEmptyCrateScan.setText("");
        txtEmptyCrate.setText("");
        txtHubMapCrate.setText("");
        UIFuncs.disableInput(con, txtEmptyCrateScan);
        UIFuncs.disableInput(con, txtHubMapCrate);
        resetArticleFields();
        UIFuncs.disableInput(con, txtScanArticle);
        UIFuncs.enableInput(con, txtScanCrate);
    }

    private void resetScreen() {
        resetAfterFloorChange();
        clearLocalSession();
        ddFloor.setEnabled(true);
        ddFloor.setText(FLOOR_OPTIONS.get(0));
        floorSelected = true;
        UIFuncs.enableInput(con, txtScanCrate);
        updateActionButtons();
        txtScanCrate.post(() -> txtScanCrate.requestFocus());
    }

    private boolean hasSessionProgress() {
        return (pendingScans != null && pendingScans.length() > 0)
                || !TextUtils.isEmpty(validatedCrate)
                || !TextUtils.isEmpty(emptyCrate)
                || !TextUtils.isEmpty(hubMapCrate)
                || (referenceEtData != null && referenceEtData.length() > 0);
    }

    private void persistLocalSession() {
        if (localPreferences == null) {
            return;
        }
        if (!hasSessionProgress()) {
            localPreferences.edit().remove(LOCAL_SESSION).apply();
            return;
        }
        JSONObject session = new JSONObject();
        try {
            session.put("FLOOR", getSelectedFloor());
            session.put("MSA_CRATE", validatedCrate == null ? "" : validatedCrate);
            session.put("EMPTY_CRATE", emptyCrate == null ? "" : emptyCrate);
            session.put("HUB_MAP_CRATE", hubMapCrate == null ? "" : hubMapCrate);
            session.put("ET_DATA", referenceEtData == null ? new JSONArray() : referenceEtData);
            session.put("ET_EAN_DATA", referenceEanData == null ? new JSONArray() : referenceEanData);
            session.put("SCANS", pendingScans == null ? new JSONArray() : pendingScans);
            JSONObject scannedQuantities = new JSONObject();
            for (Map.Entry<String, Double> entry : scannedQtyByArticle.entrySet()) {
                scannedQuantities.put(entry.getKey(), entry.getValue());
            }
            session.put("SCANNED_QTY", scannedQuantities);
            localPreferences.edit().putString(LOCAL_SESSION, session.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "persistLocalSession", e);
        }
    }

    private void restoreLocalSession() {
        if (localPreferences == null) {
            return;
        }
        String stored = localPreferences.getString(LOCAL_SESSION, "");
        if (TextUtils.isEmpty(stored)) {
            updateActionButtons();
            return;
        }
        try {
            JSONObject session = new JSONObject(stored);
            String floor = session.optString("FLOOR", "0");
            int floorPosition = FLOOR_OPTIONS.indexOf(floor);
            if (floorPosition >= 0) {
                ddFloor.setText(FLOOR_OPTIONS.get(floorPosition));
                floorSelected = true;
            } else {
                ddFloor.setText(FLOOR_OPTIONS.get(0));
                floorSelected = true;
            }

            validatedCrate = session.optString("MSA_CRATE", "");
            emptyCrate = session.optString("EMPTY_CRATE", "");
            hubMapCrate = session.optString("HUB_MAP_CRATE", "");
            referenceEtData = session.optJSONArray("ET_DATA");
            referenceEanData = session.optJSONArray("ET_EAN_DATA");
            pendingScans = session.optJSONArray("SCANS");
            if (referenceEtData == null) {
                referenceEtData = new JSONArray();
            }
            if (referenceEanData == null) {
                referenceEanData = new JSONArray();
            }
            if (pendingScans == null) {
                pendingScans = new JSONArray();
            }
            rebuildReferenceMaps();
            rebuildScannedQuantities();
            JSONObject restoredQuantities = session.optJSONObject("SCANNED_QTY");
            if (restoredQuantities != null) {
                Iterator<String> keys = restoredQuantities.keys();
                while (keys.hasNext()) {
                    String article = keys.next();
                    scannedQtyByArticle.put(
                            normalizeArticle(article),
                            restoredQuantities.optDouble(article, 0));
                }
            }

            txtCrate.setText(validatedCrate);
            txtEmptyCrate.setText(emptyCrate);
            txtHubMapCrate.setText(hubMapCrate);
            ddFloor.setEnabled(true);
            if (!TextUtils.isEmpty(validatedCrate)) {
                UIFuncs.disableInput(con, txtScanCrate);
                if (TextUtils.isEmpty(emptyCrate)) {
                    UIFuncs.enableInput(con, txtEmptyCrateScan);
                    txtEmptyCrateScan.requestFocus();
                } else {
                    UIFuncs.disableInput(con, txtEmptyCrateScan);
                    UIFuncs.enableInput(con, txtScanArticle);
                    UIFuncs.enableInput(con, txtHubMapCrate);
                    txtScanArticle.requestFocus();
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "restoreLocalSession", e);
            clearLocalSession();
            resetScreen();
        }
        updateActionButtons();
    }

    private void rebuildReferenceMaps() throws JSONException {
        etDataMap = new HashMap<>();
        eanDataMap = new HashMap<>();
        int etStart = SapJsonRows.startIndex(referenceEtData, "CRATE", "ARTICLE");
        for (int i = etStart; i < referenceEtData.length(); i++) {
            JSONObject row = referenceEtData.getJSONObject(i);
            if (SapJsonRows.isMetadataRow(row, "CRATE", "ARTICLE")) {
                continue;
            }
            String article = row.optString("ARTICLE", "").trim();
            String matnr = row.optString("MATNR", "").trim();
            if (!article.isEmpty()) {
                etDataMap.put(normalizeArticle(article), row);
            }
            if (!matnr.isEmpty()) {
                etDataMap.put(normalizeArticle(matnr), row);
            }
        }
        int eanStart = SapJsonRows.startIndex(referenceEanData, "EAN11", "ARTICLE");
        for (int i = eanStart; i < referenceEanData.length(); i++) {
            JSONObject row = referenceEanData.getJSONObject(i);
            if (!SapJsonRows.isMetadataRow(row, "EAN11", "ARTICLE")) {
                String ean = row.optString("EAN11", "").trim().toUpperCase(Locale.ROOT);
                if (!ean.isEmpty()) {
                    eanDataMap.put(ean, row);
                }
            }
        }
    }

    private void rebuildScannedQuantities() {
        scannedQtyByArticle = new HashMap<>();
        for (int i = 0; i < pendingScans.length(); i++) {
            JSONObject row = pendingScans.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String article = normalizeArticle(row.optString("ARTICLE", ""));
            double quantity = Util.convertStringToDouble(row.optString("SCAN_QTY", "0"));
            Double existing = scannedQtyByArticle.get(article);
            scannedQtyByArticle.put(article, (existing == null ? 0 : existing) + quantity);
        }
    }

    private void clearLocalSession() {
        pendingScans = new JSONArray();
        if (localPreferences != null) {
            localPreferences.edit().remove(LOCAL_SESSION).apply();
        }
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasUnsavedScans = pendingScans != null && pendingScans.length() > 0;
        boolean hasMsaCrate = !TextUtils.isEmpty(validatedCrate)
                || !TextUtils.isEmpty(UIFuncs.toUpperTrim(txtCrate));
        if (btnBack != null) {
            btnBack.setEnabled(!hasUnsavedScans);
            btnBack.setAlpha(hasUnsavedScans ? 0.45f : 1f);
        }
        if (btnEmpty != null) {
            btnEmpty.setEnabled(hasMsaCrate);
            btnEmpty.setAlpha(hasMsaCrate ? 1f : 0.45f);
        }
        if (btnSave != null) {
            btnSave.setEnabled(hasUnsavedScans);
            btnSave.setAlpha(hasUnsavedScans ? 1f : 0.45f);
        }
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private Response.ErrorListener volleyErrorListener(int request) {
        return error -> {
            releaseRequestGuard(request);
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
            } else if (request == REQUEST_VALIDATE_REV_CRATE) {
                clearAfterRevCrateValidateFailure();
            } else if (request == REQUEST_TAG_HUB) {
                autoTagInProgress = false;
                UIFuncs.enableInput(con, txtHubMapCrate);
                txtHubMapCrate.requestFocus();
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
            if (pendingScans.length() == 0) {
                BackPressHandler.confirmCloseProcess(fm, requireContext());
            }
        } else if (view.getId() == R.id.btn_ptl_grt_hub_sorting_scan_crate_empty) {
            requestEmptyShortClose();
        } else if (view.getId() == R.id.btn_ptl_grt_hub_sorting_scan_crate_save) {
            savePendingScans();
        }
    }
}
