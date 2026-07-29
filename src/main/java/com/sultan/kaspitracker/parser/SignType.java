package com.sultan.kaspitracker.parser;

/**
 * Represents the sign (direction) of a transaction as extracted from the PDF.
 * Maps the literal {@code +} / {@code -} character from the statement.
 */
public enum SignType {

    /** Incoming funds — corresponds to {@code +} in the PDF. */
    CREDIT('+'),

    /** Outgoing funds — corresponds to {@code -} in the PDF. */
    DEBIT('-');

    private final char symbol;

    SignType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    /**
     * Converts the raw PDF character ({@code '+'} or {@code '-'}) to a
     * {@code SignType}.
     *
     * @throws IllegalArgumentException if the character is not recognized
     */
    public static SignType fromChar(char c) {
        return switch (c) {
            case '+' -> CREDIT;
            case '-' -> DEBIT;
            default -> throw new IllegalArgumentException("Unknown sign character: '" + c + "'");
        };
    }
}
