package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.parser.OperationType;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.parser.SignType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Milestone 3 — parses the raw text extracted from a Kaspi Bank PDF statement
 * (English version) and returns a list of {@link ParsedTransaction} objects.
 *
 * <h3>Input contract</h3>
 * <p>Accepts the multi-line string produced by Apache PDFBox
 * ({@code PDFTextStripper.getText()}). Does <em>not</em> touch the PDF itself —
 * PDF → text extraction is a separate concern (future Milestone).
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Scan lines until the table-start marker is found.</li>
 *   <li>Stop at the table-end marker.</li>
 *   <li>Skip known page header/footer patterns (JSC Kaspi Bank, Appendix lines).</li>
 *   <li>Detect the two-line "Transfer to your / account" anomaly and merge.</li>
 *   <li>Parse each valid line with the transaction regex.</li>
 *   <li>Log WARN (never throw) for lines that cannot be classified.</li>
 * </ol>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Stateless: each {@link #parse(String)} call is independent.</li>
 *   <li>No persistence: caller is responsible for saving results (Milestone 4).</li>
 *   <li>No categorisation: fuzzy-matching against the merchant dictionary is Milestone 5.</li>
 * </ul>
 */
@Service
public class StatementParserService {

    private static final Logger log = LoggerFactory.getLogger(StatementParserService.class);

    // ──────────────────────────────────────────────────────────────────────────
    // Table boundary markers (from TS §7)
    // ──────────────────────────────────────────────────────────────────────────

    /** Prefix of the column-header line that marks the start of the transaction table. */
    static final String TABLE_START_MARKER = "Date Amount Transaction";

    /** Prefix of the footnote line that marks the end of the transaction table. */
    static final String TABLE_END_MARKER = "The section";

    // ──────────────────────────────────────────────────────────────────────────
    // Page header / footer lines to skip
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Pattern matching page-level header/footer lines injected between transactions
     * on every page break.  These are NOT transactions and must be skipped.
     *
     * <ul>
     *   <li>{@code JSC «Kaspi Bank», BIC CASPKZKA, www.kaspi.kz}</li>
     *   <li>{@code Appendix to Statement No.1246706548 dated July 27 2026}</li>
     * </ul>
     */
    private static final Pattern SKIP_PATTERN = Pattern.compile(
            "JSC.+Kaspi Bank|Appendix to Statement No\\.",
            Pattern.CASE_INSENSITIVE
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Main transaction regex (derived from TS §7 and validated on the real PDF)
    //
    // Groups:
    //   1 = date        DD.MM.YY
    //   2 = sign        + or -
    //   3 = amount      e.g. "400 000,00" or "480,00"
    //   4 = opType      e.g. "Purchases", "Transfer to your"
    //   5 = merchant    everything after the 4+ space gap
    // ──────────────────────────────────────────────────────────────────────────

    private static final Pattern TX_PATTERN = Pattern.compile(
            "^(\\d{2}\\.\\d{2}\\.\\d{2})" // group 1 — date
            + "\\s+([+\\-])"               // group 2 — sign (+ or -)
            + "\\s+([\\d\\s]+,\\d{2})"     // group 3 — amount (space as thousands sep, comma as decimal)
            + "\\s*\\S?"                   // optional currency symbol (₸ or encoding artifact)
            + "\\s{2,}"                    // gap between currency and operation type
            + "(.+?)"                      // group 4 — operation type (non-greedy)
            + "\\s{4,}"                    // gap between operation type and merchant (≥4 spaces)
            + "(.+)$"                      // group 5 — merchant / details
    );

    /** Two-digit year date format used in Kaspi statements: {@code DD.MM.YY}. */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");

    /**
     * The partial operation-type text that appears on the <em>first</em> line of
     * the two-line "Transfer to your account" anomaly.
     */
    private static final String PARTIAL_TRANSFER = "Transfer to your";

    /**
     * The standalone word that appears on the <em>second</em> (continuation) line
     * of the "Transfer to your account" anomaly.
     */
    private static final String CONTINUATION_WORD = "account";

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Parses the raw multi-line text from a Kaspi Bank PDF statement.
     *
     * @param rawText full text output of {@code PDFTextStripper.getText()}; may be null
     * @return ordered list of parsed transactions; empty if no table found or input is blank
     */
    public List<ParsedTransaction> parse(String rawText) {
        List<ParsedTransaction> result = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            return result;
        }

        String[] lines = rawText.split("\\r?\\n", -1);

        boolean inTable = false;

        // Holds the first line of a two-line "Transfer to your / account" transaction
        // while we wait to confirm the continuation line.
        ParsedTransaction pendingTransfer = null;

        for (String raw : lines) {
            String line = raw.trim();

            // ── Step 1: Wait for the table-start marker ──────────────────────
            if (!inTable) {
                if (line.startsWith(TABLE_START_MARKER)) {
                    inTable = true;
                    log.debug("StatementParser: table start found");
                }
                continue;
            }

            // ── Step 2: Stop at the table-end marker ─────────────────────────
            if (line.startsWith(TABLE_END_MARKER)) {
                log.debug("StatementParser: table end found");
                break;
            }

            // ── Step 3: Skip page header / footer lines ──────────────────────
            if (SKIP_PATTERN.matcher(line).find()) {
                log.debug("StatementParser: skipping header/footer: '{}'", line);
                continue;
            }

            // ── Step 4: Handle "account" continuation line ───────────────────
            if (line.equalsIgnoreCase(CONTINUATION_WORD) && pendingTransfer != null) {
                // Upgrade operation type to the full TRANSFER_TO_YOUR_ACCOUNT
                result.add(new ParsedTransaction(
                        pendingTransfer.date(),
                        pendingTransfer.sign(),
                        pendingTransfer.amount(),
                        OperationType.TRANSFER_TO_YOUR_ACCOUNT,
                        pendingTransfer.merchantDetails()
                ));
                pendingTransfer = null;
                continue;
            }

            // ── Step 5: Flush stale pending (guard — not expected in practice) ─
            if (pendingTransfer != null) {
                log.warn("StatementParser: 'Transfer to your' was NOT followed by 'account' — "
                         + "flushing with TRANSFER_TO_YOUR_ACCOUNT type anyway. Next line was: '{}'", line);
                result.add(pendingTransfer);
                pendingTransfer = null;
            }

            // ── Step 6: Skip empty lines ─────────────────────────────────────
            if (line.isEmpty()) continue;

            // ── Step 7: Match against transaction regex ───────────────────────
            Matcher m = TX_PATTERN.matcher(line);
            if (!m.matches()) {
                log.warn("StatementParser: unrecognised line skipped: '{}'", line);
                continue;
            }

            LocalDate  date     = LocalDate.parse(m.group(1), DATE_FMT);
            SignType   sign     = SignType.fromChar(m.group(2).charAt(0));
            BigDecimal amount   = parseAmount(m.group(3));
            String     rawOp   = m.group(4).trim();
            String     merchant = m.group(5).trim();

            // ── Step 8: Detect first line of two-line "Transfer to your account" ─
            if (rawOp.equalsIgnoreCase(PARTIAL_TRANSFER)) {
                // Store pending; wait for the "account" continuation line
                pendingTransfer = new ParsedTransaction(
                        date, sign, amount, OperationType.TRANSFER_TO_YOUR_ACCOUNT, merchant
                );
                continue;
            }

            // ── Step 9: Resolve operation type ───────────────────────────────
            OperationType opType = OperationType.fromPdfLabel(rawOp).orElse(null);
            if (opType == null) {
                log.warn("StatementParser: unknown operationType '{}' on line: '{}'", rawOp, line);
                continue;
            }

            result.add(new ParsedTransaction(date, sign, amount, opType, merchant));
        }

        // Flush any remaining pending at end of input (defensive)
        if (pendingTransfer != null) {
            log.warn("StatementParser: pending 'Transfer to your' at end of text — flushing as-is.");
            result.add(pendingTransfer);
        }

        log.info("StatementParser: successfully parsed {} transactions", result.size());
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts a raw amount string (e.g. {@code "400 000,00"} or {@code "480,00"})
     * to a positive {@link BigDecimal}.
     *
     * <ul>
     *   <li>Removes space thousands-separators.</li>
     *   <li>Replaces the decimal comma with a period.</li>
     * </ul>
     */
    private BigDecimal parseAmount(String raw) {
        String normalized = raw
                .replace(" ", "")   // "400 000,00"  → "400000,00"
                .replace(",", "."); // "400000,00"   → "400000.00"
        return new BigDecimal(normalized);
    }
}
