package com.v2retail.dotvik.dc.ptlnew.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PTL-GRT — Receive Pallet at Hub Sorting.
 * Pallet validate: {@link Vars#ZCOMBO_VALIDATE_PALLETE_REC} — IM_USER, IM_PALETTE → EX_RETURN.
 */
public class FragmentPTLGrtReceivePalletAtHubSorting extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentPTLGrtReceivePalletAtHubSorting.class.getSimpleName();
    private static final String ACTION_BAR_TITLE = "Receive pallate at hub sorting";
    private static final List<String> FLOOR_OPTIONS = Arrays.asList("0", "1", "2", "3", "4", "5");
    private static final int REQUEST_VALIDATE_PALLET = 5901;

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private ProgressDialog dialog;
    private String URL = "";
    private String USER = "";

    private Spinner ddFloor;
    private EditText txtScanPallet;
    private TextView txtMessage;
    private TableLayout scannedItems;
    private Button btnBack;

    private boolean floorSelected = false;
    private final List<String[]> rowsData = new ArrayList<>();

    public FragmentPTLGrtReceivePalletAtHubSorting() {
    }

    public static FragmentPTLGrtReceivePalletAtHubSorting newInstance() {
        return new FragmentPTLGrtReceivePalletAtHubSorting();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ptl_grt_receive_pallet_at_hub_sorting, container, false);
        con = requireContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        USER = data.read("USER");

        ddFloor = root.findViewById(R.id.dd_ptl_grt_receive_pallet_hub_sorting_floor);
        txtScanPallet = root.findViewById(R.id.txt_ptl_grt_receive_pallet_hub_sorting_pallet);
        txtMessage = root.findViewById(R.id.txt_ptl_grt_receive_pallet_hub_sorting_message);
        scannedItems = root.findViewById(R.id.table_ptl_grt_receive_pallet_hub_sorting_scanned_items);
        btnBack = root.findViewById(R.id.btn_ptl_grt_receive_pallet_hub_sorting_back);

        setupFloorDropdown();
        addPalletScanEvents();
        btnBack.setOnClickListener(this);
        resetScreen();

        return root;
    }

    private void setupFloorDropdown() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_list_item_1,
                FLOOR_OPTIONS);
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ddFloor.setAdapter(floorAdapter);
        ddFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                floorSelected = true;
                txtScanPallet.requestFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                floorSelected = false;
            }
        });
    }

    private void addPalletScanEvents() {
        txtScanPallet.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String scanned = UIFuncs.toUpperTrim(txtScanPallet);
                    if (!TextUtils.isEmpty(scanned)) {
                        requestPalletValidate(scanned);
                    }
                    return true;
                }
                return false;
            }
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

    private String getSelectedFloor() {
        Object selected = ddFloor.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void requestPalletValidate(String scannedPallet) {
        if (TextUtils.isEmpty(scannedPallet)) {
            return;
        }
        if (!floorSelected || TextUtils.isEmpty(getSelectedFloor())) {
            UIFuncs.errorSound(con);
            box.getBox("Validation", "Please select Floor Number first.");
            txtScanPallet.setText("");
            ddFloor.requestFocus();
            return;
        }
        rowsData.clear();
        generateHeader();
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZCOMBO_VALIDATE_PALLETE_REC);
            args.put("IM_USER", USER);
            args.put("IM_PALETTE", scannedPallet);
            showProcessingAndSubmit(Vars.ZCOMBO_VALIDATE_PALLETE_REC, REQUEST_VALIDATE_PALLET, args);
        } catch (JSONException e) {
            Log.e(TAG, "requestPalletValidate", e);
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

            displayMessage(message);

            if ("E".equals(type)) {
                UIFuncs.errorSound(con);
                box.getBox("Err", message);
                clearPalletFieldAndRefocus();
                return;
            }

            if (request == REQUEST_VALIDATE_PALLET) {
                addRow(responsebody);
                clearPalletFieldAndRefocus();
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleRfcResponse", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
    }

    private void displayMessage(String message) {
        if (txtMessage != null) {
            txtMessage.setText(message == null ? "" : message);
        }
    }

    private void clearPalletFieldAndRefocus() {
        txtScanPallet.setText("");
        txtScanPallet.requestFocus();
    }

    private void resetScreen() {
        displayMessage("");
        txtScanPallet.setText("");
        rowsData.clear();
        if (scannedItems != null) {
            scannedItems.removeAllViews();
        }
        if (ddFloor.getAdapter() != null && ddFloor.getAdapter().getCount() > 0) {
            ddFloor.setSelection(0);
            floorSelected = true;
        }
        txtScanPallet.post(() -> txtScanPallet.requestFocus());
    }

    private void generateHeader() {
        if (scannedItems == null) {
            return;
        }
        int headerTextSize = 20;
        scannedItems.removeAllViews();

        TextView headerSno = new TextView(con);
        TextView headerPalette = new TextView(con);
        TextView headerCrate = new TextView(con);

        headerSno.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        headerSno.setGravity(Gravity.CENTER);
        headerSno.setPadding(5, 5, 0, 5);
        headerSno.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSno.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSno.setText("S.No");

        headerPalette.setGravity(Gravity.CENTER);
        headerPalette.setPadding(0, 5, 0, 5);
        headerPalette.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerPalette.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerPalette.setText("Palette");

        headerCrate.setGravity(Gravity.CENTER);
        headerCrate.setPadding(0, 5, 0, 5);
        headerCrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerCrate.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerCrate.setText("Crate");

        TableRow tr = new TableRow(con);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(0, 0, 0, 0);
        tr.setLayoutParams(trParams);
        tr.addView(headerSno);
        tr.addView(headerPalette);
        tr.addView(headerCrate);
        scannedItems.addView(tr, trParams);
    }

    private void addRow(JSONObject responsebody) {
        if (scannedItems == null) {
            return;
        }
        int textSize = 15;
        try {
            if (!responsebody.has("ET_DATA")) {
                return;
            }
            JSONArray etDataArray = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = etDataArray.length();
            if (totalEtRecords <= 0) {
                return;
            }
            for (int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++) {
                JSONObject etRecord = etDataArray.getJSONObject(recordIndex);
                String[] data = new String[]{
                        etRecord.getString("PALETTE"),
                        etRecord.getString("CRATE"),
                        etRecord.getString("TANUM"),
                        etRecord.getString("MESSAGE")
                };
                rowsData.add(data);

                TextView tvSno = new TextView(con);
                tvSno.setText(String.valueOf(rowsData.size()));
                tvSno.setTextSize(textSize);
                tvSno.setPadding(5, 2, 0, 2);
                tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvPalette = new TextView(con);
                tvPalette.setText(etRecord.getString("PALETTE"));
                tvPalette.setTextSize(textSize);
                tvPalette.setPadding(5, 2, 0, 2);
                tvPalette.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TextView tvCrate = new TextView(con);
                tvCrate.setText(etRecord.getString("CRATE"));
                tvCrate.setTextSize(textSize);
                tvCrate.setPadding(5, 2, 0, 2);
                tvCrate.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                TableRow tr = new TableRow(con);
                TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT);
                trParams.setMargins(0, 0, 0, 0);
                tr.setLayoutParams(trParams);
                tr.setId(rowsData.size());
                tr.setTag(data);
                tr.addView(tvSno);
                tr.addView(tvPalette);
                tr.addView(tvCrate);
                scannedItems.addView(tr, trParams);
            }
        } catch (Exception e) {
            Log.e(TAG, "addRow", e);
            box.getErrBox(e);
            UIFuncs.errorSound(con);
        }
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
            UIFuncs.errorSound(con);
            displayMessage(err);
            box.getBox("Err", err);
            clearPalletFieldAndRefocus();
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle(ACTION_BAR_TITLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_ptl_grt_receive_pallet_hub_sorting_back) {
            fm.popBackStack();
        }
    }
}
