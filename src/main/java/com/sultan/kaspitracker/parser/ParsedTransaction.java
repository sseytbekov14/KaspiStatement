package com.sultan.kaspitracker.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Intermediate parsing model — <strong>NOT</strong> a JPA entity.
 *
 * <p>Represents a single transaction row extracted from a Kaspi Bank PDF statement.
 * All amounts are positive; use {@link #sign()} to determine direction.
 *
 * <p>Will be converted to the persistent {@code Transaction} entity in Milestone 4
 * when the data model and Flyway migration are in place.
 *
 * @param date            transaction date (format {@code DD.MM.YY} in the source PDF)
 * @param sign            {@link SignType#CREDIT} for incoming, {@link SignType#DEBIT} for outgoing
 * @param amount          absolute amount (always positive), currency KZT
 * @param operationType   built-in Kaspi category (from the fixed PDF vocabulary)
 * @param merchantDetails raw merchant / counterparty string as printed in the PDF
 */
public record ParsedTransaction(
        LocalDate date,
        SignType sign,
        BigDecimal amount,
        OperationType operationType,
        String merchantDetails
) {}
