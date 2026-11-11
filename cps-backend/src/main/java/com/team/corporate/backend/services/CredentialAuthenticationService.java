package com.team.corporate.backend.services;

import com.team.corporate.backend.security.ProcurementUserDetails;
import com.team.corporate.backend.services.responses.AuthenticatedUser;
import com.team.corporate.backend.validation.PasswordPolicy;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CredentialAuthenticationService {
    public static final String INVALID_CREDENTIALS = "Неверный логин или пароль.";
    private final AuthenticationManager authenticationManager;

    public CredentialAuthenticationService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public AuthenticatedUser authenticate(String username, String password) {
        if (!PasswordPolicy.isBcryptCompatible(password)) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        var request = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        try {
            var authentication = authenticationManager.authenticate(request);
            var principal = (ProcurementUserDetails) authentication.getPrincipal();
            return new AuthenticatedUser(principal.getId(), principal.getUsername(), principal.getRole(),
                    principal.isMustChangePassword(), principal.getTemporaryPasswordExpiresAt());
        } catch (BadCredentialsException | AccountStatusException error) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        } finally {
            request.eraseCredentials();
        }
    }
}
