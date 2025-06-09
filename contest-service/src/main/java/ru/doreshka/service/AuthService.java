package ru.doreshka.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.entity.User;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.dto.auth.LoginRequest;
import ru.doreshka.dto.auth.RegisterRequest;
import ru.doreshka.exceptions.ConflictException;
import ru.doreshka.exceptions.WrongPasswordException;
import ru.doreshka.security.JWTGenerator;
import ru.doreshka.security.PasswordHasher;

import java.util.List;

@ApplicationScoped
public class AuthService {
    @Inject
    private UserRepository userRepository;

    @Inject
    private PasswordHasher passwordHasher;

    @Inject
    private JWTGenerator jwtGenerator;

    public Uni<User> registerUser(RegisterRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .onItem().ifNotNull().failWith(() -> new ConflictException("User already exists"))
                .flatMap(existing -> userRepository.insertUser(request.getUsername(),
                        passwordHasher.hash(request.getPassword())));
    }

    public Uni<String> loginUser(LoginRequest request) {
        return userRepository.getHash(request.getUsername())
                .onItem().transformToUni(hash -> {
                    if (!passwordHasher.verify(request.getPassword(), hash)) {
                        throw new WrongPasswordException("Invalid password");
                    }

                    return jwtGenerator.generateJwt(request.getUsername(), "user");
                });
    }

    public Uni<List<User>> getUsers() {
        return userRepository.getUsers();
    }


}
