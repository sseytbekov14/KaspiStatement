package com.sultan.kaspitracker.controller.api;

import com.sultan.kaspitracker.dto.StatementDto;
import com.sultan.kaspitracker.dto.UploadResponseDto;
import com.sultan.kaspitracker.entity.Statement;
import com.sultan.kaspitracker.parser.ParsedTransaction;
import com.sultan.kaspitracker.repository.StatementRepository;
import com.sultan.kaspitracker.repository.TransactionRepository;
import com.sultan.kaspitracker.service.PersistenceResult;
import com.sultan.kaspitracker.service.StatementParserService;
import com.sultan.kaspitracker.service.StatementPersistenceService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class StatementApiController {

    private static final Logger log = LoggerFactory.getLogger(StatementApiController.class);

    private final StatementParserService parserService;
    private final StatementPersistenceService persistenceService;
    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;

    public StatementApiController(StatementParserService parserService,
                                  StatementPersistenceService persistenceService,
                                  StatementRepository statementRepository,
                                  TransactionRepository transactionRepository) {
        this.parserService = parserService;
        this.persistenceService = persistenceService;
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDto> uploadStatement(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!"application/pdf".equals(file.getContentType()) && 
            (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf"))) {
            throw new IllegalArgumentException("File is not a PDF");
        }

        try {
            byte[] fileBytes = file.getBytes();
            
            // Extract text using PDFBox
            String rawText;
            try (PDDocument document = Loader.loadPDF(fileBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setLineSeparator("\n");
                stripper.setWordSeparator(" ");
                stripper.setSortByPosition(true);
                rawText = stripper.getText(document);
            }

            List<ParsedTransaction> parsedTransactions = parserService.parse(rawText);
            
            if (parsedTransactions.isEmpty()) {
                throw new IllegalArgumentException("No transactions found in the PDF. Are you sure it is a Kaspi English statement?");
            }

            PersistenceResult result = persistenceService.saveStatement(fileBytes, parsedTransactions);

            UploadResponseDto response = new UploadResponseDto(
                result.success(),
                result.isDuplicate(),
                result.savedTransactionsCount(),
                result.statementId(),
                result.isDuplicate() ? "Statement already exists in the system." : "Successfully uploaded and parsed."
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to read PDF file", e);
            throw new IllegalArgumentException("Failed to read or parse the PDF file", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<StatementDto>> getStatements() {
        List<Statement> statements = statementRepository.findAll(Sort.by(Sort.Direction.DESC, "uploadedAt"));
        
        List<StatementDto> dtos = statements.stream().map(stmt -> {
            int txCount = transactionRepository.countByStatementId(stmt.getId());
            return new StatementDto(
                stmt.getId(),
                stmt.getPeriodStart(),
                stmt.getPeriodEnd(),
                stmt.getUploadedAt(),
                txCount
            );
        }).toList();
        
        return ResponseEntity.ok(dtos);
    }
}
