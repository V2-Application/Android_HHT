package com.v2retail.dotvik.ecomm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import com.android.volley.RetryPolicy;
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


public class CLA_UntagPaletteWithBin extends Fragment implements View.OnClickListener {

    private final String TAG = "CLA_TagPaletteWithBin";
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private Activity activity;

    private View rootView;

    private EditText text_source_palette;
    private EditText text_cla_bin;
    private EditText text_cla_bin_display;
    private EditText text_scanned_count;
    private Button button_save;

    String courier;

    private ProgressDialog dialog = null;

    public CLA_UntagPaletteWithBin() {

    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_cla_tag_palette_with_bin, container, false);
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

        text_source_palette = rootView.findViewById(R.id.text_palette);
        text_cla_bin = rootView.findViewById(R.id.text_cla_bin);
        text_cla_bin_display = rootView.findViewById(R.id.text_cla_bin_display);
        text_scanned_count = rootView.findViewById(R.id.text_scanned_count);

        if(rootView.findViewById(R.id.control_row_scanned_count)!=null) {
            rootView.findViewById(R.id.control_row_scanned_count).setVisibility(View.GONE);
        }


        button_save = rootView.findViewById(R.id.button_save);

        handleClicks();
        handlePicklistKeypadEditor();

        if(activity instanceof AppCompatActivity) {
            ((AppCompatActivity)activity).getSupportActionBar().setTitle("Un tag Palette with Ecom CLA Bin");
        }

        text_source_palette.requestFocus();
    }

    void handleClicks() {
        button_save.setOnClickListener(this);
    }

    private void handlePicklistKeypadEditor() {
        text_source_palette.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String  palette = text_source_palette.getText().toString();
                    if(palette.length()>0) {
                        validatePalette(palette);
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });

        text_source_palette.addTextChangedListener(new TextWatcher() {
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
                String palette = s.toString();
                if(palette.length()>0 && scannerReading) {
                    validatePalette(palette);
                }
            }
        });

        /*
        text_source_palette.setOnKeyListener(new View.OnKeyListener() {
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
                        String  palette = text_source_palette.getText().toString();
                        if(palette.length()>0) {
                            validatePalette(palette);
                            return true;
                        }
                    }
                }

                return false;
            }
        });
         */

        text_cla_bin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(activity);
                    String claBin = text_cla_bin.getText().toString();
                    if(claBin.length()>0) {
                        validateClaBin(claBin);
                        return true;
                    }
                }
                return false;
            }
        });

        text_cla_bin.addTextChangedListener(new TextWatcher() {
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
                String claBin = s.toString();
                if(claBin.length()>0 && scannerReading) {
                    validateClaBin(claBin);
                }
            }
        });

        /*
        text_cla_bin.setOnKeyListener(new View.OnKeyListener() {
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
                        String claBin = text_cla_bin.getText().toString();
                        if(claBin.length()>0) {
                            validateClaBin(claBin);
                            return true;
                        }
                    }
                }
                return false;
            }
        });
         */

    }
    @Override
    public void onClick(View view) {

        CommonUtils.hideKeyboard(activity);
        switch (view.getId()){
            case R.id.button_save:
                // SCANNER_CURRENT_TYPE = SavePicklistFragment.SCANNER_TYPES.PICKLIST.name();
                // openScanner();
                saveUntaggedPalette_ClaBin();
                break;
        }
    }


    private void validatePalette(final String palette) {
        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidatePaletteJson(palette);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }


    void sendValidatePaletteJson(String palette) {

        String rfc = "ZECOM_CLA_PALETTE_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            // params.put("IM_USER", USER);
            // params.put("WERKS", WERKS);

            JSONObject imParam = new JSONObject();
            imParam.put("PALETTE", palette);
            imParam.put("INDICATOR", "3");
            params.put("IM_PARMS", imParam);

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
                                        text_source_palette.setText("");
                                        text_source_palette.requestFocus();
                                        return;
                                    } else {

                                        JSONObject paletteExData = responsebody.getJSONObject("EX_DATA");
                                        courier = paletteExData.optString("COURIER");
                                        text_cla_bin.requestFocus();
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


    private void validateClaBin(final String claBin) {
        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendValidateClaBinJson(claBin);
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);
    }


    void sendValidateClaBinJson(String claBin) {

        String rfc = "ZECOM_CLA_BIN_VALIDATE";
        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            // params.put("IM_USER", USER);
            // params.put("WERKS", WERKS);

            JSONObject imParam = new JSONObject();
            imParam.put("PALETTE", text_source_palette.getText().toString());
            imParam.put("INDICATOR", "2");
            imParam.put("CLABIN", claBin);

            params.put("IM_PARMS", imParam);

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
                                        text_cla_bin.setText("");
                                        text_cla_bin.requestFocus();
                                        return;
                                    } else {

                                        /*
                                        JSONObject exData = responsebody.getJSONObject("EX_DATA");
                                        String huCount = exData.optString("HU_COUNT");

                                        if(huCount!=null && huCount.length()>0) {
                                            text_scanned_count.setText(huCount.toString());
                                        }

                                         */

                                        text_cla_bin_display.setText(claBin);
                                        text_cla_bin.setText("");
                                        text_cla_bin.requestFocus();

                                        // Now need to save it.

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

    void saveUntaggedPalette_ClaBin() {

        dialog = new ProgressDialog(getContext());

        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendPaletteAndClaBinSaveJson();
                } catch (Exception e) {
                    dialog.dismiss();
                    AlertBox box = new AlertBox(getContext());
                    box.getErrBox(e);
                }
            }
        }, 1000);

    }

    void sendPaletteAndClaBinSaveJson() {
        String rfc = "ZECOM_CLA_PALETTE_BIN_TAG_SAVE";

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
        Log.d(TAG, "URL_>" + url);
        final JSONObject params = new JSONObject();

        try {
            params.put("bapiname", rfc);
            // params.put("IM_TEST", "X");  // TODO: remove X on release

            JSONObject imParam = new JSONObject();
            imParam.put("PALETTE", text_source_palette.getText().toString());
            imParam.put("INDICATOR", "2");
            imParam.put("CLABIN", text_cla_bin_display.getText().toString());

            params.put("IM_PARMS", imParam);

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
                                        text_source_palette.setText("");
                                        text_source_palette.requestFocus();
                                        return;
                                    } else {
                                        // JSONObject paletteExData = responsebody.getJSONObject("EX_DATA")

                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Success", returnobj.getString("MESSAGE"), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                               //  getFragmentManager().popBackStack();
                                            }
                                        });

                                        text_source_palette.setText("");
                                        text_cla_bin.setText("");
                                        text_cla_bin_display.setText("");
                                        text_source_palette.requestFocus();
                                        courier = "";

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
