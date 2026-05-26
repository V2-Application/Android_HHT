package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
import com.v2retail.util.AppConstants;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

public class Pick_Order_For_Packing extends Fragment implements View.OnClickListener {

    private final String TAG = "Pick_Order_For_Packing";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private Activity activity;

    private View rootView;
    private EditText text_crate;
    private Button button_picking_validate;

    private ProgressDialog dialog = null;

    public Pick_Order_For_Packing() {

    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // using the same view which was used in putway_empty_crate process
        // we will remove the controls which are not needed.
        rootView = inflater.inflate(R.layout.fragment_putway_empty_crate, container, false);
        initView();
        return rootView;
    }

    void initView() {
        activity = getActivity();

        SharedPreferencesData data = new SharedPreferencesData(activity);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);

        View controlRow = rootView.findViewById(R.id.control_row_bin);
        // remove from this view as it is not needed
        controlRow.setVisibility(View.INVISIBLE);
        ((ViewGroup) controlRow.getParent()).removeView(controlRow);

        text_crate = rootView.findViewById(R.id.text_crate);
        button_picking_validate = rootView.findViewById(R.id.button_validate_update);

        button_picking_validate.setText("Picking Validate");

        handleClicks();

        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Pick Order for Packing");
        }

        text_crate.requestFocus();

    }

    private void handlePicklistKeypadEditor() {

        text_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String crateNo = text_crate.getText().toString();
                    // validateCrate(crateNo);
                    if(crateNo.length()>0) {
                        cratePickingValidate(crateNo);
                        return true;
                    }

                }
                return false;
            }
        });

        text_crate.addTextChangedListener(new TextWatcher() {
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
                String crateNo = s.toString();
                if(crateNo.length()>0 && scannerReading) {
                    cratePickingValidate(crateNo);
                }
            }
        });

        /*
        text_crate.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == keyEvent.ACTION_UP) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1
                            || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1
                            || keyEvent.getKeyCode() == 10036
                            || keyEvent.getScanCode() == 310
                            || keyEvent.getScanCode() == 311) {
                        // we need to validate the bin
                        CommonUtils.hideKeyboard(activity);
                        String  crateNo = text_crate.getText().toString();
                        if(crateNo.length()>0) {
                            cratePickingValidate(crateNo);
                            return true;
                        }
                    }
                }

                return false;
            }
        });
         */
    }

    void handleClicks() {
        button_picking_validate.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

    }

    void cratePickingValidate(String crateNumber) {

        String rfc = "ZECOM_CRATE_PICKING_VALIDATE";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", rfc);
            params.put("IM_CRATE", crateNumber.toUpperCase());
            params.put("IM_USER", USER.toUpperCase());
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
                                        text_crate.setText("");
                                        text_crate.requestFocus();
                                        return;
                                    } else {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("", returnobj.getString("MESSAGE"));
                                        text_crate.setText("");
                                        text_crate.requestFocus();
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
}
