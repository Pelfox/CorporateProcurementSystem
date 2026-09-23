package com.team.corporate.utils;

import com.team.corporate.entities.*;
import com.team.corporate.services.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class DataInitializer {
    private final UsersService usersService;
    private final CategoriesService categoriesService;
    private final ProductsService productsService;
    private final OrdersService ordersService;

    public DataInitializer(UsersService usersService, CategoriesService categoriesService,
                           ProductsService productsService, OrdersService ordersService) {
        this.usersService = usersService;
        this.categoriesService = categoriesService;
        this.productsService = productsService;
        this.ordersService = ordersService;
    }

    public void run() {
        if (!usersService.getAll().isEmpty()) {
            System.out.println("База данных уже содержит данные.");
            return;
        }

        System.out.println("Инициализация тестовых данных ...");

        // Категории
        Category office = categoriesService.createCategory("Офисная техника");
        Category stationery = categoriesService.createCategory("Канцелярские товары");
        Category furniture = categoriesService.createCategory("Мебель");

        // Пользователи
        User user1 = usersService.createUser("ivan_sid", UserRole.USER);
        User user2 = usersService.createUser("nikita_och", UserRole.USER);
        User manager1 = usersService.createUser("matvey_rat", UserRole.MANAGER);
        User user3 = usersService.createUser("alexey_dem", UserRole.USER);
        User manager2 = usersService.createUser("dmitry_sla", UserRole.MANAGER);

        // Товары
        Product p1 = productsService.createProduct("Ноутбук Dell Latitude", "LAPTOP-001", office.getId(), new BigDecimal("75000.00"));
        Product p2 = productsService.createProduct("Монитор LG 27\"", "MONITOR-002", office.getId(), new BigDecimal("25000.00"));
        Product p3 = productsService.createProduct("Бумага А4 (500 листов)", "PAPER-003", stationery.getId(), new BigDecimal("450.00"));
        Product p4 = productsService.createProduct("Ручка шариковая синяя", "PEN-004", stationery.getId(), new BigDecimal("30.00"));
        Product p5 = productsService.createProduct("Кресло офисное", "CHAIR-005", furniture.getId(), new BigDecimal("12000.00"));
        Product p6 = productsService.createProduct("Стол письменный", "DESK-006", furniture.getId(), new BigDecimal("15000.00"));
        Product p7 = productsService.createProduct("Принтер HP LaserJet", "PRINTER-007", office.getId(), new BigDecimal("30000.00"));
        Product p8 = productsService.createProduct("Блокнот А5", "NOTEBOOK-008", stationery.getId(), new BigDecimal("150.00"));
        Product p9 = productsService.createProduct("Настольная лампа", "LAMP-009", office.getId(), new BigDecimal("3500.00"));
        Product p10 = productsService.createProduct("Шкаф для документов", "CABINET-010", furniture.getId(), new BigDecimal("18000.00"));

        // Заказы в статусе CREATED
        ordersService.createOrder(user1.getId(), List.of(new OrdersService.Line(p1.getId(), 2)), "Заказ на новое оборудование");
        ordersService.createOrder(user3.getId(), List.of(new OrdersService.Line(p3.getId(), 50)), "Заказ на канцелярию");
        ordersService.createOrder(user2.getId(), List.of(new OrdersService.Line(p2.getId(), 3)), "Нужны новые мониторы");

        UUID order4Id = ordersService.createOrder(user2.getId(), List.of(new OrdersService.Line(p4.getId(), 100)), "Согласовано с начальством");
        ordersService.changeStatus(manager1.getId(), order4Id, OrderStatus.APPROVED);

        UUID order5Id = ordersService.createOrder(user1.getId(), List.of(new OrdersService.Line(p5.getId(), 1)), "Одобрено");
        ordersService.changeStatus(manager1.getId(), order5Id, OrderStatus.APPROVED);

        UUID order6Id = ordersService.createOrder(user3.getId(), List.of(new OrdersService.Line(p6.getId(), 2)), "В процессе");
        ordersService.changeStatus(manager2.getId(), order6Id, OrderStatus.APPROVED);

        UUID order7Id = ordersService.createOrder(manager1.getId(), List.of(new OrdersService.Line(p7.getId(), 1)), "Доставлено в офис");
        ordersService.changeStatus(manager1.getId(), order7Id, OrderStatus.DELIVERED);

        UUID order8Id = ordersService.createOrder(manager1.getId(), List.of(new OrdersService.Line(p8.getId(), 20)), "Получено");
        ordersService.changeStatus(manager2.getId(), order8Id, OrderStatus.DELIVERED);

        UUID order9Id = ordersService.createOrder(user3.getId(), List.of(new OrdersService.Line(p9.getId(), 5)), "Отменено по причине смены поставщика");
        ordersService.changeStatus(manager1.getId(), order9Id, OrderStatus.CANCELLED);

        UUID order10Id = ordersService.createOrder(manager2.getId(), List.of(new OrdersService.Line(p10.getId(), 1)), "Отказ");
        ordersService.changeStatus(manager2.getId(), order10Id, OrderStatus.CANCELLED);

        System.out.println("Тестовые данные добавлены!");
    }
}