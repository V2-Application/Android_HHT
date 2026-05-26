package com.v2retail.dotvik.dc.ptl;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONException;
import org.json.JSONObject;

public class PTLCrateSendToPTL extends Fragment {

    private static final int REQUEST_VALIDATE_CRATE = 306;
    /*private static final int REQUEST_VALIDATE_BIN = 307;
    private static final int REQUEST_SAVE = 308;*/
    private static final String TAG = PTLCrateSendToPTL.class.getName();

    private View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Tables tables = new Tables();
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText text_crate;
    EditText text_validated_crate;
    EditText text_destination_bin;
    TextView lbl_ptl_scan_crate;

    LinearLayout ll_validated_crate;

    Button button_save,button_reset;

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Crate Send to PTL");
    }

    public PTLCrateSendToPTL() {
        // Required empty public constructor
    }

    public static PTLCrateSendToPTL newInstance(String param1, String param2) {
        PTLCrateSendToPTL fragment = new PTLCrateSendToPTL();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm=getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ptl_crate_to_msa_bin, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        text_crate = rootView.findViewById(R.id.text_ptl_crate_to_msa_crate);
        text_validated_crate = rootView.findViewById(R.id.text_ptl_crate_to_msa_validated_crate);
        text_destination_bin = rootView.findViewById(R.id.text_ptl_crate_to_msa_bin);
        lbl_ptl_scan_crate =  rootView.findViewById(R.id.lbl_ptl_crate_to_msa_scan_crate);
        ll_validated_crate =  rootView.findViewById(R.id.ll_ptl_crate_to_msa_validated_crate);
        lbl_ptl_scan_crate.setText("Scan Crate");
        ll_validated_crate.setVisibility(View.VISIBLE);

        text_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String  crate = text_crate.getText().toString().toUpperCase();
                    if(crate.length()>0) {
                        validateCrate(crate);
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
                String crate = s.toString().toUpperCase();
                if(crate.length()>0 && scannerReading) {
                    validateCrate(crate);
                }
            }
        });

        button_save = rootView.findViewById(R.id.btn_ptl_crate_to_msa_save);
        button_save.setVisibility(View.INVISIBLE);
        button_reset = rootView.findViewById(R.id.btn_ptl_crate_to_msa_reset);

        /*button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRequest(text_crate.getText().toString(),text_destination_bin.getText().toString());
            }
        });*/
        button_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text_crate.setText("");
                text_validated_crate.setText("");
                text_destination_bin.setText("");
                text_crate.requestFocus();
            }
        });
        text_crate.requestFocus();
        return  rootView;
    }

    /*private void saveRequest(String crate,String destbin) {
        JSONObject args = new JSONObject();

        if(crate.trim().length() == 0 || destbin.trim().length() == 0){
            AlertBox box = new AlertBox(getContext());
            box.getBox("Missing Input","Crate and Bin data required");
            return;
        }

        try {

            args.put("bapiname", Vars.PTL_CTOMSA_SAVE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());
            args.put("IM_BIN", destbin.toUpperCase());

            submitRequest(Vars.PTL_CTOMSA_SAVE,REQUEST_SAVE,args);

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

    private void validateBin(String bin) {
        JSONObject args = new JSONObject();

        try {

            args.put("bapiname", Vars.PTL_CTOMSA_VALIDATE_BIN);
            args.put("IM_USER", USER);
            args.put("IM_BIN", bin.toUpperCase());

            submitRequest(Vars.PTL_CTOMSA_VALIDATE_BIN, REQUEST_VALIDATE_BIN,args);

        } catch (JSONException e) {
            e.printStackTrace();
            if(dialog!=null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }*/

    private void validateCrate(String crate) {
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.PTL_CTOPTL_VALIDATE_CRATE);
            args.put("IM_USER", USER);
            args.put("IM_CRATE", crate.toUpperCase());
            args.put("IM_NATURE", "F");

            text_destination_bin.setText("");
            text_destination_bin.setEnabled(false);

            showProcessingAndSubmit(Vars.PTL_CTOPTL_VALIDATE_CRATE,REQUEST_VALIDATE_CRATE,args);
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

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args){

        dialog = new ProgressDialog(getContext());

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

    private void submitRequest(String rfc, int request, JSONObject args){

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

        mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject responsebody) {
                if(dialog!=null) {
                    dialog.dismiss();
                    dialog = null;
                }

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
                                if (type != null) {
                                    if (type.equals("E")) {
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        text_crate.setText("");
                                        return;
                                    } else {

                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            text_validated_crate.setText(text_crate.getText());
                                            text_destination_bin.setText(returnobj.getString("MESSAGE_V1"));
                                            text_crate.setText("");
                                        }

                                        /*else if (request == REQUEST_SAVE) {

                                            AlertBox box = new AlertBox(getContext());
                                            box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    text_crate.setText("");
                                                    text_destination_bin.setText("");
                                                    text_crate.requestFocus();
                                                }
                                            });

                                        }*/
                                        return;
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