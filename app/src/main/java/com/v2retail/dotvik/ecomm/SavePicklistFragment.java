package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
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
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.picklist.CrateModel;
import com.v2retail.dotvik.modal.picklist.EanModel;
import com.v2retail.dotvik.modal.picklist.GetPickDataResponse;
import com.v2retail.dotvik.modal.picklist.MaterialModel;
//import com.v2retail.dotvik.scanner.BaseScanner;
import com.v2retail.util.AlertBox;
import com.v2retail.util.AppConstants;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

public class SavePicklistFragment extends Fragment implements View.OnClickListener {

    private final String TAG = "SavePicklistFragment";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";
    private View rootView;
    private Activity activity;
    private RecyclerView recycler_view;
    private ArrayList<CrateModel> list = new ArrayList<>();
    private PicklistItemAdapter adapter = null;
    private EditText text_picklist;
    private EditText text_crate;
    private EditText text_article;
    private EditText text_tq_sq;
    private EditText text_remaining_qty;

    private GetPickDataResponse getPickDataResponse = null;
    private ProgressDialog dialog = null;

    private enum  SCANNER_TYPES {
        PICKLIST, CRATE, ARTICLE
    }
    private String SCANNER_CURRENT_TYPE = "";
    private CrateModel SCANNER_CRATE_MODEL;


    private int totalCount = 0;
    private int scannedCount = 0;


    TreeMap<GroupByTuple, ArrayList<GetPickDataResponse.ETPICKDATA> > groupMap = null;

    public SavePicklistFragment() { }

    public static SavePicklistFragment newInstance() {
        return new SavePicklistFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_save_picklist, container, false);
        initView();
        return rootView;
    }

    private void initView() {
        activity = getActivity();

        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

        text_picklist = rootView.findViewById(R.id.text_picklist);
        text_crate = rootView.findViewById(R.id.text_crate);
        text_article = rootView.findViewById(R.id.text_article);
        text_tq_sq = rootView.findViewById(R.id.text_tq_sq);
        text_remaining_qty = rootView.findViewById(R.id.text_remaining_qty);

        recycler_view = rootView.findViewById(R.id.recycler_view);

        handlePicklistKeypadEditor();
        handleRecyclerView();
        handleClicks();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("eComm Picking");
        }

        text_picklist.requestFocus();

        updateDisplayStats();

        if(getArguments()!=null && getArguments().getString("picklistNumber")!=null) {
            String picklistNumber = getArguments().getString("picklistNumber");
            if(picklistNumber.length()>0) {
                text_picklist.setText(picklistNumber.toString());
                validatePicklist(picklistNumber);
            }
        }


    }

    private void handlePicklistKeypadEditor() {

        text_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String crateNo = text_crate.getText().toString();
                    if(crateNo.length()>0) {
                        validateCrate(crateNo);
                        return true;
                    }
                }
                return false;
            }
        });

        text_crate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {


            }
        });

        text_crate.addTextChangedListener(new TextWatcher() {
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
                String crateNo = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Crate No: " +  crateNo);
                    validateCrate(crateNo);
                }
            }
        });

/*
        text_crate.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {

                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if ( (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036) && (
                            keyEvent.getScanCode() == 310
                                    || keyEvent.getScanCode() == 311
                                    || keyEvent.getScanCode() == 0)
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String crateNo = text_crate.getText().toString();
                        if(crateNo.length()>0) {
                            validateCrate(crateNo);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    }
                }
                text_crate.requestFocus();

                return false;
            }
        });

 */

        text_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String article = text_article.getText().toString();

                    if(article.length()>0) {
                        // validate article
                        CrateModel crateModel = findCrateModel(text_crate.getText().toString());
                        if(crateModel!=null && validateArticle(crateModel, article)) {
                            text_article.setText("");
                            text_article.requestFocus();
                            scannedCount = scannedCount + 1;
                            updateDisplayStats();
                            return true;
                        } else {
                            text_article.setText("");
                            text_article.requestFocus();
                        }
                    }
                }
                return false;
            }
        });


        text_article.addTextChangedListener(new TextWatcher() {
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
                String article = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned article No: " +  article);
                    CrateModel crateModel = findCrateModel(text_crate.getText().toString());
                    if(validateArticle(crateModel, article)) {
                        scannedCount = scannedCount + 1;
                        updateDisplayStats();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                text_article.setText("");
                                text_article.requestFocus();
                            }
                        });
                    }
                }
            }
        });

        /*
        text_article.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {

                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if ( (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036) && (
                            keyEvent.getScanCode() == 310
                                    || keyEvent.getScanCode() == 311
                                    || keyEvent.getScanCode() == 0)
                    ) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String article = text_article.getText().toString();

                        if(article.length()>0) {
                            // validate article

                            Log.d("DEBUG", "Read Crate is " + text_crate.getText().toString());
                            CrateModel crateModel = findCrateModel(text_crate.getText().toString());
                            if(validateArticle(crateModel, article)) {
                                scannedCount = scannedCount + 1;
                                updateDisplayStats();
                                text_article.setText("");
                                text_article.requestFocus();
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            } else {
                                text_article.setText("");
                                text_article.requestFocus();
                            }
                        }
                    } else {
                        text_article.setText(""); // if no scan code received
                    }
                }
                text_article.requestFocus();
                return false;
            }
        });
         */

    }

    private void handleRecyclerView() {
        recycler_view.setLayoutManager(new LinearLayoutManager(activity));

        /*
        adapter = new CrateAdapter(activity, list, new CrateAdapter.OnItemClickListener() {
            @Override
            public void onAddArticleClick(CrateModel item) {
                openAddArticleDialog(item);
            }

            @Override
            public void onScanArticleClick(CrateModel item) {
                SCANNER_CURRENT_TYPE = SCANNER_TYPES.ARTICLE.name();
                SCANNER_CRATE_MODEL = item;
                openScanner();
            }
        });
        recycler_view.setAdapter(adapter);
         */
    }

    private void handleClicks() {

        rootView.findViewById(R.id.button_scan_new_crate).setOnClickListener(this);
        rootView.findViewById(R.id.button_save_picklist).setOnClickListener(this);
        rootView.findViewById(R.id.back).setOnClickListener(this);
        rootView.findViewById(R.id.prev_button).setOnClickListener(this);
        rootView.findViewById(R.id.next_button).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        CommonUtils.hideKeyboard(activity);
        switch (view.getId()){
            case R.id.button_scan_new_crate:
               // SCANNER_CURRENT_TYPE = SCANNER_TYPES.CRATE.name();
               // openScanner();
                text_crate.setText("");
                text_crate.requestFocus();
                break;

            case R.id.back:
                //TODO back
                getFragmentManager().popBackStack();
                break;
            case R.id.prev_button:
                //TODO save api use the CrateModel list and getPickListResponse to create send object


                break;
            case R.id.next_button:
                //TODO save api use the CrateModel list and getPickListResponse to create send object


                break;
            case R.id.button_save_picklist:
                //TODO save api use the CrateModel list and getPickListResponse to create send object
                savePicklist();
                break;
        }
    }

    private void openScanner() {
        if (text_picklist.getText().toString().isEmpty() || getPickDataResponse == null) {
            CommonUtils.showToast(activity, "Please scan valid picklist");
            return;
        }

        if(CameraCheck.isCameraAvailable(activity))
            IntentIntegrator.forSupportFragment(SavePicklistFragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
    }

    private void addCrate(String crateNo) {

        Log.d("DEBUG", "Crate No = " + crateNo);
        boolean crateExists = false;
        for (CrateModel crateModel: list) {
            if (crateModel.ID.equals(crateNo)) {
                crateExists = true;
            }
        }

        if (!crateExists) {
            Log.d("DEBUG", "New Crate Adding = " + crateNo);
            CrateModel newCrateModel = new CrateModel();
            newCrateModel.ID = crateNo;
            list.add(newCrateModel);
        }

       //  adapter.notifyDataSetChanged();
        //TODO ask if want to scan article next
    }

    CrateModel findCrateModel(String crateNo) {
        for (CrateModel crateModel: list) {
            if (crateModel.ID.equalsIgnoreCase(crateNo)) {
                return crateModel;
            }
        }
        /*
        CrateModel newCrateModel = new CrateModel();
        newCrateModel.ID = crateNo;
        list.add(newCrateModel);
         */
        return null;
    }

    private boolean validateArticle(CrateModel crateModel, String articleEAN) {
        boolean isEanValid = false;
        GetPickDataResponse.ETEANDATA selectedETEANDATA = null;
        GetPickDataResponse.ETPICKDATA selectedETPICKDATA = null;

        for (GetPickDataResponse.ETEANDATA eteandata : getPickDataResponse.ET_EAN_DATA) {
            if (eteandata.EAN11.equals(articleEAN)) {
                isEanValid = true;
                selectedETEANDATA = eteandata;
                break;
            }
        }

        if (!isEanValid) {
            AlertBox box = new AlertBox(activity);
            box.getBox("ERR", "Scanned EAN invalid");
            text_article.setText("");
            text_article.requestFocus();
            return false;
        } else {
            for (GetPickDataResponse.ETPICKDATA etpickdata : getPickDataResponse.ET_PICKDATA) {
                if(!etpickdata.picked) {
                    if (etpickdata.MATERIAL.equals(selectedETEANDATA.MATNR)) {
                        selectedETPICKDATA = etpickdata;
                        break;
                    }
                }
            }

            if(selectedETPICKDATA==null) {
                Log.d(TAG, "No pick data found for the article = " + articleEAN);
                Log.d(TAG, "User already scanned all articles for  " + articleEAN);
                AlertBox box = new AlertBox(activity);
                box.getBox("ERR", "Already scanned all articles needed.");
                text_article.setText("");
                text_article.requestFocus();
                return false;
            }

            // we need to check if it is marked as Not found then do not continue
            if(validateIfNotFoundMarked(selectedETPICKDATA)) {
                Log.d(TAG, "User already marked Not Found for this article = " + articleEAN);
                AlertBox box = new AlertBox(activity);
                box.getBox("ERR", "Already marked Not Found for this article");
                text_article.setText("");
                text_article.requestFocus();
                return false;
            }

            // need to validate the scan count as well
            if(validateScanCount(selectedETPICKDATA)) {
                Log.d(TAG, "User already scanned all articles for  " + articleEAN);
                AlertBox box = new AlertBox(activity);
                box.getBox("ERR", "Already scanned all articles needed.");
                text_article.setText("");
                text_article.requestFocus();
                return false;
            }

            if (crateModel.GNATURE == null || crateModel.GNATURE.isEmpty()) {
                crateModel.GNATURE = selectedETPICKDATA.GNATURE;
            } else {
                if (!crateModel.GNATURE.equals(selectedETPICKDATA.GNATURE)) {
                    AlertBox box = new AlertBox(activity);
                    box.getBox("ERR", "Nature of article does not match the crate");
                    text_article.setText("");
                    text_article.requestFocus();
                    return false;
                }
            }


            MaterialModel materialModel = new MaterialModel();
            materialModel.MATERIAL = selectedETPICKDATA.MATERIAL;
            materialModel.CRATE = crateModel.ID;
            materialModel.LFIMG = selectedETPICKDATA.LFIMG;
            materialModel.MEINS = selectedETPICKDATA.MEINS;
            materialModel.SAMMG = selectedETPICKDATA.SAMMG;
            materialModel.VBELN = selectedETPICKDATA.VBELN;
            materialModel.VGBEL = selectedETPICKDATA.VGBEL;
            materialModel.POSNR = selectedETPICKDATA.POSNR;
            materialModel.GNATURE = selectedETPICKDATA.GNATURE;

            selectedETPICKDATA.picked = true;  // it is picked now

            EanModel eanModel = new EanModel();
            eanModel.EAN = articleEAN;

            materialModel.scannedEAN.add(eanModel);

            crateModel.materialList.add(materialModel);

            // adapter.notifyDataSetChanged();
        }
        return isEanValid;
    }

    boolean validateIfNotFoundMarked(GetPickDataResponse.ETPICKDATA etpickdata) {
        boolean retVal = false;
        Set<GroupByTuple> set = groupMap.keySet();

        // ArrayList<GetPickDataResponse.ETPICKDATA> list = groupMap.get(new GroupByTuple(etpickdata.BIN, etpickdata.GNATURE, etpickdata.MATNR));
        // if(list)
        Iterator<GroupByTuple> iter = set.iterator();
        while(iter.hasNext()) {
            GroupByTuple tuple = iter.next();
            if(tuple.matnr.equals(etpickdata.MATNR) && tuple.bin.equals(etpickdata.BIN)
                    && tuple.nature.equals(etpickdata.GNATURE)) {
                if (tuple.notFound) {
                    return true;
                }
            }
        }
        return retVal;
    }

    boolean validateScanCount(GetPickDataResponse.ETPICKDATA etpickdata) {
        boolean retVal = false;

        Set<GroupByTuple> set = groupMap.keySet();

        // ArrayList<GetPickDataResponse.ETPICKDATA> list = groupMap.get(new GroupByTuple(etpickdata.BIN, etpickdata.GNATURE, etpickdata.MATNR));
        // if(list)
        Iterator<GroupByTuple> iter = set.iterator();
        while(iter.hasNext()) {
            GroupByTuple tuple = iter.next();
            if (tuple.matnr.equals(etpickdata.MATNR) && tuple.bin.equals(etpickdata.BIN)
                    && tuple.nature.equals(etpickdata.GNATURE)) {
                if (tuple.pickCount == tuple.groupList.size()) {
                    return true;
                }
            }
        }
        return retVal;
    }


    private void validateCrate(final String crateNo) {
        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateCrateJson(crateNo);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 2000);
    }

    private void sendValidateCrateJson(final String crateNo) {
        String rfc = "ZECOM_CRATE_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNo.toUpperCase());
            params.put("IM_USER", USER);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        text_crate.setText("");
                                        text_crate.requestFocus();
                                        return;
                                    } else {
                                        text_article.requestFocus();

                                        addCrate(crateNo);
                                        return;
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

    private void validatePicklist(final String picklistNo) {
        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidatePicklistJson(picklistNo);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 2000);
    }

    private void sendValidatePicklistJson(final String picklistNo) {
        String rfc = "ZECOM_GET_PICK_DATA";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_TANUM", picklistNo);
            params.put("IM_USER", USER);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        text_picklist.setText("");
                                        text_picklist.requestFocus();
                                        return;
                                    } else {
                                        getPickDataResponse = new Gson().fromJson(responsebody.toString(), GetPickDataResponse.class);
                                        text_picklist.setText(picklistNo);
                                        text_crate.requestFocus();
                                        updatePicklistDetails(getPickDataResponse.ET_PICKDATA);
                                        return;
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

    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        CommonUtils.hideKeyboard(activity);
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        AlertBox box = new AlertBox(activity);
        if (scanningResult == null) {
            Log.d(TAG, TAG + " scanned result...");
            box.getBox("Scanner Err", "Unable to receive Data");
        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);
            if (scanContent != null) {
                if (SCANNER_CURRENT_TYPE.equals(SCANNER_TYPES.PICKLIST.name())) {
                    validatePicklist(scanContent);
                } else if (SCANNER_CURRENT_TYPE.equals(SCANNER_TYPES.CRATE.name())) {
                    validateCrate(scanContent);
                } else if (SCANNER_CURRENT_TYPE.equals(SCANNER_TYPES.ARTICLE.name())) {
                    validateArticle(SCANNER_CRATE_MODEL, scanContent);
                }
            } else {
                box.getBox("Scanner Err", "No Content Received. Please Scan Again");
            }
        }
    }

     */

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

    void updatePicklistDetails(ArrayList<GetPickDataResponse.ETPICKDATA> pickListItems) {

        if(pickListItems!=null && pickListItems.size()>1) {

            // we need to remove the first item as it contains the definition
            // and this should be displayed
            pickListItems.remove(0);

            groupByBIN_NATURE_MATNR(pickListItems);

            Set<GroupByTuple> set = groupMap.keySet();
           //  GroupByTuple
            ArrayList<GroupByTuple> list = new ArrayList<GroupByTuple>();
            list.addAll(set);

            totalCount = pickListItems.size();
            updateDisplayStats();
            // we need to update the recycler view
            adapter = new PicklistItemAdapter(list);
            recycler_view.setAdapter(adapter);

        }
    }

    void updateDisplayStats() {
        text_tq_sq.setText("" + scannedCount + " / " +    totalCount);
        text_remaining_qty.setText("" + (totalCount - scannedCount));

        if(adapter!=null) {
            adapter.notifyDataSetChanged();
        }
    }

    void savePicklist() {

        /*
        if(scannedCount < totalCount) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Err", "Scanned quantity is less then the required count.");
            return;
        }

         */

        Log.d(TAG, "savePicklist");
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;

        String rfc = "ZECOM_SAVE_PICK_DATA";
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);

        final JSONObject params = createSavePayload(rfc);

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                       // text_picklist.setText("");
                                       //  text_picklist.requestFocus();
                                        return;
                                    } else {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                getFragmentManager().popBackStack();
                                            }
                                        });
                                        return;
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

        // no retry policy on save
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( AppConstants.VOLLEY_TIMEOUT, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    JSONObject createSavePayload(String rfc) {
        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_TANUM", text_picklist.getText().toString());
            params.put("IM_USER", USER);
            // params.put("IM_TEST", "X");  // was for testing only
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

        // we need to validate as well
        JSONArray itData = new JSONArray();
        for(int i=0; i<list.size();i++) {
            CrateModel crateModel = list.get(i);

            if(crateModel.materialList!=null && crateModel.materialList.size()>0) {
                for (int j = 0; j < crateModel.materialList.size(); j++) {
                    JSONObject jsonObject = new JSONObject();
                    MaterialModel mModel = crateModel.materialList.get(j);

                    try {
                        jsonObject.put("MATERIAL", mModel.MATERIAL);
                        jsonObject.put("PLANT", "DH25");
                        jsonObject.put("STOR_LOC", "0001");

                        jsonObject.put("MEINS", mModel.MEINS);  // from ETPICK
                        jsonObject.put("SCAN_QTY", mModel.LFIMG);
                        jsonObject.put("BIN", mModel.SAMMG);
                        jsonObject.put("CRATE", crateModel.ID);
                        jsonObject.put("SONUM", mModel.VGBEL);
                        jsonObject.put("DELNUM", mModel.VBELN);
                        jsonObject.put("POSNR", mModel.POSNR);
                        jsonObject.put("GNATURE", mModel.GNATURE );


                        itData.put(jsonObject);
                    } catch(JSONException je ) {
                        Log.d(TAG, je.getMessage());
                        je.printStackTrace();
                    }
                }
            }
        }

        try {
            params.put("IT_DATA", itData);
        } catch(JSONException je ) {
            Log.d(TAG, je.getMessage());
            je.printStackTrace();
        }

        return params;
    }


    void groupByBIN_NATURE_MATNR(ArrayList<GetPickDataResponse.ETPICKDATA> inList) {
        TreeMap<GroupByTuple, ArrayList<GetPickDataResponse.ETPICKDATA> > map = new TreeMap<GroupByTuple, ArrayList<GetPickDataResponse.ETPICKDATA>>();

        for(int i=0; i<inList.size(); i++) {
            GetPickDataResponse.ETPICKDATA pickingLine = inList.get(i);

            GroupByTuple tuple = new GroupByTuple(pickingLine.BIN, pickingLine.GNATURE, pickingLine.MATNR);
            tuple.crate = pickingLine.CRATE;
            tuple.url = pickingLine.URL;

            if(!map.containsKey(tuple)) {
                tuple.groupList.add(pickingLine);
                map.put(tuple, tuple.groupList);
            } else {
                map.get(tuple).add(pickingLine);
            }

        }
        this.groupMap = map;
    }

    // saving not found data.
    void saveNotFoundLines(final GroupByTuple notFoundTuple) {

        Log.d(TAG, "saveNotFoundLines()");

        if(notFoundTuple.notFound == false) return; // return if there is no "Not Found"

        if(notFoundTuple.notFoundUploaded == true ) return; //return if already upload;


        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;

        String rfc = "ZECOM_SAVE_PICK_NOT_FOUND_DATA";
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);

        final JSONObject params = createNotFoundLinePayload(rfc, notFoundTuple);

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
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        // text_picklist.setText("");
                                        //  text_picklist.requestFocus();
                                        return;
                                    } else {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                text_article.requestFocus();
                                            }
                                        });
                                        notFoundTuple.notFoundUploaded = true;
                                        return;
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

        // no retry policy on save
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( AppConstants.VOLLEY_TIMEOUT, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    JSONObject createNotFoundLinePayload(String rfc, GroupByTuple notFoundTuple) {

        JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_TANUM", text_picklist.getText().toString());
            params.put("IM_USER", USER);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }

        // we need to validate as well
        JSONArray itData = new JSONArray();

        for(int i=0; i<notFoundTuple.groupList.size();i++) {
            GetPickDataResponse.ETPICKDATA item = notFoundTuple.groupList.get(i);

            if(!item.picked) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("VBELN", item.VBELN);
                    jsonObject.put("MATNR", item.MATNR);
                    jsonObject.put("VGBEL", item.VGBEL);
                    jsonObject.put("LFIMG", item.LFIMG);
                    jsonObject.put("MEINS", item.MEINS);

                    jsonObject.put("SAMMG", item.SAMMG);
                    jsonObject.put("MATERIAL", item.MATERIAL);
                    jsonObject.put("POSNR", item.POSNR);
                    jsonObject.put("GNATURE", item.GNATURE );

                    jsonObject.put("BIN", item.BIN);
                    jsonObject.put("MC_DESCR", item.MC_DESCR);

                    itData.put(jsonObject);

                } catch(JSONException jsone) {


                }
            }
        }

        try {
            params.put("IT_DATA", itData);
        } catch(JSONException je ) {
            Log.d(TAG, je.getMessage());
            je.printStackTrace();
        }

        return params;
    }

    class GroupByTuple implements  Comparable<GroupByTuple> {
        String bin;
        String crate;
        String nature;
        String matnr;
        String url;


        ArrayList<GetPickDataResponse.ETPICKDATA> groupList = new ArrayList<>();

        // for the current state of individual lines
        int pickCount = 0;
        boolean notFound = false;
        boolean notFoundUploaded = false;

        GroupByTuple(String bin, String nature, String matnr) {
            this.bin = bin;
            this.nature = nature;
            this.matnr = matnr;
        }

        @Override
        public int compareTo(GroupByTuple o) {
            if(this.bin.compareTo(o.bin) < 0) {
                return -1;
            } else if(this.bin.compareTo(o.bin) > 0) {
                return 1;
            } else if(this.bin.compareTo(o.bin) == 0) {
                if(this.nature.compareTo(o.nature) < 0) {
                    return -1;
                } else if(this.nature.compareTo(o.nature) > 0) {
                    return 1;
                } else if(this.nature.compareTo(o.nature) == 0) {
                    if(this.matnr.compareTo(o.matnr) < 0) {
                        return -1;
                    } else if(this.matnr.compareTo(o.matnr) > 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
            return 0;
        }
    }

    class PicklistItemAdapter extends RecyclerView.Adapter<PickListViewHolder> {

        ArrayList<GroupByTuple> groupList = null;

        PicklistItemAdapter(ArrayList<GroupByTuple> list) {
            this.groupList = list;
        }
        @Override
        public PickListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ecomm_picklist_item_view, parent, false);
            return new PickListViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(PickListViewHolder holder, int position) {
            holder.updateView(position, this.groupList.get(position));

        }

        @Override
        public int getItemCount() {
            if(groupList!=null) {
                return groupList.size();
            }
            return 0;
        }
    }



    class PickListViewHolder extends RecyclerView.ViewHolder {
        TextView textBin;
        TextView textCrate;
        TextView textNature;
        TextView textMaterial;
        TextView textQuantity;
        ImageView articleImage;
        CheckBox cbNotFound;

        public PickListViewHolder(View itemView) {
            super(itemView);

            textBin = itemView.findViewById(R.id.text_bin);
            textCrate = itemView.findViewById(R.id.text_line_crate);
            textNature =  itemView.findViewById(R.id.text_nature);
            textMaterial =  itemView.findViewById(R.id.text_item_matnr);
            textQuantity = itemView.findViewById(R.id.text_item_qty);
            articleImage = itemView.findViewById(R.id.iv_article_image);
            cbNotFound = itemView.findViewById(R.id.cb_not_found);

            cbNotFound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        final GroupByTuple tuple  = (GroupByTuple)buttonView.getTag();
                        // do not do anything if already picked all
                        if(tuple.pickCount == tuple.groupList.size()) return;

                        if(tuple.notFound == false) {

                            AlertBox box = new AlertBox(getContext());
                            box.getBox("Error", "Article not found. Do you want to update 'Not Found' status.", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO:  Pradeep: 2021-07-30, Need to show confirmation popup
                                    tuple.notFound = true;
                                    saveNotFoundLines(tuple);
                                }
                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // negative

                                }
                            });
                        }
                    } else {
                        GroupByTuple tuple  = (GroupByTuple)buttonView.getTag();
                        if(tuple.notFound == true) {
                            cbNotFound.setChecked(true);  // keep checked if it was already set to not found
                        }
                    }
                }
            });
        }

        public void updateView(int position, GroupByTuple item) {

            item.pickCount = 0;
            for(int i=0; i<item.groupList.size(); i++) {
                if(item.groupList.get(i).picked) {
                    item.pickCount = item.pickCount + 1;
                }
            }

            if(item.pickCount > 0) {
                itemView.setBackgroundColor(0xffcccccc);
                textBin.setBackgroundColor(0xffcccccc);
                textCrate.setBackgroundColor(0xffcccccc);
                textNature.setBackgroundColor(0xffcccccc);
                textMaterial.setBackgroundColor(0xffcccccc);
                textQuantity.setBackgroundColor(0xffcccccc);
            } else {
                itemView.setBackgroundColor(0xffffffff);
                textBin.setBackgroundColor(0xffffffff);
                textCrate.setBackgroundColor(0xffffffff);
                textNature.setBackgroundColor(0xffffffff);
                textMaterial.setBackgroundColor(0xffffffff);
                textQuantity.setBackgroundColor(0xffffffff);
            }

            String trimZeros = item.bin.replaceFirst("^0+(?!$)", "");
            textBin.setText(trimZeros);

            trimZeros = item.nature.replaceFirst("^0+(?!$)", "");
            textNature.setText(trimZeros);

            trimZeros = item.matnr.replaceFirst("^0+(?!$)", "");
            textMaterial.setText(trimZeros + " - " + item.groupList.get(0).MC_DESCR);

            textQuantity.setText("" + item.pickCount  +  " / " +  item.groupList.size());

            textCrate.setText(item.crate);

            if(item.url!=null && item.url.length()>0) {
                Glide.with(getContext())
                        .load(new File(item.url))
                        .into(articleImage);
            } else {
                articleImage.setImageBitmap(null);
            }

            cbNotFound.setTag(item);

        }
    }

     /*
                Log.d("KeyEvent", "-----------------------");
                Log.d("KeyEvent", "Action: is " + keyEvent.getAction());
                Log.d("KeyEvent", "Action: UP constant" + keyEvent.ACTION_UP);
                Log.d("KeyEvent", "Action: DOWN constant" + keyEvent.ACTION_DOWN);

                Log.d("KeyEvent", "Action: KeyCode is " + keyEvent.getKeyCode());
                Log.d("KeyEvent", "Action: R1 Constant" + KeyEvent.KEYCODE_BUTTON_R1);
                Log.d("KeyEvent", "Action: L1 Constant" + keyEvent.KEYCODE_BUTTON_L1);
                Log.d("KeyEvent", "Action: Scan Code" + keyEvent.getScanCode());

                Log.d("KeyEvent", "-----------------------");
       */
}
