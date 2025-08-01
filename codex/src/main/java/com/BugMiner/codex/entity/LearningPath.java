package com.BugMiner.codex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "learning_paths")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String language;

    private String topic;

    private String subtopic;
}