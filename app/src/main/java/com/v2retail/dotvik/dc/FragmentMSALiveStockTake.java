package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
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

    /**
     * Empty Bin button — plain button (no color toggle).
     * Clicking it immediately triggers Submit with IM_BIN_EMP = "X".
     * A BIN must have been scanned first (currentData != null).
     */
    Button btn_empty_bin;

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
    private boolean saveInFlight = false;

    public FragmentMSALiveStockTake() {}

    public static FragmentMSALiveStockTake newInstance(String param1, String param2) {
        return new FragmentMSALiveStockTake();
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("MSA Live Stock Take");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_msa_live_stock_take, container, false);

        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        FragmentActivity activity = getActivity();

        tv_stock_take_id = rootView.findViewById(R.id.tv_msa_live_stock_take_stock_id);
<<<<<<< HEAD
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
=======
        tv_plant         = rootView.findViewById(R.id.tv_msa_live_stock_take_plant);
        txt_tq           = rootView.findViewById(R.id.txt_msa_live_stock_take_tq);
        txt_sq           = rootView.findViewById(R.id.txt_msa_live_stock_take_sq);
        txt_pq           = rootView.findViewById(R.id.txt_msa_live_stock_take_rq);
        txt_cur_binno    = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_bin);
        txt_cur_crate    = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_crate);
        txt_cur_article  = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_article);
        txt_cur_sqty     = rootView.findViewById(R.id.txt_msa_live_stock_take_curr_sqty);
        txt_scan_binno   = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_bin);
        txt_scan_crate   = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_crate);
        txt_scan_article = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_article);
        txt_scan_sqty    = rootView.findViewById(R.id.txt_msa_live_stock_take_scan_sqty);
        llNextScreen     = rootView.findViewById(R.id.ll_msa_live_stock_take_next_screen);
        llStockTake      = rootView.findViewById(R.id.ll_msa_live_stock_take_stock_take);
        tableItems       = rootView.findViewById(R.id.table_msa_live_stock_take_items);
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac

        dd_stock_id_list = rootView.findViewById(R.id.dd_msa_live_stock_take_stock_take_id);
        dd_stock_id_list.setSelection(0);
        stockAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, stockIds);
        stockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_stock_id_list.setAdapter(stockAdapter);
        dd_stock_id_list.setOnTouchListener((v, me) -> { spinnerTouched = true; v.performClick(); return false; });

<<<<<<< HEAD
        btn_back = rootView.findViewById(R.id.btn_msa_live_stock_take_back);
        btn_next = rootView.findViewById(R.id.btn_msa_live_stock_take_next);
        btn_submit = rootView.findViewById(R.id.btn_msa_live_stock_take_submit);
=======
        btn_back      = rootView.findViewById(R.id.btn_msa_live_stock_take_back);
        btn_next      = rootView.findViewById(R.id.btn_msa_live_stock_take_next);
        btn_submit    = rootView.findViewById(R.id.btn_msa_live_stock_take_submit);
        btn_empty_bin = rootView.findViewById(R.id.btn_msa_live_stock_take_empty_bin);
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_submit.setOnClickListener(this);
        btn_empty_bin.setOnClickListener(this);

        clear(true);
        addInputEvents();
        return rootView;
    }

<<<<<<< HEAD
    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }
=======
    // ── onClick ───────────────────────────────────────────────────────────────
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac

    private static final class BapiOutcome {
        final boolean success;
        final String message;
        BapiOutcome(boolean success, String message) {
            this.success = success;
            this.message = message != null ? message : "";
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
        if (errors.length() > 0) return new BapiOutcome(false, errors.toString());
        StringBuilder infos = new StringBuilder();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String t = row.optString("TYPE", "");
            String m = row.optString("MESSAGE", "").trim();
            if (m.isEmpty()) continue;
            if ("S".equals(t) || "W".equals(t) || "I".equals(t)) {
                if (infos.length() > 0) infos.append('\n');
                infos.append(m);
            }
        }
        return new BapiOutcome(true, infos.length() > 0 ? infos.toString() : "OK");
    }

    private static BapiOutcome outcomeFromExMessage(JSONObject responsebody) {
        if (responsebody == null || !responsebody.has("EX_MESSAGE")) return null;
        try {
            Object raw = responsebody.get("EX_MESSAGE");
            if (raw instanceof JSONObject) {
                JSONObject o = (JSONObject) raw;
                String type = o.optString("TYPE", "");
                String m = o.optString("MESSAGE", "").trim();
                if ("E".equals(type) || "A".equals(type)) return new BapiOutcome(false, m.isEmpty() ? "Error" : m);
                return new BapiOutcome(true, m.isEmpty() ? "Saved." : m);
            }
        } catch (JSONException ignored) {}
        return null;
    }

    /**
     * Called after a wrong Crate or Article scan error is dismissed.
     * Clears BOTH the Crate and Article scan fields, then returns
     * focus to Crate so the user can re-scan without extra taps.
     */
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
<<<<<<< HEAD
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { clear(true); }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {}
                    });
=======
                    (d, w) -> clear(true), (d, w) -> {});
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
                break;

            case R.id.btn_msa_live_stock_take_next:
<<<<<<< HEAD
                if(dd_stock_id_list.getSelectedItem() != null
=======
                if (dd_stock_id_list.getSelectedItem() != null
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
                        && !dd_stock_id_list.getSelectedItem().toString().isEmpty()
                        && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select")) {
                    validateStockTakeId(dd_stock_id_list.getSelectedItem().toString());
                }
                break;

            case R.id.btn_msa_live_stock_take_submit:
<<<<<<< HEAD
                if(!scanData.isEmpty()){
                    saveData();
                }else{
                    box.getBox("No Data", "Nothing to save. Scan a BIN from the list, then crate (if required), then article/barcode until the server confirms the scan. Then tap SUBMIT.");
=======
                if (saveInFlight) { Log.w(TAG, "SUBMIT ignored: save in flight"); return; }
                if (!scanData.isEmpty()) {
                    saveData(false);
                } else {
                    box.getBox("No Data", "Nothing to save. Scan a BIN, then article, then tap SUBMIT.");
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
                }
                break;

            case R.id.btn_msa_live_stock_take_empty_bin:
                // Empty Bin: no toggle, no color change.
                // Immediately submits with IM_BIN_EMP = "X".
                onEmptyBinClicked();
                break;
        }
    }

<<<<<<< HEAD
=======
    /**
     * Empty Bin clicked — validates a BIN was scanned, then fires Submit
     * immediately with IM_BIN_EMP = "X". No color change on the button.
     */
    private void onEmptyBinClicked() {
        if (saveInFlight) {
            Log.w(TAG, "Empty Bin ignored: save in flight");
            return;
        }
        if (currentData == null) {
            box.getBox("No BIN Scanned", "Please scan a BIN first before marking it as empty.");
            return;
        }
        Log.d(TAG, "Empty Bin clicked for BIN=" + currentData.getBin() + " — firing submit with IM_BIN_EMP=X");
        saveData(true);
    }

    // ── Input events ──────────────────────────────────────────────────────────

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        new AlertBox(getContext()).getBox(title, message);
    }

>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
    private void addInputEvents() {
        dd_stock_id_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) {
                if (spinnerTouched) {
                    btn_next.setVisibility(View.GONE);
<<<<<<< HEAD
                    if(dd_stock_id_list.getSelectedItem() != null
                            && !dd_stock_id_list.getSelectedItem().toString().isEmpty()
                            && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select")) {
=======
                    if (dd_stock_id_list.getSelectedItem() != null
                            && !dd_stock_id_list.getSelectedItem().toString().isEmpty()
                            && !dd_stock_id_list.getSelectedItem().toString().equalsIgnoreCase("Select"))
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
                        btn_next.setVisibility(View.VISIBLE);
                    spinnerTouched = false;
                }
            }
<<<<<<< HEAD
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
                    Log.d(TAG, "Crate entered (editor action): " + value + " -> focus Art.No.");
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
                    Log.d(TAG, "Crate scanned: " + value + " -> focus Art.No.");
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
=======
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        txt_scan_binno.setOnEditorActionListener((tv, id, ke) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String v = UIFuncs.toUpperTrim(txt_scan_binno);
                if (!v.isEmpty()) { validateBinNo(v); return true; }
            }
            return false;
        });
        txt_scan_binno.addTextChangedListener(scanWatcher(s -> validateBinNo(s)));

        txt_scan_crate.setOnEditorActionListener((tv, id, ke) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String v = UIFuncs.toUpperTrim(txt_scan_crate);
                if (!v.isEmpty()) { crateScanned(v); return true; }
            }
            return false;
        });
        txt_scan_crate.addTextChangedListener(scanWatcher(s -> crateScanned(s)));

        txt_scan_article.setOnEditorActionListener((tv, id, ke) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String v = UIFuncs.toUpperTrim(txt_scan_article);
                if (!v.isEmpty()) { validateArticle(v); return true; }
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
            }
            return false;
        });
        txt_scan_article.addTextChangedListener(scanWatcher(s -> validateArticle(s)));
    }

    private TextWatcher scanWatcher(java.util.function.Consumer<String> action) {
        return new TextWatcher() {
            boolean fromScanner = false;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                fromScanner = (b == 0 && st == 0) && c > 3;
            }
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().toUpperCase().trim();
                if (!v.isEmpty() && fromScanner) action.accept(v);
            }
        };
    }

    private void crateScanned(String value) {
        txt_cur_crate.setText(value);
        UIFuncs.enableInput(con, txt_scan_article);
        txt_scan_article.requestFocus();
    }

    // ── State management ──────────────────────────────────────────────────────

    private void clear(boolean clearAll) {
        scanData = new HashMap<>();
        liveStockList = new ArrayList<>();
        totalScanned = 0;
        saveInFlight = false;
        step2();
        if (clearAll) {
            llNextScreen.setVisibility(View.GONE);
            llStockTake.setVisibility(View.VISIBLE);
            btn_next.setVisibility(View.GONE);
            btn_submit.setVisibility(View.GONE);
            UIFuncs.enableInput(con, dd_stock_id_list);
            getStockIDs();
        } else {
            validateStockTakeId(dd_stock_id_list.getSelectedItem().toString());
        }
    }

<<<<<<< HEAD
    private void step2(){
        tv_stock_take_id.setText("");
        tv_plant.setText(WERKS);
=======
    private void step2() {
        tv_stock_take_id.setText(""); tv_plant.setText(WERKS);
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
        txt_tq.setText(""); txt_sq.setText(""); txt_pq.setText("");
        txt_cur_crate.setText(""); txt_cur_binno.setText("");
        txt_cur_article.setText(""); txt_cur_sqty.setText("");
        txt_scan_binno.setText(""); txt_scan_crate.setText("");
        txt_scan_article.setText(""); txt_scan_sqty.setText("");
        tableItems.removeAllViews();
<<<<<<< HEAD
        llNextScreen.setVisibility(View.VISIBLE);
        llStockTake.setVisibility(View.GONE);
        btn_next.setVisibility(View.GONE);
        btn_submit.setVisibility(View.VISIBLE);
        UIFuncs.disableInput(con, txt_scan_crate);
        UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno);
        txt_scan_binno.requestFocus();
=======
        llNextScreen.setVisibility(View.VISIBLE); llStockTake.setVisibility(View.GONE);
        btn_next.setVisibility(View.GONE); btn_submit.setVisibility(View.VISIBLE);
        if (btn_submit != null) btn_submit.setEnabled(true);
        UIFuncs.disableInput(con, txt_scan_crate); UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno); txt_scan_binno.requestFocus();
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
    }

    private void afterSave() {
        scanData = new HashMap<>();
        currentData = null;
        txt_cur_crate.setText(""); txt_cur_binno.setText("");
        txt_cur_article.setText(""); txt_cur_sqty.setText("0");
        txt_scan_binno.setText(""); txt_scan_crate.setText("");
        txt_scan_article.setText(""); txt_scan_sqty.setText("0");
        UIFuncs.disableInput(con, txt_scan_crate); UIFuncs.disableInput(con, txt_scan_article);
        UIFuncs.enableInput(con, txt_scan_binno); txt_scan_binno.requestFocus();
    }

    // ── RFC helpers ───────────────────────────────────────────────────────────

    private void getStockIDs() {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_STOCK_TAKE_ID);
            args.put("IM_WERKS", WERKS); args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.ZWM_GET_STOCK_TAKE_ID, REQUEST_GET_STOCK_ID, args);
<<<<<<< HEAD
        } catch (JSONException e) {
            e.printStackTrace(); UIFuncs.errorSound(con);
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            new AlertBox(getContext()).getErrBox(e);
        }
=======
        } catch (JSONException e) { handleSetupError(e); }
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
    }

    private void validateStockTakeId(String stId) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GET_STOCK_BIN);
            args.put("IM_WERKS", WERKS); args.put("IM_USER", USER); args.put("IM_ST_ID", stId);
            showProcessingAndSubmit(Vars.ZWM_GET_STOCK_BIN, REQUEST_VALIDATE_STOCK_ID, args);
<<<<<<< HEAD
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
            Log.d(TAG, "MSA-DIAG raw IT_DATA length=" + rawLength);
            for(int i = 0; i < rawLength; i++){
                LiveStockBinCrate data = new Gson().fromJson(
                        IT_DATA_ARRAY.getJSONObject(i).toString(), LiveStockBinCrate.class);
                liveStockList.add(data);
                Log.d(TAG, "MSA-DIAG row[" + i + "] BIN=" + data.getBin()
                        + " CRATE=" + data.getCrate() + " PLANT=" + data.getPlant());
            }
            Log.d(TAG, "MSA-DIAG liveStockList.size=" + liveStockList.size());
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
            Log.d(TAG, "BIN " + binno + " found. withCarate=" + withCarate);
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
=======
        } catch (JSONException e) { handleSetupError(e); }
    }

    private void validateArticle(String article) {
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_LIVE_STOCK_SCANNING);
            args.put("IM_WERKS", WERKS); args.put("IM_USER", USER);
            args.put("IM_STOCK_TAKE_ID", tv_stock_take_id.getText());
            args.put("IM_CRATE", resolvedCrateForRfc());
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_cur_binno));
            args.put("IM_BARCODE", article);
            showProcessingAndSubmit(Vars.ZWM_LIVE_STOCK_SCANNING, REQUEST_LIVE_SCAN, args);
        } catch (JSONException e) { handleSetupError(e); }
    }

    /**
     * Unified save method.
     * @param emptyBin true  → IM_BIN_EMP = "X" (Empty Bin button clicked)
     *                 false → IM_BIN_EMP = ""  (normal Submit)
     *
     * When emptyBin = true and scanData is empty, a single placeholder row
     * is built from currentData so the FM knows which BIN is being marked empty.
     */
    private void saveData(boolean emptyBin) {
        if (saveInFlight) { Log.w(TAG, "saveData ignored: in flight"); return; }

        JSONObject args = new JSONObject();
        try {
            String stId = tv_stock_take_id.getText() != null
                    ? tv_stock_take_id.getText().toString().trim() : "";
            if (stId.isEmpty()) { showError("Error", "Stock Take ID missing."); return; }

            JSONArray itData;
            if (!scanData.isEmpty()) {
                // Normal path — build from scanned articles
                itData = buildItData(stId);
                if (itData == null) return;
            } else if (emptyBin && currentData != null) {
                // Empty Bin path — no articles scanned; send BIN row with SCAN_QTY=0
                itData = buildEmptyBinItData(stId);
            } else {
                showError("No Data", "Nothing to save. Scan a BIN and article first.");
                return;
            }

            args.put("bapiname", Vars.ZWM_STK_ADJ_MSA_BIN);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_STOCK_TAKE_ID", stId);
            putSaveHeader(args, itData);
            args.put("IT_DATA", itData);
            // IM_BIN_EMP — "X" for Empty Bin, "" for normal submit
            args.put("IM_BIN_EMP", emptyBin ? "X" : "");

            Log.d(TAG, "saveData emptyBin=" + emptyBin
                    + " IM_STOCK_TAKE_ID=" + stId
                    + " rows=" + itData.length()
                    + " IM_BIN_EMP=" + (emptyBin ? "X" : "\"\""));

            saveInFlight = true;
            if (btn_submit != null) btn_submit.setEnabled(false);
            showProcessingAndSubmit(Vars.ZWM_STK_ADJ_MSA_BIN, REQUEST_SAVE, args);

        } catch (JSONException e) {
<<<<<<< HEAD
            e.printStackTrace(); UIFuncs.errorSound(con);
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            new AlertBox(getContext()).getErrBox(e);
=======
            saveInFlight = false;
            if (btn_submit != null) btn_submit.setEnabled(true);
            handleSetupError(e);
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
        }
    }

    /** Build IT_DATA from scanData map (normal submit). */
    private JSONArray buildItData(String stId) {
        try {
<<<<<<< HEAD
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
            Log.d(TAG, "BIN has crate -> cursor -> Crate field");
        }else{
            UIFuncs.enableInput(con, txt_scan_crate);
            UIFuncs.enableInput(con, txt_scan_article);
            txt_scan_article.requestFocus();
            Log.d(TAG, "BIN has no crate -> cursor -> Art.No.");
        }

        populateTableData();
        txt_tq.setText(liveStockList.size() + "");
        txt_sq.setText(totalScanned + "");
        txt_pq.setText((liveStockList.size() - totalScanned) + "");
    }

    private void populateTableData(){
        tableItems.removeAllViews();
        int headerTextSize = 16, textSize = 14;
=======
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, LiveScanData> e : scanData.entrySet()) {
                JSONObject row = new JSONObject(new Gson().toJson(e.getValue()));
                row.put("ST_TAKE_ID", stId);
                if (row.optString("PLANT", "").trim().isEmpty() && WERKS != null) row.put("PLANT", WERKS);
                if (row.optString("SCAN_QTY", "").trim().isEmpty()) row.put("SCAN_QTY", "0");
                arr.put(row);
            }
            if (arr.length() == 0) { showError("Empty", "Nothing to submit."); return null; }
            return arr;
        } catch (Exception e) { box.getErrBox(e); return null; }
    }

    /** Build a single IT_DATA row for Empty Bin (no articles scanned). */
    private JSONArray buildEmptyBinItData(String stId) throws JSONException {
        JSONObject row = new JSONObject();
        row.put("BIN", currentData.getBin());
        String crate = currentData.getCrate() != null ? currentData.getCrate().trim() : "";
        row.put("CRATE", crate);
        row.put("MATERIAL", "");
        row.put("PLANT", WERKS != null ? WERKS : "");
        row.put("SCAN_QTY", "0");
        row.put("ST_TAKE_ID", stId);
        if (currentData.getLgtyp() != null) row.put("LGTYP", currentData.getLgtyp());
        JSONArray arr = new JSONArray();
        arr.put(row);
        return arr;
    }

    private void putSaveHeader(JSONObject args, JSONArray rows) throws JSONException {
        if (rows == null || rows.length() == 0) {
            args.put("IM_BIN", UIFuncs.toUpperTrim(txt_cur_binno));
            args.put("IM_CRATE", UIFuncs.toUpperTrim(txt_cur_crate));
            args.put("IM_DESKTOP", UIFuncs.toUpperTrim(txt_cur_crate).isEmpty() ? "" : "X"); return;
        }
        if (rows.length() == 1) {
            String bin = rows.getJSONObject(0).optString("BIN", "").trim().toUpperCase();
            String cr  = rows.getJSONObject(0).optString("CRATE", "").trim().toUpperCase();
            args.put("IM_BIN", bin); args.put("IM_CRATE", cr);
            args.put("IM_DESKTOP", cr.isEmpty() ? "" : "X"); return;
        }
        String fb = rows.getJSONObject(0).optString("BIN", "").trim();
        String fc = rows.getJSONObject(0).optString("CRATE", "").trim();
        boolean sb = true, sc = true;
        for (int i = 1; i < rows.length(); i++) {
            if (!fb.equalsIgnoreCase(rows.getJSONObject(i).optString("BIN","").trim())) sb = false;
            if (!fc.equalsIgnoreCase(rows.getJSONObject(i).optString("CRATE","").trim())) sc = false;
        }
        if (sb && sc) { args.put("IM_BIN",fb.toUpperCase()); args.put("IM_CRATE",fc.toUpperCase()); args.put("IM_DESKTOP",fc.isEmpty()?"":"X"); }
        else          { args.put("IM_BIN",""); args.put("IM_CRATE",""); args.put("IM_DESKTOP",""); }
    }
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac

    private void handleSetupError(Exception e) {
        e.printStackTrace(); UIFuncs.errorSound(con);
        if (dialog != null) { dialog.dismiss(); dialog = null; }
        new AlertBox(getContext()).getErrBox(e);
    }

<<<<<<< HEAD
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
=======
    // ── BAPI response parsing ─────────────────────────────────────────────────

    private static final class BapiOutcome {
        final boolean success, explicit;
        final String message;
        BapiOutcome(boolean s, String m, boolean e) { success=s; message=m!=null?m:""; explicit=e; }
        BapiOutcome(boolean s, String m)            { this(s,m,true); }
    }

    private static BapiOutcome evaluateBapiReturn(JSONObject body) throws JSONException {
        if (body == null || !body.has("EX_RETURN")) return null;
        Object raw = body.get("EX_RETURN");
        JSONArray rows = new JSONArray();
        if (raw instanceof JSONArray)      rows = (JSONArray) raw;
        else if (raw instanceof JSONObject) rows.put(raw);
        else return null;
        if (rows.length() == 0) return null;
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < rows.length(); i++) {
            if (!(rows.get(i) instanceof JSONObject)) continue;
            JSONObject row = rows.getJSONObject(i);
            String t = row.optString("TYPE","");
            if ("E".equals(t)||"A".equals(t)) {
                if (errors.length()>0) errors.append('\n');
                errors.append(row.optString("MESSAGE","Error"));
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
            }
        }
        if (errors.length()>0) return new BapiOutcome(false,errors.toString(),true);
        StringBuilder infos = new StringBuilder(); boolean explicit=false;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String t=row.optString("TYPE",""), m=row.optString("MESSAGE","").trim();
            if ("S".equals(t)||"W".equals(t)||"I".equals(t)) {
                explicit=true;
                if (!m.isEmpty()){ if(infos.length()>0) infos.append('\n'); infos.append(m); }
            }
        }
        if (explicit) return new BapiOutcome(true, infos.length()>0?infos.toString():"Saved successfully.",true);
        return new BapiOutcome(true,"",false);
    }

    private static BapiOutcome outcomeFromExMessage(JSONObject body) {
        if (body==null||!body.has("EX_MESSAGE")) return null;
        try {
<<<<<<< HEAD
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
                itDataJson.put("ST_TAKE_ID", stIdHeader);
                itDataJson.put("STOCK_TAKE", stIdHeader);
                if (itDataJson.has("MATERIAL") && !itDataJson.isNull("MATERIAL")) {
                    String mat = itDataJson.getString("MATERIAL").trim();
                    if (!mat.isEmpty()) itDataJson.put("MATNR", mat);
                }
                String plant = itDataJson.optString("PLANT", "").trim();
                if (plant.isEmpty() && WERKS != null && !WERKS.isEmpty()) itDataJson.put("PLANT", WERKS);
                String sq = itDataJson.optString("SCAN_QTY", "").trim();
                if (sq.isEmpty()) { itDataJson.put("SCAN_QTY", "0"); sq = "0"; }
                itDataJson.put("MENGE", sq);
                arrScanData.put(itDataJson);
            }
            if (arrScanData.length() == 0) {
                showError("Empty Request", "Noting to submit, please scan some articles");
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

    private void saveData() {
        JSONObject args = new JSONObject();
        JSONArray dataToSave = getScanDataToSubmit();
        if (dataToSave != null) {
            try {
                args.put("bapiname", Vars.ZWM_STK_ADJ_MSA_BIN);
                args.put("IM_WERKS", WERKS);
                args.put("IM_USER", USER);
                String stId = tv_stock_take_id.getText().toString();
                args.put("IM_STOCK_TAKE_ID", stId);
                args.put("IM_ST_ID", stId);
                putSaveHeaderFromSaveRows(args, dataToSave);
                JSONArray tablePayload = new JSONArray(dataToSave.toString());
                args.put("IT_SAVE", tablePayload);
                args.put("ET_SAVE", tablePayload);
                args.put("IT_DATA", tablePayload);
                Log.d(TAG, "saveData -> IM_STOCK_TAKE_ID=" + tv_stock_take_id.getText()
                        + " rows=" + dataToSave.length());
                showProcessingAndSubmit(Vars.ZWM_STK_ADJ_MSA_BIN, REQUEST_SAVE, args);
            } catch (JSONException e) {
                e.printStackTrace(); UIFuncs.errorSound(con);
                if (dialog != null) { dialog.dismiss(); dialog = null; }
                new AlertBox(getContext()).getErrBox(e);
=======
            Object raw = body.get("EX_MESSAGE");
            if (raw instanceof JSONObject) {
                JSONObject o=(JSONObject)raw;
                String t=o.optString("TYPE",""), m=o.optString("MESSAGE","").trim();
                if ("E".equals(t)||"A".equals(t)) return new BapiOutcome(false,m.isEmpty()?"Error":m,true);
                if ("S".equals(t)||"W".equals(t)||"I".equals(t)) return new BapiOutcome(true,m.isEmpty()?"Saved successfully.":m,true);
            }
        } catch (JSONException ignored) {}
        return null;
    }

    private void resetAfterScanError() {
        txt_scan_crate.setText(""); txt_scan_article.setText("");
        UIFuncs.enableInput(con, txt_scan_crate); UIFuncs.enableInput(con, txt_scan_article);
        txt_scan_crate.requestFocus();
    }

    // ── Data population ───────────────────────────────────────────────────────

    private void populateStockIDs(JSONObject body) {
        try {
            stockIds.clear(); stockIds.add("Select");
            JSONArray arr = body.getJSONArray("IT_DATA");
            for (int i=1; i<arr.length(); i++)
                stockIds.add(arr.getJSONObject(i).getString("ST_TAKE_ID"));
            if (stockIds.size()>0) {
                ((BaseAdapter)dd_stock_id_list.getAdapter()).notifyDataSetChanged();
                dd_stock_id_list.setEnabled(true); dd_stock_id_list.invalidate();
                dd_stock_id_list.setSelection(0); dd_stock_id_list.requestFocus();
            } else new AlertBox(getContext()).getBox("No Data","No Stock Take IDs found.",(d,w)->clear(true));
        } catch (Exception e) { e.printStackTrace(); new AlertBox(getContext()).getErrBox(e); }
    }

    private void setData(JSONObject body) {
        try {
            liveStockList=new ArrayList<>(); scanData=new HashMap<>(); totalScanned=0;
            JSONArray arr = body.getJSONArray("IT_DATA");
            for (int i=0; i<arr.length(); i++) {
                LiveStockBinCrate d = new Gson().fromJson(arr.getJSONObject(i).toString(), LiveStockBinCrate.class);
                liveStockList.add(d);
            }
            if (liveStockList.size()>0) {
                step2();
                tv_stock_take_id.setText(dd_stock_id_list.getSelectedItem().toString());
                txt_tq.setText(String.valueOf(liveStockList.size())); txt_sq.setText("0");
                populateTableData();
            } else new AlertBox(getContext()).getBox("No Data","Picklist is empty.",(d,w)->clear(true));
        } catch (Exception e) { e.printStackTrace(); new AlertBox(getContext()).getErrBox(e); }
    }

    private void validateBinNo(String binno) {
        for (LiveStockBinCrate data : liveStockList) {
            if (data.getBin().equalsIgnoreCase(binno) && !data.isPicked()) {
                data.setPicked(true);
                currentData = LiveStockBinCrate.newInstance(data);
                totalScanned++;
                boolean hasCrate = data.getCrate()!=null && !data.getCrate().isEmpty();
                setLastScanedItem(hasCrate);
                return;
            }
        }
        txt_scan_binno.setText("");
        UIFuncs.errorSound(con);
        box.getBox("Invalid Bin","Invalid BIN — check the table below.");
        txt_scan_binno.requestFocus();
    }

    private String resolvedCrateForRfc() {
        String s=UIFuncs.toUpperTrim(txt_scan_crate); if (!s.isEmpty()) return s;
        String c=UIFuncs.toUpperTrim(txt_cur_crate);  if (!c.isEmpty()) return c;
        if (currentData!=null&&currentData.getCrate()!=null&&!currentData.getCrate().isEmpty())
            return currentData.getCrate().trim().toUpperCase();
        return "";
    }

    private static String effectiveMaterial(LiveArticleQty d) {
        if (d==null) return "";
        String m=d.getMatnr();   if (m!=null&&!m.trim().isEmpty()) return m.trim();
        String a=d.getArticle(); if (a!=null&&!a.trim().isEmpty()) return a.trim();
        String b=d.getBarcode(); if (b!=null&&!b.trim().isEmpty()) return b.trim();
        return "";
    }

    private void updateScanStats(JSONObject body) {
        try {
            JSONObject ex=body.getJSONObject("EX_DATA");
            LiveArticleQty raw=new Gson().fromJson(ex.toString(),LiveArticleQty.class);
            String matKey=effectiveMaterial(raw);
            if (matKey.isEmpty()&&ex.has("MATNR")&&!ex.isNull("MATNR"))   matKey=ex.getString("MATNR").trim();
            if (matKey.isEmpty()&&ex.has("ARTICLE")&&!ex.isNull("ARTICLE")) matKey=ex.getString("ARTICLE").trim();
            if (matKey.isEmpty()) { showError("Scan Error","Server did not return article."); return; }

            LiveScanData row=scanData.get(matKey);
            if (row==null) {
                row=LiveScanData.copyProperties(currentData); row.setMaterial(matKey);
                String crate=resolvedCrateForRfc(); if (!crate.isEmpty()) row.setCrate(crate);
                String stId=tv_stock_take_id.getText()!=null?tv_stock_take_id.getText().toString().trim():"";
                if (!stId.isEmpty()) row.setStockTakeId(stId);
                if (row.getLgtyp()==null&&currentData!=null) row.setLgtyp(currentData.getLgtyp());
                scanData.put(matKey,row);
            }
            LiveScanData.updateScanQty(row,raw.getQty());
            double prev=Util.convertStringToDouble(txt_cur_sqty.getText().toString());
            double qty=Util.convertStringToDouble(raw.getQty());
            txt_cur_article.setText(UIFuncs.removeLeadingZeros(matKey));
            txt_cur_sqty.setText(Util.formatDouble(prev+qty));
            txt_scan_sqty.setText(Util.formatDouble(qty));
        } catch (Exception e) { e.printStackTrace(); new AlertBox(getContext()).getErrBox(e); }
        txt_scan_article.setText(""); txt_scan_article.requestFocus();
    }

    private void setLastScanedItem(boolean withCrate) {
        UIFuncs.disableInput(con,txt_scan_article); UIFuncs.disableInput(con,txt_scan_crate); UIFuncs.disableInput(con,txt_scan_binno);
        txt_cur_binno.setText(currentData.getBin()); txt_cur_crate.setText(currentData.getCrate());
        txt_cur_article.setText(""); txt_cur_sqty.setText("0");
        txt_scan_crate.setText(""); txt_scan_article.setText(""); txt_scan_sqty.setText("0");
        if (withCrate) { UIFuncs.enableInput(con,txt_scan_crate); txt_scan_crate.requestFocus(); }
        else           { UIFuncs.enableInput(con,txt_scan_crate); UIFuncs.enableInput(con,txt_scan_article); txt_scan_article.requestFocus(); }
        populateTableData();
        txt_tq.setText(String.valueOf(liveStockList.size()));
        txt_sq.setText(String.valueOf(totalScanned));
        txt_pq.setText(String.valueOf(liveStockList.size()-totalScanned));
    }

    private void populateTableData() {
        tableItems.removeAllViews();
        int hSz=16, rSz=14;
        TableLayout.LayoutParams tp=new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow hr=new TableRow(getContext()); hr.setId(0); hr.setPadding(0,0,0,0); hr.setLayoutParams(tp);
        hr.addView(headerCell("Bin",280,hSz)); hr.addView(headerCell("Crate",hSz));
        hr.addView(headerCell("Type",hSz));    hr.addView(headerCell("Plant",hSz));
        tableItems.addView(hr,tp);
        int rowNum=1;
        for (LiveStockBinCrate data : liveStockList) {
            if (!data.isPicked()) {
                TableRow tr=new TableRow(getContext()); tr.setId(rowNum); tr.setPadding(0,0,0,0); tr.setLayoutParams(tp);
                tr.addView(dataCell(data.getBin(),rSz)); tr.addView(dataCell(data.getCrate(),rSz));
                tr.addView(dataCell(data.getLgtyp()!=null?data.getLgtyp():"",rSz)); tr.addView(dataCell(data.getPlant(),rSz));
                tr.setTag(data); tableItems.addView(tr,tp); rowNum++;
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
            }
        }
    }

<<<<<<< HEAD
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
=======
    private TextView headerCell(String text, int textSizeSp) { return headerCell(text,TableRow.LayoutParams.WRAP_CONTENT,textSizeSp); }
    private TextView headerCell(String text, int width, int textSizeSp) {
        TextView tv=new TextView(getContext());
        tv.setLayoutParams(new TableRow.LayoutParams(width,TableRow.LayoutParams.WRAP_CONTENT));
        tv.setGravity(Gravity.CENTER); tv.setPadding(0,5,0,5);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,textSizeSp);
        tv.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        tv.setText(text); return tv;
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
    }
    private TextView dataCell(String text, int textSizeSp) {
        TextView tv=new TextView(getContext());
        tv.setText(text); tv.setTextSize(textSizeSp); tv.setPadding(5,2,0,2);
        tv.setBackground(getResources().getDrawable(R.drawable.table_cell_border)); return tv;
    }

    // ── Volley ────────────────────────────────────────────────────────────────

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
<<<<<<< HEAD
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        new Handler().postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                dialog.dismiss();
=======
        dialog=new ProgressDialog(getContext());
        dialog.setMessage("Please wait..."); dialog.setCancelable(false); dialog.show();
        new Handler().postDelayed(()->{
            try { submitRequest(rfc,request,args); }
            catch (Exception e) {
                dialog.dismiss();
                if (request==REQUEST_SAVE) unlockSave();
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
                new AlertBox(getContext()).getErrBox(e);
            }
        },1000);
    }

    private void unlockSave() { saveInFlight=false; if (btn_submit!=null) btn_submit.setEnabled(true); }

    private void submitRequest(String rfc, int request, JSONObject args) {
<<<<<<< HEAD
        String url = GatewayUrls.noAclJsonRfcUrl(this.URL, rfc);
        if (url.isEmpty()) {
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            UIFuncs.errorSound(con);
            box.getBox("Err", "Server URL is missing. Log in again from IP selection.");
            return;
        }
        final JSONObject params = args;
        Log.d(TAG, "RFC URL -> " + url);
        Log.d(TAG, "payload ->" + params.toString());

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params,
            responsebody -> {
                if (dialog != null) { dialog.dismiss(); dialog = null; }
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
                    if (outcome == null && request == REQUEST_SAVE) outcome = outcomeFromExMessage(responsebody);

                    if (outcome != null) {
                        if (!outcome.success) {
                            UIFuncs.errorSound(getContext());
                            AlertBox errBox = new AlertBox(getContext());
                            if (request == REQUEST_LIVE_SCAN) {
                                // Wrong Crate or Article No. — clear BOTH fields,
                                // return focus to Crate so user can re-scan immediately.
                                errBox.getBox("Err", outcome.message,
                                    (d, w) -> resetAfterScanError());
                            } else {
                                errBox.getBox("Err", outcome.message);
                            }
                            if (request == REQUEST_SAVE) Log.w(TAG, "MSA save SAP error: " + outcome.message);
                            return;
                        }
                        if (request == REQUEST_GET_STOCK_ID) { populateStockIDs(responsebody); return; }
                        if (request == REQUEST_VALIDATE_STOCK_ID) { setData(responsebody); return; }
                        if (request == REQUEST_LIVE_SCAN) { updateScanStats(responsebody); return; }
                        if (request == REQUEST_SAVE) {
                            Log.i(TAG, "MSA save response: " + outcome.message);
                            new AlertBox(getContext()).getBox("Success", outcome.message, (d, w) -> afterSave());
                            return;
                        }
                        return;
                    }
                    // outcome == null fallback
                    if (request == REQUEST_GET_STOCK_ID && responsebody.has("IT_DATA")) { populateStockIDs(responsebody); return; }
                    if (request == REQUEST_VALIDATE_STOCK_ID && responsebody.has("IT_DATA")) { setData(responsebody); return; }
                    if (request == REQUEST_LIVE_SCAN && responsebody.has("EX_DATA")) { updateScanStats(responsebody); return; }
                    if (request == REQUEST_SAVE) {
                        Log.w(TAG, "Save response missing EX_RETURN/EX_MESSAGE: " + responsebody);
                        UIFuncs.errorSound(getContext());
                        new AlertBox(getContext()).getBox("Err", "Unexpected server response after submit. Data may not be saved.");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    new AlertBox(getContext()).getErrBox(e);
                }
            }, volleyErrorListener()) {
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody() { return params.toString().getBytes(StandardCharsets.UTF_8); }
            @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res.toString());
                return res;
            }
        };
        mJsonRequest.setRetryPolicy(new RetryPolicy() {
            @Override public int getCurrentTimeout() { return 50000; }
            @Override public int getCurrentRetryCount() { return 1; }
            @Override public void retry(VolleyError error) throws VolleyError {}
        });
        mRequestQueue.add(mJsonRequest);
        Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError e) {
            e.printStackTrace();
            if (dialog != null) { dialog.dismiss(); dialog = null; }
=======
        String url=GatewayUrls.noAclJsonRfcUrl(this.URL,rfc);
        if (url.isEmpty()) {
            if (dialog!=null){dialog.dismiss();dialog=null;}
            if (request==REQUEST_SAVE) unlockSave();
            UIFuncs.errorSound(con); box.getBox("Err","Server URL missing."); return;
        }
        final JSONObject params=args; final int reqType=request;
        Log.d(TAG,"POST "+url+" payload="+params);

        RequestQueue q=ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest req=new JsonObjectRequest(Request.Method.POST,url,params,
            body->{
                if (dialog!=null){dialog.dismiss();dialog=null;}
                if (reqType==REQUEST_SAVE) unlockSave();
                if (body==null||body.equals("")||body.equals("null")||body.equals("{}")) {
                    UIFuncs.errorSound(con); new AlertBox(getContext()).getBox("Err","No response from server"); return;
                }
                try {
                    BapiOutcome o=evaluateBapiReturn(body);
                    if (o==null&&reqType==REQUEST_SAVE) o=outcomeFromExMessage(body);

                    if (o!=null) {
                        if (!o.success) {
                            UIFuncs.errorSound(getContext());
                            AlertBox eb=new AlertBox(getContext());
                            if (reqType==REQUEST_LIVE_SCAN) eb.getBox("Err",o.message,(d,w)->resetAfterScanError());
                            else                            eb.getBox("Err",o.message);
                            return;
                        }
                        if (reqType==REQUEST_GET_STOCK_ID)      { populateStockIDs(body); return; }
                        if (reqType==REQUEST_VALIDATE_STOCK_ID) { setData(body);          return; }
                        if (reqType==REQUEST_LIVE_SCAN)         { updateScanStats(body);  return; }
                        if (reqType==REQUEST_SAVE) {
                            String msg=o.explicit&&!o.message.isEmpty()?o.message:"Stock take saved successfully.";
                            new AlertBox(getContext()).getBox("Success",msg,(d,w)->afterSave()); return;
                        }
                        return;
                    }
                    if (reqType==REQUEST_GET_STOCK_ID      &&body.has("IT_DATA")) { populateStockIDs(body); return; }
                    if (reqType==REQUEST_VALIDATE_STOCK_ID &&body.has("IT_DATA")) { setData(body);          return; }
                    if (reqType==REQUEST_LIVE_SCAN         &&body.has("EX_DATA")) { updateScanStats(body);  return; }
                    if (reqType==REQUEST_SAVE)
                        new AlertBox(getContext()).getBox("Success","Stock take saved successfully.",(d,w)->afterSave());

                } catch (JSONException e) { e.printStackTrace(); new AlertBox(getContext()).getErrBox(e); }
            },
            err->{
                if (dialog!=null){dialog.dismiss();dialog=null;}
                if (reqType==REQUEST_SAVE) unlockSave();
                String msg;
                if      (err instanceof TimeoutError||err instanceof NoConnectionError) msg="Communication Error!";
                else if (err instanceof AuthFailureError) msg="Authentication Error!";
                else if (err instanceof ServerError)      msg="Server Side Error!";
                else if (err instanceof NetworkError)     msg="Network Error!";
                else if (err instanceof ParseError)       msg="Parse Error!";
                else                                      msg=err.toString();
                new AlertBox(getContext()).getBox("Err",msg);
            }) {
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody() { return params.toString().getBytes(StandardCharsets.UTF_8); }
            @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse r) {
                return super.parseNetworkResponse(r);
            }
        };
        req.setRetryPolicy(request==REQUEST_SAVE
            ? new RetryPolicy(){ @Override public int getCurrentTimeout(){return 50000;} @Override public int getCurrentRetryCount(){return 0;} @Override public void retry(VolleyError e) throws VolleyError{throw e;} }
            : new RetryPolicy(){ @Override public int getCurrentTimeout(){return 50000;} @Override public int getCurrentRetryCount(){return 1;}  @Override public void retry(VolleyError e){} });
        q.add(req);
        try { Log.d(TAG,"headers: "+req.getHeaders()); }
        catch (AuthFailureError e) {
            if (dialog!=null){dialog.dismiss();dialog=null;}
            if (request==REQUEST_SAVE) unlockSave();
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    Response.ErrorListener volleyErrorListener() {
<<<<<<< HEAD
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
=======
        return err->{
            String msg;
            if      (err instanceof TimeoutError||err instanceof NoConnectionError) msg="Communication Error!";
            else if (err instanceof AuthFailureError) msg="Authentication Error!";
            else if (err instanceof ServerError)      msg="Server Side Error!";
            else if (err instanceof NetworkError)     msg="Network Error!";
            else if (err instanceof ParseError)       msg="Parse Error!";
            else                                      msg=err.toString();
            if (dialog!=null){dialog.dismiss();dialog=null;}
            new AlertBox(getContext()).getBox("Err",msg);
>>>>>>> e14d7aa332ceb2f241c6851c778f717e7cc667ac
        };
    }
}
