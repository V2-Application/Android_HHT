package com.v2retail.dotvik.store;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.google.gson.Gson;

import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import com.v2retail.db.V2RDBClient;
import com.v2retail.db.dao.ETStateDao;
import com.v2retail.db.entities.ETState;
import com.v2retail.dotvik.R;

import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.material.ETPACKMAT;

import com.v2retail.util.AlertBox;
import com.v2retail.util.AppConstants;
import com.v2retail.util.Util;
import com.v2retail.util.Barcode2D;
import com.v2retail.util.IBarcodeResult;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class PapperLessPicking extends Fragment implements IBarcodeResult, Observer {
    private final String DB_MODULE_NAME_TVS = "TVS_PAPERLESS_SCAN_LIVE_HU";
    public PapperLessPicking() { }
    private String TAG=PapperLessPicking.class.getName();
    private Bin_To_Bin_Transfer_Fragment.OnFragmentInteractionListener mListener;

    final ArrayList<String> pickStringList=new ArrayList<>();

    Button backV2, nextV2, btnClear;
    TextView title_h;
    EditText inputExternalHu;

    Spinner selectPac;
    Spinner optionDeliverySelection;
    FragmentManager fm;
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapter2;

    String requestUrl = "";
    String loginUser = "";
    String werks = "";

    ArrayList<String> deliveryList = null;

    String mPackingNumber = "";
    String mDeliveryNumber = "";

    JSONObject m_likp_JSON = null;
    JSONArray m_bin_mc_array = null;
    JSONArray m_ean_array = null;
    JSONArray m_lips_array= null;

    LinearLayout ll_external_hu;

    private List<ETPACKMAT> mETPACKMAT=new ArrayList<>();


    ProgressDialog dialog = null;
    String mode = Vars.PAPER_LESS;


    // ChainwayBarCode
    private Barcode2D barcode2D;


    public static PapperLessPicking newInstance(String mode) {
        PapperLessPicking fragment = new PapperLessPicking();
        fragment.mode = mode;
        return fragment;
    }

    String resposne="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_papper_less_picking, container, false);

        title_h=view.findViewById(R.id.title_h);

        selectPac=view.findViewById(R.id.select_pac);
        optionDeliverySelection=view.findViewById(R.id.option_delivery_selection);
        inputExternalHu=view.findViewById(R.id.input_external_hu);
        ll_external_hu = view.findViewById(R.id.ll_paperless_picking_external_hu);
        btnClear = view.findViewById(R.id.clear_scan);

        backV2=view.findViewById(R.id.back_v2);
        nextV2=view.findViewById(R.id.next_v2);

        SharedPreferencesData data = new SharedPreferencesData(getContext());

        this.requestUrl = data.read("URL");
        this.loginUser = data.read("USER");
        this.werks = data.read("WERKS");

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            resposne = bundle.getString("data", null);
            Log.d("resposneData", resposne);
        }

        deliveryList=new ArrayList<>();
        deliveryList.add("Select Delivery");

        try{
            Gson gson=new Gson();
            JSONArray jsonObject=new JSONArray(resposne);

            for(int i=1;i<jsonObject.length();i++){
                Log.d("scan", jsonObject.getString(i));
                JSONObject etDataNode = jsonObject.getJSONObject(i);
                deliveryList.add(etDataNode.getString("VBELN") + "-" +
                        etDataNode.getString("WERKS") + "-" +
                        etDataNode.getString("PRIORITY") + "-" +
                        etDataNode.getString("FLOOR"));
            }

        }catch (JSONException e){
            e.printStackTrace();
        }

        nextV2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Vars.TVS_PAPER_LESS_LHU.equalsIgnoreCase(mode)) {
                    if(!isPendingScanAvailable()){
                        validationData();
                    }
                }else{
                    validationData();
                }
            }
        });
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Are you sure you clear previously scanned data from this device?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ETStateDao stateDao = V2RDBClient.getInstance(getContext()).getV2ROfflineDB().etStateDao();
                        stateDao.clearStateByModule(DB_MODULE_NAME_TVS);
                        box.getBox("Cleared", "Previous scanned data removed successfully. Now tap on next");
                        btnClear.setVisibility(View.GONE);
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // negative

                    }
                });
            }
        });


        backV2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,pickStringList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectPac.setAdapter(adapter);


        selectPac.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if(position>0) {
                    mPackingNumber = pickStringList.get(position);
                } else {
                    mPackingNumber = "";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
            
        });

        adapter2 = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, deliveryList);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionDeliverySelection.setAdapter(adapter2);

        optionDeliverySelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {

                 Log.d(TAG, "Selected Deliver = " + deliveryList.get(position));
                 if(position>0) {
                     mDeliveryNumber = deliveryList.get(position);
                     validateDelivery(mDeliveryNumber);
                 } else {
                     mDeliveryNumber = "";
                 }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        fetchPackingMaterial();

        inputExternalHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(inputExternalHu.getWindowToken(), 0);
                    validationData();
                    return true;
                }
                return false;
            }
        });


        inputExternalHu.addTextChangedListener(new TextWatcher() {
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

                String huString = s.toString();
                if(scannerReading) {
                    Log.d(TAG, "Scanned huString : " +  huString);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            validationData();
                        }
                    });
                }
            }
        });

        inputExternalHu.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    Activity activity = getActivity();
                } else {
                }
            }
        });


        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle(Vars.PAPER_LESS.equals(mode) ? "Paperless-Validate HU" : (Vars.TVS_PAPER_LESS_LHU.equals(mode) ? "TVS Paperless-Validate Live HU" : "TVS Paperless"));

        if(Vars.TVS_PAPER_LESS.equalsIgnoreCase(mode)){
            ll_external_hu.setVisibility(View.GONE);
        }
        if(Vars.TVS_PAPER_LESS_LHU.equalsIgnoreCase(mode)){
            isPendingScanAvailable();
        }
        return view;
    }

    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle(Vars.PAPER_LESS.equals(mode) ? "Paperless-Validate HU" : (Vars.TVS_PAPER_LESS_LHU.equals(mode) ? "TVS Paperless-Validate Live HU" : "TVS Paperless"));
        inputExternalHu.setText("");
        try {
            optionDeliverySelection.setSelection(0);
        } catch(Exception e) {

        }

        try {
            selectPac.setSelection(0);
        } catch(Exception e) {

        }
    }

    private void validationData(){

            String mInputExternalHu = inputExternalHu.getText().toString().trim();

            if (TextUtils.isEmpty(mPackingNumber)){
                // packingNo.setError("Select the packing no");
                Toast.makeText(this.getContext(), "Please Pick Packing", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(mDeliveryNumber)){
               //  editDeliverySelection.setError("Select input external");
                Toast.makeText(this.getContext(), "Please Pick Delivery", Toast.LENGTH_LONG).show();
                return;
            }

            if(Vars.PAPER_LESS.equalsIgnoreCase(mode) || Vars.TVS_PAPER_LESS_LHU.equalsIgnoreCase(mode)){
                if (TextUtils.isEmpty(mInputExternalHu)){
                    inputExternalHu.setError("Enter the Delivery selection");
                    Toast.makeText(this.getContext(), "Please Enter External HU", Toast.LENGTH_LONG).show();
                    return;
                }

                if(mInputExternalHu!=null && mInputExternalHu.trim().length()>0) {

                    if(dialog==null) {
                        dialog = new ProgressDialog(getContext());
                        dialog.setMessage("Please wait...");
                        dialog.setCancelable(false);
                        dialog.show();
                    }


                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                validateExternalHu(mPackingNumber, mDeliveryNumber, mInputExternalHu);
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
                } else {
                    Toast.makeText(getContext(), "Please Enter External HU", Toast.LENGTH_LONG).show();
                }
        }else{
            Bundle bundle = new Bundle();
            bundle.putString("packing_no", mPackingNumber);// mPackingNo);
            bundle.putString("delivery_number", mDeliveryNumber);
            bundle.putBoolean("continue_scan", false);
            bundle.putString("external_hu", "");

            PapperLessScan papperLessScan = PapperLessScan.newInstance(Vars.TVS_PAPER_LESS);
            papperLessScan.setArguments(bundle);
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.add(R.id.home, papperLessScan);
            ft.addToBackStack("PapperLessPicking");
            ft.commit();
        }
    }

    private boolean isPendingScanAvailable(){
        ETStateDao stateDao = V2RDBClient.getInstance(getContext()).getV2ROfflineDB().etStateDao();
        ETState state = stateDao.getSateByModule(DB_MODULE_NAME_TVS);
        AlertBox box = new AlertBox(getContext());
        if(state != null){
            try{
                JSONArray scannedDataForSubmit = new JSONArray(state.data);
                if(scannedDataForSubmit.length() > 0){
                    String hu = state.param3;
                    Bundle bundle = new Bundle();
                    bundle.putString("packing_no", state.param4);
                    bundle.putString("delivery_number", state.param1);
                    bundle.putString("external_hu", hu);
                    box.getBox("Saved Data",
                            String.format("There are unsaved data found for HU %s. Do you want to continue scanning?. \n\n Tap OK to continue scanning",hu), (dialogInterface, i) -> {
                                bundle.putBoolean("continue_scan", true);
                                btnClear.setVisibility(View.GONE);
                                PapperLessScan papperLessScan = PapperLessScan.newInstance(this.mode);
                                papperLessScan.setArguments(bundle);
                                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                                ft.add(R.id.home, papperLessScan);
                                ft.addToBackStack(null);
                                ft.commit();
                            }, (dialogInterface, i) -> {
                                btnClear.setVisibility(View.VISIBLE);
                            });
                    return true;
                }
            }catch (Exception exce){
                box.getErrBox(exce);
            }
        }
        return false;
    }
    private void fetchPackingMaterial() {
        final RequestQueue mRequestQueue;
        String rfc = "ZWM_GET_PACKING_MATERIAL";
        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        JsonObjectRequest mJsonRequest = null;

        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);

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
                try {
                    Log.d("responsePick", responsebody.toString());

                    pickStringList.add("Select Packing Material");

                    Gson gson= new Gson();
                    JSONArray jsonArray=responsebody.getJSONArray("ET_PACK_MAT");
                    for(int i=1;i<jsonArray.length();i++) {
                            Log.d("pick", jsonArray.getString(i));
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            ETPACKMAT mETDATA = gson.fromJson(jsonObject1.toString(), ETPACKMAT.class);
                            pickStringList.add(mETDATA.getMATNR());
                            mETPACKMAT.add(mETDATA);
                    }
                    adapter.notifyDataSetChanged();
                    adapter2.notifyDataSetChanged();
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

    Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

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

    //
    // Need to call validateDelivery first
    //
    private void validateExternalHu(final String mPackingNo, final String mEditDeliverySelection, final String mInputExternalHu) {
        final RequestQueue mRequestQueue;

        String rfc = mode.equalsIgnoreCase(Vars.PAPER_LESS) ? "ZWM_VALIDATE_EXTERNAL_HU" : "ZWM_TVS_VAL_EXTERNAL_HU";
        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";


        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname",rfc);
            params.put("IM_WERKS", werks);
            params.put("IM_EXIDV", mInputExternalHu);

            if(this.m_likp_JSON!=null && this.m_likp_JSON.has("KUNNR")) {
                params.put("IM_DWERKS", this.m_likp_JSON.getString("KUNNR") );  // using same
            }
            if(this.m_likp_JSON!=null && this.m_likp_JSON.has("VBELN")) {
                params.put("IM_VBELN",  this.m_likp_JSON.getString("VBELN") );
            }

        } catch (JSONException e) {
            e.printStackTrace();

            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);

        }
        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        JsonObjectRequest mJsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {

                Log.d(TAG, "ZWM_VALIDATE_EXTERNAL_HU(): " + responsebody.toString());

                if(dialog!=null) {
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
                                        inputExternalHu.setText("");
                                        inputExternalHu.requestFocus();
                                        return;
                                    } else {

                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Bundle bundle = new Bundle();
                                                bundle.putString("packing_no", mPackingNo);// mPackingNo);
                                                bundle.putString("delivery_number", mDeliveryNumber);
                                                bundle.putBoolean("continue_scan", false);
                                                bundle.putString("external_hu", mInputExternalHu);

                                                PapperLessScan papperLessScan = PapperLessScan.newInstance(mode);
                                                papperLessScan.setArguments(bundle);
                                                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                                                ft.add(R.id.home, papperLessScan);
                                                ft.addToBackStack("PapperLessPicking");
                                                ft.commit();
                                            }
                                        });
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
            params.put("IM_VBELN", Util.deliveryVbelnForPaperlessRfc(deliveryNumber));

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
                                        // Pradeep: 2022-01-21, we will fetch these details in the next screen
                                        m_likp_JSON= responsebody.getJSONObject("EX_LIKP");
                                        m_bin_mc_array =responsebody.getJSONArray("ET_BIN_MC");
                                        m_ean_array = responsebody.getJSONArray("ET_EAN_DATA");
                                        m_lips_array= responsebody.getJSONArray("ET_LIPS");
                                        inputExternalHu.requestFocus();
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

    @Override
    public void onStart() {
        super.onStart();
        // openBarcodeReader();

    }

    @Override
    public void onStop() {

        // closeBarcodeReader();

        super.onStop();
    }


    // chainway coding

    public void initializeChainway() {
       //  barcode2D=new Barcode2D(getActivity());

       //  new ChainwayBarcodeReader().execute();
    }

    @Override
    public void update(Observable o, Object arg) {
        Log.d(TAG, "Update called, refreshing the delivery = " + mDeliveryNumber);
        // validateDelivery(mDeliveryNumber);

        // sending user back to paperless scanning
         if(getActivity()!=null) {
             inputExternalHu.setText("");
             getActivity().runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     ApplicationController.getInstance().refreshObservable().deleteObserver(PapperLessPicking.this);
                     if(fm!=null) {
                         try {
                             fm.popBackStack();
                         } catch(Exception e) {

                         }
                     }
                 }
             });
         }

    }

    public class ChainwayBarcodeReader extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;
        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            openBarcodeReader();
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
        // barcode2D.stopScan(getContext());
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
            inputExternalHu.setText(barcode);
            validationData();
        }

    }

}