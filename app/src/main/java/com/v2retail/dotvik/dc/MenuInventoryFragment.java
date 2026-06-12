package com.v2retail.dotvik.dc;

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
import com.v2retail.dotvik.dc.bincreateidentifier.MenuBinCrateIdentifier;

public class MenuInventoryFragment extends Fragment implements View.OnClickListener {

    private DC_DashBoard.OnFragmentInteractionListener mListener;

    Button btn_v11_to_msa;
    Button btn_stock_take;
    Button btn_bin_crate_identifier;
    Button btn_msa_live_stock_take;
    Button btn_scan_hu_article;
    Button btn_empty_bin;
    Button btn_shade_stock_movement;
    FragmentManager fm;

    public MenuInventoryFragment() {
    }

    public static MenuInventoryFragment newInstance() {
        return new MenuInventoryFragment();
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
                .setActionBarTitle("Inventory");
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

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStack();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inventory_menu, container, false);

        btn_v11_to_msa = rootView.findViewById(R.id.inventory_menu_v11_to_msa);
        btn_stock_take = rootView.findViewById(R.id.inventory_menu_stock_take);
        btn_bin_crate_identifier = rootView.findViewById(R.id.inventory_menu_bin_crate_identifier);
        btn_msa_live_stock_take = rootView.findViewById(R.id.inventory_menu_msa_live_stock_take);
        btn_scan_hu_article = rootView.findViewById(R.id.inventory_menu_scan_hu_article);
        btn_empty_bin = rootView.findViewById(R.id.inventory_menu_empty_bin);
        btn_shade_stock_movement = rootView.findViewById(R.id.inventory_menu_shade_stock_movement);

        btn_v11_to_msa.setOnClickListener(this);
        btn_stock_take.setOnClickListener(this);
        btn_bin_crate_identifier.setOnClickListener(this);
        btn_msa_live_stock_take.setOnClickListener(this);
        btn_scan_hu_article.setOnClickListener(this);
        btn_empty_bin.setOnClickListener(this);
        btn_shade_stock_movement.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.inventory_menu_v11_to_msa:
                fragment = new V11ToMsaFragment();
                break;
            case R.id.inventory_menu_stock_take:
                fragment = new Stock_Take_Process_Fragment();
                break;
            case R.id.inventory_menu_bin_crate_identifier:
                fragment = new MenuBinCrateIdentifier();
                break;
            case R.id.inventory_menu_msa_live_stock_take:
                fragment = new FragmentMSALiveStockTake();
                break;
            case R.id.inventory_menu_scan_hu_article:
                fragment = FragmentScanHuArticle.newInstance();
                break;
            case R.id.inventory_menu_empty_bin:
                fragment = new EmptyBinFragment();
                break;
            case R.id.inventory_menu_shade_stock_movement:
                fragment = new ShadeStockMovementFragment();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "inventory_menu");
            ft.addToBackStack("inventory_menu");
            ft.commit();
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
