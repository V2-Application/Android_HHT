package com.v2retail.dotvik.dc;

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
import com.v2retail.util.AlertBox;

public class MenuGateEntryFragment extends Fragment implements View.OnClickListener {

    private DC_DashBoard.OnFragmentInteractionListener mListener;
    private FragmentManager fm;
    private Context con;

    public MenuGateEntryFragment() {
    }

    public static MenuGateEntryFragment newInstance() {
        return new MenuGateEntryFragment();
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
        View view = inflater.inflate(R.layout.fragment_menu_gate_entry, container, false);
        con = getContext();

        view.findViewById(R.id.btn_gate_entry_lot_putaway).setOnClickListener(this);
        view.findViewById(R.id.btn_inbound_process_new).setOnClickListener(this);
        view.findViewById(R.id.btn_return_gate_entry_process).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.btn_gate_entry_lot_putaway:
                fragment = MenuGateEntryLotPutawayFragment.newInstance();
                break;
            case R.id.btn_inbound_process_new:
                fragment = FragmentMenuInboundNew.newInstance();
                break;
            case R.id.btn_return_gate_entry_process:
                new AlertBox(con).getBox("Alert", "Implementation In Process");
                return;
        }

        if (fragment != null && fm != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment);
            ft.addToBackStack("gate_entry_menu");
            ft.commit();
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
