package com.v2retail.util;

import java.util.HashMap;

/**
 * Local plant-code → display-name mapping for HU Swap label printing.
 * Used when ZWM_HUSWAP RFC does not return name fields.
 *
 * Format on label:  "DW01 Dehradun-DC"  or  "HB05 Patna-HUB"
 *
 * TO UPDATE: add/edit entries in the static block below.
 * Keys are exact SAP plant codes (WERKS / location codes).
 */
public class PlantNames {

    private static final HashMap<String, String> MAP = new HashMap<>();

    static {
        // ── Distribution Centres ─────────────────────────────────────────
        MAP.put("DW01", "Dehradun-DC");
        MAP.put("DH24", "Delhi-DC");
        MAP.put("DH25", "Delhi-Ecomm");

        // ── Hubs ─────────────────────────────────────────────────────────
        MAP.put("HB05", "Patna-HUB");
        MAP.put("DB03", "Patna-HUB");   // legacy hub code from existing label

        // ── Stores (add as needed) ────────────────────────────────────────
        // MAP.put("HD31", "Store-HD31");
        // MAP.put("HN40", "Store-HN40");

        // ADD MORE ENTRIES HERE
    }

    /**
     * Returns the display name for a plant code, or "" if unknown.
     * Label will show "DW01 Dehradun-DC" when name is found,
     * or just "DW01" when name is empty.
     */
    public static String get(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = MAP.get(code.trim().toUpperCase());
        return name != null ? name : "";
    }

    /**
     * Returns "CODE Name" e.g. "DW01 Dehradun-DC", or just "CODE" if unknown.
     */
    public static String label(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = get(code);
        return name.isEmpty() ? code.trim() : code.trim() + " " + name;
    }
}
