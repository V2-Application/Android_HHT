package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Inbound Process New — VND Box Put to BIN
 * Flow: Scan HU (client-side) → auto focus Scan BIN →
 *       {@link Vars#ZVND_HU_PUTWAY_BIN_RFC} (I_WERKS, I_HU, I_BIN → ES_RETURN).
 * RFC message is shown as a bottom Toast.
 */
public class FragmentVndBoxPutBin extends Fragment {

    private static final String TAG = "FragmentVndBoxPutBin";

    private Activity activity;
    private ProgressDialog dialog;

    private EditText etPlant, etScanHu, etHuNumber, etScanBin, etBin;
    private TextView tvStatus;

    private String URL = "";
    private String werks = "";
    private String huNumber = "";
    private boolean requestInProgress = false;

    public FragmentVndBoxPutBin() {}

    public static FragmentVndBoxPutBin newInstance() {
        return new FragmentVndBoxPutBin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vnd_box_put_bin, container, false);

        etPlant    = view.findViewById(R.id.et_plant);
        etScanHu   = view.findViewById(R.id.et_scan_hu);
        etHuNumber = view.findViewById(R.id.et_hu_number);
        etScanBin  = view.findViewById(R.id.et_scan_bin);
        etBin      = view.findViewById(R.id.et_bin);
        tvStatus   = view.findViewById(R.id.tv_status);

        etScanHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_GO
                        || enterDown
                        || actionId == EditorInfo.IME_NULL) {
                    String hu = etScanHu.getText().toString().trim();
                    if (!hu.isEmpty()) {
                        onHuScanned(hu);
                    }
                    return true;
                }
                return false;
            }
        });

        etScanBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_GO
                        || enterDown
                        || actionId == EditorInfo.IME_NULL) {
                    String bin = etScanBin.getText().toString().trim();
                    if (!bin.isEmpty()) {
                        onBinScanned(bin);
                    }
                    return true;
                }
                return false;
            }
        });

        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("VND BOX PUT TO BIN");
        }
    }

    private void init() {
        activity = getActivity();
        if (activity == null) {
            return;
        }

        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        werks = prefs.read("WERKS");
        if (werks == null) {
            werks = "";
        }

        etPlant.setText(werks);
        disableScanBin();
        clearAllAndRefocusHu(false);
        showStatus("Scan HU Barcode.", true);
    }

    /** Step A — capture HU locally, then move focus to Scan BIN. */
    private void onHuScanned(String scannedHu) {
        if (werks.trim().isEmpty()) {
            showStatus("Enter plant first.", false);
            showBottomToast("Enter plant first.");
            return;
        }

        String hu = scannedHu.trim().toUpperCase(Locale.ROOT);
        if (hu.isEmpty()) {
            return;
        }

        huNumber = hu;
        etHuNumber.setText(hu);
        etBin.setText("");
        etScanBin.setText("");
        etScanHu.setText("");
        enableScanBin();
        etScanBin.requestFocus();
        showStatus("Scan destination BIN.", true);
    }

    /** Step B — BIN scan immediately calls ZVND_HU_PUTWAY_BIN_RFC. */
    private void onBinScanned(final String scannedBin) {
        if (requestInProgress) {
            return;
        }

        final String bin = scannedBin.trim().toUpperCase(Locale.ROOT);
        if (werks.trim().isEmpty() || huNumber.trim().isEmpty() || bin.isEmpty()) {
            showStatus("Plant, HU Number, and Bin are required. Scan HU first.", false);
            showBottomToast("Plant, HU Number, and Bin are required. Scan HU first.");
            return;
        }

        String rfcUrl = GatewayUrls.noAclJsonRfcUrl(URL, Vars.ZVND_HU_PUTWAY_BIN_RFC);
        if (rfcUrl.isEmpty()) {
            showStatus("Server URL missing. Please log in again.", false);
            showBottomToast("Server URL missing. Please log in again.");
            return;
        }

        JSONObject payload;
        try {
            payload = new JSONObject();
            payload.put("bapiname", Vars.ZVND_HU_PUTWAY_BIN_RFC);
            payload.put("I_WERKS", werks);
            payload.put("I_HU", huNumber);
            payload.put("I_BIN", bin);
        } catch (JSONException e) {
            showBottomToast("Could not build request.");
            return;
        }

        requestInProgress = true;
        etScanBin.setEnabled(false);
        showProgress("Putting HU to BIN...");
        Log.d(TAG, "RFC request -> " + Vars.ZVND_HU_PUTWAY_BIN_RFC);
        Log.d(TAG, "RFC url -> " + rfcUrl);
        Log.d(TAG, "RFC payload -> " + payload);

        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, rfcUrl, payload,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) {
                    dismissProgress();
                    requestInProgress = false;
                    Log.d(TAG, "RFC response -> " + Vars.ZVND_HU_PUTWAY_BIN_RFC + ": " + r);
                    handleRfcResponse(bin, r != null ? r : new JSONObject());
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    Log.e(TAG, "RFC error -> " + Vars.ZVND_HU_PUTWAY_BIN_RFC, e);
                    failAfterBinSubmit("Network error. Please retry.");
                }
            });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void handleRfcResponse(String bin, JSONObject r) {
        JSONObject esReturn = extractEsReturn(r);
        String type = esReturn != null ? esReturn.optString("TYPE", "").trim() : "";
        String message = esReturn != null ? esReturn.optString("MESSAGE", "").trim() : "";
        if (message.isEmpty()) {
            message = r.optString("Message", "").trim();
        }

        boolean success = isSapSuccess(type, r);
        if (success) {
            if (message.isEmpty()) {
                message = "HU putaway to bin completed.";
            }
            etBin.setText(bin);
            clearHuAndBinScanFields();
            showStatus(message, true);
            showBottomToast(message);
            etScanHu.requestFocus();
        } else {
            if (message.isEmpty()) {
                message = "Putaway to bin failed.";
            }
            failAfterBinSubmit(message);
        }
    }

    private static JSONObject extractEsReturn(JSONObject r) {
        JSONObject es = r.optJSONObject("ES_RETURN");
        if (es != null) {
            return es;
        }
        JSONObject data = r.optJSONObject("Data");
        if (data != null) {
            es = data.optJSONObject("ES_RETURN");
            if (es != null) {
                return es;
            }
        }
        return r.optJSONObject("EX_RETURN");
    }

    /** BAPIRET2 TYPE S / blank, or gateway Status S, is success. TYPE E is error. */
    private static boolean isSapSuccess(String type, JSONObject r) {
        if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
            return false;
        }
        if ("S".equalsIgnoreCase(type) || "W".equalsIgnoreCase(type) || "I".equalsIgnoreCase(type)) {
            return true;
        }
        Object status = r.opt("Status");
        if (status instanceof Boolean) {
            return (Boolean) status;
        }
        if (status != null && "S".equalsIgnoreCase(status.toString())) {
            return true;
        }
        if (status instanceof Number && ((Number) status).intValue() == 1) {
            return true;
        }
        return type.isEmpty();
    }

    private void failAfterBinSubmit(String message) {
        dismissProgress();
        requestInProgress = false;
        showStatus(message, false);
        showBottomToast(message);
        clearAllAndRefocusHu(false);
    }

    private void clearHuAndBinScanFields() {
        huNumber = "";
        etHuNumber.setText("");
        etScanHu.setText("");
        etScanBin.setText("");
        disableScanBin();
    }

    private void clearAllAndRefocusHu(boolean showScanHuStatus) {
        huNumber = "";
        etHuNumber.setText("");
        etBin.setText("");
        etScanHu.setText("");
        etScanBin.setText("");
        disableScanBin();
        etScanHu.requestFocus();
        if (showScanHuStatus) {
            showStatus("Scan HU Barcode.", true);
        }
    }

    private void enableScanBin() {
        etScanBin.setEnabled(true);
        etScanBin.setBackgroundResource(R.drawable.border);
    }

    private void disableScanBin() {
        etScanBin.setEnabled(false);
        etScanBin.setBackgroundResource(R.drawable.border_disabled_input);
    }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus == null) {
            return;
        }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
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
        if (activity == null) {
            return;
        }
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);
        }
        dialog.setMessage(msg);
        dialog.show();
    }

    private void dismissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
