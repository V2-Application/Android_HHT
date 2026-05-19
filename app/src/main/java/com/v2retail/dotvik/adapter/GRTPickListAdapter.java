package com.v2retail.dotvik.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.v2retail.dotvik.dc.ptl.PTLPickCrateBin;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.grt.GRT_Pick_Crate_Bin;
import com.v2retail.dotvik.modal.grt.cratepick.ETPickList;

import java.util.ArrayList;

public class GRTPickListAdapter extends ArrayAdapter<ETPickList> {
    FragmentActivity activity;
    public GRTPickListAdapter(Context context, ArrayList<ETPickList> pickListItems){
        super(context,0,pickListItems);
        activity = (FragmentActivity) context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ETPickList pickList = getItem(position);
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_grt_pick_list,parent,false);
        }

        CardView cv_grt_picklist_item = convertView.findViewById(R.id.cv_grt_picklist_item);
        TextView tanum = convertView.findViewById(R.id.picklist_tanum);
        tanum.setText(pickList.getLgtanum());
        cv_grt_picklist_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFragmentFromAdapter(pickList);
            }
        });
        return convertView;
    }

    public void changeFragmentFromAdapter(ETPickList pickList)
    {
        Bundle args = new Bundle();
        args.putString("picklistno", pickList.getLgtanum() != null ? pickList.getLgtanum() : "");
        args.putString("section", pickList.getSection() != null ? pickList.getSection() : "");
        // PTL only: full vs partial mode
        args.putString("picktype", pickList.getPicktype() != null ? pickList.getPicktype() : "");
        Fragment fragment =  new GRT_Pick_Crate_Bin();
        if(pickList.isPtl()){
            fragment = new PTLPickCrateBin();
        }
        fragment.setArguments(args);
        clear();
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.home, fragment, pickList.isPtl() ? "Ptl_Pick_Crate_Bin":"Grt_Pick_Crate_Bin");
        ft.addToBackStack(pickList.isPtl() ? "Ptl_Pick_Crate_Bin":"Grt_Pick_Crate_Bin");
        ft.commit();
    }
}
