package com.v2retail.dotvik.store;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.ETDATum;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PaperLessDate extends Fragment {

    String mode = Vars.PAPER_LESS;

    public PaperLessDate() {
    }

    private String TAG = PaperLessDate.class.getName();
    private Bin_To_Bin_Transfer_Fragment.OnFragmentInteractionListener mListener;

    public static PaperLessDate newInstance(String mode) {
        PaperLessDate fragment = new PaperLessDate();
        if(mode != null){
            fragment.mode = mode;
        }
        return fragment;
    }

    Button backV2, nextV2;
    ImageView selectDate;
    EditText inputFromDate;
    EditText inputUser;

    FragmentManager fm;

    String loginUser = "";
    String requestUrl = "";
    private List<ETDATum> mETDATAEtdaTumList = new ArrayList<>();

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
        View view = inflater.inflate(R.layout.fragment_paper_less_date, container, false);
        selectDate = view.findViewById(R.id.select_date);
        inputFromDate = view.findViewById(R.id.input_date);
        inputUser = view.findViewById(R.id.input_user);



        nextV2 = view.findViewById(R.id.next_v2);
        backV2 = view.findViewById(R.id.back_v2);
        SharedPreferencesData data = new SharedPreferencesData(getContext());


        this.requestUrl = data.read("URL");
        this.loginUser = data.read("USER");

        if(inputUser!=null && loginUser!=null) {
            inputUser.setText(loginUser);
        }

        setDefaultValues();

        try {


            final DatePickerDialog.OnDateSetListener fromDateListener = new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar myCalendar = Calendar.getInstance();
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    updateFromDateLabel(myCalendar);
                    inputFromDate.setError(null);
                }


            };

            final DatePickerDialog.OnDateSetListener toDateListener = new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar myCalendar = Calendar.getInstance();

                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateToDateLabel(myCalendar);
                }


            };
            selectDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Calendar myCalendar = Calendar.getInstance();
                    myCalendar.add(Calendar.DAY_OF_MONTH, -2);
                    new DatePickerDialog(getContext(), fromDateListener, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                }
            });


            nextV2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSubmit();
//
                }
            });

            backV2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
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


                }
            });
//          SharedPreferencesData data = new SharedPreferencesData(getContext());
//          this.requestUrl = data.read("URL");

            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Paperless Picking");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    void setDefaultValues() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        updateToDateLabel(calendar);

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        updateFromDateLabel(calendar);

    }
    private void updateFromDateLabel(Calendar myCalendar) {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        inputFromDate.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateToDateLabel(Calendar myCalendar) {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

    }


    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle(Vars.PAPER_LESS.equals(mode) ? "Paperless Picking" : "TVS Paperless Picking");
    }

    private void onSubmit() {
        String fromDate = inputFromDate.getText().toString().trim();

        if (TextUtils.isEmpty(fromDate)) {
            inputFromDate.setError("Enter the start Date");
        }

        if (fromDate.isEmpty()) {
            Toast.makeText(getContext(), "Show Error", Toast.LENGTH_SHORT).show();
        } else {
            sendJSONGenericRequest(fromDate);
        }
    }

    private void sendJSONGenericRequest(String fromDate) {
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        // String rfc = "ZHHTUSR_DEL_PICKING_RFC";
        String rfc = "ZWM_GET_HHTUSER_DELIVERY";

        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            String[] fromParts = fromDate.split("\\/"); // we need to send date in yyyymmyy format
            params.put("bapiname", rfc);
           //  params.put("IM_WERKS", "DH24");

            params.put("IM_DELDATE", fromParts[2] + fromParts[1] + fromParts[0]);
            params.put("IM_USER", this.loginUser);

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
                    JSONArray jsonArray = responsebody.getJSONArray("ET_DATA");

                    if (jsonArray != null && jsonArray.length()>1) {
                        PapperLessPicking papperLessPicking = PapperLessPicking.newInstance(mode);
                        Bundle bundle = new Bundle();
                        bundle.putString("data", jsonArray.toString());

                        papperLessPicking.setArguments(bundle);
                        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();

                        ft.add(R.id.home, papperLessPicking);
                        ft.addToBackStack("PapperLessPicking");
                        ft.commit();
                    } else {
                        Toast.makeText(getContext(), "No Delivery Found", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
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
        mJsonRequest.setRetryPolicy(new RetryPolicy() {
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



}