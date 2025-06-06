package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User extends PanacheEntity {
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, name = "password_hash")
    @JsonIgnore
    private String passwordHash;

    @Column(nullable = false, name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<UserContestAccess> contestAccesses = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Submission> submissions = new HashSet<>();

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}
