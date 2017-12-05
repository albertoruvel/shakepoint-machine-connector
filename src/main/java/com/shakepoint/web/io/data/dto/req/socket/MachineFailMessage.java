package com.shakepoint.web.io.data.dto.req.socket;

public class MachineFailMessage {
    private String failMessage;
    private String date;

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
