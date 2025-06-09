package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "problem_test",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"input_path", "problem_id", "answer_path"})
        }
)
public class ProblemTest extends PanacheEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_id")
    @JsonIgnore
    public Problem problem;

    public String inputPath;
    public String answerPath;

    public int orderIndex;
}
