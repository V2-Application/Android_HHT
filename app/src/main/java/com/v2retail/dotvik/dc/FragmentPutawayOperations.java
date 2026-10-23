package com.v2retail.dotvik.dc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.v2retail.dotvik.R;

/**
 * Submenu under Inward. Opens Putway, Ecom Putway, GRC Putway,
 * and GRC Done-Putway Pending.
 */
public class FragmentPutawayOperations extends Fragment implements View.OnClickListener {

    public FragmentPutawayOperations() {
    }

    public static FragmentPutawayOperations newInstance() {
        return new FragmentPutawayOperations();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_putaway_operations, container, false);
        Button putway = view.findViewById(R.id.putway);
        Button ecomPutway = view.findViewById(R.id.ecom_putway);
        Button grcPutway = view.findViewById(R.id.grc_putway);
        Button grcDonePutwayPending = view.findViewById(R.id.grc_done_putway_pending);
        putway.setOnClickListener(this);
        ecomPutway.setOnClickListener(this);
        grcPutway.setOnClickListener(this);
        grcDonePutwayPending.setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar bar =
                    ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (bar != null) {
                bar.setTitle("Putaway Operations");
            }
        }
    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {
            case R.id.putway:
                fragment = new TO_Creation_Fragment();
                break;
            case R.id.ecom_putway:
                fragment = new EcomPutwayFragment();
                break;
            case R.id.grc_putway:
                fragment = new GRCPutwayFragment();
                break;
            case R.id.grc_done_putway_pending:
                fragment = FragmentGrcDonePutwayPending.newInstance();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "inward");
            ft.addToBackStack("inward");
            ft.commit();
        }
    }
}
