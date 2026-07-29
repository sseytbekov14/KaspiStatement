package com.sultan.kaspitracker.service;

public record PersistenceResult(
        boolean success,
        boolean isDuplicate,
        int savedTransactionsCount,
        Long statementId
) {
    public static PersistenceResult success(int count, Long statementId) {
        return new PersistenceResult(true, false, count, statementId);
    }

    public static PersistenceResult duplicate(Long existingStatementId) {
        return new PersistenceResult(false, true, 0, existingStatementId);
    }
}
