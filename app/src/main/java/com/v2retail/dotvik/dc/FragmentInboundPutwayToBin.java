package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.android.volley.Request;

/**
 * Screen 02 — Inbound Putway to BIN
 * Flow: Scan BIN (ZVND_PUTWAY_BIN_VAL_RFC)
 *       Scan Palette (ZVND_PUTWAY_PALETTE_VAL_RFC — shows PO/Inv/Vendor/box count)
 *       Save (ZVND_PUTWAY_SAVE_DATA_RFC)
 *       After save: clear BIN+Palette only, keep PO/Vendor/Inv.
 * @version 12.106
 */
public class FragmentInboundPutwayToBin extends Fragment implements View.OnClickListener {

    private static final String RFC_BIN  = "ZVND_PUTWAY_BIN_VAL_RFC";
    private static final String RFC_PALL = "ZVND_PUTWAY_PALETTE_VAL_RFC";
    private static final String RFC_SAVE = "ZVND_PUTWAY_SAVE_DATA_RFC";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etBin, etPalette;
    private TextView tvDc, tvPo, tvInv, tvVendor, tvBoxCount, tvStatus;
    private Button btnSave, btnReset, btnBack;

    private String URL="",USER="",WERKS="";
    private boolean binValidated=false,pallValidated=false;
    private String validatedBin="",validatedPall="",poNo="",billNo="",vendorName="";

    public FragmentInboundPutwayToBin(){}
    public static FragmentInboundPutwayToBin newInstance(){return new FragmentInboundPutwayToBin();}

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
        view=inflater.inflate(R.layout.fragment_inbound_putway_to_bin,container,false);
        tvDc      =view.findViewById(R.id.tv_dc);
        etBin     =view.findViewById(R.id.et_bin);
        etPalette =view.findViewById(R.id.et_palette);
        tvPo      =view.findViewById(R.id.tv_po);
        tvInv     =view.findViewById(R.id.tv_inv);
        tvVendor  =view.findViewById(R.id.tv_vendor);
        tvBoxCount=view.findViewById(R.id.tv_box_count);
        tvStatus  =view.findViewById(R.id.tv_status);
        btnSave   =view.findViewById(R.id.btn_save);
        btnReset  =view.findViewById(R.id.btn_reset);
        btnBack   =view.findViewById(R.id.btn_back);
        btnSave.setOnClickListener(this);btnReset.setOnClickListener(this);btnBack.setOnClickListener(this);
        etBin.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override public boolean onEditorAction(TextView v,int a,android.view.KeyEvent e){
                String b=etBin.getText().toString().trim();if(!b.isEmpty())validateBin(b);return true;}});
        etPalette.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override public boolean onEditorAction(TextView v,int a,android.view.KeyEvent e){
                if(!binValidated){showStatus("Scan BIN first.",false);return true;}
                String p=etPalette.getText().toString().trim();if(!p.isEmpty())validatePalette(p);return true;}});
        init();return view;
    }

    private void init(){
        activity=getActivity();box=new AlertBox(activity);
        SharedPreferencesData prefs=new SharedPreferencesData(activity);
        URL=prefs.read("URL");USER=prefs.read("USER");WERKS=prefs.read("WERKS");
        tvDc.setText("DC: "+WERKS);
        etPalette.setEnabled(false);btnSave.setEnabled(false);
        showStatus("Scan Destination BIN.",true);
    }

    private void validateBin(final String bin){
        showProgress("Validating BIN...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_BIN);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_BIN",bin);}
        catch(JSONException e){dismissProgress();return;}
        rfc(RFC_BIN,p,new Cb(){
            @Override public void ok(JSONObject r){
                JSONObject ret=r.optJSONObject("EX_RETURN");
                String type=ret!=null?ret.optString("TYPE",""):"";
                if("S".equalsIgnoreCase(type)||type.isEmpty()){
                    binValidated=true;validatedBin=bin;
                    etBin.setEnabled(false);etPalette.setEnabled(true);etPalette.requestFocus();
                    showStatus("BIN OK: "+bin+" — Scan Palette.",true);
                }else{
                    String msg=ret!=null?ret.optString("MESSAGE",""):"";
                    showStatus("BIN Error: "+msg,false);etBin.setText("");etBin.requestFocus();
                }
            }
            @Override public void err(String e){showStatus("Network: "+e,false);}
        });
    }

    private void validatePalette(final String palette){
        showProgress("Validating Palette...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_PALL);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_BIN",validatedBin);p.put("IM_PALL",palette);}
        catch(JSONException e){dismissProgress();return;}
        rfc(RFC_PALL,p,new Cb(){
            @Override public void ok(JSONObject r){
                try{
                    JSONObject ret=r.optJSONObject("EX_RETURN");
                    String type=ret!=null?ret.optString("TYPE",""):"";
                    if("S".equalsIgnoreCase(type)||type.isEmpty()){
                        pallValidated=true;validatedPall=palette;
                        JSONArray et=r.optJSONArray("ET_DATA");
                        if(et!=null&&et.length()>0){
                            JSONObject row=et.getJSONObject(0);
                            poNo=row.optString("PO_NO","");billNo=row.optString("BILL_NO","");vendorName=row.optString("VENDOR_NAME","");
                            tvPo.setText("PO: "+poNo);tvPo.setVisibility(View.VISIBLE);
                            tvInv.setText("INV: "+billNo);tvInv.setVisibility(View.VISIBLE);
                            tvVendor.setText("Vendor: "+vendorName);tvVendor.setVisibility(View.VISIBLE);
                            tvBoxCount.setText("Boxes: "+et.length());tvBoxCount.setVisibility(View.VISIBLE);
                        }
                        etPalette.setEnabled(false);btnSave.setEnabled(true);
                        showStatus("Palette OK — Ready to Save.",true);
                    }else{
                        String msg=ret!=null?ret.optString("MESSAGE",""):"";
                        showStatus("Palette Error: "+msg,false);etPalette.setText("");etPalette.requestFocus();
                    }
                }catch(JSONException e){showStatus("Parse error",false);}
            }
            @Override public void err(String e){showStatus("Network: "+e,false);}
        });
    }

    private void save(){
        if(!pallValidated){showStatus("Validate Palette first.",false);return;}
        showProgress("Saving...");
        JSONObject p=new JSONObject();
        try{
            p.put("bapiname",RFC_SAVE);p.put("IM_USER",USER);
            JSONObject row=new JSONObject();
            row.put("PLANT",WERKS);row.put("BIN",validatedBin);row.put("PALETTE",validatedPall);
            row.put("PO_NO",poNo);row.put("BILL_NO",billNo);
            JSONArray it=new JSONArray();it.put(row);p.put("IT_DATA",it);
        }catch(JSONException e){dismissProgress();return;}
        rfc(RFC_SAVE,p,new Cb(){
            @Override public void ok(JSONObject r){
                JSONObject ret=r.optJSONObject("EX_RETURN");
                String type=ret!=null?ret.optString("TYPE",""):"";
                if("S".equalsIgnoreCase(type)||type.isEmpty()){
                    showStatus("Saved! Palette "+validatedPall+" to BIN "+validatedBin,true);
                    partialReset();
                }else{
                    String msg=ret!=null?ret.optString("MESSAGE",""):"";
                    showStatus("Save Error: "+msg,false);
                }
            }
            @Override public void err(String e){showStatus("Network: "+e,false);}
        });
    }

    private void partialReset(){
        binValidated=false;pallValidated=false;validatedBin="";validatedPall="";
        etBin.setText("");etPalette.setText("");
        etBin.setEnabled(true);etPalette.setEnabled(false);btnSave.setEnabled(false);
        etBin.requestFocus();showStatus("BIN cleared — Scan next BIN.",true);
    }

    private void fullReset(){
        partialReset();poNo="";billNo="";vendorName="";
        tvPo.setVisibility(View.GONE);tvInv.setVisibility(View.GONE);
        tvVendor.setVisibility(View.GONE);tvBoxCount.setVisibility(View.GONE);
        showStatus("Scan Destination BIN.",true);
    }

    @Override public void onClick(View v){
        int id=v.getId();
        if(id==R.id.btn_save)save();
        else if(id==R.id.btn_reset)fullReset();
        else if(id==R.id.btn_back){if(getFragmentManager()!=null)getFragmentManager().popBackStack();}
    }

    private interface Cb{void ok(JSONObject r);void err(String e);}
    private void rfc(String name,JSONObject params,final Cb cb){
        String base=URL.contains("/ValueXMW")?URL.replace("/ValueXMW",""):URL;
        JsonObjectRequest req=new JsonObjectRequest(Request.Method.POST,base+"/noacljsonrfcadaptor?bapiname="+name+"&aclclientid=android",params,
            new Response.Listener<JSONObject>(){@Override public void onResponse(JSONObject r){dismissProgress();cb.ok(r);}},
            new Response.ErrorListener(){@Override public void onErrorResponse(VolleyError e){dismissProgress();cb.err(e.getMessage()!=null?e.getMessage():"Network error");}});
        req.setRetryPolicy(new DefaultRetryPolicy(90000,0,1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }
    private void showStatus(String msg,boolean ok){if(tvStatus==null)return;tvStatus.setVisibility(View.VISIBLE);tvStatus.setText(msg);tvStatus.setBackgroundColor(ok?0xFFE8F5E9:0xFFFFEBEE);tvStatus.setTextColor(ok?0xFF065F46:0xFFB71C1C);}
    private void showProgress(String msg){if(dialog==null||!dialog.isShowing()){dialog=new ProgressDialog(activity);dialog.setCancelable(false);}dialog.setMessage(msg);dialog.show();}
    private void dismissProgress(){if(dialog!=null&&dialog.isShowing())dialog.dismiss();}
}
