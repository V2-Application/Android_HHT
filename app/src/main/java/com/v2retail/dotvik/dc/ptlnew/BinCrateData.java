package com.v2retail.dotvik.dc.ptlnew;

import com.google.gson.annotations.SerializedName;

public class BinCrateData {
    @SerializedName("PICKLIST")
    private String picklist;

    @SerializedName("BIN")
    private String bin;

    @SerializedName("FLOOR")
    private String floor;

    @SerializedName("ZSECTION")
    private String section;

    @SerializedName("CRATE")
    private String crate;

    @SerializedName("HUB")
    private String hub;

    private boolean isScanned;

    public String getPicklist() {
        return picklist;
    }

    public void setPicklist(String picklist) {
        this.picklist = picklist;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getCrate() {
        return crate;
    }

    public void setCrate(String crate) {
        this.crate = crate;
    }

    public String getHub() {
        return hub;
    }

    public void setHub(String hub) {
        this.hub = hub;
    }

    public boolean isScanned() {
        return isScanned;
    }

    public void setScanned(boolean scanned) {
        isScanned = scanned;
    }
}
