package com.v2retail.dotvik.dc;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

import java.util.Locale;

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
        Log.d(TAG, "onCreate -> Scan HU (Article) process started");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> title=Scan Article");
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
        Log.d(TAG, "onCreateView -> WERKS=" + WERKS + " USER=" + USER + " URL=" + URL);

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
        addScanEvents();

        txtScanHu.requestFocus();
        Log.d(TAG, "init complete -> focus=Scan HU");
        return rootView;
    }

    private void addScanEvents() {
        txtScanHu.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enterDown) {
                    String hu = valueOf(txtScanHu);
                    Log.d(TAG, "Scan HU editor action -> hu=" + hu + " actionId=" + actionId);
                    onHuScanned(hu);
                    return true;
                }
                return false;
            }
        });

        txtScanHu.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hu = s.toString().trim().toUpperCase(Locale.ROOT);
                if (!hu.isEmpty() && scannerReading) {
                    Log.d(TAG, "Scan HU wedge detect -> hu=" + hu);
                    onHuScanned(hu);
                }
            }
        });

        txtScanArticle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterDown = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enterDown) {
                    String article = valueOf(txtScanArticle);
                    Log.d(TAG, "Scan Article editor action -> article=" + article + " actionId=" + actionId);
                    onArticleScanned(article);
                    return true;
                }
                return false;
            }
        });

        txtScanArticle.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = (before == 0 && start == 0) && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String article = s.toString().trim().toUpperCase(Locale.ROOT);
                if (!article.isEmpty() && scannerReading) {
                    Log.d(TAG, "Scan Article wedge detect -> article=" + article);
                    onArticleScanned(article);
                }
            }
        });
    }

    private void onHuScanned(String hu) {
        Log.d(TAG, "onHuScanned -> hu=" + hu);
        if (hu == null || hu.isEmpty()) {
            Log.d(TAG, "onHuScanned skipped -> empty HU");
            box.getBox("Alert", "Scan HU Number!");
            txtScanHu.requestFocus();
            return;
        }
        Log.d(TAG, "HU accepted -> waiting for Scan Article. current MAT=" + valueOf(txtMat)
                + " HUQ=" + valueOf(txtHuQty)
                + " ScanQ=" + valueOf(txtScanQty)
                + " DiffQ=" + valueOf(txtDiffQty));
        txtScanArticle.requestFocus();
    }

    private void onArticleScanned(String article) {
        Log.d(TAG, "onArticleScanned -> article=" + article + " scannedHU=" + valueOf(txtScanHu));
        if (valueOf(txtScanHu).isEmpty()) {
            Log.d(TAG, "onArticleScanned skipped -> Scan HU first");
            box.getBox("Alert", "Scan HU Number!");
            txtScanHu.requestFocus();
            return;
        }
        if (article == null || article.isEmpty()) {
            Log.d(TAG, "onArticleScanned skipped -> empty article");
            box.getBox("Alert", "Scan Article!");
            txtScanArticle.requestFocus();
            return;
        }
        Log.d(TAG, "Article accepted -> MAT=" + valueOf(txtMat)
                + " HUQ=" + valueOf(txtHuQty)
                + " ScanQ=" + valueOf(txtScanQty)
                + " DiffQ=" + valueOf(txtDiffQty)
                + " tableRows=" + (tableItems == null ? 0 : Math.max(0, tableItems.getChildCount() - 1)));
        txtScanArticle.setText("");
        txtScanArticle.requestFocus();
    }

    private String valueOf(EditText field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim().toUpperCase(Locale.ROOT);
    }

    private void logScreenState(String action) {
        Log.d(TAG, action + " -> HU=" + valueOf(txtScanHu)
                + " Article=" + valueOf(txtScanArticle)
                + " MAT=" + valueOf(txtMat)
                + " HUQ=" + valueOf(txtHuQty)
                + " ScanQ=" + valueOf(txtScanQty)
                + " DiffQ=" + valueOf(txtDiffQty)
                + " noDataVisible=" + (tvNoData != null && tvNoData.getVisibility() == View.VISIBLE));
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_scan_hu_article_back) {
            logScreenState("BACK clicked");
            if (fm != null) {
                fm.popBackStack();
            }
        } else if (view.getId() == R.id.btn_scan_hu_article_save) {
            logScreenState("SAVE clicked");
            Log.d(TAG, "SAVE skipped -> RFC not wired, no data to save");
            box.getBox("Alert", "No data to save.");
        }
    }
}
