package com.shakepoint.web.io.data.dto.req.socket;

public enum MachineMessageType {
    QR_CODE_DISPENSED("qr_code_dispensed"),
    TURN_ON("turned_on"),
    NOT_VALID("");

    String value;
    MachineMessageType(String value){
        this.value = value;
    }

    public static MachineMessageType get(String value){
        if (value.equals(TURN_ON.value)){
            return TURN_ON;
        }else return NOT_VALID;
    }
}
