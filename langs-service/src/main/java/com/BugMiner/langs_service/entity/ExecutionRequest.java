package com.BugMiner.langs_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {
    private String language;
    private String code;
    private List<TestCase> testCases;
}
