package com.v2retail.dotvik.ecomm;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
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
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;

import com.v2retail.dotvik.store.Bin_To_Bin_Transfer_Fragment;

import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.Comparator;
import java.util.Locale;


public class EComm_Picking_DateFilter extends Fragment {
    private String TAG = "EComm_Picking_DateFilter";

    public EComm_Picking_DateFilter() {
    }

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private Bin_To_Bin_Transfer_Fragment.OnFragmentInteractionListener mListener;

    public static EComm_Picking_DateFilter newInstance(String ARG_PARAM1, String ARG_PARAM2) {
        EComm_Picking_DateFilter fragment = new EComm_Picking_DateFilter();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, ARG_PARAM1);
        args.putString(ARG_PARAM2, ARG_PARAM2);
        fragment.setArguments(args);
        return fragment;
    }

    Button buttonBack, buttonSubmit;
    ImageView selectDate;
    EditText inputFromDate;
    EditText inputUser;
    TextView labelHeading;
    RecyclerView recycler_view = null;
    PicklistAdapter adapter = null;

    FragmentManager fm;

    String loginUser = "";
    String requestUrl = "";

    JSONArray toNumberList = null;

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
        View view = inflater.inflate(R.layout.fragment_ecomm_picking_filter, container, false);
        selectDate = view.findViewById(R.id.select_date);
        inputFromDate = view.findViewById(R.id.input_date);
        inputUser = view.findViewById(R.id.input_user);

        buttonSubmit = view.findViewById(R.id.button_submit);
        buttonBack = view.findViewById(R.id.button_back);

        labelHeading = view.findViewById(R.id.text_heading);

        recycler_view = view.findViewById(R.id.recycler_view);
        adapter = new PicklistAdapter();
        recycler_view.setAdapter(adapter);
        recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));


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


            buttonSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSubmit();
//
                }
            });

            buttonBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fm.popBackStack();

                }
            });

            ((Ecomm_Process_Selection) getActivity()).setActionBarTitle("EComm Picking");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    void setDefaultValues() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        updateToDateLabel(calendar);

        // calendar.add(Calendar.DAY_OF_MONTH, -7);
        updateFromDateLabel(calendar);

    }
    private void updateFromDateLabel(Calendar myCalendar) {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        inputFromDate.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateToDateLabel(Calendar myCalendar) {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

    }


    public void onResume() {
        super.onResume();
        ((Ecomm_Process_Selection) getActivity())
                .setActionBarTitle("Ecomm Picking");
    }

    private void onSubmit() {
        String fromDate = inputFromDate.getText().toString().trim();

        if (TextUtils.isEmpty(fromDate)) {
            inputFromDate.setError("Enter the date");
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

        String rfc = "ZECOM_PICK_GET_TO_LIST";

        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            String[] fromParts = fromDate.split("\\/"); // we need to send date in yyyymmyy format
            params.put("bapiname", rfc);

            params.put("IM_DATE", fromParts[2] + fromParts[1] + fromParts[0]);
            params.put("IM_USER", this.loginUser);

        } catch (JSONException e) {
            e.printStackTrace();

            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);

        }
        Log.d(TAG, "payload ->" + params.toString());

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();

        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                try {

                    Log.d(TAG, responsebody.toString());
                    JSONArray jsonArray = responsebody.getJSONArray("ET_DATA");

                    if (jsonArray != null && jsonArray.length()>0) {
                        toNumberList = jsonArray;
                        displayAvailablePicklists(jsonArray);
                    } else {
                        Toast.makeText(getContext(), "No Picking Found", Toast.LENGTH_LONG).show();
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

    void displayAvailablePicklists(JSONArray picklists) {

        if(picklists!=null && picklists.length()>1) {
            // we need to remove the first item as it contains the definition
            // and this should be displayed
            picklists.remove(0);
            // we need to update the recycler view
            toNumberList = sortJsonArray(picklists);
            adapter.notifyDataSetChanged();

            labelHeading.setText("Below Available Picklist");
        } else {
            labelHeading.setText("Picklist not Available.");
        }
    }

    JSONArray sortJsonArray(JSONArray picklists) {
        ArrayList<JSONObject> list  = new ArrayList<>();
        for(int i=0; i<picklists.length(); i++) {
            try {
                list.add(picklists.getJSONObject(i));
            } catch (JSONException jsone) {
            }
        }
        list.sort(new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                try {
                    String s1 = o1.getString("TANUM");
                    String s2 = o2.getString("TANUM");
                    return s1.compareTo(s2);
                } catch(JSONException jsone) {
                }
                return 0;
            }
        });

        JSONArray sortedArray = new JSONArray();
        for(int i=0; i<list.size(); i++) {
            sortedArray.put(list.get(i));
        }

        return sortedArray;
    }


    class PicklistAdapter extends RecyclerView.Adapter<ToViewHolder> {

        PicklistAdapter() {

        }
        @Override
        public ToViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_ecomm_tanum_display, parent, false);
            return new ToViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ToViewHolder holder, int position) {
            try {
                holder.updateView(position, toNumberList.getJSONObject(position));
            } catch(JSONException jsone) {

            }
        }

        @Override
        public int getItemCount() {
            if(toNumberList!=null) {
                return toNumberList.length();
            }
            return 0;
        }
    }

    class ToViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textToNumber;
        TextView textDate;

        public ToViewHolder(View itemView) {
            super(itemView);

            textToNumber = itemView.findViewById(R.id.text_tanum);
            textDate =  itemView.findViewById(R.id.text_erdat);

            textToNumber.setOnClickListener(this);
            textDate.setOnClickListener(this);

        }

        public void updateView(int position, JSONObject item) {
            String toNumber = "";

            itemView.setTag("" + position);

            try {
                toNumber = item.getString("TANUM");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String trimZeros = toNumber.replaceFirst("^0+(?!$)", "");
            textToNumber.setText(trimZeros);
            textToNumber.setTag("" + position);

            String toDate = "";
            try {
                toDate = item.getString("ERDAT");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            textDate.setText(toDate);
            textDate.setTag("" + position);

        }

        @Override
        public void onClick(View v) {
            // we need to go to the next screen

            Log.d(TAG, "onClick() view tag = " + v.getTag());

            String tag = (String)v.getTag();
            if(tag!=null && tag.length()>0) {
               int rowIndex =  Integer.parseInt(tag);
                try {
                    JSONObject json = toNumberList.getJSONObject(rowIndex);
                    String pickListNumber = json.getString("TANUM");

                    Bundle bundle = new Bundle();
                    bundle.putString("picklistNumber", pickListNumber );

                    Fragment fragment = new SavePicklistFragment();
                    fragment.setArguments(bundle);

                    if (fragment != null) {
                        FragmentTransaction ft =fm.beginTransaction();
                        ft.replace(R.id.home, fragment);
                        ft.addToBackStack("Picking");
                        ft.commit();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}