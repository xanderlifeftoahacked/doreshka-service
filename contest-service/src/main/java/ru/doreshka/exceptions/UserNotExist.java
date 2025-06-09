package ru.doreshka.exceptions;

public class UserNotExist extends LoginException {
    public UserNotExist(String message) {
        super(message);
    }
}
