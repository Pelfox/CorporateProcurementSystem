package com.team.corporate.entities;

import com.team.corporate.utils.EntityValidation;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    public OrderItem() {
    }

    public OrderItem(@NotNull Order order,
                     @NotNull Product product,
                     int quantity,
                     @NotNull BigDecimal purchasePrice) {
        this.order = order;
        this.product = product;
        this.quantity = EntityValidation.requireAtLeast(quantity, 1, "quantity");
        this.purchasePrice = EntityValidation.requireMoney(purchasePrice, "purchasePrice");
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public Order getOrder() {
        return order;
    }

    public void setOrder(@NotNull Order order) {
        this.order = order;
    }

    @NotNull
    public Product getProduct() {
        return product;
    }

    public void setProduct(@NotNull Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = EntityValidation.requireAtLeast(quantity, 1, "quantity");
    }

    @NotNull
    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(@NotNull BigDecimal purchasePrice) {
        this.purchasePrice = EntityValidation.requireMoney(purchasePrice, "purchasePrice");
    }
}
