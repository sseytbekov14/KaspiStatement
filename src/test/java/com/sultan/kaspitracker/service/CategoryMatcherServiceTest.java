package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.entity.Category;
import com.sultan.kaspitracker.entity.MappingSource;
import com.sultan.kaspitracker.entity.MerchantCategoryMapping;
import com.sultan.kaspitracker.repository.MerchantCategoryMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryMatcherServiceTest {

    private MerchantCategoryMappingRepository repositoryMock;
    private AiCategorizationService aiCategorizationServiceMock;
    private CategoryMatcherService matcherService;

    private Category groceriesCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        repositoryMock = mock(MerchantCategoryMappingRepository.class);
        aiCategorizationServiceMock = mock(AiCategorizationService.class);
        matcherService = new CategoryMatcherService(repositoryMock, aiCategorizationServiceMock);

        groceriesCategory = new Category("Groceries");
        transportCategory = new Category("Transport");

        List<MerchantCategoryMapping> mockMappings = List.of(
                new MerchantCategoryMapping("MAGNUM", groceriesCategory, MappingSource.MANUAL, Instant.now()),
                new MerchantCategoryMapping("YANDEX.GO", transportCategory, MappingSource.MANUAL, Instant.now()),
                new MerchantCategoryMapping("SMALL", groceriesCategory, MappingSource.MANUAL, Instant.now())
        );

        when(repositoryMock.findAll()).thenReturn(mockMappings);
    }

    @Test
    @DisplayName("Should return empty for null or blank input")
    void matchCategory_nullOrEmpty() {
        assertThat(matcherService.matchCategory(null)).isEmpty();
        assertThat(matcherService.matchCategory("")).isEmpty();
        assertThat(matcherService.matchCategory("   ")).isEmpty();
    }

    @Test
    @DisplayName("Should find exact match after normalization")
    void matchCategory_exactMatch() {
        // "magnum" -> "MAGNUM"
        Optional<Category> result = matcherService.matchCategory("magnum");
        assertThat(result).isPresent().contains(groceriesCategory);

        // "  Yandex.Go  " -> "YANDEX.GO"
        Optional<Category> result2 = matcherService.matchCategory("  Yandex.Go  ");
        assertThat(result2).isPresent().contains(transportCategory);
    }

    @Test
    @DisplayName("Should find fuzzy match for slight variations and typos")
    void matchCategory_fuzzyMatch() {
        // Typos or extra words should match "MAGNUM" if Jaro-Winkler score is >= 0.85
        Optional<Category> result = matcherService.matchCategory("MAGNUM CASH&CARRY");
        assertThat(result).isPresent().contains(groceriesCategory);

        Optional<Category> result2 = matcherService.matchCategory("SMALLL");
        assertThat(result2).isPresent().contains(groceriesCategory);
    }

    @Test
    @DisplayName("Should return empty for scores below threshold")
    void matchCategory_belowThreshold() {
        // "KFC" is too different from "MAGNUM", "SMALL", "YANDEX.GO"
        Optional<Category> result = matcherService.matchCategory("KFC");
        assertThat(result).isEmpty();

        // Completely unrelated
        Optional<Category> result2 = matcherService.matchCategory("UNKNOWN MERCHANT");
        assertThat(result2).isEmpty();
    }
}
