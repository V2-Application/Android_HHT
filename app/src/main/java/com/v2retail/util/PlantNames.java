package com.v2retail.util;

import android.util.Log;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Plant / store name lookup for HU Swap label printing.
 *
 * Fetches from the HHT middleware /plants endpoint (which pre-caches
 * the Supabase store_plant_master_aka table on deployment).
 * Only one network call per app session; subsequent calls use the in-memory cache.
 *
 * Usage:
 *   PlantNames.load();           // call once, e.g. in FragmentHUSwapPrint.onResume()
 *   PlantNames.label("DW01")     // "DW01 KOLKATA-RDC" or just "DW01" if not loaded
 *   PlantNames.get("DW01")       // "KOLKATA-RDC" or "" if not found
 */
public class PlantNames {

    private static final String TAG = "PlantNames";

    private static final HashMap<String, String> CACHE = new HashMap<>();
    private static volatile boolean loaded  = false;
    private static volatile boolean loading = false;

    // ── Public API ─────────────────────────────────────────────────────────────

    /** "CODE Name" e.g. "DW01 KOLKATA-RDC", or just "DW01" if names not loaded. */
    public static String label(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = get(code);
        return name.isEmpty() ? code.trim() : code.trim() + " " + name;
    }

    /** Short name e.g. "KOLKATA-RDC", or "" if unknown / not yet loaded. */
    public static String get(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String v = CACHE.get(code.trim().toUpperCase());
        return v != null ? v : "";
    }

    /**
     * Trigger a one-time background fetch from the HHT middleware /plants endpoint.
     * Safe to call multiple times — only fetches once per app session.
     */
    public static void load() {
        if (loaded || loading) return;
        loading = true;

        // /plants returns a JSON object: { "DW01": "KOLKATA-RDC", "HD22": "KPSHR", ... }
        String url = Vars.URL + "/plants";

        JsonObjectRequest req = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    HashMap<String, String> temp = new HashMap<>();
                    Iterator<String> keys = response.keys();
                    while (keys.hasNext()) {
                        String code = keys.next().trim().toUpperCase();
                        String name = response.optString(code, "").trim();
                        if (!code.isEmpty() && !name.isEmpty()) {
                            temp.put(code, name);
                        }
                    }
                    CACHE.clear();
                    CACHE.putAll(temp);
                    loaded  = true;
                    loading = false;
                    Log.d(TAG, "Loaded " + CACHE.size() + " plant names from middleware");
                } catch (Exception e) {
                    loading = false;
                    Log.e(TAG, "Failed to parse plant names response: " + e.getMessage());
                }
            },
            error -> {
                loading = false;
                Log.w(TAG, "Could not load plant names — codes only on label: " + error.getMessage());
            }
        );

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            8000, 1, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    public static boolean isLoaded() { return loaded; }
}
