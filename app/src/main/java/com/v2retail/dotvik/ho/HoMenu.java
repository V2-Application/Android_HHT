package com.v2retail.dotvik.ho;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.FragmentV11ToMsaPutway;
import com.v2retail.util.AlertBox;

public class HoMenu extends Fragment implements View.OnClickListener {

    Context con;
    FragmentManager fm;
    AlertBox box;

    Button v11ToMsaPutway, pickingMsaToSmpl;
    private OnFragmentInteractionListener mListener;

    public HoMenu() {

    }

    public static HoMenu newInstance() {
        return new HoMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ho_menu, container, false);
        fm = getParentFragmentManager();
        con = getContext();
        box = new AlertBox(con);
        v11ToMsaPutway = view.findViewById(R.id.ho_v11_to_msa_putway);
        v11ToMsaPutway.setOnClickListener(this);
        pickingMsaToSmpl = view.findViewById(R.id.ho_picking_msa_to_smpl);
        pickingMsaToSmpl.setOnClickListener(this);
        return view;
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
        ((HoProcessSelectionActivity) getActivity()).setActionBarTitle("HO Process");
    }

    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        if (fragmentID == R.id.ho_v11_to_msa_putway) {
            fragment = FragmentV11ToMsaPutway.newInstance();
        } else if (fragmentID == R.id.ho_picking_msa_to_smpl) {
            fragment = FragmentPickingFromMsaToSmpl.newInstance();
        }
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment);
            ft.addToBackStack("ho_menu");
            ft.commit();
        }
    }
}
