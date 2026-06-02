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
import com.v2retail.dotvik.store.directpicking.MenuDirectPickingV01To0001;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OutboundFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OutboundFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OutboundFragment extends Fragment implements View.OnClickListener,
        MenuDirectPickingV01To0001.OnFragmentInteractionListener

{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    Button direct_picking;
    Button picking_against_picking;
    Button bin_to_bin_transfer;
    Button sloc_to_sloc_withouth_wm;
    Button procees_to_display;

    Button grt_from_display;
    Button grt_from_msa;
    Button baseStock;
    Button picking_with_consolidation;
    Button carticle_process, direct_picking_v01_0001;
    Button store_grt_process;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    FragmentManager fm;
    private OnFragmentInteractionListener mListener;

    public OutboundFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OutWardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OutboundFragment newInstance(String param1, String param2) {
        OutboundFragment fragment = new OutboundFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Outbound");
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
        View view = inflater.inflate(R.layout.fragment_outbound, container, false);
        direct_picking = (Button) view.findViewById(R.id.direct_picking);
        picking_against_picking = (Button) view.findViewById(R.id.Picking_Against_Picklist);
        bin_to_bin_transfer = (Button) view.findViewById(R.id.Bin_To_Bin_Transfer);
        sloc_to_sloc_withouth_wm = (Button) view.findViewById(R.id.Sloc_To_SLoc_Without_WM);
        procees_to_display = (Button) view.findViewById(R.id.processTodisplay);
        baseStock = (Button) view.findViewById(R.id.base_stock);
        grt_from_display = (Button) view.findViewById(R.id.grt_from_Display);
        grt_from_msa = (Button) view.findViewById(R.id.grt_from_msa);
        picking_with_consolidation = (Button) view.findViewById(R.id.button_picking_with_consolidation);
        carticle_process = view.findViewById(R.id.carticle_process);
        direct_picking_v01_0001 = view.findViewById(R.id.direct_picking_v01_0001);
        store_grt_process = view.findViewById(R.id.store_grt_process);

        direct_picking.setOnClickListener(this);
        picking_against_picking.setOnClickListener(this);
        bin_to_bin_transfer.setOnClickListener(this);
        sloc_to_sloc_withouth_wm.setOnClickListener(this);
        procees_to_display.setOnClickListener(this);
        grt_from_display.setOnClickListener(this);
        grt_from_msa.setOnClickListener(this);
        baseStock.setOnClickListener(this);
        picking_with_consolidation.setOnClickListener(this);
        carticle_process.setOnClickListener(this);
        direct_picking_v01_0001.setOnClickListener(this);
        store_grt_process.setOnClickListener(this);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }

        //    fm.popBackStack();
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
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        Bundle bundle = new Bundle();
        switch (view.getId()) {

            case R.id.direct_picking:
                fragment = new Direct_Picking_Fragment();
                break;
            case R.id.Picking_Against_Picklist:
                fragment = new Picking_Against_Picklist_Fragment();
                break;
            case R.id.Bin_To_Bin_Transfer:
                fragment = new Bin_To_Bin_Transfer_Fragment();
                break;
            case R.id.Sloc_To_SLoc_Without_WM:
                fragment = new Sloc_To_Sloc_without_WM_Fragment();
                break;
            case R.id.processTodisplay:
                fragment = new TRFDispToProc();
                bundle.putString("TYPE", "3");
                fragment.setArguments(bundle);
                break;
            case R.id.grt_from_msa:
                fragment = new GRT_From_MSAFragment();
                break;
            case R.id.grt_from_Display:
                fragment = new GRT_From_DisplayFragment();
                break;
            case R.id.base_stock:
                fragment = new DisplayAreaProcessFragment();
                break;
            case R.id.button_picking_with_consolidation:
                fragment = new PickingWithConsolidationFragment();
                break;
            case R.id.carticle_process:
                fragment = new FragmentCArticleProcess();
                break;
            case R.id.direct_picking_v01_0001:
                fragment = new MenuDirectPickingV01To0001();
                break;
            case R.id.store_grt_process:
                fragment = new FragmentStoreGrtProcess();
                break;
        }
        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "outbound");
            ft.addToBackStack("outbound");
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
