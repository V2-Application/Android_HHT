package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtBinModel;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtPoDataModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;
import com.v2retail.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Scan_grc_putway_Process_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Scan_grc_putway_Process_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Scan_grc_putway_Process_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "bin_no";
    private static final String ARG_PARAM2 = "po_data";
    private static final String ARG_PARAM3 = "hhu_qty";
    private static final String ARG_PARAM4 = "ean";

    ArrayList<ArrayList<String>> dtPO;
    ArrayList<ArrayList<String>> dtEAN;
    ArrayList<String> dtBin;
    ArrayList<ArrayList<String>> dtMaterial;
    // TODO: Rename and change types of parameters

    ArrayList<Integer> rows_index = new ArrayList<>();
    private String TAG = Scan_grc_putway_Process_Fragment.class.getName();

    Tables table = new Tables();
    ProgressDialog dialog;
    AlertBox box;
    String URL = "";
    String WERKS = "";
    String USER = "";
    TextView mResponseView;
    Context con;
    FragmentManager fm;
    Tables tables = new Tables();
    Button back;
    Button save;
    int ScanQty = 0;
    int OpenQty = 0;
    int ScQty = 0;
    EditText hu_no_et;
    EditText total_hu_et;
    EditText bin_et;
    EditText barcode_art_et;
    EditText article_no_et;
    EditText ahq_et;
    EditText ahoq_et;
    EditText sq_et;
    EditText tsq_et;
    int sum = 0;
    String scanner;
    String hu_no;
    String total_hu;
    String barcode_art;
    private ArrayList<EtBinModel> bin = new ArrayList<>();
    private ArrayList<EtEanDataModel> ean = new ArrayList<>();
    private List<EtPoDataModel> poData = new ArrayList<>();
    private String scanBin="";
    private JSONArray jsonArray ;


    private OnFragmentInteractionListener mListener;

    public Scan_grc_putway_Process_Fragment() {
        // Required empty public constructor

    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Scan GRC Putway");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OutWardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static Scan_grc_putway_Process_Fragment newInstance(String param1, String param2) {
        Scan_grc_putway_Process_Fragment fragment = new Scan_grc_putway_Process_Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

            bin = (ArrayList<EtBinModel>) getArguments().getSerializable("bin");
            ean = (ArrayList<EtEanDataModel>) getArguments().getSerializable("ean");
            total_hu = Util.convertToDoubleString(getArguments().getString("TotalHuQty"));
            hu_no = getArguments().getString("huNumber");
            poData = (ArrayList<EtPoDataModel>) getArguments().getSerializable("poData");
            jsonArray = new JSONArray();

        }
        fm = getFragmentManager();
        dtMaterial = table.getMATTAble();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scan_grc_putway, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);
        back = (Button) view.findViewById(R.id.back);

        save = (Button) view.findViewById(R.id.save);


        hu_no_et = (EditText) view.findViewById(R.id.hu_no);
        total_hu_et = (EditText) view.findViewById(R.id.hu_total_qty);
        bin_et = (EditText) view.findViewById(R.id.bin_no);
        article_no_et = (EditText) view.findViewById(R.id.article_no);
        ahoq_et = (EditText) view.findViewById(R.id.ahoq);
        ahq_et = (EditText) view.findViewById(R.id.ahq);
        sq_et = (EditText) view.findViewById(R.id.sq);
        tsq_et = (EditText) view.findViewById(R.id.tsq);
        barcode_art_et = (EditText) view.findViewById(R.id.barcode_et);

        mResponseView = (TextView) view.findViewById(R.id.response);



            if (total_hu != null && !total_hu.equals("")) {
                total_hu_et.setText(Util.convertToDoubleString(total_hu));
            }
            if (!hu_no.equals("")) {
                hu_no_et.setText(hu_no);


        }
        back.setOnClickListener(this);
        save.setOnClickListener(this);

        onEditListener();
        addTextChangeListeners();
        bin_et.requestFocus();
        return view;

    }

    private void onEditListener() {

        bin_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = bin_et.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan HU Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(bin_et.getWindowToken(), 0);
                            loadBinData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });

        barcode_art_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = barcode_art_et.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan HU Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(barcode_art_et.getWindowToken(), 0);
                            loadBarcodeData();
                        }catch (Exception e){
                            box.getErrBox(e);
                        }


                        return true;
                    }
                }

                return false;
            }
        });
    }
    private void addTextChangeListeners() {

        bin_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 2) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String crateNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Hu Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBinData();
                        }
                    });
                }
            }
        });
        barcode_art_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( (before==0 && start ==0) && count > 2) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String crateNumber = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned Hu Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadBarcodeData();
                        }
                    });
                }
            }
        });
    }

    private void showProgress(){
        dismissDialog();
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
    }

    private void dismissDialog(){
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }
    }

    private void loadBinData(){
        showProgress();
        String binStr = bin_et.getText().toString();
        if (binStr.equals("") || binStr.length() == 0 || binStr == null) {
            dismissDialog();
            box.getBox( "Alert","Scan Bin Number");
            return;
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                    scanBinData(binStr);

                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }

    private void loadBarcodeData(){
        showProgress();
        String barcodeStr = barcode_art_et.getText().toString();
        if (barcodeStr==null||barcodeStr.equals("")||barcodeStr.isEmpty()){
            dismissDialog();
            box.getBox( "Alert","Scan BarCode");
            return;
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    scanBarcodeData(barcodeStr);
                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }

    private void scanBinData(String binStr){
        boolean flag = true;
        for (int i = 0; i< bin.size();i++){
            if (bin.get(i).getLGPLA().equals(binStr)){
                barcode_art_et.requestFocus();
                scanBin = binStr;
                flag = false;
            }
        }
        if (flag){
            box.getBox( "Alert","Incorrect Bin");
            bin_et.setText("");
            bin_et.requestFocus();
        }
        dismissDialog();
    }

    private boolean matnrEquals(String matnr1, String matnr2) {
        if (matnr1 == null || matnr2 == null) {
            return false;
        }
        return UIFuncs.removeLeadingZeros(matnr1.trim())
                .equalsIgnoreCase(UIFuncs.removeLeadingZeros(matnr2.trim()));
    }

    private EtPoDataModel findPoData(String matnr) {
        for (EtPoDataModel po : poData) {
            if (matnrEquals(po.getMATNR(), matnr)) {
                return po;
            }
        }
        return null;
    }

    private void addAndScanEan(JSONObject responsebody){
        try{
            box = new AlertBox(con);
            EtEanDataModel eanModel = null;
            JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
            String scannedEan = UIFuncs.toUpperTrim(barcode_art_et);
            JSONObject jsonObject = null;
            for (int i = SapJsonRows.startIndex(ET_EAN_DATA, "EAN11", "MATNR"); i < ET_EAN_DATA.length(); i++) {
                JSONObject row = ET_EAN_DATA.getJSONObject(i);
                if (SapJsonRows.isMetadataRow(row, "EAN11", "MATNR")) {
                    continue;
                }
                if (row.getString("EAN11").equalsIgnoreCase(scannedEan)) {
                    jsonObject = row;
                    break;
                }
            }
            if (jsonObject == null && ET_EAN_DATA.length() == 1) {
                jsonObject = ET_EAN_DATA.getJSONObject(0);
            }
            if(jsonObject != null){
                String EAN11 = jsonObject.getString("EAN11");
                String MATNR = jsonObject.getString("MATNR");
                String UMREZ = jsonObject.getString("UMREZ");
                for (EtEanDataModel existingEan: ean) {
                    if(existingEan.getEAN11().equals(EAN11)){
                        eanModel = existingEan;
                        eanModel.setNewEan("Validate");
                    }
                }
                if(eanModel == null){
                    String MANDT = jsonObject.getString("MANDT");
                    String EANNR = jsonObject.getString("EANNR");
                    eanModel = new EtEanDataModel(MATNR,UMREZ,MANDT,EAN11,EANNR);
                    eanModel.setNewEan("Validate");
                    ean.add(eanModel);
                }
                EtPoDataModel model = findPoData(MATNR);
                if (model == null) {
                    dismissDialog();
                    box.getBox("Alert", "HU quantity not found for this article");
                    return;
                }
                scanBarcodeData(UIFuncs.toUpperTrim(barcode_art_et));
            }else{
                dismissDialog();
                box.getBox("No Data Found", "Do data returned by server matching this EAN");
            }
        }catch(Exception exce){
            dismissDialog();
            box.getErrBox(exce);
        }
    }

    private void scanBarcodeData(String barcodeStr){

        String EAMMAterial = "";
        String UMREZ = "";
        String VEMNG ="";
        String BDMNG = "";
        boolean check = true;
        boolean notFound = true;
        if (scanBin.equals("")||scanBin.isEmpty()){
            dismissDialog();
            box.getBox( "Alert","First Scan Bin");
            return;
        }
        for (int i = 0; i < ean.size(); i++) {
            if (ean.get(i).getEAN11().equalsIgnoreCase(barcodeStr.trim())) {
                EtEanDataModel eanDataModel = ean.get(i);
                article_no_et.setText(eanDataModel.getMATNR());
                UMREZ = eanDataModel.getUMREZ();
                int umrez = (int) Util.convertStringToDouble(UMREZ);
                EAMMAterial = eanDataModel.getMATNR();
                if(eanDataModel.getNewEan() != null){
                    if(eanDataModel.getNewEan().equals("New")){
                        sendEanValidationRequest(barcodeStr);
                        return;
                    }else{
                        eanDataModel.setNewEan("New");
                    }
                }
                EtPoDataModel poModel = findPoData(EAMMAterial);
                if (poModel == null) {
                    dismissDialog();
                    box.getBox("Alert", "HU quantity not found for this article");
                    return;
                }
                VEMNG = poModel.getVEMNG();
                ahq_et.setText(Util.convertToDoubleString(VEMNG));
                BDMNG = poModel.getBDMNG();
                int bdmng = (int) Util.convertStringToDouble(BDMNG);
                try {
                    for (int a=0;a<jsonArray.length();a++){
                        JSONObject jsonObject1 = jsonArray.getJSONObject(a);
                        String m = jsonObject1.getString("MATNR");
                        String bin = jsonObject1.getString("LGPLA");
                        if (matnrEquals(m, EAMMAterial) && bin.equals(scanBin)){
                            String sq = jsonObject1.getString("VEMNG");
                            sum = (int) Util.convertStringToDouble(sq) + umrez;
                            if (sum<=bdmng) {
                                jsonObject1.put("VEMNG", String.valueOf(sum));
                            }
                            check=false;
                            break;
                        }
                    }
                    if (check) {
                        sum = umrez;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

                if (sum<=bdmng){
                    ScQty = ScQty+  umrez;
                    tsq_et.setText(String.valueOf(ScQty));
                    sq_et.setText(String.valueOf(sum));
                    ahoq_et.setText(String.valueOf(bdmng - sum));
                    try {
                        if (check){
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("VEMNG",String.valueOf(umrez));
                            jsonObject.put("MATNR",EAMMAterial);
                            jsonObject.put("LGPLA",scanBin);
                            jsonArray.put(jsonObject);
                        }
                        Log.v(TAG,"Save Data-->"+jsonArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }else {
                    dismissDialog();
                    box.getBox("Alert", "Scanned Qty can't be greater than Open Qty!");
                }

                notFound = false;
                break;
            }
        }

        if (notFound){
            sendEanValidationRequest(barcodeStr);
            return;
        }
        barcode_art_et.setText("");
        barcode_art_et.requestFocus();
        dismissDialog();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())

        {
            case R.id.save:

                try {
                    saveNetworkCall();
                } catch (Exception e) {
                    box.getErrBox(e);
                }

                break;
            case R.id.back:
                fm.popBackStack();
                break;

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
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
                switch (scanner)

                {
                    case "bar":

                        if (!(bin_et.equals("") || bin_et.length() < 0 || bin_et.equals(null))) {
                            barcode_art_et.setText(scanContent);
                            //bar_scan.setText("Rescan");
                            Log.v(TAG, "bar code = " + scanContent);
                            // Log.v(TAG,"stored bar code = " + barcode_art);

                            try {
                            } catch (Exception e) {
                                box.getErrBox(e);
                                return;
                            }
                        } else {

                            box.getBox("Alert!!", "First Scan Bar Number");
                            return;
                        }

                        break;
                    case "bin":
                        Log.v(TAG, "bin code = " + scanContent);

                        if (dtBin.contains(scanContent))
                            bin_et.setText(scanContent);
                        else {

                            box.getBox("Alert", "Incorrect Bin!");
                            return;
                        }

                        break;
                }

            } else {

                box.getBox("Scanner Err", "No Content Received. Please Scan Again");

            }
        }


    }

    private void sendAndRequestResponse() {


        String rfc = "ZWM_STORE_GRC_PUTWAY";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EXIDV",hu_no_et.getText().toString() );
            params.put("IM_WERKS", WERKS);
            params.put("IM_USER", USER);
            params.put("IT_DATA", jsonArray);


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
                                        return;
                                    } else {

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Alert", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                fm.popBackStack();
                                                bin_et.setText("");
                                                article_no_et.setText("");
                                                barcode_art_et.setText("");
                                                sq_et.setText("");
                                                tsq_et.setText("");
                                                ahoq_et.setText("");
                                                scanBin="";
                                                ScQty =0;
                                                ScanQty = 0;
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
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    private void sendEanValidationRequest(String ean) {


        String rfc = "ZWM_RFC_STORE_EAN_DATA";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_WERKS", WERKS);
            params.put("IM_USER", USER);
            params.put("IM_EAN11", ean);


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
                                        barcode_art_et.setText("");
                                        barcode_art_et.requestFocus();
                                        return;
                                    } else {
                                        addAndScanEan(responsebody);
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
        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 30000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    void saveNetworkCall() {
        showProgress();

        if (jsonArray.length()<=0){
            dismissDialog();
            box.getBox("Alert", "Scan All Details");
            return;
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    sendAndRequestResponse();
                } catch (Exception e) {
                    box.getErrBox(e);
                }
            }
        }, 2000);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStack();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
