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

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.bincreateidentifier.MenuBinCrateIdentifier;
import com.v2retail.dotvik.dc.binwisepicking.MenuMSABinwisePickingFragment;
import com.v2retail.dotvik.dc.grt.GRTCratePickingProcess;
import com.v2retail.dotvik.dc.grt.GRTProcessMenu;
import com.v2retail.dotvik.dc.ptl.PTLProcessFragment;
import com.v2retail.dotvik.dc.ptlnew.MenuPTLNewFragment;
import com.v2retail.dotvik.dc.ptlnew.fullcrate30.MenuPTLNewPickingFullCrate30;
import com.v2retail.dotvik.dc.ptlnew.ptl40.MenuPTLNewPickingProcess40;
import com.v2retail.dotvik.dc.ptlnew.withoutpallate.MenuPTLNewPickingWithoutPallateFragment;
import com.v2retail.dotvik.dc.ptlnew.withpallate.MenuPTLNewPickingWithPallateFragment;
import com.v2retail.util.AlertBox;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DC_DashBoard.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DC_DashBoard#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DC_DashBoard extends Fragment implements View.OnClickListener,
        InwardFragment.OnFragmentInteractionListener,
        OutWardFragment.OnFragmentInteractionListener,
        Stock_Take_Process_Fragment.OnFragmentInteractionListener,
        GRTCratePickingProcess.OnFragmentInteractionListener,
        PTLProcessFragment.OnFragmentInteractionListener,
        GRTProcessMenu.OnFragmentInteractionListener,
        MenuFragmentInwardTVSPaperLess.OnFragmentInteractionListener,
        MenuPTLNewFragment.OnFragmentInteractionListener,
        MenuPTLNewPickingWithPallateFragment.OnFragmentInteractionListener,
        MenuPTLNewPickingWithoutPallateFragment.OnFragmentInteractionListener,
        MenuPTLNewPickingFullCrate30.OnFragmentInteractionListener,
        MenuPTLNewPickingProcess40.OnFragmentInteractionListener,
        MenuMSABinwisePickingFragment.OnFragmentInteractionListener,
        MenuBinCrateIdentifier.OnFragmentInteractionListener
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Context con;
    FragmentManager fm;
    AlertBox box;

    Button inbound;
    Button outbound;
    Button stock_take;
    Button grt_process;
    Button ptl_process;
    Button cla_process;
    Button v11_to_msa;
    Button ptl_new;
    Button msa_binwise_picking;
    Button bin_crate_identifier;
    Button msa_live_stock_take;

    private OnFragmentInteractionListener mListener;

    public DC_DashBoard() {
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
    public static DC_DashBoard newInstance(String param1, String param2) {
        DC_DashBoard fragment = new DC_DashBoard();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_process_selection, container, false);
        fm=getFragmentManager();
        con = getContext();
        box=new AlertBox(con);
        inbound = view.findViewById(R.id.inward);
        outbound = view.findViewById(R.id.outward);
        stock_take = view.findViewById(R.id.stock_take);
        grt_process = view.findViewById(R.id.grt_process);
        ptl_process = view.findViewById(R.id.ptl_process);
        cla_process = view.findViewById(R.id.cla_process);
        v11_to_msa = view.findViewById(R.id.v11_to_msa);
        ptl_new = view.findViewById(R.id.ptl_new);
        msa_binwise_picking = view.findViewById(R.id.msa_binwise_picking);
        bin_crate_identifier = view.findViewById(R.id.bin_crate_identifier);
        msa_live_stock_take = view.findViewById(R.id.msa_live_stock_take);

        inbound.setOnClickListener(this);
        outbound.setOnClickListener(this);
        stock_take.setOnClickListener(this);
        grt_process.setOnClickListener(this);
        ptl_process.setOnClickListener(this);
        cla_process.setOnClickListener(this);
        v11_to_msa.setOnClickListener(this);
        ptl_new.setOnClickListener(this);
        msa_binwise_picking.setOnClickListener(this);
        bin_crate_identifier.setOnClickListener(this);
        msa_live_stock_take.setOnClickListener(this);

        return view;
    }
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }



        if (getFragmentManager().getBackStackEntryCount() == 1){

            box.getDialogBox(getActivity());

        }
        else {
           fm.popBackStack();
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


    @Override
    public void onResume() {
        super.onResume();
        ((Process_Selection_Activity) getActivity())
                .setActionBarTitle("DashBoard");
    }

    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

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


    public void setFragment(int fragmentID) {

        Fragment fragment = null;
        switch (fragmentID) {

            case R.id.inward:
                fragment = new InwardFragment();
                break;
            case R.id.outward:
                fragment = new OutWardFragment();
                break;
            case R.id.stock_take:
                 fragment=new Stock_Take_Process_Fragment();
                break;
            case R.id.grt_process:
                fragment=new GRTProcessMenu();
                break;
            case R.id.ptl_process:
                fragment = new PTLProcessFragment();
                break;
            case R.id.cla_process:
                fragment = new CLAProcessFragment();
                break;
            case R.id.v11_to_msa:
                fragment = new V11ToMsaFragment();
                break;
            case R.id.ptl_new:
                fragment = new MenuPTLNewFragment();
                break;
            case R.id.msa_binwise_picking:
                fragment = new MenuMSABinwisePickingFragment();
                break;
            case R.id.bin_crate_identifier:
                fragment = new MenuBinCrateIdentifier();
                break;
            case R.id.msa_live_stock_take:
                fragment = new FragmentMSALiveStockTake();
                break;
        }
        clearStack();
        if (fragment != null) {
            FragmentTransaction ft =fm.beginTransaction();
            ft.replace(R.id.home, fragment);
            ft.addToBackStack("dashboard");
            ft.commit();
        }
    }

    public void clearStack() {
        if(fm!=null)
        {
            int count=fm.getBackStackEntryCount();
            if (count > 1) {
                fm.popBackStackImmediate();
            }
        }

    }
}
