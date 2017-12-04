package com.shakepoint.web.io.data.entity;

import javax.persistence.*;
import java.util.List;

@Entity(name = "Machine")
@Table(name = "machine")
public class Machine {

    @Id
    private String id;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "machine_product",
            joinColumns = @JoinColumn(name = "machine_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products;

    public Machine() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
