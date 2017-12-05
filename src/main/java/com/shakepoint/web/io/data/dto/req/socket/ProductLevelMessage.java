package com.shakepoint.web.io.data.dto.req.socket;

public class ProductLevelMessage {
    private String productId;
    private String productLevelType;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductLevelType() {
        return productLevelType;
    }

    public void setProductLevelType(String productLevelType) {
        this.productLevelType = productLevelType;
    }
}
