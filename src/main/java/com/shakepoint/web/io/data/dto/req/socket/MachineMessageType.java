package com.shakepoint.web.io.data.dto.req.socket;

public enum MachineMessageType {
    RECONNECTED("reconnected"),
    TURN_ON("turned_on"),
    QR_CODE_EXCHANGE("qr_code_exchange"),
    PRODUCT_LEVEL_ALERT("product_level_alert"),
    MACHINE_FAIL("machine_fail"),
    PRODUCTS_REPLACEMENT("replacement"),
    PRODUCT_RECAP("product_recap"),
    PRODUCT_REPLACEMENT("replacement"),
    NOT_VALID("");

    String value;

    MachineMessageType(String value) {
        this.value = value;
    }

    public static MachineMessageType get(String value) {
        if (value.equals(TURN_ON.value)) {
            return TURN_ON;
        } else if (value.equals(QR_CODE_EXCHANGE.value)) {
            return QR_CODE_EXCHANGE;
        } else if (value.equals(RECONNECTED.value)) {
            return RECONNECTED;
        } else if (value.equals(PRODUCT_LEVEL_ALERT.value)) {
            return PRODUCT_LEVEL_ALERT;
        } else if (value.equals(MACHINE_FAIL.value)) {
            return MACHINE_FAIL;
        } else if (value.equals(PRODUCT_RECAP.value)) {
            return PRODUCT_RECAP;
<<<<<<< HEAD
        } else if (value.equals(PRODUCT_REPLACEMENT.value)) {
            return PRODUCT_REPLACEMENT;
        } else return NOT_VALID;
=======
        } else if (value.equals(PRODUCTS_REPLACEMENT.value)) {
            return PRODUCTS_REPLACEMENT;
        }
        else return NOT_VALID;
>>>>>>> 09823c258fda24693b559321305ff73b8628a2da
    }
}
