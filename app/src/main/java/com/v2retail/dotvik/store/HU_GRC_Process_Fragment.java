package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.EditTextDate;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HU_GRC_Process_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HU_GRC_Process_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HU_GRC_Process_Fragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    private static final String TAG = HU_GRC_Process_Fragment.class.getName();

    ProgressDialog dialog;

    TextView mResponseView;
    Button next;
    Button back;
    EditText date;
    EditText store_name;
    EditText invoice_number;
    String date_string;
    String store_number_string;
    String invoice_no_string;
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    FragmentManager fm;
    AlertBox box;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public HU_GRC_Process_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("HU GRC ");
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
    public static HU_GRC_Process_Fragment newInstance(String param1, String param2) {
        HU_GRC_Process_Fragment fragment = new HU_GRC_Process_Fragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.hu_grc, container, false);
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

        store_name = (EditText) view.findViewById(R.id.store_name);
        date = (EditText) view.findViewById(R.id.date);
        invoice_number = (EditText) view.findViewById(R.id.invoice);
        invoice_number.requestFocus();
        mResponseView = (TextView) view.findViewById(R.id.response);
        next = (Button) view.findViewById(R.id.next);
        back = (Button) view.findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fm.popBackStack();
            }
        });
        onEditListener();
        addTextChangeListeners();


        EditTextDate editTextDate = new EditTextDate(getContext());
        editTextDate.setDateOnView(date);
        //date.setEnabled(false);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    networkCall();
                } catch (Exception e) {
                    box.getErrBox(e);
                }


            }
        });

        if (WERKS != null || !WERKS.equals("") || WERKS.length() > 0)
            store_name.setText(WERKS);
        else {
            box.getBox("Err", "Invalid Store Name!");
            return null;
        }

        Log.d(TAG, TAG + " created");
        return view;
    }
    private void onEditListener() {

        invoice_number.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String invoiceNumber = invoice_number.getText().toString();
                    if (invoiceNumber == null || invoiceNumber.length() < 0 || invoiceNumber.equals("")) {
                        box.getBox("Alert", "Scan invoice Number!");
                    } else {
                        try {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(invoice_number.getWindowToken(), 0);
                            loadInvoiceData();
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
    void addTextChangeListeners() {

        invoice_number.addTextChangedListener(new TextWatcher() {
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
                    Log.d(TAG, "Scanned crate Number : " +  crateNumber);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadInvoiceData();
                        }
                    });
                }
            }
        });
    }

    private void loadInvoiceData(){
        next.requestFocus();
    }
    private void networkCall() {
        dialog = new ProgressDialog(con);

        date_string = date.getText().toString();
        invoice_no_string = invoice_number.getText().toString();
        store_number_string = store_name.getText().toString();
        if (date_string.equals("") || date_string.length() == 0 || date_string == null) {


            box.getBox("Invalid Value!", "Please Set Date");
            return;
        }
        if (invoice_no_string.equals("") || invoice_no_string.length() == 0 || invoice_no_string == null) {


            box.getBox("Invalid Value!", "Please enter Invoice No. ");
            return;
        }
        if (store_number_string.equals("") || store_number_string.length() == 0 || store_number_string == null) {

            box.getBox("Invalid Value!", "No value for store name found");
            return;
        }


        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {

                     String addrec = null;
                    String store = store_name.getText().toString();
                    if (store != null || !store.equals("") || store.length() > 0)
                        addrec = store + "#" + invoice_number.getText().toString() + "#" + date.getText().toString();
                    else {
                        box.getBox("Error", "Start Again , Store Name not Found");
                        return;
                    }
                    String valueRequestPayload = "gethus#" + addrec + "#<eol>";
                    Log.d(TAG, "payload : " + valueRequestPayload);
                    Log.d(TAG, "payload sent to server ");


                    sendAndRequestResponse(date_string,invoice_no_string,store_number_string);
                } catch (Exception e) {
                    box.getErrBox(e);
                }

            }
        }, 1000);
    }

    private void sendAndRequestResponse(String date_string, String invoice_no_string , String store_number_string) {


            String rfc = "ZWM_STORE_GET_HUS";
            final RequestQueue mRequestQueue;
            JsonObjectRequest mJsonRequest = null;
            String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
            url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
            Log.d(TAG, "URL_>" + url);
            final JSONObject params = new JSONObject();

            try {
                params.put("bapiname", rfc);
                params.put("IM_VBELN",invoice_no_string );
                params.put("IM_EDOCNO", date_string);
                params.put("IM_WERKS", store_number_string);


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
                                            invoice_number.setText("");
                                            invoice_number.requestFocus();
                                            return;
                                        } else {
                                            JSONArray arrHu = responsebody.getJSONArray("ET_HUS");
                                            if(arrHu.length() > 1){
                                                List<String> hus = new ArrayList<>();
                                                for(int i=1; i < arrHu.length(); i++){
                                                    hus.add(arrHu.getJSONObject(i).getString("HU_NO"));
                                                }
                                                Bundle args=new Bundle();
                                                args.putString("TTL_HU", (arrHu.length() - 1)+"");
                                                args.putString("store_name",WERKS);
                                                args.putSerializable("hu_list", (Serializable) hus);
                                                Fragment fragment = new Scan_HU_GRC_Fragment();
                                                fragment.setArguments(args);
                                                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                                                ft.replace(R.id.home, fragment, "HU_GRC");
                                                ft.addToBackStack("HU_GRC");
                                                invoice_number.setText("");
                                                ft.commit();
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
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
}
