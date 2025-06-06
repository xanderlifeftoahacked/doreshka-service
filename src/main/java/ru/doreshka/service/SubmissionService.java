package ru.doreshka.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.entity.Submission;
import ru.doreshka.domain.entity.User;
import ru.doreshka.domain.entity.Verdict;
import ru.doreshka.service.SolutionFileStorage;

import java.io.IOException;
import java.nio.file.Path;

@ApplicationScoped
public class SubmissionService {

    @Inject
    SolutionFileStorage solutionFileStorage;

    public Uni<Submission> createSubmissionReactive(Problem problem, User user, byte[] solutionCode) {
        Submission submission = new Submission();
        submission.problem = problem;
        submission.user = user;
        submission.verdict = Verdict.Pending;

        return submission.<Submission>persist()
                .onItem().transformToUni(persisted -> {
                    try {
                        Path solutionDir = solutionFileStorage.getSolutionDirectory(persisted.id);
                        solutionFileStorage.storeSolutionFile(solutionDir, solutionCode);

                        persisted.sourcePath = solutionDir.resolve(SolutionFileStorage.DEFAULT_FILENAME)
                                .toAbsolutePath().toString();

                        return persisted.<Submission>persist();
                    } catch (IOException e) {
                        persisted.verdict = Verdict.SystemError;
                        return persisted.<Submission>persistAndFlush()
                                .onItem().invoke(() -> Log.error("Failed to store solution file", e));
                    }
                });
    }
}
