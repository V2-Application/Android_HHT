package com.v2retail.dotvik.modal.irod;

import com.google.gson.annotations.SerializedName;

public class IRODMC {
    @SerializedName("MANDT")
    private String mandt;
    @SerializedName("WERKS")
    private String werks;
    @SerializedName("MAJ_CAT_CD")
    private String lgcat;
    @SerializedName("TYPE")
    private String lgtype;
    @SerializedName("IROD")
    private String irod;

    public IRODMC(String werks, String lgcat, String lgtype, String irod){
        this.werks = werks;
        this.lgcat = lgcat;
        this.lgtype = lgtype;
        this.irod = irod;
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

    public String getLgcat() {
        return lgcat;
    }

    public void setLgcat(String lgcat) {
        this.lgcat = lgcat;
    }

    public String getLgtype() {
        return lgtype;
    }

    public void setLgtype(String lgtype) {
        this.lgtype = lgtype;
    }

    public String getIrod() {
        return irod;
    }

    public void setIrod(String irod) {
        this.irod = irod;
    }
}
