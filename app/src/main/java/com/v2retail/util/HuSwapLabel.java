package com.v2retail.util;

import com.v2retail.commons.UIFuncs;

import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text lines for the HU Swap label, laid out like the regular HU label:
 * <pre>
 *   NEW-DES: HO26 STORE NAME
 *   VIA: DO01 HUB NAME                    Qty 107
 *   RDC: DH24 NAME                 Dt:-10.10.2026
 *   OLD-DES: HF03 STORE NAME                 SWAP
 *   ||||||||| new HU barcode |||||||||
 * </pre>
 * Built from ZWM_HUSWAP EX_DATA (NEW_DES, VIA, SOURCE, OLD_DES). Names come from {@link PlantNames},
 * which holds the ZGET_CUSTOMER_NAME_RFC results.
 */
final class HuSwapLabel {

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    final String huNo;
    final String newDesLine;
    final String viaLine;
    final String qtyText;
    final String rdcLine;
    final String dateText;
    final String oldDesLine;
    final String tagText;

    HuSwapLabel(JSONObject ex) {
        String via = firstNonEmpty(field(ex, "VIA"), field(ex, "HUB"));

        huNo = clean(removeLeadingZeros(field(ex, "EXIDV_NEW")));
        newDesLine = clean(truncate(codeWithName("NEW-DES: ", field(ex, "NEW_DES")), 33));
        viaLine = clean(truncate(codeWithName("VIA: ", via), 26));
        qtyText = clean("Qty " + Util.convertToDoubleString(field(ex, "VEMNG_NEW")));
        rdcLine = clean(truncate(codeWithName("RDC: ", field(ex, "SOURCE")), 18));
        dateText = clean("Dt:-" + formatDate(field(ex, "HU_CR_ON")));
        oldDesLine = clean(truncate(codeWithName("OLD-DES: ", field(ex, "OLD_DES")), 28));
        tagText = clean(firstNonEmpty(field(ex, "TVS_TEXT"), "SWAP"));
    }

    /** e.g. "VIA: DO01 BHOPAL HUB", or "VIA: -" when the code is empty. */
    private static String codeWithName(String label, String code) {
        if (code.isEmpty()) {
            return label + "-";
        }
        return (label + code + " " + PlantNames.get(code)).trim();
    }

    /**
     * DD.MM.YYYY from SAP DATS "20261010", "2026-10-10..." or Java Date text
     * "Sat Oct 10 09:50:51 IST 2026". Anything else is returned as-is.
     */
    private static String formatDate(String d) {
        if (d.matches("\\d{8}")) {
            return d.substring(6, 8) + "." + d.substring(4, 6) + "." + d.substring(0, 4);
        }
        if (d.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return d.substring(8, 10) + "." + d.substring(5, 7) + "." + d.substring(0, 4);
        }
        Matcher m = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\b.*?(\\d{4})\\s*$",
                Pattern.CASE_INSENSITIVE).matcher(d);
        if (m.find()) {
            int month = 1;
            for (int i = 0; i < MONTHS.length; i++) {
                if (MONTHS[i].equalsIgnoreCase(m.group(1))) {
                    month = i + 1;
                    break;
                }
            }
            return String.format(Locale.US, "%02d.%02d.%s", Integer.parseInt(m.group(2)), month, m.group(3));
        }
        return d;
    }

    private static String field(JSONObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.isNull(key)) {
            return "";
        }
        return obj.optString(key, "").trim();
    }

    private static String firstNonEmpty(String a, String b) {
        return a != null && !a.isEmpty() ? a : (b != null ? b.trim() : "");
    }

    private static String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private static String removeLeadingZeros(String hu) {
        if (hu == null || hu.trim().isEmpty()) {
            return "";
        }
        return UIFuncs.removeLeadingZeros(hu.trim());
    }

    private static String clean(String text) {
        return text.replace("\r", " ").replace("\n", " ").replace("\\", "").replace("\"", "'");
    }
}
