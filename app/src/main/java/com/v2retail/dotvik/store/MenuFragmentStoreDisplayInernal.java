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

import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;

/**
 * @author Narayanan
 * @version 11.72
 * {@code Author: Narayanan, Revision: 1, Created: 16th Aug 2024, Modified: 16th Aug 2024}
 */
public class MenuFragmentStoreDisplayInernal extends Fragment implements View.OnClickListener {

    Context con;
    FragmentManager fm;
    AlertBox box;
    private OnFragmentInteractionListener mListener;
    Button art_irod_identifier, tagging_irod, detagging_irod, irod_nature;

    public MenuFragmentStoreDisplayInernal() {
        // Required empty public constructor
    }

    public static MenuFragmentStoreDisplayInernal newInstance(String param1, String param2) {
        MenuFragmentStoreDisplayInernal fragment = new MenuFragmentStoreDisplayInernal();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_fragment_store_display_inernal, container, false);

        con=getContext();
        box=new AlertBox(con);
        fm=getActivity().getSupportFragmentManager();

        art_irod_identifier = view.findViewById(R.id.store_display_internal_art_irod);
        tagging_irod = view.findViewById(R.id.store_display_internal_tagging_irod);
        detagging_irod = view.findViewById(R.id.store_display_internal_detagging_irod);
        irod_nature = view.findViewById(R.id.store_display_internal_irod_nature);

        art_irod_identifier.setOnClickListener(this);
        tagging_irod.setOnClickListener(this);
        detagging_irod.setOnClickListener(this);
        irod_nature.setOnClickListener(this);

        return view;
    }
    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        switch (fragmentID) {

            case R.id.store_display_internal_art_irod:
                fragment = FragmentStoreDisplayInternalArtIrod.newInstance("Display > Internal");
                break;
            case R.id.store_display_internal_tagging_irod:
                fragment = FragmentStoreDisplayInternalTaggingIROD.newInstance("Display > Internal");
                break;
            case R.id.store_display_internal_detagging_irod:
                fragment = FragmentStoreDisplayInternalDeTagIROD.newInstance("Display > Internal");
                break;
            case R.id.store_display_internal_irod_nature:
                fragment = FragmentStoreDisplayInternalIRODNature.newInstance("Display > Internal");
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft =fm.beginTransaction();
            ft.replace(R.id.home, fragment, "StoreDisplayInternal");
            ft.addToBackStack("StoreDisplayInternal");
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
                .setActionBarTitle("Display > Internal");
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}