package com.shakepoint.web.io.data.entity;

public enum PurchaseStatus {
    PRE_AUTH(199),
    AUTHORIZED(69),
    CASHED(999);
    int value;

    PurchaseStatus(int value) {
        this.value = value;
    }

    public int getValue(){
        return value;
    }
}
