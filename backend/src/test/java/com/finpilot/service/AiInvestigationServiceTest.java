package com.finpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiInvestigationServiceTest {

    private AiInvestigationService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiInvestigationService(new ObjectMapper());
    }

    @Test
    @DisplayName("Should populate AI investigation for exceptions and ignore MATCHED items")
    void testInvestigateBatch() {
        ReconciliationItem matched = ReconciliationItem.builder()
                .transactionId("TXN_1001")
                .status(ReconciliationStatus.MATCHED)
                .paymentAmount(new BigDecimal("1500.00"))
                .settledAmount(new BigDecimal("1500.00"))
                .currency("INR")
                .build();

        ReconciliationItem mismatch = ReconciliationItem.builder()
                .transactionId("TXN_1003")
                .orderId("ORD_5003")
                .status(ReconciliationStatus.AMOUNT_MISMATCH)
                .paymentAmount(new BigDecimal("5000.00"))
                .settledAmount(new BigDecimal("4900.00"))
                .differenceAmount(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        ReconciliationItem missing = ReconciliationItem.builder()
                .transactionId("TXN_1004")
                .orderId("ORD_5004")
                .status(ReconciliationStatus.MISSING_SETTLEMENT)
                .paymentAmount(new BigDecimal("1200.00"))
                .differenceAmount(new BigDecimal("1200.00"))
                .currency("INR")
                .build();

        ReconciliationItem duplicate = ReconciliationItem.builder()
                .transactionId("TXN_1005")
                .orderId("ORD_5005")
                .status(ReconciliationStatus.DUPLICATE_TRANSACTION)
                .paymentAmount(new BigDecimal("850.00"))
                .currency("INR")
                .build();

        ReconciliationItem unknown = ReconciliationItem.builder()
                .settlementId("SETTL_9005")
                .status(ReconciliationStatus.UNKNOWN_TRANSACTION)
                .settledAmount(new BigDecimal("750.00"))
                .differenceAmount(new BigDecimal("750.00"))
                .currency("INR")
                .bankReference("UTRIB00009999")
                .build();

        List<ReconciliationItem> items = new ArrayList<>(List.of(matched, mismatch, missing, duplicate, unknown));
        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id("batch_test_ai")
                .items(items)
                .build();

        ReconciliationBatch result = aiService.investigateBatch(batch);

        assertNotNull(result);
        assertNull(matched.getAiInvestigation(), "Matched items should not have an AI investigation");

        // Verify AMOUNT_MISMATCH investigation
        assertNotNull(mismatch.getAiInvestigation());
        assertTrue(mismatch.getAiInvestigation().getPossibleReason().contains("MDR"));
        assertTrue(mismatch.getAiInvestigation().getConfidence() >= 0.9);
        assertNotNull(mismatch.getAiInvestigation().getRecommendedAction());
        assertNotNull(mismatch.getAiInvestigation().getEvidence());

        // Verify MISSING_SETTLEMENT investigation
        assertNotNull(missing.getAiInvestigation());
        assertTrue(missing.getAiInvestigation().getPossibleReason().contains("settlement cycle"));

        // Verify DUPLICATE_TRANSACTION investigation
        assertNotNull(duplicate.getAiInvestigation());
        assertTrue(duplicate.getAiInvestigation().getPossibleReason().toLowerCase().contains("duplicate"));

        // Verify UNKNOWN_TRANSACTION investigation
        assertNotNull(unknown.getAiInvestigation());
        assertTrue(unknown.getAiInvestigation().getPossibleReason().contains("without an originating gateway"));
    }
}
