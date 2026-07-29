package com.sultan.kaspitracker.parser;

import com.sultan.kaspitracker.service.StatementParserService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test — reads the <em>real</em> Kaspi Bank PDF from {@code sample-data/}
 * and verifies that {@link StatementParserService} produces a reasonable result.
 *
 * <h3>CI behaviour</h3>
 * <p>The test is annotated with {@code @EnabledIf("samplePdfExists")} so it is
 * automatically <em>skipped</em> when the PDF is absent (e.g. in GitHub Actions,
 * where {@code sample-data/} is gitignored and not checked out).
 *
 * <h3>Running locally</h3>
 * <pre>
 *   mvn test -Dtest=PdfParserIntegrationTest --no-transfer-progress
 * </pre>
 */
@Tag("integration")
class PdfParserIntegrationTest {

    private static final Path SAMPLE_PDF =
            Paths.get("sample-data", "kaspi_gold_statement.pdf");

    /** JUnit 5 condition method — must be {@code static}. */
    static boolean samplePdfExists() {
        return SAMPLE_PDF.toFile().exists();
    }

    @Test
    @EnabledIf("samplePdfExists")
    @SuppressWarnings("java:S5960") // AssertJ in integration tests is intentional
    void parseRealPdf_producesReasonableResults() throws Exception {
        // ── 1. Extract raw text via PDFBox ────────────────────────────────────
        File pdfFile = SAMPLE_PDF.toFile();
        String rawText;
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            rawText = stripper.getText(doc);
        }

        // ── 2. Parse ──────────────────────────────────────────────────────────
        StatementParserService service = new StatementParserService();
        List<ParsedTransaction> transactions = service.parse(rawText);

        // ── 3. Print summary (visible in Maven Surefire report / console) ─────
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║       Milestone 3 — Integration Test Results         ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf("║  Total parsed transactions : %-24d║%n", transactions.size());
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  By operation type:                                  ║");

        Map<OperationType, Long> byType = transactions.stream()
                .collect(Collectors.groupingBy(ParsedTransaction::operationType, Collectors.counting()));

        for (OperationType type : OperationType.values()) {
            long count = byType.getOrDefault(type, 0L);
            System.out.printf("║  %-30s : %-20d║%n", type, count);
        }

        BigDecimal totalDebit = transactions.stream()
                .filter(tx -> tx.sign() == com.sultan.kaspitracker.parser.SignType.DEBIT)
                .map(ParsedTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = transactions.stream()
                .filter(tx -> tx.sign() == com.sultan.kaspitracker.parser.SignType.CREDIT)
                .map(ParsedTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf("║  Total DEBIT  (outgoing) KZT: %-22s║%n", totalDebit.toPlainString());
        System.out.printf("║  Total CREDIT (incoming) KZT: %-22s║%n", totalCredit.toPlainString());
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // ── 4. Assertions ─────────────────────────────────────────────────────

        // The sample file covers one full month; we expect well over 100 transactions
        assertThat(transactions)
                .as("Should parse at least 100 transactions from a full-month statement")
                .hasSizeGreaterThan(100);

        // All mandatory fields must be populated
        assertThat(transactions).allSatisfy(tx -> {
            assertThat(tx.date()).as("date must not be null").isNotNull();
            assertThat(tx.sign()).as("sign must not be null").isNotNull();
            assertThat(tx.amount()).as("amount must be positive")
                    .isGreaterThan(BigDecimal.ZERO);
            assertThat(tx.operationType()).as("operationType must not be null").isNotNull();
            assertThat(tx.merchantDetails()).as("merchantDetails must not be blank").isNotBlank();
        });

        // The sample PDF must contain at least one of each expected operation type
        assertThat(byType.keySet())
                .as("Statement should contain Purchases, Replenishment and Transfers at minimum")
                .contains(OperationType.PURCHASES, OperationType.REPLENISHMENT, OperationType.TRANSFERS);

        // Transfers-to-your-account should have been assembled (5 in the sample)
        assertThat(byType.getOrDefault(OperationType.TRANSFER_TO_YOUR_ACCOUNT, 0L))
                .as("TRANSFER_TO_YOUR_ACCOUNT anomaly should be present and assembled")
                .isGreaterThan(0);
    }
}
