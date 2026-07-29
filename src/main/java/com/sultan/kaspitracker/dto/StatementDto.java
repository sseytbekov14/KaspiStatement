package com.sultan.kaspitracker.dto;

import java.time.Instant;
import java.time.LocalDate;

public record StatementDto(
    Long id,
    LocalDate periodStart,
    LocalDate periodEnd,
    Instant uploadDate,
    int transactionCount
) {}
