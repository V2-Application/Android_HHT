package com.v2retail.dotvik.dc.ptlnew.ptl40;

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

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.fullcrate30.FragmentPTLNewFullCrateFloorStaging;
import com.v2retail.dotvik.dc.ptlnew.fullcrate30.FragmentPTLNewFullCrateTagWithFloorBIN;
import com.v2retail.dotvik.dc.ptlnew.withoutpallate.FragmentPTLNewWithoutPallateCrateFloorStaging;
import com.v2retail.dotvik.dc.ptlnew.withoutpallate.FragmentPTLNewWithoutPallatePicking;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewArticlePutwayStorewise;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewFloorModule;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewHUCloseAndPrint;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewHUZoneStoreMapping;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewReceiveAtZone;
import com.v2retail.util.AlertBox;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MenuPTLNewPickingProcess40#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MenuPTLNewPickingProcess40 extends Fragment implements View.OnClickListener {

    FragmentManager fm;
    Context con;
    String TAG = MenuPTLNewPickingProcess40.class.getName();
    private OnFragmentInteractionListener mListener;

    Button ptl_picking, flr_bin, flr_stagging, receive_at_zone, proposed_hub_station, hu_zone_store_mapping, hu_close, hu_print, article_putway_storewise, hu_packing_weighing_area, packed_hu_pnd_trf_floor_dcla, floor_dcla, ground_floor_dcla, cla_area, pnd_lorry_loading_staging_area, lorry_loading_processing_area;

    public MenuPTLNewPickingProcess40() {
    }

    public static MenuPTLNewPickingProcess40 newInstance() {
        return new MenuPTLNewPickingProcess40();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_ptl_new_picking_process40, container, false);
        con = getContext();

        ptl_picking = view.findViewById(R.id.ptl_new_picking_process_4_0_ptl_picking);
        flr_bin = view.findViewById(R.id.ptl_new_picking_process_4_0_flr_bin);
        flr_stagging = view.findViewById(R.id.ptl_new_picking_process_4_0_crate_tag_plt_flr_stagging);
        receive_at_zone = view.findViewById(R.id.ptl_new_picking_process_4_0_receive_at_zone);
        proposed_hub_station = view.findViewById(R.id.ptl_new_picking_process_4_0_proposed_hub_station);
        hu_zone_store_mapping = view.findViewById(R.id.ptl_new_picking_process_4_0_hu_zone_store_mapping);
        hu_close = view.findViewById(R.id.ptl_new_picking_process_4_0_hu_close);
        hu_print = view.findViewById(R.id.ptl_new_picking_process_4_0_hu_print);
        article_putway_storewise = view.findViewById(R.id.ptl_new_picking_process_4_0_article_putway_storewise);
        hu_packing_weighing_area = view.findViewById(R.id.ptl_new_picking_process_4_0_hu_packing_weighing_area);
        packed_hu_pnd_trf_floor_dcla = view.findViewById(R.id.ptl_new_picking_process_4_0_packed_hu_pnd_trf_floor_dcla);
        floor_dcla = view.findViewById(R.id.ptl_new_picking_process_4_0_floor_dcla);
        ground_floor_dcla = view.findViewById(R.id.ptl_new_picking_process_4_0_ground_floor_dcla);
        cla_area = view.findViewById(R.id.ptl_new_picking_process_4_0_cla_area);
        pnd_lorry_loading_staging_area = view.findViewById(R.id.ptl_new_picking_process_4_0_pnd_lorry_loading_staging_area);
        lorry_loading_processing_area = view.findViewById(R.id.ptl_new_picking_process_4_0_lorry_loading_processing_area);

        ptl_picking.setOnClickListener(this);
        flr_bin.setOnClickListener(this);
        flr_stagging.setOnClickListener(this);
        receive_at_zone.setOnClickListener(this);
        proposed_hub_station.setOnClickListener(this);
        hu_zone_store_mapping.setOnClickListener(this);
        hu_close.setOnClickListener(this);
        hu_print.setOnClickListener(this);
        article_putway_storewise.setOnClickListener(this);
        hu_packing_weighing_area.setOnClickListener(this);
        packed_hu_pnd_trf_floor_dcla.setOnClickListener(this);
        floor_dcla.setOnClickListener(this);
        ground_floor_dcla.setOnClickListener(this);
        cla_area.setOnClickListener(this);
        pnd_lorry_loading_staging_area.setOnClickListener(this);
        lorry_loading_processing_area.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("PTL - 4.0 Process");
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


    @Override
    public void onClick(View view) {
        AlertBox box = new AlertBox(con);
        Fragment fragment = null;
        switch (view.getId()) {

            case R.id.ptl_new_picking_process_4_0_ptl_picking:
                fragment = FragmentPTLNewProcess40Picking.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_flr_bin:
                fragment = FragmentPTLNewFullCrateTagWithFloorBIN.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_crate_tag_plt_flr_stagging:
                fragment = FragmentPTLNewFullCrateFloorStaging.newInstance("PTL - Crate Floor Staging");
                break;
            case R.id.ptl_new_picking_process_4_0_receive_at_zone:
                fragment = FragmentPTLNewReceiveAtZone.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_proposed_hub_station:
                fragment = FragmentPTLNewProcess40ProposedHubStation.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_hu_zone_store_mapping:
                fragment = FragmentPTLNewProcess40ZoneStoreHUMapping.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_hu_close:
                fragment = FragmentPTLNewHUCloseAndPrint.newInstance("HU Close");
                break;
            case R.id.ptl_new_picking_process_4_0_hu_print:
                fragment = FragmentPTLNewHUCloseAndPrint.newInstance("HU Print");
                break;
            case R.id.ptl_new_picking_process_4_0_article_putway_storewise:
                // Redesigned 2026-05-09 — modern card UI with FLR-Station based flow.
                // (See FragmentPTLNewArticlePutwayStorewise + spec doc "PTL Article
                //  Putway Store Wise". The legacy FragmentPTLNewWithoutPallatePutwayStorewise
                //  is no longer reachable from this menu.)
                fragment = FragmentPTLNewArticlePutwayStorewise.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_hu_packing_weighing_area:
                fragment = FragmentPTLHUPackingWeighingArea.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_packed_hu_pnd_trf_floor_dcla:
                fragment = FragmentPTLPackedHuPndTrfFloorDcla.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_floor_dcla:
                fragment = FragmentPTLFloorDcla.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_ground_floor_dcla:
                fragment = FragmentPTLGroundFloorDcla.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_cla_area:
                fragment = FragmentPTLClaArea.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_pnd_lorry_loading_staging_area:
                fragment = FragmentPTLPndLorryLoadingStagingArea.newInstance();
                break;
            case R.id.ptl_new_picking_process_4_0_lorry_loading_processing_area:
                fragment = FragmentPTLLorryLoadingProcessingArea.newInstance();
                break;
        }
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "ptlnew_process_4");
            ft.addToBackStack("ptlnew_process_4");
            ft.commit();
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}