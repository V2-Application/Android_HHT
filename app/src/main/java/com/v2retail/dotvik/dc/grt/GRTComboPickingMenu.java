package com.v2retail.dotvik.dc.grt;

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
import com.v2retail.dotvik.dc.reverseputway.GRTReversePutway;
import com.v2retail.util.AlertBox;

public class GRTComboPickingMenu extends Fragment implements View.OnClickListener {

    private static final String TAG = GRTComboPickingMenu.class.getName();
    private GRTComboPickingMenu.OnFragmentInteractionListener mListener;

    Button btn_hu_zone_store,btn_crate_picking,btn_de_tag_hu,btn_sm_crate_scanning,btn_crate_msa,grt_reverse_putway,palette_putway,palette_receive,btn_hu_pick;
    FragmentManager fm;
    Context con;
    View rootView;

    public GRTComboPickingMenu() {

    }
    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("GRT Combo Picking");
    }
    public static GRTComboPickingMenu newInstance(String param1, String param2) {
        GRTComboPickingMenu fragment = new GRTComboPickingMenu();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getChildFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_grt_combo_picking_menu, container, false);
        con = getContext();

        btn_hu_zone_store = rootView.findViewById(R.id.btn_combo_hu_zone_store);
        btn_crate_picking = rootView.findViewById(R.id.grt_combo_pick_crate_picking);
        btn_de_tag_hu = rootView.findViewById(R.id.btn_grt_combo_pick_de_tag_hu);
        btn_sm_crate_scanning = rootView.findViewById(R.id.btn_grt_combo_crate_scanning);
        btn_crate_msa = rootView.findViewById(R.id.grt_combo_crate_to_msa_bin);
        palette_putway = rootView.findViewById(R.id.grt_combo_pick_palette_putway);
        palette_receive = rootView.findViewById(R.id.grt_combo_pick_palette_receive);
        grt_reverse_putway = rootView.findViewById(R.id.grt_combo_grt_reverse_putway);
        btn_hu_pick = rootView.findViewById(R.id.grt_combo_hu_pick);

        btn_hu_zone_store.setOnClickListener(this);
        btn_crate_picking.setOnClickListener(this);
        btn_de_tag_hu.setOnClickListener(this);
        btn_sm_crate_scanning.setOnClickListener(this);
        btn_crate_msa.setOnClickListener(this);
        grt_reverse_putway.setOnClickListener(this);
        palette_putway.setOnClickListener(this);
        palette_receive.setOnClickListener(this);
        btn_hu_pick.setOnClickListener(this);

        return rootView;
    }
    @Override
    public void onClick(View view) {
        AlertBox box = new AlertBox(con);
        Fragment fragment = null;
        Bundle args = new Bundle();
        switch (view.getId()) {

            case R.id.btn_combo_hu_zone_store:
                fragment=new GRTTagHUStoreZone();
                break;
            case R.id.grt_combo_pick_crate_picking:
                fragment=new GRTCratePickingProcess();
                break;
            case R.id.grt_combo_pick_palette_putway:
                fragment=new GRTComboPalettePutway();
                break;
            case R.id.grt_combo_pick_palette_receive:
                fragment=new GRTComboPaletteReceive();
                break;
            case R.id.btn_grt_combo_crate_scanning:
                fragment=new GRTComboCrateScanning();
                break;
            case R.id.btn_grt_combo_pick_de_tag_hu:
                fragment=new GRTSingleDeTagHuProcess();
                break;
            case R.id.grt_combo_crate_to_msa_bin:
                fragment=new GRTCrateToMSABin();
                break;
            case R.id.grt_combo_grt_reverse_putway:
                fragment = GRTReversePutway.newInstance(true);
                break;
            case R.id.grt_combo_hu_pick:
                fragment=new GRTSingleComboHUPick();
                args.putString("singleOrCombo","combo");
                fragment.setArguments(args);
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "grt_combo_picking_process_menu");
            ft.addToBackStack("grt_combo_picking_process_menu");
            ft.commit();
        }
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}