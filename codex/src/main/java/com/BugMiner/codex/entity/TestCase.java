package com.BugMiner.codex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(length = 10000)
    private String input;

    @Column(length = 10000)
    private String expectedOutput;

    private boolean isPublic; // Whether to show this test case to the user or keep it hidden for evaluation
}
