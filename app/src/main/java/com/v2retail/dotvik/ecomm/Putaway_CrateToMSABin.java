package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Handler;
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
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Putaway_CrateToMSABin#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Putaway_CrateToMSABin extends Fragment implements View.OnClickListener {

    private static int VALIDATE_CRATE_REQUEST = 100;
    private static int VALIDATE_BIN_REQUEST = 101;
    private static int SAVE_REQUEST = 102;

    private final String TAG = "Putaway_CrateToMSABin";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private ProgressDialog dialog = null;

    private Activity activity;

    private View rootView;

    private EditText text_destination_crate;
    private EditText text_destination_bin;

    private Button button_save;
    private Button button_reset;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Putaway_CrateToMSABin() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment fragment_putaway_crate_to_msabin.
     */
    // TODO: Rename and change types and number of parameters
    public static Putaway_CrateToMSABin newInstance(String param1, String param2) {
        Putaway_CrateToMSABin fragment = new Putaway_CrateToMSABin();
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_putaway_crate_to_msabin, container, false);
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

        text_destination_crate = rootView.findViewById(R.id.text_crate_msatobin);
        text_destination_bin = rootView.findViewById(R.id.text_destination_bin_msatobin);

        button_save = rootView.findViewById(R.id.button_save_msatobin);
        button_reset = rootView.findViewById(R.id.button_reset_msatobin);

        handleClicks();

        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Putaway Crate To MSA Bin");
        }

        text_destination_crate.requestFocus();
    }

    void handleClicks() {

        button_save.setOnClickListener(this);
        button_reset.setOnClickListener(this);
    }

    private void handlePicklistKeypadEditor() {

        text_destination_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String crate = text_destination_crate.getText().toString();
                    if(crate.length()>0) {
                        validateCrateRequest(crate);
                        return true;
                    }
                }
                return false;
            }
        });

        text_destination_crate.addTextChangedListener(new TextWatcher() {
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
                String crate = s.toString();
                if(crate.length()>0) {
                    validateCrateRequest(crate);
                }
            }
        });

        text_destination_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String  destbin = text_destination_bin.getText().toString();
                    if(destbin.length()>0) {
                        validateBinRequest(destbin);
                        return true;
                    }
                }
                return false;
            }
        });

        text_destination_bin.addTextChangedListener(new TextWatcher() {
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
                String destbin = s.toString();
                if(destbin.length()>0) {
                    validateBinRequest(destbin);
                }
            }
        });
    }

    private void validateCrateRequest(String crate) {
        JSONObject args = new JSONObject();

        try {

            args.put("bapiname", Vars.CANCELPUT_VALIDATE_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());

            text_destination_bin.setText("");
            text_destination_bin.setEnabled(false);

            validateRequest(Vars.CANCELPUT_VALIDATE_CRATE, args,VALIDATE_CRATE_REQUEST);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void validateBinRequest(String destbin) {
        JSONObject args = new JSONObject();

        try {

            args.put("bapiname", Vars.CANCELPUT_VALIDATE_BIN);
            args.put("IM_USER", USER);
            args.put("IM_BIN", destbin.toUpperCase());

            validateRequest(Vars.CANCELPUT_VALIDATE_BIN, args,VALIDATE_BIN_REQUEST);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void saveRequest(String crate,String destbin) {
        JSONObject args = new JSONObject();

        if(crate.trim().length() == 0 || destbin.trim().length() == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Missing Input","Crate and Bin data required");
            return;
        }

        try {

            args.put("bapiname", Vars.SAVE_MSA_TO_BIN);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());
            args.put("IM_BIN", destbin.toUpperCase());

            validateRequest(Vars.SAVE_MSA_TO_BIN, args,SAVE_REQUEST);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    @Override
    public void onClick(View view) {

        CommonUtils.hideKeyboard(activity);
        switch (view.getId()){
            case R.id.button_save_msatobin:
                    saveRequest(text_destination_crate.getText().toString(),text_destination_bin.getText().toString());
                break;
            case R.id.button_reset_msatobin:
                    text_destination_crate.setText("");
                    text_destination_bin.setText("");
                    text_destination_crate.requestFocus();
                break;
        }
    }

    public void validateRequest(String rfc, JSONObject params, int request){

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    callRFC(rfc, params, request);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }

    void callRFC(String rfc, JSONObject args, int request) {


        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = args;

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

                                        if(request == VALIDATE_CRATE_REQUEST) {

                                            text_destination_crate.setText("");
                                            text_destination_crate.requestFocus();

                                        }
                                        else if(request == VALIDATE_BIN_REQUEST){

                                            text_destination_bin.setText("");
                                            text_destination_bin.requestFocus();
                                        }
                                        else if(request == SAVE_REQUEST){

                                            text_destination_crate.setText("");
                                            text_destination_bin.setText("");
                                            text_destination_bin.setEnabled(false);
                                            text_destination_crate.requestFocus();
                                        }

                                        return;
                                    } else {

                                        if(request == VALIDATE_CRATE_REQUEST) {

                                            text_destination_bin.setEnabled(true);
                                            text_destination_bin.requestFocus();

                                        }
                                        else if(request == SAVE_REQUEST){

                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"));

                                            text_destination_bin.setEnabled(false);
                                            text_destination_crate.setEnabled(true);
                                            text_destination_crate.setText("");
                                            text_destination_bin.setText("");
                                            text_destination_crate.requestFocus();

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