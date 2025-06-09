package ru.doreshka.judging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeRequest {
    private Long submissionId;
    private Long problemId;
    private String sourcePath;
    private Integer timeLimit;
    private Integer memoryLimit;
} 