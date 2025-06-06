package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contest")
@NoArgsConstructor
public class Contest extends PanacheEntity {
    @Column(nullable = false, unique = true, name = "name")
    private String contestName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, name = "time_begin")
    private LocalDateTime beginTime;

    @Column(nullable = false, name = "time_end")
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    public List<ContestProblem> contestProblems = new ArrayList<>();

    public Contest(String contestName, String description, LocalDateTime beginTime, LocalDateTime endTime) {
        this.contestName = contestName;
        this.description = description;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    @Transient
    public ContestStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();

        if(now.isBefore(beginTime))
            return ContestStatus.NOT_STARTED;
        else if (now.isAfter(endTime))
            return ContestStatus.DORESHKA;

        return ContestStatus.ONGOING;
    }

    public void addProblem(Problem problem, String shortName) {
        ContestProblem cp = new ContestProblem(this, problem, shortName);
        contestProblems.add(cp);
    }
}
