package com.v2retail.dotvik.dc.ptlnew.fullcrate30;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewArticlePutwayStorewise;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewFloorModule;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewHUCloseAndPrint;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewHUZoneStoreMapping;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewPTLPicking;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewReceiveAtZone;
import com.v2retail.dotvik.dc.ptlnew.withpallate.MenuPTLNewPickingWithPallateFragment;
import com.v2retail.util.AlertBox;

public class MenuPTLNewPickingFullCrate30 extends Fragment implements View.OnClickListener {

    FragmentManager fm;
    Context con;
    String TAG = MenuPTLNewPickingFullCrate30.class.getName();
    private OnFragmentInteractionListener mListener;

    Button ptl_picking, flr_bin, floor_stagging, receive_at_floor, putway_sorting_zone_wise, receive_at_hub, hu_zone_crate_mapping, receive_at_zone, hu_zone_store_mapping;

    public MenuPTLNewPickingFullCrate30() {
        // Required empty public constructor
    }

    public static MenuPTLNewPickingFullCrate30 newInstance() {
        return new MenuPTLNewPickingFullCrate30();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.menu_ptl_new_picking_full_crate30, container, false);
        con = getContext();

        ptl_picking = view.findViewById(R.id.ptl_new_full_crate_ptl_picking);
        flr_bin = view.findViewById(R.id.ptl_new_full_crate_flr_bin);
        floor_stagging = view.findViewById(R.id.ptl_new_full_crate_move_flr_staging);
        receive_at_floor = view.findViewById(R.id.ptl_new_full_crate_receive_at_floor);
        putway_sorting_zone_wise = view.findViewById(R.id.ptl_new_full_crate_putway_sorting_zone_wise);
        receive_at_hub = view.findViewById(R.id.ptl_new_full_crate_receive_at_hub_station);
        hu_zone_crate_mapping = view.findViewById(R.id.ptl_new_full_crate_hu_stn_crate_zone_mapping);
        hu_zone_store_mapping = view.findViewById(R.id.ptl_new_full_crate_hu_zone_store_mapping);
        receive_at_zone = view.findViewById(R.id.ptl_new_full_crate_receive_at_zone);


        ptl_picking.setOnClickListener(this);
        flr_bin.setOnClickListener(this);
        floor_stagging.setOnClickListener(this);
        receive_at_floor.setOnClickListener(this);
        receive_at_floor.setOnClickListener(this);
        putway_sorting_zone_wise.setOnClickListener(this);
        receive_at_hub.setOnClickListener(this);
        hu_zone_crate_mapping.setOnClickListener(this);
        hu_zone_store_mapping.setOnClickListener(this);
        receive_at_zone.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Picking Full Crate Process");
    }

    @Override
    public void onClick(View view) {
        AlertBox box = new AlertBox(con);
        Fragment fragment = null;
        switch (view.getId()) {

            case R.id.ptl_new_full_crate_ptl_picking:
                fragment = new FragmentPTLNewFullCratePicking();
                break;
            case R.id.ptl_new_full_crate_flr_bin:
                fragment = FragmentPTLNewFullCrateTagWithFloorBIN.newInstance();
                break;
            case R.id.ptl_new_full_crate_move_flr_staging:
                fragment = FragmentPTLNewFullCrateFloorStaging.newInstance("Full Crate Floor Staging");
                break;
            case R.id.ptl_new_full_crate_receive_at_floor:
                fragment = FragmentPTLNewFullCrateReceiveAtFloor.newInstance("Receive At Floor");
                break;
            case R.id.ptl_new_full_crate_receive_at_hub_station:
                fragment = FragmentPTLNewFullCrateReceiveAtHubStation.newInstance();
                break;
            case R.id.ptl_new_full_crate_putway_sorting_zone_wise:
                fragment = FragmentPTLNewFullCrateZoneWiseSorting.newInstance();
                break;
            case R.id.ptl_new_full_crate_hu_stn_crate_zone_mapping:
                fragment = FragmentPTLNewFullCrateHubStnCrateZoneMapping.newInstance();
                break;
            case R.id.ptl_new_full_crate_hu_zone_store_mapping:
                fragment = FragmentPTLNewHUZoneStoreMapping.newInstance("full-crate");
                break;
            case R.id.ptl_new_full_crate_receive_at_zone:
                fragment = FragmentPTLNewReceiveAtZone.newInstance();
                break;
        }
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "ptlnew_full_crate");
            ft.addToBackStack("ptlnew_full_crate");
            ft.commit();
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStack();
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

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}