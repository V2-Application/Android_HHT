package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.v2retail.ApplicationController;
import com.v2retail.commons.GatewayUrls;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FragmentGrtHuCreationPrint extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentGrtHuCreationPrint.class.getName();
    private static final int REQUEST_VALIDATE_HU = 1701;
    private static final int REQUEST_VALIDATE_PLANT = 1702;
    private static final int REQUEST_SAVE_HU = 1703;
    private static final int REQUEST_PRINT_HU = 1704;

    View rootView;
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    SharedPreferencesData data;

    String URL = "";
    String USER = "";
    String WERKS = "";

    Button btn_reset, btn_save;
    EditText txt_printer, txt_scan_hu, txt_scan_dplant;

    String tvsprinter;
    String validatedHu = "";
    String validatedPlant = "";
    String lastSavedHu = "";
    boolean requestInFlight = false;
    boolean continueAfterHuValidate = false;
    boolean printAfterPlantValidate = false;

    public FragmentGrtHuCreationPrint() {
    }

    public static FragmentGrtHuCreationPrint newInstance() {
        return new FragmentGrtHuCreationPrint();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("GRT HU Creation Print");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_hu_creation_print, container, false);
        con = getContext();
        box = new AlertBox(con);
        data = new SharedPreferencesData(con);
        URL = data.read("URL");
        USER = data.read("USER");
        WERKS = data.read("WERKS");

        txt_printer = rootView.findViewById(R.id.txt_grt_hu_creation_printer);
        txt_scan_hu = rootView.findViewById(R.id.txt_grt_hu_creation_scan_hu);
        txt_scan_dplant = rootView.findViewById(R.id.txt_grt_hu_creation_scan_dplant);

        btn_reset = rootView.findViewById(R.id.btn_grt_hu_creation_reset);
        btn_save = rootView.findViewById(R.id.btn_grt_hu_creation_save);

        btn_reset.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        loadSavedPrinter();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_grt_hu_creation_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                });
                break;
            case R.id.btn_grt_hu_creation_save:
                save();
                break;
        }
    }

    private void addInputEvents() {
        bindScanField(txt_printer, () -> {
            String value = UIFuncs.toUpperTrim(txt_printer);
            if (!value.isEmpty()) {
                validatePrinter(value, false);
            }
        });
        bindScanField(txt_scan_hu, () -> {
            String value = UIFuncs.toUpperTrim(txt_scan_hu);
            if (value.isEmpty()) {
                return;
            }
            UIFuncs.hideKeyboard(getActivity());
            validateHu(false);
        });
        bindScanField(txt_scan_dplant, () -> {
            String value = UIFuncs.toUpperTrim(txt_scan_dplant);
            if (value.isEmpty()) {
                return;
            }
            UIFuncs.hideKeyboard(getActivity());
            validatePlant(false);
        });
    }

    private void bindScanField(EditText field, Runnable onScan) {
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    onScan.run();
                    return true;
                }
                return false;
            }
        });
        field.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().toUpperCase().trim().isEmpty() && scannerReading) {
                    scannerReading = false;
                    onScan.run();
                }
            }
        });
    }

    private void loadSavedPrinter() {
        String defaultPrinter = data.read(Vars.TVS_PRINTER);
        if (defaultPrinter != null && !defaultPrinter.isEmpty()) {
            txt_printer.setText(defaultPrinter);
            validatePrinter(defaultPrinter, true);
        } else {
            UIFuncs.disableInput(con, txt_scan_hu);
            UIFuncs.disableInput(con, txt_scan_dplant);
            txt_printer.requestFocus();
        }
    }

    private void validatePrinter(String printerName, boolean isSilentCheck) {
        TSPLPrinter printerHelper = new TSPLPrinter(con);
        if (!printerHelper.findBluetoothPrinter(printerName, false)) {
            if (!isSilentCheck) {
                box.getBox("Not Paired", "Scanned printer ( " + printerName + " ) is not paired with this device.");
            }
            this.tvsprinter = null;
            txt_printer.setText("");
            txt_printer.requestFocus();
            UIFuncs.disableInput(con, txt_scan_hu);
            UIFuncs.disableInput(con, txt_scan_dplant);
            return;
        }
        data.write(Vars.TVS_PRINTER, printerName);
        this.tvsprinter = printerName;
        UIFuncs.enableInput(con, txt_scan_hu);
        UIFuncs.disableInput(con, txt_scan_dplant);
    }

    private void clear() {
        validatedHu = "";
        validatedPlant = "";
        lastSavedHu = "";
        continueAfterHuValidate = false;
        printAfterPlantValidate = false;
        txt_scan_hu.setText("");
        txt_scan_dplant.setText("");
        UIFuncs.disableInput(con, txt_scan_dplant);
        if (tvsprinter != null && !tvsprinter.isEmpty()) {
            UIFuncs.enableInput(con, txt_scan_hu);
            txt_scan_hu.requestFocus();
        } else {
            UIFuncs.disableInput(con, txt_scan_hu);
            txt_printer.requestFocus();
        }
    }

    private void save() {
        String hu = UIFuncs.toUpperTrim(txt_scan_hu);
        if (hu.isEmpty()) {
            box.getBox("Alert", "Please scan HU No");
            UIFuncs.enableInput(con, txt_scan_hu);
            return;
        }
        if (!hu.equals(validatedHu)) {
            validateHu(true);
            return;
        }
        String dplant = UIFuncs.toUpperTrim(txt_scan_dplant);
        if (dplant.isEmpty()) {
            box.getBox("Alert", "Please scan D. Plant");
            UIFuncs.enableInput(con, txt_scan_dplant);
            return;
        }
        if (!dplant.equals(validatedPlant)) {
            validatePlant(true);
            return;
        }
        if (hu.equals(lastSavedHu)) {
            printHu(hu);
            return;
        }
        submitSave();
    }

    private void validateHu(boolean continueAfterSuccess) {
        if (requestInFlight) {
            return;
        }
        String hu = UIFuncs.toUpperTrim(txt_scan_hu);
        if (hu.isEmpty()) {
            box.getBox("Alert", "Please scan HU No");
            UIFuncs.enableInput(con, txt_scan_hu);
            return;
        }

        continueAfterHuValidate = continueAfterSuccess;
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_HU_VALIDATE);
            args.put("IM_HU", hu);
            showProcessingAndSubmit(Vars.ZWM_GRT_HU_VALIDATE, REQUEST_VALIDATE_HU, args);
        } catch (JSONException e) {
            continueAfterHuValidate = false;
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onHuValidated() {
        String hu = UIFuncs.toUpperTrim(txt_scan_hu);
        validatedHu = hu;
        UIFuncs.disableInput(con, txt_scan_hu);
        UIFuncs.enableInput(con, txt_scan_dplant);
        if (continueAfterHuValidate) {
            continueAfterHuValidate = false;
            save();
        }
    }

    private void validatePlant(boolean printAfterSuccess) {
        if (requestInFlight) {
            return;
        }
        String dplant = UIFuncs.toUpperTrim(txt_scan_dplant);
        if (dplant.isEmpty()) {
            box.getBox("Alert", "Please scan D. Plant");
            UIFuncs.enableInput(con, txt_scan_dplant);
            return;
        }

        printAfterPlantValidate = printAfterSuccess;
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_PLANT_VALIDATE);
            args.put("IM_PLANT", dplant);
            showProcessingAndSubmit(Vars.ZWM_GRT_PLANT_VALIDATE, REQUEST_VALIDATE_PLANT, args);
        } catch (JSONException e) {
            printAfterPlantValidate = false;
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onPlantValidated() {
        String dplant = UIFuncs.toUpperTrim(txt_scan_dplant);
        validatedPlant = dplant;
        UIFuncs.disableInput(con, txt_scan_dplant);
        if (printAfterPlantValidate) {
            printAfterPlantValidate = false;
            submitSave();
        }
    }

    private void submitSave() {
        if (requestInFlight) {
            return;
        }
        String printer = tvsprinter != null ? tvsprinter : UIFuncs.toUpperTrim(txt_printer);
        String hu = UIFuncs.toUpperTrim(txt_scan_hu);
        String dplant = UIFuncs.toUpperTrim(txt_scan_dplant);
        String splant = WERKS != null ? WERKS.trim().toUpperCase() : "";

        if (printer.isEmpty()) {
            box.getBox("Alert", "Please scan TVS printer");
            txt_printer.requestFocus();
            return;
        }
        if (hu.isEmpty()) {
            box.getBox("Alert", "Please scan HU No");
            UIFuncs.enableInput(con, txt_scan_hu);
            return;
        }
        if (splant.isEmpty()) {
            box.getBox("Alert", "Source plant missing. Please log in again.");
            return;
        }
        if (dplant.isEmpty()) {
            box.getBox("Alert", "Please scan D. Plant");
            UIFuncs.enableInput(con, txt_scan_dplant);
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_GRT_STORE_HU);
            args.put("IM_USER", USER);
            args.put("IM_EXIDV", hu);
            args.put("IM_SWERKS", splant);
            args.put("IM_DWERKS", dplant);
            showProcessingAndSubmit(Vars.ZWM_GRT_STORE_HU, REQUEST_SAVE_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void onSaveSuccess(JSONObject responsebody) {
        String hu = UIFuncs.toUpperTrim(txt_scan_hu);
        lastSavedHu = hu;
        printHu(hu);
    }

    private void printHu(String hu) {
        if (requestInFlight) {
            return;
        }
        if (hu == null || hu.isEmpty()) {
            box.getBox("Alert", "Please scan HU No");
            return;
        }
        if (tvsprinter == null || tvsprinter.isEmpty()) {
            box.getBox("Alert", "Please scan TVS printer");
            txt_printer.requestFocus();
            return;
        }

        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_TVS_HU_PRINT_2);
            args.put("IM_USER", USER);
            args.put("IM_EXIDV", hu);
            showProcessingAndSubmit(Vars.ZWM_PTL_TVS_HU_PRINT_2, REQUEST_PRINT_HU, args);
        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            box.getErrBox(e);
        }
    }

    private void sendToPrinter(JSONObject huObj) {
        TSPLPrinter printer = new TSPLPrinter(getContext(), Vars.PTL_NEW_MODULE_HU_CLOSE);
        printer.sendPrintCommandToBluetoothPrinter(this.tvsprinter, huObj, "1");
    }

    private void onPrintDataReceived(JSONObject responsebody) {
        JSONObject huObj = extractHuData(responsebody);
        if (huObj != null) {
            sendToPrinter(huObj);
            Toast.makeText(con, "Details sent to printer " + tvsprinter, Toast.LENGTH_SHORT).show();
            clear();
            return;
        }
        UIFuncs.errorSound(con);
        box.getBox("Err", "Print data not received for HU");
    }

    private static JSONObject extractHuData(JSONObject responsebody) {
        if (responsebody == null || !responsebody.has("EX_HUDATA") || responsebody.isNull("EX_HUDATA")) {
            return null;
        }
        try {
            Object raw = responsebody.get("EX_HUDATA");
            if (raw instanceof JSONObject) {
                JSONObject row = (JSONObject) raw;
                return SapJsonRows.isMetadataRow(row) ? null : row;
            }
            if (raw instanceof JSONArray) {
                JSONArray arr = (JSONArray) raw;
                int start = SapJsonRows.startIndex(arr);
                for (int i = start; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row != null && !SapJsonRows.isMetadataRow(row)) {
                        return row;
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    private void resetHuInput() {
        validatedHu = "";
        continueAfterHuValidate = false;
        txt_scan_hu.setText("");
        UIFuncs.enableInput(con, txt_scan_hu);
        UIFuncs.disableInput(con, txt_scan_dplant);
        txt_scan_dplant.setText("");
        validatedPlant = "";
        printAfterPlantValidate = false;
    }

    private void resetDplantInput() {
        validatedPlant = "";
        printAfterPlantValidate = false;
        txt_scan_dplant.setText("");
        UIFuncs.enableInput(con, txt_scan_dplant);
    }

    private void resetFailedInput(int request) {
        if (request == REQUEST_VALIDATE_HU) {
            resetHuInput();
        } else if (request == REQUEST_PRINT_HU) {
            // Save already succeeded; keep HU/plant so Save can retry print.
        } else {
            resetDplantInput();
        }
    }

    private boolean isReturnError(JSONObject responsebody) {
        JSONObject returnObj = extractReturn(responsebody);
        if (returnObj == null) {
            return false;
        }
        String type = returnObj.optString("TYPE", "").trim();
        return "E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type);
    }

    private String getReturnMessage(JSONObject responsebody) {
        JSONObject returnObj = extractReturn(responsebody);
        if (returnObj != null) {
            String message = returnObj.optString("MESSAGE", "").trim();
            if (!message.isEmpty()) {
                return message;
            }
        }
        return "Request failed.";
    }

    private static JSONObject extractReturn(JSONObject responsebody) {
        if (responsebody == null || !responsebody.has("EX_RETURN") || responsebody.isNull("EX_RETURN")) {
            return null;
        }
        try {
            Object raw = responsebody.get("EX_RETURN");
            if (raw instanceof JSONObject) {
                return (JSONObject) raw;
            }
            if (raw instanceof JSONArray) {
                JSONArray arr = (JSONArray) raw;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.optJSONObject(i);
                    if (row != null) {
                        return row;
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        requestInFlight = true;
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                requestInFlight = false;
                dismissDialog();
                box.getErrBox(e);
            }
        }, 500);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = GatewayUrls.noAclJsonRfcUrl(URL, rfc);
        if (url.isEmpty()) {
            requestInFlight = false;
            dismissDialog();
            UIFuncs.errorSound(con);
            box.getBox("Err", "Server URL missing. Please log in again.");
            return;
        }

        final JSONObject params = args;
        Log.d(TAG, "url -> " + url);
        Log.d(TAG, "payload -> " + params);

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        requestInFlight = false;
                        dismissDialog();
                        Log.d(TAG, "response -> " + responsebody);

                        if (responsebody == null || responsebody.length() == 0) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", "No response from Server");
                            resetFailedInput(request);
                            return;
                        }
                        if (isReturnError(responsebody)) {
                            UIFuncs.errorSound(con);
                            box.getBox("Err", getReturnMessage(responsebody));
                            resetFailedInput(request);
                            return;
                        }
                        if (request == REQUEST_VALIDATE_HU) {
                            onHuValidated();
                        } else if (request == REQUEST_VALIDATE_PLANT) {
                            onPlantValidated();
                        } else if (request == REQUEST_SAVE_HU) {
                            onSaveSuccess(responsebody);
                        } else if (request == REQUEST_PRINT_HU) {
                            onPrintDataReceived(responsebody);
                        }
                    }
                }, volleyErrorListener(request)) {
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
            public void retry(VolleyError error) {
            }
        });
        mRequestQueue.add(mJsonRequest);
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    Response.ErrorListener volleyErrorListener(int request) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestInFlight = false;
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
                dismissDialog();
                UIFuncs.errorSound(con);
                box.getBox("Err", err);
                resetFailedInput(request);
            }
        };
    }
}
