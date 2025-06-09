package ru.doreshka.judging.storage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class FileStorageService {

    @ConfigProperty(name = "judging.storage.solutions.path")
    String solutionsPath;

    @ConfigProperty(name = "judging.storage.tests.path")
    String testsPath;

    public Path getSolutionPath(String sourcePath) {
        return Paths.get(solutionsPath, sourcePath);
    }

    public Path getSolutionDirectory(Long submissionId) {
        return Paths.get(solutionsPath, submissionId.toString());
    }

    public Path getTestDirectory(Long problemId) {
        return Paths.get(testsPath, problemId.toString());
    }

    public List<Path> getTestInputFiles(Long problemId) throws IOException {
        Path testDir = getTestDirectory(problemId);
        if (!Files.exists(testDir)) {
            throw new IOException("Test directory not found for problem: " + problemId);
        }

        try (Stream<Path> files = Files.list(testDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".in"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public Path getExpectedOutputFile(Path inputFile) {
        return Path.of(inputFile.toString().replace(".in", ".a"));
    }

    public boolean solutionFileExists(String sourcePath) {
        return Files.exists(getSolutionPath(sourcePath));
    }

    public String readFile(Path filePath) throws IOException {
        return Files.readString(filePath);
    }
} 