package ru.doreshka.judging.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.doreshka.judging.dto.JudgeRequest;
import ru.doreshka.judging.dto.JudgeResult;
import ru.doreshka.judging.entity.Submission;
import ru.doreshka.judging.entity.Verdict;
import ru.doreshka.judging.storage.FileStorageService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class JudgingService {

    private final Executor judgingExecutor = Executors.newFixedThreadPool(16);
    @Inject
    FileStorageService fileStorageService;
    @ConfigProperty(name = "judging.compiler.command", defaultValue = "g++")
    String compilerCommand;

    @ConfigProperty(name = "judging.compiler.flags", defaultValue = "-std=c++17,-O2")
    List<String> compilerFlags;

    @ConfigProperty(name = "judging.executor.prlimit", defaultValue = "prlimit")
    String prlimitCommand;

    public String saveSolutionFile(Submission submission, String sourceCode) throws IOException {
        Path solutionDir = fileStorageService.getSolutionDirectory(submission.id);
        Files.createDirectories(solutionDir);

        Path solutionFile = solutionDir.resolve("solution.cpp");
        Files.write(solutionFile, sourceCode.getBytes());

        return submission.id + "/solution.cpp";
    }

    public void judgeAsync(JudgeRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                JudgeResult result = judge(request);
                updateSubmissionWithResults(request.getSubmissionId(), result);
            } catch (Exception e) {
                Log.errorf(e, "Async judging failed for submission %d", request.getSubmissionId());
                updateSubmissionVerdict(request.getSubmissionId(), Verdict.SystemError,
                        "Internal judging error: " + e.getMessage());
            }
        }, judgingExecutor);
    }

    public JudgeResult judge(JudgeRequest request) {
        Log.infof("Starting judging for submission %d", request.getSubmissionId());

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("submission-" + request.getSubmissionId());
            updateSubmissionVerdict(request.getSubmissionId(), Verdict.Pending);
            Path executable = compile(request, tempDir);
            return runAllTests(request, executable);

        } catch (CompilationException e) {
            Log.warnf("Compilation failed for submission %d: %s", request.getSubmissionId(), e.getMessage());
            updateSubmissionVerdict(request.getSubmissionId(), Verdict.CompilationError, e.getMessage());
            return new JudgeResult(request.getSubmissionId(), Verdict.CompilationError, 0, 0L, 0L, e.getMessage());

        } catch (Exception e) {
            Log.errorf(e, "System error during judging for submission %d", request.getSubmissionId());
            updateSubmissionVerdict(request.getSubmissionId(), Verdict.SystemError, e.getMessage());
            return new JudgeResult(request.getSubmissionId(), Verdict.SystemError, 0, 0L, 0L, e.getMessage());

        } finally {
            if (tempDir != null) {
                deleteTempDir(tempDir);
            }
        }
    }

    private Path compile(JudgeRequest request, Path workDir) throws CompilationException, IOException {
        Path sourcePath = fileStorageService.getSolutionPath(request.getSourcePath());
        if (!Files.exists(sourcePath)) {
            throw new CompilationException("Source file not found: " + request.getSourcePath());
        }

        Path executable = workDir.resolve("solution");

        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(compilerCommand);
        pb.command().addAll(compilerFlags);
        pb.command().add(sourcePath.toString());
        pb.command().add("-o");
        pb.command().add(executable.toString());

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorOutput = new String(process.getInputStream().readAllBytes());
                throw new CompilationException("Compilation failed: " + errorOutput);
            }

            return executable;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompilationException("Compilation interrupted");
        }
    }

    private JudgeResult runAllTests(JudgeRequest request, Path executable) throws IOException {
        List<Path> inputFiles = fileStorageService.getTestInputFiles(request.getProblemId());

        if (inputFiles.isEmpty()) {
            throw new IOException("No test files found for problem: " + request.getProblemId());
        }

        int passedTests = 0;
        long maxExecutionTime = 0;
        long maxMemoryUsed = 0;

        for (Path inputFile : inputFiles) {
            try {
                TestResult testResult = runSingleTest(request, executable, inputFile);
                passedTests++;
                maxExecutionTime = Math.max(maxExecutionTime, testResult.executionTime);
                maxMemoryUsed = Math.max(maxMemoryUsed, testResult.memoryUsed);

            } catch (TestFailedException e) {
                Log.debugf("Test failed for submission %d on test %s: %s",
                        request.getSubmissionId(), inputFile.getFileName(), e.verdict);

                updateSubmissionVerdict(request.getSubmissionId(), e.verdict, e.getMessage());
                return new JudgeResult(request.getSubmissionId(), e.verdict, passedTests,
                        maxExecutionTime, maxMemoryUsed, e.getMessage());
            }
        }

        updateSubmissionVerdict(request.getSubmissionId(), Verdict.Accepted);
        return new JudgeResult(request.getSubmissionId(), Verdict.Accepted, passedTests,
                maxExecutionTime, maxMemoryUsed, null);
    }

    private TestResult runSingleTest(JudgeRequest request, Path executable, Path inputFile)
            throws TestFailedException, IOException {

        Path outputFile = Files.createTempFile("output", ".txt");
        Path errorFile = Files.createTempFile("error", ".txt");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    prlimitCommand,
                    "--cpu=" + (request.getTimeLimit() / 1000 + 1),
                    "--as=" + (request.getMemoryLimit() * 1024L * 1024L),
                    executable.toString()
            );

            pb.redirectInput(inputFile.toFile());
            pb.redirectOutput(outputFile.toFile());
            pb.redirectError(errorFile.toFile());

            long startTime = System.nanoTime();
            Process process = pb.start();

            boolean finished = process.waitFor(request.getTimeLimit(), TimeUnit.MILLISECONDS);
            long executionTime = (System.nanoTime() - startTime) / 1_000_000;

            if (!finished || executionTime > request.getTimeLimit()) {
                process.destroyForcibly();
                throw new TestFailedException(Verdict.TimeLimitExceeded, "Time limit exceeded");
            }

            int exitCode = process.exitValue();
            String errorOutput = Files.readString(errorFile);

            if (exitCode != 0) {
                if (errorOutput.contains("exceeded")) {
                    throw new TestFailedException(Verdict.MemoryLimitExceeded, "Memory limit exceeded");
                } else {
                    throw new TestFailedException(Verdict.RuntimeError, "Runtime error: " + errorOutput);
                }
            }

            String actualOutput = Files.readString(outputFile);
            String expectedOutput = fileStorageService.readFile(fileStorageService.getExpectedOutputFile(inputFile));

            if (!normalize(actualOutput).equals(normalize(expectedOutput))) {
                throw new TestFailedException(Verdict.WrongAnswer, "Wrong answer");
            }

            long estimatedMemoryMB = 1L;
            return new TestResult(executionTime, estimatedMemoryMB);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestFailedException(Verdict.SystemError, "Test execution interrupted");

        } finally {
            Files.deleteIfExists(outputFile);
            Files.deleteIfExists(errorFile);
        }
    }

    @Transactional
    protected void updateSubmissionVerdict(Long submissionId, Verdict verdict) {
        updateSubmissionVerdict(submissionId, verdict, null);
    }

    @Transactional
    protected void updateSubmissionVerdict(Long submissionId, Verdict verdict, String errorMessage) {
        Submission submission = Submission.findById(submissionId);
        if (submission != null) {
            submission.verdict = verdict;
            submission.errorMessage = errorMessage;
            submission.persist();
        }
    }

    @Transactional
    protected void updateSubmissionWithResults(Long submissionId, JudgeResult result) {
        Submission submission = Submission.findById(submissionId);
        if (submission != null) {
            submission.verdict = result.getVerdict();
            submission.curTest = result.getPassedTests();
            submission.executionTime = result.getExecutionTime();
            submission.memoryUsed = result.getMemoryUsed();
            submission.errorMessage = result.getErrorMessage();
            submission.persist();
            Log.infof("Updated submission %d: verdict=%s, tests=%d, time=%dms, memory=%dMB",
                    submissionId, result.getVerdict(), result.getPassedTests(),
                    result.getExecutionTime(), result.getMemoryUsed());
        }
    }

    private void deleteTempDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            Log.error("Failed to cleanup temp directory: " + dir, e);
        }
    }

    private String normalize(String output) {
        return output.trim()
                .replaceAll("\\r\\n?", "\n")
                .replaceAll("\\s+", " ");
    }

    private static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }

    private static class TestFailedException extends Exception {
        final Verdict verdict;

        public TestFailedException(Verdict verdict, String message) {
            super(message);
            this.verdict = verdict;
        }
    }

    private static class TestResult {
        final long executionTime;
        final long memoryUsed;

        public TestResult(long executionTime, long memoryUsed) {
            this.executionTime = executionTime;
            this.memoryUsed = memoryUsed;
        }
    }
} 