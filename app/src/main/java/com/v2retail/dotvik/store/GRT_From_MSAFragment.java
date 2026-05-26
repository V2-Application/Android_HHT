package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
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
 * {@link GRT_From_MSAFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GRT_From_MSAFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GRT_From_MSAFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int REQUEST_MSA_CATEGORY = 5401;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String TAG = GRT_From_MSAFragment.class.getName();

    Button back;
    Button next;

    FragmentManager fm;
    private String date_string;
    private String store_name_string;
    Context con;
    ProgressDialog dialog;
    AlertBox box;
    String URL = "";
    String WERKS = "";
    String USER = "";
    EditText store_name_et;
    EditText date;
    TextView mResponseView;
//    Spinner dd_category_list;
//    List<String> categories = new ArrayList<String>();
//    ArrayAdapter<String> categoryAdapter;

    private OnFragmentInteractionListener mListener;

    public GRT_From_MSAFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("GRT from MSA");
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
    public static GRT_From_MSAFragment newInstance(String param1, String param2) {
        GRT_From_MSAFragment fragment = new GRT_From_MSAFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.floor_putway, container, false);

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

        date = (EditText) view.findViewById(R.id.date);
        store_name_et = (EditText) view.findViewById(R.id.store_name);
        mResponseView = (TextView) view.findViewById(R.id.response);
//        dd_category_list = view.findViewById(R.id.dd_grt_msa_category);
//
//        dd_category_list.setSelection(0);
//        categoryAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1, categories);
//        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        dd_category_list.setAdapter(categoryAdapter);

        EditTextDate editTextDate = new EditTextDate(con);
        editTextDate.setDateOnView(date);
        store_name_et.setText(WERKS);
        //  date.setEnabled(false);
        back = (Button) view.findViewById(R.id.back);
        next = (Button) view.findViewById(R.id.next);
        next.setOnClickListener(this);
        back.setOnClickListener(this);
        next.requestFocus();

//        if(categoryAdapter.getCount() == 0) {
//            getCategoryList();
//        }

        return view;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next:

                try {
                    if (ValidateData())
                        searchData();
                } catch (Exception e) {
                    box.getErrBox(e);
                }
                Log.d(TAG, "Clicked ");
                break;
            case R.id.back:
                FragmentManager fm = getActivity().getSupportFragmentManager();
                if(  fm.getBackStackEntryCount()>1)
                    fm.popBackStack();

                break;
        }
    }
//    private void getCategoryList(){
//        JSONObject args = new JSONObject();
//        try {
//            args.put("bapiname", Vars.ZWM_STORE_GRT_CATEGORY);
//            args.put("IM_USER", USER);
//            args.put("IM_WERKS", WERKS);
//            showProcessingAndSubmit(Vars.ZWM_STORE_GRT_CATEGORY, REQUEST_MSA_CATEGORY, args);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            if(dialog!=null) {
//                dialog.dismiss();
//                dialog = null;
//            }
//            AlertBox box = new AlertBox(getContext());
//            box.getErrBox(e);
//        }
//    }
//    public void setCategoryData(JSONObject responsebody){
//        try
//        {
//            JSONArray arrExData = responsebody.getJSONArray("ET_DATA");
//            int totalExRecords = arrExData.length();
//            categories.clear();
//            categories.add("Select");
//            if(totalExRecords > 0){
//                for(int recordIndex = 1; recordIndex < totalExRecords; recordIndex++){
//                    JSONObject EX_RECORD  = arrExData.getJSONObject(recordIndex);
//                    categories.add(EX_RECORD.getString("CATEGORY") + "");
//                }
//                ((BaseAdapter) dd_category_list.getAdapter()).notifyDataSetChanged();
//                dd_category_list.invalidate();
//                dd_category_list.setSelection(0);
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//            AlertBox box = new AlertBox(getContext());
//            box.getErrBox(e);
//        }
//    }
    private void searchData() {
        dialog = new ProgressDialog(con);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String addrec = store_name_string + "#" + date_string;
                String valueRequestPayload = "storegetbin#" + addrec + "#<eol>";
                Log.d(TAG, "payload : " + valueRequestPayload);
                Log.d(TAG, "payload sent to server ");

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
                    Log.d(TAG, " Response   ->  " + response);
                    response = response.substring(2, response.length());

                    String[] arrayRcvdData = response.split("!");
                    String[] arrayBinData = arrayRcvdData[0].split("#");


                    ArrayList<String> bin_list = new ArrayList<>();
                    for (String temp : arrayBinData) {

                        bin_list.add(temp);

                    }
                    Log.d("tag", "bin list ->" + bin_list);


                    Fragment fragment = new Scan_GRT_MSA_Fragment();
                    Bundle args = new Bundle();
                    args.putSerializable("bin_list", bin_list);
                    //args.putString("category", dd_category_list.getSelectedItem().toString());
                    dialog.dismiss();
                    fragment.setArguments(args);
                    if (fragment != null) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.home, fragment, "GRC_MSA");
                        ft.addToBackStack("GRC_MSA");
                        ft.commit();
                    }


                } else if (response.charAt(0) == ('E')) {
                    Log.d(TAG, " Response is Failure -> Invalid Invoice :" + response);

                    response = response.substring(2, response.length());
                    dialog.dismiss();
                    box.getBox("Err", response);

                } else {

                    Log.d(TAG, " Response is unknown :" + response);
                    dialog.dismiss();
                    box.getBox("Err", response);
                    ;


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

    private boolean ValidateData() {


        date_string = date.getText().toString();
        store_name_string = store_name_et.getText().toString();


        if (!Validate(date_string, "Date"))
            if (!Validate(store_name_string, "store Name")) {
                return false;
            }

//        if (categoryAdapter.getCount() > 0 && dd_category_list.getSelectedItemPosition() == 0) {
//            box.getBox("Missing Input", "Please select category");
//            dd_category_list.requestFocus();
//            return false;
//        }

        return true;


    }

    private boolean Validate(String value, String Tag) {


        if (value.equals("") || value.length() == 0 || value == null) {

            box.getBox("Invalid Value!", "Please enter value for " + Tag);
            return false;
        }

        return true;
    }

    private void clear() {

        mResponseView.setText("");
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

//    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
//
//        dialog = new ProgressDialog(getContext());
//
//        dialog.setMessage("Please wait...");
//        dialog.setCancelable(false);
//        dialog.show();
//
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    submitRequest(rfc, request, args);
//                } catch (Exception e) {
//                    dialog.dismiss();
//                    AlertBox box = new AlertBox(getContext());
//                    box.getErrBox(e);
//                }
//            }
//        }, 1000);
//    }
//    private void submitRequest(String rfc, int request, JSONObject args) {
//
//        final RequestQueue mRequestQueue;
//        JsonObjectRequest mJsonRequest = null;
//        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
//        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
//
//        final JSONObject params = args;
//
//        Log.d(TAG, "payload ->" + params.toString());
//
//        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
//        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
//
//            @Override
//            public void onResponse(JSONObject responsebody) {
//                if (dialog != null) {
//                    dialog.dismiss();
//                    dialog = null;
//                }
//                Log.d(TAG, "response ->" + responsebody);
//
//                if (responsebody == null) {
//                    UIFuncs.errorSound(con);
//                    AlertBox box = new AlertBox(getContext());
//                    box.getBox("Err", "No response from Server");
//                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
//                    UIFuncs.errorSound(con);
//                    AlertBox box = new AlertBox(getContext());
//                    box.getBox("Err", "Unable to Connect Server/ Empty Response");
//                    return;
//                } else {
//                    try {
//                        if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
//                            JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
//                            if (returnobj != null) {
//                                String type = returnobj.getString("TYPE");
//                                if (type != null) {
//                                    if (type.equals("E")) {
//                                        UIFuncs.errorSound(getContext());
//                                        AlertBox box = new AlertBox(getContext());
//                                        box.getBox("Err", returnobj.getString("MESSAGE"));
//                                    } else {
//                                        if (request == REQUEST_MSA_CATEGORY) {
//                                            setCategoryData(responsebody);
//                                        }
//                                    }
//                                }
//                                return;
//                            }
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        AlertBox box = new AlertBox(getContext());
//                        box.getErrBox(e);
//                    }
//                }
//            }
//        }, volleyErrorListener()) {
//            @Override
//            public String getBodyContentType() {
//                return "application/json";
//            }
//
//            @Override
//            public byte[] getBody() {
//                return params.toString().getBytes();
//            }
//
//            @Override
//            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
//
//                Response<JSONObject> res = super.parseNetworkResponse(response);
//                Log.d(TAG, "Network response -> " + res.toString());
//
//                return res;
//            }
//        };
//        mJsonRequest.setRetryPolicy(new RetryPolicy() {
//            @Override
//            public int getCurrentTimeout() {
//                return 50000;
//            }
//
//            @Override
//            public int getCurrentRetryCount() {
//                return 1;
//            }
//
//            @Override
//            public void retry(VolleyError error) throws VolleyError {
//
//            }
//        });
//        mRequestQueue.add(mJsonRequest);
//        Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
//        Log.d(TAG, "jsonRequest getBodyContentType->" + mJsonRequest.getBodyContentType());
//        Log.d(TAG, "jsonRequest getBody->" + mJsonRequest.getBody().toString());
//        Log.d(TAG, "jsonRequest getMethod->" + mJsonRequest.getMethod());
//        try {
//            Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
//        } catch (AuthFailureError authFailureError) {
//            authFailureError.printStackTrace();
//            if (dialog != null) {
//                dialog.dismiss();
//                dialog = null;
//            }
//            AlertBox box = new AlertBox(getContext());
//            box.getErrBox(authFailureError);
//        }
//    }
//
//    Response.ErrorListener volleyErrorListener() {
//        return new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//
//                Log.i(TAG, "Error :" + error.toString());
//                String err = "";
//
//                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
//                    err = "Communication Error!";
//
//                } else if (error instanceof AuthFailureError) {
//                    err = "Authentication Error!";
//                } else if (error instanceof ServerError) {
//                    err = "Server Side Error!";
//                } else if (error instanceof NetworkError) {
//                    err = "Network Error!";
//                } else if (error instanceof ParseError) {
//                    err = "Parse Error!";
//                } else err = error.toString();
//
//                if (dialog != null) {
//                    dialog.dismiss();
//                    dialog = null;
//                }
//                AlertBox box = new AlertBox(getContext());
//                box.getBox("Err", err);
//            }
//        };
//    }
}
