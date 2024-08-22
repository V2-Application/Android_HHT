package com.v2retail.dotvik.modal.irod;

import com.google.gson.annotations.SerializedName;

public class EXData{
    @SerializedName("MANDT")
    private String mandt;
    @SerializedName("WERKS")
    private String werks;
    @SerializedName("LGNUM")
    private String lgnum;
    @SerializedName("LGTYP")
    private String lgtyp;
    @SerializedName("IROD")
    private String irod;
    @SerializedName("LGPLA")
    private String lgpla;

    public EXData(String werks, String lgnum, String lgtyp, String irod, String lgpla){
        this.werks = werks;
        this.lgnum = lgnum;
        this.lgtyp = lgtyp;
        this.irod = irod;
        this.lgpla = lgpla;
    }

    public String getMandt() {
        return mandt;
    }

    public void setMandt(String mandt) {
        this.mandt = mandt;
    }

    public String getWerks() {
        return werks;
    }

    public void setWerks(String werks) {
        this.werks = werks;
    }

    public String getLgnum() {
        return lgnum;
    }

    public void setLgnum(String lgnum) {
        this.lgnum = lgnum;
    }

    public String getLgtyp() {
        return lgtyp;
    }

    public void setLgtyp(String lgtyp) {
        this.lgtyp = lgtyp;
    }

    public String getIrod() {
        return irod;
    }

    public void setIrod(String irod) {
        this.irod = irod;
    }

    public String getLgpla() {
        return lgpla;
    }

    public void setLgpla(String lgpla) {
        this.lgpla = lgpla;
    }
}
