package com.sultan.kaspitracker.dto;

public record UploadResponseDto(
    boolean success,
    boolean isDuplicate,
    int savedTransactionsCount,
    Long statementId,
    String message
) {}
