package com.v2retail.dotvik.dc.ptlnew.ptl40;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses export parameters shared by PTL HU transfer RFCs:
 * {@code ZWM_PTL_HU_V61_V62}, {@code ZWM_PTL_HU_V62_V63}, {@code ZWM_PTL_HU_V63_V64},
 * {@code ZWM_PTL_HU_V64_V65}.
 * <p>
 * Import: {@code IM_USER}, {@code IM_PLANT}, {@code IM_PALETTE}.
 * Export: {@code EX_HUB} ({@code ZCLA_HU}), {@code EX_STORE} ({@code ZGRT_ORDCONF}),
 * {@code EX_PALETTE_CNT} ({@code I}), {@code EX_RETURN} ({@code BAPIRET2}).
 */
public final class PtlHuTransferRfcResponse {

    private static final String TAG = PtlHuTransferRfcResponse.class.getSimpleName();

    private PtlHuTransferRfcResponse() {
    }

    /** {@code EX_HUB} is {@code ZCLA_HU}; adaptor may return a flat string or structure with {@code HUB}. */
    public static String extractHub(JSONObject responsebody) {
        return firstNonEmpty(
                extractExportString(responsebody, "EX_HUB", "HUB"),
                responsebody.optString("HUB", "").trim());
    }

    /** {@code EX_STORE} is {@code ZGRT_ORDCONF}; adaptor may return a flat string or structure with {@code STORE}. */
    public static String extractStore(JSONObject responsebody) {
        return firstNonEmpty(
                extractExportString(responsebody, "EX_STORE", "STORE"),
                responsebody.optString("STORE", "").trim());
    }

    /** {@code EX_PALETTE_CNT} is type {@code I}. */
    public static String extractPaletteCount(JSONObject responsebody) {
        if (!responsebody.has("EX_PALETTE_CNT")) {
            return "";
        }
        try {
            Object raw = responsebody.get("EX_PALETTE_CNT");
            if (raw instanceof Number) {
                return String.valueOf(((Number) raw).intValue());
            }
            String text = String.valueOf(raw).trim();
            return "null".equalsIgnoreCase(text) ? "" : text;
        } catch (JSONException e) {
            return responsebody.optString("EX_PALETTE_CNT", "").trim();
        }
    }

    static String extractExportString(JSONObject responsebody, String exportKey, String... nestedKeys) {
        if (!responsebody.has(exportKey)) {
            return "";
        }
        try {
            Object raw = responsebody.get(exportKey);
            if (raw instanceof JSONObject) {
                JSONObject obj = (JSONObject) raw;
                if (nestedKeys != null) {
                    for (String key : nestedKeys) {
                        String value = obj.optString(key, "").trim();
                        if (!TextUtils.isEmpty(value)) {
                            return value;
                        }
                    }
                }
                return "";
            }
            String text = String.valueOf(raw).trim();
            return "null".equalsIgnoreCase(text) ? "" : text;
        } catch (JSONException e) {
            Log.w(TAG, "extractExportString: " + exportKey, e);
            return responsebody.optString(exportKey, "").trim();
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (!TextUtils.isEmpty(v)) {
                return v;
            }
        }
        return "";
    }
}
