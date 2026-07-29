package com.sultan.kaspitracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDto(
    Long id,
    LocalDate date,
    String merchant,
    BigDecimal amount,
    String sign,
    String operationType,
    String categoryName
) {}
