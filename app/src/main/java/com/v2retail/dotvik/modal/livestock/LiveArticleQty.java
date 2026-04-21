package com.v2retail.dotvik.modal.livestock;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class LiveArticleQty  implements Serializable {

    @SerializedName(value = "ARTICLE", alternate = {"MATERIAL"})
    private String article;
    @SerializedName("BARCODE")
    private String barcode;
    @SerializedName(value = "QUANTITY", alternate = {"SCAN_QTY", "QTY"})
    private String qty;

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }
}
