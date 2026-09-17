package com.team.corporate.models;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public Product() {
    }

    public Product(@NotNull String sku,
                   @NotNull String name,
                   @NotNull Category category,
                   @NotNull BigDecimal price) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public Product(@NotNull String sku, @NotNull String name) {
        this.sku = sku;
        this.name = name;
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getSku() {
        return sku;
    }

    public void setSku(@NotNull String sku) {
        this.sku = sku;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public Category getCategory() {
        return category;
    }

    public void setCategory(@NotNull Category category) {
        this.category = category;
    }

    @NotNull
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(@NotNull BigDecimal price) {
        this.price = price;
    }
}
