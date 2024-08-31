package com.v2retail.util;

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
    public static double convertStringToDouble(String input) {
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
}
