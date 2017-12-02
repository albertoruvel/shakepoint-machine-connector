package com.shakepoint.web.io.data.dto.res;

import java.util.List;

public class PrePurchasesResponse {
    private String message;
    private List<PrePurchaseResponse> prePurchases;

    public PrePurchasesResponse(String message, List<PrePurchaseResponse> prePurchases) {
        this.message = message;
        this.prePurchases = prePurchases;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PrePurchaseResponse> getPrePurchases() {
        return prePurchases;
    }

    public void setPrePurchases(List<PrePurchaseResponse> prePurchases) {
        this.prePurchases = prePurchases;
    }
}
