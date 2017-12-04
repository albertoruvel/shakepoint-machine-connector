package com.shakepoint.web.io.data.dto.req.socket;

import java.util.List;

public class MachineReconnectMessage {
    private List<String> purchases;

    public List<String> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<String> purchases) {
        this.purchases = purchases;
    }
}
