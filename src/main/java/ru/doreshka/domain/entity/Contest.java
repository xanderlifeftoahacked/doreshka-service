package ru.doreshka.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contest")
public class Contest extends PanacheEntity {
    @Column(nullable = false, unique = true, name = "name")
    private String contestName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, name = "time_begin")
    private LocalDateTime beginTime;

    @Column(nullable = false, name = "time_end")
    private LocalDateTime endTime;

    @Transient
    public ContestStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();

        if(now.isBefore(beginTime))
            return ContestStatus.NOT_STARTED;
        else if (now.isAfter(endTime))
            return ContestStatus.DORESHKA;

        return ContestStatus.ONGOING;
    }

}
