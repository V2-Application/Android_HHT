package com.v2retail.commons;

import java.util.Locale;

/**
 * Builds RFC URLs from the gateway URL stored at login ({@code SharedPreferences "URL"}).
 */
public final class GatewayUrls {

    private GatewayUrls() {
    }

    public static String baseForNoAclJsonRfc(String storedGatewayUrl) {
        if (storedGatewayUrl == null) {
            return "";
        }
        String u = storedGatewayUrl.trim();
        if (u.isEmpty()) {
            return "";
        }
        String low = u.toLowerCase(Locale.ROOT);
        int vxm = low.indexOf("/valuexmw");
        if (vxm >= 0) {
            return u.substring(0, vxm);
        }
        int last = u.lastIndexOf('/');
        int scheme = u.indexOf("://");
        if (last > scheme + 3 && scheme >= 0) {
            return u.substring(0, last);
        }
        return u;
    }

    public static String noAclJsonRfcUrl(String storedGatewayUrl, String rfcName) {
        String base = baseForNoAclJsonRfc(storedGatewayUrl);
        if (base.isEmpty()) {
            return "";
        }
        return base + "/noacljsonrfcadaptor?bapiname=" + rfcName + "&aclclientid=android";
    }

    /**
     * Production gateways from the connect-screen list ({@code R.array.ipAddress}).
     * Whitelisted rather than derived so an unrecognised URL falls back to the dev
     * behaviour instead of silently writing to production.
     */
    private static final String[] PRODUCTION_GATEWAYS = {
            "v2-hht-api.azurewebsites.net",
            "192.168.144.200",
            "v2axasync-prd",
    };

    /** True when the URL stored at login points at a production gateway. */
    public static boolean isProductionGateway(String storedGatewayUrl) {
        if (storedGatewayUrl == null) {
            return false;
        }
        String low = storedGatewayUrl.toLowerCase(Locale.ROOT);
        for (String marker : PRODUCTION_GATEWAYS) {
            if (low.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /** Production RFC REST API, e.g. {@code https://routemaster.../api/ZWM_HU_SELECTION_RFC}. */
    public static String routemasterApiUrl(String rfcName) {
        return Vars.ROUTEMASTER_API_BASE + "/api/" + rfcName;
    }

    /** REST API path under the login gateway base, e.g. {@code /api/ZVND_PUT01_HU_VAL_RFC}. */
    public static String apiUrl(String storedGatewayUrl, String apiPath) {
        String base = baseForNoAclJsonRfc(storedGatewayUrl);
        if (base.isEmpty()) {
            return "";
        }
        String path = apiPath.startsWith("/") ? apiPath : "/" + apiPath;
        return base + path;
    }
}
