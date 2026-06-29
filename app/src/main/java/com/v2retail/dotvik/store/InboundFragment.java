package com.v2retail.dotvik.store;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;


/**
 * {@code Author: Narayanan, Modified: 30th Jul 2024, Release: 11.71}
 */
public class InboundFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    Button hru_grc;
    Button grc_putway;
    Button floor_to_processg,fullHuPutway,huWiseGateEntry,storeBinConsPutway,putway_0002;
    Button proceesg_to_msa;
    Button article_scan;
    Button storeBinConsolidation;
    FragmentManager fm;
    AlertBox box;
    Context con;
    String TAG = InboundFragment.class.getName();
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public InboundFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InboundFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InboundFragment newInstance(String param1, String param2) {
        InboundFragment fragment = new InboundFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        fm = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        con=getContext();
        box=new AlertBox(con);
        View view = inflater.inflate(R.layout.fragment_inbound, container, false);
        hru_grc = (Button) view.findViewById(R.id.hru_grc);
        grc_putway = (Button) view.findViewById(R.id.grc_putway);
        floor_to_processg = (Button) view.findViewById(R.id.floortoprocessG);
        proceesg_to_msa = (Button) view.findViewById(R.id.processgtomsa);
        fullHuPutway = (Button) view.findViewById(R.id.fullHuPutway);
        huWiseGateEntry = (Button) view.findViewById(R.id.huWiseGateEntry);
        putway_0002 = (Button) view.findViewById(R.id.msa_inbound_putway_0002);
        storeBinConsolidation = (Button) view.findViewById(R.id.storeBinConsolidation);
        storeBinConsPutway = (Button) view.findViewById(R.id.storeBinConsPutway);

        try {
            hru_grc.setOnClickListener(this);
            grc_putway.setOnClickListener(this);
            floor_to_processg.setOnClickListener(this);
            proceesg_to_msa.setOnClickListener(this);
            fullHuPutway.setOnClickListener(this);
            huWiseGateEntry.setOnClickListener(this);
            storeBinConsolidation.setOnClickListener(this);
            storeBinConsPutway.setOnClickListener(this);
            putway_0002.setOnClickListener(this);

        } catch (Exception e) {
            box.getErrBox(e);
        }

        Log.d(TAG, TAG + " created");
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Inbound");
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        fm.popBackStack();
        Fragment fragment=new DashBoard();
        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment );
            ft.commit();
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

    public void onClick(View view) {
        Fragment fragment = null;
        switch (view.getId()) {

            case R.id.fullHuPutway:
                fragment = new FullHuPutwayFragment();
                break;
            case R.id.huWiseGateEntry:
                fragment = HuWiseGateEntryFragment.newInstance();
                break;
            case R.id.msa_inbound_putway_0002:
                fragment = FragmentMsaInboundPutway0002.newInstance("MSA > Inbound >");
                break;
            case R.id.hru_grc:
                fragment = new HU_GRC_Process_Fragment();
                break;

            case R.id.grc_putway:
                fragment = new GRC_Putway_Fragment();
                break;

            case R.id.floortoprocessG:
                fragment = new TRFDispToProc();
                Bundle bundle = new Bundle();
                bundle.putString("TYPE", "4");
                fragment.setArguments(bundle);
                break;

            case R.id.processgtomsa:
                fragment = new Floor_Putway_Fragment();
                break;

            case R.id.storeBinConsPutway:
                fragment = new HuBinConsPutwayFragment();
                break;

            case R.id.storeBinConsolidation:
                fragment = new StoreBinConsolidationFragment();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "inbound");
            ft.addToBackStack("inbound");
            ft.commit();
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}