package com.sultan.kaspitracker.controller.api;

import com.sultan.kaspitracker.entity.Category;
import com.sultan.kaspitracker.entity.Statement;
import com.sultan.kaspitracker.entity.Transaction;
import com.sultan.kaspitracker.parser.OperationType;
import com.sultan.kaspitracker.parser.SignType;
import com.sultan.kaspitracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionApiController.class)
@Import(com.sultan.kaspitracker.config.SecurityConfig.class)
@WithMockUser
public class TransactionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    public void testGetTransactions() throws Exception {
        Statement stmt = new Statement("hash", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), Instant.now());
        Transaction tx = new Transaction(stmt, LocalDate.of(2026, 1, 5), SignType.DEBIT, new BigDecimal("1000.00"), OperationType.PURCHASES, "MAGNUM");
        Category cat = new Category("Groceries");
        tx.setCategory(cat);

        when(transactionRepository.findByStatementIdOrderByDateDesc(anyLong())).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/transactions?statementId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant").value("MAGNUM"))
                .andExpect(jsonPath("$[0].amount").value(1000.00))
                .andExpect(jsonPath("$[0].categoryName").value("Groceries"));
    }

    @Test
    public void testGetAnalyticsSummary() throws Exception {
        Statement stmt = new Statement("hash", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), Instant.now());
        
        Transaction tx1 = new Transaction(stmt, LocalDate.of(2026, 1, 5), SignType.DEBIT, new BigDecimal("1000.00"), OperationType.PURCHASES, "MAGNUM");
        tx1.setCategory(new Category("Groceries"));

        Transaction tx2 = new Transaction(stmt, LocalDate.of(2026, 1, 6), SignType.DEBIT, new BigDecimal("500.00"), OperationType.PURCHASES, "UNKNOWN");
        // tx2 has no category

        Transaction tx3 = new Transaction(stmt, LocalDate.of(2026, 1, 7), SignType.CREDIT, new BigDecimal("5000.00"), OperationType.REPLENISHMENT, "Salary");
        
        when(transactionRepository.findByStatementId(anyLong())).thenReturn(List.of(tx1, tx2, tx3));

        mockMvc.perform(get("/api/analytics/summary?statementId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDebit").value(1500.00))
                .andExpect(jsonPath("$.totalCredit").value(5000.00))
                .andExpect(jsonPath("$.categories.length()").value(2))
                // Ordered by amount descending, so Groceries (1000) should be first, then Uncategorized (500)
                .andExpect(jsonPath("$.categories[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.categories[0].totalAmount").value(1000.00))
                .andExpect(jsonPath("$.categories[1].categoryName").value("Uncategorized"))
                .andExpect(jsonPath("$.categories[1].totalAmount").value(500.00));
    }
}
