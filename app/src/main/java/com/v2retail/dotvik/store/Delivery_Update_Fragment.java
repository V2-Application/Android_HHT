package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.TextUtils;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

public class Delivery_Update_Fragment  extends Fragment  {

    static String TAG  = "Delivery_Update";

    FragmentManager fm;

    View rootView;
    EditText deliveryBoy;
    EditText awbNumber;

    Button scan;
    Button back;
    Button delivered;

    ProgressDialog dialog = null;

    String dBoy = null;
    String awbNo = null;

    String requestUrl = "";
    String loginUser = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //  dataList = ( ArrayList<HashMap<String,String>>) getArguments().getSerializable("data");


        }
        fm = getFragmentManager();
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delivery_update, container, false);


        deliveryBoy = (EditText) view.findViewById(R.id.delivery_boy);
        awbNumber  = (EditText) view.findViewById(R.id.awb_no);


        scan = (Button) view.findViewById(R.id.scan);
        back = (Button) view.findViewById(R.id.back);
        delivered = (Button) view.findViewById(R.id.delivered);


        awbNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    String code = awbNumber.getText().toString().trim();
                    if (TextUtils.isEmpty(code)) {
                        AlertBox box = new AlertBox(getContext());
                        box.getBox("Alert", "Enter AWB No!");
                        return true;
                    } else {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(awbNumber.getWindowToken(), 0);
                        fetchAWBDetails();
                        return true;
                    }
                }
                return false;
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (CameraCheck.isCameraAvailable(getContext())) {
                    IntentIntegrator.forSupportFragment(Delivery_Update_Fragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();
                }
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fm.popBackStack();
            }
        });

        delivered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAWBDetails();
            }
        });

        SharedPreferencesData data = new SharedPreferencesData(getContext());


        this.requestUrl = data.read("URL");
        this.loginUser = data.read("USER");

        this.rootView = view;
        return view;
    }


    void fetchAWBDetails() {
        awbNo = awbNumber.getText().toString().trim();
        if (TextUtils.isEmpty(awbNo)) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Alert", "Enter AWB Number!");
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
                    sendDeliveryInfoJSONGenericRequest("ZECOM_GET_DELIVERY_INFO", "ZECOM_GET_DELIVERY_INFO");

                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 2000);


    }


    void clearAWBDetails() {

        TextView textView = rootView.findViewById(R.id.order_number);
        if( textView!=null) {
            textView.setText("");
            textView.setVisibility(View.GONE);
        }

        textView = rootView.findViewById(R.id.customer_name);
        if(textView!=null) {
            textView.setText("");
            textView.setVisibility(View.VISIBLE);
        }

        textView = rootView.findViewById(R.id.address_1);
        if(textView!=null) {
            textView.setText("");
            textView.setVisibility(View.VISIBLE);
        }

        textView = rootView.findViewById(R.id.address_2);
        if(textView!=null) {
            textView.setText("");
            textView.setVisibility(View.VISIBLE);
        }

        textView = rootView.findViewById(R.id.city);
        if(textView!=null) {
            textView.setText("");
            textView.setVisibility(View.VISIBLE);
        }

        textView = rootView.findViewById(R.id.postal_code);
        if(textView!=null) {
            textView.setText("");
            textView.setVisibility(View.VISIBLE);
        }
    }

    void displayAWBDetails(JSONObject ordInfo) {

        Log.d(TAG, "Order Info: " + ordInfo.toString());

        /*
        "CUSTOMER_NAME1": "SONALIKA RAUTRAY",
        "CONTACT_NO": "",
        "CUSTOMER_NAME2": "SONALIKA RAUTRAY",
        "CITY": "BHUBANESHWAR",
        "POSTAL_CODE": "751002",
        "SHIPPING_ADD3": "",
        "SHIPPING_ADD2": "BHUBANESHWAR",
        "ORDER_NO": "V2-000002132",
        "SHIPPING_ADD1": "BHUBANESHWAR"
         */
        try {
            String orderNumber = ordInfo.getString("ORDER_NO");
            TextView textView = rootView.findViewById(R.id.order_number);
            if(orderNumber!=null && orderNumber.length()>0 && textView!=null) {
                textView.setText(orderNumber);
                textView.setVisibility(View.VISIBLE);
            }

        } catch(JSONException jsone) {

        }

        try {
            String customerName1 = ordInfo.getString("CUSTOMER_NAME1");

            TextView textView = rootView.findViewById(R.id.customer_name);
            if(customerName1!=null && customerName1.length()>0 && textView!=null) {
                textView.setText(customerName1);
                textView.setVisibility(View.VISIBLE);
            }

        } catch(JSONException jsone) {

        }


        try {
            String add1 = ordInfo.getString("SHIPPING_ADD1");

            TextView textView = rootView.findViewById(R.id.address_1);
            if(add1!=null && add1.length()>0 && textView!=null) {
                textView.setText(add1);
                textView.setVisibility(View.VISIBLE);
            }

        } catch(JSONException jsone) {

        }

        try {
            String add2 = ordInfo.getString("SHIPPING_ADD2");
            TextView textView = rootView.findViewById(R.id.address_2);
            if(add2!=null && add2.length()>0 && textView!=null) {
                textView.setText(add2);
                textView.setVisibility(View.VISIBLE);
            }

        } catch(JSONException jsone) {

        }

        try {
            String city = ordInfo.getString("CITY");
            TextView textView = rootView.findViewById(R.id.city);
            if(city!=null && city.length()>0 && textView!=null) {
                textView.setText(city);
                textView.setVisibility(View.VISIBLE);
            }


        } catch(JSONException jsone) {

        }

        try {
            String postalCode = ordInfo.getString("POSTAL_CODE");
            TextView textView = rootView.findViewById(R.id.postal_code);
            if(postalCode!=null && postalCode.length()>0 && textView!=null) {
                textView.setText(postalCode);
                textView.setVisibility(View.VISIBLE);
            }

        } catch(JSONException jsone) {

        }
    }


    void updateAWBDetails() {

        dBoy = deliveryBoy.getText().toString().trim();
        if (TextUtils.isEmpty(dBoy)) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Alert", "Enter Delivery Boy Details!");
            return;
        }

        awbNo = awbNumber.getText().toString().trim();
        if (TextUtils.isEmpty(awbNo)) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Alert", "Enter AWB Number!");
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
                    sendJSONGenericRequest("ZECOM_UPD_DELIVERY_STATUS", "ZECOM_UPD_DELIVERY_STATUS");

                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 2000);

    }


    // fetch  AWB Details from Server
    // and display to User
    private void sendDeliveryInfoJSONGenericRequest(final String opcode, String rfc) {

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;

        // String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));

        // Use below for testing
        String url = "http://192.168.151.40:6080/xmwgw";

        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_AWBNO", awbNo);
            params.put("IM_USER", this.loginUser);
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
                                        // type = S, "MESSAGE":"Successful"
                                        JSONObject ordIndo = responsebody.getJSONObject("EX_ORDINFO");
                                        if(ordIndo!=null) {
                                            displayAWBDetails(ordIndo);
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
        mJsonRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 50000;
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);

        }
    }



    private void sendJSONGenericRequest(final String opcode, String rfc) {

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.requestUrl.substring(0, this.requestUrl.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_AWBNO", awbNo);
            params.put("IM_DLVBOY", dBoy);
            params.put("IM_USER", this.loginUser);
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
                                        box.getBox("Delivery Updated!", returnobj.getString("MESSAGE"));
                                        clearAWBDetails();
                                        return;
                                    }
                            }
                        }

                        awbNumber.setText("");
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
                return 50000;
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
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(authFailureError);

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, TAG + " scanned result...");
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult == null) {
            AlertBox box = new AlertBox(getContext());
            box.getBox("Scanner Err", "Unable to receive Data");
        } else {
            Log.d(TAG, "Scan data received");
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            Log.v(TAG, "scanContent = " + scanContent);
            Log.v(TAG, "scanFormat = " + scanFormat);

            if (scanContent != null) {
                awbNumber.setText(scanContent);

                if (TextUtils.isEmpty(scanContent)) {
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Alert", "Enter AWB No!");
                    return;
                }
                fetchAWBDetails();
            } else {
                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Enter AWB No!");
            }
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

}
