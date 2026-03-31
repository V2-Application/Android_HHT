package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HU Swap Print Process
 * Scans old HU barcode -> calls ZWM_HUSWAP RFC -> prints new HU label via TVS Bluetooth printer.
 * Running counter tracks how many HUs have been swapped in this session.
 *
 * @author V2 Retail Tech
 * @version 12.106
 */
public class FragmentHUSwapPrint extends Fragment implements View.OnClickListener {

    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    private static final String TAG = FragmentHUSwapPrint.class.getName();
    private static final int REQUEST_HU_SWAP = 5601;

    String URL;
    String WERKS;
    String USER;
    String tvsPrinter;
    String title;
    int scanCount = 0;

    EditText txt_printer;
    EditText txt_old_hu;
    TextView txt_count;
    TextView txt_last_new_hu;
    TextView txt_status;
    TextView txt_printer_status;   // NEW: printer connection badge
    Button btn_detect_printer;
    Button btn_reset;
    Button btn_back;

    SharedPreferencesData data;

    public FragmentHUSwapPrint() { }

    public static FragmentHUSwapPrint newInstance(String breadcrumb) {
        FragmentHUSwapPrint fragment = new FragmentHUSwapPrint();
        fragment.title = breadcrumb;
        return fragment;
    }

    @Override
    public void onResume() {
        String _srv = new com.v2retail.util.SharedPreferencesData(getContext()).read("URL");
        com.v2retail.util.PlantNames.load(_srv);
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .getSupportActionBar()
                .setTitle(UIFuncs.getSmallTitle(title + " > HU SWAP PRINT"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getActivity().getSupportFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_hu_swap_print, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        data = new SharedPreferencesData(con);

        URL   = data.read("URL");
        WERKS = data.read("WERKS");
        USER  = data.read("USER");

        txt_printer         = view.findViewById(R.id.txt_hu_swap_printer);
        txt_old_hu          = view.findViewById(R.id.txt_hu_swap_old_hu);
        txt_count           = view.findViewById(R.id.txt_hu_swap_count);
        txt_last_new_hu     = view.findViewById(R.id.txt_hu_swap_last_new_hu);
        txt_status          = view.findViewById(R.id.txt_hu_swap_status);
        txt_printer_status  = view.findViewById(R.id.txt_hu_swap_printer_status);
        btn_detect_printer  = view.findViewById(R.id.btn_hu_swap_detect_printer);
        btn_reset           = view.findViewById(R.id.btn_hu_swap_reset);
        btn_back            = view.findViewById(R.id.btn_hu_swap_back);

        btn_detect_printer.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_back.setOnClickListener(this);

        // Auto-detect previously used printer
        String savedPrinter = data.read(Vars.TVS_PRINTER);
        if (savedPrinter != null && savedPrinter.length() > 0) {
            TSPLPrinter helper = new TSPLPrinter(con);
            if (helper.findBluetoothPrinter(savedPrinter, false)) {
                tvsPrinter = savedPrinter;
                txt_printer.setText(savedPrinter);
                setPrinterConnected(savedPrinter);
            } else {
                txt_printer.requestFocus();
                setPrinterDisconnected();
                UIFuncs.disableInput(con, txt_old_hu);
            }
        } else {
            txt_printer.requestFocus();
            setPrinterDisconnected();
            UIFuncs.disableInput(con, txt_old_hu);
        }

        addInputListeners();
        updateCounter();
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_hu_swap_detect_printer:
                detectPrinter(UIFuncs.toUpperTrim(txt_printer));
                break;
            case R.id.btn_hu_swap_reset:
                box.getBox("Confirm", "Reset session counter? Are you sure?",
                        (d, i) -> resetSession(),
                        (d, i) -> { });
                break;
            case R.id.btn_hu_swap_back:
                box.confirmBack(fm, con);
                break;
        }
    }

    // ── Printer badge helpers ─────────────────────────────────────────────────

    private void setPrinterConnected(String printerName) {
        txt_printer_status.setText("● CONNECTED: " + printerName);
        txt_printer_status.setTextColor(0xFF1B8A4C);
        UIFuncs.enableInput(con, txt_old_hu);
        txt_old_hu.requestFocus();
    }

    private void setPrinterDisconnected() {
        txt_printer_status.setText("● NOT CONNECTED");
        txt_printer_status.setTextColor(0xFFB71C1C);
        UIFuncs.disableInput(con, txt_old_hu);
    }

    // ── Status card helpers ───────────────────────────────────────────────────

    private void showStatus(String message, boolean isSuccess) {
        txt_status.setVisibility(View.VISIBLE);
        txt_status.setText(message);
        if (isSuccess) {
            txt_status.setTextColor(0xFF065f46);
            txt_status.setBackgroundColor(0xFFE8F5E9);
        } else {
            txt_status.setTextColor(0xFF7F1D1D);
            txt_status.setBackgroundColor(0xFFFFEBEE);
        }
    }

    private void hideStatus() {
        txt_status.setVisibility(View.GONE);
        txt_status.setText("");
    }

    // ── Input listeners ───────────────────────────────────────────────────────

    private void addInputListeners() {
        // Printer field: scan or manual entry
        txt_printer.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                detectPrinter(UIFuncs.toUpperTrim(txt_printer));
                return true;
            }
            return false;
        });

        txt_printer.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                scannerReading = (b == 0 && st == 0 && c > 3);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && scannerReading)
                    detectPrinter(s.toString().toUpperCase().trim());
            }
        });

        // Old HU field: fires on hardware scanner (text dump) or IME done
        txt_old_hu.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String hu = UIFuncs.toUpperTrim(txt_old_hu);
                if (!hu.isEmpty()) callHuSwap(hu);
                return true;
            }
            return false;
        });

        txt_old_hu.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                scannerReading = (b == 0 && st == 0 && c > 3);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && scannerReading)
                    callHuSwap(s.toString().toUpperCase().trim());
            }
        });
    }

    // ── Printer detection ─────────────────────────────────────────────────────

    private void detectPrinter(String printerName) {
        if (printerName == null || printerName.isEmpty()) {
            box.getBox("Error", "Please enter or scan a printer name.");
            return;
        }
        TSPLPrinter helper = new TSPLPrinter(con);
        if (!helper.findBluetoothPrinter(printerName, false)) {
            box.getBox("Not Paired",
                    "Printer \"" + printerName + "\" is not paired with this device.\nPair it in Bluetooth settings first.");
            tvsPrinter = null;
            txt_printer.setText("");
            txt_printer.requestFocus();
            setPrinterDisconnected();
            return;
        }
        tvsPrinter = printerName;
        data.write(Vars.TVS_PRINTER, printerName);
        txt_printer.setText(printerName);
        setPrinterConnected(printerName);
        showStatus("Printer ready: " + printerName, true);
    }

    // ── RFC call ──────────────────────────────────────────────────────────────

    private void callHuSwap(String oldHu) {
        if (tvsPrinter == null || tvsPrinter.isEmpty()) {
            box.getBox("Error", "Please connect a printer first.");
            return;
        }
        try {
            JSONObject args = new JSONObject();
            args.put("bapiname", Vars.ZWM_HUSWAP);
            args.put("IM_USER",  USER);
            args.put("IM_EXIDV", oldHu);
            showProcessingAndSubmit(Vars.ZWM_HUSWAP, REQUEST_HU_SWAP, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void handleSwapSuccess(JSONObject exData, String oldHu) {
        try {
            String newHu   = exData.optString("EXIDV_NEW", "");
            String vemng   = exData.optString("VEMNG_NEW", "");
            String crOn    = exData.optString("HU_CR_ON",  "");
            String crAt    = exData.optString("HU_CR_AT",  "");
            String source  = exData.optString("SOURCE",    "");
            String newDes  = exData.optString("NEW_DES",   "");

            if (newHu.isEmpty()) {
                UIFuncs.errorSound(con);
                box.getBox("Error", "RFC returned empty new HU number.");
                txt_old_hu.setText("");
                txt_old_hu.requestFocus();
                return;
            }

            // Print label
            TSPLPrinter printer = new TSPLPrinter(con, Vars.HU_SWAP_PROCESS);
            printer.sendHuSwapPrintCommand(tvsPrinter, oldHu, newHu, vemng, crOn, crAt, source, newDes);

            // Update counter and UI
            scanCount++;
            updateCounter();
            txt_last_new_hu.setText(oldHu + "  →  " + newHu);
            showStatus("✔ Printed: " + newHu + "   Qty: " + vemng, true);

            Toast.makeText(con, "Printed new HU: " + newHu, Toast.LENGTH_SHORT).show();

            // Clear and re-focus for next scan
            txt_old_hu.setText("");
            txt_old_hu.requestFocus();

        } catch (Exception e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    // ── Counter / reset ───────────────────────────────────────────────────────

    private void updateCounter() {
        txt_count.setText(String.valueOf(scanCount));
    }

    private void resetSession() {
        scanCount = 0;
        updateCounter();
        txt_last_new_hu.setText("—");
        hideStatus();
        txt_old_hu.setText("");
        txt_old_hu.requestFocus();
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private void showProcessingAndSubmit(String rfc, int reqCode, JSONObject args) {
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Fetching swap data...");
        dialog.setCancelable(false);
        dialog.show();
        new Handler().postDelayed(() -> {
            try {
                submitRequest(rfc, reqCode, args);
            } catch (Exception e) {
                if (dialog != null) { dialog.dismiss(); dialog = null; }
                new AlertBox(getContext()).getErrBox(e);
            }
        }, 800);
    }

    private void submitRequest(String rfc, int reqCode, JSONObject args) {
        String rfcUrl = URL.substring(0, URL.lastIndexOf("/"))
                + "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, rfcUrl, args,
                responsebody -> {
                    if (dialog != null) { dialog.dismiss(); dialog = null; }
                    Log.d(TAG, "response -> " + responsebody);

                    if (responsebody == null || responsebody.length() == 0) {
                        UIFuncs.errorSound(con);
                        new AlertBox(getContext()).getBox("Error", "Empty response from server.");
                        return;
                    }
                    try {
                        if (responsebody.has("EX_RETURN") &&
                                responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject ret = responsebody.getJSONObject("EX_RETURN");
                            String type = ret.optString("TYPE", "");
                            if ("E".equals(type) || "A".equals(type)) {
                                UIFuncs.errorSound(con);
                                showStatus("SAP: " + ret.optString("MESSAGE", "Unknown error."), false);
                                new AlertBox(getContext()).getBox("SAP Error",
                                        ret.optString("MESSAGE", "Unknown error."));
                                txt_old_hu.setText("");
                                txt_old_hu.requestFocus();
                                return;
                            }
                        }
                        if (reqCode == REQUEST_HU_SWAP) {
                            String scannedOldHu = args.optString("IM_EXIDV", "");
                            if (responsebody.has("EX_DATA") &&
                                    responsebody.get("EX_DATA") instanceof JSONObject) {
                                handleSwapSuccess(
                                        responsebody.getJSONObject("EX_DATA"), scannedOldHu);
                            } else {
                                UIFuncs.errorSound(con);
                                showStatus("No EX_DATA in RFC response.", false);
                                new AlertBox(getContext()).getBox("Error",
                                        "No EX_DATA in RFC response.");
                                txt_old_hu.setText("");
                                txt_old_hu.requestFocus();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        new AlertBox(getContext()).getErrBox(e);
                    }
                },
                volleyErrorListener()) {
            @Override public String getBodyContentType() { return "application/json"; }
            @Override public byte[] getBody() { return args.toString().getBytes(); }
            @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse r) {
                return super.parseNetworkResponse(r);
            }
        };
        req.setRetryPolicy(new RetryPolicy() {
            @Override public int getCurrentTimeout()   { return 50000; }
            @Override public int getCurrentRetryCount() { return 1; }
            @Override public void retry(VolleyError e) throws VolleyError { }
        });
        queue.add(req);
        Log.d(TAG, "RFC URL -> " + rfcUrl);
        Log.d(TAG, "Payload -> " + args);
    }

    private Response.ErrorListener volleyErrorListener() {
        return error -> {
            if (dialog != null) { dialog.dismiss(); dialog = null; }
            String msg;
            if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                msg = "Communication Error — check network.";
            } else if (error instanceof AuthFailureError) {
                msg = "Authentication Error.";
            } else if (error instanceof ServerError) {
                msg = "Server Side Error.";
            } else if (error instanceof NetworkError) {
                msg = "Network Error.";
            } else if (error instanceof ParseError) {
                msg = "Parse Error.";
            } else {
                msg = error.toString();
            }
            UIFuncs.errorSound(con);
            showStatus(msg, false);
            new AlertBox(getContext()).getBox("Error", msg);
        };
    }
}
