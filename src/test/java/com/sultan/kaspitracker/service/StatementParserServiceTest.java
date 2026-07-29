package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.parser.OperationType;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.parser.SignType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StatementParserService}.
 *
 * No Spring context, no PDF file — all inputs are hardcoded string literals
 * taken from the real PDF analysis (Technical Specification §7).
 *
 * Each test wraps its transaction lines between the standard table markers
 * using {@link #table(String...)} so the parser finds the data region.
 */
class StatementParserServiceTest {

    private StatementParserService parser;

    @BeforeEach
    void setUp() {
        parser = new StatementParserService();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: wrap transaction lines in table markers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Surrounds the given transaction lines with the real table-start and
     * table-end markers so the parser's boundary detection is exercised.
     */
    private String table(String... txLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date Amount Transaction       Details\n");
        for (String line : txLines) {
            sb.append(line).append("\n");
        }
        sb.append("The section «Transaction summary» contains information\n");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Ordinary DEBIT transaction — no thousands separator
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DEBIT transaction without thousands separator (e.g. 480,00)")
    void parse_regularDebit_noThousandsSeparator() {
        String text = table("27.07.26 - 480,00 ₸   Purchases      ONAY. БН");

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(1);
        ParsedTransaction tx = result.get(0);
        assertThat(tx.date()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(tx.sign()).isEqualTo(SignType.DEBIT);
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("480.00"));
        assertThat(tx.operationType()).isEqualTo(OperationType.PURCHASES);
        assertThat(tx.merchantDetails()).isEqualTo("ONAY. БН");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Ordinary CREDIT transaction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CREDIT (Replenishment) transaction")
    void parse_regularCredit_replenishment() {
        String text = table("15.07.26 + 138 900,00 ₸   Replenishment      Salary");

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(1);
        ParsedTransaction tx = result.get(0);
        assertThat(tx.sign()).isEqualTo(SignType.CREDIT);
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("138900.00"));
        assertThat(tx.operationType()).isEqualTo(OperationType.REPLENISHMENT);
        assertThat(tx.merchantDetails()).isEqualTo("Salary");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. DEBIT with thousands separator
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DEBIT with space-thousands separator (e.g. 400 000,00)")
    void parse_debit_withThousandsSeparator() {
        String text = table("18.07.26 - 400 000,00 ₸   Withdrawals      ATM Magnum Cash  Carry");

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(1);
        ParsedTransaction tx = result.get(0);
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("400000.00"));
        assertThat(tx.operationType()).isEqualTo(OperationType.WITHDRAWALS);
        assertThat(tx.merchantDetails()).isEqualTo("ATM Magnum Cash  Carry");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Two-line "Transfer to your account" is assembled into ONE transaction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Two-line 'Transfer to your account' merges into one ParsedTransaction")
    void parse_transferToYourAccount_mergedCorrectly() {
        String text = table(
                "10.07.26 - 98,00 ₸   Transfer to your      Pay for Kaspi Credit",
                "account  "
        );

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(1);   // NOT 2 (no split) and NOT 0 (not lost)
        ParsedTransaction tx = result.get(0);
        assertThat(tx.operationType()).isEqualTo(OperationType.TRANSFER_TO_YOUR_ACCOUNT);
        assertThat(tx.merchantDetails()).isEqualTo("Pay for Kaspi Credit");
        assertThat(tx.sign()).isEqualTo(SignType.DEBIT);
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("98.00"));
    }

    @Test
    @DisplayName("Multiple two-line 'Transfer to your account' transactions all merge correctly")
    void parse_multipleTransferToYourAccount() {
        String text = table(
                "10.07.26 - 98,00 ₸   Transfer to your      Pay for Kaspi Credit",
                "account  ",
                "08.07.26 - 240 000,00 ₸   Transfer to your      Pay for Kaspi Credit",
                "account  ",
                "06.07.26 - 230 000,00 ₸   Transfer to your      Pay for Kaspi Credit",
                "account  "
        );

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(tx -> tx.operationType() == OperationType.TRANSFER_TO_YOUR_ACCOUNT);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("98.00"));
        assertThat(result.get(1).amount()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(result.get(2).amount()).isEqualByComparingTo(new BigDecimal("230000.00"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Page header / footer lines are ignored
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("JSC Kaspi Bank header and Appendix footer lines are silently ignored")
    void parse_pageHeaderFooter_areIgnored() {
        String text = table(
                "24.07.26 - 360,00 ₸   Purchases      ONAY. БН",
                "JSC «Kaspi Bank», BIC CASPKZKA, www.kaspi.kz",
                "Appendix to Statement No.1246706548 dated July 27 2026",
                "24.07.26 - 1 238,00 ₸   Purchases      MAGNUM CASH&CARRY"
        );

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).merchantDetails()).isEqualTo("ONAY. БН");
        assertThat(result.get(1).merchantDetails()).isEqualTo("MAGNUM CASH&CARRY");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Unrecognised / malformed line does NOT throw; rest of transactions parsed
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Malformed line is skipped (WARN logged) — parsing continues for rest")
    void parse_malformedLine_doesNotThrow_andOtherTransactionsParsed() {
        // The "blocked amount" line from the real PDF does not start with DD.MM.YY
        String blockedLine =
            "    - The amount is blocked.  The bank expects the confirmation of the payment system.";

        String text = table(
                "27.07.26 - 480,00 ₸   Purchases      ONAY. БН",
                blockedLine,
                "27.07.26 - 300,00 ₸   Transfers      Bekzat R."
        );

        List<ParsedTransaction> result = parser.parse(text);

        // Does NOT throw; only the two valid transactions are returned
        assertThat(result).hasSize(2);
        assertThat(result.get(0).operationType()).isEqualTo(OperationType.PURCHASES);
        assertThat(result.get(1).operationType()).isEqualTo(OperationType.TRANSFERS);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Others operation type
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("'Others' operation type is parsed correctly (commission lines)")
    void parse_othersOperationType() {
        String text = table(
                "11.07.26 - 243,88 ₸   Others      Commission for transfer of other banks"
        );

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(1);
        ParsedTransaction tx = result.get(0);
        assertThat(tx.operationType()).isEqualTo(OperationType.OTHERS);
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("243.88"));
        assertThat(tx.merchantDetails()).isEqualTo("Commission for transfer of other banks");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. Transfers with long merchant detail (spaces in detail field)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Merchant details containing spaces are captured fully")
    void parse_merchantWithSpaces() {
        String text = table(
                "11.07.26 - 25 672,00 ₸   Transfers      To card of other banks Super App P2P"
        );

        List<ParsedTransaction> result = parser.parse(text);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).merchantDetails())
                .isEqualTo("To card of other banks Super App P2P");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 9. Date parsing — all fields correct
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Date is parsed as LocalDate with correct day, month, year")
    void parse_date_isCorrect() {
        String text = table("30.06.26 + 50 401,91 ₸   Replenishment      Salary");

        ParsedTransaction tx = parser.parse(text).get(0);

        assertThat(tx.date().getYear()).isEqualTo(2026);
        assertThat(tx.date().getMonthValue()).isEqualTo(6);
        assertThat(tx.date().getDayOfMonth()).isEqualTo(30);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 10. Boundary / null / empty input
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Null input returns empty list without exception")
    void parse_nullInput_returnsEmptyList() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("Blank input returns empty list")
    void parse_blankInput_returnsEmptyList() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   \n  ")).isEmpty();
    }

    @Test
    @DisplayName("Text without table markers returns empty list")
    void parse_noTableMarkers_returnsEmptyList() {
        assertThat(parser.parse("Some random text without any markers")).isEmpty();
    }

    @Test
    @DisplayName("Lines before the table-start marker are ignored")
    void parse_linesBeforeTable_areIgnored() {
        // All interesting text comes BEFORE the marker — should yield zero results
        String text = "27.07.26 - 480,00 ₸   Purchases      ONAY. БН\n"
                + "Date Amount Transaction       Details\n"   // marker — but no content follows
                + "The section «Transaction summary» info\n";

        assertThat(parser.parse(text)).isEmpty();
    }
}
