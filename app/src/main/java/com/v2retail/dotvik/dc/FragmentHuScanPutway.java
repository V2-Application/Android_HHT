package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import androidx.fragment.app.Fragment;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.Vars;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.android.volley.Request;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Screen 01 — Unloading: HU Scanning & Putway to Palette
 * Flow: Enter Vehicle → Scan HU ({@link Vars#ZVND_UNLOAD_HU_VALIDATE_RFC}, shows PO/Inv/Vendor)
 *       → Scan Palette ({@link Vars#ZVND_UNLOAD_PALLATE_VALIDATION})
 *       → Enter HU weight
 *       → Save ({@link Vars#ZVND_UNLOAD_SAVE_RFC})
 * @version 12.106
 */
public class FragmentHuScanPutway extends Fragment {
    private static final String TAG = "FragmentHuScanPutway";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etDcSite, etVehicle, etHu, etHuNumber, etPalette, etPalletDisplay;
    private EditText etPo, etInv, etVendor, etSqTsq;
    private TextView tvStatus;

    private String URL = "", USER = "", WERKS = "";
    private boolean huValidated = false;
    private String validatedHu = "", poNo = "", billNo = "";
    /** Session total of successful weight submissions; increments by +1 per save. */
    private int sessionTsq = 0;

    public FragmentHuScanPutway() {}
    public static FragmentHuScanPutway newInstance() { return new FragmentHuScanPutway(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_hu_scan_putway, container, false);

        etDcSite        = view.findViewById(R.id.hu_et_dc_site);
        etVehicle       = view.findViewById(R.id.et_vehicle);
        etHu            = view.findViewById(R.id.et_hu);
        etHuNumber      = view.findViewById(R.id.hu_et_hu_number);
        etPalette       = view.findViewById(R.id.et_palette);
        etPalletDisplay = view.findViewById(R.id.hu_et_pallet);
        etPo            = view.findViewById(R.id.tv_po);
        etInv           = view.findViewById(R.id.tv_inv);
        etVendor        = view.findViewById(R.id.tv_vendor);
        etSqTsq         = view.findViewById(R.id.hu_et_sq_tsq);
        tvStatus        = view.findViewById(R.id.tv_status);

        etVehicle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    moveFocusToScanHu();
                    return true;
                }
                return false;
            }
        });
        addScannerFocusWatcher(etVehicle, new Runnable() {
            @Override public void run() { moveFocusToScanHu(); }
        });

        etHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String hu = etHu.getText().toString().trim();
                    if (!hu.isEmpty()) validateHu(hu);
                    return true;
                }
                return false;
            }
        });
        addScannerFocusWatcher(etHu, new Runnable() {
            @Override public void run() {
                String hu = etHu.getText().toString().trim();
                if (!hu.isEmpty()) validateHu(hu);
            }
        });

        etPalette.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) {
                if (!huValidated) { showStatus("Scan HU first.", false); return true; }
                String p = etPalette.getText().toString().trim();
                if (!p.isEmpty()) validatePalette(p);
                return true;
            }
        });
        addScannerFocusWatcher(etPalette, new Runnable() {
            @Override public void run() {
                if (!huValidated) {
                    showStatus("Scan HU first.", false);
                    return;
                }
                String palette = etPalette.getText().toString().trim();
                if (!palette.isEmpty()) {
                    validatePalette(palette);
                }
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
                    .setActionBarTitle("HU SCANNING & PUTWAY TO PALLET");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        refreshSessionValues();
        etDcSite.setText(WERKS);
        etPalette.setEnabled(false);
        clearDisplayFields();
        showStatus("Enter Vehicle No. and scan HU.", true);
        etVehicle.requestFocus();
    }

    private void refreshSessionValues() {
        if (activity == null) {
            return;
        }
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL") != null ? prefs.read("URL").trim() : "";
        USER = prefs.getSapUserId();
        WERKS = prefs.read("WERKS") != null ? prefs.read("WERKS").trim() : "";
        Log.d(TAG, "Session values -> URL=" + URL + ", USER=" + USER + ", WERKS=" + WERKS);
    }

    private void moveFocusToScanHu() {
        if (etVehicle.getText().toString().trim().isEmpty()) {
            etVehicle.requestFocus();
            return;
        }
        etHu.requestFocus();
    }

    private void moveFocusToScanPallet() {
        etPalette.post(new Runnable() {
            @Override public void run() {
                etPalette.requestFocus();
            }
        });
    }

    /** Barcode scanners often paste the full code without IME action — detect rapid input. */
    private void addScannerFocusWatcher(final EditText field, final Runnable onScanComplete) {
        field.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 2;
            }

            @Override public void afterTextChanged(Editable s) {
                if (scannerReading && !s.toString().trim().isEmpty()) {
                    onScanComplete.run();
                }
            }
        });
    }

    private void clearDisplayFields() {
        etHuNumber.setText("");
        etPalletDisplay.setText("");
        etPo.setText("");
        etInv.setText("");
        etVendor.setText("");
        updateSqTsq(0);
    }

    private void updateSqTsq(int sq) {
        etSqTsq.setText(sq + " / " + sessionTsq);
    }

    private void validateHu(final String hu) {
        refreshSessionValues();
        showProgress("Validating HU...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_UNLOAD_HU_VALIDATE_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_HU", hu);
        } catch (JSONException e) { dismissProgress(); return; }

        rfc(Vars.ZVND_UNLOAD_HU_VALIDATE_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                try {
                    JSONObject ret = r.optJSONObject("EX_RETURN");
                    String type = ret != null ? ret.optString("TYPE", "") : "";
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        huValidated = true;
                        validatedHu = hu;
                        etHuNumber.setText(hu);

                        JSONArray et = r.optJSONArray("ET_DATA");
                        if (et != null && et.length() > 0) {
                            JSONObject row = et.getJSONObject(0);
                            poNo = row.optString("PO_NO", "");
                            billNo = row.optString("BILL_NO", "");
                            String vendor = row.optString("VENDOR_NAME", "");
                            int sq = 0;
                            try {
                                sq = Integer.parseInt(row.optString("SQ", "0").trim());
                            } catch (Exception ignored) { }
                            etPo.setText(poNo);
                            etInv.setText(billNo);
                            etVendor.setText(vendor);
                            // Keep session TSQ (successful weight count); do not overwrite from API
                            updateSqTsq(sq);
                        }

                        etHu.setEnabled(false);
                        etPalette.setEnabled(true);
                        moveFocusToScanPallet();
                        showStatus("HU OK: " + hu + " — Scan Pallet.", true);
                    } else {
                        huValidated = false;
                        validatedHu = "";
                        clearDisplayFields();
                        String msg = sapMessage(ret, "HU not valid.");
                        showStatus("HU Error: " + msg, false);
                        etHu.setText("");
                        etHu.requestFocus();
                    }
                } catch (JSONException e) { showStatus("Parse error while validating HU.", false); }
            }
            @Override public void err(String e) { showStatus("Network: " + e, false); }
        });
    }

    private void validatePalette(final String palette) {
        refreshSessionValues();
        showProgress("Validating Palette...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_UNLOAD_PALLATE_VALIDATION);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_HU", validatedHu);
            p.put("IM_PALL", palette);
        } catch (JSONException e) { dismissProgress(); return; }

        rfc(Vars.ZVND_UNLOAD_PALLATE_VALIDATION, p, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    etPalletDisplay.setText(palette);
                    etPalette.setEnabled(false);
                    showStatus("Palette OK — Enter weight to save.", true);
                    showWeightDialog(palette);
                } else {
                    String msg = sapMessage(ret, "Pallet not valid.");
                    showStatus("Palette Error: " + msg, false);
                    etPalette.setText("");
                    etPalette.requestFocus();
                }
            }
            @Override public void err(String e) { showStatus("Network: " + e, false); }
        });
    }

    private void showWeightDialog(final String palette) {
        if (activity == null || activity.isFinishing()) {
            showStatus("Unable to open weight dialog.", false);
            etPalette.setEnabled(true);
            etPalette.requestFocus();
            return;
        }

        final EditText etWeight = new EditText(activity);
        etWeight.setHint("Enter HU Weight");
        etWeight.setSingleLine(true);
        etWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        etWeight.setPadding(pad, pad, pad, pad);

        final AlertDialog weightDialog = new AlertDialog.Builder(activity)
                .setTitle("Enter HU Weight")
                .setMessage("Enter weight for pallet " + palette)
                .setView(etWeight)
                .setCancelable(false)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    etPalette.setEnabled(true);
                    etPalette.requestFocus();
                    showStatus("Weight entry cancelled.", false);
                })
                .setPositiveButton("Confirm", null)
                .create();

        weightDialog.setOnShowListener(dialog -> weightDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String weight = etWeight.getText().toString().trim();
                    if (weight.isEmpty()) {
                        etWeight.setError("Enter HU weight");
                        etWeight.requestFocus();
                        return;
                    }
                    weightDialog.dismiss();
                    save(palette, weight);
                }));

        weightDialog.show();
        etWeight.requestFocus();
    }

    private void save(final String palette, final String huWeight) {
        refreshSessionValues();
        String vehicle = etVehicle.getText().toString().trim();
        if (USER.trim().isEmpty()) {
            Log.e(TAG, "Save blocked: USER is blank. URL=" + URL + ", WERKS=" + WERKS);
            showStatus("User can not be blank. Please login again.", false);
            etPalette.setEnabled(true);
            etPalette.requestFocus();
            return;
        }
        if (vehicle.isEmpty()) {
            showStatus("Enter Vehicle No.", false);
            etPalette.setEnabled(true);
            etPalette.requestFocus();
            return;
        }

        final String plant = clip(firstNonEmpty(textOf(etDcSite), WERKS), 4);
        final String extHu = clip(firstNonEmpty(textOf(etHuNumber), validatedHu, textOf(etHu)), 20);
        final String pallet = clip(firstNonEmpty(textOf(etPalletDisplay), palette), 10);
        final String po = clip(firstNonEmpty(textOf(etPo), poNo), 10);
        final String bill = clip(firstNonEmpty(textOf(etInv), billNo), 16);
        final String weight = formatHuWeight(huWeight);
        if (plant.isEmpty() || extHu.isEmpty() || pallet.isEmpty() || weight.isEmpty()) {
            showStatus("Plant, HU, pallet, and weight are required.", false);
            etPalette.setEnabled(true);
            etPalette.requestFocus();
            return;
        }

        JSONObject params = new JSONObject();
        try {
            JSONObject row = new JSONObject();
            row.put("PLANT", plant);
            row.put("VEHICLE", clip(vehicle, 10));
            row.put("EXT_HU", extHu);
            row.put("PALETTE", pallet);
            row.put("PO_NO", po);
            row.put("BILL_NO", bill);
            row.put("HU_WT", weight);
            JSONArray imParms = new JSONArray();
            imParms.put(row);

            params.put("bapiname", Vars.ZVND_UNLOAD_SAVE_RFC);
            params.put("IM_USER", USER.trim());
            params.put("IM_PARMS", imParms);
        } catch (JSONException e) {
            showStatus("Could not build save request.", false);
            etPalette.setEnabled(true);
            etPalette.requestFocus();
            return;
        }

        showProgress("Saving...");
        rfc(Vars.ZVND_UNLOAD_SAVE_RFC, params, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                String msg = ret != null ? ret.optString("MESSAGE", "").trim() : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    if (!msg.isEmpty()) {
                        showBottomToast(msg);
                    }
                    resetFields();
                } else {
                    showStatus("Save Error: " + (msg.isEmpty() ? "Could not save data." : msg), false);
                    etPalette.setEnabled(true);
                    etPalette.requestFocus();
                }
            }
            @Override public void err(String e) {
                showStatus("Network: " + e, false);
                etPalette.setEnabled(true);
                etPalette.requestFocus();
            }
        });
    }

    private static String textOf(EditText field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    /** ZSTR_UNLOAD_SAVE component lengths: CHAR fields are clipped so one long value cannot drop the row. */
    private static String clip(String value, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, max);
    }

    /** HU_WT is QUAN 15(3). */
    private static String formatHuWeight(String raw) {
        String t = raw == null ? "" : raw.trim().replace(',', '.');
        if (t.isEmpty()) {
            return "";
        }
        try {
            return new BigDecimal(t).setScale(3, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private void resetFields() {
        huValidated = false;
        validatedHu = poNo = billNo = "";
        // Keep Vehicle No for continuous putaway on the same vehicle
        etHu.setText("");
        etPalette.setText("");
        etHu.setEnabled(true);
        etPalette.setEnabled(false);
        sessionTsq++; // +1 for each successful weight submission
        clearDisplayFields(); // SQ / TSQ → 0 / sessionTsq
        etHu.requestFocus();
        showStatus("Scan next HU.", true);
    }

    private interface Cb { void ok(JSONObject r); void err(String e); }

    private String sapMessage(JSONObject ret, String fallback) {
        if (ret == null) {
            return fallback;
        }
        String msg = ret.optString("MESSAGE", "").trim();
        return msg.isEmpty() ? fallback : msg;
    }

    private void showBottomToast(String message) {
        if (activity == null || message == null || message.trim().isEmpty()) {
            return;
        }
        Toast toast = Toast.makeText(activity, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.show();
    }

    private void rfc(String name, JSONObject params, final Cb cb) {
        String base = URL.contains("/ValueXMW") ? URL.replace("/ValueXMW", "") : URL;
        String url = base + "/noacljsonrfcadaptor?bapiname=" + name + "&aclclientid=android";
        Log.d(TAG, "RFC request -> " + name + " " + params);
        Log.d(TAG, "RFC url -> " + url);
        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST, url, params,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) {
                    dismissProgress();
                    Log.d(TAG, "RFC response -> " + name + ": " + r);
                    cb.ok(r);
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    dismissProgress();
                    Log.e(TAG, "RFC error -> " + name, e);
                    cb.err(e.getMessage() != null ? e.getMessage() : "Network error");
                }
            });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void showStatus(String msg, boolean ok) {
        if (tvStatus == null) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ok ? 0xFFE8F5E9 : 0xFFFFEBEE);
        tvStatus.setTextColor(ok ? 0xFF065F46 : 0xFFB71C1C);
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
