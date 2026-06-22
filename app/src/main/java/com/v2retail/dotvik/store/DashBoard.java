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
import com.v2retail.dotvik.dc.HU_RFID_Scan_Fragment;
import com.v2retail.util.AlertBox;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DashBoard.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DashBoard#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashBoard extends Fragment implements
        View.OnClickListener,
        OutboundFragment.OnFragmentInteractionListener,
        InboundFragment.OnFragmentInteractionListener,
        StockTakeFragment.OnFragmentInteractionListener,
        Retail_App_Fragment.OnFragmentInteractionListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Button inbound;
    Button outbound;
    Button stock_take;
    Button stock_take2;
    Button retailApp;
    Button Rf_idScanner;
    Button save_picklist;
    Button paperless_picking;
    Button ecomm;
    Button article_consumption;

    Context con;
    FragmentManager fm;
    AlertBox box;
    private OnFragmentInteractionListener mListener;

    public DashBoard() {
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
    public static DashBoard newInstance(String param1, String param2) {
        DashBoard fragment = new DashBoard();
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
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        con=getContext();
        box=new AlertBox(con);
        fm=getFragmentManager();
        inbound = (Button) view.findViewById(R.id.inbound);
        outbound = (Button) view.findViewById(R.id.outbound);
        stock_take = (Button) view.findViewById(R.id.stock_take);
        stock_take2 = (Button) view.findViewById(R.id.stock_take2);
        retailApp = (Button) view.findViewById(R.id.retailApp);
        ecomm = view.findViewById(R.id.store_ecomm);

        Rf_idScanner  =  (Button) view.findViewById(R.id.Rf_idScanner);
        save_picklist = view.findViewById(R.id.picklist_save);
        article_consumption = view.findViewById(R.id.article_consumption);

        inbound.setOnClickListener(this);
        outbound.setOnClickListener(this);
        stock_take.setOnClickListener(this);
        stock_take2.setOnClickListener(this);
        retailApp.setOnClickListener(this);
        ecomm.setOnClickListener(this);

        Rf_idScanner.setOnClickListener(this);
        save_picklist.setOnClickListener(this);
        article_consumption.setOnClickListener(this);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        if (fm.getBackStackEntryCount() == 1){

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
        ((Home_Activity) getActivity())
                .setActionBarTitle("DashBoard");
    }

    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        switch (fragmentID) {

            case R.id.inbound:
                fragment = new InboundFragment();
                break;
            case R.id.retailApp:
                fragment = new RetailFragment();  //new
                break;
            case R.id.outbound:
                fragment = new OutboundFragment();
                break;

            case R.id.stock_take:
                fragment = new StockTakeFragment();
                break;
            case R.id.stock_take2:
                fragment = new StockTakeV2_Fragment();
                break;
            case R.id.store_ecomm:
                fragment = new EcommFragmentProcess();  //new
                break;
            case R.id.Rf_idScanner:
                fragment = new HU_RFID_Scan_Fragment();  //new
                break;
            case R.id.paperless_picking:
                fragment = new PaperLessDate();  //new
                break;
            case R.id.article_consumption:
                fragment = new FragmentArticleConsumption();  //new
                break;
        }

        if (fragment != null) {
            FragmentTransaction ft =fm.beginTransaction();
            ft.replace(R.id.home, fragment, "Dashboard");
            ft.addToBackStack("Dashboard");
            ft.commit();
        }
    }
}
