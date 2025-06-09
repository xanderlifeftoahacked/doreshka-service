package ru.doreshka.judging.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Problem extends PanacheEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, name = "time_limit")
    private int timeLimit;

    @Column(nullable = false, name = "memory_limit")
    private int memoryLimit;

    public Problem(String title, String description, int timeLimit, int memoryLimit) {
        this.title = title;
        this.description = description;
        this.timeLimit = timeLimit;
        this.memoryLimit = memoryLimit;
    }
} 