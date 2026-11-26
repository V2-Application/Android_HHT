package com.v2retail.dotvik.store.directpicking;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.v2retail.ApplicationController;
import com.v2retail.commons.SapJsonObjectRequest;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentMsaV01PicklistPicking extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentMsaV01PicklistPicking.class.getName();
    private static final int REQUEST_GET_PICKLIST = 1600;
    private static final String[] PICKLIST_TYPES = new String[]{"V01-HU", "MSA"};
    private static final String PICKLIST_HINT = "Select Picklist";
    private static final String TYPE_V01_HU = "V01-HU";

    FragmentManager fm;
    EditText txt_store, txt_date;
    Spinner dd_picklist_type, dd_picklist;
    Button btn_back, btn_next;
    List<String> picklists = new ArrayList<>();
    ArrayAdapter<String> picklistAdapter;

    String URL = "";
    String WERKS = "";
    String USER = "";
    ProgressDialog dialog;
    boolean requestInFlight = false;

    public FragmentMsaV01PicklistPicking() {
    }

    public static FragmentMsaV01PicklistPicking newInstance() {
        return new FragmentMsaV01PicklistPicking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("MSA & V01 Picklist Picking");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_msa_v01_picklist_picking, container, false);

        txt_store = rootView.findViewById(R.id.txt_msa_v01_picklist_store);
        txt_date = rootView.findViewById(R.id.txt_msa_v01_picklist_date);
        dd_picklist_type = rootView.findViewById(R.id.dd_msa_v01_picklist_type);
        dd_picklist = rootView.findViewById(R.id.dd_msa_v01_picklist_no);
        btn_back = rootView.findViewById(R.id.btn_msa_v01_picklist_back);
        btn_next = rootView.findViewById(R.id.btn_msa_v01_picklist_next);

        SharedPreferencesData data = new SharedPreferencesData(getContext());
        URL = data.read("URL");
        USER = data.read("USER");
        WERKS = data.read("WERKS");
        txt_store.setText(WERKS == null ? "" : WERKS);
        txt_date.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, PICKLIST_TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_picklist_type.setAdapter(typeAdapter);

        picklists.add(PICKLIST_HINT);
        picklistAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, picklists);
        picklistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dd_picklist.setAdapter(picklistAdapter);

        dd_picklist_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = parent.getItemAtPosition(position) == null
                        ? ""
                        : parent.getItemAtPosition(position).toString();
                if (TYPE_V01_HU.equals(type)) {
                    getPicklist();
                } else {
                    resetPicklistDropdown();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btn_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_msa_v01_picklist_back) {
            fm.popBackStack();
        } else if (id == R.id.btn_msa_v01_picklist_next) {
            String picklistNo = "";
            if (dd_picklist.getSelectedItemPosition() > 0 && dd_picklist.getSelectedItem() != null) {
                picklistNo = dd_picklist.getSelectedItem().toString().trim();
            }
            Fragment fragment = FragmentV01HUSorting.newInstance(picklistNo);
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "direct_picking_01_0001");
            ft.addToBackStack("direct_picking_01_0001");
            ft.commit();
        }
    }

    private void resetPicklistDropdown() {
        picklists.clear();
        picklists.add(PICKLIST_HINT);
        picklistAdapter.notifyDataSetChanged();
        dd_picklist.setSelection(0);
    }

    private void getPicklist() {
        if (requestInFlight) {
            return;
        }
        requestInFlight = true;
        JSONObject args = new JSONObject();
        try {
            args.put("bapiname", Vars.ZWM_V01_GET_PICKLIST);
            args.put("IM_USER", USER == null ? "" : USER);
            args.put("IM_PLANT", WERKS == null ? "" : WERKS);
            showProcessingAndSubmit(Vars.ZWM_V01_GET_PICKLIST, REQUEST_GET_PICKLIST, args);
        } catch (JSONException e) {
            requestInFlight = false;
            e.printStackTrace();
            dismissDialog();
            new AlertBox(getContext()).getErrBox(e);
        }
    }

    private void bindPicklist(JSONObject responsebody) {
        picklists.clear();
        picklists.add(PICKLIST_HINT);
        JSONArray rows = responsebody.optJSONArray("ET_PICKLIST");
        if (rows != null) {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String pickNo = row.optString("PICK_NO", "").trim();
                if (pickNo.isEmpty() || "PICK_NO".equalsIgnoreCase(pickNo)) {
                    continue;
                }
                picklists.add(pickNo);
            }
        }
        picklistAdapter.notifyDataSetChanged();
        dd_picklist.setSelection(0);
        if (picklists.size() == 1) {
            new AlertBox(getContext()).getBox("No Picklist", "No picklist found");
        }
    }

    private void showProcessingAndSubmit(String rfc, int request, JSONObject args) {
        dismissDialog();
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    submitRequest(rfc, request, args);
                } catch (Exception e) {
                    requestInFlight = false;
                    dismissDialog();
                    new AlertBox(getContext()).getErrBox(e);
                }
            }
        }, 1000);
    }

    private void submitRequest(String rfc, int request, JSONObject args) {
        if (URL == null || URL.lastIndexOf("/") < 0) {
            requestInFlight = false;
            dismissDialog();
            new AlertBox(getContext()).getBox("Err", "Server URL is not set");
            return;
        }
        String url = URL.substring(0, URL.lastIndexOf("/"));
        url += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";

        final JSONObject params = args;
        Log.d(TAG, "payload ->" + params.toString());

        RequestQueue mRequestQueue = ApplicationController.getInstance().getRequestQueue();
        JsonObjectRequest mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject responsebody) {
                requestInFlight = false;
                dismissDialog();
                Log.d(TAG, "response ->" + responsebody);
                if (responsebody == null || responsebody.length() == 0) {
                    UIFuncs.errorSound(getContext());
                    new AlertBox(getContext()).getBox("Err", "No response from Server");
                    return;
                }
                if (request != REQUEST_GET_PICKLIST) {
                    return;
                }
                try {
                    if (responsebody.has("EX_RETURN") && responsebody.get("EX_RETURN") instanceof JSONObject) {
                        JSONObject returnobj = responsebody.getJSONObject("EX_RETURN");
                        String type = returnobj.optString("TYPE", "");
                        if ("E".equalsIgnoreCase(type)) {
                            UIFuncs.errorSound(getContext());
                            resetPicklistDropdown();
                            String message = returnobj.optString("MESSAGE", "");
                            new AlertBox(getContext()).getBox("Err", message.isEmpty() ? "Unable to get picklist" : message);
                            return;
                        }
                    }
                    bindPicklist(responsebody);
                } catch (JSONException e) {
                    e.printStackTrace();
                    new AlertBox(getContext()).getErrBox(e);
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
                return super.parseNetworkResponse(response);
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
    }

    private Response.ErrorListener volleyErrorListener() {
        return new Response.ErrorListener() {
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
                requestInFlight = false;
                dismissDialog();
                resetPicklistDropdown();
                new AlertBox(getContext()).getBox("Err", err);
            }
        };
    }

    private void dismissDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        dialog = null;
    }
}
