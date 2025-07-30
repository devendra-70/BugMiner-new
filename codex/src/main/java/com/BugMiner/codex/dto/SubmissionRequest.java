package com.BugMiner.codex.dto;

import lombok.Data;

@Data
public class SubmissionRequest {
    private Long problemId;
    private String userCode;
    private String language; // e.g., "java", "python"
}
