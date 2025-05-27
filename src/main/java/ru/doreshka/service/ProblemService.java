package ru.doreshka.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.entity.Contest;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.entity.UserContestAccess;
import ru.doreshka.domain.repository.ContestRepository;
import ru.doreshka.domain.repository.ProblemRepository;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.dto.contest.AddContestRequest;
import ru.doreshka.dto.problem.AddProblemRequest;

import java.util.List;

@ApplicationScoped
public class ProblemService {
    @Inject
    ProblemRepository problemRepository;


    public Uni<Problem> createProblem(AddProblemRequest request) {
        return problemRepository.insertProblem(
                new Problem(request.getTitle(), request.getDescription(),
                        request.getTimeLimit(), request.getMemoryLimit()));
    }

}
