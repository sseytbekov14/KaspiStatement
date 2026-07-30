package com.sultan.kaspitracker;

import com.sultan.kaspitracker.entity.Category;
import com.sultan.kaspitracker.entity.MappingSource;
import com.sultan.kaspitracker.entity.MerchantCategoryMapping;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.repository.MerchantCategoryMappingRepository;
import com.sultan.kaspitracker.service.AiCategorizationService;
import com.sultan.kaspitracker.service.CategoryMatcherService;
import com.sultan.kaspitracker.service.StatementParserService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorizationStatsScratchTest {

    @Test
    public void printStats() throws Exception {
        MerchantCategoryMappingRepository repo = mock(MerchantCategoryMappingRepository.class);
        
        Category groceries = new Category("Groceries");
        Category transport = new Category("Transport");
        Category subscriptions = new Category("Subscriptions");
        Category other = new Category("Other");
        Category communication = new Category("Communication");
        Category utilities = new Category("Utilities");
        Category entertainment = new Category("Entertainment");

        List<MerchantCategoryMapping> mockMappings = List.of(
            new MerchantCategoryMapping("MAGNUM", groceries, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("SMALL", groceries, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("WOLT", groceries, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("GLOVO", groceries, MappingSource.MANUAL, Instant.now()),
            
            new MerchantCategoryMapping("ONAY", transport, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("YANDEX.GO", transport, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("YANDEX.TAXI", transport, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("UBER", transport, MappingSource.MANUAL, Instant.now()),
            
            new MerchantCategoryMapping("GOOGLE", subscriptions, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("APPLE.COM/BILL", subscriptions, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("NETFLIX", subscriptions, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("SPOTIFY", subscriptions, MappingSource.MANUAL, Instant.now()),
            
            new MerchantCategoryMapping("WORKOUTGYM", other, MappingSource.MANUAL, Instant.now()),
            
            new MerchantCategoryMapping("KCELL", communication, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("TELE2", communication, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("BEELINE", communication, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("ALTEL", communication, MappingSource.MANUAL, Instant.now()),
            
            new MerchantCategoryMapping("KAZAKHTELECOM", utilities, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("ALSECO", utilities, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("ASTANAAERC", utilities, MappingSource.MANUAL, Instant.now()),
            
            new MerchantCategoryMapping("KINO.KZ", entertainment, MappingSource.MANUAL, Instant.now()),
            new MerchantCategoryMapping("TICKETON", entertainment, MappingSource.MANUAL, Instant.now())
        );

        when(repo.findAll()).thenReturn(mockMappings);
        
        AiCategorizationService aiService = mock(AiCategorizationService.class);
        CategoryMatcherService matcherService = new CategoryMatcherService(repo, aiService);
        StatementParserService parserService = new StatementParserService();

        Path pdfPath = Path.of("sample-data", "kaspi_gold_statement.pdf");
        if (!Files.exists(pdfPath)) {
            System.out.println("Could not find sample PDF!");
            return;
        }

        String rawText;
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            stripper.setWordSeparator(" ");
            stripper.setSortByPosition(true); // From exploration test for layout
            rawText = stripper.getText(document);
        }

        List<ParsedTransaction> txs = parserService.parse(rawText);

        int matched = 0;
        int unmatched = 0;
        
        System.out.println("--- MATCHED ---");
        for (ParsedTransaction tx : txs) {
            Optional<Category> category = matcherService.matchCategory(tx.merchantDetails());
            if (category.isPresent()) {
                matched++;
                System.out.println("MATCHED: " + tx.merchantDetails() + " -> " + category.get().getName());
            } else {
                unmatched++;
            }
        }
        
        System.out.println("\n--- UNMATCHED ---");
        for (ParsedTransaction tx : txs) {
            Optional<Category> category = matcherService.matchCategory(tx.merchantDetails());
            if (category.isEmpty()) {
                System.out.println("UNMATCHED: " + tx.merchantDetails());
            }
        }

        System.out.println("\n--- STATS ---");
        System.out.println("Total Transactions: " + txs.size());
        System.out.println("Matched: " + matched + " (" + (matched * 100 / txs.size()) + "%)");
        System.out.println("Unmatched: " + unmatched + " (" + (unmatched * 100 / txs.size()) + "%)");
    }
}
