package com.team.corporate;

import com.team.corporate.console.ConsoleApplication;
import com.team.corporate.console.ConsoleInput;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.entities.UserRole;
import com.team.corporate.services.StatisticsService;
import com.team.corporate.services.SystemStatistics;
import com.team.corporate.services.impls.CsvExportServiceImpl;
import com.team.corporate.services.impls.StatisticsServiceImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest extends BaseServiceTest {
    private StatisticsService statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = new StatisticsServiceImpl(usersRepository, productsRepository, categoriesRepository,
                warehousesRepository, ordersRepository, inventoriesRepository, transactions);
    }

    @Test
    void emptyDatabaseHasZeroStatistics() {
        assertEquals(new SystemStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), statistics.getStatistics());
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
        inventoriesService.placeProductInWarehouse(otherProduct.getId(), warehouse.getId(), 0);
        var created = ordersService.createOrder(buyer.getId(), OrderStatus.CREATED, null);
        ordersService.createOrder(manager.getId(), OrderStatus.CREATED, null);
        ordersService.createOrder(buyer.getId(), OrderStatus.APPROVED, null);
        ordersService.createOrder(buyer.getId(), OrderStatus.DELIVERED, null);
        ordersService.createOrder(manager.getId(), OrderStatus.CANCELLED, null);
        orderItemsService.createOrderItemForOrder(created.getId(), product.getId(), 2, BigDecimal.TEN);
        orderItemsService.createOrderItemForOrder(created.getId(), otherProduct.getId(), 3, BigDecimal.ONE);

        var summary = statistics.getStatistics();

        assertEquals(new SystemStatistics(2, 2, 2, 2, 5, 2, 1, 1, 1,
                (long) Integer.MAX_VALUE + 10), summary);
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

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void menuDisplaysStatisticsAndReturnsToMainMenu(UserRole role) throws Exception {
        usersService.createUser("Тест", role);
        var answers = List.of("Тест", "7", "0").iterator();
        var output = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal("test", "dumb", InputStream.nullInputStream(), output, StandardCharsets.UTF_8)) {
            LineReader reader = (LineReader) Proxy.newProxyInstance(LineReader.class.getClassLoader(),
                    new Class<?>[]{LineReader.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getTerminal" -> terminal;
                        case "readLine" -> {
                            if (!answers.hasNext()) {
                                throw new EndOfFileException();
                            }
                            yield answers.next();
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            var exporter = new CsvExportServiceImpl(usersRepository, categoriesRepository, productsRepository,
                    warehousesRepository, inventoriesRepository, ordersRepository, orderItemsRepository,
                    auditLogsRepository, transactions);
            new ConsoleApplication(new ConsoleInput(reader), usersService, productsService, categoriesService,
                    ordersService, orderItemsService, inventoriesService, warehousesService, exporter, statistics).run();
        }

        String text = output.toString(StandardCharsets.UTF_8);
        assertFalse(answers.hasNext());
        for (String line : List.of("Всего пользователей: 1", "Всего товаров в каталоге: 0", "Всего категорий: 0",
                "Всего складов: 0", "Всего заказов: 0", "Активных заказов (созданных и одобренных): 0",
                "Созданных заказов: 0", "Одобренных заказов: 0", "Завершённых заказов (доставленных): 0",
                "Отменённых заказов: 0", "Общий остаток на складах (шт.): 0")) {
            assertTrue(text.contains(line), line);
        }
        assertEquals(2, text.lines().filter(line -> line.equals("7. Статистика системы")).count());
    }
}
