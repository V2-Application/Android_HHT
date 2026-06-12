package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
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

/**
 * Screen 02 — Inbound Putway to BIN
 * Flow: Scan BIN ({@link Vars#ZVND_PUTWAY_BIN_VAL_RFC})
 *       Scan Palette ({@link Vars#ZVND_PUTWAY_PALETTE_VAL_RFC} — auto-fetches PO/Inv/Vendor/box count)
 *       Auto Save ({@link Vars#ZVND_PUTWAY_SAVE_DATA_RFC})
 *       After save: clear BIN+Palette only, keep PO/Vendor/Inv.
 * @version 12.106
 */
public class FragmentInboundPutwayToBin extends Fragment {

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etDcSite, etBin, etBinDisplay, etPalette, etPaletteDisplay;
    private EditText etPo, etInv, etVendor, etScanBinBox, etTotScannedBin;
    private TextView tvStatus;

    private String URL = "", USER = "", WERKS = "";
    private boolean binValidated = false;
    private String validatedBin = "", validatedPall = "", poNo = "", billNo = "", vendorName = "";
    private int totScannedBin = 0;

    public FragmentInboundPutwayToBin() {}
    public static FragmentInboundPutwayToBin newInstance() { return new FragmentInboundPutwayToBin(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_inbound_putway_to_bin, container, false);

        etDcSite         = view.findViewById(R.id.et_dc_site);
        etBin            = view.findViewById(R.id.et_bin);
        etBinDisplay     = view.findViewById(R.id.et_bin_display);
        etPalette        = view.findViewById(R.id.et_palette);
        etPaletteDisplay = view.findViewById(R.id.et_palette_display);
        etPo             = view.findViewById(R.id.tv_po);
        etInv            = view.findViewById(R.id.tv_inv);
        etVendor         = view.findViewById(R.id.tv_vendor);
        etScanBinBox     = view.findViewById(R.id.et_scan_bin_box);
        etTotScannedBin  = view.findViewById(R.id.et_tot_scanned_bin);
        tvStatus         = view.findViewById(R.id.tv_status);

        etBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) {
                String b = etBin.getText().toString().trim();
                if (!b.isEmpty()) validateBin(b);
                return true;
            }
        });
        etPalette.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) {
                if (!binValidated) { showStatus("Scan BIN first.", false); return true; }
                String p = etPalette.getText().toString().trim();
                if (!p.isEmpty()) validatePalette(p);
                return true;
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
                    .setActionBarTitle("HU PUTWAY TO BIN");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        SharedPreferencesData prefs = new SharedPreferencesData(activity);
        URL = prefs.read("URL");
        USER = prefs.read("USER");
        WERKS = prefs.read("WERKS");
        etDcSite.setText(WERKS);
        etPalette.setEnabled(false);
        clearDisplayFields();
        showStatus("Scan Destination BIN.", true);
    }

    private void clearDisplayFields() {
        etBinDisplay.setText("");
        etPaletteDisplay.setText("");
        etPo.setText("");
        etInv.setText("");
        etVendor.setText("");
        etScanBinBox.setText("0 / 0");
    }

    private void validateBin(final String bin) {
        showProgress("Validating BIN...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_PUTWAY_BIN_VAL_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_BIN", bin);
        } catch (JSONException e) { dismissProgress(); return; }

        rfc(Vars.ZVND_PUTWAY_BIN_VAL_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    binValidated = true;
                    validatedBin = bin;
                    etBinDisplay.setText(bin);
                    etBin.setEnabled(false);
                    etPalette.setEnabled(true);
                    etPalette.requestFocus();
                    showStatus("BIN OK: " + bin + " — Scan Palette.", true);
                } else {
                    String msg = ret != null ? ret.optString("MESSAGE", "") : "";
                    showStatus("BIN Error: " + msg, false);
                    etBin.setText("");
                    etBin.requestFocus();
                }
            }
            @Override public void err(String e) { showStatus("Network: " + e, false); }
        });
    }

    private void validatePalette(final String palette) {
        showProgress("Validating Palette...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_PUTWAY_PALETTE_VAL_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_BIN", validatedBin);
            p.put("IM_PALL", palette);
        } catch (JSONException e) { dismissProgress(); return; }

        rfc(Vars.ZVND_PUTWAY_PALETTE_VAL_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                try {
                    JSONObject ret = r.optJSONObject("EX_RETURN");
                    String type = ret != null ? ret.optString("TYPE", "") : "";
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        validatedPall = palette;
                        etPaletteDisplay.setText(palette);

                        JSONArray et = r.optJSONArray("ET_DATA");
                        if (et != null && et.length() > 0) {
                            JSONObject row = et.getJSONObject(0);
                            poNo = row.optString("PO_NO", "");
                            billNo = row.optString("BILL_NO", "");
                            vendorName = row.optString("VENDOR_NAME", "");
                            String sq = row.optString("SQ", "0");
                            String tsq = row.optString("TSQ", String.valueOf(et.length()));
                            etPo.setText(poNo);
                            etInv.setText(billNo);
                            etVendor.setText(vendorName);
                            etScanBinBox.setText(sq + " / " + tsq);
                        }

                        etPalette.setEnabled(false);
                        showStatus("Palette OK — Saving...", true);
                        save();
                    } else {
                        String msg = ret != null ? ret.optString("MESSAGE", "") : "";
                        showStatus("Palette Error: " + msg, false);
                        etPalette.setText("");
                        etPalette.requestFocus();
                    }
                } catch (JSONException e) { showStatus("Parse error", false); }
            }
            @Override public void err(String e) { showStatus("Network: " + e, false); }
        });
    }

    private void save() {
        showProgress("Saving...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_PUTWAY_SAVE_DATA_RFC);
            p.put("IM_USER", USER);
            JSONObject row = new JSONObject();
            row.put("PLANT", WERKS);
            row.put("BIN", validatedBin);
            row.put("PALETTE", validatedPall);
            row.put("PO_NO", poNo);
            row.put("BILL_NO", billNo);
            JSONArray it = new JSONArray();
            it.put(row);
            p.put("IT_DATA", it);
        } catch (JSONException e) { dismissProgress(); return; }

        rfc(Vars.ZVND_PUTWAY_SAVE_DATA_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    totScannedBin++;
                    etTotScannedBin.setText(String.valueOf(totScannedBin));
                    showStatus("Saved! Palette " + validatedPall + " to BIN " + validatedBin, true);
                    partialReset();
                } else {
                    String msg = ret != null ? ret.optString("MESSAGE", "") : "";
                    showStatus("Save Error: " + msg, false);
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

    private void partialReset() {
        binValidated = false;
        validatedBin = "";
        validatedPall = "";
        etBin.setText("");
        etPalette.setText("");
        etBinDisplay.setText("");
        etPaletteDisplay.setText("");
        etBin.setEnabled(true);
        etPalette.setEnabled(false);
        etBin.requestFocus();
        showStatus("BIN cleared — Scan next BIN.", true);
    }

    private interface Cb { void ok(JSONObject r); void err(String e); }

    private void rfc(String name, JSONObject params, final Cb cb) {
        String base = URL.contains("/ValueXMW") ? URL.replace("/ValueXMW", "") : URL;
        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST,
                base + "/noacljsonrfcadaptor?bapiname=" + name + "&aclclientid=android", params,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) { dismissProgress(); cb.ok(r); }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    dismissProgress();
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
