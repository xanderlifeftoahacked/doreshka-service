package ru.doreshka.config;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class AdminTokenGenerator {
    void onStart(@Observes StartupEvent ev) {
        try {
            String token = Jwt.issuer("doreshka-issuer")
                    .upn("admin@system")
                    .groups("admin")
                    .expiresIn(Duration.ofDays(365))
                    .sign();

            System.out.println("====================================");
            System.out.println("ADMIN TOKEN: " + token);
            System.out.println("====================================");
        } catch (Exception e) {
            System.err.println("Failed to generate admin token: " + e.getMessage());
        }
    }
}
