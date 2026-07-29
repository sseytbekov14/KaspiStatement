package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.entity.Statement;
import com.sultan.kaspitracker.entity.Transaction;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.repository.StatementRepository;
import com.sultan.kaspitracker.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class StatementPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(StatementPersistenceService.class);

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryMatcherService categoryMatcherService;

    public StatementPersistenceService(StatementRepository statementRepository,
                                       TransactionRepository transactionRepository,
                                       CategoryMatcherService categoryMatcherService) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.categoryMatcherService = categoryMatcherService;
    }

    /**
     * Saves a parsed statement and its transactions to the database.
     * Uses SHA-256 hash of the original PDF file to prevent duplicate uploads.
     *
     * @param pdfBytes The original raw PDF file bytes.
     * @param parsedTransactions The transactions parsed from the PDF.
     * @return PersistenceResult containing success flag and count, or duplicate flag.
     */
    @Transactional
    public PersistenceResult saveStatement(byte[] pdfBytes, List<ParsedTransaction> parsedTransactions) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF bytes cannot be empty");
        }
        if (parsedTransactions == null || parsedTransactions.isEmpty()) {
            throw new IllegalArgumentException("Parsed transactions cannot be empty");
        }

        String fileHash = calculateSha256(pdfBytes);
        log.debug("Calculated SHA-256 hash for PDF: {}", fileHash);

        Optional<Statement> existing = statementRepository.findByFileHash(fileHash);
        if (existing.isPresent()) {
            log.warn("Statement with hash {} already exists. Rejecting duplicate upload.", fileHash);
            return PersistenceResult.duplicate(existing.get().getId());
        }

        LocalDate periodStart = parsedTransactions.stream()
                .map(ParsedTransaction::date)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        LocalDate periodEnd = parsedTransactions.stream()
                .map(ParsedTransaction::date)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        Statement statement = new Statement(fileHash, periodStart, periodEnd, Instant.now());
        statementRepository.save(statement);
        log.info("Saved new statement with ID {} for period {} to {}", statement.getId(), periodStart, periodEnd);

        List<Transaction> transactionsToSave = parsedTransactions.stream()
                .map(pt -> {
                    Transaction transaction = new Transaction(
                            statement,
                            pt.date(),
                            pt.sign(),
                            pt.amount(),
                            pt.operationType(),
                            pt.merchantDetails()
                    );
                    
                    // Attempt to categorize
                    categoryMatcherService.matchCategory(pt.merchantDetails())
                            .ifPresent(transaction::setCategory);
                            
                    return transaction;
                })
                .toList();

        transactionRepository.saveAll(transactionsToSave);
        log.info("Successfully saved {} transactions for statement ID {}", transactionsToSave.size(), statement.getId());

        return PersistenceResult.success(transactionsToSave.size(), statement.getId());
    }

    /**
     * Helper to compute SHA-256 hash of a byte array.
     * 
     * Why hashing the file? It perfectly protects against double-processing the exact same PDF
     * without relying on fragile merchant/date matching rules that might falsely flag legitimate
     * identical recurring transactions as duplicates.
     */
    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}
