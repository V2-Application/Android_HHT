package com.v2retail.util;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.v2retail.ApplicationController;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches plant/store/hub/DC names from Supabase store_plant_master_aka.
 * Data is loaded once on first use and cached in memory.
 *
 * Usage:
 *   PlantNames.load();            // call once on app start or first HU Swap Print
 *   PlantNames.label("DW01")      // returns "DW01 KOLKATA-DC" or just "DW01" if not loaded
 *   PlantNames.get("DW01")        // returns "KOLKATA-DC" or "" if not found
 */
public class PlantNames {

    private static final String TAG          = "PlantNames";
    private static final String BASE_URL     = "https://pymdqnnwwxrgeolvgvgv.supabase.co";
    private static final String ANON_KEY     = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB5bWRxbm53d3hyZ2VvbHZndmd2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMzMzU0NzYsImV4cCI6MjA2ODkxMTQ3Nn0.jUrb0jIg6qjj2Rlh9DxYesSnbstoD4uoDCswqOqAkUM";
    // Fetch STORE-CODE + STORE-NAME (short name, more label-friendly than STORE-FULL-NAME)
    private static final String ENDPOINT     =
        BASE_URL + "/rest/v1/store_plant_master_aka?select=STORE-CODE,STORE-NAME&limit=1000";

    private static final HashMap<String, String> CACHE = new HashMap<>();
    private static boolean loaded  = false;
    private static boolean loading = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns "CODE Short-Name" e.g. "DW01 KOLKATA-RDC"
     * Falls back to just "CODE" if names not loaded yet.
     */
    public static String label(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = get(code);
        return name.isEmpty() ? code.trim() : code.trim() + " " + name;
    }

    /**
     * Returns the short name for a code, e.g. "KOLKATA-RDC", or "" if unknown.
     */
    public static String get(String code) {
        if (code == null || code.trim().isEmpty()) return "";
        String name = CACHE.get(code.trim().toUpperCase());
        return name != null ? name : "";
    }

    /**
     * Trigger a background fetch from Supabase.
     * Safe to call multiple times — only fetches once.
     * Call from Application.onCreate() or FragmentHUSwapPrint.onResume().
     */
    public static void load() {
        if (loaded || loading) return;
        loading = true;

        JsonArrayRequest req = new JsonArrayRequest(
            Request.Method.GET, ENDPOINT, null,
            response -> {
                try {
                    HashMap<String, String> temp = new HashMap<>();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject row = response.getJSONObject(i);
                        String code = row.optString("STORE-CODE", "").trim().toUpperCase();
                        String name = row.optString("STORE-NAME", "").trim();
                        if (!code.isEmpty() && !name.isEmpty()) {
                            temp.put(code, name);
                        }
                    }
                    CACHE.clear();
                    CACHE.putAll(temp);
                    loaded  = true;
                    loading = false;
                    Log.d(TAG, "Loaded " + CACHE.size() + " plant names from Supabase");
                } catch (Exception e) {
                    loading = false;
                    Log.e(TAG, "Failed to parse plant names: " + e.getMessage());
                }
            },
            error -> {
                loading = false;
                Log.w(TAG, "Could not load plant names (will show codes only): " + error.getMessage());
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey",        ANON_KEY);
                headers.put("Authorization", "Bearer " + ANON_KEY);
                return headers;
            }
        };

        req.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            10000, 1, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(req);
    }

    /** True if names have been successfully loaded. */
    public static boolean isLoaded() { return loaded; }
}
