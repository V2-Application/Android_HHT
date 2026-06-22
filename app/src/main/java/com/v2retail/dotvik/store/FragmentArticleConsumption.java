package com.v2retail.dotvik.store;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.v2retail.commons.UIFuncs;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.Util;

public class FragmentArticleConsumption extends Fragment implements View.OnClickListener {

    View rootView;
    Context con;
    AlertBox box;
    FragmentManager fm;

    String WERKS = "";
    String USER = "";

    // Conversion factor (EA per PAC) for the resolved article. Auto-populated from material.
    double packSize = 1;

    EditText txt_store, txt_barcode, txt_article, txt_scan_qty;
    RadioGroup rg_uom;
    RadioButton rb_single, rb_pack;
    TextView txt_uom_note, txt_total_qty;
    Button btn_back, btn_save;

    public FragmentArticleConsumption() {
    }

    public static FragmentArticleConsumption newInstance() {
        return new FragmentArticleConsumption();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Home_Activity) getActivity()).setActionBarTitle("Article Consumption");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_article_consumption, container, false);

        con = getContext();
        box = new AlertBox(con);
        SharedPreferencesData data = new SharedPreferencesData(con);
        WERKS = data.read("WERKS");
        USER = data.read("USER");

        txt_store = rootView.findViewById(R.id.txt_article_consumption_store);
        txt_barcode = rootView.findViewById(R.id.txt_article_consumption_barcode);
        txt_article = rootView.findViewById(R.id.txt_article_consumption_article);
        txt_scan_qty = rootView.findViewById(R.id.txt_article_consumption_scan_qty);
        rg_uom = rootView.findViewById(R.id.rg_article_consumption_uom);
        rb_single = rootView.findViewById(R.id.rb_article_consumption_single);
        rb_pack = rootView.findViewById(R.id.rb_article_consumption_pack);
        txt_uom_note = rootView.findViewById(R.id.txt_article_consumption_uom_note);
        txt_total_qty = rootView.findViewById(R.id.txt_article_consumption_total_qty);

        btn_back = rootView.findViewById(R.id.btn_article_consumption_back);
        btn_save = rootView.findViewById(R.id.btn_article_consumption_save);

        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        clear();
        addInputEvents();

        return rootView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_article_consumption_back:
                box.confirmBack(fm, con);
                break;
            case R.id.btn_article_consumption_save:
                save();
                break;
        }
    }

    private void clear() {
        packSize = 1;
        txt_store.setText(WERKS);
        txt_barcode.setText("");
        txt_article.setText("");
        txt_scan_qty.setText("");
        rb_single.setChecked(true);
        updatePackLabel();
        recalcTotal();
        UIFuncs.enableInput(con, txt_barcode);
        txt_barcode.requestFocus();
    }

    private void updatePackLabel() {
        if (packSize > 1) {
            rb_pack.setText("UOM-PACK  — PAC = " + Util.formatDouble(packSize) + " EA");
        } else {
            rb_pack.setText("UOM-PACK");
        }
    }

    private double currentFactor() {
        return rb_pack.isChecked() ? packSize : 1;
    }

    private void recalcTotal() {
        double qty = Util.convertStringToDouble(UIFuncs.toUpperTrim(txt_scan_qty));
        double total = qty * currentFactor();
        txt_total_qty.setText(Util.formatDouble(total) + " EA");
    }

    private void addInputEvents() {
        txt_barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    UIFuncs.hideKeyboard(getActivity());
                    String value = UIFuncs.toUpperTrim(txt_barcode);
                    if (!value.isEmpty()) {
                        onBarcodeScanned(value);
                        return true;
                    }
                }
                return false;
            }
        });

        txt_scan_qty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recalcTotal();
            }
        });

        rg_uom.setOnCheckedChangeListener((group, checkedId) -> recalcTotal());
    }

    // Resolves the scanned barcode to an article and its pack conversion (EA per PAC).
    private void onBarcodeScanned(String barcode) {
        // TODO: wire to backend lookup to resolve article + pack size (UMREZ) for this barcode.
        txt_article.setText(barcode);
        updatePackLabel();
        recalcTotal();
        txt_scan_qty.requestFocus();
    }

    private void save() {
        String article = UIFuncs.toUpperTrim(txt_article);
        double qty = Util.convertStringToDouble(UIFuncs.toUpperTrim(txt_scan_qty));

        if (article.isEmpty()) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "Please scan a barcode first");
            return;
        }
        if (qty <= 0) {
            UIFuncs.errorSound(con);
            box.getBox("Invalid", "Please enter a valid scan qty");
            return;
        }

        double total = qty * currentFactor();
        box.getBox("Success", "Consumption saved: " + Util.formatDouble(total) + " EA",
                (dialog, which) -> clear());
    }
}
