package ru.doreshka.security;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.repository.UserRepository;

import java.time.Duration;

@ApplicationScoped
public class JWTGenerator {
    @Inject
    private UserRepository userRepository;

    public Uni<String> generateJwt(String username, String role) {
        return userRepository.
                getUserId(username)
                .onItem()
                .transformToUni(userId ->
                        Uni.createFrom().item(() ->
                                Jwt.issuer("doreshka-issuer")
                                        .upn(userId.toString())
                                        .groups(role)
                                        .expiresIn(Duration.ofHours(24))
                                        .sign()
                        )
                );
    }
}
