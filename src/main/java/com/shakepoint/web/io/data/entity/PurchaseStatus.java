package com.shakepoint.web.io.data.entity;

public enum PurchaseStatus {
    PAID(1), PRE_AUTH(666), CASHED(69);
    int value;

    PurchaseStatus(int value) {
        this.value = value;
    }
}
