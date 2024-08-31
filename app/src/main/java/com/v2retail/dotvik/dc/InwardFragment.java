package com.v2retail.dotvik.dc;

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

import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.PaperLessDate;
import com.v2retail.util.AlertBox;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InwardFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InwardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InwardFragment extends Fragment implements View.OnClickListener,
        MenuFragmentInwardTVSPaperLess.OnFragmentInteractionListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    Button create_scan;
    Button grc_putway;
    Button putway;
    Button bin_consolidation,ecom_putway,stored_stock,grt_scanning03,grt_scanning01, tvs_paperless_picking;
    FragmentManager fm;
    Context con;

    String TAG = InwardFragment.class.getName();
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public InwardFragment() {
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
    public static InwardFragment newInstance(String param1, String param2) {
        InwardFragment fragment = new InwardFragment();
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
        View view = inflater.inflate(R.layout.fragment_inward, container, false);
        con = getContext();
        create_scan = (Button) view.findViewById(R.id.create_scan);
        grc_putway = (Button) view.findViewById(R.id.grc_putway);
        bin_consolidation = (Button) view.findViewById(R.id.bin_consolidation);
        ecom_putway = (Button) view.findViewById(R.id.ecom_putway);
        putway = (Button) view.findViewById(R.id.putway);
        stored_stock = (Button) view.findViewById(R.id.stored_stock);
        grt_scanning03 = (Button) view.findViewById(R.id.grt_putway03);
        grt_scanning01 = (Button) view.findViewById(R.id.grt_scanning01);
        tvs_paperless_picking= view.findViewById(R.id.tvs_paperless_picking);

        create_scan.setOnClickListener(this);
        grc_putway.setOnClickListener(this);
        putway.setOnClickListener(this);
        ecom_putway.setOnClickListener(this);
        stored_stock.setOnClickListener(this);
        grt_scanning03.setOnClickListener(this);
        grt_scanning01.setOnClickListener(this);
        tvs_paperless_picking.setOnClickListener(this);
       // bin_consolidation.setOnClickListener(this);

        Log.d(TAG, TAG + "Inward created");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Inward");
    }

    // TODO: Rename method, update argument and hook method into UI event
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
        AlertBox box = new AlertBox(con);
        Fragment fragment = null;
        switch (view.getId()) {

            case R.id.create_scan:
                fragment = new Stock_In_Out_Fragment();
                break;

            case R.id.grc_putway:
//                box.getBox("Alert", "Implementation In Process");
                fragment = new GRCPutwayFragment();
                break;

            case R.id.putway:
                fragment = new TO_Creation_Fragment();
                break;

            case R.id.ecom_putway:
                fragment  = new EcomPutwayFragment();
                break;

            case R.id.stored_stock:
                fragment = new StoredStockFragment();
                break;

            case R.id.grt_scanning01:
                fragment = new GRTScanning01Fragment();
                break;

            case R.id.grt_putway03:
                fragment = new GRTPutway03Fragment();
                break;
            case R.id.tvs_paperless_picking:
                fragment = MenuFragmentInwardTVSPaperLess.newInstance();
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "inward");
            ft.addToBackStack("inward");
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

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
