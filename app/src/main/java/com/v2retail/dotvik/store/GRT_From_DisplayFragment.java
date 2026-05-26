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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.EditTextDate;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GRT_From_DisplayFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GRT_From_DisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRT_From_DisplayFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int REQUEST_MSA_CATEGORY = 5401;

    private String TAG = GRT_From_DisplayFragment.class.getName();
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String scanner = "";
    private String requester = "";
    AlertBox box;
    ProgressDialog dialog;
    Context con;
    Button back;
    Button next;
    String URL = "";
    String WERKS = "";
    String USER = "";
    FragmentManager fm;
    EditText date_et;
    EditText store_name_et;
    EditText dest_sloc_et;
    Spinner source_sloc_et;
    TextView mResponseView;
    ArrayList<String> source_bin_table = new ArrayList<>();
    ArrayList<String> dest_bin_table = new ArrayList<>();
    private OnFragmentInteractionListener mListener;

    Spinner dd_category_list;
    List<String> categories = new ArrayList<String>();
    ArrayAdapter<String> categoryAdapter;
    ArrayAdapter<String> sourceLocAdapter;

    public GRT_From_DisplayFragment() {
        // Required empty public constructor

    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("GRT Display");


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
    public static GRT_From_DisplayFragment newInstance(String param1, String param2) {
        GRT_From_DisplayFragment fragment = new GRT_From_DisplayFragment();
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

    private void getSlocdata() {
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String addrec = null;
                addrec = store_name_et.getText().toString() + "#" + " ";
                String valueRequestPayload = "getsloc#" + addrec + "#<eol>";
                requester = "getSloc";
                Log.d(TAG, "payload-> " + valueRequestPayload);
                sendAndRequestResponse(valueRequestPayload);
            }
        }, 2000);

    }


    private void sendAndRequestResponse(final String requestBody) {
        final RequestQueue mRequestQueue;
        StringRequest mStringRequest;

        //RequestQueue initialized
        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        mStringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {


            @Override
            public void onResponse(String response) {

                response = response.substring(9, response.length());

                Log.d(TAG, "response ->" + response);

                if (response == null) {
                    dialog.dismiss();
                    box.getBox("Err", "No response from Server");

                } else if (response.equals("")) {
                    Log.d(TAG, " Response is Failure -> " + response);

                    dialog.dismiss();
                    box.getBox("Err", "Unable to Connect Server/ Empty Response");


                } else if (response.charAt(0) == ('S')) {
                    if (requester.equals("getSloc"))
                        try {
                            loadSlocDataFromServer(response);
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }


                    Log.d("tag", "  response ->" + response);

                } else if (response.charAt(0) == ('E')) {
                    Log.d(TAG, " Response is Failure -> Invalid Invoice :" + response);

                    response = response.substring(2, response.length());

                    dialog.dismiss();
                    box.getBox("Err", response);


                } else {

                    Log.d(TAG, " Response is unknown :" + response);

                    dialog.dismiss();
                    box.getBox("Err", response);

                }
            }
        }, new Response.ErrorListener() {
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

                dialog.dismiss();
                box.getBox("Err", err);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {

                Response<String> res = super.parseNetworkResponse(response);
                String data = res.result;
                Log.i(TAG, data);

                return res;
            }


        };
        mStringRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        mRequestQueue.add(mStringRequest);

    }

    private void loadSlocDataFromServer(String rcvdData) {
        if(dialog != null){
            dialog.dismiss();
        }
        String[] myArr = null;
        String addrec = null;

        myArr = rcvdData.split("#");

        String[] arrayRcvdData = null;
        String[] arraySRCData = null;
        String[] arrayDSTData = null;
        if (myArr[0].equals("S")) {
            rcvdData = rcvdData.substring(2, rcvdData.length());
            arrayRcvdData = rcvdData.split("!");
            arraySRCData = arrayRcvdData[0].split("#");
            arrayDSTData = arrayRcvdData[1].split("#");

            for (int lk = 0; lk < arraySRCData.length; lk++) {
                source_bin_table.add(arraySRCData[lk]);
            }
            for (int j = 0; j < arrayDSTData.length; j++) {

                dest_bin_table.add(arrayDSTData[j]);
            }
            Log.d(TAG, " source bin table ->" + source_bin_table);
            Log.d(TAG, " dest bin table ->" + dest_bin_table);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sloc_to_sloc_wwm, container, false);
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

        List<String> locations = new ArrayList<>();
        locations.add("0001");
        locations.add("0006");


        date_et = (EditText) view.findViewById(R.id.date);
        store_name_et = (EditText) view.findViewById(R.id.store_name);
        dest_sloc_et = (EditText) view.findViewById(R.id.dest_sloc);

        source_sloc_et = view.findViewById(R.id.source_sloc);
        source_sloc_et.setSelection(0);
        sourceLocAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1, locations);
        sourceLocAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        source_sloc_et.setAdapter(sourceLocAdapter);

        mResponseView = (TextView) view.findViewById(R.id.response);
        dd_category_list = view.findViewById(R.id.dd_grt_display_category);

        EditTextDate editTextDate = new EditTextDate(con);
        editTextDate.setDateOnView(date_et);
        store_name_et.setText(WERKS);

        back = (Button) view.findViewById(R.id.back);
        next = (Button) view.findViewById(R.id.next);

        dd_category_list.setSelection(0);
        categoryAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_category_list.setAdapter(categoryAdapter);

        next.setOnClickListener(this);
        back.setOnClickListener(this);
        try {
            getSlocdata();
        } catch (Exception e) {
            box.getErrBox(e);
        }

        if(categoryAdapter.getCount() == 0) {
            getCategoryList();
        }
        source_sloc_et.setSelection(0);
        dest_sloc_et.requestFocus();
        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next:
                try {
                    nextScreen();

                } catch (Exception e) {
                    box.getErrBox(e);
                }
                break;
            case R.id.back:
                fm.popBackStack();
                break;

        }
    }

    private void getCategoryList(){
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_GRT_CATEGORY);
            args.put("IM_USER", USER);
            args.put("IM_WERKS", WERKS);
            showProcessingAndSubmit(Vars.ZWM_STORE_GRT_CATEGORY, REQUEST_MSA_CATEGORY, args);
        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    public void setCategoryData(JSONObject responsebody){
        try
        {
            JSONArray arrExData = responsebody.getJSONArray("ET_DATA");
            int totalExRecords = arrExData.length();
            categories.clear();
            categories.add("Select");
            if(totalExRecords > 0){
                for(int recordIndex = 1; recordIndex < totalExRecords; recordIndex++){
                    JSONObject EX_RECORD  = arrExData.getJSONObject(recordIndex);
                    categories.add(EX_RECORD.getString("CATEGORY") + "");
                }
                ((BaseAdapter) dd_category_list.getAdapter()).notifyDataSetChanged();
                dd_category_list.invalidate();
                dd_category_list.setSelection(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void nextScreen() {

        String source = source_sloc_et.getSelectedItem().toString();
        String dest = dest_sloc_et.getText().toString();
        if (!source_bin_table.contains(source)) {
            box.getBox("Alert", "Invalid Source!");
            return;
        }

        if (!dest_bin_table.contains(dest)) {
            box.getBox("Alert", "Invalid Destination!");
            return;
        }

        if (source == null || source.equals("") || source.length() < 0) {
            box.getBox("Alert", "Please Scan/Fill source");
            return;
        }
        if (dest == null || dest.equals("") || dest.length() < 0) {
            box.getBox("Alert", "Please Scan/Fill destination");
            return;
        }
        //Version 11.81
        //if (dest.equals("0002") || dest.equals("0005")) {
        if (dest.equals("0002")) {
            box.getBox("Alert", "Invalid destination");
            dest_sloc_et.setText("");
            return;
        }
        if (source.equals("0002")) {
            box.getBox("Alert", "Invalid source");
            source_sloc_et.setSelection(0);
            return;
        }
        if (source.equals(dest)) {
            box.getBox("Alert", "Source and destination can not be same");
            return;
        }
        if (categoryAdapter.getCount() > 0 && dd_category_list.getSelectedItemPosition() == 0) {
            box.getBox("Missing Input", "Please select category");
            dd_category_list.requestFocus();
            return;
        }
        Bundle args = new Bundle();
        args.putString("source", source);
        args.putString("dest", dest);
        args.putString("category", dd_category_list.getSelectedItem().toString());
        Fragment fragment = new Scan_GRT_Display_Fragment();
        fragment.setArguments(args);
        source_sloc_et.setSelection(0);
        dest_sloc_et.setText("");
        if(dialog != null){
            dialog.dismiss();
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home, fragment, "GRC_display");
        ft.addToBackStack("GRC_display");
        ft.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(dialog !=null){
            dialog.dismiss();
        }
        if (scanningResult == null) {

            Log.d(TAG, TAG + " scanned result...");
            box.getBox("Scanner Err", "Unable to receive Data");
            ;
        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null) {
                switch (scanner)

                {
                    case "source":
                        Log.v(TAG, "scanContent source = " + scanContent);

                        if (source_bin_table.contains(scanContent))
                            if (sourceLocAdapter.getPosition(scanContent) >= 0) {
                                source_sloc_et.setSelection(sourceLocAdapter.getPosition(scanContent)); // select that item
                            } else {
                                Log.w("Spinner", "Value not found in spinner: " + scanContent);
                            }
                        else {
                            box.getBox("Alert", "Invalid Source!");
                        }
                        break;
                    case "dest":
                        Log.v(TAG, "scanContent dest = " + scanContent);
                        dest_sloc_et.setText(scanContent);
                        if (dest_bin_table.contains(scanContent))
                            dest_sloc_et.setText(scanContent);
                        else {
                            box.getBox("Alert", "Invalid Destination!");
                        }

                        break;
                }

            } else {
                box.getBox("Scanner Err", "No Content Received. Please Scan Again");

            }
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

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
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
    private void submitRequest(String rfc, int request, JSONObject args) {

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
                if (dialog != null) {
                    dialog.dismiss();
                }
                Log.d(TAG, "response ->" + responsebody);

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
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                    } else {
                                        if (request == REQUEST_MSA_CATEGORY) {
                                            setCategoryData(responsebody);
                                        }
                                    }
                                }
                                return;
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
            if (dialog != null) {
                dialog.dismiss();
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

                if (dialog != null) {
                    dialog.dismiss();
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }
}
