package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.entity.Category;
import com.sultan.kaspitracker.entity.MerchantCategoryMapping;
import com.sultan.kaspitracker.repository.MerchantCategoryMappingRepository;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryMatcherService {

    private static final Logger log = LoggerFactory.getLogger(CategoryMatcherService.class);
    
    // Configurable threshold for Jaro-Winkler distance
    // 0.0 is a perfect match. 0.15 allows for some typos or missing suffixes/prefixes (equivalent to 0.85 similarity).
    private static final double MAX_DISTANCE_THRESHOLD = 0.15;

    private final MerchantCategoryMappingRepository mappingRepository;
    private final JaroWinklerDistance jaroWinkler = new JaroWinklerDistance();

    public CategoryMatcherService(MerchantCategoryMappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    /**
     * Attempts to find a category for a raw merchant string.
     * Uses exact matching first, then falls back to fuzzy matching using Jaro-Winkler distance.
     * 
     * Note: Cyrillic strings (e.g. "Живая Вода") will generally not match against Latin 
     * dictionaries unless explicitly mapped. Cross-lingual matching is beyond the scope of 
     * this rule-based matcher and will be handled by a future AI fallback mechanism.
     *
     * @param rawMerchant The raw merchant string from the PDF transaction
     * @return An Optional containing the matched Category, or empty if no match found.
     */
    public Optional<Category> matchCategory(String rawMerchant) {
        if (rawMerchant == null || rawMerchant.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = normalize(rawMerchant);
        
        // Load mappings (simple fetch since data size is very small, ~20-30 rows)
        List<MerchantCategoryMapping> allMappings = mappingRepository.findAll();

        // 1. Try Exact Match
        for (MerchantCategoryMapping mapping : allMappings) {
            if (mapping.getMerchantPattern().equals(normalized)) {
                log.debug("Exact match found for '{}' -> Category ID {}", normalized, mapping.getCategory().getId());
                return Optional.of(mapping.getCategory());
            }
        }

        // 2. Try Fuzzy Match (Jaro-Winkler Distance: 0.0 is identical, 1.0 is completely different)
        MerchantCategoryMapping bestMatch = null;
        double bestDistance = 1.0;

        for (MerchantCategoryMapping mapping : allMappings) {
            double distance = jaroWinkler.apply(normalized, mapping.getMerchantPattern());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = mapping;
            }
        }

        if (bestDistance <= MAX_DISTANCE_THRESHOLD && bestMatch != null) {
            log.debug("Fuzzy match found for '{}' against '{}' (Distance: {}) -> Category ID {}", 
                    normalized, bestMatch.getMerchantPattern(), bestDistance, bestMatch.getCategory().getId());
            return Optional.of(bestMatch.getCategory());
        }

        log.debug("No match found for '{}'. Best fuzzy distance was {} against '{}'", 
                normalized, bestDistance, bestMatch != null ? bestMatch.getMerchantPattern() : "N/A");
        
        return Optional.empty();
    }

    private String normalize(String input) {
        return input.trim().toUpperCase().replaceAll("\\s+", " ");
    }
}
