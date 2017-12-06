package com.shakepoint.web.io.data.dto.res.socket;

public class ProductRecap {
    private int slot;
    private String productId;

    public ProductRecap(String productId, int slot) {
        this.productId = productId;
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
