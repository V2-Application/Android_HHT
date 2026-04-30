package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.provider.Contacts;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.livestock.LiveArticleQty;
import com.v2retail.dotvik.modal.livestock.LiveScanData;
import com.v2retail.dotvik.modal.livestock.LiveStockBinCrate;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FragmentMSALiveStockTake extends Fragment implements View.OnClickListener {

    private static final int REQUEST_GET_STOCK_ID = 1500;
    private static final int REQUEST_VALIDATE_STOCK_ID = 1501;
    private static final int REQUEST_LIVE_SCAN = 1502;
    private static final int REQUEST_SAVE = 1503;

    private static final String TAG = FragmentMSALiveStockTake.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    Button btn_back, btn_next, btn_submit;
    TextView tv_plant, tv_stock_take_id;
    EditText txt_tq, txt_sq, txt_pq;
    EditText txt_cur_binno, txt_cur_crate, txt_cur_article, txt_cur_sqty;
    EditText txt_scan_binno, txt_scan_crate, txt_scan_article, txt_scan_sqty;

    LinearLayout llStockTake, llNextScreen;
    TableLayout tableItems;

    boolean spinnerTouched = false;
    Spinner dd_stock_id_list;
    List<String> stockIds = new ArrayList<String>();
    ArrayAdapter<String> stockAdapter;

    List<LiveStockBinCrate> liveStockList = new ArrayList<>();
    Map<String, LiveScanData> scanData = new HashMap<>();
    LiveStockBinCrate currentData = null;

    int totalScanned = 0;

    /**
     * Re-entrancy guard for SAVE — prevents the SUBMIT button from firing
     * multiple ZWM_STK_ADJ_MSA_BIN RFC calls when tapped quickly or while a
     * previous save is still in-flight. Reset only in onResponse / onErrorResponse.
     */
    private boolean saveInFlight = false;

    public FragmentMSALiveStockTake() {
        // Required empty public constructor
    }

    public static FragmentMSALiveStockTake newInstance(String param1, String param2) {
        return new FragmentMSALiveStockTake();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("MSA Live Stock Take");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_msa_live_stock_take, container, false);

        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        FragmentActivity activity = getActivity();

        tv_stock_take_id = rootView.findViewById(R.id.tv_msa_live_stock_take_stock_id);
        tv_plant = rootView.findViewById(R.id.tv_msa_live_stock_take_plant);

        txt_tq = rootView.findViewById(R.id.txt_msa_live_stock_take_tq);
        txt_sq = rootView.findViewById(R.id.txt_msa_live_stock_take_sq);
        txt_pq = rootView.findViewById(R.id.txt_msa_live_stock_take_rq);

        txt_cur_binno = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_bin);
        txt_cur_crate = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_crate);
        txt_cur_article = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_article);
        txt_cur_sqty = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_sqty);

        txt_scan_binno = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_bin);
        txt_scan_crate = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_crate);
        txt_scan_article = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_article);
        txt_scan_sqty = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_sqty);

        llNextScreen = rootView.findViewById(R.id.ll_msa_live_stock_take_next_screen);
        llStockTake = rootView.findViewById(R.id.ll_msa_live_stock_take_stock_take);

        tableItems = rootView.findViewById(R.id.table_msa_live_stock_take_items);

        dd_stock_id_list = rootView.findViewById(R.id.dd_msa_live_stock_take_stock_take_id);
        dd_stock_id_list.setSelection(0);
        stockAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1, stockIds);
        stockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_stock_id_list.setAdapter(stockAdapter);
        dd_stock_id_list.setOnTouchListener((v,me) -> {spinnerTouched = true; v.performClick(); return false;});

        btn_back = rootView.findViewById(R.id.btn_msa_live_stock_take_back);
        btn_next = rootView.findViewById(R.id.btn_msa_live_stock_take_next);
        btn_submit = rootView.findViewById(R.id.btn_msa_live_stock_take_submit);

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_submit.setOnClickListener(this);

        clear(true);
        addInputEvents();
        return rootView;
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private static final class BapiOutcome {
        final boolean success;
        final String message;
        final boolean explicit; // true = TYPE was S/E/W/I/A (clear); false = TYPE was blank
        BapiOutcome(boolean success, String message, boolean explicit) {
            this.success = success;
            this.message = message != null ? message : "";
            this.explicit = explicit;
        }
        BapiOutcome(boolean success, String message) {
            this(success, message, true);
        }
    }

    private static BapiOutcome evaluateBapiReturn(JSONObject responsebody) throws JSONException {
        if (responsebody == null || !responsebody.has("EX_RETURN")) return null;
        Object raw = responsebody.get("EX_RETURN");
        JSONArray rows = new JSONArray();
        if (raw instanceof JSONArray) rows = (JSONArray) raw;
        else if (raw instanceof JSONObject) rows.put(raw);
        else return null;
        if (rows.length() == 0) return null;
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < rows.length(); i++) {
            Object o = rows.get(i);
            if (!(o instanceof JSONObject)) continue;
            JSONObject row = (JSONObject) o;
            String t = row.optString("TYPE", "");
            if ("E".equals(t) || "A".equals(t)) {
                String m = row.optString("MESSAGE", "Error");
                if (errors.length() > 0) errors.append('\n');
                errors.append(m);
            }
        }
        if (errors.length() > 0) return new BapiOutcome(false, errors.toString(), true);
        StringBuilder infos = new StringBuilder();
        boolean anyExplicitType = false;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String t = row.optString("TYPE", "");
            String m = row.optString("MESSAGE", "").trim();
            if ("S".equals(t) || "W".equals(t) || "I".equals(t)) {
                anyExplicitType = true;
                if (!m.isEmpty()) {
                    if (infos.length() > 0) infos.append('\n');
                    infos.append(m);
                }
            }
        }
        if (anyExplicitType) {
            return new BapiOutcome(true, infos.length() > 0 ? infos.toString() : "Saved successfully.", true);
        }
        return new BapiOutcome(true, "", false);
    }

    private static BapiOutcome outcomeFromExMessage(JSONObject responsebody) {
        if (responsebody == null || !responsebody.has("EX_MESSAGE")) return null;
        try {
            Object raw = responsebody.get("EX_MESSAGE");
            if (raw instanceof JSONObject) {
                JSONObject o = (JSONObject) raw;
                String type = o.optString("TYPE", "");
                String m = o.optString("MESSAGE", "").trim();
                if ("E".equals(type) || "A".equals(type)) return new BapiOutcome(false, m.isEmpty() ? "Error" : m, true);
                if ("S".equals(type) || "W".equals(type) || "I".equals(type)) {
                    return new BapiOutcome(true, m.isEmpty() ? "Saved successfully." : m, true);
                }
            }
        } catch (JSONException ignored) {}
        return null;
    }

    private void resetAfterScanError() {
        txt_scan_crate.setText("");
        txt_scan_article.setText("");
        UIFuncs.enableInput(con, txt_scan_crate);
        UIFuncs.enableInput(con, txt_scan_article);
        txt_scan_crate.requestFocus();
        Log.d(TAG, "resetAfterScanError: crate + article cleared, focus -> Crate");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_msa_live_stock_take_back:
                box.getBox("Alert", "Do you want to go back. Any unsaved progress will be lost",
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { clear(true); }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {}
                    });
                break;
            case R.id.btn_msa_live_stock_take_next:
                if(dd_stock_id_list.getSelectedItem() != null
                        && !dd_stock_id_list.getSelectedItem().toString().isEmpty()
                        && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select")) {
                    validateStockTakeId(dd_stock_id_list.getSelectedItem().toString());
                }
                break;
            case R.id.btn_msa_live_stock_take_submit:
                if (saveInFlight) {
                    Log.w(TAG, "SUBMIT ignored: previous save still in flight");
                    return;
                }
                if(!scanData.isEmpty()){
                    saveData();
                }else{
                    box.getBox("No Data", "Nothing to save. Scan a BIN from the list, then crate (if required), then article/barcode until the server confirms the scan. Then tap SUBMIT.");
                }
                break;
        }
    }

    private void addInputEvents() {
        dd_stock_id_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spinnerTouched) {
                    btn_next.setVisibility(View.GONE);
                    if(dd_stock_id_list.getSelectedItem() != null
                            && !dd_stock_id_list.getSelectedItem().toString().isEmpty()
                            && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select")) {
                        btn_next.setVisibility(View.VISIBLE);
                    }
                    spinnerTouched = false;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        txt_scan_binno.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_binno);
                if (!value.isEmpty()) { validateBinNo(value); return true; }
            }
            return false;
        });
        txt_scan_binno.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }
            @Override public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) validateBinNo(value);
            }
        });

        txt_scan_crate.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_crate);
                if (!value.isEmpty()) {
                    txt_cur_crate.setText(value);
                    UIFuncs.enableInput(con, txt_scan_article);
                    txt_scan_article.requestFocus();
                    return true;
                }
            }
            return false;
        });
        txt_scan_crate.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }
            @Override public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    txt_cur_crate.setText(value);
                    UIFuncs.enableInput(con, txt_scan_article);
                    txt_scan_article.requestFocus();
                }
            }
        });

        txt_scan_article.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String value = UIFuncs.toUpperTrim(txt_scan_article);
                if (!value.isEmpty()) { validateArticle(value); return true; }
            }
            return false;
        });
        txt_scan_article.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }
            @Override public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) validateArticle(value);
            }
        });
    }

    private void clear(boolean clearAll) {
        scanData = new HashMap<>();
        liveStockList = new ArrayList<>();
        totalScanned = 0;
        saveInFlight = false;
        step2();
        if(clearAll){
            llNextScreen.setVisibility(View.GONE);
            llStockTake.setVisibility(View.VISIBLE);
            btn_next.setVisibility(View.GONE);
            btn_submit.setVisibility(View.GONE);
            totalScanned = 0;
            UIFuncs.enableInput(con, dd_stock_id_list);
            getStockIDs();
        }else{
            validateStockTakeId(dd_stock_id_list.getSelectedItem().toString());
        }
    }

    private void step2(){
        tv_stock_take_id.setText("");
        tv_plant.setText(WERKS);
        txt_tq.setText(""); txt_sq.setText(""); txt_pq.setText("");
        txt_cur_crate.setText(""); txt_cur_binno.setText("");
        txt_cur_article.setText(""); txt_cur_sqty.setText("");
        txt_scan_binno.setText(""); txt_scan_crate.setText("");
        txt_scan_article.setText(""); txt_scan_sqty.setText("");
        tableItems.removeAllViews();
        llNextScreen.setVisibility(View.VISIBLE);
        llStockTake.setVisibility(View.GONE);
        btn_next.setVisibility(View.GONE);
        btn_submit.setVisibility(View.VISIBLE);
        if (btn_submit != null) btn_submit.setEnabled(true);
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno);
        txt_scan_binno.requestFocus();
    }

    private void getStockIDs(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_STOCK_TAKE_ID);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.ZWM_GET_STOCK_TAKE_ID, REQUEST_GET_STOCK_ID, args);
        } catch (JSONException e) {
            e.printStackTrace(); UIFuncs.errorSound(con);
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void validateStockTakeId(String stockTakeId){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_STOCK_BIN);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_ST_ID", stockTakeId);
            showProcessingAndSubmit(Vars.ZWM_GET_STOCK_BIN, REQUEST_VALIDATE_STOCK_ID, args);
        } catch (JSONException e) {
            e.printStackTrace(); UIFuncs.errorSound(con);
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void populateStockIDs(JSONObject responsebody){
        try {
            stockIds.clear();
            stockIds.add("Select");
            JSONArray IT_DATA_ARRAY = responsebody.getJSONArray("IT_DATA");
            int length = IT_DATA_ARRAY.length();
            for(int i = 1; i < length; i++){
                stockIds.add(IT_DATA_ARRAY.getJSONObject(i).getString("ST_TAKE_ID"));
            }
            if(stockIds.size() > 0){
                ((BaseAdapter) dd_stock_id_list.getAdapter()).notifyDataSetChanged();
                dd_stock_id_list.setEnabled(true);
                dd_stock_id_list.invalidate();
                dd_stock_id_list.setSelection(0);
                dd_stock_id_list.requestFocus();
            }else{
                new AlertBox(getContext()).getBox("No Data", "Not Stock Take IDs Found.",
                    (d, w) -> clear(true));
            }
        }catch (Exception e) {
            e.printStackTrace();
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void setData(JSONObject responsebody){
        try {
            liveStockList = new ArrayList<>();
            scanData = new HashMap<>();
            totalScanned = 0;
            JSONArray IT_DATA_ARRAY = responsebody.getJSONArray("IT_DATA");
            int rawLength = IT_DATA_ARRAY.length();
            for(int i = 0; i < rawLength; i++){
                LiveStockBinCrate data = new Gson().fromJson(
                        IT_DATA_ARRAY.getJSONObject(i).toString(), LiveStockBinCrate.class);
                liveStockList.add(data);
            }
            if(liveStockList.size() > 0){
                step2();
                tv_stock_take_id.setText(dd_stock_id_list.getSelectedItem().toString());
                txt_tq.setText(liveStockList.size()+"");
                txt_sq.setText("0");
                txt_tq.setText(UIFuncs.toUpperTrim(txt_tq));
                populateTableData();
            }else{
                new AlertBox(getContext()).getBox("No Data", "Picklist is empty.", (d, w) -> clear(true));
            }
        }catch (Exception e) {
            e.printStackTrace();
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void validateBinNo(String binno){
        boolean binFound = false;
        boolean withCarate = false;
        for (LiveStockBinCrate data : liveStockList) {
            if(data.getBin().equalsIgnoreCase(binno) && !data.isPicked()){
                data.setPicked(true);
                currentData = LiveStockBinCrate.newInstance(data);
                binFound = true;
                if(data.getCrate() != null && !data.getCrate().isEmpty()) withCarate = true;
                totalScanned++;
                break;
            }
        }
        if(!binFound){
            txt_scan_binno.setText("");
            UIFuncs.errorSound(con);
            box.getBox("Invalid Bin", "Invalid BIN, please check below table for allowed BINs");
            txt_scan_binno.requestFocus();
        }else{
            setLastScanedItem(withCarate);
        }
    }

    private static String effectiveMaterialFromScanResponse(LiveArticleQty data) {
        if (data == null) return "";
        String m = data.getMatnr();
        if (m != null && !m.trim().isEmpty()) return m.trim();
        String a = data.getArticle();
        if (a != null && !a.trim().isEmpty()) return a.trim();
        String b = data.getBarcode();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return "";
    }

    private String resolvedCrateForRfc() {
        String fromScan = UIFuncs.toUpperTrim(txt_scan_crate);
        if (!fromScan.isEmpty()) return fromScan;
        String fromCur = UIFuncs.toUpperTrim(txt_cur_crate);
        if (!fromCur.isEmpty()) return fromCur;
        if (currentData != null && currentData.getCrate() != null && !currentData.getCrate().isEmpty())
            return currentData.getCrate().trim().toUpperCase();
        return "";
    }

    private void validateArticle(String article){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_LIVE_STOCK_SCANNING);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_STOCK_TAKE_ID", tv_stock_take_id.getText());
            args.put("IM_CRATE", resolvedCrateForRfc());
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_cur_binno));
            args.put("IM_BARCODE", article);
            showProcessingAndSubmit(Vars.ZWM_LIVE_STOCK_SCANNING, REQUEST_LIVE_SCAN, args);
        } catch (JSONException e) {
            e.printStackTrace(); UIFuncs.errorSound(con);
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void updateScanStats(JSONObject responsebody){
        try {
            JSONObject EX_DATA = responsebody.getJSONObject("EX_DATA");
            LiveArticleQty data = new Gson().fromJson(EX_DATA.toString(), LiveArticleQty.class);
            String materialKey = effectiveMaterialFromScanResponse(data);
            if (materialKey.isEmpty() && EX_DATA.has("MATNR") && !EX_DATA.isNull("MATNR"))
                materialKey = EX_DATA.getString("MATNR").trim();
            if (materialKey.isEmpty() && EX_DATA.has("ARTICLE") && !EX_DATA.isNull("ARTICLE"))
                materialKey = EX_DATA.getString("ARTICLE").trim();
            if (materialKey.isEmpty()) {
                showError("Scan Error", "Server did not return article/barcode for this scan.");
                return;
            }
            LiveScanData existing;
            if(scanData.containsKey(materialKey)){
                existing = scanData.get(materialKey);
            }else{
                existing = LiveScanData.copyProperties(currentData);
                existing.setMaterial(materialKey);
                String crateForRow = resolvedCrateForRfc();
                if (!crateForRow.isEmpty()) existing.setCrate(crateForRow);
                String stId = tv_stock_take_id.getText() != null
                        ? tv_stock_take_id.getText().toString().trim() : "";
                if (!stId.isEmpty()) existing.setStockTakeId(stId);
                scanData.put(materialKey, existing);
            }
            LiveScanData.updateScanQty(existing, data.getQty());
            double scanQty = Util.convertStringToDouble(txt_cur_sqty.getText().toString());
            double dataQty = Util.convertStringToDouble(data.getQty());
            scanQty += dataQty;
            txt_cur_article.setText(UIFuncs.removeLeadingZeros(materialKey));
            txt_cur_sqty.setText(Util.formatDouble(scanQty));
            txt_scan_sqty.setText(Util.formatDouble(dataQty));
        }catch (Exception e) {
            e.printStackTrace();
            new AlertBox(getContext()).getErrBox(e);
        }
        txt_scan_article.setText("");
        txt_scan_article.requestFocus();
    }

    private void setLastScanedItem(boolean withCarate){
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_binno);

        txt_cur_binno.setText(currentData.getBin());
        txt_cur_crate.setText(currentData.getCrate());
        txt_cur_article.setText("");
        txt_cur_sqty.setText("0");
        txt_scan_crate.setText("");
        txt_scan_article.setText("");
        txt_scan_sqty.setText("0");

        if(withCarate){
            UIFuncs.enableInput(con, txt_scan_crate);
            txt_scan_crate.requestFocus();
        }else{
            UIFuncs.enableInput(con, txt_scan_crate);
            UIFuncs.enableInput(con, txt_scan_article);
            txt_scan_article.requestFocus();
        }

        populateTableData();
        txt_tq.setText(liveStockList.size() + "");
        txt_sq.setText(totalScanned + "");
        txt_pq.setText((liveStockList.size() - totalScanned) + "");
    }

    private void populateTableData(){
        tableItems.removeAllViews();
        int headerTextSize = 16, textSize = 14;

        TextView headerBin = new TextView(getContext());
        TextView headerHuNo = new TextView(getContext());
        TextView headerExHuNo = new TextView(getContext());

        headerBin.setLayoutParams(new TableRow.LayoutParams(350, TableRow.LayoutParams.WRAP_CONTENT));
        headerBin.setGravity(Gravity.CENTER); headerBin.setPadding(0,5,0,5);
        headerBin.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerBin.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerBin.setText("Bin");

        headerHuNo.setGravity(Gravity.CENTER); headerHuNo.setPadding(0,5,0,5);
        headerHuNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerHuNo.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerHuNo.setText("Crate");

        headerExHuNo.setGravity(Gravity.CENTER); headerExHuNo.setPadding(0,5,0,5);
        headerExHuNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerExHuNo.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerExHuNo.setText("Plant");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(0,0,0,0);
        tr.setPadding(0,0,0,0); tr.setLayoutParams(trParams);
        tr.addView(headerBin); tr.addView(headerHuNo); tr.addView(headerExHuNo);
        tableItems.addView(tr, trParams);

        int rowNum = 1;
        for (LiveStockBinCrate data : liveStockList) {
            if(!data.isPicked()){
                TextView tvBin = new TextView(getContext());
                tvBin.setText(data.getBin()); tvBin.setTextSize(textSize); tvBin.setPadding(5,2,0,2);
                tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvHu = new TextView(getContext());
                tvHu.setText(data.getCrate()); tvHu.setTextSize(textSize); tvHu.setPadding(5,2,0,2);
                tvHu.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvExHu = new TextView(getContext());
                tvExHu.setText(data.getPlant()); tvExHu.setTextSize(textSize); tvExHu.setPadding(5,2,0,2);
                tvExHu.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                tr = new TableRow(getContext());
                tr.setId(rowNum); tr.setPadding(0,0,0,0); tr.setLayoutParams(trParams);
                tr.addView(tvBin); tr.addView(tvHu); tr.addView(tvExHu);
                tr.setTag(data);
                tableItems.addView(tr, trParams);
                rowNum++;
            }
        }
    }

    /**
     * Build the IT_DATA table payload for ZWM_STK_ADJ_MSA_BIN.
     *
     * The FM signature (verified against SAP ABAP Function Builder) requires
     * the rows in IT_DATA to use the exact field names:
     *   BIN, CRATE, MATERIAL, PLANT, SCAN_QTY, ST_TAKE_ID
     *
     * LiveScanData is already annotated with the correct @SerializedName values
     * so Gson serialization produces these field names directly. This method
     * only adds defensive defaults (PLANT from device, SCAN_QTY="0" if blank,
     * ST_TAKE_ID from screen header) — it must NOT add legacy aliases like
     * MATNR, MENGE, or STOCK_TAKE because those break the FM's structure
     * mapping and cause SAP to silently skip rows.
     */
    private JSONArray getScanDataToSubmit(){
        try {
            JSONArray arrScanData = new JSONArray();
            String stIdHeader = tv_stock_take_id.getText() != null
                    ? tv_stock_take_id.getText().toString().trim() : "";
            if (stIdHeader.isEmpty()) {
                showError("Submit Error", "Stock take ID is missing — cannot save to SAP.");
                return null;
            }
            for (Map.Entry<String, LiveScanData> dataEntry : scanData.entrySet()) {
                LiveScanData data = dataEntry.getValue();
                JSONObject itDataJson = new JSONObject(new Gson().toJson(data));

                // Required FM fields — defensive defaults
                itDataJson.put("ST_TAKE_ID", stIdHeader);

                String plant = itDataJson.optString("PLANT", "").trim();
                if (plant.isEmpty() && WERKS != null && !WERKS.isEmpty()) {
                    itDataJson.put("PLANT", WERKS);
                }

                String sq = itDataJson.optString("SCAN_QTY", "").trim();
                if (sq.isEmpty()) itDataJson.put("SCAN_QTY", "0");

                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Nothing to submit, please scan some articles");
            } else {
                return arrScanData;
            }
        }catch (Exception exce){ box.getErrBox(exce); }
        return null;
    }

    private void putSaveHeaderFromSaveRows(JSONObject args, JSONArray rows) throws JSONException {
        if (rows == null || rows.length() == 0) {
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_cur_binno));
            args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_cur_crate));
            args.put("IM_DESKTOP", UIFuncs.toUpperTrim(txt_cur_crate).isEmpty() ? "" : "X");
            return;
        }
        if (rows.length() == 1) {
            JSONObject r = rows.getJSONObject(0);
            String bin = r.optString("BIN", "").trim().toUpperCase();
            String cr = r.optString("CRATE", "").trim().toUpperCase();
            args.put("IM_BIN", bin); args.put("IM_CRATE", cr);
            args.put("IM_DESKTOP", cr.isEmpty() ? "" : "X");
            return;
        }
        String firstBin = rows.getJSONObject(0).optString("BIN", "").trim();
        String firstCrate = rows.getJSONObject(0).optString("CRATE", "").trim();
        boolean allSameBin = true, allSameCrate = true;
        for (int i = 1; i < rows.length(); i++) {
            JSONObject r = rows.getJSONObject(i);
            if (!firstBin.equalsIgnoreCase(r.optString("BIN", "").trim())) allSameBin = false;
            if (!firstCrate.equalsIgnoreCase(r.optString("CRATE", "").trim())) allSameCrate = false;
        }
        if (allSameBin && allSameCrate) {
            args.put("IM_BIN", firstBin.toUpperCase()); args.put("IM_CRATE", firstCrate.toUpperCase());
            args.put("IM_DESKTOP", firstCrate.isEmpty() ? "" : "X");
        } else {
            args.put("IM_BIN", ""); args.put("IM_CRATE", ""); args.put("IM_DESKTOP", "");
        }
    }

    /**
     * Fire ZWM_STK_ADJ_MSA_BIN with the FM-exact parameters:
     *   Imports : IM_USER, IM_WERKS, IM_STOCK_TAKE_ID, IM_CRATE, IM_BIN, IM_DESKTOP
     *   Tables  : IT_DATA  (rows with BIN/CRATE/MATERIAL/PLANT/SCAN_QTY/ST_TAKE_ID)
     *   Exports : EX_RETURN
     *
     * Legacy aliases IT_SAVE / ET_SAVE / IM_ST_ID are NOT sent — they don't
     * exist on the FM and pollute the request payload.
     */
    private void saveData() {
        if (saveInFlight) {
            Log.w(TAG, "saveData() ignored: previous save still in flight");
            return;
        }
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave != null) {
            try {
                args.put("bapiname", Vars.ZWM_STK_ADJ_MSA_BIN);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                args.put("IM_STOCK_TAKE_ID", tv_stock_take_id.getText().toString());
                putSaveHeaderFromSaveRows(args, dataToSave);

                // ── ONLY IT_DATA — no IT_SAVE, no ET_SAVE (those are not on the FM) ──
                args.put("IT_DATA", dataToSave);

                Log.d(TAG, "saveData -> ZWM_STK_ADJ_MSA_BIN IM_STOCK_TAKE_ID="
                        + tv_stock_take_id.getText() + " rows=" + dataToSave.length()
                        + " sample=" + (dataToSave.length() > 0 ? dataToSave.getJSONObject(0).toString() : "[]"));

                saveInFlight = true;
                if (btn_submit != null) btn_submit.setEnabled(false);

                showProcessingAndSubmit(Vars.ZWM_STK_ADJ_MSA_BIN, REQUEST_SAVE, args);
            } catch (JSONException e) {
                saveInFlight = false;
                if (btn_submit != null) btn_submit.setEnabled(true);
                e.printStackTrace(); UIFuncs.errorSound(con);
                if (dialog != null) { dialog.dismiss(); dialog = null; }
                new AlertBox(getContext()).getErrBox(e);
            }
        }
    }

    private void afterSave(){
        scanData = new HashMap<>();
        currentData = null;
        txt_cur_crate.setText(""); txt_cur_binno.setText("");
        txt_cur_article.setText(""); txt_cur_sqty.setText("0");
        txt_scan_binno.setText(""); txt_scan_crate.setText("");
        txt_scan_article.setText(""); txt_scan_sqty.setText("0");
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno);
        txt_scan_binno.requestFocus();
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        new Handler().postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                dialog.dismiss();
                if (request == REQUEST_SAVE) {
                    saveInFlight = false;
                    if (btn_submit != null) btn_submit.setEnabled(true);
                }
                new AlertBox(getContext()).getErrBox(e);
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = GatewayUrls.noAclJsonRfcUrl(this.URL, rfc);
        if (url.isEmpty()) {
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            if (request == REQUEST_SAVE) {
                saveInFlight = false;
                if (btn_submit != null) btn_submit.setEnabled(true);
            }
            UIFuncs.errorSound(con);
            box.getBox("Err", "Server URL is missing. Log in again from IP selection.");
            return;
        }
        final JSONObject params = args;
        final int reqType = request;
        Log.d(TAG, "RFC URL -> " + url);
        Log.d(TAG, "payload ->" + params.toString());

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params,
            responsebody -> {
                if (dialog != null) { dialog.dismiss(); dialog = null; }
                if (reqType == REQUEST_SAVE) {
                    saveInFlight = false;
                    if (btn_submit != null) btn_submit.setEnabled(true);
                }
                Log.d(TAG, "response ->" + responsebody);
                if (responsebody == null) {
                    UIFuncs.errorSound(con);
                    new AlertBox(getContext()).getBox("Err", "No response from Server");
                    return;
                }
                if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
                    new AlertBox(getContext()).getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                }
                try {
                    BapiOutcome outcome = evaluateBapiReturn(responsebody);
                    if (outcome == null && reqType == REQUEST_SAVE) outcome = outcomeFromExMessage(responsebody);

                    if (outcome != null) {
                        if (!outcome.success) {
                            UIFuncs.errorSound(getContext());
                            AlertBox errBox = new AlertBox(getContext());
                            if (reqType == REQUEST_LIVE_SCAN) {
                                errBox.getBox("Err", outcome.message,
                                    (d, w) -> resetAfterScanError());
                            } else {
                                errBox.getBox("Err", outcome.message);
                            }
                            if (reqType == REQUEST_SAVE) Log.w(TAG, "MSA save SAP error: " + outcome.message);
                            return;
                        }
                        if (reqType == REQUEST_GET_STOCK_ID) { populateStockIDs(responsebody); return; }
                        if (reqType == REQUEST_VALIDATE_STOCK_ID) { setData(responsebody); return; }
                        if (reqType == REQUEST_LIVE_SCAN) { updateScanStats(responsebody); return; }
                        if (reqType == REQUEST_SAVE) {
                            if (outcome.explicit) {
                                Log.i(TAG, "MSA save confirmed: " + outcome.message);
                                new AlertBox(getContext()).getBox("Success", outcome.message, (d, w) -> afterSave());
                            } else {
                                Log.w(TAG, "MSA save NOT confirmed by SAP — blank EX_RETURN. Response: " + responsebody);
                                UIFuncs.errorSound(getContext());
                                new AlertBox(getContext()).getBox(
                                    "Save Not Confirmed",
                                    "SAP did not confirm the save. The data may not have been written.\n\n" +
                                    "Please check Stock Take ID " + tv_stock_take_id.getText()
                                    + " in SAP (table ZWM_DCSTK2) and contact IT if data is missing.\n\n" +
                                    "Do NOT tap Submit again — repeated submits may create duplicate records."
                                );
                            }
                            return;
                        }
                        return;
                    }

                    if (reqType == REQUEST_GET_STOCK_ID && responsebody.has("IT_DATA")) { populateStockIDs(responsebody); return; }
                    if (reqType == REQUEST_VALIDATE_STOCK_ID && responsebody.has("IT_DATA")) { setData(responsebody); return; }
                    if (reqType == REQUEST_LIVE_SCAN && responsebody.has("EX_DATA")) { updateScanStats(responsebody); return; }

                    if (reqType == REQUEST_SAVE) {
                        Log.w(TAG, "Save response missing EX_RETURN/EX_MESSAGE: " + responsebody);
                        UIFuncs.errorSound(getContext());
                        new AlertBox(getContext()).getBox(
                            "Save Not Confirmed",
                            "Server response did not contain a save confirmation.\n\n" +
                            "Please verify Stock Take ID " + tv_stock_take_id.getText()
                            + " in SAP and contact IT if data is missing.\n\n" +
                            "Do NOT tap Submit again — repeated submits may create duplicate records."
                        );
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    new AlertBox(getContext()).getErrBox(e);
                }
            }, error -> {
                Log.i(TAG, "Error :" + error.toString());
                String err;
                if (error instanceof TimeoutError || error instanceof NoConnectionError) err = "Communication Error!";
                else if (error instanceof AuthFailureError) err = "Authentication Error!";
                else if (error instanceof ServerError) err = "Server Side Error!";
                else if (error instanceof NetworkError) err = "Network Error!";
                else if (error instanceof ParseError) err = "Parse Error!";
                else err = error.toString();
                if (dialog != null) { dialog.dismiss(); dialog = null; }
                if (reqType == REQUEST_SAVE) {
                    saveInFlight = false;
                    if (btn_submit != null) btn_submit.setEnabled(true);
                }
                new AlertBox(getContext()).getBox("Err", err);
            }) {
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody() { return params.toString().getBytes(StandardCharsets.UTF_8); }
            @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res.toString());
                return res;
            }
        };
        // Volley retries DISABLED for SAVE — prevents duplicate writes
        if (request == REQUEST_SAVE) {
            mJsonRequest.setRetryPolicy(new RetryPolicy() {
                @Override public int getCurrentTimeout() { return 50000; }
                @Override public int getCurrentRetryCount() { return 0; }
                @Override public void retry(VolleyError error) throws VolleyError { throw error; }
            });
        } else {
            mJsonRequest.setRetryPolicy(new RetryPolicy() {
                @Override public int getCurrentTimeout() { return 50000; }
                @Override public int getCurrentRetryCount() { return 1; }
                @Override public void retry(VolleyError error) throws VolleyError {}
            });
        }
        mRequestQueue.add(mJsonRequest);
        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError e) {
            e.printStackTrace();
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            if (request == REQUEST_SAVE) {
                saveInFlight = false;
                if (btn_submit != null) btn_submit.setEnabled(true);
            }
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    Response.ErrorListener volleyErrorListener() {
        return error -> {
            Log.i(TAG, "Error :" + error.toString());
            String err;
            if (error instanceof TimeoutError || error instanceof NoConnectionError) err = "Communication Error!";
            else if (error instanceof AuthFailureError) err = "Authentication Error!";
            else if (error instanceof ServerError) err = "Server Side Error!";
            else if (error instanceof NetworkError) err = "Network Error!";
            else if (error instanceof ParseError) err = "Parse Error!";
            else err = error.toString();
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            new AlertBox(getContext()).getBox("Err", err);
        };
    }
}
