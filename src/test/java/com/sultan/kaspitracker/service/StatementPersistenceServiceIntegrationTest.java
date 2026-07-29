package com.sultan.kaspitracker.service;

import com.sultan.kaspitracker.TestcontainersConfiguration;
import com.sultan.kaspitracker.entity.Statement;
import com.sultan.kaspitracker.entity.Transaction;
import com.sultan.kaspitracker.parser.OperationType;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.parser.SignType;
import com.sultan.kaspitracker.repository.StatementRepository;
import com.sultan.kaspitracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional // Rollback after each test
@Disabled("Docker Desktop 4.75 / Testcontainers API incompatibility — see Technical Specification.md open issues")
class StatementPersistenceServiceIntegrationTest {

    @Autowired
    private StatementPersistenceService persistenceService;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        // Since we are using @Transactional on the class level, data is rolled back automatically.
        // However, we just ensure it's empty to be safe.
        transactionRepository.deleteAll();
        statementRepository.deleteAll();
    }

    @Test
    @DisplayName("Successfully saves a new statement and its transactions")
    void saveStatement_success() {
        byte[] pdfBytes = "Dummy PDF Content 1".getBytes(StandardCharsets.UTF_8);
        List<ParsedTransaction> parsed = List.of(
                new ParsedTransaction(LocalDate.of(2026, 7, 10), SignType.DEBIT, new BigDecimal("500.00"), OperationType.PURCHASES, "Magnum"),
                new ParsedTransaction(LocalDate.of(2026, 7, 12), SignType.CREDIT, new BigDecimal("1000.00"), OperationType.REPLENISHMENT, "Salary")
        );

        PersistenceResult result = persistenceService.saveStatement(pdfBytes, parsed);

        assertThat(result.success()).isTrue();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.savedTransactionsCount()).isEqualTo(2);

        List<Statement> statements = statementRepository.findAll();
        assertThat(statements).hasSize(1);
        Statement savedStatement = statements.get(0);
        assertThat(savedStatement.getPeriodStart()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(savedStatement.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 7, 12));

        List<Transaction> transactions = transactionRepository.findByStatementId(savedStatement.getId());
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(Transaction::getMerchantDetails)
                .containsExactlyInAnyOrder("Magnum", "Salary");
        
        // Assert category is null on initial save
        assertThat(transactions.get(0).getCategory()).isNull();
    }

    @Test
    @DisplayName("Rejects duplicate upload based on file hash")
    void saveStatement_duplicateRejected() {
        byte[] pdfBytes = "Dummy PDF Content 2".getBytes(StandardCharsets.UTF_8);
        List<ParsedTransaction> parsed = List.of(
                new ParsedTransaction(LocalDate.of(2026, 7, 10), SignType.DEBIT, new BigDecimal("500.00"), OperationType.PURCHASES, "Magnum")
        );

        // First upload should succeed
        PersistenceResult firstResult = persistenceService.saveStatement(pdfBytes, parsed);
        assertThat(firstResult.success()).isTrue();

        // Second upload with the SAME byte array should be flagged as duplicate
        PersistenceResult secondResult = persistenceService.saveStatement(pdfBytes, parsed);
        
        assertThat(secondResult.success()).isFalse();
        assertThat(secondResult.isDuplicate()).isTrue();
        assertThat(secondResult.savedTransactionsCount()).isEqualTo(0);

        // Verify DB only has 1 statement and 1 transaction
        assertThat(statementRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }
}
