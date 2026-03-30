package com.v2retail.util;

import android.util.Log;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.SharedPreferencesData;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Plant / Store / Hub / DC name lookup.
 *
 * Data source: Azure middleware GET /api/hht/plantnames
 * The middleware fetches from Supabase store_plant_master_aka once
 * and caches for 24h. Android calls the middleware once per session.
 *
 * Usage:
 *   PlantNames.load(serverUrl);      // call from FragmentHUSwapPrint.onResume()
 *   PlantNames.label("DW01")          // "DW01 KOLKATA-RDC" or "DW01" if not yet loaded
 *   PlantNames.get("DW01")            // "KOLKATA-RDC" or ""
 */
public class PlantNames {

    private static final String TAG      = "PlantNames";
    private static final String ENDPOINT = "/api/hht/plantnames";

    private static final HashMap<String, String> CACHE = new HashMap<>();
    private static volatile boolean loaded  = false;
    private static volatile boolean loading = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * "CODE Short-Name" e.g. "DW01 KOLKATA-RDC"
     * Falls back to just "CODE" while names are loading or if unknown.
     */
    public static String label(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = get(code);
        return name.isEmpty() ? code.trim() : code.trim() + " " + name;
    }

    /** Short name for a code, e.g. "KOLKATA-RDC", or "" if not found. */
    public static String get(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = CACHE.get(code.trim().toUpperCase());
        return name != null ? name : "";
    }

    /**
     * Fetch plant names from middleware in the background.
     * Safe to call multiple times — only one fetch per app session.
     *
     * @param serverUrl  The saved server URL (e.g. "https://hht-api.v2retail.net/api/hht/ValueXMW")
     *                   We strip the path and append /api/hht/plantnames.
     */
    public static void load(String serverUrl) {
        if (loaded || loading) return;
        if (serverUrl == null || serverUrl.isEmpty()) return;
        loading = true;

        // Derive base: "https://hht-api.v2retail.net" from any /api/hht/... URL
        String base = serverUrl;
        int apiIdx = base.indexOf("/api/");
        if (apiIdx > 0) base = base.substring(0, apiIdx);
        // Strip trailing slash
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String url = base + ENDPOINT;
        Log.d(TAG, "Fetching plant names from: " + url);

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
                    Log.e(TAG, "Failed to parse plant names: " + e.getMessage());
                }
            },
            error -> {
                loading = false;
                Log.w(TAG, "Could not load plant names (will show codes only): " + error);
            }
        );

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            15000, 1, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    public static boolean isLoaded() { return loaded; }
}
