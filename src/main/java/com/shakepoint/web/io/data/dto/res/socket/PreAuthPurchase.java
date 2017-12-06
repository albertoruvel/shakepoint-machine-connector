package com.shakepoint.web.io.data.dto.res.socket;

public class PreAuthPurchase {
    private String purchaseId;
    private String productId;
    private String date;
    private Integer engineUseTime;

    public PreAuthPurchase(String purchaseId, String productId, String date, Integer engineUseTime) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.date = date;
        this.engineUseTime = engineUseTime;
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

    public Integer getEngineUseTime() {
        return engineUseTime;
    }

    public void setEngineUseTime(Integer engineUseTime) {
        this.engineUseTime = engineUseTime;
    }
}
