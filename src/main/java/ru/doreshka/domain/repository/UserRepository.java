package ru.doreshka.domain.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import ru.doreshka.domain.entity.User;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    public Uni<User> insertUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(password);
        return user.persistAndFlush();
    }

    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public Uni<List<User>> getUsers(){
        return findAll().list();
    }
}
