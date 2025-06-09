package ru.doreshka.domain.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.exceptions.DBException;

@ApplicationScoped
public class ProblemRepository implements PanacheRepository<Problem> {

    @WithSession
    public Uni<Problem> findById(Long id) {
        return Problem.findById(id);
    }

    @WithSession
    public Uni<Problem> insertProblem(Problem problem) {
        return Panache.<Problem>withTransaction(problem::persistAndFlush)
                .onFailure(PersistenceException.class)
                .transform(ex ->
                        new DBException("Problem with name '" + problem.getTitle() + "' already exists")
                );
    }

    @WithTransaction
    public Uni<Void> clear() {
        return deleteAll().replaceWithVoid();
    }
}
