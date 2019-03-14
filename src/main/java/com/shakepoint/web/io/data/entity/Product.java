package com.shakepoint.web.io.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "Product")
@Table(name = "product")
public class Product {

    @Id
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "mix_time")
    private Integer mixTime;

    @Column(name = "price")
    private Double price;

    @Column(name = "description")
    private String description;

    @Column(name = "engine_use_time")
    private Integer engineUseTime;

    @Column(name = "nutritional_data_url")
    private String nutritionalDataUrl;

    public Product() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEngineUseTime() {
        return engineUseTime;
    }

    public void setEngineUseTime(Integer engineUseTime) {
        this.engineUseTime = engineUseTime;
    }

    public String getNutritionalDataUrl() {
        return nutritionalDataUrl;
    }

    public void setNutritionalDataUrl(String nutritionalDataUrl) {
        this.nutritionalDataUrl = nutritionalDataUrl;
    }

    public Integer getMixTime() {
        return mixTime;
    }

    public void setMixTime(Integer mixTime) {
        this.mixTime = mixTime;
    }
}
