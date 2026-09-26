package com.team.corporate.console;

import com.team.corporate.entities.*;
import com.team.corporate.services.*;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConsoleApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleApplication.class);
    private final ConsoleInput io;
    private final UsersService users;
    private final ProductsService products;
    private final CategoriesService categories;
    private final OrdersService orders;
    private final OrderItemsService items;
    private final InventoriesService inventories;
    private final WarehousesService warehouses;
    private final CsvExportService csvExport;
    private final StatisticsService statistics;
    private User user;

    public ConsoleApplication(@NotNull ConsoleInput io,
                              @NotNull UsersService users,
                              @NotNull ProductsService products,
                              @NotNull CategoriesService categories,
                              @NotNull OrdersService orders,
                              @NotNull OrderItemsService items,
                              @NotNull InventoriesService inventories,
                              @NotNull WarehousesService warehouses,
                              @NotNull CsvExportService csvExport,
                              @NotNull StatisticsService statistics) {
        this.io = io;
        this.users = users;
        this.products = products;
        this.categories = categories;
        this.orders = orders;
        this.items = items;
        this.inventories = inventories;
        this.warehouses = warehouses;
        this.csvExport = csvExport;
        this.statistics = statistics;
    }

    public void run() {
        io.print("Система корпоративных закупок\nВход по имени существующего пользователя. 0 - выход.");
        try {
            while (true) {
                try {
                    if (user == null) {
                        String name = io.required("Имя пользователя");
                        if (name.equals("0")) {
                            return;
                        }
                        user = users.login(name).orElse(null);
                        if (user == null) {
                            io.print("Пользователь не найден.");
                            continue;
                        }
                    }
                    io.print("\n" + user.getUsername() + (isManager() ? " (менеджер)" : ""));
                    io.print("1. Создать заказ\n2. История заказов\n3. Каталог");
                    if (isManager()) {
                        io.print("4. Редактирование заказа (только для менеджера)\n5. Редактирование склада (только для менеджера)");
                        io.print("6. Экспорт данных в CSV (только для менеджера)");
                    }
                    io.print("7. Статистика системы\n9. Сменить пользователя\n0. Выход");
                    switch (io.number("Выбор", 0, 9)) {
                        case 0 -> {
                            return;
                        }
                        case 1 -> createOrder();
                        case 2 -> history();
                        case 3 -> catalog();
                        case 4 -> {
                            requireManager();
                            manageOrders();
                        }
                        case 5 -> {
                            requireManager();
                            manageWarehouse();
                        }
                        case 6 -> exportCsv();
                        case 7 -> showStatistics();
                        case 9 -> user = null;
                        default -> io.print("Нет такого пункта меню.");
                    }
                } catch (EndOfFileException e) {
                    throw e;
                } catch (UserInterruptException e) {
                    io.print("Действие отменено.");
                } catch (IllegalArgumentException e) {
                    io.print(e.getMessage());
                } catch (RuntimeException e) {
                    LOG.error("Не удалось выполнить действие в консоли", e);
                    io.print("Операция не выполнена. Проверьте данные и подключение к базе.");
                }
            }
        } catch (EndOfFileException e) {
            io.print("До свидания.");
        }
    }

    private void showStatistics() {
        var summary = statistics.getStatistics();
        io.print("\nСтатистика системы");
        io.print("Всего пользователей: " + summary.totalUsers());
        io.print("Всего товаров в каталоге: " + summary.totalProducts());
        io.print("Всего категорий: " + summary.totalCategories());
        io.print("Всего складов: " + summary.totalWarehouses());
        io.print("Всего заказов: " + summary.totalOrders());
        io.print("Активных заказов (созданных и одобренных): " + summary.activeOrders());
        io.print("Созданных заказов: " + summary.createdOrders());
        io.print("Одобренных заказов: " + summary.approvedOrders());
        io.print("Завершённых заказов (доставленных): " + summary.deliveredOrders());
        io.print("Отменённых заказов: " + summary.cancelledOrders());
        io.print("Общий остаток на складах (шт.): " + summary.totalStockQuantity());
    }

    private void exportCsv() {
        requireManager();
        String directory = io.required("Папка для экспорта (0 - отмена)");
        if (directory.equals("0")) {
            return;
        }
        try {
            Path result = csvExport.exportAll(user.getId(), Path.of(directory));
            io.print("Экспорт завершён. CSV-файлы сохранены в: " + result);
        } catch (InvalidPathException e) {
            io.print("Некорректный путь к папке экспорта.");
        } catch (IOException e) {
            LOG.error("Не удалось сохранить CSV-файлы", e);
            io.print("Не удалось сохранить CSV-файлы. Проверьте путь, права на запись и свободное место.");
        }
    }

    private boolean isManager() {
        return user.getUserRole() == UserRole.MANAGER;
    }

    private void requireManager() {
        if (!isManager()) {
            throw new IllegalArgumentException("Доступ только для менеджера.");
        }
    }

    @NotNull
    private String productLabel(@NotNull Product p) {
        return p.getName() + " | " + p.getSku() + " | " + p.getPrice() + " руб. | "
                + (p.getCategory() == null ? "Без категории" : p.getCategory().getName());
    }

    @NotNull
    private String statusLabel(@NotNull OrderStatus status) {
        return switch (status) {
            case CREATED -> "Создан";
            case APPROVED -> "Одобрен";
            case DELIVERED -> "Доставлен";
            case CANCELLED -> "Отменён";
        };
    }

    @NotNull
    private String orderLabel(@NotNull Order order) {
        return order.getId() + " | " + statusLabel(order.getStatus()) + " | " + order.getCreatedAt()
                + " | " + order.getUser().getUsername();
    }

    @NotNull
    private String itemLabel(@NotNull OrderItem item) {
        return item.getProduct().getName() + " × " + item.getQuantity() + " по " + item.getPurchasePrice() + " руб.";
    }

    private void catalog() {
        List<Product> all = products.getAll();
        if (all.isEmpty()) {
            io.print("Каталог пуст.");
        }
        for (Product p : all) {
            io.print(productLabel(p));
            for (Inventory stock : inventories.getAllByProduct(p.getId())) {
                io.print("  " + stock.getWarehouse().getName() + ": " + stock.getQuantity() + " шт.");
            }
        }
    }

    private void createOrder() {
        List<OrdersService.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        while (true) {
            Product product = io.select("Выберите товар", products.getAll(), this::productLabel);
            if (product == null) {
                return;
            }
            int quantity = io.number("Количество", 1, Integer.MAX_VALUE);
            lines.add(new OrdersService.Line(product.getId(), quantity));
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            io.print("Добавлено: " + product.getName() + " × " + quantity + ". Итого: " + total + " руб.");
            if (!io.requestConfirmation("Добавить ещё товар?")) {
                break;
            }
        }
        String notes = io.notes("Примечание (можно оставить пустым)");
        if (io.requestConfirmation("Создать заказ на " + total + " руб.?")) {
            io.print("Заказ создан: " + orders.createOrder(user.getId(), lines, notes));
        }
    }

    private void showOrder(@NotNull Order order) {
        io.print(orderLabel(order));
        io.print("Примечание: " + (order.getNotes() == null ? "" : order.getNotes()));
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items.getAllByOrder(order.getId())) {
            io.print("  " + itemLabel(item));
            total = total.add(item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        io.print("Итого: " + total + " руб.");
    }

    private void history() {
        while (true) {
            Order order = io.select("История заказов", orders.getAllByUser(user.getId()), this::orderLabel);
            if (order == null) {
                return;
            }
            editOwnOrder(order);
        }
    }

    private void editOwnOrder(@NotNull Order selected) {
        while (true) {
            Order order = orders.getOrder(selected.getId()).orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
            showOrder(order);
            if (order.getStatus() != OrderStatus.CREATED) {
                io.print("Этот заказ недоступен для редактирования.");
                return;
            }
            io.print("1. Добавить товар\n2. Изменить количество\n3. Удалить позицию\n4. Изменить примечание\n0. Назад");
            switch (io.number("Выбор", 0, 4)) {
                case 0 -> {
                    return;
                }
                case 1 -> {
                    Product p = io.select("Товар", products.getAll(), this::productLabel);
                    if (p != null) {
                        orders.addItem(user.getId(), order.getId(), new OrdersService.Line(p.getId(), io.number("Количество", 1, Integer.MAX_VALUE)));
                    }
                }
                case 2 -> editItem(order, false);
                case 3 -> editItem(order, true);
                case 4 ->
                        orders.updateNotes(user.getId(), order.getId(), io.notes("Новое примечание (пустое - очистить)"));
            }
        }
    }

    private void editItem(@NotNull Order order, boolean remove) {
        OrderItem item = io.select("Выберите позицию", items.getAllByOrder(order.getId()), this::itemLabel);
        if (item != null) {
            int quantity = remove ? 0 : io.number("Новое количество", 1, Integer.MAX_VALUE);
            if (quantity != 0 || io.requestConfirmation("Удалить позицию?")) {
                orders.updateQuantity(user.getId(), order.getId(), item.getId(), quantity);
            }
        }
    }

    private void manageOrders() {
        requireManager();
        Order order = io.select("Все заказы", orders.getAll(), this::orderLabel);
        if (order == null) {
            return;
        }
        showOrder(order);
        OrderStatus status = io.select("Новый статус", Arrays.asList(OrderStatus.values()), this::statusLabel);
        if (status != null && io.requestConfirmation("Изменить статус на «" + statusLabel(status) + "»?")) {
            orders.changeStatus(user.getId(), order.getId(), status);
            io.print("Статус сохранён.");
        }
    }

    private void manageWarehouse() {
        requireManager();
        while (true) {
            io.print("\nРедактирование склада\n1. Добавить товар\n2. Удалить товар\n3. Добавить категорию\n4. Удалить категорию\n5. Создать склад\n6. Установить остаток товара\n7. Удалить товар со склада\n8. Просмотреть остатки\n0. Назад");
            switch (io.number("Выбор", 0, 8)) {
                case 0 -> {
                    return;
                }
                case 1 -> {
                    Category category = io.select("Категория (создайте категорию, если список пуст)", categories.getAll(), Category::getName);
                    if (category == null) {
                        continue;
                    }
                    String name = io.required("Название товара");
                    String sku = io.required("Артикул");
                    products.createProduct(name, sku, category.getId(), io.money("Цена"));
                    io.print("Товар добавлен.");
                }
                case 2 -> {
                    Product p = io.select("Удалить товар из каталога", products.getAll(), this::productLabel);
                    if (p != null && io.requestConfirmation("Удалить товар и все его складские остатки?")) {
                        products.deleteProduct(p.getId());
                    }
                }
                case 3 -> {
                    categories.createCategory(io.required("Название категории"));
                    io.print("Категория добавлена.");
                }
                case 4 -> {
                    Category c = io.select("Удалить категорию", categories.getAll(), Category::getName);
                    if (c != null && io.requestConfirmation("Удалить категорию «" + c.getName() + "»?")) {
                        categories.deleteCategory(c.getId());
                    }
                }
                case 5 -> {
                    warehouses.createWarehouse(io.required("Название склада"), io.text("Адрес"));
                    io.print("Склад создан.");
                }
                case 6 -> setStock();
                case 7 -> {
                    Inventory stock = io.select("Удалить остаток", inventories.getAll(), i -> i.getWarehouse().getName() + " | " + i.getProduct().getName() + " | " + i.getQuantity());
                    if (stock != null && io.requestConfirmation("Удалить товар со склада?")) {
                        inventories.deleteInventory(stock.getId());
                    }
                }
                case 8 -> catalog();
            }
        }
    }

    private void setStock() {
        Warehouse warehouse = io.select("Склад", warehouses.getAll(), Warehouse::getName);
        if (warehouse == null) {
            return;
        }
        Product product = io.select("Товар", products.getAll(), this::productLabel);
        if (product == null) {
            return;
        }
        int quantity = io.number("Остаток", 0, Integer.MAX_VALUE);
        var stock = inventories.getAllByProductAndWarehouse(product.getId(), warehouse.getId());
        if (stock.isPresent()) {
            inventories.updateInventory(stock.get().getId(), quantity);
        } else {
            inventories.placeProductInWarehouse(product.getId(), warehouse.getId(), quantity);
        }
        io.print("Остаток сохранён.");
    }
}
