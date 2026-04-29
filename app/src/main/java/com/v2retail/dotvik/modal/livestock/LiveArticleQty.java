package com.v2retail.dotvik.modal.livestock;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class LiveArticleQty  implements Serializable {

    @SerializedName("ARTICLE")
    private String article;
    @SerializedName("MATNR")
    private String matnr;
    @SerializedName("BARCODE")
    private String barcode;
    @SerializedName("QUANTITY")
    private String qty;

    public String getArticle() { return article; }
    public void setArticle(String article) { this.article = article; }
    public String getMatnr() { return matnr; }
    public void setMatnr(String matnr) { this.matnr = matnr; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getQty() { return qty; }
    public void setQty(String qty) { this.qty = qty; }
}
