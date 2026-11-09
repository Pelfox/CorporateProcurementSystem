package com.team.corporate.backend.config;

import com.team.corporate.core.repositories.TagsRepository;
import com.team.corporate.core.repositories.Transactions;
import com.team.corporate.core.repositories.UsersRepository;
import com.team.corporate.core.repositories.impls.HibernateTagsRepository;
import com.team.corporate.core.services.*;
import com.team.corporate.core.services.impls.BulkOperationsServiceImpl;
import com.team.corporate.core.services.impls.TagsServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BulkOperationsConfiguration {

    @Bean
    TagsRepository tagsRepository(EntityManagerFactory factory) {
        return new HibernateTagsRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    TagsService tagsService(TagsRepository tagsRepository) {
        return new TagsServiceImpl(tagsRepository);
    }

    @Bean
    BulkOperationsService bulkOperationsService(
            com.team.corporate.core.repositories.OrdersRepository ordersRepository,
            UsersRepository usersRepository,
            AuditLogsService auditLogsService,
            TagsService tagsService,
            OrdersService ordersService,
            Transactions transactions) {
        return new BulkOperationsServiceImpl(ordersRepository, usersRepository, auditLogsService,
                tagsService, ordersService, transactions);
    }
}