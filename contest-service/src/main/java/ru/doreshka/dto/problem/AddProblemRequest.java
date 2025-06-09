package ru.doreshka.dto.problem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddProblemRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @Min(value = 1)
    private int memoryLimit;

    @Min(value = 1)
    private int timeLimit;
}
