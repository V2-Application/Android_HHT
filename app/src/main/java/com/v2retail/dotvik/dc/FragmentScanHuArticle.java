package com.v2retail.dotvik.dc;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

public class FragmentScanHuArticle extends Fragment implements View.OnClickListener {

    private static final String TAG = FragmentScanHuArticle.class.getSimpleName();

    private FragmentManager fm;
    private Context con;
    private AlertBox box;
    private String URL = "";
    private String WERKS = "";
    private String USER = "";

    private EditText txtScanHu;
    private EditText txtScanArticle;
    private EditText txtMat;
    private EditText txtHuQty;
    private EditText txtScanQty;
    private EditText txtDiffQty;
    private TableLayout tableItems;
    private TextView tvNoData;
    private Button btnBack;
    private Button btnSave;

    public FragmentScanHuArticle() {
    }

    public static FragmentScanHuArticle newInstance() {
        return new FragmentScanHuArticle();
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
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Scan Article");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan_hu_article, container, false);
        con = requireContext();
        box = new AlertBox(con);

        SharedPreferencesData data = new SharedPreferencesData(con);
        URL = data.read("URL");
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txtScanHu = rootView.findViewById(R.id.txt_scan_hu_article_scan_hu);
        txtScanArticle = rootView.findViewById(R.id.txt_scan_hu_article_scan_article);
        txtMat = rootView.findViewById(R.id.txt_scan_hu_article_mat);
        txtHuQty = rootView.findViewById(R.id.txt_scan_hu_article_hu_qty);
        txtScanQty = rootView.findViewById(R.id.txt_scan_hu_article_scan_qty);
        txtDiffQty = rootView.findViewById(R.id.txt_scan_hu_article_diff_qty);
        tableItems = rootView.findViewById(R.id.table_scan_hu_article_items);
        tvNoData = rootView.findViewById(R.id.tv_scan_hu_article_no_data);
        btnBack = rootView.findViewById(R.id.btn_scan_hu_article_back);
        btnSave = rootView.findViewById(R.id.btn_scan_hu_article_save);

        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        txtScanHu.requestFocus();
        return rootView;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_scan_hu_article_back) {
            if (fm != null) {
                fm.popBackStack();
            }
        } else if (view.getId() == R.id.btn_scan_hu_article_save) {
            box.getBox("Alert", "No data to save.");
        }
    }
}
