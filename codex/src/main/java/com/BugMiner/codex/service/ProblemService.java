// src/main/java/com/BugMiner/codex/service/ProblemService.java
package com.BugMiner.codex.service;

import com.BugMiner.codex.entity.Problem;
import com.BugMiner.codex.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    public Optional<Problem> getProblemById(Long id) {
        return problemRepository.findById(id);
    }

    public Problem createOrUpdateProblem(Problem problem) {
        return problemRepository.save(problem);
    }

    public void deleteProblem(Long id) {
        problemRepository.deleteById(id);
    }
}
