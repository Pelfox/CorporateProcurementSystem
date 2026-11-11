package com.team.corporate.backend.security;

import com.team.corporate.core.entities.SessionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final SessionValidator validator;

    public SessionJwtAuthenticationConverter(SessionValidator validator) {
        this.validator = validator;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var principal = validator.validate(jwt);
        var authorities = principal.type() == SessionType.FULL
                ? List.of(new SimpleGrantedAuthority("SESSION_FULL"), new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
                : List.of(new SimpleGrantedAuthority("SESSION_PASSWORD_CHANGE"));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }
}
