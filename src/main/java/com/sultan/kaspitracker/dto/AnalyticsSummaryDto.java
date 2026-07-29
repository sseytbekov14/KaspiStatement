package com.sultan.kaspitracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsSummaryDto(
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    List<CategorySummaryDto> categories
) {}
