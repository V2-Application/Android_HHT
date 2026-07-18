package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Screen 03 — HU Picking From BIN (GateLot2)
 * Flow: Select Picklist ({@link Vars#ZVND_GATELOT2_PICKLIST_VAL_RFC} — auto-fetches PO/INV)
 *       Scan BIN ({@link Vars#ZVND_GATELOT2_BIN_VAL_RFC})
 *       Scan Palette ({@link Vars#ZVND_GATELOT2_PALETTE_VAL_RFC})
 *       Auto Save ({@link Vars#ZVND_GATELOT2_SAVE_DATA_RFC})
 * @version 12.106
 */
public class FragmentHuPickingFromBin extends Fragment {

    private static final String TAG = "HuPickingFromBin";
    private static final String SPINNER_DEFAULT = "Select";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etDcSite, etBin, etBinDisplay, etPalette, etPaletteDisplay;
    private EditText etPo, etInv, etScanBinBox, etTotScannedBin;
    private Spinner spPicklist;

    private ArrayAdapter<String> picklistAdapter;
    private final List<String> picklistOptions = new ArrayList<>();
    private boolean ignoreSpinnerSelection = false;

    private String URL = "", USER = "", WERKS = "";
    private boolean plOk = false, binOk = false;
    private String vPl = "", vBin = "", vPall = "", poNo = "", invNo = "";
    private int totScannedBin = 0;

    public FragmentHuPickingFromBin() {}
    public static FragmentHuPickingFromBin newInstance() { return new FragmentHuPickingFromBin(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_hu_picking_from_bin, container, false);

        etDcSite         = view.findViewById(R.id.et_dc_site);
        spPicklist       = view.findViewById(R.id.sp_picklist);
        etBin            = view.findViewById(R.id.et_bin);
        etBinDisplay     = view.findViewById(R.id.et_bin_display);
        etPalette        = view.findViewById(R.id.et_palette);
        etPaletteDisplay = view.findViewById(R.id.et_palette_display);
        etPo             = view.findViewById(R.id.tv_po);
        etInv            = view.findViewById(R.id.tv_inv);
        etScanBinBox     = view.findViewById(R.id.et_scan_bin_box);
        etTotScannedBin  = view.findViewById(R.id.et_tot_scanned_bin);

        etBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) {
                if (!plOk) { showStatus("Select Picklist first.", false); return true; }
                String b = etBin.getText().toString().trim();
                if (!b.isEmpty()) validateBin(b);
                return true;
            }
        });
        etPalette.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) {
                if (!binOk) { showStatus("Scan BIN first.", false); return true; }
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
                    .setActionBarTitle("HU PICKING FROM BIN");
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
        setupPicklistSpinner();
        resetScanFields();
        loadPicklistList();
        showStatus("Select Picklist No.", true);
    }

    private void setupPicklistSpinner() {
        picklistOptions.clear();
        picklistOptions.add(SPINNER_DEFAULT);
        picklistAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, picklistOptions);
        picklistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPicklist.setAdapter(picklistAdapter);
        spPicklist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (ignoreSpinnerSelection || position <= 0) {
                    return;
                }
                validatePicklist(picklistOptions.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadPicklistList() {
        if (TextUtils.isEmpty(WERKS)) {
            showStatus("DC Site not found.", false);
            return;
        }

        showProgress("Loading picklists...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_GATELOT2_PICKLIST_VAL_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
        } catch (JSONException e) {
            dismissProgress();
            return;
        }

        rfc(Vars.ZVND_GATELOT2_PICKLIST_VAL_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                Set<String> picklists = new LinkedHashSet<>();
                JSONArray et = r.optJSONArray("ET_DATA");
                if (et != null) {
                    for (int i = 0; i < et.length(); i++) {
                        JSONObject row = et.optJSONObject(i);
                        if (row == null) {
                            continue;
                        }
                        String pl = row.optString("ZPICK_NO", "").trim();
                        if (pl.isEmpty()) {
                            pl = row.optString("PICKLIST", "").trim();
                        }
                        if (pl.isEmpty()) {
                            pl = row.optString("PICKLIST_NO", "").trim();
                        }
                        if (!pl.isEmpty()) {
                            picklists.add(pl);
                        }
                    }
                }

                ignoreSpinnerSelection = true;
                picklistOptions.clear();
                picklistOptions.add(SPINNER_DEFAULT);
                picklistOptions.addAll(picklists);
                picklistAdapter.notifyDataSetChanged();
                spPicklist.setSelection(0);
                ignoreSpinnerSelection = false;

                if (picklists.isEmpty()) {
                    showStatus("No open picklists found.", false);
                } else {
                    showStatus("Select Picklist No.", true);
                }
            }

            @Override public void err(String e) {
                showStatus("Failed to load picklists: " + e, false);
            }
        });
    }

    private void validatePicklist(final String pl) {
        showProgress("Validating Picklist...");
        resetScanFields();
        plOk = false;
        binOk = false;
        vPl = "";
        poNo = "";
        invNo = "";

        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_GATELOT2_PICKLIST_VAL_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_PICKLIST", pl);
        } catch (JSONException e) {
            dismissProgress();
            return;
        }

        rfc(Vars.ZVND_GATELOT2_PICKLIST_VAL_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                try {
                    JSONObject ret = r.optJSONObject("EX_RETURN");
                    String type = ret != null ? ret.optString("TYPE", "") : "";
                    if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                        plOk = true;
                        vPl = pl;
                        JSONArray et = r.optJSONArray("ET_DATA");
                        if (et != null && et.length() > 0) {
                            JSONObject row = et.getJSONObject(0);
                            poNo = row.optString("VPONO", "");
                            if (poNo.isEmpty()) {
                                poNo = row.optString("PO_NO", "");
                            }
                            invNo = row.optString("INVNO", "");
                            if (invNo.isEmpty()) {
                                invNo = row.optString("INV_NO", "");
                            }
                            if (invNo.isEmpty()) {
                                invNo = row.optString("BILL_NO", "");
                            }
                            String sq = row.optString("SQ", "0");
                            String tsq = row.optString("TSQ", "0");
                            etScanBinBox.setText(sq + " / " + tsq);
                        }
                        etPo.setText(poNo);
                        etInv.setText(invNo);
                        etBin.setEnabled(true);
                        etBin.requestFocus();
                        showStatus("Picklist OK — Scan BIN.", true);
                    } else {
                        String msg = ret != null ? ret.optString("MESSAGE", "") : "";
                        showStatus("Picklist Error: " + msg, false);
                        ignoreSpinnerSelection = true;
                        spPicklist.setSelection(0);
                        ignoreSpinnerSelection = false;
                    }
                } catch (JSONException e) {
                    showStatus("Parse error", false);
                }
            }

            @Override public void err(String e) {
                showStatus("Network: " + e, false);
            }
        });
    }

    private void validateBin(final String bin) {
        showProgress("Validating BIN...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_GATELOT2_BIN_VAL_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_PICKLIST", vPl);
            p.put("IM_BIN", bin);
        } catch (JSONException e) {
            dismissProgress();
            return;
        }

        rfc(Vars.ZVND_GATELOT2_BIN_VAL_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    binOk = true;
                    vBin = bin;
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

            @Override public void err(String e) {
                showStatus("Network: " + e, false);
            }
        });
    }

    private void validatePalette(final String palette) {
        showProgress("Validating Palette...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_GATELOT2_PALETTE_VAL_RFC);
            p.put("IM_USER", USER);
            p.put("IM_PLANT", WERKS);
            p.put("IM_PICKLIST", vPl);
            p.put("IM_BIN", vBin);
            p.put("IM_PALL", palette);
        } catch (JSONException e) {
            dismissProgress();
            return;
        }

        rfc(Vars.ZVND_GATELOT2_PALETTE_VAL_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    vPall = palette;
                    etPaletteDisplay.setText(palette);
                    etPalette.setEnabled(false);
                    showStatus("Palette OK — Saving...", true);
                    save();
                } else {
                    String msg = ret != null ? ret.optString("MESSAGE", "") : "";
                    showStatus("Palette Error: " + msg, false);
                    etPalette.setText("");
                    etPalette.requestFocus();
                }
            }

            @Override public void err(String e) {
                showStatus("Network: " + e, false);
            }
        });
    }

    private void save() {
        showProgress("Saving...");
        JSONObject p = new JSONObject();
        try {
            p.put("bapiname", Vars.ZVND_GATELOT2_SAVE_DATA_RFC);
            p.put("IM_USER", USER);
            JSONObject row = new JSONObject();
            row.put("WERKS", WERKS);
            row.put("ZPICK_NO", vPl);
            row.put("LGPLA", vBin);
            row.put("PALETTE", vPall);
            JSONArray it = new JSONArray();
            it.put(row);
            p.put("IT_DATA", it);
        } catch (JSONException e) {
            dismissProgress();
            return;
        }

        rfc(Vars.ZVND_GATELOT2_SAVE_DATA_RFC, p, new Cb() {
            @Override public void ok(JSONObject r) {
                JSONObject ret = r.optJSONObject("EX_RETURN");
                String type = ret != null ? ret.optString("TYPE", "") : "";
                if ("S".equalsIgnoreCase(type) || type.isEmpty()) {
                    totScannedBin++;
                    etTotScannedBin.setText(String.valueOf(totScannedBin));
                    showStatus("Saved! Palette " + vPall + " from BIN " + vBin, true);
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

    private void resetScanFields() {
        binOk = false;
        vBin = "";
        vPall = "";
        etBin.setText("");
        etPalette.setText("");
        etBinDisplay.setText("");
        etPaletteDisplay.setText("");
        etBin.setEnabled(false);
        etPalette.setEnabled(false);
    }

    private void partialReset() {
        binOk = false;
        vBin = "";
        vPall = "";
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

    private void rfc(final String name, JSONObject params, final Cb cb) {
        String base = URL.contains("/ValueXMW") ? URL.replace("/ValueXMW", "") : URL;
        String url = base + "/noacljsonrfcadaptor?bapiname=" + name + "&aclclientid=android";
        Log.d(TAG, "RFC [" + name + "] URL -> " + url);
        Log.d(TAG, "RFC [" + name + "] REQUEST -> " + params.toString());
        JsonObjectRequest req = new SapJsonObjectRequest(Request.Method.POST,
                url, params,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject r) {
                    Log.d(TAG, "RFC [" + name + "] RESPONSE -> " + r.toString());
                    dismissProgress();
                    cb.ok(r);
                }
            },
            new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError e) {
                    Log.e(TAG, "RFC [" + name + "] ERROR -> " + e.toString(), e);
                    if (e.networkResponse != null) {
                        Log.e(TAG, "RFC [" + name + "] HTTP " + e.networkResponse.statusCode
                                + " BODY -> " + new String(e.networkResponse.data));
                    }
                    dismissProgress();
                    cb.err(e.getMessage() != null ? e.getMessage() : "Network error");
                }
            });
        req.setRetryPolicy(new DefaultRetryPolicy(90000, 0, 1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    private void showStatus(String msg, boolean ok) {
        if (activity == null || msg == null || msg.trim().isEmpty()) {
            return;
        }
        Toast toast = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
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
