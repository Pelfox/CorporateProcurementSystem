package com.team.corporate.backend.config;

import com.team.corporate.backend.services.BootstrapAdminService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.auth.bootstrap.enabled", havingValue = "true")
public class AdminBootstrapConfig {
    @Bean
    ApplicationRunner bootstrapAdmin(BootstrapAdminService bootstrap, Environment environment) {
        return arguments -> bootstrap.provision(environment.getRequiredProperty("BOOTSTRAP_ADMIN_USERNAME"),
                () -> environment.getRequiredProperty("BOOTSTRAP_ADMIN_TEMPORARY_PASSWORD"));
    }
}
