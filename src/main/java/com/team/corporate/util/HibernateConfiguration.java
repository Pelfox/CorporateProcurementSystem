package com.team.corporate.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

public class HibernateConfiguration {
    public static SessionFactory getSessionFactory() {
        Configuration configuration = new Configuration();

        Properties settings = new Properties();
        settings.put(Environment.JAKARTA_JDBC_DRIVER, PropertiesUtil.get("db.driver"));
        settings.put(Environment.JAKARTA_JDBC_URL, PropertiesUtil.get("db.url"));
        settings.put(Environment.JAKARTA_JDBC_USER, PropertiesUtil.get("db.username"));
        settings.put(Environment.JAKARTA_JDBC_PASSWORD, PropertiesUtil.get("db.password"));
        settings.put(Environment.SHOW_SQL, "true");
        settings.put(Environment.HBM2DDL_AUTO, "update");

        configuration.setProperties(settings);

        return configuration.buildSessionFactory();
    }
}
