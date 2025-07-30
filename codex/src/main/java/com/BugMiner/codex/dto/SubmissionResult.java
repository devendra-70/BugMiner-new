package com.BugMiner.codex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubmissionResult {
    private boolean allPassed;
    private List<TestCaseResult> testResults;

    @Data
    @AllArgsConstructor
    public static class TestCaseResult {
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;
    }
}
