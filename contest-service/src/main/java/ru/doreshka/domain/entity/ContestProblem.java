package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "contest_problem",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"contest_id", "problem_id"})
        }
)
public class ContestProblem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "contest_id")
    @JsonIgnore
    public Contest contest;

    @ManyToOne
    @JoinColumn(name = "problem_id")
    @JsonIgnore
    public Problem problem;

    public String shortName;

    public ContestProblem() {
    }

    public ContestProblem(Contest contest, Problem problem, String shortName) {
        this.contest = contest;
        this.problem = problem;
        this.shortName = shortName;
    }
}
