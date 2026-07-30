package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.entity.Category;
import com.sultan.kaspitracker.entity.Transaction;
import com.sultan.kaspitracker.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BackfillCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(BackfillCategorizationService.class);

    private final TransactionRepository transactionRepository;
    private final CategoryMatcherService categoryMatcherService;

    public BackfillCategorizationService(TransactionRepository transactionRepository,
                                         CategoryMatcherService categoryMatcherService) {
        this.transactionRepository = transactionRepository;
        this.categoryMatcherService = categoryMatcherService;
    }

    /**
     * Backfills categories for all transactions that currently have no category.
     * Uses the CategoryMatcherService to resolve them (Exact -> Fuzzy -> AI Fallback).
     * 
     * @return The number of transactions successfully updated.
     */
    @Transactional
    public int backfillUncategorizedTransactions() {
        List<Transaction> uncategorizedTransactions = transactionRepository.findByCategoryIsNull();
        if (uncategorizedTransactions.isEmpty()) {
            log.info("No uncategorized transactions found for backfill.");
            return 0;
        }

        log.info("Found {} uncategorized transactions. Starting backfill...", uncategorizedTransactions.size());

        int updatedCount = 0;
        for (Transaction transaction : uncategorizedTransactions) {
            String merchantDetails = transaction.getMerchantDetails();
            
            Optional<Category> categoryOpt = categoryMatcherService.matchCategory(merchantDetails);
            if (categoryOpt.isPresent()) {
                transaction.setCategory(categoryOpt.get());
                updatedCount++;
            }
        }

        // Transactions are saved automatically due to @Transactional and JPA dirty checking
        log.info("Backfill complete. Successfully categorized {} out of {} transactions.", 
                 updatedCount, uncategorizedTransactions.size());
                 
        return updatedCount;
    }
}
