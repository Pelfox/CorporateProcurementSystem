package com.team.corporate.backend.security;

import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.validation.InvalidUsernameException;
import com.team.corporate.backend.validation.UsernamePolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserJpaRepository users;
    private final Clock clock;

    public DatabaseUserDetailsService(UserJpaRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String normalized;
        try {
            normalized = UsernamePolicy.normalize(username);
        } catch (InvalidUsernameException error) {
            throw new UsernameNotFoundException("Неверный логин или пароль.");
        }
        var user = users.findByUsername(normalized)
                .filter(value -> value.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль."));
        return new ProcurementUserDetails(user, clock.instant());
    }
}
