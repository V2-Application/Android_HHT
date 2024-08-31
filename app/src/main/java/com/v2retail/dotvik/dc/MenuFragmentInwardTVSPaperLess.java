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

import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.dotvik.store.PaperLessDate;
import com.v2retail.util.AlertBox;

/**
 * @author Narayanan
 * @version 11.73
 * {@code Author: Narayanan, Revision: 1, Created: 28th Aug 2024, Modified: 28th Aug 2024}
 */
public class MenuFragmentInwardTVSPaperLess extends Fragment implements View.OnClickListener {

    Button btn_picking, btn_picking_confirmation, btn_reprint;
    FragmentManager fm;
    Context con;
    private OnFragmentInteractionListener mListener;

    public MenuFragmentInwardTVSPaperLess() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .getSupportActionBar().setTitle("TVS Paperless Picking");
    }

    public static MenuFragmentInwardTVSPaperLess newInstance() {
        MenuFragmentInwardTVSPaperLess fragment = new MenuFragmentInwardTVSPaperLess();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getActivity().getSupportFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_inward_tvs_paper_less, container, false);

        con = getContext();
        btn_picking = view.findViewById(R.id.btn_tvs_paperless_picking);
        btn_picking_confirmation = view.findViewById(R.id.btn_tvs_paperless_picking_confirm);
        btn_reprint = view.findViewById(R.id.btn_tvs_paperless_re_print);

        btn_picking.setOnClickListener(this);
        btn_picking_confirmation.setOnClickListener(this);
        btn_reprint.setOnClickListener(this);

        return view;
    }

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


    @Override
    public void onClick(View view) {

        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.btn_tvs_paperless_picking:
                fragment = PaperLessDate.newInstance(Vars.TVS_PAPER_LESS);
                break;

            case R.id.btn_tvs_paperless_picking_confirm:

                break;

            case R.id.btn_tvs_paperless_re_print:

                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "inward");
            ft.addToBackStack("inward");
            ft.commit();
        }
    }
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}