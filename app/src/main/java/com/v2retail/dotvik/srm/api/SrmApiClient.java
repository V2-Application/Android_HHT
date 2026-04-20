package com.v2retail.dotvik.srm.api;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SrmApiClient {

    // ── Change this to your SRM Portal backend URL ──────────────────────
    public static final String BASE_URL = "http://192.168.151.50:5000/api";
    // ─────────────────────────────────────────────────────────────────────

    private static final String PREFS     = "srm_prefs";
    private static final String KEY_TOKEN = "srm_access_token";
    private static final String KEY_USER  = "srm_user_json";
    private static final String KEY_ROLE  = "srm_user_role";

    private static RequestQueue queue;

    public static RequestQueue getQueue(Context ctx) {
        if (queue == null) queue = Volley.newRequestQueue(ctx.getApplicationContext());
        return queue;
    }

    public static String getToken(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    public static void saveSession(Context ctx, String token, String userJson, String role) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER, userJson)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public static void clearSession(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static String getSavedRole(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROLE, "");
    }

    public static String getSavedUser(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, "{}");
    }

    /** Authenticated JSON POST */
    public static void post(Context ctx, String path, JSONObject body,
                            Response.Listener<JSONObject> onSuccess,
                            Response.ErrorListener onError) {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST,
                BASE_URL + path, body, onSuccess, onError) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders(ctx);
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(30000, 1, 1.0f));
        getQueue(ctx).add(req);
    }

    /** Authenticated JSON GET */
    public static void get(Context ctx, String path,
                           Response.Listener<JSONObject> onSuccess,
                           Response.ErrorListener onError) {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                BASE_URL + path, null, onSuccess, onError) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders(ctx);
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(30000, 1, 1.0f));
        getQueue(ctx).add(req);
    }

    /** Authenticated JSON PATCH */
    public static void patch(Context ctx, String path, JSONObject body,
                             Response.Listener<JSONObject> onSuccess,
                             Response.ErrorListener onError) {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.PATCH,
                BASE_URL + path, body, onSuccess, onError) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders(ctx);
            }
        };
        getQueue(ctx).add(req);
    }

    private static Map<String, String> authHeaders(Context ctx) {
        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "application/json");
        String token = getToken(ctx);
        if (token != null) h.put("Authorization", "Bearer " + token);
        return h;
    }

    /** Parse Volley error into human-readable message */
    public static String parseError(com.android.volley.VolleyError error) {
        if (error.networkResponse != null) {
            try {
                String body = new String(error.networkResponse.data);
                JSONObject json = new JSONObject(body);
                return json.optString("message", "Request failed");
            } catch (Exception ignored) {}
            return "HTTP " + error.networkResponse.statusCode;
        }
        return error.getMessage() != null ? error.getMessage() : "Network error";
    }
}
