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
}
