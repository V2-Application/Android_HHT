package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
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
import android.widget.LinearLayout;
import android.widget.RadioButton;
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
import com.v2retail.commons.DataHelper;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.ptl.PTLPicking;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickData;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickITTabc;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StoreEcommPicking#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreEcommPicking extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private final int REQUEST_PICK_STORE = 1028;
    private final int REQUEST_VALIDATE_BIN = 1029;
    private final int REQUEST_VALIDATE_MATNR = 1030;
    private final int REQUEST_SAVE = 1031;

    private static final String TAG = StoreEcommPicking.class.getName();

    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;
    RadioButton radio_001,radio_002;
    Button btn_ecomm_picking_next;
    TableLayout table_bin_material;
    EditText text_ecomm_picking_scan_bin,text_ecomm_picking_group_number, text_ecomm_picking_scan_material;
    LinearLayout ll_ecomm_picking_radio_group, ll_ecomm_picking_bin_number, ll_ecomm_picking_material_number, ll_ecomm_picking_btn_next, ll_ecomm_picking_btn_save;

    private String penqty;
    private String lgort;
    private String loc001;
    private String loc002;
    private String tottoqty;
    private String sammg;
    private String zzcrate;

    private Set<String> binlist = new TreeSet<>();
    private List<String> matnrlist = new ArrayList<>();
    private List<ETPickData> etPickData = new ArrayList<>();

    public StoreEcommPicking() {
        // Required empty public constructor
    }
    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Ecomm Picking");
    }
    public static StoreEcommPicking newInstance(String param1, String param2) {
        StoreEcommPicking fragment = new StoreEcommPicking();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_store_ecomm_picking, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        radio_001 = rootView.findViewById(R.id.radio_ecomm_picking_loc_001);
        radio_002 = rootView.findViewById(R.id.radio_ecomm_picking_loc_002);

        btn_ecomm_picking_next = rootView.findViewById(R.id.btn_ecomm_picking_next);
        text_ecomm_picking_scan_bin = rootView.findViewById(R.id.text_ecomm_picking_scan_bin);
        text_ecomm_picking_group_number = rootView.findViewById(R.id.text_ecomm_picking_group_number);
        text_ecomm_picking_scan_material = rootView.findViewById(R.id.text_ecomm_picking_scan_material);

        ll_ecomm_picking_radio_group = rootView.findViewById(R.id.ll_ecomm_picking_radio_group);
        ll_ecomm_picking_bin_number = rootView.findViewById(R.id.ll_ecomm_picking_bin_number);
        ll_ecomm_picking_btn_next = rootView.findViewById(R.id.ll_ecomm_picking_btn_next);
        ll_ecomm_picking_material_number = rootView.findViewById(R.id.ll_ecomm_picking_material_number);
        ll_ecomm_picking_btn_save = rootView.findViewById(R.id.ll_ecomm_picking_btn_save);

        table_bin_material = rootView.findViewById(R.id.table_ecomm_picking_bin_material);
        text_ecomm_picking_group_number.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  groupnumber = text_ecomm_picking_group_number.getText().toString().toUpperCase().trim();
                    if(groupnumber.length()>0) {
                        text_ecomm_picking_group_number.selectAll();
                        validateGroupNumber(groupnumber);
                        return true;
                    }
                }
                return false;
            }
        });
        text_ecomm_picking_group_number.addTextChangedListener(new TextWatcher() {
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
                String groupnumber = s.toString().toUpperCase().trim();
                if(groupnumber.length()>0 && scannerReading) {
                    text_ecomm_picking_group_number.selectAll();
                    validateGroupNumber(groupnumber);
                }
            }
        });

        text_ecomm_picking_scan_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String bin = text_ecomm_picking_scan_bin.getText().toString().toUpperCase().trim();
                    if(bin.length()>0) {
                        text_ecomm_picking_scan_bin.selectAll();
                        getBinData(bin);
                        return true;
                    }
                }
                return false;
            }
        });
        text_ecomm_picking_scan_bin.addTextChangedListener(new TextWatcher() {
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
                String bin = s.toString().toUpperCase().trim();
                if(bin.length()>0 && scannerReading) {
                    text_ecomm_picking_scan_bin.selectAll();
                    getBinData(bin);
                }
            }
        });
        text_ecomm_picking_scan_material.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String matnr = text_ecomm_picking_scan_material.getText().toString().toUpperCase().trim();
                    if(matnr.length()>0) {
                        text_ecomm_picking_scan_material.selectAll();
                        checkAndSendMatnr(matnr);
                        return true;
                    }
                }
                return false;
            }
        });
        text_ecomm_picking_scan_material.addTextChangedListener(new TextWatcher() {
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
                String matnr = s.toString().toUpperCase().trim();
                if(matnr.length()>0 && scannerReading) {
                    text_ecomm_picking_scan_material.selectAll();
                    checkAndSendMatnr(matnr);
                }
            }
        });
        setInitialState();
        return rootView;
    }
    private void setInitialState(){
        text_ecomm_picking_group_number.setText("");
        text_ecomm_picking_scan_bin.setText("");
        text_ecomm_picking_scan_bin.setEnabled(true);
        text_ecomm_picking_scan_material.setText("");
        text_ecomm_picking_scan_material.setEnabled(true);

        ll_ecomm_picking_radio_group.setVisibility(View.VISIBLE);
        radio_002.setChecked(true);
        ll_ecomm_picking_bin_number.setVisibility(View.GONE);
        ll_ecomm_picking_material_number.setVisibility(View.GONE);
        ll_ecomm_picking_btn_next.setVisibility(View.VISIBLE);
        ll_ecomm_picking_btn_save.setVisibility(View.GONE);

        table_bin_material.removeAllViews();

        text_ecomm_picking_group_number.setEnabled(true);
        text_ecomm_picking_group_number.requestFocus();

    }
    private void validateGroupNumber(String groupnumber){
        JSONObject args = new JSONObject();
        binlist = new TreeSet<>();
        etPickData = new ArrayList<>();
        table_bin_material.removeAllViews();
        try {
            args.put("bapiname", Vars.ZOMINI_PICK_STORE);
            args.put("IM_0002_LOC", radio_002.isChecked() ? "X":"");
            args.put("IM_0001_LOC", radio_001.isChecked() ? "X":"");
            args.put("IM_GRP_NO", groupnumber.trim().toUpperCase());
            args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.ZOMINI_PICK_STORE, REQUEST_PICK_STORE, args,null);

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

    private void getBinData(String bin){
        JSONObject args = new JSONObject();
        zzcrate = "";
        try {
            args.put("bapiname", Vars.ZOMINI_BIN_BARCODE_VALIDATE);
            args.put("IM_SCAN", bin.trim().toUpperCase(Locale.ROOT));
            args.put("IM_0002_LOC", loc002);
            args.put("IM_0001_LOC", loc001);
            args.put("IM_GRP_NO", sammg);
            args.put("IM_USER", USER);
            args.put("IM_BIN", "");
            showProcessingAndSubmit(Vars.ZOMINI_BIN_BARCODE_VALIDATE, REQUEST_VALIDATE_BIN, args,null);

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
    private void checkAndSendMatnr(String matnr){
        JSONObject args = new JSONObject();
        ETPickData pickData = checkInPickData(matnr,text_ecomm_picking_scan_bin.getText().toString().toUpperCase(Locale.ROOT).trim());
        if(pickData != null){
            try{
            JSONArray et_itTabc = new JSONArray();
            ETPickITTabc itTabc = new ETPickITTabc();
            itTabc.setLgcrate(zzcrate);
            itTabc.setLgmatnr(pickData.getLgmatnr());
            itTabc.setLggnature(pickData.getLggnature());
            itTabc.setLgsmmmg(sammg);
            itTabc.setLgmeins(pickData.getLgmeins());
            itTabc.setLgscanqty(1);
            itTabc.setLgposnr(Integer.parseInt(pickData.getLgposnr()));
            JSONObject IT_TABC = new JSONObject(new Gson().toJson(itTabc));
            et_itTabc.put(IT_TABC);
                args.put("bapiname", Vars.ZOMINI_BIN_BARCODE_VALIDATE);
                args.put("IM_SCAN", matnr);
                args.put("IM_0002_LOC", loc002);
                args.put("IM_0001_LOC", loc001);
                args.put("IM_GRP_NO", sammg);
                args.put("IM_USER", USER);
                args.put("IM_BIN", zzcrate);
                args.put("ET_ITTABC", et_itTabc);
                showProcessingAndSubmit(Vars.ZOMINI_BIN_BARCODE_VALIDATE, REQUEST_VALIDATE_MATNR, args,pickData);

            } catch (JSONException e) {
                e.printStackTrace();
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }
                UIFuncs.errorSound(con);
                AlertBox box = new AlertBox(getContext());
                box.getErrBox(e);
            }
        }else{
            UIFuncs.errorSound(con);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", "MATNR not found in PICK DATA");
        }

    }
    private ETPickData checkInPickData(String matnr, String bin){
        ETPickData fpickData = null;
        for (ETPickData pickData:etPickData) {
            if (pickData.getLgmatnr().equals(matnr) && pickData.getLgbin().equals(bin)) {
                fpickData = pickData;
                break;
            }
        }
        return fpickData;
    }
    public void showProcessingAndSubmit(String rfc, int request, JSONObject args, ETPickData targetEtData){

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args,targetEtData);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }
    private void submitRequest(String rfc, int request, JSONObject args, ETPickData targetEtData){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }
                Log.d(TAG, "response ->" + responsebody);

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
                                        if (request == REQUEST_PICK_STORE) {
                                            text_ecomm_picking_group_number.setText("");
                                            text_ecomm_picking_group_number.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_BIN){
                                            text_ecomm_picking_scan_bin.setText("");
                                            text_ecomm_picking_scan_bin.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_MATNR){
                                            text_ecomm_picking_scan_material.setText("");
                                            text_ecomm_picking_scan_material.requestFocus();
                                        }
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        if (request == REQUEST_PICK_STORE) {
                                            ll_ecomm_picking_radio_group.setVisibility(View.GONE);
                                            ll_ecomm_picking_btn_next.setVisibility(View.GONE);
                                            ll_ecomm_picking_bin_number.setVisibility(View.VISIBLE);
                                            text_ecomm_picking_group_number.setEnabled(false);
                                            text_ecomm_picking_scan_bin.requestFocus();
                                            processAndListBins(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_BIN) {
                                            ll_ecomm_picking_material_number.setVisibility(View.VISIBLE);
                                            ll_ecomm_picking_btn_save.setVisibility(View.VISIBLE);
                                            text_ecomm_picking_scan_bin.setEnabled(false);
                                            text_ecomm_picking_scan_material.setText("");
                                            text_ecomm_picking_scan_material.requestFocus();
                                            processAndListMaterials(responsebody);
                                        }
                                        else if (request == REQUEST_VALIDATE_MATNR) {
                                            text_ecomm_picking_scan_material.setText("");
                                            text_ecomm_picking_scan_material.requestFocus();
                                            addMaterialInTable(targetEtData);
                                        }
                                        else if (request == REQUEST_SAVE) {;
                                            ll_ecomm_picking_btn_save.setVisibility(View.GONE);

                                            text_ecomm_picking_scan_material.setText("");
                                            ll_ecomm_picking_material_number.setVisibility(View.GONE);

                                            text_ecomm_picking_scan_bin.setEnabled(true);
                                            text_ecomm_picking_scan_bin.setText("");
                                            ll_ecomm_picking_bin_number.setVisibility(View.GONE);

                                            ll_ecomm_picking_btn_next.setVisibility(View.VISIBLE);

                                            text_ecomm_picking_group_number.setEnabled(true);
                                            text_ecomm_picking_group_number.requestFocus();
                                        }
                                        return;
                                    }
                                }
                            }
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
        Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
        Log.d(TAG, "jsonRequest getBodyContentType->" + mJsonRequest.getBodyContentType());
        Log.d(TAG, "jsonRequest getBody->" + mJsonRequest.getBody().toString());
        Log.d(TAG, "jsonRequest getMethod->" + mJsonRequest.getMethod());
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

    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.i(TAG, "Error :" + error.toString());
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

    private void processAndListBins(JSONObject responsebody){
        try
        {
            penqty = responsebody.getString("EX_PENQTY");
            lgort = responsebody.getString("EX_LGORT");
            loc001 = responsebody.getString("EX_0001_LOC");
            loc002 = responsebody.getString("EX_0002_LOC");
            sammg = responsebody.getString("EX_SAMMG");
            tottoqty = responsebody.getString("EX_TOTTOQTY");

            JSONArray ET_PICK_DATA_ARRAY = responsebody.getJSONArray("ET_PICKDATA");
            ET_PICK_DATA_ARRAY = CommonUtils.sortJsonArray(ET_PICK_DATA_ARRAY,"BIN", true);
            int totalEtRecords = ET_PICK_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_PICK_DATA_ARRAY.getJSONObject(recordIndex);
                    binlist.add(ET_RECORD.getString("BIN"));
                    ETPickData pickData = new ETPickData();
                    pickData.setLgvbeln(ET_RECORD.getString("VBELN"));
                    pickData.setLggnature(ET_RECORD.getString("GNATURE"));
                    pickData.setLgvgbel(ET_RECORD.getString("VGBEL"));
                    pickData.setLglfimg(Float.parseFloat(ET_RECORD.getString("LFIMG")));
                    pickData.setLgbin(ET_RECORD.getString("BIN"));
                    pickData.setLgsammg(ET_RECORD.getString("SAMMG"));
                    pickData.setLgmeins(ET_RECORD.getString("MEINS"));
                    pickData.setLgurl(ET_RECORD.getString("URL"));
                    pickData.setLgmc_descr(ET_RECORD.getString("MC_DESCR"));
                    pickData.setLgposnr(ET_RECORD.getString("POSNR"));
                    pickData.setLgcrate(ET_RECORD.getString("CRATE"));
                    pickData.setLgmaterial(ET_RECORD.getString("MATERIAL"));
                    pickData.setLgmatnr(ET_RECORD.getString("MATNR"));
                    etPickData.add(pickData);
                }
            }
            fillBinList();
        } catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    private void fillBinList(){

        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 20;
        textSize = 15;
        //int rows = ET_PICK_DATA.size();
        int rows = binlist.size();
        table_bin_material.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerBin = new TextView(getContext());
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
        table_bin_material.addView(tr, trParams);

        //Create Data Rows in Table
        int rowNum = 1;
        for (String bin:binlist) {
            TextView tvSno = new TextView(getContext());
            tvSno.setText(rowNum+"");
            tvSno.setTextSize(textSize);
            tvSno.setPadding(5,2,0,2);
            tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            TextView tvBin = new TextView(getContext());
            tvBin.setText(bin);
            tvBin.setTextSize(textSize);
            tvBin.setPadding(5,2,0,2);
            tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

            tr = new TableRow(getContext());
            tr.setId(rowNum);
            tr.setPadding(0,0,0,0);
            tr.setLayoutParams(trParams);
            tr.addView(tvSno);
            tr.addView(tvBin);
            tr.setTag(bin);
            table_bin_material.addView(tr, trParams);
            rowNum++;
        }
        text_ecomm_picking_scan_bin.setText("");
        text_ecomm_picking_scan_bin.requestFocus();
    }

    private void processAndListMaterials(JSONObject responsebody){
        try
        {
            zzcrate = responsebody.getString("EX_BIN");
            createMaterialTable();
        } catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    private void createMaterialTable(){

        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 20;
        textSize = 15;

        table_bin_material.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerBin = new TextView(getContext());
        TextView headerScanQty = new TextView(getContext());
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
        headerBin.setText("Material");

        headerScanQty.setGravity(Gravity.CENTER);
        headerScanQty.setPadding(0,5,0,5);
        headerScanQty.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerScanQty.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerScanQty.setText("SQty");

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
        tr.addView(headerScanQty);
        table_bin_material.addView(tr, trParams);
    }
    private void addMaterialInTable(ETPickData pickData){
        String matnr = pickData.getLgmatnr();
        matnrlist.add(matnr);
        etPickData.remove(pickData);
        int length = matnrlist.size();
        int textSize =15;
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;

        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);

        TextView tvSno = new TextView(getContext());
        tvSno.setText(length+"");
        tvSno.setTextSize(textSize);
        tvSno.setPadding(5,2,0,2);
        tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

        TextView tvBin = new TextView(getContext());
        tvBin.setText(matnr);
        tvBin.setTextSize(textSize);
        tvBin.setPadding(5,2,0,2);
        tvBin.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

        TextView tvSQty = new TextView(getContext());
        tvSQty.setText("1");
        tvSQty.setTextSize(textSize);
        tvSQty.setPadding(5,2,0,2);
        tvSQty.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

        TableRow tr = new TableRow(getContext());
        tr.setId(length);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(tvSno);
        tr.addView(tvBin);
        tr.setTag(matnr+"-"+length);
        table_bin_material.addView(tr, trParams);
    }
}