package com.v2retail.dotvik.dc;

import static android.content.Context.MODE_PRIVATE;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.LinearLayout;
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
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtPoDataModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ValidateCrate_Process_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ValidateCrate_Process_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */

/**
 * @author Narayanan
 * @version 11.73
 * {@code Author: Narayanan, Revision: 2, Modified: 24th Aug 2024}
 * Changes: Added logic to check and allow Mix article scan if EtPoDataModel.MXALOW is X
 */
public class ValidateCrate_Process_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final int REQUEST_GET_CRATE_DATA = 1023;
    private static final String ARG_PARAM1 = "pono";
    private static final String ARG_PARAM2 = "pno";
    private static final String ARG_PARAM3 = "ean";
    private static final String ARG_SCREEN = "screen";

    String bol = "";
    String requester = "";
    String StorageType = "";
    String MAterialDesc = "";
    ArrayList<ArrayList<String>> dtPO;
    ArrayList<ArrayList<String>> dtEAN;
    ArrayList<ArrayList<String>> dtTemp;
    ArrayList<ArrayList<String>> dt81;
    ArrayList<ArrayList<String>> dtSeries81;
    ArrayList<String> dtPONO;
    ArrayList<ArrayList<String>> dtMaterial;
    List<EtPoDataModel> etPoDataModels,saveData;
    List<EtEanDataModel> etEanDataModels;
    ArrayList<String> SQdata;
    // TODO: Rename and change types of parameters
    ArrayList<Integer> rows_index = new ArrayList<>();
    private String TAG = ValidateCrate_Process_Fragment.class.getName();
    Tables table = new Tables();
    TextView mResponseView;
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;
    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();

    Button back;
    Button submit;

    private boolean check;
    EditText po_et;
    EditText ven_inv_et;
    EditText bill_no_et;
    EditText ge_et;
    EditText crate_et;
    EditText article_no_et;
    EditText curCrate_et;
    EditText curBin_et;
    EditText sq_et;
    EditText tsq_et;
    EditText rq_et;
    EditText tpoq_et,lastScan;
    EditText bin_et;
    TextView tv_bin,tv_lastscanned;
    LinearLayout ll_tqty_poqty;

    int ScanQty = 0;
    int OpenQty = 0;
    int ScQty = 0;
    int sum = 0;
    int qty = 0;
    int screen = 1;
    String scanner;
    int poQty =0;
    JSONArray jsonArray = new JSONArray();
    private OnFragmentInteractionListener mListener;

    public ValidateCrate_Process_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Validate Crate");
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
    public static ValidateCrate_Process_Fragment newInstance(String param1, String param2) {
        ValidateCrate_Process_Fragment fragment = new ValidateCrate_Process_Fragment();
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
            //dtPONO = (ArrayList<String>) getArguments().getSerializable(ARG_PARAM1);
//            dtPO = (ArrayList<ArrayList<String>>) getArguments().getSerializable(ARG_PARAM2);
//            dtEAN = (ArrayList<ArrayList<String>>) getArguments().getSerializable(ARG_PARAM3);
            etEanDataModels = new ArrayList<>();
            etPoDataModels = new ArrayList<>();
            etEanDataModels = (List<EtEanDataModel>) getArguments().getSerializable("ean");
            etPoDataModels = (List<EtPoDataModel>) getArguments().getSerializable("pno");
            screen = getArguments().getInt(ARG_SCREEN);
//            if (dtPONO != null && dtPONO.size() > 0)
//                Log.d(TAG, "pno array data :" + dtPONO.size());
//            if (dtPO != null && dtPO.size() > 0)
//                Log.d(TAG, "po array data :" + dtPO.size() + " " + dtPO.get(0).size());
//            if (dtEAN != null && dtEAN.size() > 0)
//                Log.d(TAG, "ean array data :" + dtEAN.size() + " " + dtEAN.get(0).size());


        }
        fm = getFragmentManager();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.validate_crate, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");
        if(!URL.isEmpty())
            Log.d(TAG,"URL->"+URL);
        if(!WERKS.isEmpty())
            Log.d(TAG,"WERKS->"+WERKS);
        if(!USER.isEmpty())
            Log.d(TAG,"USER->"+USER);
        SQdata = new ArrayList<>();
        back = (Button) view.findViewById(R.id.back); //btnExit
        submit = (Button) view.findViewById(R.id.submit);//submit

        po_et = (EditText) view.findViewById(R.id.po);//pono
        ven_inv_et = (EditText) view.findViewById(R.id.ven_inv);//inv_text
        bill_no_et = (EditText) view.findViewById(R.id.bill_no);//bill_no
        ge_et = (EditText) view.findViewById(R.id.ge);//gate_entry
        crate_et = (EditText) view.findViewById(R.id.crate_no);//crate


        curBin_et = (EditText) view.findViewById(R.id.cur_bin);//curbin
        article_no_et = (EditText) view.findViewById(R.id.article_no);//material
        curCrate_et = (EditText) view.findViewById(R.id.cur_crate);//curcrate
        sq_et = (EditText) view.findViewById(R.id.sq);//ts
        rq_et = (EditText) view.findViewById(R.id.rq);//to
        tsq_et = (EditText) view.findViewById(R.id.tsq);//tsq
        tpoq_et = (EditText) view.findViewById(R.id.tpoq);//tpoqty
        lastScan = view.findViewById(R.id.lastScan);
        tv_bin = view.findViewById(R.id.label_bin);
        tv_lastscanned = view.findViewById(R.id.label_lastscanned);
        ll_tqty_poqty = view.findViewById(R.id.ll_tqty_poqty);

        if(screen == 2){
            tv_bin.setText("Scanned Crate");
            tv_lastscanned.setText("Description");
            ll_tqty_poqty.setVisibility(View.INVISIBLE);
            curBin_et.setEnabled(false);
            article_no_et.setEnabled(false);
            sq_et.setEnabled(false);
            rq_et.setEnabled(false);
            curBin_et.setBackgroundColor(getResources().getColor(R.color.viewBg));
            curBin_et.setHint("Last Crate");
            article_no_et.setBackgroundColor(getResources().getColor(R.color.viewBg));
            sq_et.setBackgroundColor(getResources().getColor(R.color.viewBg));
            curBin_et.setBackgroundColor(getResources().getColor(R.color.viewBg));
            rq_et.setBackgroundColor(getResources().getColor(R.color.viewBg));
            lastScan.setBackgroundColor(getResources().getColor(R.color.viewBg));
            lastScan.setHint("Description");
            curCrate_et.setVisibility(View.GONE);
            crate_et.requestFocus();
        }

        lastScan.setEnabled(false);
        po_et.setEnabled(false);
        ven_inv_et.setEnabled(false);
        bill_no_et.setEnabled(false);
        ge_et.setEnabled(false);

        mResponseView = (TextView) view.findViewById(R.id.response);

        back.setOnClickListener(this);
        submit.setOnClickListener(this);

        addEditorListeners();
        addTextChangeListners();

        loadData();
        return view;

    }


    void addEditorListeners() {

        crate_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;

                    String crate = crate_et.getText().toString();
                    if (!(crate.equals("") || crate.length() < 0 || crate.equals(null))) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(crate_et.getWindowToken(), 0);
                            if(screen == 2){
                                validateCrate();
                            }else{
                                loadCrateData();
                            }
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                        return true;
                    } else {
                        box.getBox("Alert!!", "First Scan Bar Number");
                    }
                }
                return false;
            }
        });
        article_no_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    // handle only when key pressed.
                    if(keyEvent!=null && keyEvent.getAction()==KeyEvent.ACTION_DOWN) return true;
                    if(screen != 2){
                        String article = article_no_et.getText().toString();
                        if (!(article.equals("") || article.length() < 0 || article.equals(null))) {
                            try {
                                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(article_no_et.getWindowToken(), 0);
                                loadArticleData();
                            } catch (Exception e) {
                                box.getErrBox(e);
                            }
                            return true;
                        } else {
                            box.getBox("Alert!!", "First Scan Bar Number");
                        }
                    }

                }
                return false;
            }
        });

    }

    void addTextChangeListners() {
        crate_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                String poString = s.toString();
                if (scannerReading) {
                    Log.d(TAG, "Scanned poString : " + poString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(screen == 2){
                                validateCrate();
                            }else{
                                loadCrateData();
                            }
                        }
                    });
                }
            }
        });


        article_no_et.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 6) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(screen != 2) {
                    String poString = s.toString();
                    if (scannerReading) {
                        Log.d(TAG, "Scanned poString : " + poString);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadArticleData();
                            }
                        });
                    }
                }
            }
        });

    }


    private void loadData() {

        if (etPoDataModels==null|| etEanDataModels==null){
            box.getBox("Alert", "Data Didn't Receive Properly \n please try again");
            return;
        }
//        if (dtPO == null) {
//            box.getBox("Alert", "Data Didn't Receive Properly \n please try again");
//            return;
//        }
//        if (dtEAN == null) {
//            box.getBox("Alert", "Data Didn't Receive Properly \n please try again");
//            return;
//        }
//        if (dtMaterial != null) dtMaterial = null;
//        if (dtPONO == null) {
//            box.getBox("Alert", "Data Didn't Receive Properly \n please try again");
//            return;
//        }


        po_et.setText(getArguments().getString("po_et"));
        ven_inv_et.setText(getArguments().getString("invoice_et"));
        bill_no_et.setText(getArguments().getString("bno_et"));
        ge_et.setText(getArguments().getString("ge_et"));

        bol = getArguments().getString("bol_et");
        if (etPoDataModels!=null) {
            for (int i = 0; i < etPoDataModels.size(); i++) {

                poQty += Double.valueOf(etPoDataModels.get(i).getPO_QTY()).intValue();

            }
        }
        tpoq_et.setText(String.valueOf(poQty));

        crate_et.requestFocus();

    }


    @Override
    public void onClick(View view) {
        switch (view.getId())

        {
            case R.id.submit:
                try {
                    saveDataToServer();
                }catch (Exception e)
                {
                    box.getErrBox(e);
                }
                break;
            case R.id.back:
                AlertBox box = new AlertBox(getContext());
                box.getBox("Error", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // fm.popBackStack();
                        //  ApplicationController.getInstance().refreshObservable().notifyObservers();

                        fm.popBackStack();

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, TAG + " scanned result...");
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {

            box.getBox("Scanner Err","Unable to receive Data");

        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null) {
                switch (scanner) {
                    case "crate":
                        crate_et.setText(scanContent);
                        try {
                            loadCrateData();
                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }
                        break;
                    case "article":

                        article_no_et.setText(scanContent);
                        try {
                            loadArticleData();
                        }catch (Exception e)
                        {
                            box.getErrBox(e);
                        }
                        break;
                }
            } else {
                box.getBox("Scanner Err","No Content Received. Please Scan Again");

            }
        }


    }

    private void loadArticleData() {

        String article = article_no_et.getText().toString().trim();
        if (TextUtils.isEmpty(article_no_et.getText().toString().trim())|| article.equals("")) {
            box.getBox("Alert", "Please Enter Article No");
            return;
        }

        try {
            processOther();
        } catch (Exception e)  {
            box.getErrBox(e);
        }
    }

    private void processOther() {
        String article = article_no_et.getText().toString().trim();
        String crate = crate_et.getText().toString();
        String getEANNR ="";
        String getEAMMAterial ="";
        String getScanQty ="";
        String UMREZ ="";
        int newOpenQty =0;
        int flag =0;
        int flag1 =0;

        if (!crate.equals("") && !crate.isEmpty()) {
            for(int i=0;i<etEanDataModels.size();i++){
                if (etEanDataModels.get(i).getEAN11().equals(article)){
                    getEANNR = etEanDataModels.get(i).getEANNR();
                    getEAMMAterial = etEanDataModels.get(i).getMATNR();
                    UMREZ = etEanDataModels.get(i).getUMREZ();
                    flag =1;
                    break;
                }
            }

            if (flag==1){
                if (getEANNR.equals(StorageType)) {
                    boolean mixallowed = etPoDataModels.get(0).getMXALOW().equals("X");
                    for (int i = 0; i < etPoDataModels.size(); i++) {
                        if (etPoDataModels.get(i).getMATERIAL().equals(getEAMMAterial)) {
                            lastScan.setText(article+","+getEAMMAterial);
                            MAterialDesc = etPoDataModels.get(i).getMAT_DESC();
                            OpenQty = Double.valueOf(etPoDataModels.get(i).getOPEN_QTY()).intValue();
                            newOpenQty = OpenQty;
                            flag1 =1;
                            rq_et.setText(String.valueOf(OpenQty));
                            curCrate_et.setText(MAterialDesc);
                            getScanQty = etPoDataModels.get(i).getSCAN_QTY();
                            boolean check =true;
                            try {
                                for (int a=0;a<jsonArray.length();a++){
                                    JSONObject jsonObject1 = jsonArray.getJSONObject(a);
                                    String m = jsonObject1.getString("MATERIAL");
                                    String c = jsonObject1.getString("CRATE");
                                    if(c.equals(crate)) {
                                        if (m.equals(etPoDataModels.get(i).getMATERIAL())){
                                            String sq = jsonObject1.getString("SCAN_QTY");
                                            sum = Integer.valueOf(sq);
                                            sum =  sum + Integer.valueOf(UMREZ);
                                            if (sum<=newOpenQty) {
                                                jsonObject1.put("SCAN_QTY", String.valueOf(Integer.valueOf(sq) + Integer.valueOf(UMREZ)));
                                            }
                                            check=false;
                                            break;
                                        }else{
                                            if(!mixallowed){
                                                box.getBox("Alert", "Different articles are not allowed. You can scan only article "+m+" in this crate");
                                                article_no_et.setText("");
                                                article_no_et.requestFocus();
                                                return;
                                            }
                                        }
                                    }
                                }
                                if (check) {
                                    sum = 0;
                                    sum =  sum + Integer.valueOf(UMREZ);
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if (sum <=  newOpenQty){
                                ScQty = ScQty + Integer.valueOf(UMREZ);
                                tsq_et.setText(String.valueOf(ScQty));
                                crate_et.setEnabled(false);

                                sq_et.setText(String.valueOf(sum));

                                try {

                                    if (check){
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("MAT_DESC",etPoDataModels.get(i).getMAT_DESC());
                                        jsonObject.put("SCAN_QTY",String.valueOf(Integer.valueOf(UMREZ)));
                                        jsonObject.put("UNIT",etPoDataModels.get(i).getUNIT());
                                        jsonObject.put("OPEN_QTY",etPoDataModels.get(i).getOPEN_QTY());
                                        jsonObject.put("GR_QTY",etPoDataModels.get(i).getGR_QTY());
                                        jsonObject.put("MATERIAL",etPoDataModels.get(i).getMATERIAL());
                                        jsonObject.put("PO_QTY",etPoDataModels.get(i).getPO_QTY());
                                        jsonObject.put("CRATE",crate);
                                        jsonArray.put(jsonObject);
                                    }
                                    Log.v(TAG,"Save Data-->"+jsonArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                curCrate_et.setFocusableInTouchMode(false);
                                curCrate_et.setFocusable(false);
                                crate_et.setEnabled(false);
                            } else {
                                box.getBox("Alert", "Scanned Qty can't be greater than Open Qty!");
                                return;
                            }
                            break;
                        }
                    }
                } else {
                    box.getBox("Alert", "No storage type maintain for article " + article);
                    article_no_et.setText("");
                    article_no_et.requestFocus();
                    return;
                }
            } else {
                box.getBox("Alert", "Invalid Material");
                article_no_et.setText("");
                article_no_et.requestFocus();
                return;
            }

            if (flag1==0&&flag==1){
                box.getBox("Alert", "Material " + getEAMMAterial + " not found in Po");
                article_no_et.setText("");
                article_no_et.requestFocus();
                return;
            }

        } else {
            box.getBox("Alert", "Fill Crate Number");
            crate_et.requestFocus();
            return;
        }
        article_no_et.setText("");
        article_no_et.requestFocus();
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
    private void validateCrate(){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.ZFMS_CRATE_GET_DATA);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate_et.getText().toString().toUpperCase(Locale.ROOT).trim());
            args.put("IM_EBELN", po_et.getText().toString());
            args.put("IM_XBLNR", ven_inv_et.getText().toString());
            args.put("IM_GATE_ENTRY", ge_et.getText().toString());

            args.put("IM_BILL", bill_no_et.getText().toString());
            args.put("IM_FBRNR", ven_inv_et.getText().toString());
            args.put("IM_SCREEN", "");
            args.put("IM_LGNUM", "V2S");
            args.put("IM_SAVE", "X");

            showProcessingAndSubmit(Vars.ZFMS_CRATE_GET_DATA,REQUEST_GET_CRATE_DATA,args);
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
    private void loadCrateData() {

        if(dialog==null) {
            dialog=new ProgressDialog(con);
        }
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String addrec = null;
                String crate = crate_et.getText().toString();
                if (TextUtils.isEmpty(crate_et.getText().toString().trim())&& crate.equals("")) {
                    box.getBox("Alert", "Please Enter crate No");
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    return;

                }
                addrec = po_et.getText().toString().trim() + "#" + ven_inv_et.getText().toString().trim() + "#" + crate_et.getText().toString().trim();

                requester = "crate";
                String valueRequestPayload = "scnsel#" + addrec + "#<eol>";
                Log.d(TAG, "payload : " + valueRequestPayload);
                Log.d(TAG, "payload sent to server ");
                try {
//                    sendAndRequestResponse(valueRequestPayload);
                    getCreateData();
                }catch (Exception e)
                {
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    box.getErrBox(e);
                }

            }
        }, 2000);

    }

    void getCreateData(){

        String rfc = "ZWM_VALIDATE_CRATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EBELN", po_et.getText().toString().trim());
            params.put("IM_XBLNR", ven_inv_et.getText().toString().trim());
            params.put("IM_CRATE", crate_et.getText().toString().trim());

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
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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
                                        crate_et.setText("");
                                        crate_et.requestFocus();
                                        return;
                                    } else {
                                        String EX_LGTYP = responsebody.getString("EX_LGTYP");
                                        String curBin = responsebody.getString("EX_LGPLA");
                                        curBin_et.setText(curBin);
                                        StorageType = EX_LGTYP;
                                        article_no_et.requestFocus();

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



        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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


    private void saveDataToServer() {
        if(dialog == null) {
            dialog=new ProgressDialog(con);
        }

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String msg = null;
                StringBuilder Stuz = new StringBuilder();
                String addrec = null;
                String po = po_et.getText().toString().trim();
                String ven_inv = ven_inv_et.getText().toString().trim();
                String bill_no = bill_no_et.getText().toString().trim();
                String ge = ge_et.getText().toString().trim();
                if(TextUtils.isEmpty(po_et.getText().toString().trim())&& po.equals("")
                        && TextUtils.isEmpty(ven_inv_et.getText().toString().trim())&& ven_inv.equals("")
                        && TextUtils.isEmpty(bill_no_et.getText().toString().trim())&& bill_no.equals("")
                        && TextUtils.isEmpty(ge_et.getText().toString().trim())&& ge.equals("")
                        && jsonArray.length()<=0){

                    box.getBox("Alert", "Scan All Details");
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    return;

                }

                try {
//                    sendAndRequestResponse(valueRequestPayload);
                    saveData(po,ven_inv,bill_no,ge);
                }catch (Exception e)
                {
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }

    void saveData(String po,String inv,String bill_no,String ge){

        String rfc = "ZWM_PO_SCAN_DATA_SAVE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_BILL", bill_no);
            params.put("IM_EBELN", po);
            params.put("IM_FRBNR", bol);
            params.put("IM_GATE_ENTRY", ge);
            params.put("IM_LGNUM", "");
            params.put("IM_USER", USER);
            params.put("IM_XBLNR", inv);
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
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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

//                                        gr.setText("");
//                                        gr.requestFocus();
                                        return;
                                    } else {

                                        etEanDataModels.clear();
                                        etPoDataModels.clear();
                                        OpenQty = 0;
                                        sum = 0;
                                        ScQty = 0; // this is per crate, we need to save
                                        jsonArray = new JSONArray();


                                        AlertBox box = new AlertBox(getContext());


                                        box.getBox("", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                crate_et.setText("");
                                                curBin_et.setText("");
                                                article_no_et.setText("");
                                                curCrate_et.setText("");
                                                sq_et.setText("");
                                                rq_et.setText("");
                                                tsq_et.setText("");
                                                tsq_et.setText("");
                                                lastScan.setText("");
                                                // start with new crate
                                                crate_et.setEnabled(true);
                                                crate_et.requestFocus();

                                                // need to refresth the data so that user can work on another crate
                                                getPoData(po,inv,ge,bill_no,bol);
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

        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    void getPoData(String po, String inv, String ge, String bno, String bol){

        String rfc = "ZWM_PO_GET_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EBELN", po);
            params.put("IM_XBLNR", inv);
            params.put("IM_USER", USER);
            params.put("IM_GATE_ENTRY", ge);
            params.put("IM_FRBNR", bol);
            params.put("IM_BILL", bno);



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
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

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

//                                        gr.setText("");
//                                        gr.requestFocus();
                                        return;
                                    } else {
                                        JSONArray jsonArray = responsebody.getJSONArray("ET_PO_DATA");
                                        Log.v(TAG,"ET_PO_DATA--->"+jsonArray.toString());
                                        if (jsonArray.length()>1) {
                                            for (int i = 1; i < jsonArray.length(); i++) {
                                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                String MAT_DESC = jsonObject.getString("MAT_DESC");
                                                String SCAN_QTY = jsonObject.getString("SCAN_QTY");
                                                String UNIT = jsonObject.getString("UNIT");
                                                String OPEN_QTY = jsonObject.getString("OPEN_QTY");
                                                String GR_QTY = jsonObject.getString("GR_QTY");
                                                String MATERIAL = jsonObject.getString("MATERIAL");
                                                String PO_QTY = jsonObject.getString("PO_QTY");
                                                String CRATE = jsonObject.getString("CRATE");
                                                String MIX_ALLOW = jsonObject.getString("MIX_ALLOWED");
                                                etPoDataModels.add(new EtPoDataModel(MAT_DESC, SCAN_QTY, UNIT, OPEN_QTY, GR_QTY, MATERIAL, PO_QTY, CRATE, MIX_ALLOW));
                                                poQty = poQty + Double.valueOf(PO_QTY).intValue();

                                            }
                                        }

                                        JSONArray etEanData = responsebody.getJSONArray("ET_EAN_DATA");
                                        Log.v(TAG,"ET_EAN_DATA--->"+etEanData.toString());
                                        if (etEanData.length()>1) {
                                            for (int i = 1; i < etEanData.length(); i++) {
                                                JSONObject jsonObject = etEanData.getJSONObject(i);
                                                String MANDT = jsonObject.getString("MANDT");
                                                String EANNR = jsonObject.getString("EANNR");
                                                String MATNR = jsonObject.getString("MATNR");
                                                String EAN11 = jsonObject.getString("EAN11");
                                                String UMREZ = jsonObject.getString("UMREZ");
                                                String MESRT = jsonObject.getString("MESRT");
                                                etEanDataModels.add(new EtEanDataModel(MANDT,EANNR,UMREZ,EAN11,MATNR,MESRT));
                                            }
                                        }

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


        mJsonRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 0,  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStackImmediate();
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
    private void setFieldsData(JSONObject responsebody){
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length()-1;
            if(totalEtRecords > 0){
                for(int recordIndex = 0; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex+1);
                    article_no_et.setText(ET_RECORD.getString("MATERIAL"));
                    lastScan.setText(ET_RECORD.getString("MAT_DESC"));
                    sq_et.setText(ET_RECORD.getString("SCAN_QTY"));
                    rq_et.setText(ET_RECORD.getString("OPEN_QTY"));
                }
            }
        } catch (JSONException e) {
            UIFuncs.errorSound(con);
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }
    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){

        if(dialog==null) {
            dialog = new ProgressDialog(getContext());
        }

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
                    if(dialog!=null) {
                        dialog.dismiss();
                        dialog = null;
                    }
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
        mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

                if (responsebody == null) {
                    UIFuncs.errorSound(con);
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
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
                                        UIFuncs.errorSound(con);
                                        if (request == REQUEST_GET_CRATE_DATA) {
                                            crate_et.setText("");
                                            crate_et.requestFocus();
                                        }
                                        UIFuncs.errorSound(con);
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        return;
                                    } else {
                                        if (request == REQUEST_GET_CRATE_DATA) {
                                            curBin_et.setText(crate_et.getText().toString().toUpperCase(Locale.ROOT).trim());
                                            crate_et.setText("");
                                            crate_et.requestFocus();
                                            setFieldsData(responsebody);
                                        }
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

    /*    * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}

