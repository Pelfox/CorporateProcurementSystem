package com.team.corporate.services.impls;

import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderItem;
import com.team.corporate.entities.Product;
import com.team.corporate.exceptions.OrderItemNotFoundException;
import com.team.corporate.exceptions.OrderNotFoundException;
import com.team.corporate.exceptions.ProductNotFoundException;
import com.team.corporate.repositories.OrderItemsRepository;
import com.team.corporate.repositories.OrdersRepository;
import com.team.corporate.repositories.ProductsRepository;
import com.team.corporate.services.OrderItemsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderItemsServiceImpl implements OrderItemsService {
    private final OrdersRepository ordersRepository;
    private final ProductsRepository productsRepository;
    private final OrderItemsRepository orderItemsRepository;

    public OrderItemsServiceImpl(@NotNull OrdersRepository ordersRepository,
                                 @NotNull ProductsRepository productsRepository,
                                 @NotNull OrderItemsRepository orderItemsRepository) {
        this.ordersRepository = ordersRepository;
        this.productsRepository = productsRepository;
        this.orderItemsRepository = orderItemsRepository;
    }


    @Override
    public @NotNull OrderItem createOrderItemForOrder(@NotNull UUID orderId,
                                                      @NotNull UUID productId,
                                                      int quantity,
                                                      @NotNull BigDecimal purchasePrice) {
        Order order = ordersRepository.getById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным ID не найден."));
        Product product = productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным ID не найден."));
        OrderItem orderItem = new OrderItem(order, product, quantity, purchasePrice);
        return orderItemsRepository.add(orderItem);
    }

    @Override
    public void deleteOrderItem(@NotNull UUID id) {
        orderItemsRepository.getById(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Позиция заказа с указанным ID не найдена."));
        orderItemsRepository.delete(id);
    }

    @Override
    public @NotNull OrderItem updateOrderItem(@NotNull UUID id, @Nullable Integer quantity, @Nullable BigDecimal purchasePrice) {
        OrderItem orderItem = orderItemsRepository.getById(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Позиция заказа с указанным ID не найдена."));
        if (purchasePrice == null && quantity == null) {
            return orderItem;
        }
        if (quantity != null) {
            orderItem.setQuantity(quantity);
        }
        if (purchasePrice != null) {
            orderItem.setPurchasePrice(purchasePrice);
        }
        return orderItemsRepository.update(orderItem);
    }

    @Override
    public @NotNull List<OrderItem> getAllByOrder(@NotNull UUID orderId) {
        ordersRepository.getById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным ID не найден."));
        return orderItemsRepository.getByOrderId(orderId);
    }

    @Override
    public @NotNull Optional<OrderItem> getOrderItem(@NotNull UUID id) {
        return orderItemsRepository.getById(id);
    }

    @Override
    public @NotNull List<OrderItem> getAllByProduct(@NotNull UUID productId) {
        productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным ID не найден."));
        return orderItemsRepository.getByProductId(productId);
    }

    @Override
    public @NotNull List<OrderItem> getAll() {
        return orderItemsRepository.getAll();
    }
}
