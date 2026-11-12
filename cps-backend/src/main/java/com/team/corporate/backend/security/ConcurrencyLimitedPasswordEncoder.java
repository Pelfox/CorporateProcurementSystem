package com.team.corporate.backend.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/** Bounds both verification and account provisioning work without an unbounded waiting queue. */
public final class ConcurrencyLimitedPasswordEncoder implements PasswordEncoder {
    private final PasswordEncoder delegate;
    private final Semaphore permits;

    public ConcurrencyLimitedPasswordEncoder(PasswordEncoder delegate, int maximum) {
        this.delegate = delegate;
        this.permits = new Semaphore(maximum);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return limited(() -> delegate.encode(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return limited(() -> delegate.matches(rawPassword, encodedPassword));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }

    private <T> T limited(Supplier<T> work) {
        if (!permits.tryAcquire()) {
            throw new RateLimitException(1);
        }
        try {
            return work.get();
        } finally {
            permits.release();
        }
    }
}
