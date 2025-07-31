package com.BugMiner.codex.client;

import com.BugMiner.codex.grpc.CodeExecutionRequest;
import com.BugMiner.codex.grpc.CodeExecutionResponse;
import com.BugMiner.codex.grpc.ExecutionServiceGrpc;
import com.BugMiner.codex.grpc.ExecutionTestCase;
import com.BugMiner.codex.grpc.ExecutionTestResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CodeExecutionGrpcClient {

    @Value("${grpc.client.langs-service.address:localhost}")
    private String langsServiceAddress;

    @Value("${grpc.client.langs-service.port:9090}")
    private int langsServicePort;

    private ManagedChannel channel;
    private ExecutionServiceGrpc.ExecutionServiceBlockingStub blockingStub;
    private ExecutionServiceGrpc.ExecutionServiceStub asyncStub;

    @PostConstruct
    public void init() {
        // Create the gRPC channel
        channel = ManagedChannelBuilder.forAddress(langsServiceAddress, langsServicePort)
                .usePlaintext() // Use plaintext for development, use TLS in production
                .build();

        // Create blocking and async stubs
        blockingStub = ExecutionServiceGrpc.newBlockingStub(channel);
        asyncStub = ExecutionServiceGrpc.newStub(channel);

        log.info("gRPC client initialized for langs-service at {}:{}", langsServiceAddress, langsServicePort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC channel shut down successfully");
            } catch (InterruptedException e) {
                log.warn("Failed to shutdown gRPC channel gracefully", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Execute code synchronously using blocking stub
     *
     * @param language The programming language (e.g., "python", "java", "cpp")
     * @param code The source code to execute
     * @param testCases List of test cases with input and expected output
     * @return CodeExecutionResponse with results
     */
    public CodeExecutionResponse executeCode(String language, String code, List<TestCase> testCases) {
        try {
            log.info("Executing code via gRPC - Language: {}, Test cases: {}", language, testCases.size());

            // Build the request
            CodeExecutionRequest.Builder requestBuilder = CodeExecutionRequest.newBuilder()
                    .setLanguage(language)
                    .setCode(code);

            // Add test cases
            for (TestCase testCase : testCases) {
                ExecutionTestCase grpcTestCase = ExecutionTestCase.newBuilder()
                        .setInput(testCase.getInput())
                        .setExpectedOutput(testCase.getExpectedOutput())
                        .build();
                requestBuilder.addTestCases(grpcTestCase);
            }

            CodeExecutionRequest request = requestBuilder.build();

            // Make the gRPC call
            CodeExecutionResponse response = blockingStub.executeCode(request);

            log.info("Code execution completed - Success: {}, Exit code: {}",
                    response.getSuccess(), response.getExitCode());

            return response;

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus());

            // Return error response
            return CodeExecutionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("gRPC call failed: " + e.getStatus().getDescription())
                    .setExitCode(-1)
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during code execution", e);

            return CodeExecutionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Unexpected error: " + e.getMessage())
                    .setExitCode(-1)
                    .build();
        }
    }

    /**
     * Execute code with timeout
     */
    public CodeExecutionResponse executeCodeWithTimeout(String language, String code,
                                                        List<TestCase> testCases, long timeoutSeconds) {
        try {
            ExecutionServiceGrpc.ExecutionServiceBlockingStub stubWithTimeout =
                    blockingStub.withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS);

            CodeExecutionRequest.Builder requestBuilder = CodeExecutionRequest.newBuilder()
                    .setLanguage(language)
                    .setCode(code);

            for (TestCase testCase : testCases) {
                ExecutionTestCase grpcTestCase = ExecutionTestCase.newBuilder()
                        .setInput(testCase.getInput())
                        .setExpectedOutput(testCase.getExpectedOutput())
                        .build();
                requestBuilder.addTestCases(grpcTestCase);
            }

            return stubWithTimeout.executeCode(requestBuilder.build());

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed with timeout: {}", e.getStatus());

            return CodeExecutionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Execution timeout or gRPC error: " + e.getStatus().getDescription())
                    .setExitCode(-1)
                    .build();
        }
    }

    /**
     * Helper method to check if the connection is healthy
     */
    public boolean isHealthy() {
        try {
            // Try a simple call to check if service is available
            CodeExecutionRequest healthCheck = CodeExecutionRequest.newBuilder()
                    .setLanguage("python")
                    .setCode("print('health check')")
                    .build();

            ExecutionServiceGrpc.ExecutionServiceBlockingStub healthStub =
                    blockingStub.withDeadlineAfter(2, TimeUnit.SECONDS);

            healthStub.executeCode(healthCheck);
            return true;
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    // Inner class for test cases (matches your server's TestCase entity)
    public static class TestCase {
        private String input;
        private String expectedOutput;

        public TestCase() {}

        public TestCase(String input, String expectedOutput) {
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }
}