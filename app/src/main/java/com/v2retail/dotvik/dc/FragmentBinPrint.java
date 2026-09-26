package com.v2retail.dotvik.dc;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;
import com.v2retail.util.TSPLPrinter;

import java.util.HashMap;
import java.util.Map;

/**
 * Inventory BIN Print. Scan a bin, show a Code128 barcode with the bin
 * number under the bars, then print that label.
 */
public class FragmentBinPrint extends Fragment {

    private EditText printerInput;
    private TextView printerStatus;
    private EditText binInput;
    private ImageView barcodeView;
    private TextView binText;
    private Button printButton;
    private String tvsPrinter;
    private AlertBox box;
    private ProgressDialog dialog;
    private Context con;
    private SharedPreferencesData data;
    private String currentBin = "";
    private final Runnable buildRunnable = new Runnable() {
        @Override
        public void run() {
            if (binInput != null) {
                showBarcode(binInput.getText().toString());
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Process_Selection_Activity) {
            ((Process_Selection_Activity) getActivity()).setActionBarTitle("Bin Print");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bin_print, container, false);
        con = getContext();
        box = new AlertBox(con);
        dialog = new ProgressDialog(con);
        data = new SharedPreferencesData(con);

        printerInput = view.findViewById(R.id.bin_print_printer);
        printerStatus = view.findViewById(R.id.bin_print_printer_status);
        binInput = view.findViewById(R.id.bin_print_bin);
        barcodeView = view.findViewById(R.id.bin_print_barcode);
        binText = view.findViewById(R.id.bin_print_bin_text);
        printButton = view.findViewById(R.id.bin_print_print);

        printerInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enter = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE || enter) {
                    detectPrinter(printerInput.getText().toString());
                    return true;
                }
                return false;
            }
        });
        printerInput.addTextChangedListener(new TextWatcher() {
            private boolean scannerReading;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scannerReading = before == 0 && start == 0 && count > 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && scannerReading) {
                    detectPrinter(s.toString());
                }
            }
        });

        String savedPrinter = data.read(Vars.TVS_PRINTER);
        if (savedPrinter != null && savedPrinter.length() > 0) {
            TSPLPrinter helper = new TSPLPrinter(con);
            if (helper.findBluetoothPrinterByScan(savedPrinter)) {
                applyPrinter(helper.getPrinterName());
            } else {
                lockBinUntilPrinter();
            }
        } else {
            lockBinUntilPrinter();
        }

        binInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enter = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || enter) {
                    binInput.removeCallbacks(buildRunnable);
                    showBarcode(binInput.getText().toString());
                    return true;
                }
                return false;
            }
        });

        binInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binInput.removeCallbacks(buildRunnable);
                if (s.length() == 0) {
                    clearBarcode();
                    return;
                }
                binInput.postDelayed(buildRunnable, 400);
            }
        });

        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printBin();
            }
        });

        return view;
    }

    private void lockBinUntilPrinter() {
        tvsPrinter = null;
        printerStatus.setText("Scan printer first");
        printerStatus.setTextColor(0xFFB71C1C);
        UIFuncs.disableInput(con, binInput);
        printerInput.requestFocus();
    }

    private void applyPrinter(String printerName) {
        tvsPrinter = printerName;
        printerInput.setText(printerName);
        data.write(Vars.TVS_PRINTER, printerName);
        printerStatus.setText("Printer ready: " + printerName);
        printerStatus.setTextColor(0xFF1B8A4C);
        UIFuncs.enableInput(con, binInput);
        binInput.requestFocus();
    }

    private void detectPrinter(String raw) {
        String printerName = raw == null ? "" : raw.trim().toUpperCase();
        TSPLPrinter helper = new TSPLPrinter(con);
        boolean found = printerName.isEmpty()
                ? helper.findSavedOrKnownLabelPrinter(data.read(Vars.TVS_PRINTER))
                : helper.findBluetoothPrinterByScan(printerName);
        if (!found) {
            box.getBox("Not Paired",
                    "Printer is not paired with this device.\nPair TVS370 or PP310 in Bluetooth settings first.");
            printerInput.setText("");
            lockBinUntilPrinter();
            return;
        }
        applyPrinter(helper.getPrinterName());
    }

    private void showBarcode(String raw) {
        String bin = raw == null ? "" : raw.trim().toUpperCase();
        if (bin.isEmpty()) {
            clearBarcode();
            return;
        }
        Bitmap bitmap = createBarcode(bin);
        if (bitmap == null) {
            clearBarcode();
            box.getBox("Alert", "Cannot create barcode for this bin.");
            return;
        }
        currentBin = bin;
        barcodeView.setImageBitmap(bitmap);
        barcodeView.setVisibility(View.VISIBLE);
        binText.setText(bin);
        binText.setVisibility(View.VISIBLE);
    }

    private void clearBarcode() {
        currentBin = "";
        barcodeView.setImageBitmap(null);
        barcodeView.setVisibility(View.GONE);
        binText.setText("");
        binText.setVisibility(View.GONE);
    }

    private Bitmap createBarcode(String value) {
        try {
            int width = Math.max(600, value.length() * 40);
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 8);
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(value, BarcodeFormat.CODE_128, width, 180, hints);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void printBin() {
        if (tvsPrinter == null || tvsPrinter.isEmpty()) {
            box.getBox("Alert", "Scan Printer first!");
            printerInput.requestFocus();
            return;
        }
        if (currentBin == null || currentBin.isEmpty()) {
            box.getBox("Alert", "Scan Bin!");
            binInput.requestFocus();
            return;
        }
        final String printerName = tvsPrinter;
        final String bin = currentBin;

        dialog.setMessage("Printing...");
        dialog.setCancelable(false);
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = new TSPLPrinter(con).sendBinPrintCommand(printerName, bin);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        if (!isAdded()) {
                            return;
                        }
                        if (ok) {
                            Toast.makeText(con, "Printed " + bin, Toast.LENGTH_SHORT).show();
                        } else {
                            box.getBox("Print Failed",
                                    "Could not print bin label. Check the printer is on and in range.");
                        }
                    }
                });
            }
        }).start();
    }
}
