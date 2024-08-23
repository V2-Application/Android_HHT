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
import com.v2retail.dotvik.dc.HU_RFID_Scan_Fragment;
import com.v2retail.util.AlertBox;

/**
 * @author Narayanan
 * @version 11.71
 * {@code Author: Narayanan, Revision: 1, Created: 30th Jul 2024, Modified: 30th Jul 2024}
 */
public class MenuFragmentStore extends Fragment implements
        View.OnClickListener,
        DashBoard.OnFragmentInteractionListener,
        MenuFragmentStoreDisplay.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayInbound.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayOutbound.OnFragmentInteractionListener,
        OutboundFragment.OnFragmentInteractionListener,
        InboundFragment.OnFragmentInteractionListener,
        StockTakeFragment.OnFragmentInteractionListener,
        Retail_App_Fragment.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayInernal.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayInernalIRODToIROD.OnFragmentInteractionListener {

    Context con;
    FragmentManager fm;
    AlertBox box;

    private OnFragmentInteractionListener mListener;

    Button display_0001,msa_0002;
    public MenuFragmentStore() {
        // Required empty public constructor
    }

    public static MenuFragmentStore newInstance(String param1, String param2) {
        MenuFragmentStore fragment = new MenuFragmentStore();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.menu_fragment_store, container, false);
        con=getContext();
        box=new AlertBox(con);
        fm=getActivity().getSupportFragmentManager();

        display_0001 = view.findViewById(R.id.display_0001);
        msa_0002 = view.findViewById(R.id.msa_0002);

        display_0001.setOnClickListener(this);
        msa_0002.setOnClickListener(this);

        return view;
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
                .setActionBarTitle("Store Dashboard");
    }

    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        switch (fragmentID) {

            case R.id.msa_0002:
                fragment = new DashBoard();
                break;
            case R.id.display_0001:
                fragment = new MenuFragmentStoreDisplay();
                break;
        }
        clearStack();

        if (fragment != null) {
            FragmentTransaction ft =fm.beginTransaction();
            ft.replace(R.id.home, fragment, "Home");
            ft.addToBackStack("Home");
            ft.commit();
        }
    }

    public void clearStack() {
        if(fm!=null)
        {
            int count=fm.getBackStackEntryCount();
            if (count > 1) {
                fm.popBackStackImmediate();
            }

        }

    }
}