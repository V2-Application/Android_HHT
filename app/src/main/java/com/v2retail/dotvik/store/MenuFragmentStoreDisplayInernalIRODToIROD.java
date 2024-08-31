package com.v2retail.dotvik.store;

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

import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;

/**
 * @author Narayanan
 * @version 11.72
 * {@code Author: Narayanan, Revision: 1, Created: 23rd Aug 2024, Modified: 23rd Aug 2024}
 */
public class MenuFragmentStoreDisplayInernalIRODToIROD extends Fragment implements View.OnClickListener{

    Context con;
    FragmentManager fm;
    AlertBox box;
    private OnFragmentInteractionListener mListener;
    Button picking, putway, empty;

    public MenuFragmentStoreDisplayInernalIRODToIROD() {
        // Required empty public constructor
    }

    public static MenuFragmentStoreDisplayInernalIRODToIROD newInstance() {
        MenuFragmentStoreDisplayInernalIRODToIROD fragment = new MenuFragmentStoreDisplayInernalIRODToIROD();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_store_display_inernal_irod_to_irod, container, false);

        con=getContext();
        box=new AlertBox(con);
        fm=getActivity().getSupportFragmentManager();

        picking = view.findViewById(R.id.store_display_internal_irod_to_irod_picking);
        putway = view.findViewById(R.id.store_display_internal_irod_to_irod_putway);
        empty = view.findViewById(R.id.store_display_internal_irod_to_irod_empty);

        picking.setOnClickListener(this);
        putway.setOnClickListener(this);
        empty.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        switch (fragmentID) {
            case R.id.store_display_internal_irod_to_irod_picking:
                fragment = FragmentStoreDisplayInternalIRODToIRODPicking.newInstance(Vars.BREADCRUMB_IROD_TO_IROD);
                break;
            case R.id.store_display_internal_irod_to_irod_putway:
                fragment = FragmentStoreDisplayInternalIRODToIRODPutway.newInstance(Vars.BREADCRUMB_IROD_TO_IROD);
                break;
            case R.id.store_display_internal_irod_to_irod_empty:
                fragment = FragmentStoreDisplayInternalDeTagIROD.newInstance(Vars.BREADCRUMB_IROD_TO_IROD);
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft =fm.beginTransaction();
            ft.replace(R.id.home, fragment, "StoreDisplayInternalIrodToIrod");
            ft.addToBackStack("StoreDisplayInternalIrodToIrod");
            ft.commit();
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        if (fm.getBackStackEntryCount() == 1){

            box.getDialogBox(getActivity());

        }
        else {
            fm.popBackStack();
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

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Display > Irod To Irod");
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}