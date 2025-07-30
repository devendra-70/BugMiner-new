// src/main/java/com/BugMiner/codex/repository/ProblemRepository.java
package com.BugMiner.codex.repository;

import com.BugMiner.codex.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
}
