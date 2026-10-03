package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import com.android.volley.toolbox.HttpHeaderParser;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Store GRT Pick screen.
 * Picklist dropdown: ZWM_ST_GRT_PICKLIST_RFC (PICKLIST_NO).
 * Selected picklist: ZWM_ST_GRT_GET_PICKLIST_DATA.
 * Matched article scan: ZWM_RFC_GRT_STORE_SCAN (IT_SCAN).
 */
public class FragmentStoreGrtPick extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentStoreGrtPick.class.getName();
    private static final String VOLLEY_TAG = "FragmentStoreGrtPick";
    private static final String SOURCE_0001 = "0001";
    private static final String SOURCE_0006 = "0006";
    private static final String PICKLIST_HINT = "Select Picklist No.";

    private View rootView;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private FragmentManager fm;
    private boolean viewDestroyed;

    private LinearLayout source0001;
    private LinearLayout source0006;
    private Spinner spinnerPicklistNo;
    private ArrayAdapter<String> picklistSpinnerAdapter;
    private EditText txtPlant;
    private EditText txtArticle;
    private EditText txtScanQty;
    private EditText txtScannedLocal;
    private Button btnBack;
    private Button btnReset;
    private Button btnSave;

    private String selectedSource = SOURCE_0001;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";
    private boolean suppressPicklistSelection;
    private volatile int picklistDataLoadSeq;
    private Request<?> inFlightPicklistDataRequest;
    private ExecutorService picklistParseExecutor;
    private Map<String, FragmentStoreGrtProcess.PicklistArticleLine> picklistArticlesByMatnr = new HashMap<>();
    private Map<String, FragmentStoreGrtProcess.EanRecord> eanByScanCode = new HashMap<>();
    private final Map<String, FragmentStoreGrtProcess.PicklistDataParseResult> picklistDataCache = new HashMap<>();
    private final Set<String> scannedMatnrs = new LinkedHashSet<>();
    private double scannedPcs;
    private boolean ignoreArticleWatcher;
    private boolean scanRequestInFlight;

    public FragmentStoreGrtPick() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Store GRT Pick");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_store_grt_pick, container, false);
        con = getContext();
        box = new AlertBox(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        source0001 = rootView.findViewById(R.id.store_grt_pick_source_0001);
        source0006 = rootView.findViewById(R.id.store_grt_pick_source_0006);
        spinnerPicklistNo = rootView.findViewById(R.id.store_grt_pick_picklist_no);
        txtPlant = rootView.findViewById(R.id.store_grt_pick_plant);
        txtArticle = rootView.findViewById(R.id.store_grt_pick_article);
        txtScanQty = rootView.findViewById(R.id.store_grt_pick_scan_qty);
        txtScannedLocal = rootView.findViewById(R.id.store_grt_pick_scanned_local);
        btnBack = rootView.findViewById(R.id.store_grt_pick_btn_back);
        btnReset = rootView.findViewById(R.id.store_grt_pick_btn_reset);
        btnSave = rootView.findViewById(R.id.store_grt_pick_btn_save);

        source0001.setOnClickListener(this);
        source0006.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        setupPicklistSpinner();
        addPicklistSelectionListener();
        addArticleScanEvents();
        selectSource(SOURCE_0001);
        clearInputs();
        viewDestroyed = false;
        picklistParseExecutor = Executors.newSingleThreadExecutor();
        loadPicklistNumbers();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        viewDestroyed = true;
        picklistDataLoadSeq++;
        if (spinnerPicklistNo != null) {
            spinnerPicklistNo.setOnItemSelectedListener(null);
        }
        if (inFlightPicklistDataRequest != null) {
            inFlightPicklistDataRequest.cancel();
            inFlightPicklistDataRequest = null;
        }
        ApplicationController.getInstance().cancelPendingRequests(VOLLEY_TAG);
        dismissDialog();
        if (picklistParseExecutor != null) {
            picklistParseExecutor.shutdownNow();
            picklistParseExecutor = null;
        }
        picklistDataCache.clear();
        picklistArticlesByMatnr.clear();
        eanByScanCode.clear();
        super.onDestroyView();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.store_grt_pick_source_0001) {
            selectSource(SOURCE_0001);
        } else if (id == R.id.store_grt_pick_source_0006) {
            selectSource(SOURCE_0006);
        } else if (id == R.id.store_grt_pick_btn_back) {
            box.confirmBack(fm, con);
        } else if (id == R.id.store_grt_pick_btn_reset) {
            box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> clearInputs(),
                    (dialogInterface, i) -> {
                    });
        }
    }

    private void setupPicklistSpinner() {
        List<String> items = new ArrayList<>();
        items.add(PICKLIST_HINT);
        picklistSpinnerAdapter = new ArrayAdapter<>(con,
                android.R.layout.simple_spinner_item, items);
        picklistSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPicklistNo.setAdapter(picklistSpinnerAdapter);
    }

    private void addPicklistSelectionListener() {
        spinnerPicklistNo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressPicklistSelection) {
                    return;
                }
                String picklist = getSelectedPicklist();
                if (picklist.isEmpty()) {
                    clearLoadedPicklistData();
                    txtPlant.setText("");
                    return;
                }
                scannedMatnrs.clear();
                scannedPcs = 0;
                txtScanQty.setText("0");
                refreshScannedLocal();
                loadPicklistData(picklist);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                txtPlant.setText("");
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

    private void setPicklistSelection(int index) {
        suppressPicklistSelection = true;
        try {
            spinnerPicklistNo.setSelection(index);
        } finally {
            suppressPicklistSelection = false;
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

    private void clearInputs() {
        txtPlant.setText("");
        txtArticle.setText("");
        txtScanQty.setText("0");
        txtScannedLocal.setText("0 articles / 0 pcs");
        clearLoadedPicklistData();
        if (spinnerPicklistNo.getAdapter() != null && spinnerPicklistNo.getCount() > 0) {
            setPicklistSelection(0);
        }
        selectSource(SOURCE_0001);
    }

    private void clearLoadedPicklistData() {
        picklistArticlesByMatnr = new HashMap<>();
        eanByScanCode = new HashMap<>();
        scannedMatnrs.clear();
        scannedPcs = 0;
        refreshScannedLocal();
    }

    private void addArticleScanEvents() {
        txtArticle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txtArticle);
                    if (!value.isEmpty()) {
                        onArticleScanned(value);
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
                if (ignoreArticleWatcher) {
                    return;
                }
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    onArticleScanned(value);
                }
            }
        });
    }

    private void onArticleScanned(String scan) {
        if (scanRequestInFlight || scan == null || scan.trim().isEmpty()) {
            return;
        }
        String picklist = getSelectedPicklist();
        if (picklist.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Select Picklist", "Please select a Picklist No. before scanning an article.");
            resetArticleInput();
            return;
        }
        if (picklistArticlesByMatnr.isEmpty() && eanByScanCode.isEmpty()) {
            UIFuncs.errorSound(con);
            if (inFlightPicklistDataRequest != null) {
                box.getBox("Please wait", "Picklist data is still loading. Scan article after loading completes.");
            } else {
                box.getBox("Err", "Picklist data not loaded. Reselect picklist and try again.");
            }
            resetArticleInput();
            return;
        }

        FragmentStoreGrtProcess.EanRecord eanRec = resolveEanRecord(scan);
        if (eanRec == null || eanRec.matnr == null || eanRec.matnr.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Article not found in picklist data.");
            resetArticleInput();
            return;
        }
        FragmentStoreGrtProcess.PicklistArticleLine articleLine = findArticleLine(eanRec.matnr);
        if (articleLine == null) {
            UIFuncs.errorSound(con);
            box.getBox("Err", "Article not found in picklist data.");
            resetArticleInput();
            return;
        }

        double qty = Util.convertStringToDouble(txtScanQty.getText().toString());
        if (qty <= 0) {
            qty = eanRec.umrez > 0 ? eanRec.umrez : 1;
        }
        submitStoreScan(picklist, articleLine, eanRec, qty);
    }

    private void submitStoreScan(String picklist,
                                 FragmentStoreGrtProcess.PicklistArticleLine articleLine,
                                 FragmentStoreGrtProcess.EanRecord eanRec,
                                 double qty) {
        JSONObject row = new JSONObject();
        JSONArray itScan = new JSONArray();
        JSONObject args = new JSONObject();
        try {
            row.put("PICKLIST_NO", picklist);
            row.put("SOURCE_SITE", WERKS);
            row.put("LGORT", selectedSource);
            row.put("MATNR", articleLine.matnr);
            row.put("EAN11", eanRec.ean11 != null ? eanRec.ean11 : "");
            row.put("PICK_QTY", qty);
            itScan.put(row);
            args.put("bapiname", Vars.ZWM_RFC_GRT_STORE_SCAN);
            args.put("IT_SCAN", itScan);
        } catch (JSONException e) {
            Log.e(TAG, "ZWM_RFC_GRT_STORE_SCAN args error", e);
            UIFuncs.errorSound(con);
            box.getErrBox(e);
            return;
        }
        Log.d(TAG, "ZWM_RFC_GRT_STORE_SCAN payload=" + args);
        scanRequestInFlight = true;
        showLoading();
        String url = GatewayUrls.noAclJsonRfcUrl(URL, Vars.ZWM_RFC_GRT_STORE_SCAN);
        final double postedQty = qty;
        final String matnr = articleLine.matnr;
        SapJsonObjectRequest request = new SapJsonObjectRequest(Request.Method.POST, url, args,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        scanRequestInFlight = false;
                        dismissDialog();
                        if (viewDestroyed || !isAdded()) {
                            return;
                        }
                        if (response == null) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from Server");
                            resetArticleInput();
                            return;
                        }
                        try {
                            JSONObject returnObj = getScanReturn(response);
                            String type = returnObj != null ? returnObj.optString("TYPE", "").trim() : "";
                            if ("E".equals(type) || "A".equals(type)) {
                                UIFuncs.errorSound(con);
                                box.getBox("Err", returnObj.optString("MESSAGE", "Scan rejected."));
                                resetArticleInput();
                                return;
                            }
                        } catch (JSONException e) {
                            box.getErrBox(e);
                            resetArticleInput();
                            return;
                        }
                        scannedMatnrs.add(matnr.trim().toUpperCase());
                        scannedPcs += postedQty;
                        txtScanQty.setText(Util.formatDouble(postedQty));
                        refreshScannedLocal();
                        resetArticleInput();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                scanRequestInFlight = false;
                if (viewDestroyed || !isAdded()) {
                    dismissDialog();
                    return;
                }
                Log.e(TAG, "ZWM_RFC_GRT_STORE_SCAN error -> " + error, error);
                dismissDialog();
                box.getBox("Err", volleyErrorMessage(error));
                resetArticleInput();
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return args.toString().getBytes();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(50000, 0, 1.0f));
        request.setShouldCache(false);
        request.setTag(VOLLEY_TAG);
        ApplicationController.getInstance().getRequestQueue().add(request);
    }

    private FragmentStoreGrtProcess.EanRecord resolveEanRecord(String scan) {
        String upper = scan.trim().toUpperCase();
        if (upper.isEmpty()) {
            return null;
        }
        FragmentStoreGrtProcess.EanRecord hit = eanByScanCode.get(upper);
        if (hit != null) {
            return hit;
        }
        String noZeros = UIFuncs.removeLeadingZeros(upper);
        if (!noZeros.isEmpty()) {
            hit = eanByScanCode.get(noZeros.toUpperCase());
            if (hit != null) {
                return hit;
            }
        }
        FragmentStoreGrtProcess.PicklistArticleLine line = findArticleLine(upper);
        if (line == null) {
            return null;
        }
        FragmentStoreGrtProcess.EanRecord fallback = new FragmentStoreGrtProcess.EanRecord();
        fallback.matnr = line.matnr;
        fallback.ean11 = "";
        fallback.umrez = 1;
        return fallback;
    }

    private FragmentStoreGrtProcess.PicklistArticleLine findArticleLine(String matnrOrScan) {
        if (matnrOrScan == null || matnrOrScan.isEmpty()) {
            return null;
        }
        String upper = matnrOrScan.trim().toUpperCase();
        FragmentStoreGrtProcess.PicklistArticleLine direct = picklistArticlesByMatnr.get(upper);
        if (direct != null) {
            return direct;
        }
        String noZeros = UIFuncs.removeLeadingZeros(upper);
        if (!noZeros.isEmpty()) {
            return picklistArticlesByMatnr.get(noZeros.toUpperCase());
        }
        return null;
    }

    private void resetArticleInput() {
        ignoreArticleWatcher = true;
        txtArticle.setText("");
        ignoreArticleWatcher = false;
        txtArticle.requestFocus();
    }

    private void refreshScannedLocal() {
        if (txtScannedLocal == null) {
            return;
        }
        txtScannedLocal.setText(scannedMatnrs.size() + " articles / " + Util.formatDouble(scannedPcs) + " pcs");
    }

    private static JSONObject getScanReturn(JSONObject response) throws JSONException {
        JSONObject et = parseReturnNode(response, "ET_RETURN");
        if (et != null) {
            return et;
        }
        return getReturnObject(response);
    }

    private void loadPicklistNumbers() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_PICKLIST_RFC);
            args.put("SOURCE_SITE", WERKS);
            showLoading();
            submitPicklistRequest(args);
        } catch (JSONException e) {
            Log.e(TAG, "ZWM_ST_GRT_PICKLIST_RFC args error", e);
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void submitPicklistRequest(JSONObject args) {
        String url = GatewayUrls.noAclJsonRfcUrl(URL, Vars.ZWM_ST_GRT_PICKLIST_RFC);
        Log.d(TAG, "RFC request -> " + Vars.ZWM_ST_GRT_PICKLIST_RFC + " payload=" + args);
        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        SapJsonObjectRequest request = new SapJsonObjectRequest(Request.Method.POST, url, args,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (viewDestroyed || !isAdded()) {
                            dismissDialog();
                            return;
                        }
                        if (response == null) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from Server");
                            dismissDialog();
                            return;
                        }
                        bindPicklistNumbers(response);
                        dismissDialog();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (viewDestroyed || !isAdded()) {
                    dismissDialog();
                    return;
                }
                Log.e(TAG, "RFC error -> " + error, error);
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
                box.getBox("Err", err);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return args.toString().getBytes();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(50000, 0, 1.0f));
        request.setShouldCache(false);
        request.setTag(VOLLEY_TAG);
        queue.add(request);
    }

    private void bindPicklistNumbers(JSONObject response) {
        try {
            JSONObject returnObj = getReturnObject(response);
            if (returnObj != null && "E".equals(returnObj.optString("TYPE", "").trim())) {
                UIFuncs.errorSound(con);
                box.getBox("Err", returnObj.optString("MESSAGE", "Unable to load picklist."));
                return;
            }

            JSONArray arr = response.optJSONArray("ET_PICKLIST_NO");
            Set<String> picklistNos = new LinkedHashSet<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row == null || isHeaderRow(row)) {
                        continue;
                    }
                    String picklistNo = row.optString("PICKLIST_NO", "").trim();
                    if (!picklistNo.isEmpty()) {
                        picklistNos.add(picklistNo);
                    }
                }
            }

            List<String> items = new ArrayList<>();
            items.add(PICKLIST_HINT);
            items.addAll(picklistNos);
            picklistSpinnerAdapter.clear();
            picklistSpinnerAdapter.addAll(items);
            picklistSpinnerAdapter.notifyDataSetChanged();
            setPicklistSelection(0);
            Log.d(TAG, "ZWM_ST_GRT_PICKLIST_RFC done count=" + picklistNos.size());

            if (picklistNos.isEmpty()) {
                box.getBox("No Data", "No picklist found for this site.");
            }
        } catch (Exception e) {
            Log.e(TAG, "ZWM_ST_GRT_PICKLIST_RFC parse error", e);
            box.getErrBox(e);
        }
    }

    private static boolean isHeaderRow(JSONObject row) {
        return "PICKLIST_NO".equalsIgnoreCase(row.optString("PICKLIST_NO", "").trim());
    }

    private static JSONObject getReturnObject(JSONObject response) throws JSONException {
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
            JSONObject first = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                if (first == null) {
                    first = row;
                }
                String type = row.optString("TYPE", "").trim();
                if ("E".equals(type) || "A".equals(type)) {
                    return row;
                }
            }
            return first;
        }
        return null;
    }

    private void loadPicklistData(String picklistNo) {
        if (picklistNo == null || picklistNo.isEmpty()) {
            return;
        }
        FragmentStoreGrtProcess.PicklistDataParseResult cached = picklistDataCache.get(picklistNo);
        if (cached != null) {
            Log.i(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA memory cache hit picklist=" + picklistNo
                    + " articles=" + cached.articlesByMatnr.size()
                    + " eans=" + cached.eanByScanCode.size()
                    + " rdc=" + cached.rdcPlant);
            applyPicklistDataResult(picklistNo, cached);
            return;
        }
        if (viewDestroyed || !isAdded()) {
            return;
        }
        final int loadSeq = ++picklistDataLoadSeq;
        final String plant = WERKS;
        final Context appCtx = con != null ? con.getApplicationContext() : null;
        showLoading();
        if (picklistParseExecutor == null || picklistParseExecutor.isShutdown() || appCtx == null) {
            startPicklistDataRfc(picklistNo, loadSeq);
            return;
        }
        Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA check local cache picklist=" + picklistNo
                + " plant=" + plant + " seq=" + loadSeq);
        picklistParseExecutor.execute(() -> {
            FragmentStoreGrtProcess.PicklistDataParseResult disk =
                    StoreGrtPicklistLocalCache.load(appCtx, plant, picklistNo);
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                if (viewDestroyed || !isAdded() || loadSeq != picklistDataLoadSeq) {
                    Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA skip stale cache check seq=" + loadSeq);
                    return;
                }
                if (disk != null && !disk.articlesByMatnr.isEmpty()) {
                    picklistDataCache.put(picklistNo, disk);
                    Log.i(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA local cache hit picklist=" + picklistNo
                            + " articles=" + disk.articlesByMatnr.size()
                            + " eans=" + disk.eanByScanCode.size()
                            + " rdc=" + disk.rdcPlant);
                    applyPicklistDataResult(picklistNo, disk);
                    dismissDialog();
                    return;
                }
                Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA local cache miss, calling RFC picklist="
                        + picklistNo);
                startPicklistDataRfc(picklistNo, loadSeq);
            });
        });
    }

    private void startPicklistDataRfc(String picklistNo, int loadSeq) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_ST_GRT_GET_PICKLIST_DATA);
            args.put("IM_PLANT", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_PICKLIST", picklistNo);
            Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA load start picklist=" + picklistNo
                    + " plant=" + WERKS + " user=" + USER + " seq=" + loadSeq);
            submitPicklistDataRequest(args, loadSeq);
        } catch (JSONException e) {
            Log.e(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA args error", e);
            UIFuncs.errorSound(con);
            dismissDialog();
            box.getErrBox(e);
        }
    }

    private void submitPicklistDataRequest(JSONObject args, final int loadSeq) {
        if (inFlightPicklistDataRequest != null) {
            inFlightPicklistDataRequest.cancel();
            inFlightPicklistDataRequest = null;
        }
        String url = GatewayUrls.noAclJsonRfcUrl(URL, Vars.ZWM_ST_GRT_GET_PICKLIST_DATA);
        final byte[] bodyBytes = args.toString().getBytes(StandardCharsets.UTF_8);
        final long startedAt = SystemClock.elapsedRealtime();
        Log.d(TAG, "RFC request -> " + Vars.ZWM_ST_GRT_GET_PICKLIST_DATA
                + " url=" + url + " payload=" + args + " seq=" + loadSeq);
        Request<byte[]> request = new Request<byte[]>(Request.Method.POST, url, picklistDataErrorListener(loadSeq)) {
            int httpStatus;

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return bodyBytes;
            }

            @Override
            protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
                httpStatus = response == null ? 0 : response.statusCode;
                byte[] data = response == null || response.data == null ? new byte[0] : response.data;
                Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA network http=" + httpStatus
                        + " bytes=" + data.length
                        + " networkMs=" + (SystemClock.elapsedRealtime() - startedAt)
                        + " seq=" + loadSeq);
                return Response.success(data, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected void deliverResponse(byte[] response) {
                if (inFlightPicklistDataRequest == this) {
                    inFlightPicklistDataRequest = null;
                }
                long networkMs = SystemClock.elapsedRealtime() - startedAt;
                Log.i(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA response http=" + httpStatus
                        + " bytes=" + (response == null ? 0 : response.length)
                        + " networkMs=" + networkMs
                        + " seq=" + loadSeq
                        + " body=" + responsePreview(response));
                bindPicklistDataBytes(response, loadSeq, httpStatus, networkMs);
            }
        };
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1.0f));
        request.setTag(VOLLEY_TAG);
        inFlightPicklistDataRequest = request;
        ApplicationController.getInstance().getRequestQueue().add(request);
    }

    private void bindPicklistDataBytes(final byte[] body, final int loadSeq, final int httpStatus,
                                       final long networkMs) {
        if (viewDestroyed || picklistParseExecutor == null || picklistParseExecutor.isShutdown()) {
            Log.w(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA skip parse viewDestroyed/executor down seq=" + loadSeq);
            dismissDialog();
            return;
        }
        if (loadSeq != picklistDataLoadSeq) {
            Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA skip stale response seq=" + loadSeq
                    + " current=" + picklistDataLoadSeq);
            return;
        }
        if (body == null || body.length == 0) {
            Log.e(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA empty body http=" + httpStatus
                    + " networkMs=" + networkMs + " seq=" + loadSeq);
            UIFuncs.errorSound(con);
            box.getBox("Err", "No response from Server");
            dismissDialog();
            return;
        }
        Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA parse start bytes=" + body.length
                + " http=" + httpStatus + " networkMs=" + networkMs + " seq=" + loadSeq);
        final String werks = WERKS;
        picklistParseExecutor.execute(() -> {
            FragmentStoreGrtProcess.PicklistDataParseResult parsed = null;
            Exception parseError = null;
            try {
                if (loadSeq == picklistDataLoadSeq) {
                    parsed = FragmentStoreGrtProcess.parseGetPicklistData(body, werks);
                    parsed.httpStatus = httpStatus;
                    parsed.responseBytes = body.length;
                    parsed.networkMs = networkMs;
                }
            } catch (Exception ex) {
                parseError = ex;
                Log.e(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA parse error seq=" + loadSeq, ex);
            }
            final FragmentStoreGrtProcess.PicklistDataParseResult result = parsed;
            final Exception error = parseError;
            FragmentActivity activity = getActivity();
            if (activity == null) {
                Log.w(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA parse done but activity null seq=" + loadSeq);
                return;
            }
            activity.runOnUiThread(() -> {
                if (viewDestroyed || !isAdded() || loadSeq != picklistDataLoadSeq) {
                    Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA drop stale parse seq=" + loadSeq
                            + " current=" + picklistDataLoadSeq);
                    return;
                }
                if (error != null) {
                    dismissDialog();
                    box.getErrBox(error);
                    return;
                }
                String picklist = getSelectedPicklist();
                if (result != null) {
                    Log.i(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA done picklist=" + picklist
                            + " http=" + result.httpStatus
                            + " bytes=" + result.responseBytes
                            + " networkMs=" + result.networkMs
                            + " parseMs=" + result.parseMs
                            + " articles=" + result.articlesByMatnr.size()
                            + " eans=" + result.eanByScanCode.size()
                            + " rdc=" + result.rdcPlant
                            + " type=" + result.errorType
                            + " msg=" + result.errorMessage);
                }
                if (!picklist.isEmpty() && result != null && !result.articlesByMatnr.isEmpty()
                        && !isPicklistDataError(result)) {
                    picklistDataCache.put(picklist, result);
                    persistPicklistDataCache(picklist, result);
                }
                applyPicklistDataResult(picklist, result);
                dismissDialog();
            });
        });
    }

    private void applyPicklistDataResult(String picklistNo,
                                         FragmentStoreGrtProcess.PicklistDataParseResult parsed) {
        if (parsed == null) {
            Log.w(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA apply skipped parsed=null picklist=" + picklistNo);
            return;
        }
        if (isPicklistDataError(parsed)) {
            Log.e(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA SAP error TYPE=" + parsed.errorType
                    + " MESSAGE=" + parsed.errorMessage + " picklist=" + picklistNo);
            UIFuncs.errorSound(con);
            clearLoadedPicklistData();
            txtPlant.setText("");
            box.getBox("Err", parsed.errorMessage == null || parsed.errorMessage.isEmpty()
                    ? "No picklist article data returned."
                    : parsed.errorMessage);
            return;
        }
        picklistArticlesByMatnr = parsed.articlesByMatnr;
        eanByScanCode = parsed.eanByScanCode;
        txtPlant.setText(parsed.rdcPlant != null ? parsed.rdcPlant : "");
        Log.d(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA done picklist=" + picklistNo
                + " articles=" + picklistArticlesByMatnr.size()
                + " eans=" + eanByScanCode.size()
                + " rdc=" + parsed.rdcPlant);
        if (picklistArticlesByMatnr.isEmpty()) {
            Log.w(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA no article rows picklist=" + picklistNo);
            UIFuncs.errorSound(con);
            box.getBox("No Data", "No picklist article data returned.");
            return;
        }
        txtArticle.requestFocus();
    }

    private void persistPicklistDataCache(String picklistNo,
                                          FragmentStoreGrtProcess.PicklistDataParseResult parsed) {
        if (parsed == null || picklistNo == null || picklistNo.isEmpty()) {
            return;
        }
        final Context appCtx = con != null ? con.getApplicationContext() : null;
        final String plant = WERKS;
        if (appCtx == null || picklistParseExecutor == null || picklistParseExecutor.isShutdown()) {
            return;
        }
        picklistParseExecutor.execute(() ->
                StoreGrtPicklistLocalCache.save(appCtx, plant, picklistNo, parsed));
    }

    private static String responsePreview(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        int len = Math.min(body.length, 1500);
        String text = new String(body, 0, len, StandardCharsets.UTF_8);
        if (body.length > len) {
            return text + "...(" + body.length + " bytes)";
        }
        return text;
    }

    private static boolean isPicklistDataError(FragmentStoreGrtProcess.PicklistDataParseResult parsed) {
        return parsed != null && ("E".equals(parsed.errorType) || "A".equals(parsed.errorType));
    }

    private Response.ErrorListener picklistDataErrorListener(final int loadSeq) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                inFlightPicklistDataRequest = null;
                if (viewDestroyed || !isAdded() || loadSeq != picklistDataLoadSeq) {
                    return;
                }
                int status = error != null && error.networkResponse != null
                        ? error.networkResponse.statusCode : 0;
                int bytes = error != null && error.networkResponse != null
                        && error.networkResponse.data != null
                        ? error.networkResponse.data.length : 0;
                Log.e(TAG, "ZWM_ST_GRT_GET_PICKLIST_DATA error http=" + status
                        + " bytes=" + bytes + " seq=" + loadSeq + " err=" + error, error);
                dismissDialog();
                box.getBox("Err", volleyErrorMessage(error));
            }
        };
    }

    private static String volleyErrorMessage(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return "Communication Error!";
        }
        if (error instanceof AuthFailureError) {
            return "Authentication Error!";
        }
        if (error instanceof ServerError) {
            return "Server Side Error!";
        }
        if (error instanceof NetworkError) {
            return "Network Error!";
        }
        if (error instanceof ParseError) {
            return "Parse Error!";
        }
        return error == null ? "Communication Error!" : error.toString();
    }

    private void showLoading() {
        if (viewDestroyed || !isAdded()) {
            return;
        }
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(con);
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
