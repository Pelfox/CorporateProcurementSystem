package com.team.corporate;

import com.team.corporate.config.ApplicationConfiguration;
import com.team.corporate.config.FileConfigurationProvider;
import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.repositories.impls.HibernateUsersRepository;
import com.team.corporate.utils.HibernateFactory;
import org.hibernate.SessionFactory;

public class Main {
    static void main() {
        ApplicationConfiguration appConfig = new FileConfigurationProvider().getConfiguration();
        try (SessionFactory factory = HibernateFactory.createSessionFactory(appConfig)) {
            HibernateUsersRepository usersRepository = new HibernateUsersRepository(factory);
            usersRepository.add(new User("ivanov", UserRole.USER));
            usersRepository.add(new User("petrov", UserRole.USER));
            usersRepository.add(new User("sidorov", UserRole.MANAGER));
        }
    }
}
