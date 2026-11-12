package com.team.corporate.test.backend;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

@TestConfiguration(proxyBeanMethods = false)
public class AuthTestBeans {
    static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Bean
    @Primary
    MutableClock authClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    FailingEncoder failingEncoder(@Qualifier("jwtEncoder") JwtEncoder delegate) {
        return new FailingEncoder(delegate);
    }

    static final class MutableClock extends Clock {
        volatile Instant now = NOW;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static final class FailingEncoder implements JwtEncoder {
        private final JwtEncoder delegate;
        volatile boolean fail;

        FailingEncoder(JwtEncoder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Jwt encode(JwtEncoderParameters parameters) {
            if (fail) {
                throw new JwtEncodingException("Test signing failure");
            }
            return delegate.encode(parameters);
        }
    }
}
