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

    // Constructor to initialize Bluetooth adapter
    public TSPLPrinter(Context con) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.con = con;
    }

    // Function to send print command via Bluetooth
    public void sendPrintCommandToBluetoothPrinter(String printerName, JSONObject huObj) {
        try {
            // Find the Bluetooth printer by name
            findBluetoothPrinter(printerName, false);

            if (printerDevice != null) {
                // Connect to the Bluetooth printer
                connectToBluetoothPrinter();

                // Build the TSPL command
                String tsplCommand = buildHuPrintCommand(huObj);

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

    private String buildHuPrintCommand(JSONObject huObj) {
        String werks = "HDXX";
        String warehouse = "WH NAME";
        String qty = "Qty XXX";
        String hhtid = "HHT ID XXX";
        String date = "Date:- 01 Jan 1990";
        String weight = "HU Weight:- XXKg";
        String tvstext = "XXXXXX";
        String huno = "1234567890";
        if(huObj != null){
            try{
                werks = huObj.getString("DWERKS");
                warehouse = huObj.getString("DWERKS_NAME1");
                qty = String.format("Qty %s", Util.convertToDoubleString(huObj.getString("VEMNG")));
                hhtid = String.format("HHT ID %s", UIFuncs.removeLeadingZeros(huObj.getString("HHT_ID")));
                date = String.format("Date:- %s", huObj.getString("DATUM"));
                weight = String.format("HU Weight:- %s", huObj.getString("WEIGHT")+huObj.getString("GEWEI"));
                tvstext = huObj.getString("TVS_TEXT");
                huno = huObj.getString("SAP_HU");
            }catch (Exception exce){

            }
        }
        double labelWidthInDots = (70 / 25.4) * 203;
        return "SIZE 70 mm, 40 mm\n" +
                "GAP 3 mm, 0 mm\n" +
                "DIRECTION 0\n" +
                "CLS\n" +
                "TEXT 20, 40, \"3\", 0, 1, 1, \"" + werks + " " + warehouse + "\"\n" +
                generateRTLTextCommand(qty, labelWidthInDots, 12, 40, 40) +
                "TEXT 20, 100, \"3\", 0, 1, 1, \"" + hhtid + "\"\n" +
                generateRTLTextCommand(date, labelWidthInDots, 12, 100, 80) +
                "TEXT 20, 160, \"3\", 0, 1, 1, \"" + weight + "\"\n" +
                generateRTLTextCommand(tvstext, labelWidthInDots, 12, 160, 30) +
                "BARCODE 110, 210, \"128\", 100, 0, 0, 4, 8, \"" + huno + "\"\n" +
                "TEXT 210, 320, \"3\", 0, 1, 1, \"" + huno + "\"\n" +
                "PRINT 1, 2\n";
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
}