package com.v2retail.dotvik.modal.putaway;

import android.os.Bundle;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

/**
 * @author Narayanan
 * @version 11.71
 * {@code Author: Narayanan, Revision: 1, Created: 30th Jul 2024, Modified: 30th Jul 2024}
 */
@Keep
public class ETDataStorePutway {
    @SerializedName("EAN11")
    private String ean11;
    @SerializedName("LGNUM")
    private String lgnum;
    @SerializedName("LGORT")
    private String lgort;
    @SerializedName("LGPLA")
    private String lgpla;
    @SerializedName("LGTYP")
    private String lgtyp;
    @SerializedName("MAKTX")
    private String maktx;
    @SerializedName("MATKL")
    private String matkl;
    @SerializedName("MATNR")
    private String matnr;
    @SerializedName("MEINS")
    private String meins;
    @SerializedName("VERME")
    private String verme;
    @SerializedName("VLPLA")
    private String vlpla;
    @SerializedName("VLTYP")
    private String vltyp;
    @SerializedName("WERKS")
    private String werks;
    @SerializedName("WGBEZ")
    private String wgbez;
    @SerializedName("VERME1")
    private String verme1;
    @SerializedName("IROD")
    private String irod;

    public static ETDataStorePutway newInstance(JSONObject objETData,String sloc,String plant,String binno) {
        ETDataStorePutway etData = new Gson().fromJson(objETData.toString(), ETDataStorePutway.class);
        etData.setVerme1(etData.getVerme());
        etData.setVerme("0");
        etData.setVlpla(etData.getLgpla());
        etData.setVltyp(etData.getLgpla());
        etData.setLgort(sloc);
        etData.setWerks(plant);
        etData.setLgpla(binno);
        return etData;
    }

    public String getEan11() {
        return ean11;
    }

    public void setEan11(String ean11) {
        this.ean11 = ean11;
    }

    public String getLgnum() {
        return lgnum;
    }

    public void setLgnum(String lgnum) {
        this.lgnum = lgnum;
    }

    public String getLgort() {
        return lgort;
    }

    public void setLgort(String lgort) {
        this.lgort = lgort;
    }

    public String getLgpla() {
        return lgpla;
    }

    public void setLgpla(String lgpla) {
        this.lgpla = lgpla;
    }

    public String getLgtyp() {
        return lgtyp;
    }

    public void setLgtyp(String lgtyp) {
        this.lgtyp = lgtyp;
    }

    public String getMaktx() {
        return maktx;
    }

    public void setMaktx(String maktx) {
        this.maktx = maktx;
    }

    public String getMatkl() {
        return matkl;
    }

    public void setMatkl(String matkl) {
        this.matkl = matkl;
    }

    public String getMatnr() {
        return matnr;
    }

    public void setMatnr(String matnr) {
        this.matnr = matnr;
    }

    public String getMeins() {
        return meins;
    }

    public void setMeins(String meins) {
        this.meins = meins;
    }

    public String getVerme() {
        return verme;
    }

    public void setVerme(String verme) {
        this.verme = verme;
    }

    public String getVlpla() {
        return vlpla;
    }

    public void setVlpla(String vlpla) {
        this.vlpla = vlpla;
    }

    public String getVltyp() {
        return vltyp;
    }

    public void setVltyp(String vltyp) {
        this.vltyp = vltyp;
    }

    public String getWerks() {
        return werks;
    }

    public void setWerks(String werks) {
        this.werks = werks;
    }

    public String getWgbez() {
        return wgbez;
    }

    public void setWgbez(String wgbez) {
        this.wgbez = wgbez;
    }

    public String getVerme1() {
        return verme1;
    }

    public void setVerme1(String verme1) {
        this.verme1 = verme1;
    }

    public String getIrod() {
        return irod;
    }

    public void setIrod(String irod) {
        this.irod = irod;
    }
}
