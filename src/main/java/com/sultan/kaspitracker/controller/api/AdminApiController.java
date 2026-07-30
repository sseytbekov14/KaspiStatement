package com.sultan.kaspitracker.controller.api;

import com.sultan.kaspitracker.service.BackfillCategorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final BackfillCategorizationService backfillService;

    public AdminApiController(BackfillCategorizationService backfillService) {
        this.backfillService = backfillService;
    }

    @PostMapping("/backfill-categories")
    public ResponseEntity<Map<String, Object>> triggerBackfill() {
        Map<String, Integer> stats = backfillService.backfillUncategorizedTransactions();
        int foundCount = stats.get("found");
        int updatedCount = stats.get("updated");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "foundCount", foundCount,
                "updatedCount", updatedCount,
                "message", "Backfill completed successfully. Found " + foundCount + " transactions, updated " + updatedCount + "."
        ));
    }
}
