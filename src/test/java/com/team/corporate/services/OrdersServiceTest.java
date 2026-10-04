package com.team.corporate;

import com.team.corporate.entities.*;
import com.team.corporate.exceptions.UserNotFoundException;
import com.team.corporate.services.OrdersService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrdersServiceTest extends BaseServiceTest {

    // подготовка данных
    private User createUser(String name, UserRole role) {
        return usersService.createUser(name, role);
    }

    private Product createProduct(String name, String sku, BigDecimal price) {
        Category category = categoriesService.createCategory("Категория для " + name);
        return productsService.createProduct(name, sku, category.getId(), price);
    }

    // method create

    @Test
    @DisplayName("Создание заказа: должен сохраниться со статусом CREATED")
    void createOrder_shouldSaveWithCreatedStatus() {
        // given
        User user = createUser("buyer", UserRole.USER);
        Product product = createProduct("Ноутбук", "LAP-001", new BigDecimal("75000.00"));

        // when
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), "Срочный заказ");

        // then
        Order order = ordersService.getOrder(orderId).orElseThrow();
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals("Срочный заказ", order.getNotes());
        assertEquals(user.getId(), order.getUser().getId());
    }

    @Test
    @DisplayName("Создание заказа: должен выбросить исключение при пустом списке товаров")
    void createOrder_shouldThrow_whenLinesEmpty() {
        // given
        User user = createUser("buyer", UserRole.USER);

        // when and then
        assertThrows(IllegalArgumentException.class, () ->
                ordersService.createOrder(user.getId(), List.of(), "Пустой заказ"));
    }

    @Test
    @DisplayName("Создание заказа: должен выбросить исключение при несуществующем пользователе")
    void createOrder_shouldThrow_whenUserNotFound() {
        // given
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));

        // when and then
        assertThrows(UserNotFoundException.class, () ->
                ordersService.createOrder(UUID.randomUUID(),
                        List.of(new OrdersService.Line(product.getId(), 1)), null));
    }

    // method read

    @Test
    @DisplayName("Поиск заказа по ID: должен вернуть заказ")
    void getOrder_shouldReturnOrder() {
        // given
        User user = createUser("buyer", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 2)), null);

        // when
        Optional<Order> found = ordersService.getOrder(orderId);

        // then
        assertTrue(found.isPresent());
        assertEquals(orderId, found.get().getId());
    }

    @Test
    @DisplayName("Поиск заказов по пользователю: должен вернуть только его заказы")
    void getAllByUser_shouldReturnOnlyUserOrders() {
        // given
        User user1 = createUser("user1", UserRole.USER);
        User user2 = createUser("user2", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));

        ordersService.createOrder(user1.getId(), List.of(new OrdersService.Line(product.getId(), 1)), null);
        ordersService.createOrder(user1.getId(), List.of(new OrdersService.Line(product.getId(), 2)), null);
        ordersService.createOrder(user2.getId(), List.of(new OrdersService.Line(product.getId(), 3)), null);

        // when
        List<Order> user1Orders = ordersService.getAllByUser(user1.getId());
        List<Order> user2Orders = ordersService.getAllByUser(user2.getId());

        // then
        assertEquals(2, user1Orders.size());
        assertEquals(1, user2Orders.size());
    }

    @Test
    @DisplayName("Поиск заказов по статусу: должен вернуть только заказы с этим статусом")
    void getAllByStatus_shouldFilterOrders() {
        // given
        User user = createUser("buyer", UserRole.USER);
        User manager = createUser("manager", UserRole.MANAGER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));

        UUID order1 = ordersService.createOrder(user.getId(), List.of(new OrdersService.Line(product.getId(), 1)), null);
        UUID order2 = ordersService.createOrder(user.getId(), List.of(new OrdersService.Line(product.getId(), 2)), null);
        ordersService.changeStatus(manager.getId(), order2, OrderStatus.APPROVED);

        // when
        List<Order> created = ordersService.getAllByStatus(OrderStatus.CREATED);
        List<Order> approved = ordersService.getAllByStatus(OrderStatus.APPROVED);

        // then
        assertEquals(1, created.size());
        assertEquals(1, approved.size());
        assertEquals(order1, created.getFirst().getId());
    }

    // method update

    @Test
    @DisplayName("Обновление примечания: владелец может изменить примечание своего заказа")
    void updateNotes_shouldUpdate_whenOwner() {
        // given
        User user = createUser("buyer", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), "Старое примечание");

        // when
        ordersService.updateNotes(user.getId(), orderId, "Новое примечание");

        // then
        Order order = ordersService.getOrder(orderId).orElseThrow();
        assertEquals("Новое примечание", order.getNotes());
    }

    @Test
    @DisplayName("Обновление примечания: чужой пользователь не может изменить заказ")
    void updateNotes_shouldThrow_whenNotOwner() {
        // given
        User owner = createUser("owner", UserRole.USER);
        User stranger = createUser("stranger", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(owner.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);

        // when and then
        assertThrows(IllegalArgumentException.class, () ->
                ordersService.updateNotes(stranger.getId(), orderId, "Взлом"));
    }

    @Test
    @DisplayName("Обновление примечания: нельзя редактировать заказ в статусе APPROVED")
    void updateNotes_shouldThrow_whenOrderNotCreated() {
        // given
        User user = createUser("buyer", UserRole.USER);
        User manager = createUser("manager", UserRole.MANAGER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);
        ordersService.changeStatus(manager.getId(), orderId, OrderStatus.APPROVED);

        // when and then
        assertThrows(IllegalArgumentException.class, () ->
                ordersService.updateNotes(user.getId(), orderId, "Поздно менять"));
    }

    // change status (br)

    @Test
    @DisplayName("Смена статуса: менеджер может изменить статус заказа")
    void changeStatus_shouldWork_whenManager() {
        // given
        User user = createUser("buyer", UserRole.USER);
        User manager = createUser("manager", UserRole.MANAGER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);

        // when
        ordersService.changeStatus(manager.getId(), orderId, OrderStatus.APPROVED);

        // then
        Order order = ordersService.getOrder(orderId).orElseThrow();
        assertEquals(OrderStatus.APPROVED, order.getStatus());
    }

    @Test
    @DisplayName("Смена статуса: обычный пользователь НЕ может изменить статус")
    void changeStatus_shouldThrow_whenNotManager() {
        // given
        User user = createUser("buyer", UserRole.USER);
        User otherUser = createUser("other", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);

        // when and then
        assertThrows(IllegalArgumentException.class, () ->
                ordersService.changeStatus(otherUser.getId(), orderId, OrderStatus.APPROVED));
    }

    @Test
    @DisplayName("Смена статуса: должен создаться AuditLog при смене статуса")
    void changeStatus_shouldCreateAuditLog() {
        // given
        User user = createUser("buyer", UserRole.USER);
        User manager = createUser("manager", UserRole.MANAGER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);

        // when
        ordersService.changeStatus(manager.getId(), orderId, OrderStatus.APPROVED);

        // then
        List<AuditLog> logs = auditLogsService.getAllByOrder(orderId);
        assertEquals(1, logs.size());
        assertEquals(OrderStatus.CREATED, logs.getFirst().getOldStatus());
        assertEquals(OrderStatus.APPROVED, logs.getFirst().getNewStatus());
        assertEquals(manager.getId(), logs.getFirst().getChangedBy().getId());
    }

    @Test
    @DisplayName("Смена статуса: повторная установка того же статуса не создает AuditLog")
    void changeStatus_shouldNotCreateAuditLog_whenSameStatus() {
        // given
        User user = createUser("buyer", UserRole.USER);
        User manager = createUser("manager", UserRole.MANAGER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);

        // when
        ordersService.changeStatus(manager.getId(), orderId, OrderStatus.CREATED);

        // then
        List<AuditLog> logs = auditLogsService.getAllByOrder(orderId);
        assertEquals(0, logs.size(), "Не должно быть лога, если статус не изменился");
    }

    // items (br)

    @Test
    @DisplayName("Обновление количества: владелец может изменить количество в своем заказе")
    void updateQuantity_shouldUpdate_whenOwner() {
        // given
        User user = createUser("buyer", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 5)), null);
        UUID itemId = orderItemsService.getAllByOrder(orderId).getFirst().getId();

        // when
        ordersService.updateQuantity(user.getId(), orderId, itemId, 10);

        // then
        OrderItem item = orderItemsService.getOrderItem(itemId).orElseThrow();
        assertEquals(10, item.getQuantity());
    }

    @Test
    @DisplayName("Удаление позиции: нельзя удалить последнюю позицию в заказе")
    void updateQuantity_shouldThrow_whenRemovingLastItem() {
        // given
        User user = createUser("buyer", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);
        UUID itemId = orderItemsService.getAllByOrder(orderId).getFirst().getId();

        // when and then
        assertThrows(IllegalArgumentException.class, () ->
                ordersService.updateQuantity(user.getId(), orderId, itemId, 0));
    }

    // method delete

    @Test
    @DisplayName("Удаление заказа: должен исчезнуть из базы")
    void deleteOrder_shouldRemoveOrder() {
        // given
        User user = createUser("buyer", UserRole.USER);
        Product product = createProduct("Товар", "SKU-001", new BigDecimal("100.00"));
        UUID orderId = ordersService.createOrder(user.getId(),
                List.of(new OrdersService.Line(product.getId(), 1)), null);

        // when
        ordersService.deleteOrder(orderId);

        // then
        assertTrue(ordersService.getOrder(orderId).isEmpty());
    }
}