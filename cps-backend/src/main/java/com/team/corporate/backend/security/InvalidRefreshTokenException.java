package com.team.corporate.backend.security;

import org.springframework.security.authentication.BadCredentialsException;

/** A replay rejection may follow a committed session revocation. */
public final class InvalidRefreshTokenException extends BadCredentialsException {
    public InvalidRefreshTokenException() {
        super("Недействительный refresh-токен.");
    }
}
