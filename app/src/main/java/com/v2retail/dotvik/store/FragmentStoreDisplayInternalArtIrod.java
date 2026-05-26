package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.modal.putaway.ETDataStorePutway;
import com.v2retail.dotvik.modal.putaway.ETEanDataStorePutway;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Narayanan
 * @version 11.71
 * {@code Author: Narayanan, Revision: 1, Created: 30th Jul 2024, Modified: 30th Jul 2024}
 */
public class FragmentStoreDisplayInternalArtIrod extends Fragment  implements View.OnClickListener {
    View view;
    Context con;
    FragmentManager fm;
    AlertBox box;
    ProgressDialog dialog;
    String TAG = FragmentStoreDisplayInternalArtIrod.class.getName();
    private static final int REQUEST_VALIDATE_MATNR = 5401;
    String URL;
    String WERKS;
    String USER;
    private static String parent;
    Button btn_back, btn_reset, btn_next, btn_exit;
    EditText txt_store, txt_sloc, txt_mccode, txt_irod, txt_bin, txt_ean, txt_article, txt_description, txt_totqty;
    LinearLayout ll_screen2;
    String title;
    Map<String, Map<String, ETDataStorePutway>> etData = new HashMap<>();
    Map<String, Map<String, ETEanDataStorePutway>> etEanData = new HashMap<>();
    int totalScannedQty;

    public FragmentStoreDisplayInternalArtIrod() {
        // Required empty public constructor
    }

    public static FragmentStoreDisplayInternalArtIrod newInstance(String breadcrumb) {
        FragmentStoreDisplayInternalArtIrod fragment = new FragmentStoreDisplayInternalArtIrod();
        fragment.title = breadcrumb;
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .getSupportActionBar().setTitle(UIFuncs.getSmallTitle(title + " > ART IROD IDENTIFIER"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getActivity().getSupportFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_store_display_art_irod_identifier, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = view.findViewById(R.id.txt_disp_art_irod_store);
        txt_sloc = view.findViewById(R.id.txt_disp_art_irod_sloc);
        txt_irod = view.findViewById(R.id.txt_disp_art_irod_irod);
        txt_bin = view.findViewById(R.id.txt_disp_art_irod_bin);
        txt_ean = view.findViewById(R.id.txt_disp_art_irod_ean);
        txt_article = view.findViewById(R.id.txt_disp_art_irod_articleno);
        txt_mccode = view.findViewById(R.id.txt_disp_art_irod_mccode);
        txt_description = view.findViewById(R.id.txt_disp_art_irod_description);
        txt_totqty = view.findViewById(R.id.txt_disp_art_irod_tot_qty);

        btn_back = view.findViewById(R.id.btn_disp_art_irod_back);
        btn_reset = view.findViewById(R.id.btn_disp_art_irod_reset);
        btn_next = view.findViewById(R.id.btn_disp_art_irod_next);
        btn_exit = view.findViewById(R.id.btn_disp_art_irod_exit);

        ll_screen2 = view.findViewById(R.id.ll_disp_art_irod_screen2);

        btn_back.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        btn_exit.setOnClickListener(this);

        txt_store.setText(WERKS);
        txt_sloc.setText("0001");

        clear();
        addInputEvents();
        step2();

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_disp_art_irod_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_disp_art_irod_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    step2();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.btn_disp_art_irod_next:
                step2();
                break;
            case R.id.btn_disp_art_irod_exit:
                saveData();
                break;
        }
    }

    private void addInputEvents() {
        txt_ean.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_ean);
                    if (value.length() > 0) {
                        validateArticle();
                        return true;
                    }
                }
                return false;
            }
        });
        txt_ean.addTextChangedListener(new TextWatcher() {
            boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((before == 0 && start == 0) && count > 3) {
                    scannerReading = true;
                } else {
                    scannerReading = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().toUpperCase().trim();
                if (value.length() > 0 && scannerReading) {
                    validateArticle();
                }
            }
        });
    }

    private void clear() {
        step2();
        ll_screen2.setVisibility(View.GONE);
        btn_reset.setVisibility(View.INVISIBLE);
        btn_next.setVisibility(View.VISIBLE);
        btn_exit.setVisibility(View.GONE);
    }

    private void step2() {
        etData = new HashMap<>();
        etEanData = new HashMap<>();
        ll_screen2.setVisibility(View.VISIBLE);
        btn_reset.setVisibility(View.VISIBLE);
        btn_next.setVisibility(View.GONE);
        btn_exit.setVisibility(View.VISIBLE);
        txt_irod.setText("");
        txt_mccode.setText("");
        txt_bin.setText("");
        txt_ean.setText("");
        txt_article.setText("");
        txt_description.setText("");
        txt_totqty.setText("");
        UIFuncs.enableInput(con, txt_ean);
    }

    private void showError(String title, String message) {
        UIFuncs.errorSound(con);
        AlertBox box = new AlertBox(getContext());
        box.getBox(title, message);
    }

    private void validateArticle() {
        String eanmatnr = UIFuncs.toUpperTrim(txt_ean);
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_STORE_IROD_ARTICLE_FIND);
            args.put("IM_WERKS", WERKS);
            args.put("IM_USER", USER);
            args.put("IM_MATNR", eanmatnr);
            showProcessingAndSubmit(Vars.ZWM_STORE_IROD_ARTICLE_FIND, REQUEST_VALIDATE_MATNR, args);

        } catch (JSONException e) {
            e.printStackTrace();
            UIFuncs.errorSound(con);
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
    }

    private void setData(JSONObject rsponse) {
        try {
            JSONObject exData = rsponse.getJSONObject("EX_DATA");
            txt_irod.setText(exData.getString("LGPLA"));
            txt_mccode.setText(exData.getString("MAJ_CAT_CD"));
            txt_article.setText(UIFuncs.removeLeadingZeros(exData.getString("MATNR")));
            txt_description.setText(exData.getString("MAKTX"));
            txt_bin.setText(exData.getString("VLPLA"));
            txt_totqty.setText(exData.getString("VERME"));
        } catch (Exception exce) {
            box.getErrBox(exce);
        }
        txt_ean.setText("");
        UIFuncs.enableInput(con, txt_ean);
    }

    private void saveData() {

    }

    public void showProcessingAndSubmit(String rfc, int request, JSONObject args) {

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

    private void submitRequest(String rfc, int request, JSONObject args) {

        final RequestQueue mRequestQueue;
        JsonObjectRequest mJsonRequest = null;
        String url = this.URL.substring(0, this.URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;

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
                    UIFuncs.errorSound(con);
                    AlertBox box = new AlertBox(getContext());
                    box.getBox("Err", "No response from Server");
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {
                    UIFuncs.errorSound(con);
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
                                        UIFuncs.errorSound(getContext());
                                        AlertBox box = new AlertBox(getContext());
                                        box.getBox("Err", returnobj.getString("MESSAGE"));
                                        if (request == REQUEST_VALIDATE_MATNR) {
                                            step2();
                                        }
                                    } else {
                                        if (request == REQUEST_VALIDATE_MATNR) {
                                            setData(responsebody);
                                            return;
                                        }
                                    }
                                }
                                return;
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
}