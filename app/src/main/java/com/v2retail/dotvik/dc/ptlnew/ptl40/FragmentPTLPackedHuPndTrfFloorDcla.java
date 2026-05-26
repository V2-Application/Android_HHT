package com.v2retail.dotvik.dc.ptlnew.ptl40;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * PTL 4.0 — Packed HU PND TRF Floor DCLA.
 * <ul>
 *   <li>Pallet scan: {@link Vars#ZWM_PTL_PALATE_V60_V61} — IM_USER, IM_PLANT, IM_PALETTE</li>
 *   <li>HU scan: {@link Vars#ZWM_PTL_HU_V6LIDATE_RFC} — IM_USER, IM_PLANT, IM_HU → EX_HUB, EX_STORE</li>
 *   <li>Save: {@link Vars#ZWM_PTL_HU_V60_V61} — IM_USER, IM_PLANT, IM_PALETTE</li>
 * </ul>
 */
public class FragmentPTLPackedHuPndTrfFloorDcla extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentPTLPackedHuPndTrfFloorDcla.class.getSimpleName();
    private static final String ACTION_BAR_TITLE = "Packed HU PND TRF Floor DCLA";
    private static final int REQUEST_VALIDATE_PALLET = 5601;
    private static final int REQUEST_VALIDATE_HU = 5602;
    private static final int REQUEST_SAVE = 5603;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText txtScanPallet;
    private EditText txtPallet;
    private EditText txtScanHu;
    private EditText txtHu;
    private EditText txtHub;
    private EditText txtStore;
    private EditText txtNoOfHu;
    private Button btnBack;
    private Button btnSave;

    private String validatedPallet = "";
    private String validatedHu = "";

    public FragmentPTLPackedHuPndTrfFloorDcla() {
    }

    public static FragmentPTLPackedHuPndTrfFloorDcla newInstance() {
        return new FragmentPTLPackedHuPndTrfFloorDcla();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle(ACTION_BAR_TITLE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ptl_packed_hu_pnd_trf_floor_dcla, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtScanPallet = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_scan_pallet);
        txtPallet = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_pallet);
        txtScanHu = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_scan_hu);
        txtHu = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_hu);
        txtHub = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_hub);
        txtStore = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_store);
        txtNoOfHu = root.findViewById(R.id.txt_ptl_packed_hu_pnd_trf_floor_dcla_no_of_hu);
        btnBack = root.findViewById(R.id.btn_ptl_packed_hu_pnd_trf_floor_dcla_back);
        btnSave = root.findViewById(R.id.btn_ptl_packed_hu_pnd_trf_floor_dcla_save);

        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        addScanPalletEvents();
        addScanHuEvents();
        resetScreen();
        return root;
    }

    private void addScanPalletEvents() {
        txtScanPallet.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtScanPallet);
                if (!TextUtils.isEmpty(scanned)) {
                    requestPalletValidate(scanned);
                }
                return true;
            }
            return false;
        });

        txtScanPallet.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    requestPalletValidate(value);
                }
            }
        });
    }

    private void addScanHuEvents() {
        txtScanHu.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                UIFuncs.hideKeyboard(getActivity());
                String scanned = UIFuncs.toUpperTrim(txtScanHu);
                if (!TextUtils.isEmpty(scanned)) {
                    requestHuValidate(scanned);
                }
                return true;
            }
            return false;
        });

        txtScanHu.addTextChangedListener(new TextWatcher() {
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
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    requestHuValidate(value);
                }
            }
        });
    }

    private void requestPalletValidate(String scannedPallet) {
        if (TextUtils.isEmpty(scannedPallet)) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_PALATE_V60_V61);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_PALETTE", scannedPallet);
            showProcessingAndSubmit(Vars.ZWM_PTL_PALATE_V60_V61, REQUEST_VALIDATE_PALLET, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestPalletValidate", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void requestHuValidate(String scannedHu) {
        if (TextUtils.isEmpty(validatedPallet)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate pallet first.");
            txtScanPallet.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(scannedHu)) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HU_V6LIDATE_RFC);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_HU", scannedHu);
            showProcessingAndSubmit(Vars.ZWM_PTL_HU_V6LIDATE_RFC, REQUEST_VALIDATE_HU, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestHuValidate", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void requestSave() {
        if (TextUtils.isEmpty(validatedPallet)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate pallet first.");
            txtScanPallet.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(validatedHu)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate HU first.");
            txtScanHu.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HU_V60_V61);
            args.put("IM_USER", USER);
            args.put("IM_PLANT", WERKS);
            args.put("IM_PALETTE", validatedPallet);
            showProcessingAndSubmit(Vars.ZWM_PTL_HU_V60_V61, REQUEST_SAVE, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestSave", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(() -> {
            try {
                submitRequest(rfc, request, args);
            } catch (Exception e) {
                dismissDialog();
                box.getErrBox(e);
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload -> " + params);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params,
                responsebody -> {
                    dismissDialog();
                    Log.d(TAG, "response -> " + responsebody);

                    if (responsebody == null) {
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "No response from Server");
                    } else if (responsebody.length() == 0) {
                        UIFuncs.errorSound(con);
                        box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    } else {
                        handleRfcResponse(responsebody, request);
                    }
                },
                volleyErrorListener()) {
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
                Response<JSONObject> res = super.parseNetworkResponse(response);
                Log.d(TAG, "Network response -> " + res);
                return res;
            }
        };

        jsonRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 1;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {
            }
        });
        queue.add(jsonRequest);
    }

    private void handleRfcResponse(JSONObject responsebody, int request) {
        try {
            if (!responsebody.has("EX_RETURN") || !(responsebody.get("EX_RETURN") instanceof JSONObject)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", "Invalid response (EX_RETURN missing)");
                return;
            }

            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
            String type = returnobj.optString("TYPE", "");
            String message = returnobj.optString("MESSAGE", "");

            if ("E".equals(type)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", message);
                if (request == REQUEST_VALIDATE_PALLET) {
                    clearAfterPalletValidateFailure();
                } else if (request == REQUEST_VALIDATE_HU) {
                    clearAfterHuValidateFailure();
                }
                return;
            }

            if (request == REQUEST_VALIDATE_PALLET) {
                applyPalletValidateSuccess(responsebody);
                txtScanPallet.setText("");
                UIFuncs.disableInput(con, txtScanPallet);
                UIFuncs.enableInput(con, txtScanHu);
                txtScanHu.requestFocus();
            } else if (request == REQUEST_VALIDATE_HU) {
                applyHuValidateSuccess(responsebody);
                txtScanHu.setText("");
                txtScanHu.requestFocus();
            } else if (request == REQUEST_SAVE) {
                box.getBox("Ok", TextUtils.isEmpty(message) ? "Saved" : message,
                        (d, w) -> resetScreen());
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleRfcResponse", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void applyPalletValidateSuccess(JSONObject responsebody) {
        String pallet = firstNonEmpty(
                responsebody.optString("EX_PALATE", "").trim(),
                responsebody.optString("EX_PALETTE", "").trim(),
                UIFuncs.toUpperTrim(txtScanPallet));
        validatedPallet = pallet;
        txtPallet.setText(pallet);

        String hub = firstNonEmpty(
                responsebody.optString("EX_HUB", "").trim(),
                responsebody.optString("HUB", "").trim());
        if (!TextUtils.isEmpty(hub)) {
            txtHub.setText(hub);
        }

        String noOfHu = firstNonEmpty(
                responsebody.optString("EX_NO_OF_HU", "").trim(),
                responsebody.optString("NO_OF_HU", "").trim(),
                responsebody.optString("EX_HU_COUNT", "").trim(),
                responsebody.optString("EX_COUNT", "").trim());
        if (!TextUtils.isEmpty(noOfHu)) {
            txtNoOfHu.setText(UIFuncs.removeLeadingZeros(noOfHu));
        }
    }

    private void applyHuValidateSuccess(JSONObject responsebody) {
        String hu = firstNonEmpty(
                responsebody.optString("EX_HU", "").trim(),
                responsebody.optString("HU", "").trim(),
                UIFuncs.toUpperTrim(txtScanHu));
        validatedHu = hu;
        txtHu.setText(hu);

        String hub = firstNonEmpty(
                responsebody.optString("EX_HUB", "").trim(),
                responsebody.optString("HUB", "").trim());
        if (!TextUtils.isEmpty(hub)) {
            txtHub.setText(hub);
        }

        String store = firstNonEmpty(
                responsebody.optString("EX_STORE", "").trim(),
                responsebody.optString("STORE", "").trim());
        if (!TextUtils.isEmpty(store)) {
            txtStore.setText(store);
        }
    }

    private void clearAfterPalletValidateFailure() {
        validatedPallet = "";
        txtPallet.setText("");
        txtHub.setText("");
        txtNoOfHu.setText("");
        txtScanPallet.requestFocus();
    }

    private void clearAfterHuValidateFailure() {
        validatedHu = "";
        txtHu.setText("");
        txtStore.setText("");
        txtScanHu.requestFocus();
    }

    private void resetScreen() {
        validatedPallet = "";
        validatedHu = "";
        txtScanPallet.setText("");
        txtPallet.setText("");
        txtScanHu.setText("");
        txtHu.setText("");
        txtHub.setText("");
        txtStore.setText("");
        txtNoOfHu.setText("");
        UIFuncs.enableInput(con, txtScanPallet);
        UIFuncs.disableInput(con, txtScanHu);
        txtScanPallet.post(() -> txtScanPallet.requestFocus());
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (!TextUtils.isEmpty(v)) {
                return v;
            }
        }
        return "";
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private Response.ErrorListener volleyErrorListener() {
        return error -> {
            Log.i(TAG, "Error :" + error);
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
            box.getBox("Err", err);
        };
    }

    private void confirmNavigateBack() {
        new AlertDialog.Builder(con)
                .setTitle("Alert")
                .setMessage("Do you want to go back.")
                .setCancelable(false)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (fm != null) {
                        fm.popBackStack();
                    }
                })
                .show();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_ptl_packed_hu_pnd_trf_floor_dcla_back) {
            confirmNavigateBack();
        } else if (id == R.id.btn_ptl_packed_hu_pnd_trf_floor_dcla_save) {
            requestSave();
        }
    }
}
