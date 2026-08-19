package com.v2retail.dotvik.dc;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;

/**
 * Crate Putway
 *
 * Flow:
 *   1. Scan Crate No  -> copies into Crate No
 *   2. Scan Bin No    -> copies into Bin No
 *   3. Save / Reset
 */
public class FragmentCratePutway extends Fragment implements View.OnClickListener {

    private View view;
    private Activity activity;
    private AlertBox box;

    private EditText etScanCrate;
    private EditText etCrateNo;
    private EditText etScanBin;
    private EditText etBinNo;
    private Button btnReset;
    private Button btnSave;

    public FragmentCratePutway() {}

    public static FragmentCratePutway newInstance() {
        return new FragmentCratePutway();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_crate_putway, container, false);

        etScanCrate = view.findViewById(R.id.crate_putway_et_scan_crate);
        etCrateNo = view.findViewById(R.id.crate_putway_et_crate_no);
        etScanBin = view.findViewById(R.id.crate_putway_et_scan_bin);
        etBinNo = view.findViewById(R.id.crate_putway_et_bin_no);
        btnReset = view.findViewById(R.id.crate_putway_btn_reset);
        btnSave = view.findViewById(R.id.crate_putway_btn_save);

        btnReset.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        etScanCrate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                acceptCrate();
                return true;
            }
        });

        etScanBin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                acceptBin();
                return true;
            }
        });

        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity())
                    .setActionBarTitle("Crate Putway");
        }
    }

    private void init() {
        activity = getActivity();
        box = new AlertBox(activity);
        resetFields();
    }

    private void acceptCrate() {
        String crate = etScanCrate.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate No.");
            etScanCrate.requestFocus();
            return;
        }
        etScanCrate.setText("");
        etScanCrate.setEnabled(false);
        etCrateNo.setText(crate);
        etScanBin.setEnabled(true);
        etScanBin.requestFocus();
    }

    private void acceptBin() {
        String bin = etScanBin.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin No.");
            etScanBin.requestFocus();
            return;
        }
        etScanBin.setText("");
        etScanBin.setEnabled(false);
        etBinNo.setText(bin);
    }

    private void resetFields() {
        etScanCrate.setText("");
        etCrateNo.setText("");
        etScanBin.setText("");
        etBinNo.setText("");

        etScanCrate.setEnabled(true);
        etScanBin.setEnabled(false);

        etScanCrate.requestFocus();
    }

    private void save() {
        String crate = etCrateNo.getText().toString().trim();
        String bin = etBinNo.getText().toString().trim();

        if (TextUtils.isEmpty(crate)) {
            box.getBox("Alert", "Please scan Crate No.");
            etScanCrate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(bin)) {
            box.getBox("Alert", "Please scan Bin No.");
            if (etScanBin.isEnabled()) {
                etScanBin.requestFocus();
            }
            return;
        }

        box.getBox("Success", "Crate " + crate + " putway to Bin " + bin);
        resetFields();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.crate_putway_btn_reset) {
            resetFields();
        } else if (id == R.id.crate_putway_btn_save) {
            save();
        }
    }
}
