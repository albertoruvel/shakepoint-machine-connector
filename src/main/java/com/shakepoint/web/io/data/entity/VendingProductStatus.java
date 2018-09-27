package com.shakepoint.web.io.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "VendingProductStatus")
@Table(name = "machine_product")
public class VendingProductStatus {

    @Id
    private String id;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "product_id")
    private String productId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
