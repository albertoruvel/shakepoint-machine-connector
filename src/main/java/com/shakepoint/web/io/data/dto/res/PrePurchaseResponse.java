package com.shakepoint.web.io.data.dto.res;

public class PrePurchaseResponse {
    private String id;
    private String authorizationDate;

    public PrePurchaseResponse(String id, String authorizationDate) {
        this.id = id;
        this.authorizationDate = authorizationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorizationDate() {
        return authorizationDate;
    }

    public void setAuthorizationDate(String authorizationDate) {
        this.authorizationDate = authorizationDate;
    }
}
