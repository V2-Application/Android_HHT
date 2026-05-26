package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
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
import java.util.Locale;

/**
 * Store Stock Track — scan EMP → BIN → ARTICLE; each article posts to SAP and increments quantity.
 */
public class FragmentStoreStockTrack extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentStoreStockTrack.class.getName();
    private static final int REQUEST_PUSH = 410;

    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText txtEmp;
    private EditText txtBin;
    private EditText txtArticle;
    private EditText txtQty;
    private Button btnReset;

    private boolean rfcInFlight = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingArticleScan;

    public FragmentStoreStockTrack() {}

    public static FragmentStoreStockTrack newInstance() {
        return new FragmentStoreStockTrack();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Home_Activity) {
            ((Home_Activity) getActivity()).setActionBarTitle("Stock Track");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_stock_track, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData prefs = new SharedPreferencesData(con);
        URL = prefs.read("URL");
        WERKS = prefs.read("WERKS");
        USER = prefs.read("USER");

        txtEmp = view.findViewById(R.id.txt_store_stock_track_emp);
        txtBin = view.findViewById(R.id.txt_store_stock_track_bin);
        txtArticle = view.findViewById(R.id.txt_store_stock_track_article);
        txtQty = view.findViewById(R.id.txt_store_stock_track_qty);
        btnReset = view.findViewById(R.id.btn_store_stock_track_reset);

        btnReset.setOnClickListener(this);
        wireScanField(txtEmp, this::onEmpScanned);
        wireScanField(txtBin, this::onBinScanned);
        wireArticleField();

        resetAll();
        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_store_stock_track_reset) {
            resetAll();
        }
    }

    private interface ScanAction {
        void run(String value);
    }

    private void wireScanField(EditText field, ScanAction action) {
        field.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                UIFuncs.hideKeyboard(getActivity());
                String v = UIFuncs.toUpperTrim(field);
                if (!v.isEmpty()) {
                    action.run(v);
                    return true;
                }
            }
            return false;
        });
        field.addTextChangedListener(new TextWatcher() {
            boolean fromScanner = false;

            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                fromScanner = (b == 0 && st == 0) && c > 0;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String v = s.toString().toUpperCase().trim();
                if (!v.isEmpty() && fromScanner) {
                    action.run(v);
                }
            }
        });
    }

    /** Article scans: debounce so full barcode is in the field before RFC (HHT suffix Enter). */
    private void wireArticleField() {
        txtArticle.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                UIFuncs.hideKeyboard(getActivity());
                scheduleArticleScan(UIFuncs.toUpperTrim(txtArticle));
                return true;
            }
            return false;
        });
        txtArticle.addTextChangedListener(new TextWatcher() {
            boolean fromScanner = false;

            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                fromScanner = (b == 0 && st == 0) && c > 0;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!fromScanner) return;
                String v = s.toString().toUpperCase().trim();
                if (!v.isEmpty()) {
                    scheduleArticleScan(v);
                }
            }
        });
    }

    private void scheduleArticleScan(String value) {
        if (value == null || value.isEmpty() || rfcInFlight) return;
        if (pendingArticleScan != null) {
            mainHandler.removeCallbacks(pendingArticleScan);
        }
        pendingArticleScan = () -> {
            pendingArticleScan = null;
            String latest = UIFuncs.toUpperTrim(txtArticle);
            if (!latest.isEmpty()) {
                onArticleScanned(latest);
            }
        };
        mainHandler.postDelayed(pendingArticleScan, 350);
    }

    private void onEmpScanned(String value) {
        if (!txtEmp.isEnabled()) return;
        txtEmp.setText(value);
        UIFuncs.disableInput(con, txtEmp);
        UIFuncs.enableInput(con, txtBin);
        txtBin.requestFocus();
    }

    private void onBinScanned(String value) {
        if (!txtBin.isEnabled() || UIFuncs.toUpperTrim(txtEmp).isEmpty()) return;
        txtBin.setText(value);
        UIFuncs.disableInput(con, txtBin);
        UIFuncs.enableInput(con, txtArticle);
        txtArticle.requestFocus();
    }

    private void onArticleScanned(String value) {
        if (UIFuncs.toUpperTrim(txtEmp).isEmpty()) {
            showFieldError(txtEmp, "Scan EMP Code first.");
            return;
        }
        if (UIFuncs.toUpperTrim(txtBin).isEmpty()) {
            showFieldError(txtBin, "Scan BIN first.");
            return;
        }
        if (rfcInFlight) return;
        txtArticle.setText(value);
        pushArticleToSap(value);
    }

    /**
     * Same payload shape as {@link PostDataOffiineFragment#sendToServer()} (ET_DATA table rows).
     */
    private void pushArticleToSap(String article) {
        final String rfc = Vars.ZWM_STORE_PUSHDATATOSAP_1STOCK;
        final String emp = UIFuncs.toUpperTrim(txtEmp);
        final String bin = UIFuncs.toUpperTrim(txtBin);
        final String site = WERKS != null ? WERKS.trim() : "";

        JSONObject args = new JSONObject();
        try {
            JSONObject row = new JSONObject();
            row.put("EMP_CODE", emp);
            row.put("SITE", site);
            row.put("GANDOLA", bin);
            row.put("ARTICLE", article);
            row.put("QUANTITY", 1);
            row.put("ART_TYPE", "");
            row.put("ZSIZE", "");
            row.put("PICKLIST", "");

            JSONArray etData = new JSONArray();
            etData.put(row);

            args.put("bapiname", rfc);
            args.put("IM_USER", emp.isEmpty() ? (USER != null ? USER : "") : emp);
            args.put("ET_DATA", etData);

            rfcInFlight = true;
            showProcessingAndSubmit(rfc, REQUEST_PUSH, args);
        } catch (JSONException e) {
            rfcInFlight = false;
            box.getErrBox(e);
        }
    }

    private void incrementQuantity() {
        double current = Util.convertStringToDouble(txtQty.getText().toString());
        txtQty.setText(Util.formatDouble(current + 1));
        UIFuncs.blinkEffectOnSuccess(txtQty);
    }

    private void resetAll() {
        rfcInFlight = false;
        if (pendingArticleScan != null) {
            mainHandler.removeCallbacks(pendingArticleScan);
            pendingArticleScan = null;
        }
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        txtEmp.setText("");
        txtBin.setText("");
        txtArticle.setText("");
        txtQty.setText("0");

        UIFuncs.enableInput(con, txtEmp);
        UIFuncs.disableInput(con, txtBin);
        UIFuncs.disableInput(con, txtArticle);
        txtEmp.requestFocus();
    }

    private void showFieldError(EditText field, String message) {
        UIFuncs.errorSound(con);
        UIFuncs.blinkEffectOnError(con, field, false);
        box.getBox("Alert", message, (d, w) -> field.requestFocus());
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                unlockAfterRfc();
                box.getErrBox(e);
            }
        }, 500);
    }

    private void unlockAfterRfc() {
        rfcInFlight = false;
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        if (!txtEmp.isEnabled()) {
            if (!txtBin.isEnabled()) {
                UIFuncs.enableInput(con, txtArticle);
                txtArticle.requestFocus();
            }
        }
    }

    /**
     * Azure store login saves URL ending in /ValueXMW — POST there with bapiname in JSON (see LoginActivity).
     * Legacy on-prem uses /noacljsonrfcadaptor?bapiname=…
     */
    private String buildRfcUrl(String rfc) {
        String stored = URL != null ? URL.trim() : "";
        if (stored.isEmpty()) {
            return "";
        }
        String low = stored.toLowerCase(Locale.ROOT);
        if (low.contains("valuexmw") || low.contains("azurewebsites.net")) {
            return stored;
        }
        String noAcl = GatewayUrls.noAclJsonRfcUrl(stored, rfc);
        if (!noAcl.isEmpty()) {
            return noAcl;
        }
        String base = stored.contains("/") ? stored.substring(0, stored.lastIndexOf('/')) : stored;
        return base + "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = buildRfcUrl(rfc);
        if (url.isEmpty()) {
            unlockAfterRfc();
            UIFuncs.errorSound(con);
            box.getBox("Err", "Server URL missing.");
            return;
        }

        final JSONObject params = args;
        Log.d(TAG, "RFC POST url=" + url);
        Log.d(TAG, "RFC payload=" + params);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, url, params,
                body -> {
                    unlockAfterRfc();
                    Log.d(TAG, "RFC response=" + body);
                    if (body == null || body.length() == 0) {
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "No response from server");
                        return;
                    }
                    try {
                        if (body.has("EX_RETURN")) {
                            Object raw = body.get("EX_RETURN");
                            JSONObject ret = raw instanceof JSONObject ? (JSONObject) raw : null;
                            if (ret != null) {
                                String type = ret.optString("TYPE", "");
                                String msg = ret.optString("MESSAGE", "").trim();
                                if ("E".equals(type) || "A".equals(type)) {
                                    UIFuncs.errorSound(con);
                                    box.getBox("Err", msg.isEmpty() ? "SAP error" : msg,
                                            (d, w) -> txtArticle.requestFocus());
                                    return;
                                }
                                if ("S".equals(type) || "W".equals(type) || "I".equals(type)) {
                                    onPushSuccess(msg);
                                    return;
                                }
                            }
                        }
                        onPushSuccess("");
                    } catch (JSONException e) {
                        box.getErrBox(e);
                    }
                },
                err -> {
                    unlockAfterRfc();
                    Log.e(TAG, "RFC error", err);
                    UIFuncs.errorSound(con);
                    box.getBox("Err", volleyMessage(err));
                }) {
            @Override public String getBodyContentType() {
                return "application/json";
            }

            @Override public byte[] getBody() {
                return params.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                return super.parseNetworkResponse(response);
            }
        };
        req.setRetryPolicy(new RetryPolicy() {
            @Override public int getCurrentTimeout() { return 50000; }
            @Override public int getCurrentRetryCount() { return 0; }
            @Override public void retry(VolleyError error) throws VolleyError { throw error; }
        });
        queue.add(req);
    }

    private void onPushSuccess(String message) {
        incrementQuantity();
        txtArticle.setText("");
        txtArticle.requestFocus();
        if (message != null && !message.isEmpty()) {
            Log.d(TAG, "RFC OK: " + message);
        }
    }

    private static String volleyMessage(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return "Communication Error!";
        }
        if (error instanceof AuthFailureError) return "Authentication Error!";
        if (error instanceof ServerError) return "Server Side Error!";
        if (error instanceof NetworkError) return "Network Error!";
        if (error instanceof ParseError) return "Parse Error!";
        return error.toString();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
