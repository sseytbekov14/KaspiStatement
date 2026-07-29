package com.sultan.kaspitracker.parser;

import java.util.Arrays;
import java.util.Optional;

/**
 * Fixed vocabulary of operation types as they appear in the Kaspi Bank PDF statement
 * (English version). Defined in Technical Specification §7.
 *
 * <p>Note: {@code TRANSFER_TO_YOUR_ACCOUNT} spans <em>two</em> lines in the raw PDF
 * ("Transfer to your" on one line, "account" on the next).
 * {@link com.sultan.kaspitracker.service.StatementParserService} merges them automatically.
 */
public enum OperationType {

    PURCHASES("Purchases"),
    REPLENISHMENT("Replenishment"),
    WITHDRAWALS("Withdrawals"),
    TRANSFERS("Transfers"),
    OTHERS("Others"),
    TRANSFER_TO_YOUR_ACCOUNT("Transfer to your account");

    /** Exact label as it appears (possibly split) in the PDF. */
    private final String pdfLabel;

    OperationType(String pdfLabel) {
        this.pdfLabel = pdfLabel;
    }

    public String getPdfLabel() {
        return pdfLabel;
    }

    /**
     * Case-insensitive lookup by the raw PDF label (trimmed).
     *
     * @param raw raw string extracted from the PDF
     * @return matching {@code OperationType}, or {@link Optional#empty()} if unknown
     */
    public static Optional<OperationType> fromPdfLabel(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String cleaned = raw.trim();
        return Arrays.stream(values())
                .filter(t -> t.pdfLabel.equalsIgnoreCase(cleaned))
                .findFirst();
    }
}
