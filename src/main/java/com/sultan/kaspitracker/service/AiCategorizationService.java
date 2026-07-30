package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.entity.Category;
import com.sultan.kaspitracker.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(AiCategorizationService.class);

    private final ChatModel chatModel;
    private final CategoryRepository categoryRepository;

    public AiCategorizationService(ChatModel chatModel, CategoryRepository categoryRepository) {
        this.chatModel = chatModel;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Categorizes a merchant using Google Gemini.
     * 
     * @param merchantName The raw merchant name from the statement
     * @return The Category if identified, or null if the AI could not confidently categorize it.
     */
    public Category categorizeMerchant(String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            return null;
        }

        List<Category> allCategories = categoryRepository.findAll();
        if (allCategories.isEmpty()) {
            log.warn("No categories found in the database. Cannot use AI fallback.");
            return null;
        }

        String categoryNames = allCategories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String promptText = String.format(
                "You are an expert personal finance assistant. Your task is to categorize a bank transaction based on the merchant's name.\n" +
                "Available categories: [%s].\n\n" +
                "Merchant name: '%s'.\n\n" +
                "Instructions:\n" +
                "1. Choose the most appropriate category for this merchant from the available list.\n" +
                "2. If none of the categories fit well, or if the merchant is ambiguous, return 'Uncategorized'.\n" +
                "3. Your response MUST contain ONLY the exact name of the chosen category or 'Uncategorized'. Do not include any explanations, punctuation, or other text.",
                categoryNames, merchantName
        );

        try {
            log.debug("Calling Gemini API to categorize merchant: '{}'", merchantName);
            String aiResponse = chatModel.call(new org.springframework.ai.chat.prompt.Prompt(promptText, org.springframework.ai.openai.OpenAiChatOptions.builder().withModel("gemini-2.5-flash").build())).getResult().getOutput().getContent();

            try {
                Thread.sleep(15000); // 15 seconds delay to respect the 5 RPM free-tier limit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (aiResponse == null) {
                log.warn("Gemini API returned null response for merchant '{}'", merchantName);
                return null;
            }

            String categoryChoice = aiResponse.trim();
            log.debug("Gemini API responded with: '{}' for merchant '{}'", categoryChoice, merchantName);

            if ("Uncategorized".equalsIgnoreCase(categoryChoice)) {
                return null;
            }

            // Verify the AI's choice exactly matches one of our DB categories
            for (Category cat : allCategories) {
                if (cat.getName().equalsIgnoreCase(categoryChoice)) {
                    return cat;
                }
            }

            log.warn("Gemini API returned an invalid category '{}' for merchant '{}'. Expected one of: [{}]", 
                     categoryChoice, merchantName, categoryNames);
            return null;

        } catch (Exception e) {
            log.error("Failed to categorize merchant '{}' via AI: {}", merchantName, e.getMessage());
            return null;
        }
    }
}
