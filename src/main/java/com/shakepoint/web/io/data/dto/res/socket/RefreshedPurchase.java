package com.shakepoint.web.io.data.dto.res.socket;

public final class RefreshedPurchase extends PreAuthPurchase {

    //the purchase from which this one was created...
    private String refreshedPurchase;

    public RefreshedPurchase(String purchaseId, String productId, String date, Long engineUseTime, Integer slot, String refreshedPurchase) {
        super(purchaseId, productId, date, engineUseTime, slot);
    }

    public String getRefreshedPurchase() {
        return refreshedPurchase;
    }

    public void setRefreshedPurchase(String refreshedPurchase) {
        this.refreshedPurchase = refreshedPurchase;
    }
}
