package ru.doreshka.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class TestFileStorage {
    private static final String BASE_PATH = "problem_tests";

    public Path getTestDirectory(Long problemId) {
        return Paths.get(BASE_PATH, problemId.toString());
    }

    public void storeTestFile(Path directory, String filename, byte[] content)
            throws IOException {
        Files.createDirectories(directory);
        Path filePath = directory.resolve(sanitizeFilename(filename));
        Files.write(filePath, content);
    }

    public byte[] loadTestFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}