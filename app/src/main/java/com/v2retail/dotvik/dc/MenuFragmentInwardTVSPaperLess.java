package com.v2retail.dotvik.dc;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.Home_Activity;
import com.v2retail.dotvik.store.PaperLessDate;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Narayanan
 * @version 11.73
 * {@code Author: Narayanan, Revision: 1, Created: 28th Aug 2024, Modified: 28th Aug 2024}
 */
public class MenuFragmentInwardTVSPaperLess extends Fragment implements View.OnClickListener {

    Button btn_picking, btn_picking_confirmation, btn_reprint, btn_testprint, btn_picking_live_hu, btn_reprint_live_hu;
    FragmentManager fm;
    Context con;
    private OnFragmentInteractionListener mListener;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = null;
    List<BluetoothDevice> bluetoothDevices = null;
    SharedPreferencesData data;
    LinearLayout ll_tvs_paperless, ll_tvs_paperless_live_hu;
    String mode;

    public MenuFragmentInwardTVSPaperLess() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .getSupportActionBar().setTitle("TVS Paperless Picking");
    }

    public static MenuFragmentInwardTVSPaperLess newInstance(String mode) {
        MenuFragmentInwardTVSPaperLess fragment = new MenuFragmentInwardTVSPaperLess();
        fragment.mode = mode;
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
        data = new SharedPreferencesData(con);

        btn_picking = view.findViewById(R.id.btn_tvs_paperless_picking);
        btn_picking_confirmation = view.findViewById(R.id.btn_tvs_paperless_picking_confirm);
        btn_reprint = view.findViewById(R.id.btn_tvs_paperless_re_print);
        btn_testprint = view.findViewById(R.id.btn_tvs_paperless_test_print);

        btn_picking_live_hu = view.findViewById(R.id.btn_tvs_paperless_picking_live_hu);
        btn_reprint_live_hu = view.findViewById(R.id.btn_tvs_paperless_re_print_live_hu);

        ll_tvs_paperless = view.findViewById(R.id.ll_tvs_paperless);
        ll_tvs_paperless_live_hu = view.findViewById(R.id.ll_tvs_paperless_live_hu);

        if(mode.equalsIgnoreCase(Vars.TVS_PAPER_LESS)){
            ll_tvs_paperless.setVisibility(View.VISIBLE);
            ll_tvs_paperless_live_hu.setVisibility(View.GONE);
        }else{
            ll_tvs_paperless.setVisibility(View.GONE);
            ll_tvs_paperless_live_hu.setVisibility(View.VISIBLE);
        }

        btn_picking.setOnClickListener(this);
        btn_picking_confirmation.setOnClickListener(this);
        btn_picking_live_hu.setOnClickListener(this);
        btn_reprint.setOnClickListener(this);
        btn_testprint.setOnClickListener(this);

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
            case R.id.btn_tvs_paperless_picking_live_hu:
                fragment = PaperLessDate.newInstance(this.mode);
                break;
           case R.id.btn_tvs_paperless_picking_confirm:

                break;
            case R.id.btn_tvs_paperless_re_print:
                fragment = FragmentInwardTVSPaperLessRePrint.newInstance("TVS Paperless");
                break;
            case R.id.btn_tvs_paperless_test_print:
                //discover();
                testPrint();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "inward");
            ft.addToBackStack("inward");
            ft.commit();
        }
    }

    private void testPrint(){
        //4B-2033PA-BFA4 4B-2033PA-9B5A
        TSPLPrinter printerHelper = new TSPLPrinter(con);
        String defaultPrinter = data.read(Vars.TVS_PRINTER);
        if(!printerHelper.findSavedOrKnownLabelPrinter(defaultPrinter)){
            AlertBox box = new AlertBox(con);
            box.getBox("Printer Not Found", "No paired TVS or PP310 bluetooth printer found. Please pair");
            return;
        }
        defaultPrinter = printerHelper.getPrinterName();
        data.write(Vars.TVS_PRINTER, defaultPrinter);
        printerHelper.sendPrintCommandToBluetoothPrinter(defaultPrinter, null,"2");
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void discover(){
        if (ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermission(con);
        }
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    bluetoothDevices.add(device);
                }
            }
        };
        pairedDevices = bluetoothAdapter.getBondedDevices();
        bluetoothDevices = new ArrayList<>(pairedDevices);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();
        for (BluetoothDevice device: bluetoothDevices
             ) {
            Log.d("DEVICE NAME", device.getName());
            int majorDeviceClass = device.getBluetoothClass().getMajorDeviceClass();
            if (device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING || device.getBluetoothClass().getDeviceClass() == 1664) {
                Log.d("DEVICE TYPE","IMAGING "+ device.getBluetoothClass().getDeviceClass());
            }
        }
    }

    public void requestBluetoothPermission(Context con) {
        if (ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(
                    (Activity) con,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_ADVERTISE,Manifest.permission.BLUETOOTH_ADMIN},
                    1
            );
        }
    }
}