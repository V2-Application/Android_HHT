package com.v2retail.dotvik.modal.irod;

import com.google.gson.annotations.SerializedName;

public class IRODDeTag {
    @SerializedName("MANDT")
    private String mandt;
    @SerializedName("WERKS")
    private String werks;
    @SerializedName("MAJ_CAT_CD")
    private String majcat;
    @SerializedName("TYPE")
    private String type;
    @SerializedName("IROD")
    private String irod;

    public IRODDeTag(String werks, String irod){
        this.werks = werks;
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

    public String getMajcat() {
        return majcat;
    }

    public void setMajcat(String majcat) {
        this.majcat = majcat;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIrod() {
        return irod;
    }

    public void setIrod(String irod) {
        this.irod = irod;
    }
}
