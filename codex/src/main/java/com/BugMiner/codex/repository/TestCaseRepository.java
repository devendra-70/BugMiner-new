// src/main/java/com/BugMiner/codex/repository/TestCaseRepository.java
package com.BugMiner.codex.repository;

import com.BugMiner.codex.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    // Optional: List<TestCase> findByProblemId(Long problemId);
}
