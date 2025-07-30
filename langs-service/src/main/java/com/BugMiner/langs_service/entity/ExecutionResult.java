package com.BugMiner.langs_service.entity;

import java.util.List;

public class ExecutionResult {
    private boolean success;
    private List<TestCaseResult> testResults;
    private String errorMessage;
    private int exitCode;
    // constructors, getters
}
