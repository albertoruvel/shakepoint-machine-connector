package com.shakepoint.web.io.data.dto;

public class MachineConnectionStatusResponse {
    private boolean active;
    private int port;

    public MachineConnectionStatusResponse(boolean active, int port) {
        this.active = active;
        this.port = port;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
