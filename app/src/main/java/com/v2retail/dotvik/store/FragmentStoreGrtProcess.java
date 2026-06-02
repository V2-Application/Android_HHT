package com.v2retail.dotvik.store;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

/**
 * Store GRT Process screen (UI only).
 */
public class FragmentStoreGrtProcess extends Fragment implements View.OnClickListener {

    private static final String SOURCE_0001 = "0001";
    private static final String SOURCE_0006 = "0006";

    View rootView;
    Context con;
    AlertBox box;
    FragmentManager fm;

    String WERKS = "";

    LinearLayout source0001, source0006;
    EditText txtPicklistNo, txtExternalHu, txtFdesPlant, txtArticle, txtScanQty;
    TextView emptyHint;
    Button btnCancel, btnReset, btnSubmit;

    String selectedSource = SOURCE_0001;

    public FragmentStoreGrtProcess() {
    }

    public static FragmentStoreGrtProcess newInstance() {
        return new FragmentStoreGrtProcess();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Store GRT Process");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_store_grt_process, container, false);

        con = getContext();
        box = new AlertBox(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        WERKS = data.read("WERKS");

        source0001 = rootView.findViewById(R.id.store_grt_source_0001);
        source0006 = rootView.findViewById(R.id.store_grt_source_0006);

        txtPicklistNo = rootView.findViewById(R.id.store_grt_picklist_no);
        txtExternalHu = rootView.findViewById(R.id.store_grt_external_hu);
        txtFdesPlant = rootView.findViewById(R.id.store_grt_fdes_plant);
        txtArticle = rootView.findViewById(R.id.store_grt_article);
        txtScanQty = rootView.findViewById(R.id.store_grt_scan_qty);
        emptyHint = rootView.findViewById(R.id.store_grt_empty_hint);

        btnCancel = rootView.findViewById(R.id.store_grt_btn_cancel);
        btnReset = rootView.findViewById(R.id.store_grt_btn_reset);
        btnSubmit = rootView.findViewById(R.id.store_grt_btn_submit);

        source0001.setOnClickListener(this);
        source0006.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);

        selectSource(SOURCE_0001);
        clear();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.store_grt_source_0001:
                selectSource(SOURCE_0001);
                break;
            case R.id.store_grt_source_0006:
                selectSource(SOURCE_0006);
                break;
            case R.id.store_grt_btn_cancel:
                box.confirmBack(fm, con);
                break;
            case R.id.store_grt_btn_reset:
                box.getBox("Confirm", "Reset! Are you sure?", (dialogInterface, i) -> {
                    clear();
                }, (dialogInterface, i) -> {
                    return;
                });
                break;
            case R.id.store_grt_btn_submit:
                // UI only - submit logic to be implemented.
                break;
        }
    }

    private void selectSource(String source) {
        selectedSource = source;
        boolean is0001 = SOURCE_0001.equals(source);
        source0001.setBackgroundResource(is0001
                ? R.drawable.bg_grt_source_selected
                : R.drawable.bg_grt_source_unselected);
        source0006.setBackgroundResource(is0001
                ? R.drawable.bg_grt_source_unselected
                : R.drawable.bg_grt_source_selected);
    }

    private void clear() {
        txtPicklistNo.setText("");
        txtExternalHu.setText("");
        txtFdesPlant.setText("");
        txtArticle.setText("");
        txtScanQty.setText("0");
        emptyHint.setVisibility(View.VISIBLE);
    }
}
