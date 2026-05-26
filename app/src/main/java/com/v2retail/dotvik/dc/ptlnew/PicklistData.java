package com.v2retail.dotvik.dc.ptlnew;

import com.google.gson.annotations.SerializedName;

public class PicklistData {

    @SerializedName("PICKLIST")
    private String picklist;

    @SerializedName("MSA_CRATE")
    private String msaCrate;

    @SerializedName("EBELN")
    private String ebeln;

    @SerializedName("QUANTITY")
    private String quantity;

    @SerializedName("BIN")
    private String bin;

    @SerializedName("ITEMNO")
    private String itemNo;

    @SerializedName("ZONE")
    private String zone;

    @SerializedName("TANUM")
    private String tanum;

    @SerializedName("EBELP")
    private String ebelp;

    @SerializedName("ETYPE")
    private String eType;

    @SerializedName("PALATE")
    private String palate;

    @SerializedName("HU")
    private String hu;

    @SerializedName("WAVE")
    private String wave;

    @SerializedName("ARTICLE")
    private String article;

    @SerializedName("CRATE")
    private String crate;

    @SerializedName("STORE")
    private String store;

    @SerializedName("FLOOR")
    private String floor;

    @SerializedName("MATKL")
    private String matkl;

    @SerializedName("DIVISION")
    private String division;

    @SerializedName("PLANT")
    private String plant;

    @SerializedName("TAG")
    private String tag;

    // v12.115 (2026-05-08): Added to match ZWM_PTL_MSA_CRATE_ST fields 20 + 24.
    // ZWM_PTL_ZONE_HU_VALIDATE_V3 IT_DATA row requires ZONE_STATION (zone-station code) and HUB (plant).
    @SerializedName("ZONE_STATION")
    private String zoneStation;

    @SerializedName("HUB")
    private String hub;

    private int sqty;
    @SerializedName("SCAN_QTY")
    private String scanQty;

    private boolean shortScan;
    private boolean confirmShortScan;

    public PicklistData() {
        // No-arg constructor
    }

    // Getters and setters
    public String getPicklist() { return picklist; }
    public void setPicklist(String picklist) { this.picklist = picklist; }

    public String getScanQty() {
        return scanQty;
    }

    public void setScanQty(String scanQty) {
        this.scanQty = scanQty;
    }

    public String getMsaCrate() { return msaCrate; }
    public void setMsaCrate(String msaCrate) { this.msaCrate = msaCrate; }

    public String getEbeln() { return ebeln; }
    public void setEbeln(String ebeln) { this.ebeln = ebeln; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getBin() { return bin; }
    public void setBin(String bin) { this.bin = bin; }

    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getTanum() { return tanum; }
    public void setTanum(String tanum) { this.tanum = tanum; }

    public String getEbelp() { return ebelp; }
    public void setEbelp(String ebelp) { this.ebelp = ebelp; }

    public String getEType() { return eType; }
    public void setEType(String eType) { this.eType = eType; }

    public String getPalate() { return palate; }
    public void setPalate(String palate) { this.palate = palate; }

    public String getHu() { return hu; }
    public void setHu(String hu) { this.hu = hu; }

    public String getWave() { return wave; }
    public void setWave(String wave) { this.wave = wave; }

    public String getArticle() { return article; }
    public void setArticle(String article) { this.article = article; }

    public String getCrate() { return crate; }
    public void setCrate(String crate) { this.crate = crate; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getMatkl() { return matkl; }
    public void setMatkl(String matkl) { this.matkl = matkl; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public String getPlant() { return plant; }
    public void setPlant(String plant) { this.plant = plant; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getZoneStation() { return zoneStation; }
    public void setZoneStation(String zoneStation) { this.zoneStation = zoneStation; }

    public String getHub() { return hub; }
    public void setHub(String hub) { this.hub = hub; }

    public int getSqty() {
        return sqty;
    }

    public void setSqty(int sqty) {
        this.sqty = sqty;
    }

    public boolean isConfirmShortScan() {
        return confirmShortScan;
    }

    public void setConfirmShortScan(boolean confirmShortScan) {
        this.confirmShortScan = confirmShortScan;
    }

    // Static method to create a new instance from another instance
    public static PicklistData newInstance(PicklistData source) {
        if (source == null) {
            return null;
        }
        PicklistData copy = new PicklistData();
        copy.setPicklist(source.getPicklist());
        copy.setMsaCrate(source.getMsaCrate());
        copy.setEbeln(source.getEbeln());
        copy.setQuantity(source.getQuantity());
        copy.setBin(source.getBin());
        copy.setItemNo(source.getItemNo());
        copy.setZone(source.getZone());
        copy.setTanum(source.getTanum());
        copy.setEbelp(source.getEbelp());
        copy.setEType(source.getEType());
        copy.setPalate(source.getPalate());
        copy.setHu(source.getHu());
        copy.setWave(source.getWave());
        copy.setArticle(source.getArticle());
        copy.setCrate(source.getCrate());
        copy.setStore(source.getStore());
        copy.setFloor(source.getFloor());
        copy.setMatkl(source.getMatkl());
        copy.setDivision(source.getDivision());
        copy.setPlant(source.getPlant());
        copy.setTag(source.getTag());
        copy.setZoneStation(source.getZoneStation());
        copy.setHub(source.getHub());
        copy.setScanQty(source.getScanQty());
        copy.setSqty(0);
        copy.setShortScan(false);
        copy.setConfirmShortScan(false);
        return copy;
    }
    public boolean isShortScan() {
        return shortScan;
    }

    public void setShortScan(boolean shortScan) {
        this.shortScan = shortScan;
    }

    @Override
    public String toString() {
        return "PicklistData{" +
                "picklist='" + picklist + '\'' +
                ", msaCrate='" + msaCrate + '\'' +
                ", ebeln='" + ebeln + '\'' +
                ", quantity='" + quantity + '\'' +
                ", bin='" + bin + '\'' +
                ", itemNo='" + itemNo + '\'' +
                ", zone='" + zone + '\'' +
                ", tanum='" + tanum + '\'' +
                ", ebelp='" + ebelp + '\'' +
                ", eType='" + eType + '\'' +
                ", palate='" + palate + '\'' +
                ", hu='" + hu + '\'' +
                ", wave='" + wave + '\'' +
                ", article='" + article + '\'' +
                ", crate='" + crate + '\'' +
                ", store='" + store + '\'' +
                ", floor='" + floor + '\'' +
                ", matkl='" + matkl + '\'' +
                ", division='" + division + '\'' +
                ", plant='" + plant + '\'' +
                ", tag='" + tag + '\'' +
                ", zoneStation='" + zoneStation + '\'' +
                ", hub='" + hub + '\'' +
                '}';
    }
}
