package com.shakepoint.web.io.data.dto.res.socket;

public class PreAuthPurchase {
    private String purchaseId;
    private String productId;
    private String date;
    private Long engineUseTime;
    private Integer slot;
    private Integer mixTime;

    public PreAuthPurchase(String purchaseId, String productId, String date, Long engineUseTime, Integer slot, Integer mixTime) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.date = date;
        this.engineUseTime = engineUseTime;
        this.slot = slot;
        this.mixTime = mixTime;
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

    public Integer getMixTime() {
        return mixTime;
    }

    public void setMixTime(Integer mixTime) {
        this.mixTime = mixTime;
    }
}
