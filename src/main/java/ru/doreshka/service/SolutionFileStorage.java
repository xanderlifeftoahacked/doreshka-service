package ru.doreshka.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class SolutionFileStorage {
    public static final String BASE_PATH = "solutions";
    public static final String DEFAULT_FILENAME = "solution.cpp";

    public Path getSolutionDirectory(Long submissionId) {
        return Paths.get(BASE_PATH, submissionId.toString());
    }

    public void storeSolutionFile(Path directory, byte[] content) throws IOException {
        Files.createDirectories(directory);
        Path filePath = directory.resolve(DEFAULT_FILENAME);
        Files.write(filePath, content);
    }

    public byte[] loadSolutionFile(String sourcePath) throws IOException {
        return Files.readAllBytes(Paths.get(sourcePath));
    }

    public byte[] loadSolutionFile(Long submissionId) throws IOException {
        Path filePath = getSolutionDirectory(submissionId).resolve(DEFAULT_FILENAME);
        return Files.readAllBytes(filePath);
    }
}
