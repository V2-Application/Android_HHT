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
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * PTL 4.0 — HU packing & weighing area per functional spec:
 * <ul>
 *   <li>Validate scan: {@link Vars#ZWM_PTL_HUPACKING_HU_VALIDATE} — IM_USER, IM_WERKS, IM_HU</li>
 *   <li>Bind export {@code EX_DATA} (table {@code ZWM_PTL_MSA_CRATE_TT} / structure): PLANT→STORE, HUB, QUANTITY→HU QTY, HU</li>
 *   <li>Save: {@link Vars#ZWM_PTL_HUPACKING_HU_SAVE} — IM_USER, IM_WERKS, IM_HU, IM_WEIGHT (optional; may be blank)</li>
 * </ul>
 */
public class FragmentPTLHUPackingWeighingArea extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentPTLHUPackingWeighingArea.class.getSimpleName();
    private static final int REQUEST_VALIDATE = 5511;
    private static final int REQUEST_SAVE = 5512;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText txtScanHu;
    private EditText txtHu;
    private EditText txtHuQty;
    private EditText txtHub;
    private EditText txtStore;
    private EditText txtWeight;
    private Button btnBack;
    private Button btnSave;

    /** HU confirmed by validate RFC; required for SAVE. */
    private String validatedHu = "";

    public FragmentPTLHUPackingWeighingArea() {
    }

    public static FragmentPTLHUPackingWeighingArea newInstance() {
        return new FragmentPTLHUPackingWeighingArea();
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
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("PTL HU PACKING & WEIGHING AREA");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ptl_hu_packing_weighing_area, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtScanHu = root.findViewById(R.id.txt_ptl_hu_packing_scan_hu);
        txtHu = root.findViewById(R.id.txt_ptl_hu_packing_hu);
        txtHuQty = root.findViewById(R.id.txt_ptl_hu_packing_hu_qty);
        txtHub = root.findViewById(R.id.txt_ptl_hu_packing_hub);
        txtStore = root.findViewById(R.id.txt_ptl_hu_packing_store);
        txtWeight = root.findViewById(R.id.txt_ptl_hu_packing_weight);
        btnBack = root.findViewById(R.id.btn_ptl_hu_packing_back);
        btnSave = root.findViewById(R.id.btn_ptl_hu_packing_save);

        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        txtScanHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String scanned = UIFuncs.toUpperTrim(txtScanHu);
                    if (!TextUtils.isEmpty(scanned)) {
                        requestHuValidate(scanned);
                    }
                    return true;
                }
                return false;
            }
        });

        txtScanHu.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (!value.isEmpty() && scannerReading) {
                    requestHuValidate(value);
                }
            }
        });

        root.post(this::focusScanHuField);
        return root;
    }

    /** Puts cursor in Scan HU (use after layout / after dialogs close). */
    private void focusScanHuField() {
        if (txtScanHu == null) {
            return;
        }
        txtScanHu.requestFocus();
    }

    private void focusScanHuFieldPosted() {
        if (txtScanHu == null) {
            return;
        }
        txtScanHu.post(this::focusScanHuField);
    }

    private void confirmNavigateBack() {
        new AlertDialog.Builder(con)
                .setTitle("Alert")
                .setMessage("Do you want to go back.")
                .setCancelable(false)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fm.popBackStack();
                    }
                })
                .show();
    }

    private void requestHuValidate(String scannedHu) {
        if (TextUtils.isEmpty(scannedHu)) {
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HUPACKING_HU_VALIDATE);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_HU", scannedHu);
            showProcessingAndSubmit(Vars.ZWM_PTL_HUPACKING_HU_VALIDATE, REQUEST_VALIDATE, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestHuValidate", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void requestSave() {
        if (TextUtils.isEmpty(validatedHu)) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please scan and validate HU first.");
            return;
        }
        String w = txtWeight.getText() != null ? txtWeight.getText().toString().trim() : "";
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_PTL_HUPACKING_HU_SAVE);
            // Spec PDF had typo "IM_USESR"; SAP / adaptor use IM_USER (same as other PTL RFCs).
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            args.put("IM_HU", validatedHu);
            args.put("IM_WEIGHT", w);
            showProcessingAndSubmit(Vars.ZWM_PTL_HUPACKING_HU_SAVE, REQUEST_SAVE, args);
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload -> " + params);

        RequestQueue queue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
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
            if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                String type = returnobj.optString("TYPE", "");
                String message = returnobj.optString("MESSAGE", "");

                if ("E".equals(type)) {
                    UIFuncs.errorSound(con);
                    box.getBox("Err", message);
                    if (request == REQUEST_VALIDATE) {
                        clearAfterValidateFailure();
                    }
                    return;
                }

                if (request == REQUEST_VALIDATE) {
                    applyValidateSuccess(responsebody);
                    txtScanHu.setText("");
                    // Spec: after successful HU validate, cursor moves to Weight.
                    txtWeight.requestFocus();
                } else if (request == REQUEST_SAVE) {
                    box.getBox("Ok", TextUtils.isEmpty(message) ? "Saved" : message, (d, w) -> refreshScreenAfterSave());
                }
            } else {
                UIFuncs.errorSound(con);
                box.getBox("Err", "Invalid response (EX_RETURN missing)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleRfcResponse", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    /**
     * Parses {@code EX_DATA} as {@link JSONObject} or first row of {@link JSONArray} (ZWM_PTL_MSA_CRATE_TT).
     * Maps PLANT→STORE, HUB→HUB, QUANTITY→HU QTY, HU→HU per spec.
     */
    private void applyValidateSuccess(JSONObject responsebody) throws JSONException {
        JSONObject row = extractExDataRow(responsebody);
        if (row == null) {
            String scanFallback = UIFuncs.toUpperTrim(txtScanHu);
            validatedHu = scanFallback;
            if (!TextUtils.isEmpty(scanFallback)) {
                txtHu.setText(scanFallback);
            }
            return;
        }

        String hu = row.optString("HU", "").trim();
        String plant = firstNonEmpty(
                row.optString("PLANT", "").trim(),
                row.optString("WERKS", "").trim());
        String hub = row.optString("HUB", "").trim();
        String qty = firstNonEmpty(
                row.optString("QUANTITY", "").trim(),
                row.optString("QTY", "").trim());

        txtHu.setText(hu);
        txtStore.setText(plant);
        txtHub.setText(hub);
        txtHuQty.setText(UIFuncs.removeLeadingZeros(qty));

        validatedHu = !TextUtils.isEmpty(hu) ? hu : UIFuncs.toUpperTrim(txtScanHu);
        if (TextUtils.isEmpty(validatedHu)) {
            validatedHu = UIFuncs.toUpperTrim(txtHu);
        }
    }

    @Nullable
    private static JSONObject extractExDataRow(JSONObject responsebody) throws JSONException {
        if (!responsebody.has("EX_DATA")) {
            return null;
        }
        Object raw = responsebody.get("EX_DATA");
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            if (arr.length() == 0) {
                return null;
            }
            Object first = arr.get(0);
            return first instanceof JSONObject ? (JSONObject) first : null;
        }
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        return null;
    }

    private static String firstNonEmpty(String a, String b) {
        if (!TextUtils.isEmpty(a)) {
            return a;
        }
        return b != null ? b : "";
    }

    private void clearAfterValidateFailure() {
        validatedHu = "";
        txtHu.setText("");
        txtHuQty.setText("");
        txtHub.setText("");
        txtStore.setText("");
        txtWeight.setText("");
        txtScanHu.requestFocus();
    }

    /** Spec: after save, screen auto-refreshes. */
    private void refreshScreenAfterSave() {
        validatedHu = "";
        txtScanHu.setText("");
        txtHu.setText("");
        txtHuQty.setText("");
        txtHub.setText("");
        txtStore.setText("");
        txtWeight.setText("");
        focusScanHuFieldPosted();
    }

    private Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
                if (dialog != null) {
                    dialog.dismiss();
                }
                box.getBox("Err", err);
            }
        };
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ptl_hu_packing_back) {
            confirmNavigateBack();
        } else if (id == R.id.btn_ptl_hu_packing_save) {
            requestSave();
        }
    }
}
