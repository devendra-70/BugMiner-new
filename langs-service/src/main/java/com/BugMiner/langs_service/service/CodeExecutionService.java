package com.BugMiner.langs_service.service;

import com.BugMiner.langs_service.entity.ExecutionRequest;
import com.BugMiner.langs_service.entity.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Service
public class CodeExecutionService {

    private static final String JAVA_CONTAINER = "java-runner";
    private static final String CPP_CONTAINER = "cpp-runner";
    private static final String PYTHON_CONTAINER = "python-runner";

    public ExecutionResult executeCode(ExecutionRequest request) {
        String language = request.getLanguage().toLowerCase();
        String code = request.getCode();
        String input = request.getInput();

        try {
            Path tempDir = Files.createTempDirectory("exec-" + UUID.randomUUID());
            Path codeFilePath = createCodeFile(tempDir, language, code);
            Path inputFilePath = createInputFile(tempDir, input);

            String containerName = getContainerForLanguage(language);
            if (containerName == null) {
                return new ExecutionResult(false, null, "Unsupported language", 1);
            }

            if (!isContainerRunning(containerName)) {
                return new ExecutionResult(false, null, "Container down: " + containerName + " is not running.", 1);
            }

            String output = runInExistingContainer(containerName, codeFilePath, tempDir);
            return new ExecutionResult(true, output, null, 0);

        } catch (Exception e) {
            log.error("Execution failed", e);
            return new ExecutionResult(false, null, e.getMessage(), 1);
        }
    }

    private Path createCodeFile(Path dir, String language, String code) throws IOException {
        String filename = switch (language) {
            case "java" -> "Main.java";
            case "cpp" -> "main.cpp";
            case "python" -> "main.py";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
        Path filePath = dir.resolve(filename);
        Files.writeString(filePath, code, StandardOpenOption.CREATE);
        return filePath;
    }

    private Path createInputFile(Path dir, String input) throws IOException {
        Path filePath = dir.resolve("input.txt");
        Files.writeString(filePath, input != null ? input : "", StandardOpenOption.CREATE);
        return filePath;
    }

    private String getContainerForLanguage(String language) {
        return switch (language) {
            case "java" -> JAVA_CONTAINER;
            case "cpp" -> CPP_CONTAINER;
            case "python" -> PYTHON_CONTAINER;
            default -> null;
        };
    }

    private boolean isContainerRunning(String containerName) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("docker", "ps", "--format", "{{.Names}}");
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(containerName)) {
                    return true;
                }
            }
        }

        int exitCode = process.waitFor();
        return false;
    }

    private String runInExistingContainer(String containerName, Path codeFilePath, Path tempDir) throws IOException, InterruptedException {
        String fileName = codeFilePath.getFileName().toString();
        String containerFilePath = "/code/" + fileName;

        // Copy the code file into the container
        new ProcessBuilder("docker", "cp", codeFilePath.toString(), containerName + ":" + containerFilePath)
                .start().waitFor();

        String execCommand = switch (fileName) {
            case "Main.java" -> "javac " + containerFilePath + " && java -cp /code Main";
            case "main.cpp" -> "g++ " + containerFilePath + " -o /code/a.out && /code/a.out";
            case "main.py" -> "python3 " + containerFilePath;
            default -> throw new IllegalArgumentException("Unknown file type: " + fileName);
        };

        ProcessBuilder execBuilder = new ProcessBuilder("docker", "exec", containerName, "bash", "-c", execCommand);
        execBuilder.redirectErrorStream(true);
        Process execProcess = execBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(execProcess.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            execProcess.waitFor();
            return output.toString();
        }
    }
}

