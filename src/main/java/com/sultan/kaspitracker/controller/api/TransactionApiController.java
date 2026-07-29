package com.sultan.kaspitracker.controller.api;

import com.sultan.kaspitracker.dto.AnalyticsSummaryDto;
import com.sultan.kaspitracker.dto.CategorySummaryDto;
import com.sultan.kaspitracker.dto.TransactionDto;
import com.sultan.kaspitracker.entity.Transaction;
import com.sultan.kaspitracker.parser.SignType;
import com.sultan.kaspitracker.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TransactionApiController {

    private final TransactionRepository transactionRepository;

    public TransactionApiController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(@RequestParam("statementId") Long statementId) {
        List<Transaction> transactions = transactionRepository.findByStatementIdOrderByDateDesc(statementId);
        
        List<TransactionDto> dtos = transactions.stream().map(tx -> new TransactionDto(
            tx.getId(),
            tx.getDate(),
            tx.getMerchantDetails(),
            tx.getAmount(),
            tx.getSign().name(),
            tx.getOperationType().name(),
            tx.getCategory() != null ? tx.getCategory().getName() : null
        )).toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<AnalyticsSummaryDto> getAnalyticsSummary(@RequestParam("statementId") Long statementId) {
        List<Transaction> transactions = transactionRepository.findByStatementId(statementId);

        BigDecimal totalDebit = transactions.stream()
            .filter(tx -> tx.getSign() == SignType.DEBIT)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = transactions.stream()
            .filter(tx -> tx.getSign() == SignType.CREDIT)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group DEBIT transactions by category
        Map<String, BigDecimal> categoryTotals = transactions.stream()
            .filter(tx -> tx.getSign() == SignType.DEBIT)
            .collect(Collectors.groupingBy(
                tx -> tx.getCategory() != null ? tx.getCategory().getName() : "Uncategorized",
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));

        List<CategorySummaryDto> categoryDtos = categoryTotals.entrySet().stream()
            .map(entry -> new CategorySummaryDto(entry.getKey(), entry.getValue()))
            .sorted((c1, c2) -> c2.totalAmount().compareTo(c1.totalAmount())) // Sort descending by amount
            .toList();

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto(totalDebit, totalCredit, categoryDtos);
        
        return ResponseEntity.ok(summary);
    }
}
