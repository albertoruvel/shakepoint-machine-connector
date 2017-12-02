package com.shakepoint.web.io.data.dto.req.socket;

public enum MachineMessageType {
    QR_CODE_DISPENSED("qr_code_dispensed"),
    TURN_ON("turned_on"),
    QR_CODE_EXCHANGE("qr_code_exchange"),
    NOT_VALID("");

    String value;
    MachineMessageType(String value){
        this.value = value;
    }

    public static MachineMessageType get(String value){
        if (value.equals(TURN_ON.value)){
            return TURN_ON;
        } else if (value.equals(QR_CODE_EXCHANGE.value)){
            return QR_CODE_EXCHANGE;
        }else return NOT_VALID;
    }
}
