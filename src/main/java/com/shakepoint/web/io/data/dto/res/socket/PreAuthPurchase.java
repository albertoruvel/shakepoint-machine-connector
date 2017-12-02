package com.shakepoint.web.io.data.dto.res.socket;

public class PreAuthPurchase {
    private String id;
    private String productId;
    private double total;
    private String date;

    public PreAuthPurchase(String id, String productId, double total, String date) {
        this.id = id;
        this.productId = productId;
        this.total = total;
        this.date = date;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
