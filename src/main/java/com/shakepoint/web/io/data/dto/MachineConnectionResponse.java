package com.shakepoint.web.io.data.dto;

public class MachineConnectionResponse {
    private int port;
    private String connectionId;
    private String message;

    public MachineConnectionResponse(int port, String connectionId, String message) {
        this.port = port;
        this.connectionId = connectionId;
        this.message = message;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
