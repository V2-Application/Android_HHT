package com.v2retail.util;

public class AppConstants {

    // ── Azure Cloud (PRIMARY — all India rollout) ──────────────────────
    public static final String URL = "https://v2-hht-api.azurewebsites.net/api/hht/ValueXMW";

    // Timeout: 90s to handle heavy SAP RFC calls (stock take, GRT, etc.)
    public static final int VOLLEY_TIMEOUT = 90000;

    // ── Legacy on-prem (for reference only — DO NOT use for rollout) ──
    // public static final String URL = "http://192.168.144.200:9080/xmwgw/ValueXMW";
    // public static final String URL = "http://192.168.151.40:8080/xmwgw/ValueXMW";
    // public static final String URL = "http://192.168.151.40:7080/xmwgw/ValueXMW";
    // public static final String URL = "http://192.168.151.40:6080/xmwgw/ValueXMW";
}
