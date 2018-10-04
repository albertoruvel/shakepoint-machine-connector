package com.shakepoint.web.io.data.dto.res.socket;

public class PreAuthPurchase {
    private String purchaseId;
    private String productId;
    private String date;
    private Long engineUseTime;
    private Integer slot;

    public PreAuthPurchase(String purchaseId, String productId, String date, Long engineUseTime, Integer slot) {
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

    public Long getEngineUseTime() {
        return engineUseTime;
    }

    public void setEngineUseTime(Long engineUseTime) {
        this.engineUseTime = engineUseTime;
    }

    public Integer getSlot() {
        return slot;
    }

    public void setSlot(Integer slot) {
        this.slot = slot;
    }

    @Override
    public String toString() {
        return "PreAuthPurchase{" +
                "purchaseId='" + purchaseId + '\'' +
                ", productId='" + productId + '\'' +
                ", date='" + date + '\'' +
                ", engineUseTime=" + engineUseTime +
                ", slot=" + slot +
                '}';
    }
}
