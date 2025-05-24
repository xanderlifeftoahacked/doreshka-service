package ru.doreshka.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
}
