package com.v2retail.dotvik.dc.ptlnew.withpallate;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.PicklistData;
import com.v2retail.dotvik.modal.grt.createhu.HUEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PTL — Article Putway Store Wise (redesign 2026-05-09).
 *
 * Flow per spec doc "PTL_Article Putway Store Wise":
 *  1. Scan FLR STATION → {@link Vars#ZWM_PTL_STATION_VALIDATE} → SAP returns {@code EX_HUB}.
 *     The scanned station is shown in the (gray) Station field; HUB is shown in the
 *     (gray) HUB field.
 *  2. Scan Crate → {@link Vars#ZWM_PTL_STATION_CRATE_VALIDATE} (IM_USER, IM_WERKS,
 *     IM_ZONE_STATION, IM_CRATE, IM_HUB) → SAP returns ET_DATA (article/store/floor)
 *     and ET_EAN_DATA (barcodes). Cursor moves to Scan Article.
 *  3. Scan Article (barcode) → validated locally against ET_EAN_DATA-EAN11; the
 *     resolved MATNR is matched against ET_DATA-ARTICLE to populate Article,
 *     Proposed Store and Store Floor. IS_DATA is prepared for the HU step.
 *  4. Scan HU → {@link Vars#ZWM_PTL_ZONE_HU_VALIDATE_V3} (IM_USER, IM_WERKS, IS_DATA) immediately
 *     after a valid HU scan (no Submit button). On success the row's pending qty is decremented
 *     and the cursor returns to Scan Article for the next pick.
 */
public class FragmentPTLNewArticlePutwayStorewise extends Fragment implements View.OnClickListener {

    private static final int REQUEST_VALIDATE_STATION = 1500;
    private static final int REQUEST_VALIDATE_STATION_CRATE = 1501;
    private static final int REQUEST_VALIDATE_ZONE_HU = 1502;

    private static final String TAG = FragmentPTLNewArticlePutwayStorewise.class.getName();

    View rootView;
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back;
    EditText txt_scan_station, txt_station, txt_hub;
    EditText txt_scan_crate, txt_crate;
    EditText txt_scan_article, txt_article, txt_proposed_store, txt_store_floor;
    EditText txt_scan_hu, txt_hu, txt_pending_qty;

    /** FLR Station confirmed by validate RFC; required for crate/HU calls. */
    private String validatedStation = "";
    /** HUB returned by validate RFC for the FLR Station; passed back as IM_HUB. */
    private String validatedHub = "";

    Map<String, PicklistData> etDataMap = new LinkedHashMap<>();
    Map<String, HUEANData> eanDataMap = new HashMap<>();
    PicklistData currentScan = null;
    /** HU from the last {@link Vars#ZWM_PTL_ZONE_HU_VALIDATE_V3} payload (do not read {@code txt_scan_hu} on response). */
    private String lastHuSubmitted = "";
    /** Prevents double RFC when the scanner sends keystrokes + ENTER. */
    private boolean huValidateInFlight = false;

    /** Brand red used in the screen footer / spec mock-up. */
    private static final int ACTION_BAR_RED = 0xFFE71821;

    public FragmentPTLNewArticlePutwayStorewise() {
        // Required empty public constructor
    }

    public static FragmentPTLNewArticlePutwayStorewise newInstance() {
        return new FragmentPTLNewArticlePutwayStorewise();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        Process_Selection_Activity activity = (Process_Selection_Activity) getActivity();
        if (activity != null) {
            activity.setActionBarTitle("PTL- Article Putway Store Wise");
            applyRedActionBar(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        restoreActionBarBackground();
    }

    /**
     * Re-tints the activity's action bar to brand red while this screen is visible.
     * On pause we revert to the app theme's {@code colorPrimary} so other fragments
     * keep the default appearance.
     */
    private void applyRedActionBar(AppCompatActivity activity) {
        ActionBar bar = activity.getSupportActionBar();
        if (bar == null) {
            return;
        }
        bar.setBackgroundDrawable(new ColorDrawable(ACTION_BAR_RED));
        // Force a redraw — some devices don't repaint until the title is reset.
        CharSequence title = bar.getTitle();
        bar.setTitle("");
        bar.setTitle(title);
    }

    private void restoreActionBarBackground() {
        Process_Selection_Activity activity = (Process_Selection_Activity) getActivity();
        if (activity == null) {
            return;
        }
        ActionBar bar = activity.getSupportActionBar();
        if (bar == null) {
            return;
        }
        // Revert to the theme's colorPrimary (#1A1A1A — see app/src/main/res/values/colors.xml).
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1A1A1A")));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_new_article_putway_storewise, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_scan_station = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_scan_station);
        txt_station = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_station);
        txt_hub = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_hub);

        txt_scan_crate = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_scan_crate);
        txt_crate = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_crate);

        txt_scan_article = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_scan_article);
        txt_article = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_article);
        txt_proposed_store = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_proposed_store);
        txt_store_floor = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_store_floor);

        txt_scan_hu = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_scan_hu);
        txt_hu = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_hu);
        txt_pending_qty = rootView.findViewById(R.id.txt_ptl_new_article_putway_storewise_pending_qty);

        btn_back = rootView.findViewById(R.id.btn_ptl_new_article_putway_storewise_back);
        btn_back.setOnClickListener(this);

        clearAll();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_ptl_new_article_putway_storewise_back) {
            box.confirmBack(fm, con);
        }
    }

    private void endHuValidateInFlight() {
        huValidateInFlight = false;
        if (txt_scan_hu != null) {
            UIFuncs.enableInput(con, txt_scan_hu);
        }
    }

    /**
     * After a valid HU scan (or IME Done), posts {@link Vars#ZWM_PTL_ZONE_HU_VALIDATE_V3} immediately.
     */
    private void onHuScanned(String hu) {
        if (huValidateInFlight) {
            return;
        }
        if (validatedStation.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Please scan FLR Station first.");
            txt_scan_station.requestFocus();
            return;
        }
        if (currentScan == null) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Please scan a Crate and an Article before scanning HU.");
            if (txt_scan_crate.isEnabled()) {
                txt_scan_crate.requestFocus();
            } else {
                txt_scan_article.requestFocus();
            }
            txt_scan_hu.setText("");
            return;
        }
        String huNorm = hu == null ? "" : hu.trim().toUpperCase();
        if (huNorm.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Please scan an HU.");
            txt_scan_hu.requestFocus();
            return;
        }
        txt_hu.setText(huNorm);
        huValidateInFlight = true;
        UIFuncs.disableInput(con, txt_scan_hu);
        validateZoneHU(huNorm);
    }

    /**
     * Reset everything to the initial state — only the Scan FLR Station field is editable.
     */
    private void clearAll() {
        currentScan = null;
        validatedStation = "";
        validatedHub = "";
        etDataMap = new LinkedHashMap<>();
        eanDataMap = new HashMap<>();

        txt_scan_station.setText("");
        txt_station.setText("");
        txt_hub.setText("");
        txt_scan_crate.setText("");
        txt_crate.setText("");
        txt_scan_article.setText("");
        txt_article.setText("");
        txt_proposed_store.setText("");
        txt_store_floor.setText("");
        txt_scan_hu.setText("");
        txt_hu.setText("");
        txt_pending_qty.setText("");

        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.enableInput(con, txt_scan_station);
        txt_scan_station.requestFocus();
    }

    /**
     * Reset everything that depends on a crate (article/store/HU rows + pending qty).
     * Called when a fresh crate is about to be scanned and after a successful HU put-away
     * when the crate has been fully consumed.
     */
    private void clearForNextCrate() {
        currentScan = null;
        etDataMap = new LinkedHashMap<>();
        eanDataMap = new HashMap<>();

        txt_crate.setText("");
        txt_scan_article.setText("");
        txt_article.setText("");
        txt_proposed_store.setText("");
        txt_store_floor.setText("");
        txt_scan_hu.setText("");
        txt_hu.setText("");
        txt_pending_qty.setText("");

        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.enableInput(con, txt_scan_crate);
        txt_scan_crate.requestFocus();
    }

    private void addInputEvents() {
        // ─── Scan FLR Station ────────────────────────────────────────────────
        txt_scan_station.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_station);
                    if (!value.isEmpty()) {
                        validateStation(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_station.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateStation(value);
                }
            }
        });

        // ─── Scan Crate ──────────────────────────────────────────────────────
        txt_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_crate);
                    if (!value.isEmpty() && !validatedStation.isEmpty()) {
                        validateStationCrate(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_crate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading && !validatedStation.isEmpty()) {
                    validateStationCrate(value);
                }
            }
        });

        // ─── Scan Article (barcode) ──────────────────────────────────────────
        txt_scan_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_scan_article);
                    if (!value.isEmpty()) {
                        validateArticleScan(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_scan_article.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

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

        // ─── Scan HU ─────────────────────────────────────────────────────────
        // Scanning HU immediately calls ZWM_PTL_ZONE_HU_VALIDATE_V3 (no Submit button).
        txt_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    // Scanner often sends ENTER after the watcher fires; avoid duplicate RFC.
                    String value = UIFuncs.toUpperTrim(txt_scan_hu);
                    if (!value.isEmpty() && !huValidateInFlight) {
                        onHuScanned(value);
                    }
                    return true;
                }
                return false;
            }
        });
        txt_scan_hu.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    onHuScanned(value);
                }
            }
        });
    }

    // ─── Step 1: validate FLR Station ──────────────────────────────────────────

    private void validateStation(String station) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_STATION_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_ZONE_STATION", station);
            showProcessingAndSubmit(Vars.ZWM_PTL_STATION_VALIDATE, REQUEST_VALIDATE_STATION, args);
        } catch (JSONException e) {
            handleException(e);
        }
    }

    private void onStationValidated(String scannedStation, JSONObject responsebody) {
        validatedStation = scannedStation;
        validatedHub = responsebody.optString("EX_HUB", "").trim();

        txt_station.setText(validatedStation);
        txt_hub.setText(validatedHub);

        UIFuncs.disableInput(con, txt_scan_station);
        UIFuncs.enableInput(con, txt_scan_crate);

        txt_scan_station.setText("");
        txt_scan_crate.requestFocus();
    }

    // ─── Step 2: validate Crate against Station + HUB ──────────────────────────

    private void validateStationCrate(String crate) {
        JSONObject args = new JSONObject();
        try {
            currentScan = null;
            args.put("bapiname", Vars.ZWM_PTL_STATION_CRATE_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_ZONE_STATION", validatedStation);
            args.put("IM_CRATE", crate);
            args.put("IM_HUB", validatedHub);
            showProcessingAndSubmit(Vars.ZWM_PTL_STATION_CRATE_VALIDATE, REQUEST_VALIDATE_STATION_CRATE, args);
        } catch (JSONException e) {
            handleException(e);
        }
    }

    private void setEtEanData(JSONObject responsebody) {
        try {
            etDataMap = new LinkedHashMap<>();
            eanDataMap = new HashMap<>();

            String scannedCrate = UIFuncs.toUpperTrim(txt_scan_crate);
            txt_crate.setText(scannedCrate);
            txt_scan_crate.setText("");
            txt_article.setText("");
            txt_proposed_store.setText("");
            txt_store_floor.setText("");
            txt_scan_article.setText("");
            txt_pending_qty.setText("");
            txt_scan_hu.setText("");
            txt_hu.setText("");

            JSONArray ET_DATA_ARRAY = responsebody.optJSONArray("ET_DATA");
            JSONArray ET_EAN_DATA_ARRAY = responsebody.optJSONArray("ET_EAN_DATA");
            int totalEtRecords = ET_DATA_ARRAY != null ? ET_DATA_ARRAY.length() : 0;
            int totalEanRecords = ET_EAN_DATA_ARRAY != null ? ET_EAN_DATA_ARRAY.length() : 0;

            // RFC adapter returns clean 0-indexed JSON arrays with NO header row;
            // loop must start at i=0 (matches the pattern in ZoneList parsing).
            for (int i = 0; i < totalEtRecords; i++) {
                PicklistData etData = new Gson().fromJson(ET_DATA_ARRAY.getJSONObject(i).toString(), PicklistData.class);
                if (etData == null || etData.getArticle() == null) {
                    continue;
                }
                etDataMap.put(etData.getArticle() + "-" + etData.getStore(), etData);
            }
            for (int i = 0; i < totalEanRecords; i++) {
                HUEANData eanData = new Gson().fromJson(ET_EAN_DATA_ARRAY.getJSONObject(i).toString(), HUEANData.class);
                if (eanData == null || eanData.getLgean11() == null) {
                    continue;
                }
                eanDataMap.put(eanData.getLgean11(), eanData);
            }

            if (!etDataMap.isEmpty() && !eanDataMap.isEmpty()) {
                UIFuncs.disableInput(con, txt_scan_crate);
                UIFuncs.enableInput(con, txt_scan_article);
                txt_scan_article.requestFocus();
                return;
            } else {
                box.getBox("No Records Found", "No data available for scan");
            }
        } catch (JSONException e) {
            handleException(e);
        }
        txt_crate.setText("");
        txt_scan_crate.requestFocus();
    }

    // ─── Step 3: validate scanned barcode against the loaded ET data ───────────

    private void validateArticleScan(String barcode) {
        if (eanDataMap.containsKey(barcode)) {
            String matnr = eanDataMap.get(barcode).getLgmatnr();
            PicklistData scanData = null;
            for (Map.Entry<String, PicklistData> dataEntry : etDataMap.entrySet()) {
                if (dataEntry.getValue().getArticle().equalsIgnoreCase(matnr)) {
                    scanData = dataEntry.getValue();
                    if (scanData.getSqty() == 0) {
                        break;
                    }
                }
            }
            if (scanData != null) {
                double qty = Double.parseDouble(scanData.getQuantity());
                if (scanData.getSqty() == 1) {
                    box.getBox("All Scanned", String.format("Max allowed Qty of %s already scanned. Please scan different barcode", UIFuncs.removeLeadingZeros(matnr)));
                } else {
                    txt_article.setText(matnr);
                    txt_proposed_store.setText(scanData.getStore());
                    txt_store_floor.setText(scanData.getFloor() == null ? "" : scanData.getFloor());
                    txt_scan_hu.setText("");
                    txt_hu.setText("");
                    this.currentScan = scanData;
                    calculatePendingQty(qty);
                    UIFuncs.disableInput(con, txt_scan_article);
                    UIFuncs.enableInput(con, txt_scan_hu);
                    txt_scan_hu.requestFocus();
                    return;
                }
            } else {
                box.getBox("Invalid", String.format("Article %s is invalid and not found in ET Records", matnr));
            }
        } else {
            box.getBox("Invalid", "Scanned Barcode is invalid and not available in EAN Records");
        }
        txt_scan_article.setText("");
        txt_scan_article.requestFocus();
    }

    private void calculatePendingQty(double qty) {
        if (currentScan == null) {
            txt_pending_qty.setText(Util.formatDouble(qty));
            return;
        }
        for (Map.Entry<String, PicklistData> dataEntry : etDataMap.entrySet()) {
            PicklistData data = dataEntry.getValue();
            if (data.getSqty() == 1) continue;
            if (!dataEntry.getKey().equalsIgnoreCase(currentScan.getArticle() + "-" + currentScan.getStore())
                    && dataEntry.getKey().startsWith(currentScan.getArticle() + "-")) {
                qty += Double.parseDouble(data.getQuantity());
            }
        }
        txt_pending_qty.setText(Util.formatDouble(qty));
    }

    // ─── Step 4: validate the scanned HU and post the put-away ─────────────────
    // IS_DATA = single JSON object (RFC adaptor contract), same shape as SAP ZWM_PTL_MSA_CRATE_ST export.

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /** SAP MENGE-style: always three decimals (e.g. {@code "5.000"}). */
    private static String sapMenge3(double v) {
        return String.format(Locale.US, "%.3f", v);
    }

    /** NUMC-style: pad with leading zeros to {@code len}; empty → all zeros. */
    private static String sapNumc(String value, int len) {
        String s = nz(value);
        if (s.isEmpty()) {
            char[] z = new char[len];
            java.util.Arrays.fill(z, '0');
            return new String(z);
        }
        if (s.length() > len) {
            return s.substring(s.length() - len);
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = s.length(); i < len; i++) {
            sb.append('0');
        }
        sb.append(s);
        return sb.toString();
    }

    private static double parseMenge(String q) {
        try {
            return Double.parseDouble(nz(q).replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Builds {@code IS_DATA} as one JSON object (not an array), matching the adaptor payload
     * shape used for {@link Vars#ZWM_PTL_ZONE_HU_VALIDATE_V3}.
     */
    private JSONObject buildIsDataObjectForZoneHuValidate(String huRaw) {
        if (currentScan == null) {
            box.getBox("Empty Request", "Nothing to submit, please scan some articles");
            txt_scan_article.requestFocus();
            return null;
        }
        String hu = huRaw == null ? "" : huRaw.trim().toUpperCase();
        if (hu.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "HU cannot be blank. Please scan HU again.");
            return null;
        }
        if (nz(validatedStation).isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Station is missing. Please scan FLR Station again.");
            return null;
        }

        PicklistData p = currentScan;

        String crateVal = nz(p.getCrate());
        if (crateVal.isEmpty()) {
            crateVal = nz(p.getMsaCrate());
        }
        if (crateVal.isEmpty()) {
            crateVal = UIFuncs.toUpperTrim(txt_crate);
        }
        String msaCrateVal = nz(p.getMsaCrate());
        if (msaCrateVal.isEmpty()) {
            msaCrateVal = crateVal;
        }

        String article = nz(p.getArticle());
        if (article.isEmpty()) {
            article = UIFuncs.toUpperTrim(txt_article);
        }
        String store = nz(p.getStore());
        if (store.isEmpty()) {
            store = UIFuncs.toUpperTrim(txt_proposed_store);
        }
        String floor = p.getFloor() == null ? "" : nz(p.getFloor());
        if (floor.isEmpty()) {
            floor = UIFuncs.toUpperTrim(txt_store_floor);
        }

        if (crateVal.isEmpty() || article.isEmpty() || store.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Crate, article or store is missing. Rescan crate/article.");
            return null;
        }

        // Preserve ET row values before mutating for this RFC.
        String zoneFromRow = nz(p.getZone());
        double openQty = parseMenge(p.getQuantity());
        double scanQtyFromRow = parseMenge(p.getScanQty());
        double scanQtyForRfc = scanQtyFromRow > 0 ? scanQtyFromRow : (openQty > 0 ? openQty : scanQtyFromRow);

        p.setHu(hu);
        p.setZone(validatedStation);

        JSONObject isData = new JSONObject();
        try {
            isData.put("PICKLIST", nz(p.getPicklist()));
            isData.put("BIN", nz(p.getBin()));
            isData.put("MSA_CRATE", msaCrateVal);
            isData.put("ARTICLE", article);
            isData.put("QUANTITY", sapMenge3(openQty));
            isData.put("ETYPE", nz(p.getEType()));
            isData.put("WAVE", nz(p.getWave()));
            isData.put("TANUM", sapNumc(p.getTanum(), 10));
            isData.put("PALATE", nz(p.getPalate()));
            isData.put("CRATE", crateVal);
            isData.put("STORE", store);
            isData.put("ITEMNO", sapNumc(p.getItemNo(), 6));
            isData.put("EBELN", nz(p.getEbeln()));
            isData.put("EBELP", sapNumc(p.getEbelp(), 5));
            isData.put("TAG", nz(p.getTag()));
            isData.put("HU", hu);
            isData.put("ZONE", zoneFromRow);
            isData.put("PLANT", nz(p.getPlant()));
            isData.put("SCAN_QTY", sapMenge3(scanQtyForRfc));
            isData.put("ZONE_STATION", nz(validatedStation));
            isData.put("MATKL", nz(p.getMatkl()));
            isData.put("DIVISION", nz(p.getDivision()));
            isData.put("FLOOR", floor);
        } catch (JSONException e) {
            handleException(e);
            return null;
        }
        return isData;
    }

    private void validateZoneHU(String hu) {
        JSONObject args = new JSONObject();
        try {
            lastHuSubmitted = "";
            JSONObject isData = buildIsDataObjectForZoneHuValidate(hu);
            if (isData == null) {
                endHuValidateInFlight();
                return;
            }
            args.put("bapiname", Vars.ZWM_PTL_ZONE_HU_VALIDATE_V3);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IS_DATA", isData);
            lastHuSubmitted = isData.optString("HU", "");
            Log.d(TAG, "ZWM_PTL_ZONE_HU_VALIDATE_V3 IS_DATA -> " + isData.toString());
            showProcessingAndSubmit(Vars.ZWM_PTL_ZONE_HU_VALIDATE_V3, REQUEST_VALIDATE_ZONE_HU, args);
        } catch (JSONException e) {
            endHuValidateInFlight();
            handleException(e);
        }
    }

    private void onHuPutawaySuccess(String scannedHu) {
        endHuValidateInFlight();
        if (currentScan == null) {
            // Defensive — should not happen because Scan HU is gated on currentScan.
            txt_scan_article.setText("");
            txt_scan_hu.setText("");
            UIFuncs.enableInput(con, txt_scan_article);
            UIFuncs.disableInput(con, txt_scan_hu);
            txt_scan_article.requestFocus();
            return;
        }
        txt_hu.setText(scannedHu);

        double remaining = Double.parseDouble(currentScan.getQuantity()) - 1;
        if (remaining <= 0) {
            currentScan.setSqty(1);
            remaining = 0;
        }
        currentScan.setQuantity(Util.formatDouble(remaining));
        calculatePendingQty(remaining);

        // Whole crate finished? → reset back to "Scan Crate".
        if (isCrateFullyScanned()) {
            box.getBox("Crate Done", "All articles for this crate have been put away. Please scan next Crate.",
                    (d, w) -> clearForNextCrate());
            return;
        }

        currentScan = null;
        txt_scan_article.setText("");
        txt_scan_hu.setText("");
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.enableInput(con, txt_scan_article);
        txt_scan_article.requestFocus();
    }

    private boolean isCrateFullyScanned() {
        if (etDataMap == null || etDataMap.isEmpty()) {
            return false;
        }
        for (PicklistData d : etDataMap.values()) {
            if (d.getSqty() != 1) {
                return false;
            }
        }
        return true;
    }

    // ─── Networking ────────────────────────────────────────────────────────────

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    if (dialog != null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                Log.d(TAG, "response ->" + responsebody);

                if (responsebody == null) {
                    if (request == REQUEST_VALIDATE_ZONE_HU) {
                        endHuValidateInFlight();
                    }
                    UIFuncs.errorSound(con);
                    new AlertBox(getContext()).getBox("Err", "No response from Server");
                    return;
                }
                String asString = responsebody.toString();
                if (asString.equals("") || asString.equals("null") || asString.equals("{}")) {
                    if (request == REQUEST_VALIDATE_ZONE_HU) {
                        endHuValidateInFlight();
                    }
                    UIFuncs.errorSound(con);
                    new AlertBox(getContext()).getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                }
                try {
                    if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                        JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                        if (returnobj == null) {
                            if (request == REQUEST_VALIDATE_ZONE_HU) {
                                endHuValidateInFlight();
                            }
                            return;
                        }
                        String type = returnobj.optString("TYPE", "");
                        if ("E".equals(type)) {
                            UIFuncs.errorSound(getContext());
                            new AlertBox(getContext()).getBox("Err", returnobj.optString("MESSAGE", "Server error"));
                            handleErrorReset(request);
                            return;
                        }
                        // Success path
                        if (request == REQUEST_VALIDATE_STATION) {
                            String scanned = UIFuncs.toUpperTrim(txt_scan_station);
                            onStationValidated(scanned, responsebody);
                        } else if (request == REQUEST_VALIDATE_STATION_CRATE) {
                            setEtEanData(responsebody);
                        } else if (request == REQUEST_VALIDATE_ZONE_HU) {
                            onHuPutawaySuccess(lastHuSubmitted);
                        }
                    } else if (request == REQUEST_VALIDATE_ZONE_HU) {
                        endHuValidateInFlight();
                        UIFuncs.errorSound(con);
                        new AlertBox(getContext()).getBox("Err", "Invalid server response (missing EX_RETURN).");
                    }
                } catch (JSONException e) {
                    if (request == REQUEST_VALIDATE_ZONE_HU) {
                        endHuValidateInFlight();
                    }
                    e.printStackTrace();
                    new AlertBox(getContext()).getErrBox(e);
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
                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res.toString());
                return res;
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
            public void retry(VolleyError error) throws VolleyError { }
        });
        mRequestQueue.add(mJsonRequest);
        Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
        Log.d(TAG, "jsonRequest getBodyContentType->" + mJsonRequest.getBodyContentType());
        Log.d(TAG, "jsonRequest getBody->" + new String(mJsonRequest.getBody()));
        Log.d(TAG, "jsonRequest getMethod->" + mJsonRequest.getMethod());
        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            new AlertBox(getContext()).getErrBox(authFailureError);
        }
    }

    private void handleErrorReset(int request) {
        if (request == REQUEST_VALIDATE_STATION) {
            txt_scan_station.setText("");
            txt_scan_station.requestFocus();
        } else if (request == REQUEST_VALIDATE_STATION_CRATE) {
            txt_scan_crate.setText("");
            txt_scan_crate.requestFocus();
        } else if (request == REQUEST_VALIDATE_ZONE_HU) {
            endHuValidateInFlight();
            txt_scan_hu.requestFocus();
        }
    }

    private void handleException(Exception e) {
        e.printStackTrace();
        UIFuncs.errorSound(con);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        new AlertBox(getContext()).getErrBox(e);
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

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                if (huValidateInFlight) {
                    endHuValidateInFlight();
                }
                new AlertBox(getContext()).getBox("Err", err);
            }
        };
    }
}
