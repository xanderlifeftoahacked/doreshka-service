package ru.doreshka.judging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.doreshka.judging.entity.Verdict;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {
    private Long submissionId;
    private Verdict verdict;
    private Integer passedTests;
    private Long executionTime;
    private Long memoryUsed;
    private String errorMessage;
} 