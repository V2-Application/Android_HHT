package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.EtBinModel;
import com.v2retail.dotvik.modal.EtEanDataModel;
import com.v2retail.dotvik.modal.EtPoDataModel;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.EditTextDate;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GRC_Putway_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GRC_Putway_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRC_Putway_Fragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = GRC_Putway_Fragment.class.getName();
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Tables table = new Tables();
    AlertBox box;
    ProgressDialog dialog;
    String URL = "";
    String WERKS = "";
    String USER = "";

    Button next;
    Button back;
    FragmentManager fm;
    EditText date;
    EditText store_name;
    EditText hu_number;
    TextView mResponseView;
    Context con;


    ArrayList<ArrayList<String>> dtPO;
    ArrayList<ArrayList<String>> dtEAN;
    ArrayList<String> dtBin;
    ArrayList<ArrayList<String>> dtMaterial;
    String date_string;
    String hu_number_string;
    private List<EtEanDataModel> etEanDataModels;

    private List<EtBinModel> etBinModels;
    private List<EtPoDataModel> etPoDataModels;

    private OnFragmentInteractionListener mListener;

    public GRC_Putway_Fragment() {
        // Required empty public constructor
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
    public static GRC_Putway_Fragment newInstance(String param1, String param2) {
        GRC_Putway_Fragment fragment = new GRC_Putway_Fragment();
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        fm = getFragmentManager();


    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("GRC Putway");
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
                hu_number.setText(scanContent);
            } else {
                box.getBox("Scanner Err", "No Content Received. Please Scan Again");

            }
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.grc_putway, container, false);

        con = getContext();
        box = new AlertBox(con);

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

        store_name = (EditText) view.findViewById(R.id.store_name);
        date = (EditText) view.findViewById(R.id.date);
        hu_number = (EditText) view.findViewById(R.id.hu_no);
        mResponseView = (TextView) view.findViewById(R.id.response);
        hu_number.requestFocus();
        EditTextDate editTextDate = new EditTextDate(con);
        editTextDate.setDateOnView(date);
        // date.setEnabled(false);
        store_name.setText(WERKS);

        next = (Button) view.findViewById(R.id.next);
        back = (Button) view.findViewById(R.id.back);
        back.setOnClickListener(this);
        next.setOnClickListener(this);
        onEditListener();
        addTextChangeListeners();
        return view;
    }
    private void onEditListener() {

        hu_number.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = hu_number.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan HU Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(hu_number.getWindowToken(), 0);
                            loadHuData();
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

        hu_number.addTextChangedListener(new TextWatcher() {
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
                            loadHuData();
                        }
                    });
                }
            }
        });
    }


   private void loadHuData(){
       etEanDataModels = new ArrayList<>();
       etBinModels = new ArrayList<>();
       etPoDataModels = new ArrayList<>();
       next.requestFocus();
    }
    private void networkCall() {
        date_string = date.getText().toString().trim();
        hu_number_string = hu_number.getText().toString().trim();
        if (TextUtils.isEmpty(date.getText().toString().trim())&&date_string.equals("")) {


            box.getBox("Invalid Value!", "Please check date");
            return;

        }
        if (TextUtils.isEmpty(hu_number.getText().toString().trim())&&hu_number_string.equals("")) {


            box.getBox("Invalid Value!", "Please enter HU No.");
            return;

        }
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

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
        }, 1000);
    }



    private void sendAndRequestResponse() {


        String rfc = "ZWM_STORE_HU_GET_DETAILS";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            params.put("IM_EXIDV",hu_number_string );
//            params.put("IM_LGNUM", date_string);
            params.put("IM_WERKS", WERKS);


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
                                        hu_number.setText("");
                                        hu_number.requestFocus();
                                        return;
                                    } else {

                                        JSONArray ET_EAN_DATA = responsebody.getJSONArray("ET_EAN_DATA");
                                        JSONArray ET_LAGP = responsebody.getJSONArray("ET_LAGP");
                                        JSONArray ET_HU_ITEM = responsebody.getJSONArray("ET_HU_ITEM");
                                        String EX_VEMNG = responsebody.getString("EX_VEMNG");
                                        for (int i =1;i<ET_EAN_DATA.length();i++){
                                            JSONObject jsonObject = ET_EAN_DATA.getJSONObject(i);
                                            String MANDT = jsonObject.getString("MANDT");
                                            String MATNR = jsonObject.getString("MATNR");
                                            String EAN11 = jsonObject.getString("EAN11");
                                            String UMREZ = jsonObject.getString("UMREZ");
                                            String EANNR = jsonObject.getString("EANNR");
                                            etEanDataModels.add(new EtEanDataModel(MATNR,UMREZ,MANDT,EAN11,EANNR));
                                        }

                                        for (int i =1;i<ET_LAGP.length();i++){
                                            JSONObject jsonObject = ET_LAGP.getJSONObject(i);
                                            String LGPLA = jsonObject.getString("LGPLA");
                                            etBinModels.add(new EtBinModel(LGPLA));
                                        }
                                        for (int i =1;i<ET_HU_ITEM.length();i++){
                                            JSONObject jsonObject = ET_HU_ITEM.getJSONObject(i);
                                            String MATNR = jsonObject.getString("MATNR");
                                            String VEMNG = jsonObject.getString("VEMNG");
                                            String BDMNG = jsonObject.getString("BDMNG");

                                            etPoDataModels.add(new EtPoDataModel(MATNR,VEMNG,BDMNG));
                                        }

                                        Bundle bundle = new Bundle();
                                        bundle.putSerializable("ean", (Serializable) etEanDataModels);
                                        bundle.putSerializable("bin", (Serializable) etBinModels);
                                        bundle.putSerializable("poData", (Serializable) etPoDataModels);
                                        bundle.putSerializable("TotalHuQty", EX_VEMNG);
                                        bundle.putSerializable("huNumber", hu_number_string);
                                        Fragment fragment = new Scan_grc_putway_Process_Fragment();
                                        fragment.setArguments(bundle);
                                        if (fragment != null) {
                                            clear();
                                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                                            ft.replace(R.id.home, fragment, "GRC_PUTWAY");
                                            ft.addToBackStack("GRC_PUTWAY");
                                            ft.commit();

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



    private void clear() {


        hu_number.setText("");
       /* mResponseView.setText("");
        mResponseView.setVisibility(View.GONE);*/
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        //   fm.popBackStackImmediate();
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

    @Override
    public void onClick(View view) {


        switch (view.getId()) {
            case R.id.next:


                try {
                    networkCall();
                } catch (Exception e) {
                    box.getErrBox(e);
                }
                break;
            case R.id.scan:
                Log.d(TAG, TAG + " Scanning...");
                if(CameraCheck.isCameraAvailable(con))
                IntentIntegrator.forSupportFragment(GRC_Putway_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();

                break;
            case R.id.back:
                fm.popBackStack();
                break;
        }


    }

    /**
     * This interface must be implemented by activities that contain this
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
