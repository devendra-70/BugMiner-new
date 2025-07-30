package com.BugMiner.langs_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private boolean success;
    private List<TestCaseResult> testResults;
    private String errorMessage;
    private int exitCode;
}
