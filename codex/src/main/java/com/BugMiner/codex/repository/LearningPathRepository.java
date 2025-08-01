package com.BugMiner.codex.repository;
import com.BugMiner.codex.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    List<LearningPath> findByLanguage(String language);
    List<LearningPath> findByLanguageAndTopic(String language, String topic);
}

