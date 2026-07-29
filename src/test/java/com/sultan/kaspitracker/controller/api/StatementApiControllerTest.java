package com.sultan.kaspitracker.controller.api;

import com.sultan.kaspitracker.entity.Statement;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.repository.StatementRepository;
import com.sultan.kaspitracker.repository.TransactionRepository;
import com.sultan.kaspitracker.service.PersistenceResult;
import com.sultan.kaspitracker.service.StatementParserService;
import com.sultan.kaspitracker.service.StatementPersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementApiController.class)
@Import(com.sultan.kaspitracker.config.SecurityConfig.class)
@WithMockUser
public class StatementApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatementParserService parserService;

    @MockBean
    private StatementPersistenceService persistenceService;

    @MockBean
    private StatementRepository statementRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    public void testUpload_EmptyFile_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    public void testUpload_NotPdf_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy".getBytes());

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is not a PDF"));
    }

    @Test
    public void testGetStatements_ReturnsList() throws Exception {
        Statement stmt = new Statement("hash", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), Instant.now());
        // Since we don't have a setter, we can't easily set ID, but we can just use reflection or let it be null (or Mockito).
        // For testing, null ID is fine, JSON will just have null for id.
        
        when(statementRepository.findAll(Mockito.any(org.springframework.data.domain.Sort.class)))
            .thenReturn(List.of(stmt));
            
        when(transactionRepository.countByStatementId(any())).thenReturn(5);

        mockMvc.perform(get("/api/statements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodStart").value("2026-01-01"))
                .andExpect(jsonPath("$[0].transactionCount").value(5));
    }
}
