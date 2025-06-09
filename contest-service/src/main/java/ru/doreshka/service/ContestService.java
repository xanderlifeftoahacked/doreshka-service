package ru.doreshka.service;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.entity.Contest;
import ru.doreshka.domain.entity.ContestProblem;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.entity.UserContestAccess;
import ru.doreshka.domain.repository.ContestProblemRepository;
import ru.doreshka.domain.repository.ContestRepository;
import ru.doreshka.domain.repository.ProblemRepository;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.dto.contest.AddContestRequest;
import ru.doreshka.dto.contest.AddProblemToContestRequest;

import java.util.List;

@ApplicationScoped
public class ContestService {
    @Inject
    UserRepository userRepository;

    @Inject
    ContestRepository contestRepository;

    @Inject
    ProblemRepository problemRepository;

    @Inject
    ContestProblemRepository contestProblemRepository;


    public Uni<Contest> createContest(AddContestRequest request) {
        return contestRepository.insertContest(
                new Contest(request.getContestName(), request.getDescription(),
                        request.getBeginTime(), request.getEndTime()));
    }

    public Uni<List<Contest>> getContests() {
        return contestRepository.listAll();
    }

    public Uni<List<Problem>> getProblems(Long contestId) {
        return contestProblemRepository.findByContestId(contestId);
    }

    public Uni<UserContestAccess> grantAccess(Long userId, Long contestId) {
        return userRepository.addContestAccess(userId, contestId);
    }

    public Uni<List<Contest>> getAviableContests(Long userId) {
        return userRepository.getAviableContests(userId);
    }

    @WithSession
    public Uni<UserContestAccess> checkUserAccessToContest(Long userId, Long contestId) {
        return UserContestAccess.find(
                        "SELECT ua FROM UserContestAccess ua " +
                                "WHERE ua.contest.id = ?1 AND ua.user.id = ?2",
                        contestId, userId
                )
                .firstResult();
    }

    public Uni<ContestProblem> addProblemToContest(Long contestId, AddProblemToContestRequest request) {
        ContestProblem cp = new ContestProblem();
        return contestRepository.findById(contestId)
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Contest not found"))
                .onItem().transformToUni(contest -> {
                            cp.contest = contest;
                            return problemRepository.findById(request.problemId());
                        }
                )
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Problem not found"))
                .onItem().transformToUni(problem -> {
                    cp.problem = problem;
                    return contestProblemRepository.exists(contestId, request.problemId());
                })
                .onItem().transformToUni(exists -> {
                            if (exists) {
                                return Uni.createFrom().failure(new IllegalArgumentException("Problem already added"));
                            }

                            cp.shortName = request.shortName();
                            return contestProblemRepository.insert(cp);
                        }
                );
    }
}
