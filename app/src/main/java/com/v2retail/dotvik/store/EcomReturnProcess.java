package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.v2retail.commons.SapJsonObjectRequest;

import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;


public class EcomReturnProcess extends Fragment implements AdapterView.OnItemSelectedListener {

    static String TAG = "EcomReturnProcess";

    public EcomReturnProcess() {
    }

    FragmentManager fm;

    EditText awb_no_enter, delervery_boy, user, reason_others;
    LinearLayout otherresonlayout_show;
    CheckBox checkboxstatus;
    Spinner select_reason;
    Button back_ecomm, save_ecomm;

    ProgressDialog dialog = null;

    String checkData = "";

    String requestUrl = "";
    String loginUser = "";
    String mReason_others = "";


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
        View view = inflater.inflate(R.layout.fragment_ecom_return_process, container, false);
        awb_no_enter = view.findViewById(R.id.awb_no_enter);
        otherresonlayout_show = view.findViewById(R.id.otherresonlayout_show);
        delervery_boy = view.findViewById(R.id.delervery_boy);
        user = view.findViewById(R.id.user);
        reason_others = view.findViewById(R.id.reason_other);
        checkboxstatus = view.findViewById(R.id.checkboxstatus);
        select_reason = view.findViewById(R.id.select_reason);
        back_ecomm = view.findViewById(R.id.back_ecomm);
        save_ecomm = view.findViewById(R.id.save_ecomm);
        otherresonlayout_show.setVisibility(View.GONE);

        SharedPreferencesData data = new SharedPreferencesData(getContext());


        this.requestUrl = data.read("URL");
        this.loginUser = data.read("USER");

        user.setText(this.loginUser);

        back_ecomm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fm.popBackStack();

            }
        });

        save_ecomm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validationData();
            }
        });
        checkboxstatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                     checkData = "X";
                } else {
                    checkData = "";
                }
            }
        });


        // R.array.delivery_rejection_reason
        //  ArrayAdapter aa = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item,country);
        ArrayAdapter aa = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.delivery_rejection_reason));
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        select_reason.setAdapter(aa);
        select_reason.setOnItemSelectedListener(this);

        return view;
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String item = adapterView.getItemAtPosition(i).toString();

        String[] reasonList = getResources().getStringArray(R.array.delivery_rejection_reason);
        String selectedData = reasonList[i];
        if (reasonList[i].equalsIgnoreCase("Other")) {
            otherresonlayout_show.setVisibility(View.VISIBLE);
            //  Toast.makeText(getContext(), "check", Toast.LENGTH_SHORT).show();
        } else {
            //  Toast.makeText(getContext(), "check"+item, Toast.LENGTH_SHORT).show();

            otherresonlayout_show.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void validationData() {
        String mAwb_no_enter = awb_no_enter.getText().toString().trim();
        String mDelervery_boy = delervery_boy.getText().toString().trim();
        String mUser = user.getText().toString().trim();
        mReason_others = reason_others.getText().toString().trim();
        String mCheckData = checkData;


        if (TextUtils.isEmpty(mAwb_no_enter)) {//mAwb_no_enter.isEmpty()){
            awb_no_enter.setError("Enter AWB No. ");
        }
        if (TextUtils.isEmpty(mDelervery_boy)) {
            delervery_boy.setError("Enter Delivery Boy Details.");
        }

        int selectIndex = select_reason.getSelectedItemPosition();

        if(selectIndex == 0) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Validation", "Please select reason.");
            return;
        }

        if(selectIndex==select_reason.getCount()-1) {
            mReason_others = reason_others.getText().toString().trim();
            if (TextUtils.isEmpty(mReason_others)) {
                reason_others.setError("Enter Reason Others");
            }
        } else {
            mReason_others = select_reason.getSelectedItem().toString();
        }

        if (TextUtils.isEmpty(mUser)) {
            user.setError("Enter Username  ");
        }

        if (TextUtils.isEmpty(mCheckData)) {
            checkboxstatus.setError("Awb no enter ");
        }

        if (mAwb_no_enter.equals("") || mDelervery_boy.equals("") || mUser.equals("") || mReason_others.equals("") || mCheckData.equals("")) {
            Toast.makeText(getContext(), "Field can not blank", Toast.LENGTH_SHORT).show();
        } else {
            dialog = new ProgressDialog(getContext());

            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendJSONGenericRequest("ZECOM_UPD_DELIVERY_STATUS", "ZECOM_UPD_DELIVERY_STATUS", mReason_others);

                    } catch (Exception e) {
                        dialog.dismiss();
                        AlertBox box = new AlertBox(getContext());
                        box.getErrBox(e);
                    }
                }
            }, 2000);

        }
    }


    private void sendJSONGenericRequest(final String opcode, String rfc, String reason) {
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_AWBNO", awb_no_enter.getText().toString().trim());
            params.put("IM_DLVBOY", delervery_boy.getText().toString().trim());

            params.put("IM_USER", this.loginUser);
            params.put("IM_NOTDELIVERED", "X");

            params.put("IM_REASON" , reason);


        } catch (JSONException e) {
            e.printStackTrace();
            if (dialog != null) {
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
                if (dialog != null) {
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
                                        box.getBox("Delivery Updated!", returnobj.getString("MESSAGE"));
                                        clearForm();
                                        return;
                                    }
                            }
                        }

                        awb_no_enter.setText("");
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
            if (dialog != null) {
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

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                AlertBox box = new AlertBox(getContext());
                box.getBox("Err", err);
            }
        };
    }

    void clearForm() {

        if( awb_no_enter!=null) {
            awb_no_enter.setText("");
        }

        if(delervery_boy!=null) {
            delervery_boy.setText("");
        }

        if(reason_others!=null) {
            reason_others.setText("");

        }

        if(select_reason!=null) {
            select_reason.setSelection(0);
        }
    }

}