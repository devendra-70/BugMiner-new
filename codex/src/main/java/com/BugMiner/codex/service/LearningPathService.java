package com.BugMiner.codex.service;

import com.BugMiner.codex.entity.LearningPath;
import com.BugMiner.codex.repository.LearningPathRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningPathService {

    @Autowired
    private LearningPathRepository repository;

    public List<LearningPath> getAllPaths() {
        return repository.findAll();
    }

    public List<LearningPath> getPathsByLanguage(String language) {
        return repository.findByLanguage(language);
    }

    public LearningPath createPath(LearningPath path) {
        return repository.save(path);
    }

    public void deletePath(Long id) {
        repository.deleteById(id);
    }
}

