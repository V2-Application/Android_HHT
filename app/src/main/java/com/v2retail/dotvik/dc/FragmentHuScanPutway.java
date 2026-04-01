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
 * Screen 01 — Unloading: HU Scanning & Putway to Palette
 * Flow: Enter Vehicle → Scan HU (ZVND_UNLOAD_HU_VALIDATE_RFC, shows PO/Inv/Vendor)
 *       → Scan Palette (ZVND_UNLOAD_PALLATE_VALIDATION)
 *       → Save (ZVND_UNLOAD_SAVE_RFC)
 * @version 12.106
 */
public class FragmentHuScanPutway extends Fragment implements View.OnClickListener {

    private static final String RFC_HU_VAL   = "ZVND_UNLOAD_HU_VALIDATE_RFC";
    private static final String RFC_PALL_VAL = "ZVND_UNLOAD_PALLATE_VALIDATION";
    private static final String RFC_SAVE     = "ZVND_UNLOAD_SAVE_RFC";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etVehicle, etHu, etPalette;
    private TextView tvDc, tvPo, tvInv, tvVendor, tvStatus;
    private Button btnSave, btnReset, btnBack;

    private String URL="",USER="",WERKS="";
    private boolean huValidated=false;
    private String validatedHu="",poNo="",billNo="";

    public FragmentHuScanPutway(){}
    public static FragmentHuScanPutway newInstance(){return new FragmentHuScanPutway();}

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
        view=inflater.inflate(R.layout.fragment_hu_scan_putway,container,false);
        tvDc    =view.findViewById(R.id.tv_dc);
        etVehicle=view.findViewById(R.id.et_vehicle);
        etHu    =view.findViewById(R.id.et_hu);
        etPalette=view.findViewById(R.id.et_palette);
        tvPo    =view.findViewById(R.id.tv_po);
        tvInv   =view.findViewById(R.id.tv_inv);
        tvVendor=view.findViewById(R.id.tv_vendor);
        tvStatus=view.findViewById(R.id.tv_status);
        btnSave =view.findViewById(R.id.btn_save);
        btnReset=view.findViewById(R.id.btn_reset);
        btnBack =view.findViewById(R.id.btn_back);
        btnSave.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        etHu.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override public boolean onEditorAction(TextView v,int a,android.view.KeyEvent e){
                String hu=etHu.getText().toString().trim();if(!hu.isEmpty())validateHu(hu);return true;}});
        etPalette.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override public boolean onEditorAction(TextView v,int a,android.view.KeyEvent e){
                if(!huValidated){showStatus("Scan HU first.",false);return true;}
                String p=etPalette.getText().toString().trim();if(!p.isEmpty())validatePalette(p);return true;}});
        init();return view;
    }

    private void init(){
        activity=getActivity();box=new AlertBox(activity);
        SharedPreferencesData prefs=new SharedPreferencesData(activity);
        URL=prefs.read("URL");USER=prefs.read("USER");WERKS=prefs.read("WERKS");
        tvDc.setText("DC: "+WERKS);
        etPalette.setEnabled(false);btnSave.setEnabled(false);
        showStatus("Enter Vehicle No. and scan HU.",true);
    }

    private void validateHu(final String hu){
        showProgress("Validating HU...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_HU_VAL);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_HU",hu);}
        catch(JSONException e){dismissProgress();return;}
        rfc(RFC_HU_VAL,p,new Cb(){
            @Override public void ok(JSONObject r){
                try{
                    JSONObject ret=r.optJSONObject("EX_RETURN");
                    String type=ret!=null?ret.optString("TYPE",""):"";
                    if("S".equalsIgnoreCase(type)||type.isEmpty()){
                        huValidated=true;validatedHu=hu;
                        JSONArray et=r.optJSONArray("ET_DATA");
                        if(et!=null&&et.length()>0){
                            JSONObject row=et.getJSONObject(0);
                            poNo=row.optString("PO_NO","");billNo=row.optString("BILL_NO","");
                            String vendor=row.optString("VENDOR_NAME","");
                            tvPo.setText("PO: "+poNo);tvPo.setVisibility(View.VISIBLE);
                            tvInv.setText("INV: "+billNo);tvInv.setVisibility(View.VISIBLE);
                            tvVendor.setText("Vendor: "+vendor);tvVendor.setVisibility(View.VISIBLE);
                        }
                        etHu.setEnabled(false);etPalette.setEnabled(true);etPalette.requestFocus();
                        showStatus("HU OK: "+hu+" — Scan Palette.",true);
                    }else{
                        String msg=ret!=null?ret.optString("MESSAGE",""):"";
                        showStatus("HU Error: "+msg,false);etHu.setText("");etHu.requestFocus();
                    }
                }catch(JSONException e){showStatus("Parse error",false);}
            }
            @Override public void err(String e){showStatus("Network: "+e,false);}
        });
    }

    private void validatePalette(final String palette){
        showProgress("Validating Palette...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_PALL_VAL);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_HU",validatedHu);p.put("IM_PALL",palette);}
        catch(JSONException e){dismissProgress();return;}
        rfc(RFC_PALL_VAL,p,new Cb(){
            @Override public void ok(JSONObject r){
                JSONObject ret=r.optJSONObject("EX_RETURN");
                String type=ret!=null?ret.optString("TYPE",""):"";
                if("S".equalsIgnoreCase(type)||type.isEmpty()){
                    btnSave.setEnabled(true);etPalette.setEnabled(false);
                    showStatus("Palette OK — Ready to Save.",true);
                }else{
                    String msg=ret!=null?ret.optString("MESSAGE",""):"";
                    showStatus("Palette Error: "+msg,false);etPalette.setText("");etPalette.requestFocus();
                }
            }
            @Override public void err(String e){showStatus("Network: "+e,false);}
        });
    }

    private void save(){
        String vehicle=etVehicle.getText().toString().trim();
        String palette=etPalette.getText().toString().trim();
        if(vehicle.isEmpty()){showStatus("Enter Vehicle No.",false);return;}
        showProgress("Saving...");
        JSONObject p=new JSONObject();
        try{
            p.put("bapiname",RFC_SAVE);p.put("IM_USER",USER);
            JSONObject parms=new JSONObject();
            parms.put("PLANT",WERKS);parms.put("VEHICLE",vehicle);
            parms.put("EXT_HU",validatedHu);parms.put("PALETTE",palette);
            parms.put("PO_NO",poNo);parms.put("BILL_NO",billNo);
            p.put("IM_PARMS",parms);
        }catch(JSONException e){dismissProgress();return;}
        rfc(RFC_SAVE,p,new Cb(){
            @Override public void ok(JSONObject r){
                JSONObject ret=r.optJSONObject("EX_RETURN");
                String type=ret!=null?ret.optString("TYPE",""):"";
                if("S".equalsIgnoreCase(type)||type.isEmpty()){
                    showStatus("Saved! HU "+validatedHu+" to Palette "+palette,true);resetFields();
                }else{
                    String msg=ret!=null?ret.optString("MESSAGE",""):"";
                    showStatus("Save Error: "+msg,false);
                }
            }
            @Override public void err(String e){showStatus("Network: "+e,false);}
        });
    }

    private void resetFields(){
        huValidated=false;validatedHu=poNo=billNo="";
        etVehicle.setText("");etHu.setText("");etPalette.setText("");
        etHu.setEnabled(true);etPalette.setEnabled(false);btnSave.setEnabled(false);
        tvPo.setVisibility(View.GONE);tvInv.setVisibility(View.GONE);tvVendor.setVisibility(View.GONE);
        etHu.requestFocus();showStatus("Enter Vehicle No. and scan HU.",true);
    }

    @Override public void onClick(View v){
        int id=v.getId();
        if(id==R.id.btn_save)save();
        else if(id==R.id.btn_reset)resetFields();
        else if(id==R.id.btn_back){if(getFragmentManager()!=null)getFragmentManager().popBackStack();}
    }

    private interface Cb{void ok(JSONObject r);void err(String e);}
    private void rfc(String name,JSONObject params,final Cb cb){
        String base=URL.contains("/ValueXMW")?URL.replace("/ValueXMW",""):URL;
        String url=base+"/noacljsonrfcadaptor?bapiname="+name+"&aclclientid=android";
        JsonObjectRequest req=new JsonObjectRequest(Request.Method.POST,url,params,
            new Response.Listener<JSONObject>(){@Override public void onResponse(JSONObject r){dismissProgress();cb.ok(r);}},
            new Response.ErrorListener(){@Override public void onErrorResponse(VolleyError e){dismissProgress();cb.err(e.getMessage()!=null?e.getMessage():"Network error");}});
        req.setRetryPolicy(new DefaultRetryPolicy(90000,0,1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }
    private void showStatus(String msg,boolean ok){if(tvStatus==null)return;tvStatus.setVisibility(View.VISIBLE);tvStatus.setText(msg);tvStatus.setBackgroundColor(ok?0xFFE8F5E9:0xFFFFEBEE);tvStatus.setTextColor(ok?0xFF065F46:0xFFB71C1C);}
    private void showProgress(String msg){if(dialog==null||!dialog.isShowing()){dialog=new ProgressDialog(activity);dialog.setCancelable(false);}dialog.setMessage(msg);dialog.show();}
    private void dismissProgress(){if(dialog!=null&&dialog.isShowing())dialog.dismiss();}
}
