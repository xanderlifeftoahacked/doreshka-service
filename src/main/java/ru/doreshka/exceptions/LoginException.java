package ru.doreshka.exceptions;

public class LoginException extends RuntimeException {
    public LoginException(String message) {
        super(message);
    }
}
