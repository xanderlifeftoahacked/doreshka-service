package ru.doreshka.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.repository.ProblemRepository;
import ru.doreshka.dto.problem.AddProblemRequest;
import ru.doreshka.exceptions.DBException;

@ApplicationScoped
public class ProblemService {
    @Inject
    ProblemRepository problemRepository;


    public Uni<Problem> createProblem(AddProblemRequest request) {
        return problemRepository.insertProblem(
                new Problem(request.getTitle(), request.getDescription(),
                        request.getTimeLimit(), request.getMemoryLimit()));
    }

    public Uni<Problem> getProblem(Long problemId) {
        return problemRepository.findById(problemId)
                .onItem()
                .ifNull()
                .failWith(new DBException("problem not found"));
    }

}
