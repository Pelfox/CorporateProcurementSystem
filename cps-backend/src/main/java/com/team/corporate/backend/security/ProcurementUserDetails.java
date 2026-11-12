package com.team.corporate.backend.security;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class ProcurementUserDetails implements UserDetails, CredentialsContainer {
    private final UUID id;
    private final String username;
    private final UserRole role;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final Instant temporaryPasswordExpiresAt;
    private final boolean credentialsNonExpired;
    private String passwordHash;

    public ProcurementUserDetails(User user, Instant now) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getUserRole();
        this.enabled = user.isEnabled();
        this.mustChangePassword = user.isMustChangePassword();
        this.temporaryPasswordExpiresAt = user.getTemporaryPasswordExpiresAt();
        this.credentialsNonExpired = !mustChangePassword
                || (temporaryPasswordExpiresAt != null && temporaryPasswordExpiresAt.isAfter(now));
        this.passwordHash = user.getPasswordHash();
    }

    public UUID getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public Instant getTemporaryPasswordExpiresAt() {
        return temporaryPasswordExpiresAt;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public String toString() {
        return "ProcurementUserDetails[id=" + id + ", username=" + username + ", credentials=<redacted>]";
    }
}
