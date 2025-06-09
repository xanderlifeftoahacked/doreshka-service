package ru.doreshka.domain.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import ru.doreshka.domain.entity.Contest;
import ru.doreshka.domain.entity.User;
import ru.doreshka.domain.entity.UserContestAccess;
import ru.doreshka.exceptions.DBException;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Uni<User> insertUser(String username, String passwordHash) {
        return Panache.withTransaction(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordHash);
            return user.persistAndFlush();
        });
    }

    @WithSession
    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    @WithTransaction
    public Uni<Void> clear() {
        return deleteAll().replaceWithVoid();

    }

    @WithSession
    public Uni<List<User>> getUsers() {
        return User.listAll();
    }

    @WithSession
    public Uni<Long> getUserId(String username) {
        return find("username", username).firstResult().onItem().ifNull().failWith(new DBException("user doesn't exist")).onItem().transform(user -> user.id);
    }


    @WithSession
    public Uni<String> getHash(String username) {
        return find("username", username).firstResult().onItem().ifNull().failWith(new DBException("user doesn't exist")).onItem().transform(user -> user.getPasswordHash());
    }

    public Uni<UserContestAccess> addContestAccess(Long userId, Long contestId) {
        return Panache.withTransaction(() ->
                User.<User>findById(userId)
                        .onItem().ifNull().failWith(() -> new DBException("User not found"))
                        .flatMap(user ->
                                Contest.<Contest>findById(contestId)
                                        .onItem().ifNull().failWith(() -> new DBException("Contest not found"))
                                        .map(contest -> {
                                            UserContestAccess access = new UserContestAccess();
                                            access.setUser(user);
                                            access.setContest(contest);
                                            return access;
                                        })
                        )
                        .flatMap(access -> access.persist())
        );
    }

    public Uni<List<Contest>> getAviableContests(Long userId) {
        String query = """
                SELECT c FROM Contest c
                WHERE c.id IN (
                SELECT uca.contest.id FROM UserContestAccess uca
                WHERE uca.user.id = :userId
                )
                """;
        return Contest.find(query, Parameters.with("userId", userId)).list();

    }

    public Uni<Long> removeContestAccess(Long userId, Long contestId) {
        return Panache.withTransaction(() ->
                UserContestAccess.delete("user.id = ?1 and contest.id = ?2", userId, contestId)
        );
    }

    @WithSession
    public Uni<List<Contest>> getAccessibleContests(Long userId) {
        return UserContestAccess.<UserContestAccess>find("user.id", userId)
                .list()
                .map(accesses ->
                        accesses.stream()
                                .map(UserContestAccess::getContest)
                                .collect(Collectors.toList())
                );
    }

    @WithSession
    public Uni<Boolean> hasContestAccess(Long userId, Long contestId) {
        return UserContestAccess.count("user.id = ?1 and contest.id = ?2", userId, contestId)
                .map(count -> count > 0);
    }

}
