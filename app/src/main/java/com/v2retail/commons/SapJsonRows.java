package com.v2retail.commons;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

/**
 * Detects and strips SAP "template" rows from JSON RFC table arrays.
 * <p>
 * Production SAP (and Azure {@code /api/hht/ValueXMW}) often prefix {@code ET_*},
 * {@code IT_*}, etc. with a metadata row at index 0 where field values are column
 * titles (e.g. {@code "BIN":"Storage Bin"}). Older HHT code either always skipped
 * index 0 ({@code for (i = 1; ...)}) or never skipped it — both break depending
 * on the gateway. Use {@link #startIndex(JSONArray)} / {@link #sanitizeResponse(JSONObject)}
 * so the same build works on Azure and legacy xmwgw.
 */
public final class SapJsonRows {

    private SapJsonRows() {
    }

    /** True when the row is null or SAP field-description metadata, not business data. */
    public static boolean isMetadataRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        Iterator<String> keys = row.keys();
        boolean hasField = false;
        while (keys.hasNext()) {
            String key = keys.next();
            String val = row.optString(key, "").trim();
            if (val.isEmpty()) {
                continue;
            }
            hasField = true;
            if (val.equalsIgnoreCase(key)) {
                return true;
            }
            if (isKnownDescriptionValue(val)) {
                return true;
            }
        }
        return !hasField;
    }

    /** True when any of {@code fieldKeys} has value equal to the key name (SAP template row). */
    public static boolean isMetadataRow(JSONObject row, String... fieldKeys) {
        if (row == null) {
            return true;
        }
        if (fieldKeys != null) {
            for (String key : fieldKeys) {
                if (key == null) {
                    continue;
                }
                String val = row.optString(key, "").trim();
                if (!val.isEmpty() && val.equalsIgnoreCase(key)) {
                    return true;
                }
            }
        }
        return isMetadataRow(row);
    }

    /**
     * First index to read from {@code arr}. Never skips the only row (single real record).
     */
    public static int startIndex(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return 0;
        }
        if (arr.length() == 1) {
            return 0;
        }
        JSONObject first = arr.optJSONObject(0);
        return isMetadataRow(first) ? 1 : 0;
    }

    public static int startIndex(JSONArray arr, String... fieldKeys) throws JSONException {
        if (arr == null || arr.length() == 0) {
            return 0;
        }
        if (arr.length() == 1) {
            return 0;
        }
        JSONObject first = arr.optJSONObject(0);
        return isMetadataRow(first, fieldKeys) ? 1 : 0;
    }

    /** Removes leading metadata rows from one JSONArray (in place). */
    public static JSONArray stripLeadingMetadataRows(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() <= 1) {
            return arr;
        }
        int start = startIndex(arr);
        if (start == 0) {
            return arr;
        }
        JSONArray out = new JSONArray();
        for (int i = start; i < arr.length(); i++) {
            out.put(arr.get(i));
        }
        return out;
    }

    /**
     * Walks top-level keys on an RFC response and strips metadata row 0 from every JSONArray
     * whose name looks like an SAP table ({@code ET_*}, {@code IT_*}, {@code ET_*}, etc.).
     */
    public static JSONObject sanitizeResponse(JSONObject body) throws JSONException {
        if (body == null) {
            return null;
        }
        Iterator<String> keys = body.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!isSapTableKey(key)) {
                continue;
            }
            Object val = body.opt(key);
            if (val instanceof JSONArray) {
                body.put(key, stripLeadingMetadataRows((JSONArray) val));
            }
        }
        return body;
    }

    private static boolean isSapTableKey(String key) {
        if (key == null || key.length() < 3) {
            return false;
        }
        String u = key.toUpperCase(Locale.ROOT);
        return u.startsWith("ET_") || u.startsWith("IT_") || u.startsWith("ES_");
    }

    private static boolean isKnownDescriptionValue(String val) {
        String v = val.toLowerCase(Locale.ROOT);
        if (v.contains("international article number")) {
            return true;
        }
        if (v.contains("material number")) {
            return true;
        }
        if (v.contains("external handling unit")) {
            return true;
        }
        if (v.contains("numerator for conversion")) {
            return true;
        }
        if (v.contains("component of the version")) {
            return true;
        }
        if ("storage bin".equalsIgnoreCase(val)) {
            return true;
        }
        if ("transfer order number".equalsIgnoreCase(val)) {
            return true;
        }
        if ("ptl nature".equalsIgnoreCase(val)) {
            return true;
        }
        if ("floor".equalsIgnoreCase(val)) {
            return true;
        }
        if ("crate".equalsIgnoreCase(val)) {
            return true;
        }
        if (v.startsWith("section")) {
            return true;
        }
        return false;
    }

    /**
     * ZPTL_GET_DATA_FROM_BIN_RFC / similar PTL ET_DATA row 0:
     * BIN=Storage Bin, PICKLIST=Transfer Order Number, PNATURE=PTL nature, etc.
     */
    public static boolean isPtlBinCrateMetadataRow(JSONObject row) {
        if (row == null) {
            return true;
        }
        if (isMetadataRow(row, "BIN", "PICKLIST", "PNATURE")) {
            return true;
        }
        String bin = row.optString("BIN", "").trim();
        String picklist = row.optString("PICKLIST", "").trim();
        if ("Storage Bin".equalsIgnoreCase(bin) || "Transfer Order Number".equalsIgnoreCase(picklist)) {
            return true;
        }
        if ("PTL nature".equalsIgnoreCase(row.optString("PNATURE", "").trim())) {
            return true;
        }
        String floor = row.optString("FLOOR", "").trim();
        String crate = row.optString("CRATE", "").trim();
        String section = row.optString("ZSECTION", "").trim();
        return "Floor".equalsIgnoreCase(floor)
                && "Crate".equalsIgnoreCase(crate)
                && (section.equalsIgnoreCase("Section") || section.equalsIgnoreCase("Section."));
    }
}
