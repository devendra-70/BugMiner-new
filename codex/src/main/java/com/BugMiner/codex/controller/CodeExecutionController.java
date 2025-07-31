package com.BugMiner.codex.controller;

import com.BugMiner.codex.client.CodeExecutionGrpcClient;
import com.BugMiner.codex.service.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/execution")
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionController {

    private final CodeExecutionService executionService;

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeCode(@RequestBody ExecutionRequest request) {
        log.info("Received execution request for language: {}", request.getLanguage());

        try {
            // Convert request to gRPC client test cases
            List<CodeExecutionGrpcClient.TestCase> testCases = request.getTestCases().stream()
                    .map(tc -> new CodeExecutionGrpcClient.TestCase(tc.getInput(), tc.getExpectedOutput()))
                    .collect(Collectors.toList());

            // Execute code
            CodeExecutionService.ExecutionResult result = executionService.executeCode(
                    request.getLanguage(),
                    request.getCode(),
                    testCases
            );

            // Convert to response
            ExecutionResponse response = convertToResponse(result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing code", e);

            ExecutionResponse errorResponse = new ExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Internal server error: " + e.getMessage());
            errorResponse.setExitCode(-1);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/execute-with-timeout")
    public ResponseEntity<ExecutionResponse> executeCodeWithTimeout(
            @RequestBody ExecutionRequestWithTimeout request) {

        log.info("Received execution request with timeout: {} seconds", request.getTimeoutSeconds());

        try {
            List<CodeExecutionGrpcClient.TestCase> testCases = request.getTestCases().stream()
                    .map(tc -> new CodeExecutionGrpcClient.TestCase(tc.getInput(), tc.getExpectedOutput()))
                    .collect(Collectors.toList());

            CodeExecutionService.ExecutionResult result = executionService.executeCodeWithTimeout(
                    request.getLanguage(),
                    request.getCode(),
                    testCases,
                    request.getTimeoutSeconds()
            );

            ExecutionResponse response = convertToResponse(result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing code with timeout", e);

            ExecutionResponse errorResponse = new ExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Internal server error: " + e.getMessage());
            errorResponse.setExitCode(-1);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> checkHealth() {
        boolean isHealthy = executionService.isLangsServiceHealthy();

        HealthResponse response = new HealthResponse();
        response.setHealthy(isHealthy);
        response.setMessage(isHealthy ? "langs-service is available" : "langs-service is unavailable");

        return ResponseEntity.ok(response);
    }

    private ExecutionResponse convertToResponse(CodeExecutionService.ExecutionResult result) {
        ExecutionResponse response = new ExecutionResponse();
        response.setSuccess(result.isSuccess());
        response.setExitCode(result.getExitCode());
        response.setErrorMessage(result.getErrorMessage());
        response.setPassedTests(result.getPassedTests());
        response.setTotalTests(result.getTotalTests());
        response.setAllTestsPassed(result.isAllTestsPassed());

        // Convert test results
        List<TestResultResponse> testResults = result.getTestResults().stream()
                .map(tr -> {
                    TestResultResponse trr = new TestResultResponse();
                    trr.setInput(tr.getInput());
                    trr.setExpectedOutput(tr.getExpectedOutput());
                    trr.setActualOutput(tr.getActualOutput());
                    trr.setPassed(tr.isPassed());
                    return trr;
                })
                .collect(Collectors.toList());

        response.setTestResults(testResults);
        return response;
    }

    // Request/Response DTOs
    public static class ExecutionRequest {
        private String language;
        private String code;
        private List<TestCaseRequest> testCases;

        // Getters and setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public List<TestCaseRequest> getTestCases() { return testCases; }
        public void setTestCases(List<TestCaseRequest> testCases) { this.testCases = testCases; }
    }

    public static class ExecutionRequestWithTimeout extends ExecutionRequest {
        private long timeoutSeconds;

        public long getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class TestCaseRequest {
        private String input;
        private String expectedOutput;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }

        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    }

    public static class ExecutionResponse {
        private boolean success;
        private int exitCode;
        private String errorMessage;
        private List<TestResultResponse> testResults;
        private int passedTests;
        private int totalTests;
        private boolean allTestsPassed;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public List<TestResultResponse> getTestResults() { return testResults; }
        public void setTestResults(List<TestResultResponse> testResults) { this.testResults = testResults; }

        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

        public boolean isAllTestsPassed() { return allTestsPassed; }
        public void setAllTestsPassed(boolean allTestsPassed) { this.allTestsPassed = allTestsPassed; }
    }

    public static class TestResultResponse {
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;

        // Getters and setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }

        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

        public String getActualOutput() { return actualOutput; }
        public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
    }

    public static class HealthResponse {
        private boolean healthy;
        private String message;

        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}