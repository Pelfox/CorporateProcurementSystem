package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.LoginRequest;
import com.team.corporate.backend.dto.RefreshTokenRequest;
import com.team.corporate.backend.dto.TokenResponse;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.services.AuthenticationService;
import com.team.corporate.backend.services.AccountsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Авторизация", description = "Вход, обновление токенов, завершение сессии и смена пароля. Для login, refresh и logout access JWT не требуется.")
public class AuthenticationController {
    private final AccountsService accounts;
    private final AuthenticationService authenticationService;

    public AuthenticationController(AccountsService accounts, AuthenticationService authenticationService) {
        this.accounts = accounts;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Войти по логину и паролю", description = "Возвращает access JWT, refresh-токен и признак mustChangePassword. Ошибочные данные — 401, превышение лимита — 429.")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authenticationService.login(request, http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Обновить пару токенов", description = "Передайте refreshToken в теле. Замените оба токена в клиенте; повторное использование прежнего refresh-токена отзывает сессию.")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return authenticationService.refresh(request, http.getRemoteAddr());
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Завершить сессию", description = "Передайте refreshToken в теле. Повторный выход безопасен; access JWT не требуется.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        authenticationService.logout(request, http.getRemoteAddr());
    }

    @GetMapping("/me")
    @Operation(summary = "Получить свой профиль", description = "Доступно также при обязательной смене временного пароля.")
    public UserResponse currentUser(Authentication authentication) {
        return accounts.currentUser(authentication.getName());
    }

    @PutMapping("/password")
    @Operation(summary = "Сменить свой пароль", description = "Нужен текущий пароль. Новый пароль должен отличаться от текущего. Успех отзывает все сессии; войдите заново.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest http
    ) {
        authenticationService.changePassword(authentication.getName(), request, http.getRemoteAddr());
    }
}
