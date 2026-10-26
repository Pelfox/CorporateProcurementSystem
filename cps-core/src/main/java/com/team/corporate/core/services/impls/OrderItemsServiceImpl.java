package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.exceptions.OrderItemNotFoundException;
import com.team.corporate.core.exceptions.OrderNotFoundException;
import com.team.corporate.core.exceptions.ProductNotFoundException;
import com.team.corporate.core.repositories.OrderItemsRepository;
import com.team.corporate.core.repositories.OrdersRepository;
import com.team.corporate.core.repositories.ProductsRepository;
import com.team.corporate.core.repositories.Transactions;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderItemsServiceImpl implements OrderItemsService {
    private final OrdersRepository ordersRepository;
    private final Transactions transactions;
    private final OrderStock stock;
    private final ProductsRepository productsRepository;
    private final OrderItemsRepository orderItemsRepository;

    public OrderItemsServiceImpl(@NotNull OrdersRepository ordersRepository,
                                 @NotNull ProductsRepository productsRepository,
                                 @NotNull OrderItemsRepository orderItemsRepository,
                                 @NotNull Transactions transactions, @NotNull OrderStock stock) {
        this.ordersRepository = ordersRepository;
        this.transactions = transactions;
        this.stock = stock;
        this.productsRepository = productsRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    @Override
    @NotNull
    public OrderItem createOrderItemForOrder(@NotNull UUID orderId,
                                             @NotNull UUID productId,
                                             int quantity,
                                             @NotNull BigDecimal purchasePrice) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(productId, "Идентификатор товара");
        EntityValidation.requireNonNull(purchasePrice, "Цена покупки");
        return transactions.execute(() -> {
            Order order = mutableOrder(orderId);
            Product product = productsRepository.getById(productId)
                    .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным идентификатором не найден."));
            OrderItem orderItem = new OrderItem(order, product, quantity, purchasePrice);
            if (OrderStock.reservesStock(order.getStatus())) {
                var requested = stock.quantities(orderId);
                requested.merge(productId, (long) quantity, Long::sum);
                stock.requireAvailable(requested, orderId);
            }
            return orderItemsRepository.add(orderItem);
        });
    }

    @Override
    public void deleteOrderItem(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        transactions.execute(() -> {
            mutableItem(id);
            orderItemsRepository.delete(id);
        });
    }

    @Override
    @NotNull
    public OrderItem updateOrderItem(@NotNull UUID id, @Nullable Integer quantity, @Nullable BigDecimal purchasePrice) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return transactions.execute(() -> {
            OrderItem orderItem = mutableItem(id);
            Order order = orderItem.getOrder();
            if (quantity != null) {
                EntityValidation.requireOrderQuantity(quantity);
                if (OrderStock.reservesStock(order.getStatus())) {
                    var requested = stock.quantities(order.getId());
                    requested.merge(orderItem.getProduct().getId(), (long) quantity - orderItem.getQuantity(), Long::sum);
                    stock.requireAvailable(requested, order.getId());
                }
                orderItem.setQuantity(quantity);
            }
            if (purchasePrice != null) orderItem.setPurchasePrice(purchasePrice);
            return orderItemsRepository.update(orderItem);
        });
    }

    @Override
    @NotNull
    public List<OrderItem> getAllByOrder(@NotNull UUID orderId) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        ordersRepository.getById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным идентификатором не найден."));
        return orderItemsRepository.getByOrderId(orderId);
    }

    @Override
    @NotNull
    public Optional<OrderItem> getOrderItem(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return orderItemsRepository.getById(id);
    }

    @Override
    @NotNull
    public List<OrderItem> getAllByProduct(@NotNull UUID productId) {
        EntityValidation.requireNonNull(productId, "Идентификатор товара");
        productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным идентификатором не найден."));
        return orderItemsRepository.getByProductId(productId);
    }

    @Override
    @NotNull
    public List<OrderItem> getAll() {
        return orderItemsRepository.getAll();
    }

    private Order mutableOrder(UUID id) {
        Order order = ordersRepository.getByIdForUpdate(id)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден."));
        if (order.getStatus() == OrderStatus.DELIVERED)
            throw new IllegalArgumentException("Нельзя изменять доставленный заказ.");
        return order;
    }

    private OrderItem mutableItem(UUID id) {
        OrderItem reference = orderItemsRepository.getById(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Позиция заказа не найдена."));
        mutableOrder(reference.getOrder().getId());
        return orderItemsRepository.getByIdForUpdate(id)
                .orElseThrow(() -> new OrderItemNotFoundException("Позиция заказа не найдена."));
    }

}
