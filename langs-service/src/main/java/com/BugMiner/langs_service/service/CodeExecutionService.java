package com.BugMiner.langs_service.service;

import com.BugMiner.langs_service.entity.ExecutionRequest;
import com.BugMiner.langs_service.entity.ExecutionResult;
import com.BugMiner.langs_service.entity.TestCase;
import com.BugMiner.langs_service.entity.TestCaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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
        List<TestCase> testCases = request.getTestCases();

        try {
            Path tempDir = Files.createTempDirectory("exec-" + UUID.randomUUID());
            Path codeFilePath = createCodeFile(tempDir, language, code);

            String containerName = getContainerForLanguage(language);
            if (containerName == null) {
                return new ExecutionResult(false, null, "Unsupported language", 1);
            }

            if (!isContainerRunning(containerName)) {
                return new ExecutionResult(false, null, "Container down: " + containerName + " is not running.", 1);
            }

            List<TestCaseResult> results = new ArrayList<>();

            for (TestCase testCase : testCases) {
                Path inputFilePath = createInputFile(tempDir, testCase.getInput());
                String output = runInExistingContainer(containerName, codeFilePath, inputFilePath);

                boolean passed = output.trim().equals(testCase.getExpectedOutput().trim());
                results.add(new TestCaseResult(testCase.getInput(), testCase.getExpectedOutput(), output, passed));
            }

            boolean allPassed = results.stream().allMatch(TestCaseResult::isPassed);
            return new ExecutionResult(allPassed, results, null, 0);

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

        process.waitFor();
        return false;
    }

    private String runInExistingContainer(String containerName, Path codeFilePath, Path inputFilePath) throws IOException, InterruptedException {
        String fileName = codeFilePath.getFileName().toString();

        // Create unique filenames to avoid conflicts between concurrent executions
        String uniqueId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String containerCodePath = "/code/" + uniqueId + "_" + fileName;
        String containerInputPath = "/code/" + uniqueId + "_input.txt";
        String containerInputFilename = uniqueId + "_input.txt";

        try {
            // Copy the code file into the container
            Process copyCodeProcess = new ProcessBuilder("docker", "cp", codeFilePath.toString(), containerName + ":" + containerCodePath).start();
            int copyCodeResult = copyCodeProcess.waitFor();
            if (copyCodeResult != 0) {
                log.error("Failed to copy code file to container. Exit code: {}", copyCodeResult);
                return "Error: Failed to copy code file to container";
            }

            // Copy the input file into the container
            Process copyInputProcess = new ProcessBuilder("docker", "cp", inputFilePath.toString(), containerName + ":" + containerInputPath).start();
            int copyInputResult = copyInputProcess.waitFor();
            if (copyInputResult != 0) {
                log.error("Failed to copy input file to container. Exit code: {}", copyInputResult);
                return "Error: Failed to copy input file to container";
            }

            // Build the execution command with unique filenames and timeout
            String execCommand = switch (fileName) {
                case "Main.java" -> String.format("cd /code && timeout 10s bash -c 'javac %s && java Main < %s'", uniqueId + "_" + fileName, containerInputFilename);
                case "main.cpp" -> String.format("cd /code && timeout 10s bash -c 'g++ %s -o %s_a.out && ./%s_a.out < %s'", uniqueId + "_" + fileName, uniqueId, uniqueId, containerInputFilename);
                case "main.py" -> String.format("cd /code && timeout 10s python3 %s < %s", uniqueId + "_" + fileName, containerInputFilename);
                default -> throw new IllegalArgumentException("Unknown file type: " + fileName);
            };

            log.debug("Executing command in container {}: {}", containerName, execCommand);

            // Execute the command with timeout
            ProcessBuilder execBuilder = new ProcessBuilder("docker", "exec", containerName, "bash", "-c", execCommand);
            execBuilder.redirectErrorStream(true);
            Process execProcess = execBuilder.start();

            StringBuilder output = new StringBuilder();
            boolean finished = false;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(execProcess.getInputStream()))) {
                // Wait for process to complete with timeout
                finished = execProcess.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);

                if (!finished) {
                    // Process didn't finish within timeout, kill it
                    execProcess.destroyForcibly();
                    log.warn("Process timed out and was killed for execution: {}", uniqueId);
                    return "Error: Execution timed out (15 seconds limit exceeded)";
                }

                // Read output after process completion
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = execProcess.exitValue();
            log.debug("Container execution completed with exit code: {}", exitCode);

            String result = output.toString().trim();

            // Check if timeout occurred (exit code 124 is timeout's exit code)
            if (exitCode == 124) {
                result = "Error: Execution timed out (10 seconds limit exceeded)";
            }

            // Optional: Clean up files immediately after execution
            // Comment out these lines if you want to rely only on periodic cleanup
            cleanupContainerFiles(containerName, containerCodePath, containerInputPath, uniqueId);

            return result;

        } catch (Exception e) {
            log.error("Error during container execution", e);
            return "Error: " + e.getMessage();
        }
    }

    private void cleanupContainerFiles(String containerName, String containerCodePath, String containerInputPath, String uniqueId) {
        try {
            // Clean up files in container - wait for completion to avoid race conditions
            ProcessBuilder cleanupBuilder = new ProcessBuilder("docker", "exec", containerName, "bash", "-c",
                    String.format("rm -f %s %s /code/%s_a.out", containerCodePath, containerInputPath, uniqueId));
            Process cleanupProcess = cleanupBuilder.start();
            int cleanupResult = cleanupProcess.waitFor();

            if (cleanupResult != 0) {
                log.warn("Cleanup process exited with code: {} for container: {}", cleanupResult, containerName);
            } else {
                log.debug("Successfully cleaned up files for execution: {}", uniqueId);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup container files for execution: {}", uniqueId, e);
        }
    }

    // Periodic cleanup to prevent disk space issues
    @Scheduled(fixedRate = 3600000) // Every hour (3600000 ms)
    public void periodicContainerCleanup() {
        String[] containers = {JAVA_CONTAINER, CPP_CONTAINER, PYTHON_CONTAINER};

        for (String container : containers) {
            try {
                if (isContainerRunning(container)) {
                    // Remove files older than 2 hours to be safe
                    ProcessBuilder pb = new ProcessBuilder(
                            "docker", "exec", container,
                            "find", "/code", "-type", "f", "-mmin", "+120", "-delete"
                    );
                    Process process = pb.start();
                    int result = process.waitFor();

                    if (result == 0) {
                        log.info("Periodic cleanup completed for container: {}", container);
                    } else {
                        log.warn("Periodic cleanup had issues for container: {}, exit code: {}", container, result);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to perform periodic cleanup for container: {}", container, e);
            }
        }
    }

    // Optional: Method to get container disk usage for monitoring
    public String getContainerDiskUsage(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "exec", containerName, "du", "-sh", "/code");
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            log.error("Failed to get disk usage for container: {}", containerName, e);
            return "Unknown";
        }
    }
}