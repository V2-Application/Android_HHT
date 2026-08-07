package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
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
import androidx.fragment.app.Fragment;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * PUT01 HU Wise Scanning (Inbound Process New)
 * Flow: Scan HU → {@code ZVND_PUT01_HU_VAL_RFC} → auto {@code ZVND_PUT01_SAVE_DATA_RFC}
 */
public class FragmentPut01HuWiseScanning extends Fragment implements View.OnClickListener {

    private static final String TAG = "Put01HuWiseScanning";
    private static final String DEFAULT_USER = "250";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etDcSite, etHu, etHuNumber, etPallate, etPo, etInv, etBoxHuQty, etTotScannedHu;
    private TextView tvStatus;
    private Button btnReset, btnBack;

    private String URL = "", USER = "", WERKS = "";
    private int totScannedHu = 0;
    private boolean requestInProgress = false;

    public FragmentPut01HuWiseScanning() {}
    public static FragmentPut01HuWiseScanning newInstance() { return new FragmentPut01HuWiseScanning(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_put01_hu_wise_scanning, container, false);

        etDcSite       = view.findViewById(R.id.et_dc_site);
        etHu           = view.findViewById(R.id.et_hu);
        etHuNumber     = view.findViewById(R.id.et_hu_number);
        etPallate      = view.findViewById(R.id.et_pallate);
        etPo           = view.findViewById(R.id.tv_po);
        etInv          = view.findViewById(R.id.tv_inv);
        etBoxHuQty     = view.findViewById(R.id.et_box_hu_qty);
        etTotScannedHu = view.findViewById(R.id.et_tot_scanned_hu);
        tvStatus       = view.findViewById(R.id.tv_status);
        btnReset       = view.findViewById(R.id.btn_reset);
        btnBack        = view.findViewById(R.id.btn_back);

        btnReset.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        addScanHuEvents();
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("PUT01- HU-WISE SCANNING");
        }
        if (etHu != null && etHu.isEnabled()) {
            etHu.requestFocus();
        }
    }

    private void addScanHuEvents() {
        etHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enterDown) {
                    String hu = etHu.getText().toString().trim().toUpperCase(Locale.ROOT);
                    Log.d(TAG, "Scan HU editor action -> hu=" + hu + " actionId=" + actionId);
                    if (!hu.isEmpty()) {
                        onHuScanned(hu);
                    }
                    return true;
                }
                return false;
            }
        });

        // HHT scanners often wedge the full barcode as one paste (no Enter).
        etHu.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hu = s.toString().trim().toUpperCase(Locale.ROOT);
                if (!hu.isEmpty() && scannerReading) {
                    Log.d(TAG, "Scan HU wedge detect -> hu=" + hu);
                    onHuScanned(hu);
                }
            }
        });
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        WERKS = prefs.read("WERKS");
        if (USER == null || USER.trim().isEmpty()) USER = DEFAULT_USER;
        etDcSite.setText(WERKS != null ? WERKS : "");
        clearDisplayFields();
        etTotScannedHu.setText(String.valueOf(totScannedHu));
        clearScanInput();
        showHint("Scan HU Barcode.");
        Log.d(TAG, "init -> WERKS=" + WERKS + " USER=" + USER + " URL=" + URL);
    }

    private void clearDisplayFields() {
        etHuNumber.setText("");
        etPallate.setText("");
        etPo.setText("");
        etInv.setText("");
        etBoxHuQty.setText("0 / 0");
    }

    private void onHuScanned(final String scannedHu) {
        if (requestInProgress) {
            Log.d(TAG, "onHuScanned skipped — request already in progress");
            return;
        }
        if (WERKS == null || WERKS.trim().isEmpty()) {
            showError("Plant (DC Site) not found. Please log in again.");
            return;
        }
        if (URL == null || URL.trim().isEmpty()) {
            showError("Server URL missing. Please log in again.");
            return;
        }

        JSONObject params;
        try {
            params = new JSONObject();
            params.put("bapiname", Vars.ZVND_PUT01_HU_VAL_RFC);
            params.put("IM_HU", scannedHu);
            params.put("IM_USER", USER);
            params.put("IM_PLANT", WERKS);
        } catch (JSONException e) {
            Log.e(TAG, "Could not build validate request", e);
            showError("Could not build validate request.");
            return;
        }

        requestInProgress = true;
        showProgress("Validating HU...");
        etHu.setEnabled(false);

        callRfc(Vars.ZVND_PUT01_HU_VAL_RFC, params, new RfcCb() {
            @Override
            public void ok(JSONObject r) {
                if (!isSapSuccess(r)) {
                    requestInProgress = false;
                    showError(sapMessage(r, "HU validation failed"));
                    clearScanInput();
                    return;
                }
                try {
                    JSONObject row = firstEtDataRow(r);
                    final String exidv = nonEmpty(row != null ? row.optString("EXIDV", "") : "", scannedHu);
                    final String palette = row != null ? row.optString("PALETTE", "") : "";
                    final String poNo = row != null ? row.optString("VPONO", "") : "";
                    final String invNo = row != null ? row.optString("INVNO", "") : "";
                    final String qty = row != null ? row.optString("QTY", "0") : "0";
                    savePut01(exidv, palette, poNo, invNo, qty, sapMessage(r, ""));
                } catch (JSONException e) {
                    requestInProgress = false;
                    Log.e(TAG, "Parse error validating HU", e);
                    showError("Parse error while validating HU.");
                    clearScanInput();
                }
            }

            @Override
            public void err(String message) {
                requestInProgress = false;
                showError(message != null ? message : "Network error while validating HU. Please retry.");
                clearScanInput();
            }
        });
    }

    private void savePut01(final String exidv, final String palette,
                           final String poNo, final String invNo, final String qty,
                           final String validateMessage) {
        JSONObject params;
        try {
            params = new JSONObject();
            params.put("bapiname", Vars.ZVND_PUT01_SAVE_DATA_RFC);
            params.put("IM_USER", USER);
            JSONObject row = new JSONObject();
            row.put("WERKS", WERKS);
            row.put("EXIDV", exidv);
            row.put("PALETTE", palette);
            row.put("ZPUT01_SCAN", "X");
            JSONArray it = new JSONArray();
            it.put(row);
            params.put("IT_DATA", it);
        } catch (JSONException e) {
            requestInProgress = false;
            Log.e(TAG, "Could not build save request", e);
            showError("Could not build save request.");
            clearScanInput();
            return;
        }

        showProgress("Saving...");
        callRfc(Vars.ZVND_PUT01_SAVE_DATA_RFC, params, new RfcCb() {
            @Override
            public void ok(JSONObject r) {
                requestInProgress = false;
                if (!isSapSuccess(r)) {
                    showError(sapMessage(r, "Could not save HU data."));
                    clearScanInput();
                    return;
                }
                updateDisplayFields(exidv, palette, poNo, invNo, qty);
                totScannedHu++;
                etTotScannedHu.setText(String.valueOf(totScannedHu));
                String msg = sapMessage(r, validateMessage);
                if (msg == null || msg.trim().isEmpty()) msg = "Data saved successfully.";
                showSuccess(msg);
                clearScanInput();
            }

            @Override
            public void err(String message) {
                requestInProgress = false;
                showError(message != null ? message : "Could not save HU data.");
                clearScanInput();
            }
        });
    }

    private interface RfcCb {
        void ok(JSONObject r);
        void err(String message);
    }

    /** Hits RFC via noacljsonrfcadaptor (same pattern as other screens). */
    private void callRfc(final String rfcName, JSONObject params, final RfcCb cb) {
        String url = GatewayUrls.noAclJsonRfcUrl(URL, rfcName);
        if (url.isEmpty()) {
            cb.err("Server URL missing. Please log in again.");
            return;
        }
        Log.d(TAG, "RFC request -> " + rfcName);
        Log.d(TAG, "RFC url -> " + url);
        Log.d(TAG, "RFC payload -> " + params);

        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, url, params,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) {
                    dismissProgress();
                    Log.d(TAG, "RFC response -> " + rfcName + ": " + r);
                    cb.ok(r != null ? r : new JSONObject());
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    dismissProgress();
                    Log.e(TAG, "RFC error -> " + rfcName, e);
                    cb.err(e.getMessage() != null ? e.getMessage() : "Network error");
                }
            });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private JSONObject firstEtDataRow(JSONObject response) throws JSONException {
        JSONArray et = response.optJSONArray("ET_DATA");
        if (et == null) {
            JSONObject data = response.optJSONObject("Data");
            if (data != null) et = data.optJSONArray("ET_DATA");
        }
        if (et != null && et.length() > 0) {
            return et.getJSONObject(0);
        }
        return null;
    }

    private void updateDisplayFields(String exidv, String palette, String poNo, String invNo, String qty) {
        etHuNumber.setText(exidv);
        etPallate.setText(nonEmpty(palette, "—"));
        etPo.setText(nonEmpty(poNo, "—"));
        etInv.setText(nonEmpty(invNo, "—"));
        etBoxHuQty.setText("0 / " + nonEmpty(qty, "0"));
    }

    private void clearScanInput() {
        etHu.setText("");
        etHu.setEnabled(true);
        etHu.requestFocus();
    }

    private void resetAll() {
        requestInProgress = false;
        totScannedHu = 0;
        etTotScannedHu.setText("0");
        clearDisplayFields();
        clearScanInput();
        showHint("Scan HU Barcode.");
    }

    /** Accepts gateway {@code Status=S} or SAP {@code EX_RETURN.TYPE=S}. */
    private static boolean isSapSuccess(JSONObject r) {
        Object status = r.opt("Status");
        if (status != null) {
            return "S".equals(status.toString().toUpperCase(Locale.ROOT));
        }
        JSONObject ret = r.optJSONObject("EX_RETURN");
        if (ret != null) {
            String type = ret.optString("TYPE", "");
            return "S".equalsIgnoreCase(type) || type.isEmpty();
        }
        return false;
    }

    private static String sapMessage(JSONObject r, String fallback) {
        String msg = r.optString("Message", "").trim();
        if (!msg.isEmpty()) return msg;
        JSONObject ret = r.optJSONObject("EX_RETURN");
        if (ret != null) {
            msg = ret.optString("MESSAGE", "").trim();
            if (!msg.isEmpty()) return msg;
        }
        return fallback;
    }

    private static String nonEmpty(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_reset) resetAll();
        else if (id == R.id.btn_back) {
            if (getFragmentManager() != null) getFragmentManager().popBackStack();
        }
    }

    /** Idle / instruction text in the status bar. */
    private void showHint(String msg) {
        if (tvStatus == null) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(0xFFE8F5E9);
        tvStatus.setTextColor(0xFF065F46);
    }

    /** Success → bottom Toast; status bar returns to scan hint. */
    private void showSuccess(String msg) {
        showBottomToast(msg);
        showHint("Scan HU Barcode.");
    }

    /** Error → alert box; status bar returns to scan hint. */
    private void showError(String msg) {
        if (box != null && msg != null && !msg.trim().isEmpty()) {
            box.getBox("Error", msg);
        }
        showHint("Scan HU Barcode.");
    }

    private void showBottomToast(String message) {
        if (activity == null || message == null || message.trim().isEmpty()) {
            return;
        }
        Toast toast = Toast.makeText(activity, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.show();
    }

    private void showProgress(String msg) {
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);
        }
        dialog.setMessage(msg);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }
}
