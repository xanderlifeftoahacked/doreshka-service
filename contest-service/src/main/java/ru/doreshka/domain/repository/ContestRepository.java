package ru.doreshka.domain.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import ru.doreshka.domain.entity.Contest;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.exceptions.DBException;

import java.util.List;

@ApplicationScoped
public class ContestRepository implements PanacheRepository<Contest> {

    @WithSession
    public Uni<Contest> findById(Long id) {
        return Contest.findById(id);
    }

    @WithSession
    public Uni<List<Contest>> listAll() {
        return Contest.listAll();
    }

    @WithSession
    public Uni<Contest> insertContest(Contest contest) {
        return Panache.<Contest>withTransaction(contest::persistAndFlush)
                .onFailure(PersistenceException.class)
                .transform(ex ->
                        new DBException("Contest with name '" + contest.getContestName() + "' already exists")
                );
    }

    @WithSession
    public Uni<Problem> insertProblem(Contest contest, Problem problem, String shortName) {
        return Panache.withTransaction(() -> {
            contest.addProblem(problem, shortName);
            return contest.persistAndFlush();
        });
    }

    @WithTransaction
    public Uni<Void> clear() {
        return deleteAll().replaceWithVoid();
    }

}
