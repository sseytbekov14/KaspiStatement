package com.sultan.kaspitracker.repository;

import com.sultan.kaspitracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByStatementId(Long statementId);
    
    List<Transaction> findByStatementIdOrderByDateDesc(Long statementId);
    
    int countByStatementId(Long statementId);
    
    boolean existsByDateAndAmountAndMerchantDetailsAndOperationType(
            java.time.LocalDate date, 
            java.math.BigDecimal amount, 
            String merchantDetails, 
            com.sultan.kaspitracker.parser.OperationType operationType
    );

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t LEFT JOIN t.category c WHERE c IS NULL OR c.name = 'Uncategorized'")
    List<Transaction> findUncategorizedTransactions();
}
