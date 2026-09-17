package com.team.corporate.utils;

import com.team.corporate.config.ApplicationConfiguration;
import com.team.corporate.entities.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class HibernateFactory {
    private static HikariDataSource createDataSource(@NotNull ApplicationConfiguration appConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(appConfig.getDatabaseUrl());
        config.setUsername(appConfig.getDatabaseUsername());
        config.setPassword(appConfig.getDatabasePassword());
        return new HikariDataSource(config);
    }

    public static SessionFactory createSessionFactory(@NotNull ApplicationConfiguration appConfig) {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClasses(
                AuditLog.class, Category.class, Inventory.class,
                Order.class, OrderItem.class, Product.class,
                User.class, Warehouse.class
        );

        Properties properties = new Properties();
        properties.put("hibernate.connection.datasource", createDataSource(appConfig));
        properties.put("hibernate.hbm2ddl.auto", appConfig.getHibernateDdl());
        properties.put("hibernate.jdbc.time_zone", appConfig.getJdbcTimeZone());

        configuration.setProperties(properties);
        return configuration.buildSessionFactory();
    }
}
