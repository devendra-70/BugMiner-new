package com.BugMiner.codex.service;

import com.BugMiner.codex.client.CodeExecutionGrpcClient;
import com.BugMiner.codex.grpc.CodeExecutionResponse;
import com.BugMiner.codex.grpc.ExecutionTestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeExecutionService {

    private final CodeExecutionGrpcClient grpcClient;

    /**
     * Execute code and return execution result
     *
     * @param language The programming language
     * @param code The source code
     * @param testCases List of test cases
     * @return ExecutionResult containing the results
     */
    public ExecutionResult executeCode(String language, String code,
                                       List<CodeExecutionGrpcClient.TestCase> testCases) {

        log.info("Executing code for language: {} with {} test cases", language, testCases.size());

        // Call the gRPC service
        CodeExecutionResponse grpcResponse = grpcClient.executeCode(language, code, testCases);

        // Convert gRPC response to domain object
        ExecutionResult result = new ExecutionResult();
        result.setSuccess(grpcResponse.getSuccess());
        result.setExitCode(grpcResponse.getExitCode());
        result.setErrorMessage(grpcResponse.getErrorMessage());

        // Convert test results
        List<TestResult> testResults = grpcResponse.getTestResultsList().stream()
                .map(this::convertGrpcTestResult)
                .collect(Collectors.toList());
        result.setTestResults(testResults);

        // Calculate summary statistics
        long passedTests = testResults.stream().mapToLong(tr -> tr.isPassed() ? 1 : 0).sum();
        result.setPassedTests((int) passedTests);
        result.setTotalTests(testResults.size());
        result.setAllTestsPassed(passedTests == testResults.size() && grpcResponse.getSuccess());

        log.info("Code execution completed - Success: {}, Passed: {}/{}",
                result.isSuccess(), result.getPassedTests(), result.getTotalTests());

        return result;
    }

    /**
     * Execute code with timeout
     */
    public ExecutionResult executeCodeWithTimeout(String language, String code,
                                                  List<CodeExecutionGrpcClient.TestCase> testCases,
                                                  long timeoutSeconds) {

        log.info("Executing code with timeout: {} seconds", timeoutSeconds);

        CodeExecutionResponse grpcResponse = grpcClient.executeCodeWithTimeout(
                language, code, testCases, timeoutSeconds);

        ExecutionResult result = new ExecutionResult();
        result.setSuccess(grpcResponse.getSuccess());
        result.setExitCode(grpcResponse.getExitCode());
        result.setErrorMessage(grpcResponse.getErrorMessage());

        List<TestResult> testResults = grpcResponse.getTestResultsList().stream()
                .map(this::convertGrpcTestResult)
                .collect(Collectors.toList());
        result.setTestResults(testResults);

        long passedTests = testResults.stream().mapToLong(tr -> tr.isPassed() ? 1 : 0).sum();
        result.setPassedTests((int) passedTests);
        result.setTotalTests(testResults.size());
        result.setAllTestsPassed(passedTests == testResults.size() && grpcResponse.getSuccess());

        return result;
    }

    /**
     * Check if the langs-service is available
     */
    public boolean isLangsServiceHealthy() {
        return grpcClient.isHealthy();
    }

    private TestResult convertGrpcTestResult(ExecutionTestResult grpcResult) {
        TestResult testResult = new TestResult();
        testResult.setInput(grpcResult.getInput());
        testResult.setExpectedOutput(grpcResult.getExpectedOutput());
        testResult.setActualOutput(grpcResult.getActualOutput());
        testResult.setPassed(grpcResult.getPassed());
        return testResult;
    }

    // Domain classes
    public static class ExecutionResult {
        private boolean success;
        private int exitCode;
        private String errorMessage;
        private List<TestResult> testResults;
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

        public List<TestResult> getTestResults() { return testResults; }
        public void setTestResults(List<TestResult> testResults) { this.testResults = testResults; }

        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

        public boolean isAllTestsPassed() { return allTestsPassed; }
        public void setAllTestsPassed(boolean allTestsPassed) { this.allTestsPassed = allTestsPassed; }
    }

    public static class TestResult {
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
}