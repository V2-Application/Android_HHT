package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.SapJsonRows;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.EditTextDate;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Tables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Picking_Against_Picklist_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Picking_Against_Picklist_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Picking_Against_Picklist_Fragment extends Fragment implements View.OnClickListener {
    private static final String RFC_GET_PICKLIST = "ZWM_STORE_GET_PICKLIST";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String TAG = Picking_Against_Picklist_Fragment.class.getName();
    String URL = "";
    String WERKS = "";
    String USER = "";
    Context con;
    ProgressDialog dialog;
    AlertBox box;
    EditText date;
    EditText Store_name_et;
    EditText pick_list_et;
    TextView mResponseView;
    Button back;
    Button next;

    Tables tables = new Tables();

    ArrayList<ArrayList<String>> dtPO;
    ArrayList<ArrayList<String>> dtEAN;
    FragmentManager fm;
    private OnFragmentInteractionListener mListener;

    public Picking_Against_Picklist_Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Picking Against Picking");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OutWardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static Picking_Against_Picklist_Fragment newInstance(String param1, String param2) {
        Picking_Against_Picklist_Fragment fragment = new Picking_Against_Picklist_Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private void loadData() {
        if (dtEAN != null) {
            dtEAN = null;

        }

        if (dtPO != null) {
            dtPO = null;

        }


        dtPO = tables.getPOTAble("pick");
        dtEAN = tables.getEANTAble("");


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next:
                try {
                    networkCall();
                } catch (Exception e) {
                    box.getErrBox(e);
                }
                break;
            case R.id.back:
                fm.popBackStack();
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.picking_against_picklist, container, false);
        con = getContext();
        dialog = new ProgressDialog(con);
        box = new AlertBox(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");
        if (!URL.isEmpty())
            Log.d(TAG, "URL->" + URL);
        if (!WERKS.isEmpty())
            Log.d(TAG, "WERKS->" + WERKS);
        if (!USER.isEmpty())
            Log.d(TAG, "USER->" + USER);
        date = (EditText) view.findViewById(R.id.date);
        Store_name_et = (EditText) view.findViewById(R.id.store_name);
        pick_list_et = (EditText) view.findViewById(R.id.pick_list);
        //UIFuncs.disableKeyInput(pick_list_et, view, con);
        mResponseView = (TextView) view.findViewById(R.id.response);
        EditTextDate editTextDate = new EditTextDate(getContext());
        editTextDate.setDateOnView(date);
        //  date.setEnabled(false);

        back = (Button) view.findViewById(R.id.back);
        back.setOnClickListener(this);

        next = (Button) view.findViewById(R.id.next);
        next.setOnClickListener(this);

        Store_name_et.setText(WERKS);
        pick_list_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                    /*|| (keyEvent.getAction() == KeyEvent.KEYCODE_ENTER)*/) {

                    String list = pick_list_et.getText().toString();
                    if (list == null || list.length() < 0 || list.equals("")) {

                        box.getBox("Alert", "Please Enter Pick List No. !!");
                        return true;
                    } else {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(pick_list_et.getWindowToken(), 0);

                        next.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
        try {
            loadData();

        } catch (Exception e) {
            box.getErrBox(e);
        }
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void networkCall() {
        String pickList = pick_list_et.getText().toString().trim();
        if (TextUtils.isEmpty(pickList)) {
            box.getBox("Alert", "Please enter pick list first");
            return;
        }
        String store = Store_name_et.getText().toString().trim();
        if (TextUtils.isEmpty(store)) {
            box.getBox("Alert", "Start Again , Store Name not Found");
            return;
        }
        if (TextUtils.isEmpty(URL)) {
            box.getBox("Err", "Server URL not found");
            return;
        }
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();
        getPickList(store, pickList);
    }

    private void getPickList(String store, String pickList) {
        final JSONObject params = new JSONObject();
        try {
            params.put("bapiname", RFC_GET_PICKLIST);
            params.put("IM_WERKS", store);
            params.put("IM_PICNR", pickList);
            params.put("IM_LGORT", "0002");
            params.put("IM_LGNUM", "SDC");
        } catch (JSONException e) {
            dismissDialog();
            box.getErrBox(e);
            return;
        }

        String url = URL.substring(0, URL.lastIndexOf("/"))
                + "/noacljsonrfcadaptor?bapiname=" + RFC_GET_PICKLIST + "&aclclientid=android";
        Log.d(TAG, "URL->" + url);
        Log.d(TAG, "payload ->" + params);

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest request = new SapJsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responsebody) {
                        dismissDialog();
                        Log.d(TAG, "response ->" + responsebody);
                        if (responsebody == null || responsebody.length() == 0) {
                            box.getBox("Err", "Empty response from Server/Unable to connect server");
                            return;
                        }
                        try {
                            if (isErrorReturn(responsebody)) {
                                return;
                            }
                            openScanScreen(responsebody);
                        } catch (Exception e) {
                            box.getErrBox(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error :" + error.toString());
                String err;
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
                } else {
                    err = error.toString();
                }
                dismissDialog();
                box.getBox("Err", err);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return params.toString().getBytes();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(request);
    }

    private boolean isErrorReturn(JSONObject responsebody) throws JSONException {
        if (!responsebody.has("EX_RETURN") || responsebody.isNull("EX_RETURN")) {
            return false;
        }
        Object ret = responsebody.get("EX_RETURN");
        JSONObject returnobj = null;
        if (ret instanceof JSONObject) {
            returnobj = (JSONObject) ret;
        } else if (ret instanceof JSONArray && ((JSONArray) ret).length() > 0) {
            returnobj = ((JSONArray) ret).optJSONObject(0);
        }
        if (returnobj == null) {
            return false;
        }
        String type = returnobj.optString("TYPE", "").trim();
        if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
            String message = returnobj.optString("MESSAGE", "").trim();
            if (message.isEmpty()) {
                message = "Error from server";
            }
            box.getBox("Response", message);
            return true;
        }
        return false;
    }

    private void openScanScreen(JSONObject responsebody) throws JSONException {
        loadData();
        int pickCount = fillPickList(responsebody.optJSONArray("ET_PICKLIST"));
        int eanCount = fillEan(responsebody.optJSONArray("ET_EAN_DATA"));
        Log.d(TAG, "DTPO :" + dtPO);
        Log.d(TAG, "DTean :" + dtEAN);
        if (pickCount == 0) {
            box.getBox("Alert", "No pick list data found");
            return;
        }
        if (eanCount == 0) {
            box.getBox("Alert", "No barcode data found for this pick list");
            return;
        }

        Bundle args = new Bundle();
        args.putSerializable("po_list", dtPO);
        args.putSerializable("ean_list", dtEAN);
        args.putSerializable("picklist_number", pick_list_et.getText().toString().trim());

        Fragment fragment = new Scan_Picking_against_picking_Process_Fragment();
        fragment.setArguments(args);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home, fragment, "picking_against_picking");
        ft.addToBackStack("picking_against_picking");
        ft.commit();
    }

    private int fillPickList(JSONArray rows) throws JSONException {
        if (rows == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null || SapJsonRows.isMetadataRow(row, "MATERIAL", "BIN", "PLANT")) {
                continue;
            }
            String material = row.optString("MATERIAL", "").trim();
            String bin = row.optString("BIN", "").trim();
            if (material.isEmpty()) {
                continue;
            }
            dtPO.get(0).add(material);
            dtPO.get(1).add(firstQty(row, "AVL_STOCK", "OPEN_STOCK", "PICK_QTY"));
            dtPO.get(2).add(firstQty(row, "OPEN_STOCK", "PICK_QTY", "SCAN_QTY"));
            dtPO.get(3).add(bin);
            count++;
        }
        return count;
    }

    private int fillEan(JSONArray rows) throws JSONException {
        if (rows == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null || SapJsonRows.isMetadataRow(row, "MATNR", "EAN11")) {
                continue;
            }
            String matnr = row.optString("MATNR", "").trim();
            String ean = row.optString("EAN11", "").trim();
            if (matnr.isEmpty() || ean.isEmpty()) {
                continue;
            }
            dtEAN.get(0).add(row.optString("MANDT", "").trim());
            dtEAN.get(1).add(matnr);
            dtEAN.get(2).add(ean);
            dtEAN.get(3).add(wholeNumber(row.optString("UMREZ", "1")));
            dtEAN.get(4).add(row.optString("EANNR", "").trim());
            count++;
        }
        return count;
    }

    private String firstQty(JSONObject row, String... keys) {
        for (String key : keys) {
            if (!row.has(key) || row.isNull(key)) {
                continue;
            }
            String value = row.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "0";
    }

    private String wholeNumber(String value) {
        if (value == null) {
            return "1";
        }
        String number = value.trim();
        if (number.isEmpty()) {
            return "1";
        }
        int dot = number.indexOf('.');
        if (dot >= 0) {
            number = number.substring(0, dot);
        }
        if (number.isEmpty()) {
            return "1";
        }
        return number;
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
