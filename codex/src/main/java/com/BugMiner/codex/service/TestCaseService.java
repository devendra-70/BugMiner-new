// src/main/java/com/BugMiner/codex/service/TestCaseService.java
package com.BugMiner.codex.service;

import com.BugMiner.codex.entity.TestCase;
import com.BugMiner.codex.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;

    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    public Optional<TestCase> getTestCaseById(Long id) {
        return testCaseRepository.findById(id);
    }

    public TestCase createOrUpdateTestCase(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }

    public void deleteTestCase(Long id) {
        testCaseRepository.deleteById(id);
    }
}
