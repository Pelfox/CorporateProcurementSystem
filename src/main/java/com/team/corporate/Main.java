package com.team.corporate;

import com.team.corporate.config.FileConfigurationProvider;
import com.team.corporate.console.ConsoleApplication;
import com.team.corporate.console.ConsoleInput;
import com.team.corporate.console.ConsoleTerminal;
import com.team.corporate.repositories.impls.*;
import com.team.corporate.services.impls.*;
import com.team.corporate.utils.HibernateFactory;
import org.jline.reader.LineReaderBuilder;

public class Main {
    static void main() throws Exception {
        var config = new FileConfigurationProvider().getConfiguration();
        try (var factory = HibernateFactory.createSessionFactory(config);
             var terminal = ConsoleTerminal.open()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var transactions = new HibernateTransactions(factory);
            var usersRepository = new HibernateUsersRepository(factory);
            var productsRepository = new HibernateProductsRepository(factory);
            var categoriesRepository = new HibernateCategoriesRepository(factory);
            var ordersRepository = new HibernateOrdersRepository(factory);
            var itemsRepository = new HibernateOrderItemsRepository(factory);
            var inventoriesRepository = new HibernateInventoriesRepository(factory);
            var warehousesRepository = new HibernateWarehousesRepository(factory);
            var auditLogsRepository = new HibernateAuditLogsRepository(factory);

            var users = new UsersServiceImpl(usersRepository);
            var products = new ProductsServiceImpl(
                    productsRepository,
                    categoriesRepository,
                    itemsRepository,
                    inventoriesRepository,
                    transactions
            );
            var categories = new CategoriesServiceImpl(categoriesRepository, productsRepository, transactions);
            var orders = new OrdersServiceImpl(
                    ordersRepository,
                    usersRepository,
                    productsRepository,
                    itemsRepository,
                    auditLogsRepository,
                    transactions
            );
            var items = new OrderItemsServiceImpl(ordersRepository, productsRepository, itemsRepository);
            var inventories = new InventoriesServiceImpl(productsRepository, warehousesRepository, inventoriesRepository);
            var warehouses = new WarehousesServiceImpl(warehousesRepository);

            new ConsoleApplication(
                    new ConsoleInput(reader),
                    users,
                    products,
                    categories,
                    orders,
                    items,
                    inventories,
                    warehouses
            ).run();
        }
    }
}
