package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
import com.google.gson.Gson;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickData;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickSaveData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GRT_Pick_Crate_Bin#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRT_Pick_Crate_Bin extends Fragment implements View.OnClickListener {

    private static final int REQUEST_PICK_DATA = 202;
    private static final int REQUEST_SAVE_DATA = 203;
    private static final String TAG = GRT_Pick_Crate_Bin.class.getName();

    private static final String ARG_PICKLISTNO = "picklistno";
    private static final String ARG_SECTION = "section";

    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText text_picklist_no;
    EditText text_required_bin;
    EditText text_total_scanned_bin;
    EditText text_scan_bin_no;
    EditText text_crate_no;
    TableLayout table_picked_bin_crate;
    Button button_save;
    private String mPicklistno;
    private String mSection;

    private SortedMap<String, ETPickData> ET_PICK_DATA = new TreeMap<>();
    private Map<String, ETPickData> ET_SCANNED_PICK_DATA = new HashMap<>();

    public GRT_Pick_Crate_Bin() {
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Scan Bin");
    }

    public static GRT_Pick_Crate_Bin newInstance(String param1, String param2) {
        GRT_Pick_Crate_Bin fragment = new GRT_Pick_Crate_Bin();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPicklistno = getArguments().getString(ARG_PICKLISTNO);
            mSection = getArguments().getString(ARG_SECTION);
        }
        fm = getParentFragmentManager();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_grt_picklist_crate_bin, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        text_picklist_no = view.findViewById(R.id.text_picked_list_no);
        text_required_bin = view.findViewById(R.id.text_required_bin);
        text_total_scanned_bin = view.findViewById(R.id.text_scanned_bin_no);
        text_scan_bin_no = view.findViewById(R.id.text_scan_bin_no);
        text_crate_no = view.findViewById(R.id.text_crate_no);
        text_scan_bin_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  binno = text_scan_bin_no.getText().toString().toUpperCase();
                    if(binno.length()>0) {
                        text_total_scanned_bin.selectAll();
                        moveToSaveListAndRemoveRow(binno, "BIN");
                        return true;
                    }
                }
                return false;
            }
        });
        text_scan_bin_no.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String binno = s.toString().toUpperCase();
                if(binno.length()>0 && scannerReading) {
                    text_total_scanned_bin.selectAll();
                    moveToSaveListAndRemoveRow(binno, "BIN");
                }
            }
        });
        text_crate_no.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String crate = text_crate_no.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        moveToSaveListAndRemoveRow(crate, "CRATE");
                        return true;
                    }
                }
                return false;
            }
        });
        text_crate_no.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String crate = s.toString().toUpperCase();
                if(crate.length()>0 && scannerReading) {
                    moveToSaveListAndRemoveRow(crate, "CRATE");
                }
            }
        });
        button_save = view.findViewById(R.id.button_save_grt_picked_bin_crate);
        button_save.setOnClickListener(this);
        table_picked_bin_crate = view.findViewById(R.id.table_picked_bin_crate);
        text_picklist_no.setText(mPicklistno);
        text_required_bin.setText(mSection);
        getPickData();
        return view;
    }

    @Override
    public void onClick(View view) {
        CommonUtils.hideKeyboard(getActivity());
        switch (view.getId()){
            case R.id.button_save_grt_picked_bin_crate:
                validateFormAndSave();
                break;
        }
    }

    //CUSTOM FUNCTIONS
    private void validateFormAndSave(){
        if(ET_SCANNED_PICK_DATA.isEmpty()){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Invalid Input","Nothing to save. Please scan Bin and Crate");
            return;
        }
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.GRT_SAVE_PICK_DATA);
            args.put("IM_TANUM", mPicklistno);
            args.put("IM_USER", USER);

            JSONArray etData = new JSONArray();

            for (ETPickData ETData: ET_SCANNED_PICK_DATA.values()) {
                    ETPickSaveData SaveData = new ETPickSaveData();
                    SaveData.setLgbin(ETData.getLgbin());
                    SaveData.setLgplant(WERKS);
                    SaveData.setLgcrate(ETData.getLgcrate());
                    String ET_Data_JsonString = new Gson().toJson(SaveData);
                    JSONObject ET_DATA = new JSONObject(ET_Data_JsonString);
                    etData.put(ET_DATA);
                }

            if(etData.length() == 0){
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid Request", "Nothing to save. Please scan some articles");
                return;
            }

            args.put("IT_DATA", etData);
            showProcessingAndSubmit(Vars.GRT_SAVE_PICK_DATA,REQUEST_SAVE_DATA,args);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    private void getPickData(){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.GRT_GET_PICK_DATA);
            args.put("IM_TANUM", mPicklistno != null ? mPicklistno : "");
            args.put("IM_USER", USER);

            ET_PICK_DATA = new TreeMap<>();
            showProcessingAndSubmit(Vars.GRT_GET_PICK_DATA, REQUEST_PICK_DATA, args);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

                if (responsebody == null) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
                    return;
                } else {
                    try {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null) {
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {

                                        if (request == REQUEST_PICK_DATA) {
                                            SetPickData(responsebody);
                                        }
                                        else if (request == REQUEST_SAVE_DATA) {

                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    ET_SCANNED_PICK_DATA.clear();
                                                    getPickData();
                                                }
                                            });

                                        }
                                        return;
                                    }
                                }
                            }
                        }
                        if (request == REQUEST_PICK_DATA && responsebody.has("ET_PICKDATA")
                                && responsebody.get("ET_PICKDATA") instanceof JSONArray) {
                            SetPickData(responsebody);
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        AlertBox box = new AlertBox(getContext());
                        box.getErrBox(e);
                    }
                }
            }
        }, volleyErrorListener()) {
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
                Log.d(TAG, "Network response -> " + res.toString());

                return res;
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
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mJsonRequest);

        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);
        }
    }

    /**
     * Same pattern as {@link GRTCratePickingProcess}: SAP may send a dummy row at index 0, or only one data row.
     * Old {@code length - 1} + {@code recordIndex + 1} logic hid all lines when exactly one row was returned.
     */
    private static boolean isPickDataHeaderRow(JSONObject row) {
        if (row == null) return true;
        String bin = row.optString("BIN", "").trim();
        if ("BIN".equalsIgnoreCase(bin) || "STORAGE BIN".equalsIgnoreCase(bin)) return true;
        String matnr = row.optString("MATNR", "").trim();
        if ("MATNR".equalsIgnoreCase(matnr) && (bin.isEmpty() || "BIN".equalsIgnoreCase(bin))) return true;
        return false;
    }

    private static int pickDataStartIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() <= 1) return 0;
        return isPickDataHeaderRow(arr.getJSONObject(0)) ? 1 : 0;
    }

    private void SetPickData(JSONObject responsebody) {
        try {
            JSONArray ET_PICK_DATA_ARRAY = responsebody.getJSONArray("ET_PICKDATA");
            ET_PICK_DATA = new TreeMap<>();
            int start = pickDataStartIndex(ET_PICK_DATA_ARRAY);
            for (int i = start; i < ET_PICK_DATA_ARRAY.length(); i++) {
                JSONObject ET_RECORD = ET_PICK_DATA_ARRAY.getJSONObject(i);
                if (isPickDataHeaderRow(ET_RECORD)) continue;
                String bin = ET_RECORD.optString("BIN", "").trim();
                if (bin.isEmpty()) continue;

                ETPickData ET_DATA = new ETPickData();
                ET_DATA.setLgvbeln(ET_RECORD.optString("VBELN", ""));
                ET_DATA.setLgmatnr(ET_RECORD.optString("MATNR", ""));
                ET_DATA.setLgvgbel(ET_RECORD.optString("VGBEL", ""));
                ET_DATA.setLgmeins(ET_RECORD.optString("MEINS", ""));
                ET_DATA.setLgsammg(ET_RECORD.optString("SAMMG", ""));
                ET_DATA.setLgmaterial(ET_RECORD.optString("MATERIAL", ""));
                ET_DATA.setLgposnr(ET_RECORD.optString("POSNR", ""));
                ET_DATA.setLggnature(ET_RECORD.optString("GNATURE", ""));
                ET_DATA.setLgbin(bin);
                ET_DATA.setLgcrate(ET_RECORD.optString("CRATE", ""));
                ET_DATA.setLgmc_descr(ET_RECORD.optString("MC_DESCR", ""));
                ET_DATA.setLgurl(ET_RECORD.optString("URL", ""));
                ET_PICK_DATA.put(bin, ET_DATA);
            }
            if (ET_PICK_DATA.isEmpty()) {
                new AlertBox(getContext()).getBox("No data",
                        "No open pick lines for this transfer order.");
                return;
            }
            loadData();
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                String err = "";

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
                } else err = error.toString();

                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }


    public void loadData() {

        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 20;
        textSize = 15;
        int rows = ET_PICK_DATA.size();
        text_required_bin.setText(rows+"");
        text_total_scanned_bin.setText("0");
        text_scan_bin_no.setText("");
        table_picked_bin_crate.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerBin = new TextView(getContext());
        TextView headerCrate = new TextView(getContext());
        headerSno.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        headerSno.setGravity(Gravity.CENTER);
        headerSno.setPadding(5,5,0,5);
        headerSno.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSno.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSno.setText("S.No");

        headerBin.setGravity(Gravity.CENTER);
        headerBin.setPadding(0,5,0,5);
        headerBin.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerBin.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerBin.setText("Bin");

        headerCrate.setGravity(Gravity.CENTER);
        headerCrate.setPadding(0,5,0,5);
        headerCrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerCrate.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerCrate.setText("Crate");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerSno);
        tr.addView(headerBin);
        tr.addView(headerCrate);
        table_picked_bin_crate.addView(tr, trParams);

        //Create Data Rows in Table
        int rowNum = 1;
        for (Map.Entry<String,ETPickData> pickDataEntry :ET_PICK_DATA.entrySet()) {
            ETPickData data = pickDataEntry.getValue();
            TextView tvSno = new TextView(getContext());
            tvSno.setText(rowNum+"");
            tvSno.setTextSize(textSize);
            tvSno.setPadding(5,2,0,2);
            tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvBin = new TextView(getContext());
            tvBin.setText(data.getLgbin());
            tvBin.setTextSize(textSize);
            tvBin.setPadding(5,2,0,2);
            tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvCrate = new TextView(getContext());
            tvCrate.setText(data.getLgcrate());
            tvCrate.setTextSize(textSize);
            tvCrate.setPadding(5,2,0,2);
            tvCrate.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            tr = new TableRow(getContext());
            tr.setId(rowNum);
            tr.setPadding(0,0,0,0);
            tr.setLayoutParams(trParams);
            tr.addView(tvSno);
            tr.addView(tvBin);
            tr.addView(tvCrate);
            tr.setTag(data);
            table_picked_bin_crate.addView(tr, trParams);
            rowNum++;
        }
        text_scan_bin_no.requestFocus();
    }

    private void moveToSaveListAndRemoveRow(String bincrate, String mode){
        int totalRows = table_picked_bin_crate.getChildCount();
        boolean matchFound = false;
        for(int rowIndex=1;rowIndex < totalRows; rowIndex++){
            TableRow row = (TableRow) table_picked_bin_crate.getChildAt(rowIndex);
            ETPickData data = (ETPickData) row.getTag();
            if(data.getLgbin().equals(UIFuncs.toUpperTrim(text_scan_bin_no))){
                if(!mode.equalsIgnoreCase("BIN")){
                    if(data.getLgcrate().equalsIgnoreCase(UIFuncs.toUpperTrim(text_crate_no))){
                        ET_SCANNED_PICK_DATA.put(data.getLgbin(),data);
                        table_picked_bin_crate.removeView(row);
                    }else{
                        break;
                    }
                }
                matchFound = true;
                break;
            }
        }
        if(!matchFound){
            if(!mode.equalsIgnoreCase("BIN")) {
                UIFuncs.blinkEffectOnError(con, text_crate_no, true);
            }else{
                UIFuncs.blinkEffectOnError(con, text_scan_bin_no, true);
            }
            AlertBox box = new AlertBox(getContext());
            String message = mode.equals("BIN") ? "Bin No" : "Crate";
            box.getBox("Invalid Input", message + " ( "+bincrate+" ) is not in list.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!mode.equalsIgnoreCase("BIN")) {
                        text_crate_no.setText("");
                        text_crate_no.requestFocus();
                    }else{
                        UIFuncs.disableInput(con, text_crate_no);
                        text_scan_bin_no.setText("");
                        text_scan_bin_no.requestFocus();
                    }
                }
            });
        }else{
            text_crate_no.setText("");
            UIFuncs.enableInput(con, text_crate_no);
            if(!mode.equalsIgnoreCase("BIN")) {
                text_total_scanned_bin.setText(ET_SCANNED_PICK_DATA.size()+"");
                UIFuncs.disableInput(con, text_crate_no);
                text_scan_bin_no.setText("");
                text_scan_bin_no.requestFocus();
            }
        }
    }
}