package ru.doreshka.domain.entity;

public enum Verdict {
    Accepted,
    WrongAnswer,
    CompilationError,
    Testing,
    Compiling,
    Pending,
    RuntimeError,
    TimeLimitExceeded,
    MemoryLimitExceeded,
    SystemError
}