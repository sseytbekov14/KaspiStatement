package com.sultan.kaspitracker;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Milestone 2 — PDF Structure Exploration (NOT a production test).
 *
 * Purpose: extract raw text from the Kaspi Bank PDF statement and print it
 * with line numbers so we can understand the structure before designing
 * the regex parser in Milestone 3.
 *
 * Run with: mvn test -Dtest=PdfStructureExplorationTest -pl . --no-transfer-progress
 *
 * The test always PASSES — it is purely diagnostic.
 * Do NOT commit the PDF output; only this class and pom.xml go to git.
 */
class PdfStructureExplorationTest {

    /**
     * Path relative to the project root — works on any machine as long as
     * the PDF is placed in sample-data/.
     */
    private static final Path SAMPLE_PDF = Paths.get("sample-data", "kaspi_gold_statement.pdf");

    // ──────────────────────────────────────────────────────────────────────────
    // Test 1: Standard PDFTextStripper  (default reading order)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void extractText_standardStripper() throws IOException {
        System.out.println("\n");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("  PDFTextStripper  — standard (reading order)");
        System.out.println("══════════════════════════════════════════════════════════════════");

        String text = extractWithStripper(buildStandardStripper());
        printNumbered(text);
        printStats(text, "PDFTextStripper");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 2: PDFTextStripper with sort-by-position enabled
    //         (better for multi-column layouts — simulates PDFLayoutTextStripper)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void extractText_sortByPosition() throws IOException {
        System.out.println("\n");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("  PDFTextStripper  — sortByPosition=true  (layout-aware)");
        System.out.println("══════════════════════════════════════════════════════════════════");

        PDFTextStripper stripper = buildStandardStripper();
        stripper.setSortByPosition(true);
        String text = extractWithStripper(stripper);
        printNumbered(text);
        printStats(text, "PDFTextStripper[sortByPosition]");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: build base stripper
    // ──────────────────────────────────────────────────────────────────────────

    private PDFTextStripper buildStandardStripper() throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setLineSeparator("\n");
        stripper.setWordSeparator(" ");
        return stripper;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: load PDF and extract text
    // ──────────────────────────────────────────────────────────────────────────

    private String extractWithStripper(PDFTextStripper stripper) throws IOException {
        File pdfFile = SAMPLE_PDF.toFile();
        if (!pdfFile.exists()) {
            System.out.println("⚠️  PDF not found at: " + pdfFile.getAbsolutePath());
            System.out.println("   Place the Kaspi statement PDF in the sample-data/ folder.");
            return "";
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            System.out.printf("   Pages: %d%n", document.getNumberOfPages());
            return stripper.getText(document);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: print text with 1-based line numbers
    // ──────────────────────────────────────────────────────────────────────────

    private void printNumbered(String text) {
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n", -1);
        System.out.printf("   Total lines extracted: %d%n%n", lines.length);
        System.out.println("──────────────────────────────────────────────────────────────────");

        for (int i = 0; i < lines.length; i++) {
            System.out.printf("%4d │ %s%n", i + 1, lines[i]);
        }

        System.out.println("──────────────────────────────────────────────────────────────────");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: print quick stats
    // ──────────────────────────────────────────────────────────────────────────

    private void printStats(String text, String label) {
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n", -1);
        long nonEmpty = java.util.Arrays.stream(lines)
                .filter(l -> !l.trim().isEmpty())
                .count();

        System.out.printf("%n  [%s] Stats: total=%d, non-empty=%d, chars=%d%n",
                label, lines.length, nonEmpty, text.length());

        // Print first few non-empty lines as a quick preview
        System.out.println("  First 10 non-empty lines (preview):");
        java.util.Arrays.stream(lines)
                .filter(l -> !l.trim().isEmpty())
                .limit(10)
                .forEach(l -> System.out.println("    > " + l));
    }
}
