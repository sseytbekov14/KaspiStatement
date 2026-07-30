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
        List<Transaction> uncategorizedTransactions = transactionRepository.findUncategorizedTransactions();
        if (uncategorizedTransactions.isEmpty()) {
            log.info("No uncategorized transactions found for backfill.");
            return 0;
        }

        log.info("Found {} uncategorized transactions to process.", uncategorizedTransactions.size());

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
            }
        }

        log.info("Backfill complete. Successfully mapped {} out of {} transactions to real categories.", 
                 updatedCount, uncategorizedTransactions.size());
                 
        return updatedCount;
    }
}
