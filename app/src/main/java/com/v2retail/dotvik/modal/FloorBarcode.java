package com.v2retail.dotvik.modal;

import com.google.gson.annotations.SerializedName;

public class FloorBarcode {

    @SerializedName("BARCODE")
    private String barcode;

    @SerializedName("UMREZ")
    private String umrez;

    @SerializedName("WERKS")
    private String werks;

    @SerializedName("MATNR")
    private String matnr;

    @SerializedName("LGPLA")
    private String lgpla;

    @SerializedName("VERME")
    private String verme;

    @SerializedName("FLOOR_BIN")
    private String floorBin;

    @SerializedName("SCAN_QTY")
    private String scanQty;

    @SerializedName("ART_TYPE")
    private String artType;

    @SerializedName("ZSIZE")
    private String artSize;

    // --- newInstance method ---
    public static FloorBarcode newInstance(FloorBarcode source) {
        if (source == null) {
            return null;
        }

        FloorBarcode target = new FloorBarcode();

        target.setBarcode(source.getBarcode());
        target.setUmrez(source.getUmrez());
        target.setWerks(source.getWerks());
        target.setMatnr(source.getMatnr());
        target.setLgpla(source.getLgpla());
        target.setVerme(source.getVerme());
        target.setFloorBin(source.getFloorBin());
        target.setScanQty(source.getScanQty());
        target.setArtType(source.getArtType());
        target.setArtSize(source.getArtSize());

        return target;
    }

    // --- Getters and Setters ---
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getUmrez() {
        return umrez;
    }

    public void setUmrez(String umrez) {
        this.umrez = umrez;
    }

    public String getWerks() {
        return werks;
    }

    public void setWerks(String werks) {
        this.werks = werks;
    }

    public String getMatnr() {
        return matnr;
    }

    public void setMatnr(String matnr) {
        this.matnr = matnr;
    }

    public String getLgpla() {
        return lgpla;
    }

    public void setLgpla(String lgpla) {
        this.lgpla = lgpla;
    }

    public String getVerme() {
        return verme;
    }

    public void setVerme(String verme) {
        this.verme = verme;
    }

    public String getFloorBin() {
        return floorBin;
    }

    public void setFloorBin(String floorBin) {
        this.floorBin = floorBin;
    }

    public String getScanQty() {
        return scanQty;
    }

    public void setScanQty(String scanQty) {
        this.scanQty = scanQty;
    }

    public String getArtType() {
        return artType;
    }

    public void setArtType(String artType) {
        this.artType = artType;
    }

    public String getArtSize() {
        return artSize;
    }

    public void setArtSize(String artSize) {
        this.artSize = artSize;
    }
}