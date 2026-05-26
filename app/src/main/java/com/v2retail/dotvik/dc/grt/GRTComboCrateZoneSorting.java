package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import android.widget.LinearLayout;
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
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETData;
import com.v2retail.dotvik.modal.grt.cratesort.CrateSortETEANData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.UInt;

public class GRTComboCrateZoneSorting extends Fragment {

    private static final int REQUEST_VALIDATE_CRATE = 204;
    private static final int REQUEST_EMPTY_CRATE = 208;
    private static final int REQUEST_SAVE_SORT = 205;
    private static final int REQUEST_SAVE_HU = 206;

    private static final String TAG = GRTCrateSortingProcess.class.getName();
    private static final String CRATE = "crate";
    private static final String ZONE = "zone";
    private static final String ETDATA = "etdata";

    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    String URL="";
    String WERKS="";
    String USER="";
    ProgressDialog dialog;
    AlertBox box;
    String crate = "";
    String zone = "";
    JSONObject etData = new JSONObject();

    LinearLayout ll_form;
    EditText text_msa_crate, text_scan_crate,text_remaining_qty, text_scanned_qty,text_scan_article,text_article_no,text_store_bin,text_scan_hu,text_proposed_hu;
    Button button_empty,button_back;
    private Map<String, CrateSortETEANData> ET_EAN_DATA = new HashMap<>();
    private Map<Integer, CrateSortETData> ET_DATA = new HashMap<>();
    private List<String> scannedMatnr = new ArrayList<>();
    private List<CrateSortETData> EsData = new ArrayList<>();
    JSONArray ET_WAVE_HUs,ET_GRT_ORDCONFs;


    public GRTComboCrateZoneSorting() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity()).setActionBarTitle("Scanning");
    }

    public static GRTComboCrateZoneSorting newInstance(String param1, String param2) {
        GRTComboCrateZoneSorting fragment = new GRTComboCrateZoneSorting();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            zone = getArguments().getString(ZONE);
            crate = getArguments().getString(CRATE);
            try {
                etData = new JSONObject(getArguments().getString(ETDATA));
            }catch (Exception exce){

            }
        }
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_grt_combo_crate_zone_sorting, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        activity = getActivity();

        ll_form = rootView.findViewById(R.id.ll_grt_combo_crate_sort_form);
        text_remaining_qty = rootView.findViewById(R.id.text_grt_combo_sort_crate_remaining_qty);
        text_scanned_qty = rootView.findViewById(R.id.text_grt_combo_sort_crate_scanned_qty);
        text_msa_crate = rootView.findViewById(R.id.text_grt_combo_sort_msa_crate);
        text_scan_article = rootView.findViewById(R.id.text_grt_combo_sort_scan_article_bcode);
        text_article_no = rootView.findViewById(R.id.text_grt_combo_sort_scan_article_no);
        text_store_bin = rootView.findViewById(R.id.text_grt_combo_sort_sugg_store_bin);
        text_scan_crate = rootView.findViewById(R.id.text_grt_combo_sort_scan_crate);
        text_scan_hu = rootView.findViewById(R.id.text_grt_combo_sort_scan_hu);
        text_proposed_hu = rootView.findViewById(R.id.text_grt_combo_sort_proposed_hu);
        button_empty = rootView.findViewById(R.id.button_empty_grt_combo_sort_form);
        button_back = rootView.findViewById(R.id.button_back_grt_combo_sort_form);


        text_msa_crate.setText(crate);
        text_msa_crate.setEnabled(false);

        button_empty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emptyPickedCrate();
            }
        });
        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                box.getBox("Alert", "Do you want to go to Crate Scanning??", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bundle args=new Bundle();
                        Fragment fragment = null;
                        fragment =  new GRTComboCrateScanning();
                        args.putString("zone", zone);
                        fragment.setArguments(args);
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.home, fragment, "grt_combo_picking_process_menu");
                        ft.commit();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
        });

        text_scan_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String crate = text_scan_crate.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        text_scan_crate.selectAll();
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });
        text_scan_crate.addTextChangedListener(new TextWatcher() {
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
                    text_scan_crate.selectAll();
                    validateCrate(crate);
                }
            }
        });

        text_scan_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  articlebcode = text_scan_article.getText().toString().toUpperCase();
                    if(articlebcode.length()>0) {
                        text_scan_article.selectAll();
                        updateDataAndSave(articlebcode);
                        return true;
                    }
                }
                return false;
            }
        });
        text_scan_article.addTextChangedListener(new TextWatcher() {
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
                String articlebcode = s.toString().toUpperCase();
                if(articlebcode.length()>0 && scannerReading) {
                    text_scan_article.selectAll();
                    updateDataAndSave(articlebcode);
                }
            }
        });

        text_scan_hu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  scanhu = text_scan_hu.getText().toString().toUpperCase();
                    if(scanhu.length()>0) {
                        text_scan_hu.selectAll();
                        saveHU(scanhu);
                        return true;
                    }
                }
                return false;
            }
        });
        text_scan_hu.addTextChangedListener(new TextWatcher() {
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
                String scanhu = s.toString().toUpperCase();
                if(scanhu.length()>0 && scannerReading) {
                    text_scan_hu.selectAll();
                    saveHU(scanhu);
                }
            }
        });

        ET_WAVE_HUs = new JSONArray();
        ET_GRT_ORDCONFs = new JSONArray();
        text_scan_crate.requestFocus();
        return rootView;
    }
    private void resetForm(){
        text_scan_crate.setEnabled(true);
        text_scan_crate.setText("");
        text_scanned_qty.setText("");
        text_scan_article.setText("");
        text_article_no.setText("");
        text_store_bin.setText("");
        button_empty.setVisibility(View.INVISIBLE);
        text_scan_crate.requestFocus();
    }
    private void emptyPickedCrate(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZCOMBO_ZONE_CRATE_VALIDATE_SAVE);
            args.put("IM_CRATE", text_scan_crate.getText().toString().trim().toUpperCase());
            args.put("IM_USER", USER);
            showProcessingAndSubmit(Vars.ZCOMBO_ZONE_CRATE_VALIDATE_SAVE, REQUEST_EMPTY_CRATE, args,null);

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
    private void validateCrate(String crate) {
        if(crate.toUpperCase(Locale.ROOT).trim().equals(text_msa_crate.getText().toString().toUpperCase(Locale.ROOT).trim())){
            UIFuncs.errorSound(con);
            AlertBox box = new AlertBox(con);
            box.getBox("Err", "EMPTY CRATE and MSA CRATE cannot be same");
            text_scan_crate.setText("");
            text_scan_crate.requestFocus();
            return;
        }
        JSONObject args = new JSONObject();
        try {
            text_remaining_qty.setText("0");
            text_scan_article.setText("");
            text_scanned_qty.setText("0");
            text_article_no.setText("");
            text_store_bin.setText("");

            args.put("bapiname", Vars.ZGRT_COMBO_ZONE_MSA_CRATE_VALIDATE);
            args.put("IM_MSA_CRATE", crate);
            args.put("IM_USER", USER);

            ET_EAN_DATA = new HashMap<>();
            ET_DATA = new HashMap<>();
            scannedMatnr = new ArrayList<>();

            showProcessingAndSubmit(Vars.ZGRT_COMBO_ZONE_MSA_CRATE_VALIDATE, REQUEST_VALIDATE_CRATE, args,null);

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
    private void updateDataAndSave(String articlebcode) {
        CrateSortETData ETData = new CrateSortETData();
        ETData.setLgmatnr(articlebcode);
        ETData.setLgzonecrate(zone);
        ETData.setLgcrate(crate);
        saveEtData(ETData);
    }
    private void saveEtData(CrateSortETData etData){
        JSONObject args = new JSONObject();
        //CrateSortETData etData = targetEtData.getValue();
        etData.setLgmapped("X");
        try {
            String ET_Data_JsonString = new Gson().toJson(etData);
            if(ET_Data_JsonString.length() == 0){
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid Request", "Nothing to save. Please scan some articles");
                return;
            }
            JSONObject ET_DATA = new JSONObject(ET_Data_JsonString);
            args.put("bapiname", Vars.ZCOMBO_VALIDATE_SAVE_SORT);
            args.put("IM_USER", USER);
            args.put("IM_EMPTY_CRATE", text_scan_crate.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("IM_DATA", ET_DATA);
            showProcessingAndSubmit(Vars.ZCOMBO_VALIDATE_SAVE_SORT, REQUEST_SAVE_SORT, args,etData);

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
    private void saveHU(String scanhu){
        JSONObject args = new JSONObject();
        try {
            if(ET_WAVE_HUs.length() == 0 || ET_GRT_ORDCONFs.length() == 0){
                AlertBox box = new AlertBox(getContext());
                box.getBox("Invalid Request", "Nothing to save. WAVE HU or ORDCONF data is blank");
                return;
            }
            args.put("bapiname", Vars.ZCOMBO_PICK_VALIDATE_HU_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_SCAN_HU", scanhu.toUpperCase(Locale.ROOT).trim());
            args.put("IM_SHOW_HU", text_proposed_hu.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("ET_WAVE_HU", ET_WAVE_HUs);
            args.put("ET_GRT_ORDCONF", ET_GRT_ORDCONFs);
            showProcessingAndSubmit(Vars.ZCOMBO_PICK_VALIDATE_HU_SAVE, REQUEST_SAVE_HU, args,null);

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

    public void showProcessingAndSubmit(String rfc, int request,JSONObject args, CrateSortETData targetEtData){

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
    private void submitRequest(String rfc, int request, JSONObject args, CrateSortETData targetEtData){
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
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_SAVE_SORT) {
                                            text_store_bin.setText("");
                                            text_article_no.setText("");
                                            text_scan_article.setText("");
                                            text_scan_article.requestFocus();
                                        }
                                        else if (request == REQUEST_VALIDATE_CRATE) {
                                            text_scan_crate.setText("");
                                            text_scan_crate.requestFocus();
                                        }
                                        else if (request == REQUEST_SAVE_HU) {
                                            text_scan_hu.setText("");
                                            text_scan_hu.requestFocus();
                                        }
                                        return;
                                    } else {

                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            ll_form.setVisibility(View.VISIBLE);
                                            button_empty.setVisibility(View.VISIBLE);
                                            text_scan_crate.setEnabled(false);
                                            text_scan_article.requestFocus();
                                        }
                                        else if (request == REQUEST_SAVE_SORT) {
                                            button_empty.setVisibility(View.VISIBLE);
                                            updateRecordsAndUI(type,responsebody);
                                        }
                                        else if (request == REQUEST_SAVE_HU) {
                                            ET_WAVE_HUs = new JSONArray();
                                            ET_GRT_ORDCONFs = new JSONArray();
                                            text_scan_article.setText("");
                                            text_scan_article.requestFocus();

                                            text_scan_hu.setText("");
                                            text_scan_hu.setEnabled(true);
                                            text_scan_hu.setBackgroundColor(getResources().getColor(R.color.white));

                                            text_proposed_hu.setText("");
                                            text_scan_article.setText("");
                                            text_scan_article.setEnabled(true);
                                            text_scan_article.setBackgroundColor(getResources().getColor(R.color.white));

                                            text_scan_article.requestFocus();
                                        }
                                        else if (request == REQUEST_EMPTY_CRATE) {
                                            AlertBox box = new AlertBox(getContext());
                                            if(responsebody.has("EX_TANUM")){
                                                box.getBox("Success", responsebody.getString("EX_TANUM")+" data saved successfully");
                                            }
                                            else {
                                                box.getBox("Success", returnobj.getString("Data saved successfully"));
                                            }
                                            resetForm();
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
    private void updateRecordsAndUI(String type, JSONObject responsebody) throws JSONException {
        JSONObject ET_RECORD = responsebody.getJSONObject("ES_DATA");
        JSONArray ET_WAVE_HU = responsebody.getJSONArray("ET_WAVE_HU");
        JSONArray ET_GRT_ORDCONF = responsebody.getJSONArray("ET_GRT_ORDCONF");

        if(ET_RECORD.getString("MATNR").length() > 0){

            String phu = UIFuncs.removeLeadingZeros(ET_GRT_ORDCONF.getJSONObject(1).getString("EXIDV"));
            if(phu.equals("DH24")){
                text_scan_hu.setText(phu);
                text_scan_hu.setEnabled(false);
                text_scan_hu.setBackgroundColor(getResources().getColor(R.color.viewBg));

                text_scan_article.setEnabled(true);
                text_scan_article.setBackgroundColor(getResources().getColor(R.color.white));
                text_scan_article.setText("");
                ET_WAVE_HUs = new JSONArray();
                ET_GRT_ORDCONFs = new JSONArray();
                text_scan_article.requestFocus();

            }else{
                ET_WAVE_HUs.put(ET_WAVE_HUs.length(),ET_WAVE_HU.getJSONObject(1));
                ET_GRT_ORDCONFs.put(ET_GRT_ORDCONFs.length(),ET_GRT_ORDCONF.getJSONObject(1));
                text_scan_article.setText("");
                text_scan_article.setEnabled(false);
                text_scan_article.setBackgroundColor(getResources().getColor(R.color.viewBg));
                text_scan_hu.setEnabled(true);
                text_scan_hu.setBackgroundColor(getResources().getColor(R.color.white));
                text_scan_hu.setText("");
                text_scan_hu.requestFocus();
            }
            text_proposed_hu.setText(phu);

            CrateSortETData ESData = new CrateSortETData();
            ESData.setLgmandt(ET_RECORD.getString("MANDT"));
            ESData.setLgwave(ET_RECORD.getString("WAVE"));
            ESData.setLgtanum(ET_RECORD.getInt("TANUM"));
            ESData.setLgitemno(ET_RECORD.getInt("ITEMNO"));
            ESData.setLgebeln(ET_RECORD.getString("EBELN"));
            ESData.setLgebelp(ET_RECORD.getInt("EBELP"));
            ESData.setLgetype(ET_RECORD.getString("ETYPE"));
            ESData.setLgzonesec(ET_RECORD.getString("ZONE_SEC"));
            ESData.setLgvbelnvl(ET_RECORD.getString("VBELN_VL"));
            ESData.setLgdwerks(ET_RECORD.getString("DWERKS"));
            ESData.setLgmatnr(ET_RECORD.getString("MATNR"));
            ESData.setLgmenge(ET_RECORD.getDouble("MENGE"));
            ESData.setLglgtyp(ET_RECORD.getString("LGTYP"));
            ESData.setLglgpla(ET_RECORD.getString("LGPLA"));
            ESData.setLgcrate(ET_RECORD.getString("CRATE"));
            ESData.setLgcomplete(ET_RECORD.getString("COMPLETE"));
            ESData.setLglvorm(ET_RECORD.getString("LVORM"));
            ESData.setLgpnature(ET_RECORD.getString("PNATURE"));
            ESData.setLgpicked(ET_RECORD.getString("PICKED"));
            ESData.setLgmapped(ET_RECORD.getString("MAPPED"));
            ESData.setLgerror(ET_RECORD.getString("ERROR"));
            ESData.setLgmessage(ET_RECORD.getString("MESSAGE"));
            ESData.setLgiscancelled(ET_RECORD.getString("ISCANCELLED"));
            ESData.setLgcanceltanum(ET_RECORD.getInt("CANCEL_TANUM"));
            ESData.setLgcancellgnum(ET_RECORD.getString("CANCEL_LGNUM"));
            ESData.setLgerdat(ET_RECORD.getString("ERDAT"));
            ESData.setLgernam(ET_RECORD.getString("ERNAM"));
            ESData.setLgerzet(ET_RECORD.getString("ERZET"));
            ESData.setLgmapped("X");
            EsData.add(ESData);

            /*int scannedQty = EsData.stream()
                    .map(data -> (int)data.getLgmenge())
                    .reduce(0,(a,b) -> a+b);*/

            text_scanned_qty.setText(((int)ESData.getLgmenge())+"");
            text_article_no.setText(UIFuncs.removeLeadingZeros(ESData.getLgmatnr()));
            text_store_bin.setText(ESData.getLgdwerks());
            text_scan_article.setText("");

        }else{
            UIFuncs.blinkEffectOnError(con,text_scan_article,true);
            AlertBox box = new AlertBox(getContext());
            box.getBox("Not Allowed",text_scan_article.getText().toString() +" is not allowed");
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
}