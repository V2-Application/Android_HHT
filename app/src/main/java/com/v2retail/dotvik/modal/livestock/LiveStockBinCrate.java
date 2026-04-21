package com.v2retail.dotvik.modal.livestock;

import com.google.gson.annotations.SerializedName;
import com.v2retail.dotvik.dc.FragmentHuPickingFromMSA;

import java.io.Serializable;

public class LiveStockBinCrate  implements Serializable {

    @SerializedName("PLANT")
    private String plant;
    @SerializedName("BIN")
    private String bin;
    @SerializedName("CRATE")
    private String crate;
    @SerializedName("ST_TAKE_ID")
    private String stockTakeId;
    @SerializedName("HHT_ID")
    private String hhtId;
    @SerializedName("TYP")
    private String typ;
    private boolean picked;

    public static LiveStockBinCrate newInstance(LiveStockBinCrate source){
        if (source == null) {
            return null;
        }

        LiveStockBinCrate target = new LiveStockBinCrate();
        target.setPlant(source.getPlant());
        target.setBin(source.getBin());
        target.setCrate(source.getCrate());
        target.setStockTakeId(source.getStockTakeId());
        target.setHhtId(source.getHhtId());
        target.setTyp(source.getTyp());
        target.setPicked(source.isPicked());

        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LiveStockBinCrate data = (LiveStockBinCrate) obj;
        return bin.equals(data.bin) && crate.equals(data.crate) && plant.equals(data.plant) && stockTakeId.equals(data.stockTakeId) && hhtId.equals(data.hhtId);
    }

    public boolean isPicked() {
        return picked;
    }

    public void setPicked(boolean picked) {
        this.picked = picked;
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

    public String getHhtId() {
        return hhtId;
    }

    public void setHhtId(String hhtId) {
        this.hhtId = hhtId;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }
}
