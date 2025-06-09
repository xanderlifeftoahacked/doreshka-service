package ru.doreshka.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ru.doreshka.domain.entity.Problem;
import ru.doreshka.domain.entity.ProblemTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@ApplicationScoped
public class TestFileService {

    @Inject
    TestFileStorage storage;

    public Uni<List<ProblemTest>> processProblemTests(Long problemId, List<FileUpload> files) {
        return Panache.withTransaction(() ->
                Problem.<Problem>findById(problemId)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Problem not found"))
                        .flatMap(problem -> {
                            Context vertxContext = Vertx.currentContext();
                            Map<String, FilePair> filePairs = groupFilesIntoPairs(files);

                            if (filePairs.values().stream().anyMatch(FilePair::isInvalid)) {
                                return Uni.createFrom().failure(new BadRequestException("Invalid file pairs"));
                            }

                            List<Uni<ProblemTest>> tasks = filePairs.values().stream()
                                    .filter(FilePair::isValid)
                                    .map(pair -> processFilePair(problem, pair, vertxContext))
                                    .collect(Collectors.toList());

                            return Uni.join().all(tasks).andCollectFailures();
                        })
        );
    }

    private Map<String, FilePair> groupFilesIntoPairs(List<FileUpload> files) {
        Map<String, FilePair> pairs = new HashMap<>();

        for (FileUpload file : files) {
            String fileName = file.fileName();
            if (!fileName.matches(".*\\.(in|a)$")) continue;

            String baseName = fileName.replaceAll("\\.(in|a)$", "");
            FilePair pair = pairs.computeIfAbsent(baseName, k -> new FilePair());
            pair.baseName = baseName;

            if (fileName.endsWith(".in")) {
                pair.inputFile = file;
            } else if (fileName.endsWith(".a")) {
                pair.answerFile = file;
            }
        }
        return pairs;
    }

    private Uni<ProblemTest> processFilePair(Problem problem, FilePair pair, Context vertxContext) {
        return Uni.combine().all().unis(
                        readFileContent(pair.inputFile),
                        readFileContent(pair.answerFile))
                .asTuple()
                .flatMap(tuple -> createProblemTest(
                        problem,
                        pair.baseName,
                        tuple.getItem1(),
                        tuple.getItem2(),
                        vertxContext));
    }

    private Uni<byte[]> readFileContent(FileUpload file) {
        return Uni.createFrom().item(() -> {
                    try {
                        return Files.readAllBytes(Paths.get(file.uploadedFile().toUri()));
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file: " + file.fileName(), e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Uni<ProblemTest> createProblemTest(Problem problem, String baseName,
                                               byte[] input, byte[] answer, Context vertxContext) {
        return Uni.createFrom().item(() -> {
                    Path problemDir = storage.getTestDirectory(problem.id);
                    int testIndex = problem.tests.size() + 1;

                    String inputName = sanitizeFilename(baseName + ".in");
                    String answerName = sanitizeFilename(baseName + ".a");

                    try {
                        storage.storeTestFile(problemDir, inputName, input);
                        storage.storeTestFile(problemDir, answerName, answer);

                        ProblemTest test = new ProblemTest();
                        test.problem = problem;
                        test.inputPath = inputName;
                        test.answerPath = answerName;
                        test.orderIndex = testIndex;
                        return test;
                    } catch (IOException e) {
                        throw new RuntimeException("File storage failed", e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .emitOn(new VertxContextExecutor(vertxContext))
                .flatMap(test -> test.<ProblemTest>persist()
                        .onItem().invoke(persisted -> {
                            problem.tests.add(persisted);
                        })
                );
    }

    private String sanitizeFilename(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_.-]", "_")
                .replaceAll("_+", "_");
    }

    private static class FilePair {
        FileUpload inputFile;
        FileUpload answerFile;
        String baseName;

        boolean isValid() {
            return inputFile != null && answerFile != null;
        }

        boolean isInvalid() {
            return !isValid();
        }
    }

    private static class VertxContextExecutor implements Executor {
        private final Context context;

        public VertxContextExecutor(Context context) {
            this.context = context;
        }

        @Override
        public void execute(Runnable command) {
            context.runOnContext(v -> command.run());
        }
    }
}