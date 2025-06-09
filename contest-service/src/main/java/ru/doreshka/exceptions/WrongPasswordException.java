package ru.doreshka.exceptions;

public class WrongPasswordException extends LoginException {
    public WrongPasswordException(String message) {
        super(message);
    }
}
