package com.shakepoint.web.io.data.dto.res.socket;

public class QrCodeValidationResult {
    private String qrCodeId;
    private boolean valid;

    public QrCodeValidationResult(String qrCodeId, boolean valid) {
        this.qrCodeId = qrCodeId;
        this.valid = valid;
    }

    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
