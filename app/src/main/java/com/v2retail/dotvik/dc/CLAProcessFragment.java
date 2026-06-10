package com.v2retail.dotvik.dc;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.v2retail.dotvik.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CLAProcessFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CLAProcessFragment extends Fragment implements View.OnClickListener {

    Button hu_scan_in_pallete,pallete_putway,pallete_picking,hu_putway_in_msa,hu_picking_from_msa,vehicle_loading;
    FragmentManager fm;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CLAProcessFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CLP_Process_Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CLAProcessFragment newInstance(String param1, String param2) {
        CLAProcessFragment fragment = new CLAProcessFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_c_l_p__process_, container, false);
        hu_scan_in_pallete = view.findViewById(R.id.hu_scan_in_pallete);
        pallete_picking = view.findViewById(R.id.pallete_picking);
        pallete_putway = view.findViewById(R.id.pallete_putway);
        hu_putway_in_msa = view.findViewById(R.id.hu_putway_in_msa);
        hu_picking_from_msa = view.findViewById(R.id.hu_picking_from_msa);
        vehicle_loading = view.findViewById(R.id.vehicle_loading);

        fm=getFragmentManager();
        hu_scan_in_pallete.setOnClickListener(this);
        pallete_picking.setOnClickListener(this);
        pallete_putway.setOnClickListener(this);
        hu_putway_in_msa.setOnClickListener(this);
        hu_picking_from_msa.setOnClickListener(this);
        vehicle_loading.setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("CLA Process");
    }

    @Override
    public void onClick(View v) {

        Fragment fragment = null;

        switch (v.getId()){

            case R.id.hu_scan_in_pallete:

                fragment = new ScanHuInPalleteFragment();
                break;

            case R.id.pallete_putway:
                fragment = new PalletePutwayFragment();
                break;

            case R.id.pallete_picking:
                fragment = new PalletePickingFragment();
                break;
            case R.id.hu_putway_in_msa:
                fragment = new FragmentHUPutwayInMSA();
                break;
            case R.id.hu_picking_from_msa:
                fragment = new FragmentHuPickingFromMSA();
                break;
            case R.id.vehicle_loading:
                fragment = FragmentVehicleLoading.newInstance();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft =fm.beginTransaction();
            ft.replace(R.id.home, fragment);
            ft.addToBackStack("dashboard");
            ft.commit();
        }

    }
}