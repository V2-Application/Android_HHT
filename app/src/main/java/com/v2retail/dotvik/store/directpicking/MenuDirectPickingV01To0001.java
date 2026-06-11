package com.v2retail.dotvik.store.directpicking;

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
import android.widget.Toast;

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;
import com.v2retail.dotvik.dc.binwisepicking.MenuMSABinwisePickingFragment;
import com.v2retail.dotvik.dc.ptlnew.withpallate.FragmentPTLNewHUCloseAndPrint;
import com.v2retail.dotvik.store.Home_Activity;

public class MenuDirectPickingV01To0001 extends Fragment implements
        View.OnClickListener {

    FragmentManager fm;
    Context con;
    String TAG = MenuDirectPickingV01To0001.class.getName();

    Button huwise_article_scan_v01_v09, article_putway_0001, article_transfer_v01_0001;

    private OnFragmentInteractionListener mListener;

    public MenuDirectPickingV01To0001() {
        // Required empty public constructor
    }

    public static MenuDirectPickingV01To0001 newInstance() {
        return new MenuDirectPickingV01To0001();
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
        View view = inflater.inflate(R.layout.menu_direct_picking_v01_to0001_fragment, container, false);

        con = getContext();

        huwise_article_scan_v01_v09 = view.findViewById(R.id.direct_picking_v01_0001_huwise_article_v01_v09);
        article_putway_0001 = view.findViewById(R.id.direct_picking_v01_0001_huwise_article_putway_0001);
        article_transfer_v01_0001 = view.findViewById(R.id.direct_picking_v01_0001_article_transfer_v01_0001);

        huwise_article_scan_v01_v09.setOnClickListener(this);
        article_putway_0001.setOnClickListener(this);
        article_transfer_v01_0001.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity())
                .setActionBarTitle("Direct Picking(V01 To 0001)");
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

            case R.id.direct_picking_v01_0001_huwise_article_v01_v09:
                fragment = FragmentHUWiseArticleScanningV01V09.newInstance();
                break;
            case R.id.direct_picking_v01_0001_huwise_article_putway_0001:
                fragment = FragmentDirectPickingV01ArticlePutway0001.newInstance();
                break;
            case R.id.direct_picking_v01_0001_article_transfer_v01_0001:
                fragment = FragmentDirectPickingV01ArticleTransfer0001.newInstance();
                break;
        }
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment, "direct_picking_01_0001");
            ft.addToBackStack("direct_picking_01_0001");
            ft.commit();
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}