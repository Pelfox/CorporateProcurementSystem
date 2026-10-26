package com.team.corporate.test.core;

import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.services.StatisticsService;
import com.team.corporate.core.services.SystemStatistics;
import com.team.corporate.core.services.impls.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest extends BaseServiceTest {
    private StatisticsService statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = new StatisticsServiceImpl(usersRepository, productsRepository, categoriesRepository,
                warehousesRepository, ordersRepository, orderItemsRepository, inventoriesRepository, transactions);
    }

    @Test
    void emptyDatabaseHasZeroStatistics() {
        assertEquals(new SystemStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO), statistics.getStatistics());
        assertEquals(0, statistics.getStatistics().activeOrders());
    }

    @Test
    void countsAllUsersAndStatusesAndSumsStockAcrossWarehouses() {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        var buyer = usersService.createUser("Покупатель", UserRole.USER);
        var category = categoriesService.createCategory("Канцелярия");
        categoriesService.createCategory("Пустая категория");
        var product = productsService.createProduct("Бумага", "PAPER", category.getId(), new BigDecimal("100.00"));
        var otherProduct = productsService.createProduct("Ручка", "PEN", category.getId(), BigDecimal.ONE);
        var warehouse = warehousesService.createWarehouse("Основной", null);
        var otherWarehouse = warehousesService.createWarehouse("Резервный", null);
        inventoriesService.placeProductInWarehouse(product.getId(), warehouse.getId(), Integer.MAX_VALUE);
        inventoriesService.placeProductInWarehouse(product.getId(), otherWarehouse.getId(), 10);
        inventoriesService.placeProductInWarehouse(otherProduct.getId(), warehouse.getId(), 3);
        var created = ordersService.createOrder(buyer.getId(), OrderStatus.CREATED, null);
        ordersService.createOrder(manager.getId(), OrderStatus.CREATED, null);
        ordersService.createOrder(buyer.getId(), OrderStatus.APPROVED, null);
        ordersService.createOrder(buyer.getId(), OrderStatus.DELIVERED, null);
        ordersService.createOrder(manager.getId(), OrderStatus.CANCELLED, null);
        orderItemsService.createOrderItemForOrder(created.getId(), product.getId(), 2, BigDecimal.TEN);
        orderItemsService.createOrderItemForOrder(created.getId(), otherProduct.getId(), 3, BigDecimal.ONE);

        var summary = statistics.getStatistics();

        assertEquals(new SystemStatistics(2, 2, 2, 2, 5, 2, 1, 1, 1,
                (long) Integer.MAX_VALUE + 13, new BigDecimal("23.00")), summary);
        assertEquals(3, summary.activeOrders());
        assertEquals(summary.totalOrders(), summary.activeOrders() + summary.deliveredOrders() + summary.cancelledOrders());
    }

    @Test
    void recalculatesAfterStatusChangesAndDeletion() {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        var order = ordersService.createOrder(manager.getId(), OrderStatus.CREATED, null);
        assertEquals(1, statistics.getStatistics().activeOrders());

        ordersService.changeStatus(manager.getId(), order.getId(), OrderStatus.DELIVERED);
        var delivered = statistics.getStatistics();
        assertEquals(0, delivered.activeOrders());
        assertEquals(1, delivered.deliveredOrders());
        assertEquals(1, delivered.totalOrders());

        ordersService.deleteOrder(order.getId());
        assertEquals(0, statistics.getStatistics().totalOrders());
        assertEquals(0, statistics.getStatistics().deliveredOrders());
    }
}
