package com.team.corporate.backend.config;

import com.team.corporate.backend.security.ConcurrencyLimitedPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${app.auth.bcrypt-strength:12}") int strength,
                                          AuthRateLimitProperties limits) {
        return new ConcurrencyLimitedPasswordEncoder(new BCryptPasswordEncoder(strength), limits.maxParallelPasswordChecks());
    }
}
