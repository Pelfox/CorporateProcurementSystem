package com.team.corporate.backend.config;

import com.team.corporate.core.repositories.UsersRepository;
import com.team.corporate.core.services.UsersService;
import com.team.corporate.core.services.impls.UsersServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServicesConfig {

    @Bean
    public UsersService usersService(UsersRepository usersRepository) {
        return new UsersServiceImpl(usersRepository);
    }
}
