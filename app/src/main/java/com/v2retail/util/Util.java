package com.v2retail.util;

import android.content.Context;
import android.util.TypedValue;

import java.text.Format;
import java.text.SimpleDateFormat;

public class Util {
    public static String convertToDoubleString(String input) {
        try {
            double value = Double.parseDouble(input);
            long longValue = (long) value;

            if (value == longValue) {
                return formatDouble(longValue);
            } else {
                return formatDouble(value);
            }
        } catch (NumberFormatException e) {
            return formatDouble(0.0); // or throw an exception, depending on your requirements
        }
    }
    public static String DateTime(String DateFormat,java.util.Date DateToFormat)
    {
        Format formatter = new SimpleDateFormat(DateFormat);
        return formatter.format(DateToFormat);
    }
    public static double convertStringToDouble(String input) {
        if(input == null){
            return 0.0;
        }
        try {
            double value = Double.parseDouble(input);
            long longValue = (long) value;

            if (value == longValue) {
                return  Double.parseDouble(formatDouble(longValue));
            } else {
                return Double.parseDouble(formatDouble(value));
            }
        } catch (NumberFormatException e) {
            return 0.0; // or throw an exception, depending on your requirements
        }
    }
    public static String formatDouble(double value) {
        return String.format("%.3f", value).replaceAll("\\.?0*$", "");
    }
    public static int dpToPx(Context con, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                con.getResources().getDisplayMetrics()
        );
    }

    /**
     * Paperless delivery dropdown stores {@code VBELN-WERKS-PRIORITY-FLOOR}; SAP RFCs expect {@code IM_VBELN}
     * as the delivery document number only.
     */
    public static String deliveryVbelnForPaperlessRfc(String deliverySelection) {
        if (deliverySelection == null) {
            return "";
        }
        String s = deliverySelection.trim();
        if (s.isEmpty()) {
            return "";
        }
        int dash = s.indexOf('-');
        if (dash > 0) {
            return s.substring(0, dash).trim();
        }
        return s;
    }
}
