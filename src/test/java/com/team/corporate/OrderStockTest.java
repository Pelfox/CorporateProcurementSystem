package com.team.corporate;

import com.team.corporate.entities.*;
import com.team.corporate.services.OrdersService.Line;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderStockTest extends BaseServiceTest {
    private User buyer, manager;
    private Product product;
    private int warehouseNumber;

    @BeforeEach
    void prepare() {
        buyer = usersService.createUser("buyer", UserRole.USER);
        manager = usersService.createUser("manager", UserRole.MANAGER);
        var category = categoriesService.createCategory("Товары");
        product = productsService.createProduct("Товар", "SKU-001", category.getId(), BigDecimal.TEN);
    }

    private Inventory stock(int quantity) {
        var warehouse = warehousesService.createWarehouse("Склад " + ++warehouseNumber, null);
        return inventoriesService.placeProductInWarehouse(product.getId(), warehouse.getId(), quantity);
    }

    private UUID order(int quantity) {
        return ordersService.createOrder(buyer.getId(), List.of(new Line(product.getId(), quantity)), null);
    }

    private long available() {
        return stock.getAvailableQuantities(null).getOrDefault(product.getId(), 0L);
    }

    private long physical() {
        return inventoriesService.getAllByProduct(product.getId()).stream().mapToLong(Inventory::getQuantity).sum();
    }

    @Test
    void rejectsMissingAndZeroStockWithoutSavingOrder() {
        assertThrows(IllegalArgumentException.class, () -> order(1));
        stock(0);
        assertThrows(IllegalArgumentException.class, () -> order(1));
        assertTrue(ordersService.getAll().isEmpty());
        assertTrue(orderItemsService.getAll().isEmpty());
    }

    @Test
    void sumsWarehousesAndAllowsExactAvailableQuantity() {
        stock(2);
        stock(3);
        UUID id = order(5);
        assertEquals(5, orderItemsService.getAllByOrder(id).getFirst().getQuantity());
        assertEquals(0, available());
        assertEquals(5, physical());
        assertThrows(IllegalArgumentException.class, () -> order(1));
    }

    @Test
    void rejectsExcessAndAggregatesDuplicateCartLines() {
        stock(5);
        assertThrows(IllegalArgumentException.class, () -> ordersService.createOrder(buyer.getId(),
                List.of(new Line(product.getId(), 3), new Line(product.getId(), 3)), null));
        assertTrue(ordersService.getAll().isEmpty());
        UUID id = ordersService.createOrder(buyer.getId(), List.of(new Line(product.getId(), 2), new Line(product.getId(), 3)), null);
        assertEquals(1, orderItemsService.getAllByOrder(id).size());
        assertEquals(5, orderItemsService.getAllByOrder(id).getFirst().getQuantity());
    }

    @Test
    void accountsForOtherCreatedAndApprovedOrders() {
        stock(5);
        UUID first = order(3);
        ordersService.changeStatus(manager.getId(), first, OrderStatus.APPROVED);
        assertEquals(2, available());
        assertThrows(IllegalArgumentException.class, () -> order(3));
        order(2);
        assertEquals(0, available());
    }

    @Test
    void editingExcludesOwnReservationAndKeepsPreviousOrderOnFailure() {
        stock(10);
        UUID id = order(4);
        ordersService.saveOrder(buyer.getId(), id, List.of(new Line(product.getId(), 8)), "updated");
        assertEquals(2, available());
        assertThrows(IllegalArgumentException.class, () -> ordersService.saveOrder(buyer.getId(), id,
                List.of(new Line(product.getId(), 11)), "invalid"));
        assertEquals(8, orderItemsService.getAllByOrder(id).getFirst().getQuantity());
        assertEquals("updated", ordersService.getOrder(id).orElseThrow().getNotes());
    }

    @Test
    void addItemAndUpdateQuantityCannotExceedAvailableStock() {
        stock(5);
        UUID id = order(3);
        UUID item = orderItemsService.getAllByOrder(id).getFirst().getId();
        assertThrows(IllegalArgumentException.class, () -> ordersService.addItem(buyer.getId(), id, new Line(product.getId(), 3)));
        assertThrows(IllegalArgumentException.class, () -> ordersService.updateQuantity(buyer.getId(), id, item, 6));
        assertEquals(3, orderItemsService.getOrderItem(item).orElseThrow().getQuantity());
        ordersService.addItem(buyer.getId(), id, new Line(product.getId(), 2));
        assertEquals(0, available());
        ordersService.saveOrder(buyer.getId(), id, List.of(new Line(product.getId(), 4)), null);
        assertEquals(1, orderItemsService.getAllByOrder(id).size());
        assertEquals(1, available());
    }

    @Test
    void failedMultiProductOrderRollsBackAllLinesAndReservations() {
        stock(5);
        var other = productsService.createProduct("Нет на складе", "SKU-002", product.getCategory().getId(), BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> ordersService.createOrder(buyer.getId(),
                List.of(new Line(product.getId(), 2), new Line(other.getId(), 1)), null));
        assertTrue(ordersService.getAll().isEmpty());
        assertTrue(orderItemsService.getAll().isEmpty());
        assertEquals(5, available());
    }

    @Test
    void cancellationAndDeletionReleaseReservationsAndReopeningChecksStockAgain() {
        stock(5);
        UUID cancelled = order(5);
        ordersService.changeStatus(manager.getId(), cancelled, OrderStatus.CANCELLED);
        assertEquals(5, available());
        UUID replacement = order(5);
        assertThrows(IllegalArgumentException.class, () -> ordersService.changeStatus(manager.getId(), cancelled, OrderStatus.APPROVED));
        assertEquals(OrderStatus.CANCELLED, ordersService.getOrder(cancelled).orElseThrow().getStatus());
        ordersService.deleteOrder(replacement);
        assertEquals(5, available());
        ordersService.changeStatus(manager.getId(), cancelled, OrderStatus.CREATED);
        assertEquals(0, available());
    }

    @Test
    void deliveryDeductsPhysicalStockOnceAndPreservesOtherReservations() {
        stock(2);
        stock(5);
        UUID first = order(4);
        UUID second = order(3);
        ordersService.changeStatus(manager.getId(), first, OrderStatus.DELIVERED);
        assertEquals(3, physical());
        assertEquals(0, available());
        ordersService.changeStatus(manager.getId(), first, OrderStatus.DELIVERED);
        assertEquals(3, physical());
        ordersService.deleteOrder(first);
        assertEquals(3, physical());
        ordersService.changeStatus(manager.getId(), second, OrderStatus.CANCELLED);
        assertEquals(3, available());
    }

    @Test
    void cannotReduceOrDeleteStockAlreadyReservedByOrders() {
        Inventory inventory = stock(5);
        order(4);
        assertThrows(IllegalArgumentException.class, () -> inventoriesService.updateInventory(inventory.getId(), 3));
        assertThrows(IllegalArgumentException.class, () -> inventoriesService.deleteInventory(inventory.getId()));
        assertEquals(5, physical());
        inventoriesService.updateInventory(inventory.getId(), 4);
        assertEquals(0, available());
    }

    @Test
    void directOrderItemServiceCannotBypassStockValidation() {
        var empty = ordersService.createOrder(buyer.getId(), OrderStatus.CREATED, null);
        assertThrows(IllegalArgumentException.class, () -> orderItemsService.createOrderItemForOrder(
                empty.getId(), product.getId(), 1, BigDecimal.TEN));
        stock(5);
        var item = orderItemsService.createOrderItemForOrder(empty.getId(), product.getId(), 4, BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> orderItemsService.updateOrderItem(item.getId(), 6, null));
        assertEquals(4, orderItemsService.getOrderItem(item.getId()).orElseThrow().getQuantity());
        orderItemsService.deleteOrderItem(item.getId());
        assertEquals(5, available());
    }

    @Test
    void rawStatusUpdateAlsoMaintainsReservationsAndDelivery() {
        stock(5);
        UUID id = order(5);
        ordersService.updateOrder(id, OrderStatus.CANCELLED, null);
        assertEquals(5, available());
        ordersService.updateOrder(id, OrderStatus.CREATED, null);
        assertEquals(0, available());
        ordersService.updateOrder(id, OrderStatus.DELIVERED, null);
        assertEquals(0, physical());
        assertThrows(IllegalArgumentException.class, () -> ordersService.updateOrder(id, OrderStatus.CANCELLED, null));
    }

    @Test
    void deliveredOrderCannotBeChangedToCreateStock() {
        stock(5);
        UUID id = order(5);
        UUID item = orderItemsService.getAllByOrder(id).getFirst().getId();
        ordersService.changeStatus(manager.getId(), id, OrderStatus.DELIVERED);
        assertThrows(IllegalArgumentException.class, () -> ordersService.changeStatus(manager.getId(), id, OrderStatus.CANCELLED));
        assertThrows(IllegalArgumentException.class, () -> orderItemsService.updateOrderItem(item, 1, null));
        assertThrows(IllegalArgumentException.class, () -> orderItemsService.deleteOrderItem(item));
        assertEquals(0, physical());
    }

    @Test
    void concurrentOrdersCannotReserveTheSameLastUnit() throws Exception {
        stock(1);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                start.await();
                try {
                    order(1);
                    return true;
                } catch (IllegalArgumentException expected) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);
            start.countDown();
            int succeeded = (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, succeeded);
        }
        assertEquals(1, ordersService.getAll().size());
        assertEquals(0, available());
    }
}
