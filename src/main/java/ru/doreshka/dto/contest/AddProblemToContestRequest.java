package ru.doreshka.dto.contest;


public record AddProblemToContestRequest(
        Long problemId,
        String shortName
) {}
