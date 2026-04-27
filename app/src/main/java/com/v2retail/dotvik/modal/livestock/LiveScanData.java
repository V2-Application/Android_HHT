package com.v2retail.dotvik.modal.livestock;

import com.google.gson.annotations.SerializedName;
import com.v2retail.util.Util;

import java.io.Serializable;

public class LiveScanData  implements Serializable {

    @SerializedName("MATERIAL")
    private String material;
    @SerializedName("PLANT")
    private String plant;
    @SerializedName("BIN")
    private String bin;
    @SerializedName("CRATE")
    private String crate;
    @SerializedName("ST_TAKE_ID")
    private String stockTakeId;
    @SerializedName("SCAN_QTY")
    private String scanQty;

    public static LiveScanData copyProperties(LiveStockBinCrate binData){
        if(binData == null){
            return null;
        }

        LiveScanData target = new LiveScanData();
        target.setBin(binData.getBin());
        target.setPlant(binData.getPlant());
        target.setStockTakeId(binData.getStockTakeId());
        // Crate from pick list row — ZWM_GET_STOCK_BIN often omits ST_TAKE_ID but
        // still returns BIN/CRATE/PLANT; crate must reach ET_SAVE for SAP posting.
        if (binData.getCrate() != null) {
            target.setCrate(binData.getCrate());
        }
        return target;
    }

    public static void updateScanQty(LiveScanData current, String articleQty){
        double scanQty = Util.convertStringToDouble(current.getScanQty());
        double artQty = Util.convertStringToDouble(articleQty);
        current.setScanQty(Util.formatDouble(scanQty + artQty));
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getPlant() {
        return plant;
    }

    public void setPlant(String plant) {
        this.plant = plant;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getCrate() {
        return crate;
    }

    public void setCrate(String crate) {
        this.crate = crate;
    }

    public String getStockTakeId() {
        return stockTakeId;
    }

    public void setStockTakeId(String stockTakeId) {
        this.stockTakeId = stockTakeId;
    }

    public String getScanQty() {
        return scanQty;
    }

    public void setScanQty(String scanQty) {
        this.scanQty = scanQty;
    }
}
