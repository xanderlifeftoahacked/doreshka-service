package ru.doreshka.domain.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import ru.doreshka.domain.entity.ContestProblem;
import ru.doreshka.domain.entity.Problem;

import java.util.List;

@ApplicationScoped
public class ContestProblemRepository implements PanacheRepository<ContestProblem> {

    @WithSession
    public Uni<List<Problem>> findByContestId(Long contestId) {
        return find("contest.id", contestId)
                .list()
                .onItem().transform(contestProblems ->
                        contestProblems.stream()
                                .map(cp -> cp.problem)
                                .toList()
                );
    }


    @WithSession
    public Uni<Boolean> exists(Long contestId, Long problemId) {
        return count("contest.id = ?1 and problem.id = ?2", contestId, problemId)
                .onItem().transformToUni(count -> Uni.createFrom().item(count > 0));
    }

    @WithSession
    public Uni<ContestProblem> insert(ContestProblem cp) {
        return cp.persistAndFlush();
    }
}