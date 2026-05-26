package com.v2retail.commons;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Volley JSON RFC request that strips SAP metadata rows from table arrays before
 * fragments handle the response. Drop-in replacement for {@link JsonObjectRequest}.
 */
public class SapJsonObjectRequest extends JsonObjectRequest {

    public SapJsonObjectRequest(int method, String url, JSONObject jsonRequest,
                                Listener<JSONObject> listener, ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        Response<JSONObject> res = super.parseNetworkResponse(response);
        if (res != null && res.result != null) {
            try {
                SapJsonRows.sanitizeResponse(res.result);
            } catch (JSONException ignored) {
                // Keep raw body if sanitization fails.
            }
        }
        return res;
    }
}
