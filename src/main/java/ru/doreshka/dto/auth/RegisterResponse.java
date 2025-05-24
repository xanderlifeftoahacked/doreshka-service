package ru.doreshka.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.doreshka.domain.entity.User;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long id,
        String username,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static RegisterResponse fromUser(User user) {
        return new RegisterResponse(
                user.id,
                user.getUsername(),
                user.getCreatedAt()
        );
    }
}