package com.sultan.kaspitracker.dto;

import java.math.BigDecimal;

public record CategorySummaryDto(
    String categoryName,
    BigDecimal totalAmount
) {}
