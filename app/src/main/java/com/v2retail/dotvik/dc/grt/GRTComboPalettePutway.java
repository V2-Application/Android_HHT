package com.v2retail.dotvik.dc.grt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.v2retail.commons.DataHelper;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickData;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CommonUtils;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class GRTComboPalettePutway extends Fragment {

    private static final int REQUEST_VALIDATE_PALETTE = 1051;
    private static final int REQUEST_VALIDATE_CRATE = 1052;
    private static final String TAG = GRTComboPalettePutway.class.getName();

    View rootView;
    String URL="";
    String WERKS="";
    String USER="";
    Context con;
    AlertBox box;
    ProgressDialog dialog;
    FragmentManager fm;

    EditText txt_palette,txt_crate;
    TableLayout scanneditems;
    Button btnback;

    List<String[]> rowsData = new ArrayList<>();

    public GRTComboPalettePutway() {
        // Required empty public constructor
    }
    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Palette Putway");
    }
    public static GRTComboPalettePutway newInstance(String param1, String param2) {
        GRTComboPalettePutway fragment = new GRTComboPalettePutway();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_combo_palette_putway, container, false);
        con = getContext();
        box=new AlertBox(con);
        dialog=new ProgressDialog(con);
        SharedPreferencesData data=new SharedPreferencesData(con);
        URL=data.read("URL");
        WERKS=data.read("WERKS");
        USER=data.read("USER");

        txt_crate  = rootView.findViewById(R.id.text_grt_combo_palette_putway_crate);
        txt_palette  = rootView.findViewById(R.id.text_grt_combo_palette_putway_palette);
        btnback = rootView.findViewById(R.id.btn_grt_combo_palette_putway_back);
        scanneditems = rootView.findViewById(R.id.table_grt_combo_palette_putway_scanned_items);

        txt_palette.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String palette = txt_palette.getText().toString().toUpperCase().trim();
                    if(palette.length()>0) {
                        txt_palette.selectAll();
                        validatePalette(palette);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_palette.addTextChangedListener(new TextWatcher() {
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
                String palette = s.toString().toUpperCase().trim();
                if(palette.length()>0 && scannerReading) {
                    txt_palette.selectAll();
                    validatePalette(palette);
                }
            }
        });
        txt_crate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CommonUtils.hideKeyboard(getActivity());
                    String crate = txt_crate.getText().toString().toUpperCase().trim();
                    if(crate.length()>0) {
                        txt_crate.selectAll();
                        validateCrate(crate);
                        return true;
                    }
                }
                return false;
            }
        });
        txt_crate.addTextChangedListener(new TextWatcher() {
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
                String crate = s.toString().toUpperCase().trim();
                if(crate.length() > 0 && scannerReading) {
                    txt_crate.selectAll();
                    validateCrate(crate);
                }
            }
        });

        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertBox box = new AlertBox(getContext());
                box.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fm.popBackStack();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
            }
        });
        txt_palette.requestFocus();
        return rootView;
    }
    private void validatePalette(String palette){
        rowsData = new ArrayList<>();
        generateHeader();
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.ZCOMBO_VALIDATE_PALLETE);
            args.put("IM_PALETTE", palette);
            args.put("IM_USER", USER);

            showProcessingAndSubmit(Vars.ZCOMBO_VALIDATE_PALLETE, REQUEST_VALIDATE_PALETTE, args);

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
    private void validateCrate(String crate){
        JSONObject args = new JSONObject();
        try {

            args.put("bapiname", Vars.ZCOMBO_VALIDATE_CRATE);
            args.put("IM_PALETTE", txt_palette.getText().toString().trim().toUpperCase(Locale.ROOT));
            args.put("IM_CRATE", crate);
            args.put("IM_USER", USER);

            showProcessingAndSubmit(Vars.ZCOMBO_VALIDATE_CRATE, REQUEST_VALIDATE_CRATE, args);

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
                } else if (responsebody.equals("") || responsebody.equals("null") || responsebody.toString().equals("{}")) {
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
                                        if (request == REQUEST_VALIDATE_PALETTE) {
                                            txt_palette.setText("");
                                            txt_palette.requestFocus();
                                        }
                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            txt_crate.setText("");
                                            txt_crate.requestFocus();
                                        }
                                        return;
                                    }
                                    else{
                                        if (request == REQUEST_VALIDATE_PALETTE) {
                                            txt_palette.setEnabled(false);
                                            txt_palette.setBackgroundColor(getResources().getColor(R.color.viewBg));
                                            txt_crate.setEnabled(true);
                                            txt_crate.setText("");
                                            txt_crate.requestFocus();
                                        }
                                        if (request == REQUEST_VALIDATE_CRATE) {
                                            addRow(responsebody);
                                        }
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

    private void generateHeader(){
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int headerTextSize = 0, textSize =0;
        headerTextSize = 20;
        textSize = 15;

        int rows = rowsData.size();
        scanneditems.removeAllViews();

        //Create Header Row In Table
        TextView headerSno = new TextView(getContext());
        TextView headerPalette = new TextView(getContext());
        TextView headerCrate = new TextView(getContext());
        TextView headerTanum = new TextView(getContext());
        headerSno.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        headerSno.setGravity(Gravity.CENTER);
        headerSno.setPadding(5,5,0,5);
        headerSno.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerSno.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerSno.setText("S.No");

        headerPalette.setGravity(Gravity.CENTER);
        headerPalette.setPadding(0,5,0,5);
        headerPalette.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerPalette.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerPalette.setText("Palette");

        headerCrate.setGravity(Gravity.CENTER);
        headerCrate.setPadding(0,5,0,5);
        headerCrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTextSize);
        headerCrate.setBackground(getResources().getDrawable(R.drawable.table_header_cell_border));
        headerCrate.setText("Crate");

        TableRow tr = new TableRow(getContext());
        tr.setId(0);
        TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                bottomRowMargin);
        tr.setPadding(0,0,0,0);
        tr.setLayoutParams(trParams);
        tr.addView(headerSno);
        tr.addView(headerPalette);
        tr.addView(headerCrate);
        scanneditems.addView(tr, trParams);
    }
    public void addRow(JSONObject responsebody) {
        int leftRowMargin=0;
        int topRowMargin=0;
        int rightRowMargin=0;
        int bottomRowMargin = 0;
        int textSize = 15;
        try
        {
            JSONArray ET_DATA_ARRAY = responsebody.getJSONArray("ET_DATA");
            int totalEtRecords = ET_DATA_ARRAY.length();
            if(totalEtRecords > 0){
                for(int recordIndex = 1; recordIndex < totalEtRecords; recordIndex++){
                    JSONObject ET_RECORD  = ET_DATA_ARRAY.getJSONObject(recordIndex);
                    String [] data = new String[]{ET_RECORD.getString("PALETTE"),ET_RECORD.getString("CRATE"),ET_RECORD.getString("TANUM"),ET_RECORD.getString("MESSAGE")};
                    rowsData.add(data);

                    TextView tvSno = new TextView(getContext());
                    tvSno.setText(rowsData.size()+"");
                    tvSno.setTextSize(textSize);
                    tvSno.setPadding(5,2,0,2);
                    tvSno.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                    TextView tvPalette = new TextView(getContext());
                    tvPalette.setText(ET_RECORD.getString("PALETTE"));
                    tvPalette.setTextSize(textSize);
                    tvPalette.setPadding(5,2,0,2);
                    tvPalette.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                    TextView tvCrate = new TextView(getContext());
                    tvCrate.setText(ET_RECORD.getString("CRATE"));
                    tvCrate.setTextSize(textSize);
                    tvCrate.setPadding(5,2,0,2);
                    tvCrate.setBackground(getResources().getDrawable(R.drawable.table_cell_border));

                    TableRow tr = new TableRow(getContext());
                    tr.setId(0);
                    TableLayout.LayoutParams trParams = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT,
                            TableLayout.LayoutParams.WRAP_CONTENT);
                    trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin,
                            bottomRowMargin);
                    tr.setPadding(0,0,0,0);
                    tr.setLayoutParams(trParams);
                    tr.setId(rowsData.size());
                    tr.setPadding(0,0,0,0);
                    tr.setLayoutParams(trParams);
                    tr.addView(tvSno);
                    tr.addView(tvPalette);
                    tr.addView(tvCrate);
                    tr.setTag(data);
                    scanneditems.addView(tr, trParams);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertBox box = new AlertBox(getContext());
            box.getErrBox(e);
        }
        txt_crate.setText("");
        txt_crate.requestFocus();
    }
}