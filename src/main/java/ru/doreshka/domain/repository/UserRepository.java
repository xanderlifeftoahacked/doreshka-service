package ru.doreshka.domain.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import ru.doreshka.domain.entity.User;
import ru.doreshka.exceptions.DBException;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    @WithSession
    public Uni<User> insertUser(String username, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        return user.persistAndFlush();
    }

    @WithSession
    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    @WithSession
    public Uni<List<User>> getUsers() {
        return User.listAll();
    }

    @WithSession
    public Uni<Long> getUserId(String username) {
        return find("username", username)
                .firstResult()
                .onItem().ifNull().failWith(new DBException("user doesn't exist"))
                .onItem().transform(user -> user.id);
    }


    @WithSession
    public Uni<String> getHash(String username) {
        return find("username", username)
                .firstResult()
                .onItem().ifNull().failWith(new DBException("user doesn't exist"))
                .onItem()
                .transform(user -> user.getPasswordHash());
    }

}
