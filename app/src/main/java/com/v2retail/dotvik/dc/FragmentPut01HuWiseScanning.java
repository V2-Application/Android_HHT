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
 * PUT01 — HU Wise Scanning (Inbound Process)
 * Flow: Scan HU → ZVND_PUT01_HU_VAL_RFC (auto-fetches PO, INV, HU QTY)
 *       Save → ZVND_PUT01_SAVE_DATA_RFC
 * @version 12.106
 */
public class FragmentPut01HuWiseScanning extends Fragment implements View.OnClickListener {

    private static final String RFC_HU_VAL = "ZVND_PUT01_HU_VAL_RFC";
    private static final String RFC_SAVE   = "ZVND_PUT01_SAVE_DATA_RFC";

    private View view;
    private Activity activity;
    private ProgressDialog dialog;
    private AlertBox box;

    private EditText etHu;
    private TextView tvDc, tvPo, tvInv, tvQty, tvStatus;
    private Button btnSave, btnReset, btnBack;

    private String URL="",USER="",WERKS="";
    private boolean huValidated=false;
    private String validatedHu="",poNo="",invNo="",huQty="";

    public FragmentPut01HuWiseScanning(){}
    public static FragmentPut01HuWiseScanning newInstance(){return new FragmentPut01HuWiseScanning();}

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
        view=inflater.inflate(R.layout.fragment_put01_hu_wise_scanning,container,false);
        tvDc   =view.findViewById(R.id.tv_dc);
        etHu   =view.findViewById(R.id.et_hu);
        tvPo   =view.findViewById(R.id.tv_po);
        tvInv  =view.findViewById(R.id.tv_inv);
        tvQty  =view.findViewById(R.id.tv_qty);
        tvStatus=view.findViewById(R.id.tv_status);
        btnSave =view.findViewById(R.id.btn_save);
        btnReset=view.findViewById(R.id.btn_reset);
        btnBack =view.findViewById(R.id.btn_back);

        btnSave.setOnClickListener(this);btnReset.setOnClickListener(this);btnBack.setOnClickListener(this);
        etHu.setOnEditorActionListener((v,a,e)->{String hu=etHu.getText().toString().trim();if(!hu.isEmpty())validateHu(hu);return true;});
        init();return view;
    }

    private void init(){
        activity=getActivity();box=new AlertBox(activity);
        SharedPreferencesData prefs=new SharedPreferencesData(activity);
        URL=prefs.read("URL");USER=prefs.read("USER");WERKS=prefs.read("WERKS");
        tvDc.setText("DC: "+WERKS);
        btnSave.setEnabled(false);
        showStatus("Scan HU Barcode.",true);
    }

    private void validateHu(String hu){
        showProgress("Validating HU...");
        JSONObject p=new JSONObject();
        try{p.put("bapiname",RFC_HU_VAL);p.put("IM_USER",USER);p.put("IM_PLANT",WERKS);p.put("IM_HU",hu);}
        catch(JSONException e){dismissProgress();return;}
        callRfc(RFC_HU_VAL,p,r->{
            try{if(isSuccess(r)){
                huValidated=true;validatedHu=hu;
                JSONArray et=r.optJSONArray("ET_DATA");
                if(et!=null&&et.length()>0){JSONObject row=et.getJSONObject(0);poNo=row.optString("PO_NO","");invNo=row.optString("INV_NO","");huQty=row.optString("HU_QTY","");}
                tvPo.setText("PO: "+poNo);tvPo.setVisibility(View.VISIBLE);
                tvInv.setText("INV: "+invNo);tvInv.setVisibility(View.VISIBLE);
                tvQty.setText("HU Qty: "+huQty);tvQty.setVisibility(View.VISIBLE);
                etHu.setEnabled(false);btnSave.setEnabled(true);
                showStatus("HU OK: "+hu+" — Ready to Save.",true);
            }else{showStatus("HU Error: "+getMsg(r),false);etHu.setText("");etHu.requestFocus();}}
            catch(JSONException e){showStatus("Parse error",false);}
        },err->showStatus("Network: "+err,false));
    }

    private void save(){
        if(!huValidated){showStatus("Validate HU first.",false);return;}
        showProgress("Saving...");
        JSONObject p=new JSONObject();
        try{
            p.put("bapiname",RFC_SAVE);p.put("IM_USER",USER);
            JSONObject row=new JSONObject();
            row.put("PLANT",WERKS);row.put("HU",validatedHu);row.put("PO_NO",poNo);row.put("BILL_NO",invNo);row.put("HU_QTY",huQty);
            JSONArray it=new JSONArray();it.put(row);p.put("IT_DATA",it);
        }catch(JSONException e){dismissProgress();return;}
        callRfc(RFC_SAVE,p,r->{
            try{if(isSuccess(r)){showStatus("✔ Saved! HU "+validatedHu,true);resetFields();}
            else{showStatus("Save Error: "+getMsg(r),false);}}
            catch(JSONException e){showStatus("Parse error",false);}
        },err->showStatus("Network: "+err,false));
    }

    private void resetFields(){
        huValidated=false;validatedHu=poNo=invNo=huQty="";
        etHu.setText("");etHu.setEnabled(true);btnSave.setEnabled(false);
        tvPo.setVisibility(View.GONE);tvInv.setVisibility(View.GONE);tvQty.setVisibility(View.GONE);
        etHu.requestFocus();showStatus("Scan HU Barcode.",true);
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
