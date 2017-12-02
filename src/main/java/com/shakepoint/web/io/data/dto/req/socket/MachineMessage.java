package com.shakepoint.web.io.data.dto.req.socket;

public class MachineMessage {
    private String type;
    private String machineId;
    private Object message;

    public MachineMessage() {
    }

    public MachineMessage(String type, String machineId, Object message) {
        this.type = type;
        this.machineId = machineId;
        this.message = message;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
