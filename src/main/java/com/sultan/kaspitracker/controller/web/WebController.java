package com.sultan.kaspitracker.controller.web;

import com.sultan.kaspitracker.entity.Transaction;
import com.sultan.kaspitracker.parser.SignType;
import com.sultan.kaspitracker.repository.TransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sultan.kaspitracker.repository.StatementRepository;

@Controller
public class WebController {

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;

    public WebController(TransactionRepository transactionRepository, StatementRepository statementRepository) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        allTransactions.sort(Comparator.comparing(Transaction::getDate).reversed());

        List<Transaction> debitTransactions = allTransactions.stream()
                .filter(t -> t.getSign() == SignType.DEBIT)
                .toList();

        BigDecimal totalExpenses = debitTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> expensesByCategory = debitTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        String topCategory = expensesByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        List<String> chartLabels = expensesByCategory.keySet().stream().toList();
        List<BigDecimal> chartData = expensesByCategory.values().stream().toList();

        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("transactionCount", allTransactions.size());
        model.addAttribute("transactions", allTransactions.stream().limit(10).toList()); // Recent 10 for dashboard
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/statements")
    public String statements(Model model) {
        model.addAttribute("statements", statementRepository.findAll());
        return "statements";
    }

    @GetMapping("/statements/{id}")
    public String statementDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("statementId", id);
        return "statement_details";
    }
}
