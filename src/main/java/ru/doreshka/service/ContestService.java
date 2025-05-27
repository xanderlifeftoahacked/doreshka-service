package ru.doreshka.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.entity.Contest;
import ru.doreshka.domain.entity.UserContestAccess;
import ru.doreshka.domain.repository.ContestRepository;
import ru.doreshka.domain.repository.UserRepository;
import ru.doreshka.dto.contest.AddContestRequest;

import java.util.List;

@ApplicationScoped
public class ContestService {
    @Inject
    UserRepository userRepository;

    @Inject
    ContestRepository contestRepository;


    public Uni<Contest> createContest(AddContestRequest request){
        return contestRepository.insertContest(
                new Contest(request.getContestName(), request.getDescription(),
                            request.getBeginTime(), request.getEndTime()));
    }

    public Uni<UserContestAccess> grantAccess(Long userId, Long contestId){
        return userRepository.addContestAccess(userId,contestId);
    }

    public Uni<List<Contest>> getAviableContests(Long userId){
        return userRepository.getAviableContests(userId);
    }

}
