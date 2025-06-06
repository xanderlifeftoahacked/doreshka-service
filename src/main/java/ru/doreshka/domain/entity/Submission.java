package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission")
public class Submission extends PanacheEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_id")
    @JsonIgnore
    public Problem problem;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contest_id")
    @JsonIgnore
    public Contest contest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    public User user;

    @Column(nullable = false, name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @JsonIgnore
    public String sourcePath;

    public Verdict verdict;

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}

