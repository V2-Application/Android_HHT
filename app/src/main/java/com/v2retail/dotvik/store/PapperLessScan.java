package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;

import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.AppConstants;
import com.v2retail.util.Barcode2D;
import com.v2retail.util.IBarcodeResult;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

public class PapperLessScan extends Fragment implements IBarcodeResult  {

    private final String TAG = PapperLessScan.class.getName();

    FragmentManager fm;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView back;
    private int currentIndex = 1;

    private TextView pickingNo;

    private TextView tqEditTextFiled, sqEditTextField, rqEditTextFiled, tvPrinter;

    private EditText currentScanBin, currentScanArticle, currentScanQuantity, currentScanOpenQuantity;

    private EditText binEditText, crateEditText, artNoEditText, sqtyEditText, storeIdText;
    private CheckBox checkBoxEmptyBin = null;

    private TextView secondScanItemNo;
    private TextView secondItemBin, secondItemCrate, secondItemMatnr, secondItemRemainQty, secondItemEan;

    private TextView thirdScanItemNo;
    private TextView thirdItemBin, thirdItemCrate, thirdItemMatnr, thirdItemRemainQty;

    private Button prev_button, next_button, save_button;

    String mPackingNo = "";
    String mDeliveryNumber = "";
    String mExternalHu = "";
    JSONObject mExLikp = null;
    JSONArray mEtLips = null;
    JSONArray mEtBinMc = null;
    JSONArray mEtEanData = null;

    int tsqCount = 0;
    int tqCount = 0;
    int trQCount = 0;

    HashMap<String, Integer> scanMap = new HashMap<String, Integer>();
    JSONArray scannedDataForSubmit = new JSONArray();
    HashMap<String, String> emptyBinMap = new HashMap<String, String>();

    String requestUrl = "";
    String loginUser = "";
    String mode = Vars.PAPER_LESS;
    String tvsprinter;
    ProgressDialog dialog = null;
    // ChainwayBarCode
    private Barcode2D barcode2D;
    private EditText chainwayContextEditText = null;

    public PapperLessScan() {
    }

    public static PapperLessScan newInstance(String mode) {
        PapperLessScan fragment = new PapperLessScan();
        if(mode != null){
            fragment.mode = mode;
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        fm = getFragmentManager();

        SharedPreferencesData data = new SharedPreferencesData(getContext());
        this.requestUrl = data.read("URL");
        this.loginUser = data.read("USER");

        if(Vars.TVS_PAPER_LESS.equalsIgnoreCase(mode)) {
            this.tvsprinter = data.read(Vars.TVS_PRINTER);
        }
       //  initializeChainway();

    }

    void clear() {
        try {
             storeIdText.setText("");
             if(mExLikp != null) mExLikp = null;
             if(mEtLips != null) mEtLips = null;
             if(mEtBinMc != null) mExLikp = null;
             if( mEtEanData != null) mEtEanData = null;
             if(scannedDataForSubmit !=null) scannedDataForSubmit = null;
             scanMap = new HashMap<String, Integer>();
             if(emptyBinMap!=null) { emptyBinMap.clear(); emptyBinMap = null; }
        } catch(Exception e) {

        }
    }
    void resetFields(){
        clear();
        scanMap = new HashMap<String, Integer>();
        scannedDataForSubmit = new JSONArray();
        emptyBinMap = new HashMap<String, String>();
        tsqCount = 0;
        tqCount = 0;
        trQCount = 0;
        tqEditTextFiled.setText("" + tqCount);
        sqEditTextField.setText("0");
        rqEditTextFiled.setText("");
        currentScanBin.setText("");
        currentScanArticle.setText("");
        currentScanQuantity.setText("");
        currentScanOpenQuantity.setText("");
        binEditText.setText("");
        crateEditText.setText("");
        artNoEditText.setText("");
        sqtyEditText.setText("");
        storeIdText.setText("");
        checkBoxEmptyBin.setChecked(false);
        secondScanItemNo.setText("");
        secondItemBin.setText("");
        secondItemCrate.setText("");
        secondItemEan.setText("");
        secondItemMatnr.setText("");
        secondItemRemainQty.setText("");
        thirdScanItemNo.setText("");
        thirdItemBin.setText("");
        thirdItemCrate.setText("");
        thirdItemMatnr.setText("");
        thirdItemRemainQty.setText("");
        currentIndex = 1;
        validateDelivery(mDeliveryNumber);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_papper_less_scan, container, false);
//        get intent data
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            initializeFromBundle(bundle);
        }

        pickingNo = view.findViewById(R.id.picking_no);  // package no
        pickingNo.setText(mDeliveryNumber);

//      tq/rq/sqt all edit field
        tqEditTextFiled = view.findViewById(R.id.tq_editTextFiled);
        tqEditTextFiled.setText("" + tqCount);

        sqEditTextField = view.findViewById(R.id.sq_editTextField);
        sqEditTextField.setText("0");

        rqEditTextFiled = view.findViewById(R.id.rq_editTextFiled);
        rqEditTextFiled.setText("" + trQCount);

//        current scan data field in
        currentScanBin = view.findViewById(R.id.current_scan_bin);
        currentScanArticle = view.findViewById(R.id.current_scan_article);
        currentScanQuantity = view.findViewById(R.id.current_scan_quantity);
        currentScanOpenQuantity = view.findViewById(R.id.current_scan_open_quantity);
        tvPrinter = view.findViewById(R.id.tv_printer_name);

//        Edit Type filed

        binEditText = view.findViewById(R.id.bin_editText);
        crateEditText = view.findViewById(R.id.crate_editText);
        artNoEditText = view.findViewById(R.id.artno_editText);
        sqtyEditText = view.findViewById(R.id.sqty_editText);
        storeIdText = view.findViewById(R.id.paperless_scan_store);

        checkBoxEmptyBin = view.findViewById(R.id.checkbox_empty_bin);

        secondScanItemNo = view.findViewById(R.id.second_scanItemNo);// net item number
//        second types item list  row
        secondItemBin = view.findViewById(R.id.second_item_bin);
        secondItemCrate = view.findViewById(R.id.second_item_crate);
        secondItemEan = view.findViewById(R.id.second_item_ean);
        secondItemMatnr = view.findViewById(R.id.second_item_matnr);
        secondItemRemainQty = view.findViewById(R.id.second_item_rqty);// remaining scan number


        thirdScanItemNo = view.findViewById(R.id.third_scanItemNo); //third scan number
//        third item list item types
        thirdItemBin = view.findViewById(R.id.third_item_bin);
        thirdItemCrate = view.findViewById(R.id.third_item_crate);
        thirdItemMatnr = view.findViewById(R.id.third_item_matnr);
        thirdItemRemainQty = view.findViewById(R.id.third_item_rqty);// remaining scan number

//        set button next and prev button scoll data

        prev_button = view.findViewById(R.id.prev_button);
        next_button = view.findViewById(R.id.next_button);

        prev_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentIndex >= 2) {
                    currentIndex--;
                    Log.d("realtimevalue", currentIndex + "");
                    setDataInView(currentIndex);
                    binEditText.requestFocus();
                } else {
                    Toast.makeText(getContext(), "You are already index value", Toast.LENGTH_SHORT).show();
                }
            }
        });
        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextBin();
            }
        });

        back = view.findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertBox box = new AlertBox(getContext());
                box.getBox("Error", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear();
                         fm.popBackStack();

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });
            }
        });


        save_button = view.findViewById(R.id.save);
        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveScannedData();
            }
        });

        configureTextChangeListners();

        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle(Vars.PAPER_LESS.equals(mode) ?  "Paperless - Scanning" : "TVS Paperless - Scanning");

        // Validate_zmw_DELIVERY_GET_DETAILS_PLP2("packingNo","editDeliverySelection","inputExternalHu");
        // from first position
        setDataInView(currentIndex);

        if(checkBoxEmptyBin!=null) {
            checkBoxEmptyBin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String binContext = binEditText.getText().toString();
                    if(isChecked) {
                        if(binContext.length()>0) {
                            emptyBinMap.put(binContext, "checked");
                        }
                    } else {
                        if(binContext.length()>0) {
                            emptyBinMap.remove("");
                        }
                    }
                }
            });

        }
        scanMap = new HashMap<String, Integer>();
        scannedDataForSubmit = new JSONArray();
        emptyBinMap = new HashMap<String, String>();
        tvPrinter.setVisibility(View.GONE);
        if(Vars.TVS_PAPER_LESS.equalsIgnoreCase(mode)) {
            tvPrinter.setText(this.tvsprinter);
            tvPrinter.setVisibility(View.VISIBLE);
        }
        return view;
    }


    // zwm_delivery_get_details_plp2
    private void validateDelivery(String deliveryNumber) {
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String rfc = "ZWM_DELIVERY_GET_DETAILS_PLP2";
        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_READ_DELV_ALL","X");
            params.put("IM_READ_EAN","X");
            params.put("IM_VBELN",deliveryNumber);

        } catch (JSONException e) {
            e.printStackTrace();


            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);

        }
        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                try{
                    Log.d(TAG, " ZWM_DELIVERY_GET_DETAILS_PLP2(): "+  responsebody.toString());

                    if (responsebody == null) {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", "No response from Server");
                    } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", "Unable to Connect Server/ Empty Response");
                        return;
                    } else {
                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        if(returnobj.has("MESSAGE")) {
                                            box.getBox("Err", returnobj.getString("MESSAGE"));
                                        }
                                        return;
                                    } else {
                                        PapperLessScan.this.mExLikp = responsebody.getJSONObject("EX_LIKP");
                                        PapperLessScan.this.mEtLips = responsebody.getJSONArray("ET_LIPS");
                                        PapperLessScan.this.mEtEanData =   responsebody.getJSONArray("ET_EAN_DATA");
                                        try {
                                            PapperLessScan.this.mEtBinMc = responsebody.getJSONArray("ET_BIN_MC");
                                            for (int i = 1; i < PapperLessScan.this.mEtBinMc.length(); i++) {
                                                JSONObject etBin = PapperLessScan.this.mEtBinMc.getJSONObject(i);
                                                int lineQty = sapNumberToInt("VISTM", etBin);
                                                int remainQty = sapNumberToInt("REMAIN_QTY", etBin);
                                                tqCount = tqCount + lineQty;
                                                trQCount = trQCount + remainQty;
                                                tqEditTextFiled.setText("" + tqCount);
                                                rqEditTextFiled.setText("" + trQCount);
                                                PapperLessScan.this.mEtBinMc.getJSONObject(i).put("UMREZ","1");
                                            }
                                            if(mEtBinMc.getJSONObject(1).has("STORE")) {
                                                storeIdText.setText(mEtBinMc.getJSONObject(1).getString("STORE"));
                                            }
                                        } catch (JSONException jsone) {

                                        }
                                        setDataInView(currentIndex);
                                        binEditText.requestFocus();
                                        return;
                                    }
                            }
                        }
                    }
                } catch (JSONException e){
                    e.printStackTrace();
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

            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);

        }
    }


    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle(Vars.PAPER_LESS.equals(mode) ?  "Paperless - Scanning" : "TVS Paperless - Scanning");
    }

    void configureTextChangeListners() {

        // for bin scanning
        binEditText.addTextChangedListener(new TextWatcher() {
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
                String binText = s.toString();
                if(scannerReading) {

                    if (handleBinScannning(binText)) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                artNoEditText.requestFocus();
                            }
                        });
                    } else {
                        // show popup message
                        new AlertBox(getContext()).getBox("Bin Validation", "Incorrect Bin", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                binEditText.setText("");
                                binEditText.requestFocus();
                                checkBoxEmptyBin.setChecked(false);
                            }
                        });
                    }

                }
            }
        });

        binEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "Bin Next");
                    // we need to validate the bin
                    if (handleBinScannning(binEditText.getText().toString())) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                artNoEditText.requestFocus();
                            }
                        });
                        return true;
                    } else {
                        // show popup message
                        new AlertBox(getContext()).getBox("Bin Validation", "Incorrect Bin", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                binEditText.setText("");
                                binEditText.requestFocus();
                                checkBoxEmptyBin.setChecked(false);
                            }
                        });

                    }
                } else if (actionId==EditorInfo.IME_NULL) {
                    // Capture most soft enters in multi-line EditTexts and all hard enters.
                    // They supply a zero actionId and a valid KeyEvent rather than
                    // a non-zero actionId and a null event like the previous cases.
                    if (event.getAction()==KeyEvent.ACTION_DOWN) {
                        // we need to validate the bin
                        if (handleBinScannning(binEditText.getText().toString())) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    artNoEditText.requestFocus();
                                }
                            });
                            return true;
                        } else {
                            // show popup message
                            new AlertBox(getContext()).getBox("Bin Validation", "Incorrect Bin", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    binEditText.setText("");
                                    binEditText.requestFocus();
                                    checkBoxEmptyBin.setChecked(false);
                                }
                            });
                        }
                    }
                }
                return false;
            }
        });

        binEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                Log.d(TAG, "binEditText.setOnKeyListener().onKey()");
                /*
                Zebra Code
                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    // keyCode = 10036, scan code receiving is 310
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310  ) {
                        // we need to validate the bin
                        if(binEditText.getText().toString().length()>0) {
                            if (handleBinScannning(binEditText.getText().toString())) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        artNoEditText.requestFocus();
                                    }
                                });
                                return true;
                            } else {
                                // show popup message
                                new AlertBox(getContext()).getBox("Bin Validation", "Incorrect Bin", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        binEditText.setText("");
                                        checkBoxEmptyBin.setChecked(false);
                                        binEditText.requestFocus();
                                    }
                                });
                            }
                        }
                    }
                }

                 */
                // chainway code for key trigger
                if (keyEvent.getAction() == keyEvent.ACTION_DOWN) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == 294
                            || keyEvent.getScanCode() == 253) {

                        // trigerring scanning on bin
                        startBarcodeScan();
                        chainwayContextEditText = binEditText;
                        return true;
                    }
                }
                return false;
            }
        });


        binEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    /*
                    if(scannerActivity!=null) {
                        scannerActivity.startScan(new ScannerActivity.ScanningFieldResultListener() {
                            @Override
                            public void onScannedResult(View view, String scannedValue) {
                                if(scannedValue!=null && scannedValue.length()>0) {
                                    binEditText.setText(scannedValue);
                                    if (handleBinScannning(binEditText.getText().toString())) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                artNoEditText.requestFocus();
                                            }
                                        });
                                    } else {
                                        // show popup message
                                        new AlertBox(getContext()).getBox("Bin Validation", "Incorrect Bin", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                binEditText.setText("");
                                                checkBoxEmptyBin.setChecked(false);
                                                binEditText.requestFocus();
                                            }
                                        });
                                    }
                                }

                            }
                        });
                    }
                    */

                    chainwayContextEditText = binEditText;

                } else {
                    /*
                    if(scannerActivity!=null) {
                        scannerActivity.stopScan(null);
                    }*/

                }
            }
        });

        // for crate scanning
        crateEditText.addTextChangedListener(new TextWatcher() {
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
                    artNoEditText.requestFocus();
                }
            }
        });

        crateEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "Crate Next");
                    artNoEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });


        // for article scanning
        artNoEditText.addTextChangedListener(new TextWatcher() {
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
                    handleArticleScanning(article);
                }

            }
        });

        artNoEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "Article Go");
                    if (handleArticleScanning(artNoEditText.getText().toString())) {
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        });

        artNoEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                Log.d(TAG, "artNoEditText.setOnKeyListener().onKey()");
                /*
                 Zebra
                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    // keyCode = 10036, scan code receiving is 310
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310  ) {
                        // we need to validate the bin
                        if(artNoEditText.getText().toString().length()>0) {
                            if (handleArticleScanning(artNoEditText.getText().toString())) {
                                return true;
                            } else {
                                artNoEditText.setText("");
                                return false;
                            }
                        }
                    }
                }
                 */
                // chainway code for key trigger
                if (keyEvent.getAction() == keyEvent.ACTION_DOWN) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == 294
                            || keyEvent.getScanCode() == 253) {

                        //
                        // startBarcodeScan();
                        chainwayContextEditText = artNoEditText;
                        return true;
                    }
                }

                return false;
            }
        });

        artNoEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    /*
                    if(scannerActivity!=null) {
                        scannerActivity.startScan(new ScannerActivity.ScanningFieldResultListener() {
                            @Override
                            public void onScannedResult(View view, String scannedValue) {
                                if(scannedValue!=null && scannedValue.length()>0) {
                                    artNoEditText.setText(scannedValue);
                                    if(handleArticleScanning(artNoEditText.getText().toString())) {

                                    } else {
                                        artNoEditText.setText("");
                                    }
                                }
                            }
                        });
                    }*/
                    chainwayContextEditText = artNoEditText;

                } else {
                    /*
                    if(scannerActivity!=null) {
                        scannerActivity.stopScan(null);
                    }

                     */
                }
            }
        });

        binEditText.requestFocus();
    }

    void initializeFromBundle(Bundle bundle) {
        mPackingNo = bundle.getString("packing_no"); // mPackingNo);
        mDeliveryNumber = bundle.getString("delivery_number");
        mExternalHu = bundle.getString("external_hu");

        // initializeDeliveryDetails(bundle);
        validateDelivery(mDeliveryNumber);
    }

    // no longer passing delivery details from previous screen fragment
    //Since below function has no usage, commented below function in release 11.81 by Narayanan
//    void initializeDeliveryDetails(Bundle bundle) {
//        String tVar = bundle.getString("ex_likp", "");
//        if (tVar != null && tVar.length() > 0) {
//            try {
//                this.mExLikp = new JSONObject(tVar);
//            } catch (JSONException jsone) {
//
//            }
//        }
//        tVar = bundle.getString("et_lips", "");
//        if (tVar != null && tVar.length() > 0) {
//            try {
//                this.mEtLips = new JSONArray(tVar);
//            } catch (JSONException jsone) {
//
//            }
//        }
//        tVar = bundle.getString("et_bin_mc", "");
//        if (tVar != null && tVar.length() > 0) {
//            try {
//                this.mEtBinMc = new JSONArray(tVar);
//                for (int i = 1; i < this.mEtBinMc.length(); i++) {
//                    JSONObject etBin = this.mEtBinMc.getJSONObject(i);
//                    int lineQty = sapNumberToInt("VISTM", etBin);
//                    int remainQty = sapNumberToInt("REMAIN_QTY", etBin);
//                    tqCount = tqCount + lineQty;
//                    trQCount = trQCount + remainQty;
//                }
//            } catch (JSONException jsone) {
//
//            }
//        }
//        tVar = bundle.getString("et_ean_data", "");
//        if (tVar != null && tVar.length() > 0) {
//            try {
//                this.mEtEanData = new JSONArray(tVar);
//            } catch (JSONException jsone) {
//
//            }
//        }
//
//    }

    int sapNumberToInt(String key, JSONObject expJson) {
        int retVal = 0;
        try {
            String numValue = expJson.getString(key);
            if (numValue != null && numValue.length() > 0) {
                int decIndex = numValue.indexOf('.');
                if (decIndex > 0) {
                    numValue = numValue.substring(0, decIndex);
                }
                retVal = Integer.parseInt(numValue);
            }
        } catch (Exception e) {

        }
        return retVal;
    }

    void nextBin() {
        if (currentIndex != -1 && currentIndex < mEtBinMc.length() - 1) {
            currentIndex++;
            Log.d("realtimevalue", currentIndex + "");

            setDataInView(currentIndex);
            binEditText.requestFocus();
        } else {
            Toast.makeText(getContext(), " list is limit is over", Toast.LENGTH_SHORT).show();

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

                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }

    private void setDataInView(int position) {
        try {
            if (position >= 1 && position < mEtBinMc.length()) {
                JSONObject json_2 = mEtBinMc.getJSONObject(position);
                String binVLPlA = json_2.getString("VLPLA");
                String crate = json_2.getString("CRATE");
                String matnr = json_2.getString("MATNR");
                String catDesc = json_2.getString("WGBEZ");
                String ean11 = json_2.getString("EAN11");

                secondScanItemNo.setText(catDesc);
                secondItemBin.setText(binVLPlA);
                secondItemCrate.setText(crate);
                secondItemEan.setText(ean11);

                secondItemMatnr.setText(matnr);
                secondItemRemainQty.setText("" + sapNumberToInt("REMAIN_QTY", json_2));

                if (position + 1 < mEtBinMc.length()) {
                    JSONObject json_3 = mEtBinMc.getJSONObject(position + 1);
                    binVLPlA = json_3.getString("VLPLA");
                    crate = json_3.getString("CRATE");
                    matnr = json_3.getString("MATNR");
                    catDesc = json_3.getString("WGBEZ");

                    thirdScanItemNo.setText(catDesc);
                    thirdItemBin.setText(binVLPlA);
                    thirdItemCrate.setText(crate);

                    thirdItemMatnr.setText(matnr);
                    thirdItemRemainQty.setText("" + sapNumberToInt("REMAIN_QTY", json_3));
                } else {
                    thirdScanItemNo.setText("");
                    thirdItemBin.setText("");
                    thirdItemCrate.setText("");
                    thirdItemMatnr.setText("");
                    thirdItemRemainQty.setText("");
                }

                sqtyEditText.setText("");
                artNoEditText.setText("");

                if(!binEditText.getText().toString().equals(secondItemBin.getText().toString())) {
                    // next new bin
                    binEditText.setText("");
                    crateEditText.setText("");
                    checkBoxEmptyBin.setChecked(false);
                } else {
                    crateEditText.setText(secondItemCrate.getText().toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Processing functions are here
    boolean handleBinScannning(String scannedBin) {
        boolean retVal = true;

        if (scannedBin.equals(secondItemBin.getText().toString())) {
            currentScanBin.setText(secondItemBin.getText().toString());
            crateEditText.setText(secondItemCrate.getText().toString());
        } else {
            retVal = false;
        }

        return retVal;
    }


    boolean handleArticleScanning(String scannedArticle) {
        boolean retVal = false;

        if (scannedArticle.equals("")) return retVal;
        if (currentScanBin.getText().toString().trim().length() == 0) {
            currentScanArticle.setText("");
            retVal = false;
        }
        else
            {
            JSONObject eanObject = findArticleFromBarcode(scannedArticle);
            if (eanObject != null) {
                try {
                    String matnr = eanObject.getString("MATNR");
                    String articleCount = eanObject.getString("UMREZ");
                    String toBeScan = secondItemRemainQty.getText().toString();

                    if (matnr.equals(secondItemMatnr.getText().toString()) && secondItemBin.getText().toString().toUpperCase(Locale.ROOT).trim().equals(binEditText.getText().toString().toUpperCase(Locale.ROOT).trim())) {
                        String binMatnrKey = currentScanBin.getText().toString() + "," + secondItemMatnr.getText().toString();

                        if (scanMap !=null && scanMap.containsKey(binMatnrKey)) {
                            Integer prevCount = scanMap.get(binMatnrKey);
                            int tempArticleCount = prevCount.intValue() + Integer.parseInt(articleCount);
                            if (tempArticleCount == Integer.parseInt(toBeScan)) {
                                processingContainData(Integer.parseInt(articleCount), binMatnrKey, matnr, eanObject);
                                retVal = true;
                                artNoEditText.setText("");
                                nextBin();
                                binEditText.setText("");
                                binEditText.requestFocus();
                            } else if (tempArticleCount < Integer.parseInt(toBeScan)) {
                                processingContainData(Integer.parseInt(articleCount), binMatnrKey, matnr, eanObject);
                                retVal = true;
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                            } else {
                                new AlertBox(getContext()).getBox("Barcode Validation",
                                        "Scan Quantity is greater than picklist quantity"
                                        , new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                artNoEditText.setText("");
                                                artNoEditText.requestFocus();
                                            }
                                        });
                            }
                        } else {
                            if (Integer.parseInt(articleCount) == Integer.parseInt(toBeScan)) {
                                processingFirstTimeData(Integer.parseInt(articleCount), binMatnrKey, matnr, eanObject);
                                artNoEditText.setText("");
                                retVal = true;
                                nextBin();
                                binEditText.requestFocus();
                            } else if (Integer.parseInt(articleCount) < Integer.parseInt(toBeScan)) {
                                processingFirstTimeData(Integer.parseInt(articleCount), binMatnrKey, matnr, eanObject);
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                                retVal = true;
                            } else {
                                new AlertBox(getContext()).getBox("Barcode Validation",
                                        "Scan Quantity is greater than picklist quantity"
                                        , new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                artNoEditText.setText("");
                                                artNoEditText.requestFocus();
                                            }
                                        });
                            }
                        }
                    } else {
                        new AlertBox(getContext()).getBox("Barcode Validation",
                                "Barcode or Article not mapped with this BIN"
                                , new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        artNoEditText.setText("");
                                        artNoEditText.requestFocus();
                                    }
                                });
                    }
                } catch (JSONException jsone) {

                }
            }
        }
        return retVal;
    }

    JSONObject findArticleFromBarcode(String scannedArticle) {
        JSONObject jsonObject = null;
        /*boolean found = false;
        if(mEtBinMc != null){
            for (int i = 0; i < mEtBinMc.length(); i++) {
                try {
                    JSONObject tempObject = mEtBinMc.getJSONObject(i);
                    String mcBarcode = tempObject.getString("EAN11"); // MATNR, UMREZ
                    String mcMatnr = tempObject.getString("MATNR");
                    if(mcMatnr.equals(secondItemMatnr.getText().toString().trim())){

                    }
                    if (mcBarcode.trim().length() > 0 && mcBarcode.equals(scannedArticle.trim())
                            && mcBarcode.equals(secondItemEan.getText().toString().trim())) {
                        jsonObject = tempObject;
                        found = true;
                        break;
                    }
                } catch (JSONException jsone) {
                    UIFuncs.errorSound(getContext());
                    new AlertBox(getContext()).getBox("Barcode Validation",
                        "ET MC Records are not in expected format"
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                            }
                        });
                }
            }
            if(found){
                return jsonObject;
            }else{
                UIFuncs.errorSound(getContext());
                new AlertBox(getContext()).getBox("Barcode Validation",
                        "Only "+secondItemEan.getText().toString().trim()+" is allowed to scan"
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                            }
                        });
                return null;
            }
        }
        else {*/
        if (mEtEanData != null) {
            for (int i = 0; i < mEtEanData.length(); i++) {
                try {
                    JSONObject tempObject = mEtEanData.getJSONObject(i);
                    String matchBarcode = tempObject.getString("EAN11"); // MATNR, UMREZ
                    if (matchBarcode.equals(scannedArticle.trim())) {
                        jsonObject = tempObject;
                        break;
                    }
                } catch (JSONException jsone) {
                    new AlertBox(getContext()).getBox("Barcode Validation",
                            "Incorrect Barcode or Article not found."
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    artNoEditText.setText("");
                                    artNoEditText.requestFocus();
                                }
                            });
                }
            }
        }
            /*if(jsonObject == null){
                UIFuncs.errorSound(getContext());
                new AlertBox(getContext()).getBox("Barcode Validation",
                        "Incorrect Barcode or Article not found."
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                            }
                        });
            }
        }*/
        return jsonObject;
    }

    JSONObject findDeliveryLine(String matnr) {
        JSONObject jsonObject = null;

        if (mEtLips != null) {
            for (int i = 0; i < mEtLips.length(); i++) {
                try {
                    JSONObject tempObject = mEtLips.getJSONObject(i);
                    String lineMatnr = tempObject.getString("MATNR");
                    // lineMatnr = lineMatnr.replaceFirst("^0+(?!$)", "");
                    if (matnr.equals(lineMatnr)) {
                        jsonObject = tempObject;
                        break;
                    }
                } catch (JSONException jsone) {

                }
            }
        }
        return jsonObject;
    }

    void processingFirstTimeData(int articleCount, String binMatnrKey, String matnr, JSONObject eanJson) {

        JSONObject deliveryLine = findDeliveryLine(matnr);
        if(deliveryLine!=null) {
            String[] parts = binMatnrKey.split(",");// binMatnrKey.split("\\#");
            if(parts[0].length() == 0){
                new AlertBox(getContext()).getBox("Empty Bin",
                        "BIN cannot be empty."
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                            }
                        });
                return;
            }
            trQCount = trQCount - articleCount;
            tsqCount = tsqCount + articleCount;

            sqEditTextField.setText("" + tsqCount);
            rqEditTextFiled.setText("" + trQCount);

            currentScanArticle.setText(matnr);
            currentScanQuantity.setText(secondItemRemainQty.getText().toString());
            currentScanOpenQuantity.setText("" + articleCount);

            sqtyEditText.setText("" + articleCount);

            scanMap.put(binMatnrKey, new Integer(articleCount));
            try {
                addDataForUpdateInSAP(matnr, deliveryLine.getString("CHARG"),
                        deliveryLine.getString("WERKS"), deliveryLine.getString("LGORT"), mPackingNo,
                        eanJson.getString("UMREZ"), deliveryLine.getString("VRKME"), parts[0] );
            } catch(JSONException jsone) {
            }
        } else {
            Log.d(TAG, "processingFirstTimeData(): deliveryLine  for "  + matnr + ", not found." );
        }
    }

    void processingContainData(int articleCount, String binMatnrKey, String matnr, JSONObject eanJson) {

        JSONObject deliveryLine = findDeliveryLine(matnr);
        if (deliveryLine != null) {

            String[] parts = binMatnrKey.split(",");// binMatnrKey.split("\\#");
            if(parts[0].length() == 0){
                new AlertBox(getContext()).getBox("Empty Bin",
                        "BIN cannot be empty."
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                artNoEditText.setText("");
                                artNoEditText.requestFocus();
                            }
                        });
                return;
            }

            trQCount = trQCount - articleCount;
            tsqCount = tsqCount + articleCount;

            sqEditTextField.setText("" + tsqCount);
            rqEditTextFiled.setText("" + trQCount);

            int prevCount = scanMap.get(binMatnrKey);
            articleCount = prevCount + articleCount;

            currentScanArticle.setText(matnr);
            currentScanQuantity.setText(secondItemRemainQty.getText().toString());
            currentScanOpenQuantity.setText("" + articleCount);

            sqtyEditText.setText("" + articleCount);

            scanMap.put(binMatnrKey, new Integer(articleCount));

            try {
                addDataForUpdateInSAP(matnr, deliveryLine.getString("CHARG"),
                        deliveryLine.getString("WERKS"), deliveryLine.getString("LGORT"), mPackingNo,
                        eanJson.getString("UMREZ"), deliveryLine.getString("VRKME"), parts[0] );
            } catch(JSONException jsone) {

            }
        } else {
            Log.d(TAG, "processingContainData(): deliveryLine  for " + matnr + ", not found.");
        }

    }

    void addDataForUpdateInSAP(String matnr, String charg, String werks, String lgort, String material, String tmeng, String vrkme, String bin) {
        JSONObject itJson = new JSONObject();

        try {
            itJson.put("MATNR", matnr);
        } catch(JSONException jsone) {

        }
        try {
            itJson.put("CHARG", charg);
        } catch(JSONException jsone) {

        }

        try {
            itJson.put("WERKS", werks);
        } catch(JSONException jsone) {

        }

        try {
            itJson.put("LGORT",  lgort);
        } catch(JSONException jsone) {

        }

        try {
            itJson.put("P_MATERIAL", material);
        } catch(JSONException jsone) {

        }

        try {
            itJson.put("TMENG", tmeng);
        } catch(JSONException jsone) {

        }

        try {
            itJson.put("VRKME", vrkme);
        } catch(JSONException jsone) {

        }

        try {
            itJson.put("RFBEL", bin);
        } catch(JSONException jsone) {
            return;
        }

        scannedDataForSubmit.put(itJson);
    }


    void saveScannedData() {

        final RequestQueue mRequestQueue;

        String rfc = this.mode.equals(Vars.TVS_PAPER_LESS) ? "ZWM_CREATE_HU_AND_ASSIGN_TVS" : "ZWM_CREATE_HU_AND_ASSIGN";
        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";


        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname",rfc);
            params.put("IM_VBELN", mDeliveryNumber);
            params.put("IM_USER",  loginUser);
            params.put("IM_EXIDV", mExternalHu);
            params.put("IT_DATA", scannedDataForSubmit);

        } catch (JSONException e) {
            e.printStackTrace();

            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
            return;
        }

        // set IT_BIN_EMPTY info
        JSONArray itBinEmpty = new JSONArray();
        if(this.mEtBinMc!=null && this.mEtBinMc.length()>1) {
            for (int i = 1; i < this.mEtBinMc.length(); i++) {
                try {
                    JSONObject etBin = this.mEtBinMc.getJSONObject(i);
                    String binVLPlA = etBin.getString("VLPLA");

                    if(binVLPlA!=null && binVLPlA.length()>0) {
                        if (emptyBinMap != null && emptyBinMap.get(binVLPlA) != null) {
                            // we need to add this in IT_BIN_EMPTY
                            etBin.put("BIN_EMPTY_I", "X");
                            itBinEmpty.put(etBin);
                        }
                    }
                } catch(Exception e) {

                }
            }
        }

        try {
            params.put("IT_BIN_EMPTY", itBinEmpty);
        } catch(Exception jsone) {

        }

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait, saving...");
        dialog.setCancelable(false);
        dialog.show();

        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        JsonObjectRequest mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }

                try {
                    if (responsebody == null) {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", "No response from Server");
                    } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Err", "Unable to Connect Server/ Empty Response");
                        return;
                    } else {

                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                            if (returnobj != null) {
                                String type = returnobj.getString("TYPE");
                                if (type != null)
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        AlertBox box = new AlertBox(getContext());
                                        if(mode.equalsIgnoreCase(Vars.PAPER_LESS)){
                                            box.getBox("", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
//                                                    clear();
//                                                    fm.popBackStack();
                                                    resetFields();
                                                }
                                            });
                                        }else{
                                            JSONObject huObj = responsebody.getJSONObject("EX_HUDATA");
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    printHu(huObj);
                                                }
                                            });
                                        }
                                    }
                            }
                        }

                    }
                } catch (Exception e){
                    e.printStackTrace();
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

        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy(AppConstants.VOLLEY_TIMEOUT, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        
        mRequestQueue.add(mJsonRequest);

        Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
        Log.d(TAG, "jsonRequest getBodyContentType->" + mJsonRequest.getBodyContentType());
        Log.d(TAG, "jsonRequest getBody->" + mJsonRequest.getBody().toString());
        Log.d(TAG, "jsonRequest getMethod->" + mJsonRequest.getMethod());
        try {
            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();

            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }

            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);

        }
    }

    private void printHu(JSONObject huObj) {
        TSPLPrinter printer = new TSPLPrinter(getContext());
        //4B-2033PA-BFA4
        printer.sendPrintCommandToBluetoothPrinter(this.tvsprinter, huObj);
        resetFields();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {

        // closeBarcodeReader();

        super.onStop();
    }


    // chainway coding
    public void initializeChainway() {
        barcode2D=new Barcode2D(getActivity());

        new ChainwayBarcodeReader().execute();
    }

    public class ChainwayBarcodeReader extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;
        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            // openBarcodeReader();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mypDialog.cancel();
        }
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            mypDialog = new ProgressDialog(getActivity());
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("init...");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.setCancelable(false);
            mypDialog.show();
        }
    }


    private void startBarcodeScan(){
       // barcode2D.startScan(getContext());
    }

    private void stopBarcodeScan(){
        barcode2D.stopScan(getContext());
    }

    private void openBarcodeReader(){
        Log.d(TAG, "openBarcodeReader()" );
       // barcode2D.open(getContext(),this);
    }

    private void closeBarcodeReader(){
        Log.d(TAG, "closeBarcodeReader()" );

        // barcode2D.stopScan(getContext());
        // barcode2D.close(getContext());
    }

    public void getBarcode(String barcode) {
        Log.d(TAG, barcode);

        if(barcode!=null && barcode.length()>0 && !barcode.equalsIgnoreCase("Scan fail")) {
            if(chainwayContextEditText!=null) {
                chainwayContextEditText.setText(barcode);

                if(chainwayContextEditText == binEditText) {
                    if(emptyBinMap.get(barcode)!=null) {
                        checkBoxEmptyBin.setChecked(true);
                    } else {
                        checkBoxEmptyBin.setChecked(false);
                    }
                    if (handleBinScannning(binEditText.getText().toString())) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                artNoEditText.requestFocus();
                            }
                        });
                    } else {
                        // show popup message
                        new AlertBox(getContext()).getBox("Bin Validation", "Incorrect Bin", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                binEditText.setText("");
                                binEditText.requestFocus();
                                checkBoxEmptyBin.setChecked(false);
                            }
                        });
                    }
                } else if( chainwayContextEditText == artNoEditText ) {
                        if(handleArticleScanning(artNoEditText.getText().toString())) {

                        } else {
                            artNoEditText.setText("");
                        }
                }
            }
        }

    }
}