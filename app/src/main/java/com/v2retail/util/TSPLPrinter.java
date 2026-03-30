package com.v2retail.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.UUID;

/**
 * @author Narayanan
 * @version 11.73
 * {@code Author: Narayanan, Revision: 1, Created: 27th Aug 2024, Modified: 27th Aug 2024}
 */
public class TSPLPrinter {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice printerDevice;
    Context con;
    private String printerName;
    // UUID for serial port connection (SPP)
    private static final UUID SERIAL_PORT_SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String process;

    // Constructor to initialize Bluetooth adapter
    public TSPLPrinter(Context con) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.process = "";
        this.con = con;
    }

    public TSPLPrinter(Context con, String process) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.process = process;
        this.con = con;
    }

    // Function to send print command via Bluetooth
    public void sendPrintCommandToBluetoothPrinter(String printerName, JSONObject huObj, String copies) {
        try {
            // Find the Bluetooth printer by name
            findBluetoothPrinter(printerName, false);

            if (printerDevice != null) {
                // Connect to the Bluetooth printer
                connectToBluetoothPrinter();

                // Build the TSPL command
                String tsplCommand = buildHuPrintCommand(huObj, copies);

                // Send TSPL command to the printer
                if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    OutputStream outputStream = bluetoothSocket.getOutputStream();
                    PrintWriter writer = new PrintWriter(outputStream, true);

                    writer.write(tsplCommand);
                    writer.flush();

                    // Close the connection after printing
                    writer.close();
                    outputStream.close();
                    bluetoothSocket.close();
                }
            } else {
                Log.e("TSPLPrinter", "Bluetooth printer not found!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to find the Bluetooth printer by name
    public boolean findBluetoothPrinter(String printerName, boolean checkStartsWith) {
        if (ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermission(con);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices to find the printer
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equalsIgnoreCase(printerName) || (checkStartsWith && device.getName().toUpperCase().startsWith(printerName.toUpperCase()))) {
                    this.printerName = device.getName();
                    printerDevice = device;
                    return true;
                }
            }
        }
        return false;
    }
    public String getPrinterName(){
        return this.printerName;
    }
    // Function to connect to the Bluetooth printer
    private void connectToBluetoothPrinter() throws Exception {
        if (printerDevice != null) {
            if (ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermission(con);
            }
            bluetoothSocket = printerDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_UUID);
            bluetoothSocket.connect();  // Blocking call; will attempt to connect
        }
    }

    private String buildHuPrintCommand(JSONObject huObj, String copies) {

        String werks = "HDXX";
        String warehouse = "WH NAME";
        String qty = "Qty XXX";
        String hhtid = "HHT ID XXX";
        String date = "Date:- 01 Jan 1990";
        String weight = "HU Weight:- XXKg P1";
        String tvstext = "XXXXXX";
        String huno = "1234567890";
        String hubName = "X-ABC";
        String hub = "HBXX";
        if(huObj != null){
            try{
                werks = huObj.getString("DWERKS");
                warehouse = huObj.getString("DWERKS_NAME1");
                hubName = huObj.getString("HUB_NAME");
                hub = huObj.getString("HUB");
                hub = String.format("HUB:- %s / %s", hub, hubName);
                qty = String.format("Qty %s", Util.convertToDoubleString(huObj.getString("VEMNG")));
                hhtid = String.format("HHT ID %s", UIFuncs.removeLeadingZeros(huObj.getString("HHT_ID")));
                date = String.format("Date:- %s", huObj.getString("DATUM"));
                weight = String.format("HU Weight:- %s %s", huObj.getString("WEIGHT")+huObj.getString("GEWEI"),huObj.getString("PRIORITY"));
                tvstext = huObj.getString("TVS_TEXT");
                huno = huObj.getString("SAP_HU");
            }catch (Exception exce){

            }
        }
        double labelWidthInDots = (70 / 25.4) * 203;
        if(Vars.PTL_NEW_MODULE_HU_CLOSE.equalsIgnoreCase(this.process)){
            return "SIZE 70 mm, 40 mm\n" +
                    "GAP 3 mm, 0 mm\n" +
                    "DIRECTION 0\n" +
                    "CLS\n" +
                    generateRTLTextCommand(qty, labelWidthInDots, 12, 40, 40) +
                    "TEXT 20, 40, \"3\", 0, 1, 1, \"" + hub + "\"\n" +
                    "TEXT 20, 80, \"3\", 0, 1, 1, \"" + werks + " " + warehouse + "\"\n" +
                    "TEXT 20, 120, \"3\", 0, 1, 1, \"" + hhtid + "\"\n" +
                    generateRTLTextCommand(date, labelWidthInDots, 12, 120, 80) +
                    "TEXT 20, 160, \"3\", 0, 1, 1, \"" + weight + "\"\n" +
                    generateRTLTextCommand(tvstext, labelWidthInDots, 12, 160, 30) +
                    "BARCODE 110, 210, \"128\", 100, 0, 0, 4, 8, \"" + huno + "\"\n" +
                    "TEXT 210, 320, \"3\", 0, 1, 1, \"" + huno + "\"\n" +
                    "PRINT 1, "+copies+"\n";
        }else {
            return "SIZE 70 mm, 40 mm\n" +
                    "GAP 3 mm, 0 mm\n" +
                    "DIRECTION 0\n" +
                    "CLS\n" +
                    "TEXT 20, 40, \"3\", 0, 1, 1, \"" + werks + " " + warehouse + "\"\n" +
                    generateRTLTextCommand(qty, labelWidthInDots, 12, 80, 40) +
                    "TEXT 20, 80, \"3\", 0, 1, 1, \"" + hub + "\"\n" +
                    "TEXT 20, 120, \"3\", 0, 1, 1, \"" + hhtid + "\"\n" +
                    generateRTLTextCommand(date, labelWidthInDots, 12, 120, 80) +
                    "TEXT 20, 160, \"3\", 0, 1, 1, \"" + weight + "\"\n" +
                    generateRTLTextCommand(tvstext, labelWidthInDots, 12, 160, 30) +
                    "BARCODE 110, 210, \"128\", 100, 0, 0, 4, 8, \"" + huno + "\"\n" +
                    "TEXT 210, 320, \"3\", 0, 1, 1, \"" + huno + "\"\n" +
                    "PRINT 1, "+copies+"\n";
        }
    }

    public void requestBluetoothPermission(Context con) {
        if (ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(con, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(
                    (Activity) con,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_ADVERTISE,Manifest.permission.BLUETOOTH_ADMIN},
                    1
            );
        }
    }

    public String generateRTLTextCommand(String text, double labelWidthInDots, int fontSizeInDots, int y, int exwidth) {
        // Measure text width based on font size and number of characters
        int textWidth = text.length() * fontSizeInDots; // Simplified, adjust according to font

        // Calculate the starting X position for right-aligned text
        int startX = (int)(labelWidthInDots - (textWidth + exwidth));

        int fontsize = fontSizeInDots / 12;

        // Generate the TSPL command for right-aligned text
        String command = "TEXT " + startX + ", " + y + ",\"3\",0, " + fontsize + ", " + fontsize + ",\"" + text + "\"\n";

        return command;
    }

    public static String extractDate(String sapDate) {
        String[] parts = sapDate.split(" ");
        if (parts.length >= 3) {
            // Combine the first three parts to form the date
            return parts[0] + " " + parts[1] + " " + parts[2];
        } else {
            return "Invalid date format";
        }
    }

    /**
     * Print a new HU label for the HU Swap process.
     * Shows routing (source → destination), new HU barcode, quantity and date.
     */
    public void sendHuSwapPrintCommand(String printerName,
                                       String oldHu, String newHu,
                                       String qty,   String crDate,
                                       String crTime, String source, String dest) {
        try {
            findBluetoothPrinter(printerName, false);
            if (printerDevice == null) {
                Log.e("TSPLPrinter", "HU Swap: printer not found: " + printerName);
                return;
            }
            connectToBluetoothPrinter();
            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) return;

            String tspl = buildHuSwapLabel(oldHu, newHu, qty, crDate, crTime, source, dest);
            java.io.OutputStream out = bluetoothSocket.getOutputStream();
            java.io.PrintWriter w = new java.io.PrintWriter(out, true);
            w.write(tspl);
            w.flush();
            w.close();
            out.close();
            bluetoothSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildHuSwapLabel(String oldHu, String newHu,
                                    String qty,   String crDate,
                                    String crTime, String source, String dest) {

        // 1. Strip leading zeros from HU numbers
        String printHu      = removeLeadingZeros(newHu);
        String displayOldHu = removeLeadingZeros(oldHu);

        // 2. Date: handles SAP DATS (YYYYMMDD) and Java Date.toString() garbage
        String fmtDate = "";
        try {
            if (crDate != null) {
                String d = crDate.trim();
                if (d.length() == 8 && d.matches("\\d{8}")) {
                    fmtDate = d.substring(6,8) + "." + d.substring(4,6) + "." + d.substring(0,4);
                } else {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{4})",
                                 java.util.regex.Pattern.CASE_INSENSITIVE).matcher(d);
                    String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                                       "Jul","Aug","Sep","Oct","Nov","Dec"};
                    if (m.find()) {
                        String day = String.format("%02d", Integer.parseInt(m.group(1)));
                        String yr  = m.group(3);
                        int mi = 1;
                        for (int k = 0; k < months.length; k++) {
                            if (months[k].equalsIgnoreCase(m.group(2))) { mi = k+1; break; }
                        }
                        fmtDate = day + "." + String.format("%02d", mi) + "." + yr;
                    } else {
                        fmtDate = d.length() > 10 ? d.substring(0, 10) : d;
                    }
                }
            }
        } catch (Exception ignore) { fmtDate = crDate != null ? crDate : ""; }

        // 3. Time: SAP TIMS HHMMSS → HH:MM:SS, also handles pre-formatted strings
        String fmtTime = "";
        try {
            if (crTime != null) {
                String t = crTime.trim();
                if (t.length() == 6 && t.matches("\\d{6}")) {
                    fmtTime = t.substring(0,2) + ":" + t.substring(2,4) + ":" + t.substring(4,6);
                } else if (t.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    fmtTime = t;
                } else {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(\\d{2}:\\d{2}:\\d{2})").matcher(t);
                    fmtTime = m.find() ? m.group(1) : "";
                }
            }
        } catch (Exception ignore) { fmtTime = crTime != null ? crTime : ""; }

        // 4. Resolve codes to display labels using local plant name map
        //    Format: "DW01 Dehradun-DC"  or just "DW01" if name unknown
        String srcLabel  = com.v2retail.util.PlantNames.label(source);
        String destLabel = com.v2retail.util.PlantNames.label(dest);

        // Header line e.g. "HB05 Patna-HUB SWAP"
        String headerLine = (destLabel.isEmpty() ? "HU SWAP" : destLabel + " SWAP");

        // Route line e.g. "DW01 Dehradun-DC -> HB05 Patna-HUB"
        String routeLine = (!srcLabel.isEmpty() && !destLabel.isEmpty())
                         ? srcLabel + " -> " + destLabel
                         : srcLabel + destLabel;

        // Date + time on one line
        String dateTime = fmtDate + (fmtTime.isEmpty() ? "" : "  " + fmtTime);

        // ── TSPL — Landscape 70mm × 40mm @ 203 DPI ───────────────────────
        //
        //  ┌──────────────┬────────────────────────────────────────────┐
        //  │              │  {NEW HU}              (font 4, largest)  │
        //  │  [ QR CODE ] │  {DEST SWAP}           (font 3)          │
        //  │  left 30%    │  {SRC} -> {DEST}       (font 2)          │
        //  │  centered    │  DD.MM.YYYY  HH:MM:SS  (font 2)          │
        //  │              ├────────────────────────────────────────────│
        //  │              │  Old: {OLD HU}    Qty: {QTY}  (font 1)  │
        //  └──────────────┴────────────────────────────────────────────┘
        //
        // 70mm × 40mm @ 203 DPI = 560 × 320 dots
        // QR: cell=7, Version 1 (21×21 modules) = 147 dots wide/tall
        //   → left 30% = 168 dots. QR at x=12, centered vertically: y=(320-147)/2=87
        // Vertical divider: x=166
        // Text right side: x=175 onwards, 375 dots available

        return "SIZE 70 mm, 40 mm\n" +
               "GAP 3 mm, 0 mm\n" +
               "DIRECTION 0\n" +
               "CLS\n" +

               // QR code — left side, vertically centered
               // cell=7, ECC M, auto mode — no built-in readable text (zero overlap)
               "QRCODE 12, 87, M, 7, A, 0, \"" + printHu + "\"\n" +

               // Vertical divider line
               "BAR 166, 5, 2, 310\n" +

               // ── Right side — text ──
               // New HU number: largest text, most important
               "TEXT 175, 10, \"4\", 0, 1, 1, \"" + printHu + "\"\n" +

               // Destination + SWAP header
               "TEXT 175, 52, \"3\", 0, 1, 1, \"" + headerLine + "\"\n" +

               // Route: src → dest  (with names)
               "TEXT 175, 86, \"2\", 0, 1, 1, \"" + routeLine + "\"\n" +

               // Date + Time
               "TEXT 175, 112, \"2\", 0, 1, 1, \"" + dateTime + "\"\n" +

               // Horizontal divider above footer
               "BAR 175, 140, 375, 2\n" +

               // Footer: old HU left, qty right
               "TEXT 175, 152, \"1\", 0, 1, 1, \"Old: " + displayOldHu + "\"\n" +
               "TEXT 440, 152, \"1\", 0, 1, 1, \"Qty:" + qty + "\"\n" +

               "PRINT 1, 1\n";
    }

    /** Remove leading zeros: "000000H005678" → "H005678", "000012345" → "12345" */
    private static String removeLeadingZeros(String hu) {
        if (hu == null || hu.isEmpty()) return hu;
        // Find first non-zero character
        int i = 0;
        while (i < hu.length() - 1 && hu.charAt(i) == '0') i++;
        return hu.substring(i);
    }
}
