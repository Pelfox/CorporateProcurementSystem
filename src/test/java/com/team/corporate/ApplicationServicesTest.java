package com.team.corporate;

import com.team.corporate.config.ApplicationConfiguration;
import com.team.corporate.config.ApplicationServices;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationServicesTest {
    @Test
    void initializesDemoStockBeforeOrdersAndCommitsReservationsAndDeliveries() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:h2:mem:bootstrap;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        properties.setProperty("db.username", "sa");
        properties.setProperty("db.password", "");
        properties.setProperty("hibernate.ddl.auto", "create-drop");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");
        try (var app = ApplicationServices.open(ApplicationConfiguration.fromProperties(properties))) {
            assertEquals(10, app.orders().getAll().size());
            assertEquals(10, app.inventories().getAll().size());
            assertTrue(app.stock().getAvailableQuantities(null).values().stream().allMatch(quantity -> quantity > 0));
            assertEquals(9_979, app.inventories().getAll().stream().mapToLong(item -> item.getQuantity()).sum());
        }
    }
}
