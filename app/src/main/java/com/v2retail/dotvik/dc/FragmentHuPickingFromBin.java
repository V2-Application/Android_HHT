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
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * HU Picking From BIN (GateLot2 Process)
 * Flow: Enter Picklist → ZVND_GATELOT2_PICKLIST_VAL_RFC (auto-fetches PO/INV)
 *       Scan BIN → ZVND_GATELOT2_BIN_VAL_RFC
 *       Scan Palette → ZVND_GATELOT2_PALETTE_VAL_RFC
 *       Save → ZVND_GATELOT2_SAVE_DATA_RFC
 * @version 12.106
 */
public class FragmentHuPickingFromBin extends Fragment implements View.OnClickListener {

    private static final String RFC_PICKLIST = "ZVND_GATELOT2_PICKLIST_VAL_RFC";
    private static final String RFC_BIN      = "ZVND_GATELOT2_BIN_VAL_RFC";
    private static final String RFC_PALETTE  = "ZVND_GATELOT2_PALETTE_VAL_RFC";
    private static final String RFC_SAVE     = "ZVND_GATELOT2_SAVE_DATA_RFC";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etPicklist, etBin, etPalette;
    private TextView tvDc, tvPo, tvInv, tvStatus;
    private Button btnSave, btnReset, btnBack;

    private String URL="",USER="",WERKS="";
    private boolean plValidated=false,binValidated=false,pallValidated=false;
    private String validatedPl="",validatedBin="",validatedPall="";
    private String poNo="",invNo="";

    public FragmentHuPickingFromBin(){}
    public static FragmentHuPickingFromBin newInstance(){return new FragmentHuPickingFromBin();}

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
        view=inflater.inflate(R.layout.fragment_hu_picking_from_bin,container,false);
        tvDc      =view.findViewById(R.id.tv_dc);
        etPicklist=view.findViewById(R.id.et_picklist);
        etBin     =view.findViewById(R.id.et_bin);
        etPalette =view.findViewById(R.id.et_palette);
        tvPo      =view.findViewById(R.id.tv_po);
        tvInv     =view.findViewById(R.id.tv_inv);
        tvStatus  =view.findViewById(R.id.tv_status);
        btnSave   =view.findViewById(R.id.btn_save);
        btnReset  =view.findViewById(R.id.btn_reset);
        btnBack   =view.findViewById(R.id.btn_back);

        btnSave.setOnClickListener(this);btnReset.setOnClickListener(this);btnBack.setOnClickListener(this);
        etPicklist.setOnEditorActionListener((v,a,e)->{String pl=etPicklist.getText().toString().trim();if(!pl.isEmpty())validatePicklist(pl);return true;});
        etBin.setOnEditorActionListener((v,a,e)->{if(!plValidated){showStatus("Validate Picklist first.",false);return true;}String b=etBin.getText().toString().trim();if(!b.isEmpty())validateBin(b);return true;});
        etPalette.setOnEditorActionListener((v,a,e)->{if(!binValidated){showStatus("Validate BIN first.",false);return true;}String p=etPalette.getText().toString().trim();if(!p.isEmpty())validatePalette(p);return true;});
        init();return view;
    }

    private void init(){
        activity=getActivity();box=new AlertBox(activity);
        SharedPreferencesData prefs=new SharedPreferencesData(activity);
        URL=prefs.read("URL");USER=prefs.read("USER");WERKS=prefs.read("WERKS");
        tvDc.setText("DC: "+WERKS);
        etBin.setEnabled(false);etPalette.setEnabled(false);btnSave.setEnabled(false);
        showStatus("Enter Picklist No.",true);
    }

    private void validatePicklist(String pl){
        showProgress("Validating Picklist...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_PICKLIST);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_PICKLIST",pl);}
        catch(JSONException e){dismissProgress();return;}
        callRfc(RFC_PICKLIST,p,r->{
            try{if(isSuccess(r)){
                plValidated=true;validatedPl=pl;
                JSONArray et=r.optJSONArray("ET_DATA");
                if(et!=null&&et.length()>0){JSONObject row=et.getJSONObject(0);poNo=row.optString("PO_NO","");invNo=row.optString("INV_NO","");}
                tvPo.setText("PO: "+poNo);tvPo.setVisibility(View.VISIBLE);
                tvInv.setText("INV: "+invNo);tvInv.setVisibility(View.VISIBLE);
                etPicklist.setEnabled(false);etBin.setEnabled(true);etBin.requestFocus();
                showStatus("Picklist OK — Scan BIN.",true);
            }else{showStatus("Picklist Error: "+getMsg(r),false);etPicklist.setText("");etPicklist.requestFocus();}}
            catch(JSONException e){showStatus("Parse error",false);}
        },err->showStatus("Network: "+err,false));
    }

    private void validateBin(String bin){
        showProgress("Validating BIN...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_BIN);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_PICKLIST",validatedPl);p.put("IM_BIN",bin);}
        catch(JSONException e){dismissProgress();return;}
        callRfc(RFC_BIN,p,r->{
            try{if(isSuccess(r)){binValidated=true;validatedBin=bin;etBin.setEnabled(false);etPalette.setEnabled(true);etPalette.requestFocus();showStatus("BIN OK: "+bin+" — Scan Palette.",true);}
            else{showStatus("BIN Error: "+getMsg(r),false);etBin.setText("");etBin.requestFocus();}}
            catch(JSONException e){showStatus("Parse error",false);}
        },err->showStatus("Network: "+err,false));
    }

    private void validatePalette(String palette){
        showProgress("Validating Palette...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_PALETTE);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_PICKLIST",validatedPl);p.put("IM_BIN",validatedBin);p.put("IM_PALL",palette);}
        catch(JSONException e){dismissProgress();return;}
        callRfc(RFC_PALETTE,p,r->{
            try{if(isSuccess(r)){pallValidated=true;validatedPall=palette;etPalette.setEnabled(false);btnSave.setEnabled(true);showStatus("Palette OK — Ready to Save.",true);}
            else{showStatus("Palette Error: "+getMsg(r),false);etPalette.setText("");etPalette.requestFocus();}}
            catch(JSONException e){showStatus("Parse error",false);}
        },err->showStatus("Network: "+err,false));
    }

    private void save(){
        if(!pallValidated){showStatus("Complete all validations.",false);return;}
        showProgress("Saving...");
        JSONObject p=new JSONObject();
        try{
            p.put("bapiname",RFC_SAVE);p.put("IM_USER",USER);
            JSONObject row=new JSONObject();
            row.put("PLANT",WERKS);row.put("PICKLIST",validatedPl);row.put("BIN",validatedBin);row.put("PALETTE",validatedPall);
            JSONArray it=new JSONArray();it.put(row);p.put("IT_DATA",it);
        }catch(JSONException e){dismissProgress();return;}
        callRfc(RFC_SAVE,p,r->{
            try{if(isSuccess(r)){showStatus("✔ Saved!",true);resetFields();}
            else{showStatus("Save Error: "+getMsg(r),false);}}
            catch(JSONException e){showStatus("Parse error",false);}
        },err->showStatus("Network: "+err,false));
    }

    private void resetFields(){
        plValidated=binValidated=pallValidated=false;
        validatedPl=validatedBin=validatedPall=poNo=invNo="";
        etPicklist.setText("");etBin.setText("");etPalette.setText("");
        etPicklist.setEnabled(true);etBin.setEnabled(false);etPalette.setEnabled(false);btnSave.setEnabled(false);
        tvPo.setVisibility(View.GONE);tvInv.setVisibility(View.GONE);
        etPicklist.requestFocus();showStatus("Enter Picklist No.",true);
    }

    @Override
    public void onClick(View v){
        int id=v.getId();
        if(id==R.id.btn_save)save();
        else if(id==R.id.btn_reset)resetFields();
        else if(id==R.id.btn_back){if(getFragmentManager()!=null)getFragmentManager().popBackStack();}
    }

    private void callRfc(String name,JSONObject params,RfcCallback cb){
        String base=URL.contains("/ValueXMW")?URL.replace("/ValueXMW",""):URL;
        JsonObjectRequest req=new JsonObjectRequest(Request.Method.POST,base+"/noacljsonrfcadaptor?bapiname="+name+"&aclclientid=android",params,
            r->{dismissProgress();cb.onSuccess(r);},e->{dismissProgress();cb.onError(parseErr(e));});
        req.setRetryPolicy(new DefaultRetryPolicy(90000,0,1f));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }
    interface RfcCallback{void onSuccess(JSONObject r);void onError(String e);}
    private void showStatus(String msg,boolean ok){if(tvStatus==null)return;tvStatus.setVisibility(View.VISIBLE);tvStatus.setText(msg);tvStatus.setBackgroundColor(ok?0xFFE8F5E9:0xFFFFEBEE);tvStatus.setTextColor(ok?0xFF065F46:0xFFB71C1C);}
    private void showProgress(String msg){if(dialog==null||!dialog.isShowing()){dialog=new ProgressDialog(activity);dialog.setCancelable(false);}dialog.setMessage(msg);dialog.show();}
    private void dismissProgress(){if(dialog!=null&&dialog.isShowing())dialog.dismiss();}
    private boolean isSuccess(JSONObject r){JSONObject ret=r.optJSONObject("EX_RETURN");if(ret==null)return true;String t=ret.optString("TYPE","");return"S".equalsIgnoreCase(t)||t.isEmpty();}
    private String getMsg(JSONObject r){JSONObject ret=r.optJSONObject("EX_RETURN");return ret!=null?ret.optString("MESSAGE",""):"" ;}
    private String parseErr(VolleyError e){if(e instanceof TimeoutError)return"Timeout";if(e instanceof NoConnectionError)return"No connection";if(e instanceof ParseError)return"Parse error";return e.getMessage()!=null?e.getMessage():"Unknown";}
}
