package com.shakepoint.web.io.service;

import com.shakepoint.web.io.data.entity.Purchase;

public interface AWSS3Service {
    public void createProductNutritionalData(String productId);
    public String createQrCode(Purchase qrCode);
    public void deleteAllQrCodes();
}
