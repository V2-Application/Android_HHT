package com.v2retail.dotvik.store.directpicking;

import android.app.ProgressDialog;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FragmentV01HUSorting extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentV01HUSorting.class.getName();
    private static final int REQUEST_GET_HU = 1601;
    private static final int REQUEST_VALIDATE_BIN = 1602;
    private static final int REQUEST_SAVE = 1603;

    FragmentManager fm;
    Button btn_back, btn_save;
    EditText txt_picklist_no, txt_hu_no, txt_article, txt_msa_bin, txt_msa_crate, txt_crate_0001;
    CheckBox chk_crate_0003;
    int currentRequest = 0;
    JSONArray huSaveData = new JSONArray();
    String validatedMsaBin = "";
    String handledArticle = "";
    long handledArticleAt = 0;

    String URL = "";
    String WERKS = "";
    String USER = "";
    ProgressDialog dialog;
    boolean requestInFlight = false;

    public FragmentV01HUSorting() {
    }

    public static FragmentV01HUSorting newInstance() {
        return newInstance("");
    }

    public static FragmentV01HUSorting newInstance(String picklistNo) {
        FragmentV01HUSorting fragment = new FragmentV01HUSorting();
        Bundle args = new Bundle();
        args.putString("picklist_no", picklistNo == null ? "" : picklistNo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("V01 HU sorting");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_v01_hu_sorting, container, false);

        txt_picklist_no = rootView.findViewById(R.id.txt_v01_hu_sorting_picklist_no);
        txt_hu_no = rootView.findViewById(R.id.txt_v01_hu_sorting_hu_no);
        txt_article = rootView.findViewById(R.id.txt_v01_hu_sorting_article_no);
        txt_msa_bin = rootView.findViewById(R.id.txt_v01_hu_sorting_msa_bin);
        txt_msa_crate = rootView.findViewById(R.id.txt_v01_hu_sorting_msa_crate);
        txt_crate_0001 = rootView.findViewById(R.id.txt_v01_hu_sorting_crate_0001);
        chk_crate_0003 = rootView.findViewById(R.id.chk_v01_hu_sorting_crate_0003);
        btn_back = rootView.findViewById(R.id.btn_v01_hu_sorting_back);
        btn_save = rootView.findViewById(R.id.btn_v01_hu_sorting_save);

        SharedPreferencesData data = new SharedPreferencesData(getContext());
        URL = data.read("URL");
        USER = data.read("USER");
        WERKS = data.read("WERKS");

        Bundle args = getArguments();
        if (args != null) {
            txt_picklist_no.setText(args.getString("picklist_no", ""));
        }

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        addHuScanEvents();
        addArticleScanEvents();
        addMsaBinScanEvents();
        chk_crate_0003.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    applyRemainingTo0003();
                } else {
                    restore0003();
                }
            }
        });

        txt_hu_no.post(new Runnable() {
            @Override
            public void run() {
                txt_hu_no.requestFocus();
            }
        });

        return rootView;
    }

    private void addHuScanEvents() {
        txt_hu_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_hu_no);
                    if (!value.isEmpty()) {
                        getHu(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_hu_no.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    getHu(value);
                }
            }
        });
    }

    private void addArticleScanEvents() {
        txt_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_article);
                    if (!value.isEmpty()) {
                        onArticleScanned(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_article.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = start == 0 && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    onArticleScanned(value);
                }
            }
        });
    }

    private void addMsaBinScanEvents() {
        txt_msa_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_msa_bin);
                    if (!value.isEmpty()) {
                        validateMsaBin(value);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_msa_bin.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    validateMsaBin(value);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_v01_hu_sorting_back) {
            fm.popBackStack();
        } else if (view.getId() == R.id.btn_v01_hu_sorting_save) {
            save();
        }
    }

    private void save() {
        if (requestInFlight) {
            return;
        }
        String picklist = textOf(txt_picklist_no);
        String hu = textOf(txt_hu_no);
        String bin = textOf(txt_msa_bin);
        if (bin.isEmpty()) {
            bin = validatedMsaBin == null ? "" : validatedMsaBin;
        }
        if (picklist.isEmpty() || hu.isEmpty()) {
            UIFuncs.errorSound(getContext());
            new AlertBox(getContext()).getBox("Err", "Picklist and HU are required");
            return;
        }
        if (huSaveData == null || huSaveData.length() == 0) {
            UIFuncs.errorSound(getContext());
            new AlertBox(getContext()).getBox("Err", "Scan HU before save");
            focus(txt_hu_no);
            return;
        }
        if (hasMsaAllocation() && bin.isEmpty()) {
            UIFuncs.errorSound(getContext());
            new AlertBox(getContext()).getBox("Err", "MSA BIN is required");
            focus(txt_msa_bin);
            return;
        }
        requestInFlight = true;
        JSONObject args = new JSONObject();
        try {
            if (chk_crate_0003.isChecked()) {
                applyRemainingTo0003();
            }
            args.put("bapiname", Vars.ZWM_V01_SAVE_RFC);
            args.put("IM_USER", USER == null ? "" : USER);
            args.put("IM_PLANT", WERKS == null ? "" : WERKS);
            args.put("IM_MSA_BIN", bin);
            args.put("IM_HU", hu);
            args.put("IM_PICKLIST", picklist);
            args.put("ET_DATA", buildEtData());
            showProcessingAndSubmit(Vars.ZWM_V01_SAVE_RFC, REQUEST_SAVE, args);
        } catch (JSONException e) {
            requestInFlight = false;
            e.printStackTrace();
            dismissDialog();
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void clearAfterSave() {
        huSaveData = new JSONArray();
        validatedMsaBin = "";
        chk_crate_0003.setChecked(false);
        txt_hu_no.setText("");
        txt_article.setText("");
        txt_msa_bin.setText("");
        setCrateCounts(0, 0);
        focus(txt_hu_no);
    }

    private void onArticleScanned(String article) {
        long now = System.currentTimeMillis();
        if (article.equals(handledArticle) && now - handledArticleAt < 600) {
            return;
        }
        handledArticle = article;
        handledArticleAt = now;
        if (huSaveData == null || huSaveData.length() == 0) {
            showFieldError(txt_article, "Scan HU first");
            focus(txt_hu_no);
            return;
        }
        JSONObject line0001 = findArticleForCrate(article, true);
        if (line0001 != null) {
            confirmTransferTo0001(line0001);
            return;
        }
        JSONObject lineMsa = findArticleForCrate(article, false);
        if (lineMsa != null) {
            if (!allocateOne(lineMsa, false)) {
                return;
            }
            focusMsaBin();
            return;
        }
        if (findArticle(article) != null) {
            showFieldError(txt_article, "Article already transferred");
        } else {
            showFieldError(txt_article, "Article not found in HU");
        }
    }

    private void confirmTransferTo0001(final JSONObject line) {
        if (!allocateOne(line, true)) {
            return;
        }
        String article = line.optString("ARTICLE", "");
        new AlertBox(getContext()).getBox(
                "Confirm",
                "Article " + article + " transferred to 0001",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        txt_article.setText("");
                        focus(txt_article);
                    }
                });
    }

    private boolean hasMsaAllocation() {
        for (int i = 0; i < huSaveData.length(); i++) {
            JSONObject line = huSaveData.optJSONObject(i);
            if (line != null && parseQty(line.optString("SCAN_MSA_QTY", "0")) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean allocateOne(JSONObject line, boolean to0001) {
        try {
            String totalKey = to0001 ? "TO_0001" : "TO_MSA";
            String scanKey = to0001 ? "SCAN_001_QTY" : "SCAN_MSA_QTY";
            double cap = parseQty(line.optString(totalKey, "0"));
            double scanned = parseQty(line.optString(scanKey, "0"));
            if (scanned >= cap) {
                return false;
            }
            line.put(scanKey, formatQty(scanned + 1));
            syncLineQuantities(line, chk_crate_0003.isChecked());
            refreshCrateCounts();
            return true;
        } catch (JSONException e) {
            new AlertBox(getContext()).getErrBox(e);
            focus(txt_article);
            return false;
        }
    }

    private void focusMsaBin() {
        txt_msa_bin.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    return;
                }
                txt_article.clearFocus();
                txt_msa_bin.requestFocusFromTouch();
                if (txt_msa_bin.getText() != null) {
                    txt_msa_bin.setSelection(txt_msa_bin.getText().length());
                }
            }
        }, 100);
    }

    private JSONObject findArticleForCrate(String scanned, boolean crate0001) {
        String key = normalize(scanned);
        String remainKey = crate0001 ? "REMAIN_0001" : "REMAIN_MSA";
        for (int i = 0; i < huSaveData.length(); i++) {
            JSONObject line = huSaveData.optJSONObject(i);
            if (line == null || !matchesArticle(line, key)) {
                continue;
            }
            if (parseQty(line.optString(remainKey, "0")) > 0) {
                return line;
            }
        }
        return null;
    }

    private JSONObject findArticle(String scanned) {
        String key = normalize(scanned);
        for (int i = 0; i < huSaveData.length(); i++) {
            JSONObject line = huSaveData.optJSONObject(i);
            if (line != null && matchesArticle(line, key)) {
                return line;
            }
        }
        return null;
    }

    private static boolean matchesArticle(JSONObject line, String key) {
        return key.equals(normalize(line.optString("ARTICLE", "")))
                || key.equals(normalize(line.optString("EAN11", "")));
    }

    private void applyRemainingTo0003() {
        try {
            for (int i = 0; i < huSaveData.length(); i++) {
                JSONObject line = huSaveData.optJSONObject(i);
                if (line == null) {
                    continue;
                }
                syncLineQuantities(line, true);
            }
        } catch (JSONException e) {
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    /**
     * Open TO_MSA and TO_0001 are already reduced by any scanned quantity.
     * Total = open TO_MSA + open TO_0001.
     * SCAN_0003 = (TO_MSA - SCAN_MSA_QTY) + (TO_0001 - SCAN_0001_QTY).
     */
    private void syncLineQuantities(JSONObject line, boolean write0003) throws JSONException {
        double toMsa = parseQty(line.optString("TO_MSA", "0"));
        double to0001 = parseQty(line.optString("TO_0001", "0"));
        double scanMsa = parseQty(line.optString("SCAN_MSA_QTY", "0"));
        double scan0001 = parseQty(line.optString("SCAN_001_QTY", "0"));
        double remainMsa = Math.max(0, toMsa - scanMsa);
        double remain0001 = Math.max(0, to0001 - scan0001);
        line.put("TOTAL_QTY", formatQty(toMsa + to0001));
        line.put("REMAIN_MSA", formatQty(remainMsa));
        line.put("REMAIN_0001", formatQty(remain0001));
        if (write0003) {
            line.put("SCAN_0003_QTY", formatQty(remainMsa + remain0001));
        }
    }

    private void restore0003() {
        try {
            for (int i = 0; i < huSaveData.length(); i++) {
                JSONObject line = huSaveData.optJSONObject(i);
                if (line == null) {
                    continue;
                }
                line.put("SCAN_0003_QTY", line.optString("ORIG_0003", "0"));
            }
        } catch (JSONException e) {
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private JSONArray buildEtData() throws JSONException {
        JSONArray out = new JSONArray();
        for (int i = 0; i < huSaveData.length(); i++) {
            JSONObject source = huSaveData.optJSONObject(i);
            if (source == null) {
                continue;
            }
            JSONObject line = new JSONObject();
            line.put("ARTICLE", source.optString("ARTICLE", ""));
            line.put("SCAN_MSA_QTY", source.optString("SCAN_MSA_QTY", "0"));
            line.put("SCAN_0001_QTY", source.optString("SCAN_001_QTY", "0"));
            line.put("SCAN_0003_QTY", source.optString("SCAN_0003_QTY", "0"));
            out.put(line);
        }
        return out;
    }

    private void refreshCrateCounts() {
        double msaQty = 0;
        double crate0001Qty = 0;
        for (int i = 0; i < huSaveData.length(); i++) {
            JSONObject line = huSaveData.optJSONObject(i);
            if (line == null) {
                continue;
            }
            msaQty += parseQty(line.optString("SCAN_MSA_QTY", "0"));
            crate0001Qty += parseQty(line.optString("SCAN_001_QTY", "0"));
        }
        setCrateCounts(msaQty, crate0001Qty);
    }

    private void showFieldError(final EditText field, String message) {
        UIFuncs.errorSound(getContext());
        new AlertBox(getContext()).getBox("Err", message, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                focus(field);
            }
        });
    }

    private void focus(final EditText field) {
        field.post(new Runnable() {
            @Override
            public void run() {
                field.requestFocus();
                if (field.getText() != null && field.getText().length() > 0) {
                    field.setSelection(0, field.getText().length());
                }
            }
        });
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim().toUpperCase();
        int index = 0;
        while (index < text.length() - 1 && text.charAt(index) == '0') {
            index++;
        }
        return text.substring(index);
    }

    private void getHu(String hu) {
        if (requestInFlight) {
            return;
        }
        String picklist = txt_picklist_no.getText() == null ? "" : txt_picklist_no.getText().toString().trim();
        if (picklist.isEmpty()) {
            UIFuncs.errorSound(getContext());
            new AlertBox(getContext()).getBox("Err", "Picklist no is required");
            return;
        }
        huSaveData = new JSONArray();
        validatedMsaBin = "";
        requestInFlight = true;
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_V01_HU_GET_RFC);
            args.put("IM_USER", USER == null ? "" : USER);
            args.put("IM_PLANT", WERKS == null ? "" : WERKS);
            args.put("IM_PICKLIST", picklist);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZWM_V01_HU_GET_RFC, REQUEST_GET_HU, args);
        } catch (JSONException e) {
            requestInFlight = false;
            e.printStackTrace();
            dismissDialog();
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void validateMsaBin(String bin) {
        if (requestInFlight) {
            return;
        }
        requestInFlight = true;
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_V01_MSA_BIN_VALIDATE);
            args.put("IM_USER", USER == null ? "" : USER);
            args.put("IM_PLANT", WERKS == null ? "" : WERKS);
            args.put("IM_LGPLA", bin);
            showProcessingAndSubmit(Vars.ZWM_V01_MSA_BIN_VALIDATE, REQUEST_VALIDATE_BIN, args);
        } catch (JSONException e) {
            requestInFlight = false;
            e.printStackTrace();
            dismissDialog();
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void bindHuData(JSONObject responsebody) throws JSONException {
        JSONArray rows = responsebody.optJSONArray("ET_DATA");
        JSONArray saveRows = new JSONArray();
        boolean anyRow = false;
        if (rows != null) {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null || isHeaderRow(row)) {
                    continue;
                }
                String article = row.optString("MATNR", "").trim();
                if (article.isEmpty()) {
                    article = row.optString("ARTICLE", "").trim();
                }
                if (article.isEmpty() || "ARTICLE".equalsIgnoreCase(article)) {
                    continue;
                }
                anyRow = true;
                double openMsa = Math.max(0, parseQty(firstQty(row, "TO_MSA"))
                        - parseQty(firstQty(row, "SCAN_MSA_QTY")));
                double open0001 = Math.max(0, parseQty(firstQty(row, "TO_0001"))
                        - parseQty(firstQty(row, "SCAN_0001_QTY", "SCAN_001_QTY")));
                String ean = row.optString("EAN11", "").trim();
                JSONObject line = new JSONObject();
                line.put("ARTICLE", article);
                line.put("EAN11", ean);
                line.put("TO_MSA", formatQty(openMsa));
                line.put("TO_0001", formatQty(open0001));
                line.put("SCAN_MSA_QTY", "0");
                line.put("SCAN_001_QTY", "0");
                line.put("SCAN_0003_QTY", "0");
                line.put("ORIG_0003", "0");
                syncLineQuantities(line, false);
                saveRows.put(line);
            }
        }
        if (!anyRow) {
            huSaveData = new JSONArray();
            setCrateCounts(0, 0);
            UIFuncs.errorSound(getContext());
            new AlertBox(getContext()).getBox("No Data", "No HU data found");
            txt_hu_no.setText("");
            txt_hu_no.requestFocus();
            return;
        }
        huSaveData = saveRows;
        chk_crate_0003.setChecked(false);
        setCrateCounts(0, 0);
        txt_article.setText("");
        focus(txt_article);
    }

    private static String firstQty(JSONObject row, String... keys) {
        for (String key : keys) {
            String value = row.optString(key, "").trim();
            if (!value.isEmpty() && !key.equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "0";
    }

    private static String textOf(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static String returnMessage(JSONObject returnobj) {
        if (returnobj == null) {
            return "";
        }
        String message = returnobj.optString("MESSAGE", "").trim();
        if (!message.isEmpty()) {
            return message;
        }
        StringBuilder text = new StringBuilder();
        appendReturnPart(text, returnobj.optString("MESSAGE_V1", ""));
        appendReturnPart(text, returnobj.optString("MESSAGE_V2", ""));
        appendReturnPart(text, returnobj.optString("MESSAGE_V3", ""));
        appendReturnPart(text, returnobj.optString("MESSAGE_V4", ""));
        return text.toString().trim();
    }

    private static void appendReturnPart(StringBuilder text, String part) {
        if (part == null) {
            return;
        }
        String value = part.trim();
        if (value.isEmpty()) {
            return;
        }
        if (text.length() > 0) {
            text.append('\n');
        }
        text.append(value);
    }

    private static boolean isHeaderRow(JSONObject row) {
        String matnr = row.optString("MATNR", "").trim();
        String ean = row.optString("EAN11", "").trim();
        String scanMsa = row.optString("SCAN_MSA_QTY", "").trim();
        if ("MATNR".equalsIgnoreCase(matnr) || "EAN11".equalsIgnoreCase(ean)
                || "SCAN_MSA_QTY".equalsIgnoreCase(scanMsa)) {
            return true;
        }
        return matnr.equalsIgnoreCase("Material Number") || ean.contains("International Article Number");
    }

    private void setCrateCounts(double msaQty, double crate0001Qty) {
        txt_msa_crate.setText(formatQty(msaQty));
        txt_crate_0001.setText(formatQty(crate0001Qty));
    }

    private static double parseQty(String raw) {
        if (raw == null) {
            return 0;
        }
        String value = raw.trim().replace(",", "");
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatQty(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        currentRequest = request;
        dismissDialog();
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, args);
                } catch (Exception e) {
                    requestInFlight = false;
                    dismissDialog();
                    new AlertBox(getContext()).getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, JSONObject args) {
        if (URL == null || URL.lastIndexOf("/") < 0) {
            requestInFlight = false;
            dismissDialog();
            new AlertBox(getContext()).getBox("Err", "Server URL is not set");
            return;
        }
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload ->" + params.toString());

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject responsebody) {
                requestInFlight = false;
                dismissDialog();
                if (!isAdded()) {
                    return;
                }
                Log.d(TAG, "response ->" + responsebody);
                if (responsebody == null || responsebody.length() == 0) {
                    UIFuncs.errorSound(getContext());
                    new AlertBox(getContext()).getBox("Err", "No response from Server");
                    return;
                }
                try {
                    String message = "";
                    String saveMessage = "";
                    if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                        JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                        message = returnobj.optString("MESSAGE", "");
                        saveMessage = returnMessage(returnobj);
                        if ("E".equalsIgnoreCase(returnobj.optString("TYPE", ""))) {
                            UIFuncs.errorSound(getContext());
                            if (currentRequest == REQUEST_VALIDATE_BIN) {
                                txt_msa_bin.setText("");
                                new AlertBox(getContext()).getBox("Err", message.isEmpty() ? "Invalid MSA BIN" : message,
                                        new android.content.DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(android.content.DialogInterface dialog, int which) {
                                                focus(txt_msa_bin);
                                            }
                                        });
                            } else if (currentRequest == REQUEST_SAVE) {
                                new AlertBox(getContext()).getBox("Err", saveMessage.isEmpty() ? "Unable to save" : saveMessage);
                            } else {
                                huSaveData = new JSONArray();
                                chk_crate_0003.setChecked(false);
                                setCrateCounts(0, 0);
                                txt_hu_no.setText("");
                                new AlertBox(getContext()).getBox("Err", message.isEmpty() ? "Unable to get HU" : message,
                                        new android.content.DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(android.content.DialogInterface dialog, int which) {
                                                focus(txt_hu_no);
                                            }
                                        });
                            }
                            return;
                        }
                    }
                    if (currentRequest == REQUEST_GET_HU) {
                        bindHuData(responsebody);
                    } else if (currentRequest == REQUEST_VALIDATE_BIN) {
                        validatedMsaBin = textOf(txt_msa_bin);
                        txt_article.setText("");
                        txt_msa_bin.setText("");
                        focus(txt_article);
                    } else if (currentRequest == REQUEST_SAVE) {
                        new AlertBox(getContext()).getBox("Success", saveMessage.isEmpty() ? "Saved" : saveMessage);
                        clearAfterSave();
                    }
                } catch (JSONException e) {
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

    private Response.ErrorListener volleyErrorListener() {
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
                requestInFlight = false;
                dismissDialog();
                if (!isAdded()) {
                    return;
                }
                if (currentRequest == REQUEST_VALIDATE_BIN) {
                    txt_msa_bin.setText("");
                    txt_msa_bin.requestFocus();
                } else if (currentRequest == REQUEST_GET_HU) {
                    huSaveData = new JSONArray();
                    setCrateCounts(0, 0);
                }
                new AlertBox(getContext()).getBox("Err", err);
            }
        };
    }

    private void dismissDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        dialog = null;
    }
}
