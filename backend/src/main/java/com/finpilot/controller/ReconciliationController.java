package com.finpilot.controller;

import com.finpilot.dto.ReconciliationSummaryDto;
import com.finpilot.model.PaymentTransaction;
import com.finpilot.model.ReconciliationBatch;
import com.finpilot.model.ReconciliationItem;
import com.finpilot.model.ReconciliationStatus;
import com.finpilot.model.SettlementRecord;
import com.finpilot.repository.ReconciliationBatchRepository;
import com.finpilot.service.AiInvestigationService;
import com.finpilot.service.CsvParserService;
import com.finpilot.service.ReconciliationEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for financial reconciliation endpoints.
 * Base path: /reconcile
 */
@RestController
@RequestMapping("/reconcile")
public class ReconciliationController {

    private final CsvParserService csvParserService;
    private final ReconciliationEngine reconciliationEngine;
    private final ReconciliationBatchRepository batchRepository;
    private final AiInvestigationService aiInvestigationService;

    public ReconciliationController(CsvParserService csvParserService,
                                  ReconciliationEngine reconciliationEngine,
                                  ReconciliationBatchRepository batchRepository,
                                  AiInvestigationService aiInvestigationService) {
        this.csvParserService = csvParserService;
        this.reconciliationEngine = reconciliationEngine;
        this.batchRepository = batchRepository;
        this.aiInvestigationService = aiInvestigationService;
    }

    /**
     * POST /reconcile/upload
     * Accepts two multipart CSV files, reconciles them deterministically, saves to MongoDB,
     * and returns the resulting ReconciliationBatch.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndReconcile(
            @RequestParam("paymentsFile") MultipartFile paymentsFile,
            @RequestParam("settlementsFile") MultipartFile settlementsFile,
            @RequestParam(value = "batchName", required = false) String batchName) {

        if (paymentsFile == null || paymentsFile.isEmpty()) {
            return ResponseEntity.badRequest().body("paymentsFile must be provided and cannot be empty.");
        }
        if (settlementsFile == null || settlementsFile.isEmpty()) {
            return ResponseEntity.badRequest().body("settlementsFile must be provided and cannot be empty.");
        }

        try {
            List<PaymentTransaction> payments = csvParserService.parsePayments(paymentsFile.getInputStream());
            List<SettlementRecord> settlements = csvParserService.parseSettlements(settlementsFile.getInputStream());

            String effectiveBatchName = (batchName != null && !batchName.trim().isEmpty())
                    ? batchName.trim()
                    : "Batch-" + System.currentTimeMillis();

            ReconciliationBatch batch = reconciliationEngine.reconcile(effectiveBatchName, payments, settlements);
            ReconciliationBatch savedBatch = batchRepository.save(batch);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedBatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process reconciliation: " + e.getMessage());
        }
    }

    /**
     * POST /reconcile/batches/{batchId}/investigate
     * Triggers autonomous AI investigation (Gemini 2.5 Flash) on all unresolved exceptions for the given batch.
     */
    @PostMapping("/batches/{batchId}/investigate")
    public ResponseEntity<?> investigateBatch(@PathVariable String batchId) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    ReconciliationBatch investigatedBatch = aiInvestigationService.investigateBatch(batch);
                    ReconciliationBatch savedBatch = batchRepository.save(investigatedBatch);
                    return ResponseEntity.ok(savedBatch);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /reconcile/batches
     * Returns a summary list of all historical reconciliation batches.
     */
    @GetMapping("/batches")
    public ResponseEntity<List<ReconciliationSummaryDto>> getAllBatches() {
        List<ReconciliationBatch> batches = batchRepository.findAllByOrderByProcessedAtDesc();
        List<ReconciliationSummaryDto> summaries = batches.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    /**
     * GET /reconcile/batches/{batchId}
     * Returns the complete batch details and all line items for the given batch ID.
     */
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<?> getBatchById(@PathVariable String batchId) {
        return batchRepository.findById(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /reconcile/batches/{batchId}/exceptions
     * Returns only the discrepancy records (AMOUNT_MISMATCH, MISSING_SETTLEMENT, DUPLICATE_TRANSACTION, UNKNOWN_TRANSACTION).
     */
    @GetMapping("/batches/{batchId}/exceptions")
    public ResponseEntity<?> getExceptionsByBatchId(@PathVariable String batchId) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    List<ReconciliationItem> exceptions = batch.getItems().stream()
                            .filter(item -> item.getStatus() != ReconciliationStatus.MATCHED)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(exceptions);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ReconciliationSummaryDto toSummaryDto(ReconciliationBatch batch) {
        return ReconciliationSummaryDto.builder()
                .id(batch.getId())
                .batchName(batch.getBatchName())
                .processedAt(batch.getProcessedAt())
                .totalPayments(batch.getTotalPayments())
                .totalSettlements(batch.getTotalSettlements())
                .matchedCount(batch.getMatchedCount())
                .amountMismatchCount(batch.getAmountMismatchCount())
                .missingSettlementCount(batch.getMissingSettlementCount())
                .duplicateCount(batch.getDuplicateCount())
                .unknownCount(batch.getUnknownCount())
                .totalPaymentValue(batch.getTotalPaymentValue())
                .totalSettledValue(batch.getTotalSettledValue())
                .totalDiscrepancyValue(batch.getTotalDiscrepancyValue())
                .build();
    }
}
