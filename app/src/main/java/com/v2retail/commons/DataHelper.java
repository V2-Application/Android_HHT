package com.v2retail.commons;

import com.v2retail.dotvik.modal.grt.createhu.HUEANData;

import org.json.JSONException;
import org.json.JSONObject;

public class DataHelper {

    /**
     * One row of {@code ZMM_MARM_STR} / table type {@code ZMM_MARM_STR_TT}
     * ({@code ET_EAN_DATA} on {@code ZWM_PTL_STATION_CRATE_VALIDATE}): MATNR, MEINH, UMREZ, EAN11.
     */
    public static HUEANData parseZmmMarmStr(JSONObject row) {
        HUEANData ean = new HUEANData();
        if (row == null) {
            return ean;
        }
        ean.setLgmatnr(row.optString("MATNR", "").trim());
        ean.setLgmeinh(row.optString("MEINH", "").trim());
        ean.setLgumrez(parseSapDecimal(row, "UMREZ"));
        ean.setLgean11(row.optString("EAN11", "").trim());
        return ean;
    }

    private static double parseSapDecimal(JSONObject row, String key) {
        Object raw = row.opt(key);
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        String s = row.optString(key, "0").trim();
        if (s.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static HUEANData initEANData(JSONObject eanDataObj)throws JSONException
    {
        HUEANData EANData = new HUEANData();
        EANData.setLgmandt(eanDataObj.getString("MANDT"));
        EANData.setLgmatnr(eanDataObj.getString("MATNR"));
        EANData.setLgmeinh(eanDataObj.getString("MEINH"));
        EANData.setLgumrez(eanDataObj.getDouble("UMREZ"));
        EANData.setLgumren(eanDataObj.getDouble("UMREN"));
        EANData.setLgeannr(eanDataObj.getString("EANNR"));
        EANData.setLgean11(eanDataObj.getString("EAN11"));
        EANData.setLgnumtp(eanDataObj.getString("NUMTP"));
        EANData.setLglaeng(eanDataObj.getDouble("LAENG"));
        EANData.setLgbreit(eanDataObj.getDouble("BREIT"));
        EANData.setLghoehe(eanDataObj.getDouble("HOEHE"));
        EANData.setLgmeabm(eanDataObj.getString("MEABM"));
        EANData.setLgvolum(eanDataObj.getDouble("VOLUM"));
        EANData.setLgvoleh(eanDataObj.getString("VOLEH"));
        EANData.setLgbrgew(eanDataObj.getDouble("BRGEW"));
        EANData.setLggewei(eanDataObj.getString("GEWEI"));
        EANData.setLgmesub(eanDataObj.getString("MESUB"));
        EANData.setLgatinn(eanDataObj.getInt("ATINN"));
        EANData.setLgmesrt(eanDataObj.getInt("MESRT"));
        EANData.setLgxfhdw(eanDataObj.getString("XFHDW"));
        EANData.setLgxbeww(eanDataObj.getString("XBEWW"));
        EANData.setLgkzwso(eanDataObj.getString("KZWSO"));
        EANData.setLgmsehi(eanDataObj.getString("MSEHI"));
        EANData.setLgbflmemarm(eanDataObj.getString("BFLME_MARM"));
        EANData.setLggtinvariant(eanDataObj.getString("GTIN_VARIANT"));
        EANData.setLgnestftr(eanDataObj.getDouble("NEST_FTR"));
        EANData.setLgmaxstack(eanDataObj.getInt("MAX_STACK"));
        EANData.setLgtoploadfull(eanDataObj.getDouble("TOP_LOAD_FULL"));
        EANData.setLgtoploadfulluom(eanDataObj.getString("TOP_LOAD_FULL_UOM"));
        EANData.setLgcapause(eanDataObj.getDouble("CAPAUSE"));
        EANData.setLgty2tq(eanDataObj.getString("TY2TQ"));
        EANData.setLgdummyuomincleewps(eanDataObj.getString("DUMMY_UOM_INCL_EEW_PS"));
        EANData.setLgcwmty2tq(eanDataObj.getString("/CWM/TY2TQ"));
        EANData.setLgsttpecncode(eanDataObj.getString("/STTPEC/NCODE"));
        EANData.setLgsttpecncodety(eanDataObj.getString("/STTPEC/NCODE_TY"));
        EANData.setLgsttpecrcode(eanDataObj.getString("/STTPEC/RCODE"));
        EANData.setLgsttpecseruse(eanDataObj.getString("/STTPEC/SERUSE"));
        EANData.setLgsttpecsyncchg(eanDataObj.getString("/STTPEC/SYNCCHG"));
        EANData.setLgsttpecsernomanaged(eanDataObj.getString("/STTPEC/SERNO_MANAGED"));
        EANData.setLgsttpecsernoprovbup(eanDataObj.getString("/STTPEC/SERNO_PROV_BUP"));
        EANData.setLgsttpecuomsync(eanDataObj.getString("/STTPEC/UOM_SYNC"));
        EANData.setLgsttpecsergtin(eanDataObj.getString("/STTPEC/SER_GTIN"));
        EANData.setLgpcbut(eanDataObj.getString("PCBUT"));
        return EANData;
    }
}
