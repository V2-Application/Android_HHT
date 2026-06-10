package com.v2retail.dotvik.dc.ptlnew.grt;

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
import com.v2retail.dotvik.dc.grt.GRTCratePickingProcess;
import com.v2retail.dotvik.dc.grt.GRTComboPalettePutway;
import com.v2retail.dotvik.dc.grt.GRTComboPaletteReceive;

public class MenuPTLNewGrtProcess extends Fragment implements View.OnClickListener {

    FragmentManager fm;
    Context con;
    private OnFragmentInteractionListener mListener;

    Button btnCratePicking, btnPalettePutaway, btnPaletteReceive, btnFloorHubWiseCrateTag,
            btnReceivePalletAtHubSorting, btnHubSortingScanCrate, btnHubHuCrateClosed,
            btnPaletteTagWithCrate;

    public MenuPTLNewGrtProcess() {
    }

    public static MenuPTLNewGrtProcess newInstance() {
        return new MenuPTLNewGrtProcess();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_ptl_new_grt_process, container, false);
        con = getContext();

        btnCratePicking = view.findViewById(R.id.ptl_grt_crate_picking);
        btnPalettePutaway = view.findViewById(R.id.ptl_grt_palette_putaway);
        btnPaletteReceive = view.findViewById(R.id.ptl_grt_palette_receive);
        btnFloorHubWiseCrateTag = view.findViewById(R.id.ptl_grt_floor_hub_wise_crate_tag);
        btnReceivePalletAtHubSorting = view.findViewById(R.id.ptl_grt_receive_pallet_at_hub_sorting);
        btnHubSortingScanCrate = view.findViewById(R.id.ptl_grt_hub_sorting_scan_crate);
        btnHubHuCrateClosed = view.findViewById(R.id.ptl_grt_hub_hu_crate_closed);
        btnPaletteTagWithCrate = view.findViewById(R.id.ptl_grt_palette_tag_with_crate);

        btnCratePicking.setOnClickListener(this);
        btnPalettePutaway.setOnClickListener(this);
        btnPaletteReceive.setOnClickListener(this);
        btnFloorHubWiseCrateTag.setOnClickListener(this);
        btnReceivePalletAtHubSorting.setOnClickListener(this);
        btnHubSortingScanCrate.setOnClickListener(this);
        btnHubHuCrateClosed.setOnClickListener(this);
        btnPaletteTagWithCrate.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("PTL-GRT Process");
    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.ptl_grt_crate_picking:
                fragment = new GRTCratePickingProcess();
                break;
            case R.id.ptl_grt_palette_putaway:
                fragment = new GRTComboPalettePutway();
                break;
            case R.id.ptl_grt_palette_receive:
                fragment = new GRTComboPaletteReceive();
                break;
            case R.id.ptl_grt_floor_hub_wise_crate_tag:
                fragment = FragmentPTLGrtFloorHubWiseCrateTag.newInstance();
                break;
            case R.id.ptl_grt_receive_pallet_at_hub_sorting:
                fragment = FragmentPTLGrtReceivePalletAtHubSorting.newInstance();
                break;
            case R.id.ptl_grt_hub_sorting_scan_crate:
                fragment = FragmentPTLGrtHubSortingScanCrate.newInstance();
                break;
            case R.id.ptl_grt_hub_hu_crate_closed:
                fragment = FragmentPTLGrtHubHuCrateClosed.newInstance();
                break;
            case R.id.ptl_grt_palette_tag_with_crate:
                fragment = FragmentPTLGrtPaletteTagWithCrate.newInstance();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "ptlnew_grt_process");
            ft.addToBackStack("ptlnew_grt_process");
            ft.commit();
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
