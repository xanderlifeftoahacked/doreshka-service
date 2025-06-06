package ru.doreshka.dto.contest;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AddContestRequest {
    private String contestName;
    private String description;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
}
