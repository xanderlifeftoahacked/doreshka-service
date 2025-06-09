package ru.doreshka.judging.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Submission extends PanacheEntity {

    @Column(name = "problem_id", nullable = false)
    public Long problemId;

    @Column(name = "contest_id", nullable = false)
    public Long contestId;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false, name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "source_path")
    public String sourcePath;

    @Enumerated(EnumType.STRING)
    public Verdict verdict = Verdict.Pending;

    @Column(name = "cur_test")
    public int curTest = 0;

    @Column(name = "execution_time")
    public Long executionTime;

    @Column(name = "memory_used")
    public Long memoryUsed;

    @Column(name = "error_message", length = 1000)
    public String errorMessage;

    @Column(name = "time_limit")
    public Integer timeLimit;

    @Column(name = "memory_limit")
    public Integer memoryLimit;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateVerdict(Verdict newVerdict) {
        this.verdict = newVerdict;
    }

    public void updateVerdict(Verdict newVerdict, String errorMessage) {
        this.verdict = newVerdict;
        this.errorMessage = errorMessage;
    }
} 