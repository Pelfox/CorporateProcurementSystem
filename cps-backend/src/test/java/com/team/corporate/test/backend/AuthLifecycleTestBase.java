package com.team.corporate.test.backend;

import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.repositories.AuthSessionRepository;
import com.team.corporate.backend.repositories.RefreshTokenRepository;
import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.security.RefreshTokenCodec;
import com.team.corporate.backend.security.SessionValidator;
import com.team.corporate.backend.services.AuthService;
import com.team.corporate.backend.services.UserManagementService;
import com.team.corporate.backend.services.responses.LoginResponse;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.Map;
import java.util.UUID;

@Import(AuthTestBeans.class)
abstract class AuthLifecycleTestBase extends BackendIntegrationTest {
    static final String PASSWORD = "A long permanent test phrase 57!";
    static final String NEXT_PASSWORD = "Another permanent test phrase 81!";
    @Autowired protected AuthService auth;
    @Autowired protected UserManagementService management;
    @Autowired protected UserJpaRepository users;
    @Autowired protected AuthSessionRepository sessions;
    @Autowired protected RefreshTokenRepository tokens;
    @Autowired protected RefreshTokenCodec refreshCodec;
    @Autowired protected SessionValidator validator;
    @Autowired protected JwtDecoder decoder;
    @Autowired protected AuthTestBeans.MutableClock clock;
    @Autowired protected AuthTestBeans.FailingEncoder signer;

    @BeforeEach
    void resetAuthFixtures() {
        clock.now = AuthTestBeans.NOW;
        signer.fail = false;
    }

    LoginResponse limited(String name) {
        var created = management.createUser(name, UserRole.USER);
        return auth.login(name, created.temporaryPassword());
    }

    TokenPairResponse activate(String name) {
        return auth.initialPassword(limited(name).passwordChange().token(), new InitialPasswordRequest(PASSWORD));
    }

    UUID sessionId(String token) {
        return UUID.fromString(decoder.decode(token).getClaimAsString("sid"));
    }

    String sign(Map<String, Object> claims) {
        var values = JwtClaimsSet.builder().claims(map -> map.putAll(claims)).build();
        return signer.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), values))
                .getTokenValue();
    }

    void assertRejected(Jwt jwt) {
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.oauth2.core.OAuth2AuthenticationException.class, () -> validator.validate(jwt));
    }
}
