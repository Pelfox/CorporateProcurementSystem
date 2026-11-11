package com.team.corporate.backend;

import com.team.corporate.core.entities.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackageClasses = User.class)
public class BackendMain {
    public static void main(String[] args) {
        SpringApplication.run(BackendMain.class, args);
    }
}
