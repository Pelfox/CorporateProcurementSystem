package com.team.corporate.backend.services.responses;

public record LoginResponse(Status status, TokenPairResponse tokens, PasswordChangeTokenResponse passwordChange) {
    public enum Status { AUTHENTICATED, PASSWORD_CHANGE_REQUIRED }

    public LoginResponse {
        if (status == null || (status == Status.AUTHENTICATED && (tokens == null || passwordChange != null))
                || (status == Status.PASSWORD_CHANGE_REQUIRED && (passwordChange == null || tokens != null))) {
            throw new IllegalArgumentException("Несогласованный результат входа.");
        }
    }

    @Override
    public String toString() {
        return "LoginResponse[status=" + status + ", credentials=<redacted>]";
    }
}
