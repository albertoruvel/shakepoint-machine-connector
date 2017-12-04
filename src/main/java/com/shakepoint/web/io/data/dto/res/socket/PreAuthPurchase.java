package com.shakepoint.web.io.data.dto.res.socket;

public class PreAuthPurchase {
    private String purchaseId;
    private String productId;
    private String date;

    public PreAuthPurchase(String purchaseId, String productId, String date) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.date = date;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }
}
