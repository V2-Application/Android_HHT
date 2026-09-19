package com.v2retail.util;

import com.v2retail.commons.UIFuncs;
import com.v2retail.commons.Vars;

import org.json.JSONObject;

/**
 * Newland PP310 (NLS-PP310) label commands.
 * This printer speaks CPCL (not TSPL). TVS370 / 4B-2033 layouts stay in {@link TSPLPrinter}.
 */
public final class PP310Printer {

    private static final int LABEL_WIDTH_DOTS = (int) Math.round((70 / 25.4) * 203);
    private static final int LABEL_HEIGHT_DOTS = (int) Math.round((40 / 25.4) * 203);
    private static final String CRLF = "\r\n";

    private PP310Printer() {
    }

    public static boolean isPP310Name(String deviceName) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return false;
        }
        String normalized = deviceName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return normalized.contains("PP310") || normalized.contains("NLSPP310");
    }

    public static String buildHuPrintCommand(JSONObject huObj, String copies, String process) {
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
        if (huObj != null) {
            try {
                werks = huObj.getString("DWERKS");
                warehouse = huObj.getString("DWERKS_NAME1");
                hubName = huObj.getString("HUB_NAME");
                hub = huObj.getString("HUB");
                hub = String.format("HUB:- %s / %s", hub, hubName);
                qty = String.format("Qty %s", Util.convertToDoubleString(huObj.getString("VEMNG")));
                hhtid = String.format("HHT ID %s", UIFuncs.removeLeadingZeros(huObj.getString("HHT_ID")));
                date = String.format("Date:- %s", formatPrintDate(huObj.getString("DATUM")));
                weight = String.format("HU Wt:- %s %s", huObj.getString("WEIGHT") + huObj.getString("GEWEI"), huObj.getString("PRIORITY"));
                tvstext = huObj.getString("TVS_TEXT");
                huno = removeLeadingZeros(huObj.getString("SAP_HU"));
            } catch (Exception ignored) {
            }
        }

        werks = clean(werks);
        warehouse = clean(warehouse);
        qty = clean(qty);
        hhtid = clean(hhtid);
        date = clean(date);
        weight = clean(weight);
        tvstext = clean(tvstext);
        huno = clean(huno);
        hub = clean(hub);

        // 40mm @ 203dpi ≈ 320 dots. Use the top margin and keep HU number below the bars.
        final int y1 = 8;
        final int y2 = 40;
        final int y3 = 72;
        final int y4 = 104;
        final int barcodeY = 140;
        final int barcodeH = 90;
        final int numberY = barcodeY + barcodeH + 16;

        StringBuilder sb = startLabel(copies);
        if (Vars.PTL_NEW_MODULE_HU_CLOSE.equalsIgnoreCase(process)) {
            rtlText(sb, qty, 12, y1, 40);
            text(sb, 20, y1, hub);
            text(sb, 20, y2, werks + " " + warehouse);
            text(sb, 20, y3, hhtid);
            rtlText(sb, date, 12, y3, 80);
            text(sb, 20, y4, weight);
            rtlText(sb, tvstext, 12, y4, 30);
        } else {
            text(sb, 20, y1, werks + " " + warehouse);
            rtlText(sb, qty, 12, y2, 40);
            text(sb, 20, y2, hub);
            text(sb, 20, y3, hhtid);
            rtlText(sb, date, 12, y3, 80);
            text(sb, 20, y4, weight);
            rtlText(sb, tvstext, 12, y4, 30);
        }
        barcode128(sb, 90, barcodeY, barcodeH, huno);
        centerText(sb, numberY, huno);
        return endLabel(sb);
    }

    public static String buildHuSwapLabel(String oldHu, String newHu,
                                         String qty, String crDate,
                                         String crTime, String source, String dest) {
        String printHu = removeLeadingZeros(newHu);
        String displayOldHu = removeLeadingZeros(oldHu);
        String fmtDate = formatSwapDate(crDate);
        String fmtTime = formatSwapTime(crTime, crDate);

        String srcName = PlantNames.label(source);
        String destName = PlantNames.label(dest);
        final int f3Max = 23;
        String destSwap = (destName.isEmpty() ? "HU SWAP" : destName + " SWAP");
        if (destSwap.length() > f3Max) {
            destSwap = destSwap.substring(0, f3Max);
        }
        String fromLine = "From: " + (srcName.isEmpty() ? nvl(source) : srcName);
        String toLine = "To:   " + (destName.isEmpty() ? nvl(dest) : destName);
        if (fromLine.length() > f3Max) {
            fromLine = fromLine.substring(0, f3Max);
        }
        if (toLine.length() > f3Max) {
            toLine = toLine.substring(0, f3Max);
        }

        printHu = clean(printHu);
        displayOldHu = clean(displayOldHu);
        destSwap = clean(destSwap);
        fromLine = clean(fromLine);
        toLine = clean(toLine);
        qty = clean(qty);

        StringBuilder sb = startLabel("1");
        qrCode(sb, 8, 76, 8, printHu);
        line(sb, 175, 5, 177, 315, 2);
        sb.append("SETMAG 1 2").append(CRLF);
        textFont(sb, 4, 180, 5, printHu);
        sb.append("SETMAG 1 1").append(CRLF);
        line(sb, 178, 74, 553, 75, 1);
        text(sb, 180, 80, destSwap);
        line(sb, 178, 113, 553, 114, 1);
        text(sb, 180, 118, fromLine);
        text(sb, 180, 150, toLine);
        line(sb, 178, 183, 553, 184, 1);
        textFont(sb, 0, 180, 188, fmtDate + "  " + fmtTime);
        textFont(sb, 0, 180, 212, "Qty: " + qty);
        line(sb, 178, 240, 553, 243, 3);
        textFont(sb, 0, 180, 248, "Old: " + displayOldHu);
        return endLabel(sb);
    }

    public static String buildStoreGrtLabel(String huNo,
                                           String sourceCode, String sourceName,
                                           String destPlant, String destHub, String destName,
                                           String qty, String dateTime) {
        String rawHu = huNo != null ? huNo.trim() : "";
        String printHu = removeLeadingZeros(rawHu);
        if (printHu.isEmpty() && !rawHu.isEmpty()) {
            printHu = rawHu;
        }

        String lineSrcSite = formatCodeWithName("S. SITE: ", nvl(sourceCode).trim(), nvl(sourceName).trim());
        String lineDHub = "D. HUB: " + (nvl(destHub).trim().isEmpty() ? "-" : destHub.trim());
        String lineDSite = formatCodeWithName("D. SITE: ", nvl(destPlant).trim(), nvl(destName).trim());
        String lineQty = "QTY : " + (qty != null && !qty.isEmpty() ? qty : "0");
        String lineDate = "DATE:" + (dateTime != null ? dateTime.trim() : "");

        lineSrcSite = truncate(clean(lineSrcSite), 32);
        lineDHub = truncate(clean(lineDHub), 32);
        lineDSite = truncate(clean(lineDSite), 32);
        lineQty = truncate(clean(lineQty), 32);
        lineDate = truncate(clean(lineDate), 32);
        printHu = clean(printHu);

        StringBuilder sb = startLabel("1");
        text(sb, 20, 25, lineSrcSite);
        text(sb, 20, 55, lineDHub);
        text(sb, 20, 85, lineDSite);
        text(sb, 20, 115, lineQty);
        text(sb, 20, 145, lineDate);
        int barcodeX = Math.max(40, (LABEL_WIDTH_DOTS / 2) - 180);
        barcode128(sb, barcodeX, 175, 90, printHu);
        centerText(sb, 275, printHu);
        return endLabel(sb);
    }

    public static String buildHuGrtLabel(String huNo, JSONObject huRow) {
        String rawHu = huNo != null ? huNo.trim() : "";
        String printHu = removeLeadingZeros(rawHu);
        if (printHu.isEmpty() && !rawHu.isEmpty()) {
            printHu = rawHu;
        }

        String swerks = jsonField(huRow, "SWERKS");
        String dwerks = jsonField(huRow, "DWERKS");
        String vemng = jsonField(huRow, "VEMNG");
        String datum = formatPrintDate(jsonField(huRow, "DATUM"));

        String lineSrcSite = truncate(clean("S. SITE: " + (swerks.isEmpty() ? "-" : swerks)), 32);
        String lineDSite = truncate(clean("D. SITE: " + (dwerks.isEmpty() ? "-" : dwerks)), 32);
        String lineQty = truncate(clean("QTY : " + (vemng.isEmpty() ? "0" : Util.convertToDoubleString(vemng))), 32);
        String lineDate = truncate(clean("DATE: " + (datum.isEmpty() ? "-" : datum)), 32);
        printHu = clean(printHu);

        StringBuilder sb = startLabel("1");
        text(sb, 20, 25, lineSrcSite);
        text(sb, 20, 55, lineDSite);
        text(sb, 20, 85, lineQty);
        text(sb, 20, 115, lineDate);
        int barcodeX = Math.max(40, (LABEL_WIDTH_DOTS / 2) - 180);
        barcode128(sb, barcodeX, 185, 90, printHu);
        centerText(sb, 285, printHu);
        return endLabel(sb);
    }

    private static StringBuilder startLabel(String copies) {
        StringBuilder sb = new StringBuilder();
        sb.append("! 0 200 200 ").append(LABEL_HEIGHT_DOTS).append(" ").append(copyCount(copies)).append(CRLF);
        sb.append("PW ").append(LABEL_WIDTH_DOTS).append(CRLF);
        sb.append("TONE 5").append(CRLF);
        sb.append("SPEED 3").append(CRLF);
        sb.append("SETFF 0 0").append(CRLF);
        return sb;
    }

    private static String endLabel(StringBuilder sb) {
        sb.append("FORM").append(CRLF);
        sb.append("PRINT").append(CRLF);
        return sb.toString();
    }

    private static void text(StringBuilder sb, int x, int y, String value) {
        textFont(sb, 7, x, y, value);
    }

    private static void textFont(StringBuilder sb, int font, int x, int y, String value) {
        sb.append("TEXT ").append(font).append(" 0 ").append(x).append(" ").append(y).append(" ")
                .append(value == null ? "" : value).append(CRLF);
    }

    private static void rtlText(StringBuilder sb, String text, int fontSizeInDots, int y, int extraWidth) {
        if (text == null) {
            text = "";
        }
        int textWidth = text.length() * fontSizeInDots;
        int startX = Math.max(20, LABEL_WIDTH_DOTS - (textWidth + extraWidth));
        text(sb, startX, y, text);
    }

    private static void centerText(StringBuilder sb, int y, String value) {
        if (value == null) {
            value = "";
        }
        int textWidth = value.length() * 12;
        int x = Math.max(20, (LABEL_WIDTH_DOTS - textWidth) / 2);
        text(sb, x, y, value);
    }

    private static void barcode128(StringBuilder sb, int x, int y, int height, String data) {
        sb.append("BT OFF").append(CRLF);
        sb.append("BARCODE 128 2 2 ").append(height).append(" ").append(x).append(" ").append(y)
                .append(" ").append(data == null ? "" : data).append(CRLF);
    }

    private static void qrCode(StringBuilder sb, int x, int y, int cell, String data) {
        sb.append("B QR ").append(x).append(" ").append(y).append(" M 2 U ").append(cell).append(CRLF);
        sb.append(data == null ? "" : data).append(CRLF);
        sb.append("ENDQR").append(CRLF);
    }

    private static void line(StringBuilder sb, int x0, int y0, int x1, int y1, int thickness) {
        sb.append("LINE ").append(x0).append(" ").append(y0).append(" ")
                .append(x1).append(" ").append(y1).append(" ").append(thickness).append(CRLF);
    }

    private static int copyCount(String copies) {
        try {
            int n = Integer.parseInt(copies.trim());
            return n < 1 ? 1 : n;
        } catch (Exception e) {
            return 1;
        }
    }

    private static String formatPrintDate(String rawDate) {
        if (rawDate == null) {
            return "";
        }
        String d = rawDate.trim();
        if (d.length() == 8 && d.matches("\\d{8}")) {
            return d.substring(6, 8) + "-" + d.substring(4, 6) + "-" + d.substring(0, 4);
        }
        return d;
    }

    private static String formatSwapDate(String crDate) {
        try {
            if (crDate == null) {
                return "";
            }
            String d = crDate.trim();
            if (d.length() == 8 && d.matches("\\d{8}")) {
                return d.substring(6, 8) + "." + d.substring(4, 6) + "." + d.substring(0, 4);
            }
            if (d.matches(".*\\d{4}.*")) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(\\d{1,2})\\D+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\D+(\\d{4})|" +
                                        "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\D+(\\d{1,2})\\D+(\\d{4})",
                                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(d);
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                if (m.find()) {
                    String day;
                    String mon;
                    String yr;
                    if (m.group(1) != null) {
                        day = String.format("%02d", Integer.parseInt(m.group(1)));
                        mon = m.group(2);
                        yr = m.group(3);
                    } else {
                        mon = m.group(4);
                        day = String.format("%02d", Integer.parseInt(m.group(5)));
                        yr = m.group(6);
                    }
                    int mi = 1;
                    for (int k = 0; k < months.length; k++) {
                        if (months[k].equalsIgnoreCase(mon)) {
                            mi = k + 1;
                            break;
                        }
                    }
                    return day + "." + String.format("%02d", mi) + "." + yr;
                }
                return d.length() > 10 ? d.substring(0, 10) : d;
            }
            return d.length() > 10 ? d.substring(0, 10) : d;
        } catch (Exception ignore) {
            return crDate != null ? crDate : "";
        }
    }

    private static String formatSwapTime(String crTime, String crDate) {
        String fmtTime = "";
        try {
            if (crTime != null) {
                String t = crTime.trim();
                if (t.length() == 6 && t.matches("\\d{6}")) {
                    fmtTime = t.substring(0, 2) + ":" + t.substring(2, 4) + ":" + t.substring(4, 6);
                } else if (t.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    fmtTime = t;
                } else {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("(\\d{2}:\\d{2}:\\d{2})").matcher(t);
                    if (m.find()) {
                        fmtTime = m.group(1);
                    }
                }
            }
            if (fmtTime.isEmpty() && crDate != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(\\d{2}:\\d{2}:\\d{2})").matcher(crDate);
                if (m.find()) {
                    fmtTime = m.group(1);
                }
            }
        } catch (Exception ignore) {
            fmtTime = crTime != null ? crTime : "";
        }
        return fmtTime;
    }

    private static String jsonField(JSONObject obj, String key) {
        if (obj == null || key == null || !obj.has(key) || obj.isNull(key)) {
            return "";
        }
        return obj.optString(key, "").trim();
    }

    private static String formatCodeWithName(String prefix, String code, String name) {
        String c = code != null ? code.trim() : "";
        String n = name != null ? name.trim() : "";
        if (!c.isEmpty() && !n.isEmpty()) {
            return prefix + c + " (" + n + ")";
        }
        if (!c.isEmpty()) {
            return prefix + c;
        }
        if (!n.isEmpty()) {
            return prefix + n;
        }
        return prefix + "-";
    }

    private static String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text != null ? text : "";
        }
        return text.substring(0, maxLen);
    }

    private static String removeLeadingZeros(String hu) {
        if (hu == null || hu.isEmpty()) {
            return hu != null ? hu : "";
        }
        return UIFuncs.removeLeadingZeros(hu.trim());
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", " ").replace("\n", " ").replace("\"", "'");
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
