package com.shakepoint.web.io.service;

import com.shakepoint.web.io.data.entity.Purchase;

public interface QrCodeService {
    public String createQrCode(Purchase qrCode);
    public void deleteAllQrCodes();
}
