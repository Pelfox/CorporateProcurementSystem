package com.team.corporate.console;

import com.team.corporate.core.config.ConfigurationProvider;
import com.team.corporate.core.config.FileConfigurationProvider;
import com.team.corporate.core.repositories.impls.*;
import com.team.corporate.core.services.impls.*;
import com.team.corporate.core.utils.DataInitializer;
import com.team.corporate.core.utils.HibernateFactory;
import org.jline.reader.LineReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConsoleMain {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleMain.class);

    static void main() {
        int result = run(new FileConfigurationProvider());
        if (result != 0) {
            System.exit(result);
        }
    }

    static int run(ConfigurationProvider configurationProvider) {
        try {
            start(configurationProvider);
            return 0;
        } catch (Exception e) {
            LOG.error("Не удалось запустить или завершить приложение", e);
            System.err.println("Не удалось запустить или завершить приложение. Проверьте файл настроек и доступность базы данных.");
            return 1;
        }
    }

    private static void start(ConfigurationProvider configurationProvider) throws Exception {
        var config = configurationProvider.getConfiguration();
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

            var stock = new OrderStock(productsRepository, inventoriesRepository, itemsRepository, transactions);

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
                    transactions,
                    stock
            );
            var items = new OrderItemsServiceImpl(ordersRepository, productsRepository, itemsRepository, transactions, stock);
            var inventories = new InventoriesServiceImpl(productsRepository, warehousesRepository, inventoriesRepository, stock, transactions);
            var warehouses = new WarehousesServiceImpl(warehousesRepository);
            var statistics = new StatisticsServiceImpl(
                    usersRepository, productsRepository, categoriesRepository, warehousesRepository,
                    ordersRepository, itemsRepository, inventoriesRepository, transactions);
            var csvExport = new CsvExportServiceImpl(
                    usersRepository,
                    categoriesRepository,
                    productsRepository,
                    warehousesRepository,
                    inventoriesRepository,
                    ordersRepository,
                    itemsRepository,
                    auditLogsRepository,
                    transactions
            );

            transactions.execute(() -> new DataInitializer(users, categories, products, orders, warehouses, inventories).run());

            new ConsoleApplication(
                    new ConsoleInput(reader),
                    users,
                    products,
                    categories,
                    orders,
                    items,
                    inventories,
                    warehouses,
                    csvExport,
                    statistics
            ).run();
        }
    }
}
