package com.v2retail.dotvik.dc.ptlnew.ptl40;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.Process_Selection_Activity;

/**
 * PTL 4.0 — PND Lorry Loading Staging Area (UI screen only).
 */
public class FragmentPTLPndLorryLoadingStagingArea extends Fragment implements View.OnClickListener {

    private static final String ACTION_BAR_TITLE = "PND Lorry Loading Staging Area";

    private FragmentManager fm;
    private EditText txtScanPallet;
    private Button btnBack;
    private Button btnSave;

    public FragmentPTLPndLorryLoadingStagingArea() {
    }

    public static FragmentPTLPndLorryLoadingStagingArea newInstance() {
        return new FragmentPTLPndLorryLoadingStagingArea();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle(ACTION_BAR_TITLE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ptl_pnd_lorry_loading_staging_area, container, false);
        Context con = requireContext();

        txtScanPallet = root.findViewById(R.id.txt_ptl_pnd_lorry_loading_staging_area_scan_pallet);
        btnBack = root.findViewById(R.id.btn_ptl_pnd_lorry_loading_staging_area_back);
        btnSave = root.findViewById(R.id.btn_ptl_pnd_lorry_loading_staging_area_save);

        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        UIFuncs.enableInput(con, txtScanPallet);
        txtScanPallet.requestFocus();

        return root;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_ptl_pnd_lorry_loading_staging_area_back) {
            if (fm != null) {
                fm.popBackStack();
            }
        }
        // SAVE — screen only; business logic to be wired later.
    }
}
