package com.shakepoint.web.io.data.entity;

import javax.persistence.*;
import java.util.UUID;

@Entity(name = "Purchase")
@Table(name = "purchase")
public class Purchase {

    @Id
    private String id;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "purchase_date")
    private String purchaseDate;

    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private PurchaseStatus status;

    @Column(name = "total")
    private Double total;

    @Column(name = "qr_image_url")
    private String qrCodeUrl;

    @Column(name = "control_number")
    private String controlNumber;

    public Purchase() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(String controlNumber) {
        this.controlNumber = controlNumber;
    }
}
