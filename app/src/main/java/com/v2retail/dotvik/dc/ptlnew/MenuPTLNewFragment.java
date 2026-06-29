package com.v2retail.dotvik.dc.ptlnew;

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
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.ptlnew.grt.MenuPTLNewGrtProcess;
import com.v2retail.dotvik.dc.ptlnew.fullcrate30.MenuPTLNewPickingFullCrate30;
import com.v2retail.dotvik.dc.ptlnew.ptl40.MenuPTLNewPickingProcess40;
import com.v2retail.dotvik.dc.ptlnew.withoutpallate.MenuPTLNewPickingWithoutPallateFragment;
import com.v2retail.dotvik.dc.ptlnew.withpallate.MenuPTLNewPickingWithPallateFragment;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

public class MenuPTLNewFragment extends Fragment implements
        View.OnClickListener,
        MenuPTLNewPickingWithPallateFragment.OnFragmentInteractionListener,
        MenuPTLNewPickingFullCrate30.OnFragmentInteractionListener,
        MenuPTLNewPickingProcess40.OnFragmentInteractionListener,
        MenuPTLNewGrtProcess.OnFragmentInteractionListener {

    FragmentManager fm;
    Context con;
    String TAG = MenuPTLNewFragment.class.getName();
    private OnFragmentInteractionListener mListener;

    Button ptl_picking_with_pallete, ptl_picking_without_pallete, ptl_picking_full_crate_30, ptl_new_picking_4_0, ptl_new_grt_process, ptl_new_test_print;
    SharedPreferencesData data;

    private void disableAndGreyOut(Button button) {
        if (button == null) return;
        button.setEnabled(false);
        button.setClickable(false);
        button.setAlpha(0.45f);
        button.setBackgroundColor(0xFF7A7A7A); // grey
        button.setTextColor(0xFFEEEEEE);
    }

    public MenuPTLNewFragment() {
        // Required empty public constructor
    }

    public static MenuPTLNewFragment newInstance() {
        return new MenuPTLNewFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.menu_ptl_new_fragment, container, false);

        con = getContext();
        data = new SharedPreferencesData(con);

        ptl_picking_with_pallete = view.findViewById(R.id.ptl_new_picking_with_pallate);
        ptl_picking_without_pallete = view.findViewById(R.id.ptl_new_picking_without_pallate);
        ptl_picking_full_crate_30 = view.findViewById(R.id.ptl_new_picking_full_crate_3_0);
        ptl_new_picking_4_0 = view.findViewById(R.id.ptl_new_picking_4_0);
        ptl_new_grt_process = view.findViewById(R.id.ptl_new_grt_process);
        ptl_new_test_print = view.findViewById(R.id.ptl_new_test_print);

        ptl_picking_with_pallete.setOnClickListener(this);
        ptl_picking_without_pallete.setOnClickListener(this);
        ptl_picking_full_crate_30.setOnClickListener(this);
        ptl_new_picking_4_0.setOnClickListener(this);
        ptl_new_grt_process.setOnClickListener(this);
        ptl_new_test_print.setOnClickListener(this);

        // Disable and grey-out these three processes
        disableAndGreyOut(ptl_picking_with_pallete);
        disableAndGreyOut(ptl_picking_without_pallete);
        disableAndGreyOut(ptl_picking_full_crate_30);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("PTL New Process");
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
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onClick(View view) {
        AlertBox box = new AlertBox(con);
        Fragment fragment = null;
        switch (view.getId()) {

            case R.id.ptl_new_picking_with_pallate:
                fragment = MenuPTLNewPickingWithPallateFragment.newInstance();
                break;
            case R.id.ptl_new_picking_without_pallate:
                fragment = MenuPTLNewPickingWithoutPallateFragment.newInstance();
                break;
            case R.id.ptl_new_picking_full_crate_3_0:
                fragment = MenuPTLNewPickingFullCrate30.newInstance();
                break;
            case R.id.ptl_new_picking_4_0:
                fragment = MenuPTLNewPickingProcess40.newInstance();
                break;
            case R.id.ptl_new_grt_process:
                fragment = MenuPTLNewGrtProcess.newInstance();
                break;
            case R.id.ptl_new_test_print:
                testPrint();
                break;
        }
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "ptlnew");
            ft.addToBackStack("ptlnew");
            ft.commit();
        }
    }

    private void testPrint() {
        TSPLPrinter printerHelper = new TSPLPrinter(con);
        String defaultPrinter = data.read(Vars.TVS_PRINTER);
        boolean printerFound = false;
        if (defaultPrinter != null && defaultPrinter.length() > 0) {
            printerFound = printerHelper.findBluetoothPrinter(defaultPrinter, false);
        }
        if (!printerFound) {
            printerFound = printerHelper.findBluetoothPrinter("4B-2033", true);
            if (printerFound) {
                defaultPrinter = printerHelper.getPrinterName();
                data.write(Vars.TVS_PRINTER, defaultPrinter);
            }
        }
        if (!printerFound) {
            AlertBox box = new AlertBox(con);
            box.getBox("Printer Not Found", "No paired TVS bluetooth printer found. Please pair");
            return;
        }
        printerHelper.sendPrintCommandToBluetoothPrinter(defaultPrinter, null, "2");
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}