package ru.doreshka.dto.problem;

import lombok.Data;

@Data
public class AddProblemRequest {
    private String title;
    private String description;
    private int memoryLimit;
    private int timeLimit;
}
