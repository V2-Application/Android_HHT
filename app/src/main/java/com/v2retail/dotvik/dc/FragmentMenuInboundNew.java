package com.v2retail.dotvik.dc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.v2retail.dotvik.R;

/**
 * Inbound Process New — sub-menu
 * Tiles: HU Scan & Putway | HU Picking From BIN | PUT01 HU Wise Scanning
 */
public class FragmentMenuInboundNew extends Fragment implements View.OnClickListener {

    public FragmentMenuInboundNew() {}
    public static FragmentMenuInboundNew newInstance() { return new FragmentMenuInboundNew(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_inbound_new, container, false);
        view.findViewById(R.id.btn_hu_scan_putway).setOnClickListener(this);
        view.findViewById(R.id.btn_hu_picking_bin).setOnClickListener(this);
        view.findViewById(R.id.btn_put01_hu_scan).setOnClickListener(this);
        view.findViewById(R.id.btn_back_inbound_new).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Fragment f = null;
        if      (id == R.id.btn_hu_scan_putway)  f = FragmentHuScanPutway.newInstance();
        else if (id == R.id.btn_hu_picking_bin)  f = FragmentHuPickingFromBin.newInstance();
        else if (id == R.id.btn_put01_hu_scan)   f = FragmentPut01HuWiseScanning.newInstance();
        else if (id == R.id.btn_back_inbound_new){ if(getFragmentManager()!=null) getFragmentManager().popBackStack(); return; }

        if (f != null && getFragmentManager() != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, f);
            ft.addToBackStack(null);
            ft.commit();
        }
    }
}
