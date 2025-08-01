package com.BugMiner.codex.controller;

import com.BugMiner.codex.entity.LearningPath;
import com.BugMiner.codex.service.LearningPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning-paths")
public class LearningPathController {

    @Autowired
    private LearningPathService service;

    @GetMapping
    public ResponseEntity<List<LearningPath>> getAllPaths() {
        return ResponseEntity.ok(service.getAllPaths());
    }

    @GetMapping("/language/{language}")
    public ResponseEntity<List<LearningPath>> getByLanguage(@PathVariable String language) {
        return ResponseEntity.ok(service.getPathsByLanguage(language));
    }

    @PostMapping
    public ResponseEntity<LearningPath> createPath(@RequestBody LearningPath path) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPath(path));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePath(@PathVariable Long id) {
        service.deletePath(id);
        return ResponseEntity.noContent().build();
    }
}
