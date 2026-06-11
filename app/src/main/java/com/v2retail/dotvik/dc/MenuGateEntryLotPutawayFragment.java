package com.v2retail.dotvik.dc;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.v2retail.dotvik.R;

public class MenuGateEntryLotPutawayFragment extends Fragment implements View.OnClickListener {

    private DC_DashBoard.OnFragmentInteractionListener mListener;
    private FragmentManager fm;
    private Context con;

    public MenuGateEntryLotPutawayFragment() {
    }

    public static MenuGateEntryLotPutawayFragment newInstance() {
        return new MenuGateEntryLotPutawayFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Gate Entry");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DC_DashBoard.OnFragmentInteractionListener) {
            mListener = (DC_DashBoard.OnFragmentInteractionListener) context;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_gate_entry_lot_putaway, container, false);
        con = getContext();

        view.findViewById(R.id.btn_box_putaway_to_pallet).setOnClickListener(this);
        view.findViewById(R.id.btn_pallet_putaway_to_bin).setOnClickListener(this);
        view.findViewById(R.id.btn_pallet_picking_from_bin).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.btn_box_putaway_to_pallet:
                fragment = FragmentGateEntryLotPutway.newInstance();
                break;
            case R.id.btn_pallet_putaway_to_bin:
                fragment = FragmentPalletPutwayToBin.newInstance();
                break;
            case R.id.btn_pallet_picking_from_bin:
                fragment = FragmentLotPickingFromBin.newInstance();
                break;
        }

        if (fragment != null && fm != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment);
            ft.addToBackStack("gate_entry_lot_putaway_menu");
            ft.commit();
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
