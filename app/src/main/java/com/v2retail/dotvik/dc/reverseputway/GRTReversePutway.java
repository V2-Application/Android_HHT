package com.v2retail.dotvik.dc.reverseputway;

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
import com.v2retail.dotvik.dc.grt.GRTComboCrateScanning;
import com.v2retail.dotvik.dc.grt.GRTComboPalettePutway;
import com.v2retail.dotvik.dc.grt.GRTComboPaletteReceive;
import com.v2retail.dotvik.dc.grt.GRTComboPickingMenu;
import com.v2retail.dotvik.dc.grt.GRTCratePickingProcess;
import com.v2retail.dotvik.dc.grt.GRTCrateToMSABin;
import com.v2retail.dotvik.dc.grt.GRTSingleComboHUPick;
import com.v2retail.dotvik.dc.grt.GRTSingleDeTagHuProcess;
import com.v2retail.dotvik.dc.grt.GRTTagHUStoreZone;
import com.v2retail.dotvik.dc.putwayinbin.GRTHUPutwayInBin;
import com.v2retail.util.AlertBox;

public class GRTReversePutway extends Fragment implements View.OnClickListener {

    private static final String TAG = GRTReversePutway.class.getName();
    public static final String ARG_HIDE_CRATE_TO_MSA = "hide_crate_to_msa";
    private OnFragmentInteractionListener mListener;

    Button btn_crate_tagging,btn_pallate_receive,btn_crate_to_msa;
    FragmentManager fm;
    Context con;
    View rootView;

    public GRTReversePutway() {

    }
    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRT Reverse Putway");
    }
    public static GRTReversePutway newInstance(String param1, String param2) {
        GRTReversePutway fragment = new GRTReversePutway();
        return fragment;
    }

    public static GRTReversePutway newInstance(boolean hideCrateToMsa) {
        GRTReversePutway fragment = new GRTReversePutway();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HIDE_CRATE_TO_MSA, hideCrateToMsa);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_reverse_putway, container, false);
        con = getContext();

        btn_crate_tagging = rootView.findViewById(R.id.grt_reverse_putway_crate_with_pallet);
        btn_pallate_receive = rootView.findViewById(R.id.grt_reverse_putway_pallet_at_floor);
        btn_crate_to_msa = rootView.findViewById(R.id.grt_reverse_putway_crate_to_msa);

        btn_crate_tagging.setOnClickListener(this);
        btn_pallate_receive.setOnClickListener(this);
        btn_crate_to_msa.setOnClickListener(this);

        Bundle args = getArguments();
        if (args != null && args.getBoolean(ARG_HIDE_CRATE_TO_MSA, false)) {
            btn_crate_to_msa.setVisibility(View.GONE);
        }

        return rootView;
    }
    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.grt_reverse_putway_crate_with_pallet:
                fragment = FragmentCrateTagWithPallet.newInstance();
                break;
            case R.id.grt_reverse_putway_pallet_at_floor:
                fragment = FragmentPalletReceiveAtFloor.newInstance();
                break;
            case R.id.grt_reverse_putway_crate_to_msa:
                fragment = new GRTCrateToMSABin();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "combo_picking_reverse_putway");
            ft.addToBackStack("combo_picking_reverse_putway");
            ft.commit();
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
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