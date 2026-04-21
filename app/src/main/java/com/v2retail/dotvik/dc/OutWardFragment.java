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

import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.store.PaperLessDate;
import com.v2retail.util.AlertBox;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OutWardFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OutWardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OutWardFragment extends Fragment implements View.OnClickListener,
        MenuFragmentInwardTVSPaperLess.OnFragmentInteractionListener

{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    Button picking;
    Button hu_scan;
    Button paperless_picking;
    Button dc_grt;
    Button hu_cla;
    Context con;
    AlertBox box;
    Button sample_stock_movement,shade_stock_movement,empty_bin,grt_hu_move,hu_weight,tvs_paperless_picking,tvs_paperless_picking_live_hu;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    FragmentManager fm;
    private OnFragmentInteractionListener mListener;

    public OutWardFragment() {
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
    public static OutWardFragment newInstance(String param1, String param2) {
        OutWardFragment fragment = new OutWardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("Outward");
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

        View view = inflater.inflate(R.layout.fragment_outward, container, false);
        con = getContext();
        box = new AlertBox(con);
        picking = (Button) view.findViewById(R.id.picking);
        hu_scan = (Button) view.findViewById(R.id.Hu_Scan);
        paperless_picking = (Button) view.findViewById(R.id.paperless_picking);

        dc_grt = (Button) view.findViewById(R.id.dc_grt);
        hu_cla = (Button) view.findViewById(R.id.hu_move_cla);
        shade_stock_movement = (Button) view.findViewById(R.id.shade_stock_movement);
        sample_stock_movement = (Button) view.findViewById(R.id.stock_movement);
        empty_bin = (Button) view.findViewById(R.id.empty_bin);
        grt_hu_move = (Button) view.findViewById(R.id.grt_hu_move);
        hu_weight = view.findViewById(R.id.outward_hu_weight);
        tvs_paperless_picking= view.findViewById(R.id.tvs_paperless_picking);
        tvs_paperless_picking_live_hu= view.findViewById(R.id.tvs_paperless_picking_live_hu);


        picking.setOnClickListener(this);
        hu_scan.setOnClickListener(this);
        paperless_picking.setOnClickListener(this);
        shade_stock_movement.setOnClickListener(this);
        empty_bin.setOnClickListener(this);
        grt_hu_move.setOnClickListener(this);
        hu_weight.setOnClickListener(this);
        tvs_paperless_picking.setOnClickListener(this);
        tvs_paperless_picking_live_hu.setOnClickListener(this);

        return view;
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
        Fragment fragment = null;

        switch (view.getId()) {

            case R.id.picking:
                fragment = new Picking_Delivery_Scan_Fragment();
                break;
            case R.id.Hu_Scan:
                fragment = new HU_Detail_Fragment();
                break;
            case R.id.paperless_picking:
                fragment = PaperLessDate.newInstance(Vars.PAPER_LESS);
                break;

            case R.id.grt_hu_move:
                fragment = new GrtHuMoveFragment();
                break;

            case R.id.empty_bin:
                fragment = new EmptyBinFragment();
                break;
            case R.id.dc_grt:

                box.getBox("Alert", "Implementation In Process");
                // fragment=new Bin_To_Bin_Transfer_Fragment();
                break;
            case R.id.hu_move_cla:

                box.getBox("Alert", "Implementation In Process");
                // fragment=new Sloc_To_Sloc_without_WM_Fragment();
                break;
            case R.id.stock_movement:

                box.getBox("Alert", "Implementation In Process");
                //  fragment=new Sloc_To_Sloc_without_WM_Fragment();
                break;

            case R.id.shade_stock_movement:
                fragment = new ShadeStockMovementFragment();
                break;

            case R.id.outward_hu_weight:
                fragment = new OutwardHUWeightFragment();
                break;
            case R.id.tvs_paperless_picking:
                fragment = MenuFragmentInwardTVSPaperLess.newInstance(Vars.TVS_PAPER_LESS);
                break;
            case R.id.tvs_paperless_picking_live_hu:
                fragment = MenuFragmentInwardTVSPaperLess.newInstance(Vars.TVS_PAPER_LESS_LHU);
                break;

        }
        if (fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "outward");
            ft.addToBackStack("outward");
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
