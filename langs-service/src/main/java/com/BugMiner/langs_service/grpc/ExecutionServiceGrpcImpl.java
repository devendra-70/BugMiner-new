package com.BugMiner.langs_service.grpc;

import com.BugMiner.langs_service.entity.ExecutionRequest;
import com.BugMiner.langs_service.entity.ExecutionResult;
import com.BugMiner.langs_service.entity.TestCase;
import com.BugMiner.langs_service.entity.TestCaseResult;
import com.BugMiner.langs_service.service.CodeExecutionService;
import com.langservice.grpc.CodeExecutionRequest;
import com.langservice.grpc.CodeExecutionResponse;
import com.langservice.grpc.ExecutionServiceGrpc;
import com.langservice.grpc.ExecutionTestCase;
import com.langservice.grpc.ExecutionTestResult;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ExecutionServiceGrpcImpl extends ExecutionServiceGrpc.ExecutionServiceImplBase {

    private final CodeExecutionService codeExecutionService;

    @Override
    public void executeCode(CodeExecutionRequest request, StreamObserver<CodeExecutionResponse> responseObserver) {
        try {
            log.info("Received gRPC execution request for language: {}", request.getLanguage());

            // Convert gRPC request to service entity
            ExecutionRequest serviceRequest = convertToServiceRequest(request);

            // Execute code using existing service
            ExecutionResult result = codeExecutionService.executeCode(serviceRequest);

            // Convert service result to gRPC response
            CodeExecutionResponse response = convertToGrpcResponse(result);

            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully processed execution request with result: {}", result.isSuccess());

        } catch (Exception e) {
            log.error("Error processing gRPC execution request", e);

            // Send error response
            CodeExecutionResponse errorResponse = CodeExecutionResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Internal server error: " + e.getMessage())
                    .setExitCode(1)
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    private ExecutionRequest convertToServiceRequest(CodeExecutionRequest grpcRequest) {
        ExecutionRequest serviceRequest = new ExecutionRequest();
        serviceRequest.setLanguage(grpcRequest.getLanguage());
        serviceRequest.setCode(grpcRequest.getCode());

        List<TestCase> testCases = grpcRequest.getTestCasesList().stream()
                .map(this::convertToServiceTestCase)
                .collect(Collectors.toList());
        serviceRequest.setTestCases(testCases);

        return serviceRequest;
    }

    private TestCase convertToServiceTestCase(ExecutionTestCase grpcTestCase) {
        TestCase testCase = new TestCase();
        testCase.setInput(grpcTestCase.getInput());
        testCase.setExpectedOutput(grpcTestCase.getExpectedOutput());
        return testCase;
    }

    private CodeExecutionResponse convertToGrpcResponse(ExecutionResult result) {
        CodeExecutionResponse.Builder responseBuilder = CodeExecutionResponse.newBuilder()
                .setSuccess(result.isSuccess())
                .setExitCode(result.getExitCode());

        if (result.getErrorMessage() != null) {
            responseBuilder.setErrorMessage(result.getErrorMessage());
        }

        if (result.getTestResults() != null) {
            List<ExecutionTestResult> grpcTestResults = result.getTestResults().stream()
                    .map(this::convertToGrpcTestResult)
                    .collect(Collectors.toList());
            responseBuilder.addAllTestResults(grpcTestResults);
        }

        return responseBuilder.build();
    }

    private ExecutionTestResult convertToGrpcTestResult(TestCaseResult serviceResult) {
        return ExecutionTestResult.newBuilder()
                .setInput(serviceResult.getInput())
                .setExpectedOutput(serviceResult.getExpectedOutput())
                .setActualOutput(serviceResult.getActualOutput())
                .setPassed(serviceResult.isPassed())
                .build();
    }
}