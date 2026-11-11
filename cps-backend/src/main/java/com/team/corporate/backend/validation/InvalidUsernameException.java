package com.team.corporate.backend.validation;

public class InvalidUsernameException extends IllegalArgumentException {
    public InvalidUsernameException() {
        super("Логин должен содержать от 1 до 64 символов: латиницу, цифры, точку, дефис или подчёркивание.");
    }
}
