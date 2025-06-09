package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    @JsonIgnore
    public Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    @JsonIgnore
    public Contest contest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    public User user;

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
}

