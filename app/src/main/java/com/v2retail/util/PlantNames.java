package com.v2retail.util;

import android.util.Log;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.ApplicationController;
import com.v2retail.commons.Vars;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Plant code → short name lookup.
 * Fetched once from the HHT middleware /api/hht/plantnames endpoint,
 * which caches the data from Supabase (refreshed every 12 hours).
 * Supabase is never called directly from the device.
 *
 * Usage:
 *   PlantNames.load();                 // call from FragmentHUSwapPrint.onResume()
 *   PlantNames.label("DW01")           // "DW01 KOLKATA-RDC" or "DW01" if not loaded
 *   PlantNames.get("DW01")             // "KOLKATA-RDC" or ""
 */
public class PlantNames {

    private static final String TAG = "PlantNames";

    // Middleware endpoint — no direct Supabase calls from devices
    private static final String ENDPOINT = "/api/hht/plantnames";

    private static final HashMap<String, String> CACHE = new HashMap<>();
    private static boolean loaded  = false;
    private static boolean loading = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns "CODE Short-Name" e.g. "DW01 KOLKATA-RDC"
     * Returns just "CODE" if names not loaded yet or code not found.
     */
    public static String label(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = get(code);
        return name.isEmpty() ? code.trim() : code.trim() + " " + name;
    }

    /**
     * Returns the short name, e.g. "KOLKATA-RDC", or "" if not found.
     */
    public static String get(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = CACHE.get(code.trim().toUpperCase());
        return name != null ? name : "";
    }

    /**
     * Fetch plant names from middleware (which caches from Supabase).
     * Safe to call multiple times — only fetches once per app session.
     */
    public static void load() {
        if (loaded || loading) return;
        loading = true;

        // Build URL from the server address stored in SharedPreferences
        SharedPreferencesData prefs = new SharedPreferencesData(
            ApplicationController.getInstance().getApplicationContext());
        String serverUrl = prefs.read("SERVER_URL");
        if (serverUrl == null || serverUrl.isEmpty()) {
            loading = false;
            Log.w(TAG, "No server URL in prefs — skipping plant names load");
            return;
        }
        // Normalise: strip trailing slash, remove /ValueXMW if present
        String base = serverUrl.trim();
        if (base.endsWith("/ValueXMW")) base = base.substring(0, base.length() - "/ValueXMW".length());
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String url = base + ENDPOINT;
        Log.d(TAG, "Loading plant names from: " + url);

        JsonObjectRequest req = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    CACHE.clear();
                    Iterator<String> keys = response.keys();
                    while (keys.hasNext()) {
                        String code = keys.next();
                        String name = response.optString(code, "");
                        if (!code.isEmpty() && !name.isEmpty()) {
                            CACHE.put(code.toUpperCase(), name);
                        }
                    }
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
                Log.w(TAG, "Plant names fetch failed — codes only on label: " + error.getMessage());
            }
        );

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            8000, 1, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    public static boolean isLoaded() { return loaded; }
}
