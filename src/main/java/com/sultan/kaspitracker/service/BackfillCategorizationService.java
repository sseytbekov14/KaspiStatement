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
    @org.springframework.scheduling.annotation.Async
    @Transactional
    public void backfillUncategorizedTransactions() {
        long totalTx = transactionRepository.count();
        log.info("Total transactions in DB according to repository: {}", totalTx);

        List<Transaction> uncategorizedTransactions = transactionRepository.findUncategorizedTransactions();
        log.info("Found {} uncategorized transactions via Native Query", uncategorizedTransactions.size());

        if (uncategorizedTransactions.isEmpty()) {
            log.info("No uncategorized transactions found for backfill.");
            return;
        }

        int updatedCount = 0;
        for (Transaction transaction : uncategorizedTransactions) {
            String merchantDetails = transaction.getMerchantDetails();
            
            // log the exact merchants being sent to categorization
            log.info("Processing merchant for backfill: '{}'", merchantDetails);
            
            Optional<Category> categoryOpt = categoryMatcherService.matchCategory(merchantDetails);
            if (categoryOpt.isPresent()) {
                Category newCat = categoryOpt.get();
                // Check if the AI returned a real category, not Uncategorized
                if (newCat.getName() != null && !newCat.getName().equalsIgnoreCase("Uncategorized") && 
                    !newCat.getName().equalsIgnoreCase("None") && !newCat.getName().equalsIgnoreCase("Other") && 
                    !newCat.getName().equalsIgnoreCase("Без категории")) {
                    
                    transaction.setCategory(newCat);
                    transactionRepository.save(transaction); // Explicitly save to ensure flush
                    updatedCount++;
                } else {
                    log.info("AI still returned Uncategorized for merchant '{}', skipping count.", merchantDetails);
                }
            } else {
                log.info("AI returned null (or error) for merchant '{}'", merchantDetails);
            }
        }

        log.info("Backfill complete. Successfully mapped {} out of {} transactions to real categories.", 
                 updatedCount, uncategorizedTransactions.size());
    }
}
