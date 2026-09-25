package com.team.corporate.entities;

import com.team.corporate.utils.EntityValidation;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_product_warehouse", columnNames = {"product_id", "warehouse_id"}))
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public Inventory() {
    }

    public Inventory(@NotNull Product product,
                     @NotNull Warehouse warehouse,
                     int quantity) {
        this.product = EntityValidation.requireNonNull(product, "Товар");
        this.warehouse = EntityValidation.requireNonNull(warehouse, "Склад");
        this.quantity = EntityValidation.requireAtLeast(quantity, 0, "Остаток");
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public Product getProduct() {
        return product;
    }

    public void setProduct(@NotNull Product product) {
        this.product = EntityValidation.requireNonNull(product, "Товар");
    }

    @NotNull
    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(@NotNull Warehouse warehouse) {
        this.warehouse = EntityValidation.requireNonNull(warehouse, "Склад");
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = EntityValidation.requireAtLeast(quantity, 0, "Остаток");
    }
}
