package com.finpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.model.AiInvestigation;
import com.finpilot.model.ReconciliationBatch;
import com.finpilot.model.ReconciliationItem;
import com.finpilot.model.ReconciliationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live verification test for Google Gemini (gemini-2.5-flash).
 * Reads the GEMINI_API_KEY from the system environment and verifies
 * real LLM responses against our sample reconciliation exceptions.
 */
class GeminiLiveVerificationTest {

    private static final String FALLBACK_MARKER = "Potential payment gateway MDR fee";

    @Test
    @DisplayName("Verify real Gemini 2.5 Flash API call with sample reconciliation exceptions")
    void testRealGeminiApiCall() {
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("\n================================================================================");
            System.err.println("[WARNING] GEMINI_API_KEY environment variable is NOT SET!");
            System.err.println("To run a live verification with real Gemini 2.5 Flash, set it in PowerShell:");
            System.err.println("  $env:GEMINI_API_KEY=\"your_actual_api_key_here\"");
            System.err.println("Then re-run this test.");
            System.err.println("================================================================================\n");
            fail("GEMINI_API_KEY is not configured. Please set the environment variable and try again.");
            return;
        }

        // Initialize service and inject environment variables (without logging key)
        AiInvestigationService aiService = new AiInvestigationService(new ObjectMapper());
        ReflectionTestUtils.setField(aiService, "apiKey", apiKey.trim());
        ReflectionTestUtils.setField(aiService, "apiUrl", "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent");

        // Prepare the 4 real exception scenarios from sample-data/
        List<ReconciliationItem> exceptionItems = new ArrayList<>();

        exceptionItems.add(ReconciliationItem.builder()
                .transactionId("TXN_1003")
                .orderId("ORD_5003")
                .settlementId("SETTL_9003")
                .paymentAmount(new BigDecimal("5000.00"))
                .settledAmount(new BigDecimal("4900.00"))
                .differenceAmount(new BigDecimal("100.00"))
                .currency("INR")
                .status(ReconciliationStatus.AMOUNT_MISMATCH)
                .paymentMethod("netbanking")
                .bankReference("UTRIB00001003")
                .discrepancyReason("Amount mismatch: Payment is 5000.00 INR, but settled amount is 4900.00 INR")
                .build());

        exceptionItems.add(ReconciliationItem.builder()
                .transactionId("TXN_1004")
                .orderId("ORD_5004")
                .paymentAmount(new BigDecimal("1200.00"))
                .differenceAmount(new BigDecimal("1200.00"))
                .currency("INR")
                .status(ReconciliationStatus.MISSING_SETTLEMENT)
                .paymentMethod("upi")
                .discrepancyReason("Payment was captured in gateway, but no matching settlement was found in bank records.")
                .build());

        exceptionItems.add(ReconciliationItem.builder()
                .transactionId("TXN_1005")
                .orderId("ORD_5005")
                .paymentAmount(new BigDecimal("850.00"))
                .currency("INR")
                .status(ReconciliationStatus.DUPLICATE_TRANSACTION)
                .paymentMethod("wallet")
                .discrepancyReason("Duplicate transaction detected in payment records (appears 2 times).")
                .build());

        exceptionItems.add(ReconciliationItem.builder()
                .transactionId("TXN_9999")
                .settlementId("SETTL_9005")
                .settledAmount(new BigDecimal("750.00"))
                .differenceAmount(new BigDecimal("750.00"))
                .currency("INR")
                .status(ReconciliationStatus.UNKNOWN_TRANSACTION)
                .bankReference("UTRIB00009999")
                .discrepancyReason("Bank settlement payout was received, but no corresponding transaction ID was found in gateway.")
                .build());

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id("batch_live_gemini_test")
                .batchName("Live-Gemini-Verification")
                .items(exceptionItems)
                .build();

        System.out.println("\n[INFO] Sending 4 reconciliation exceptions to Gemini 2.5 Flash API...");

        // Execute AI Investigation
        ReconciliationBatch investigatedBatch = aiService.investigateBatch(batch);

        assertNotNull(investigatedBatch);
        assertNotNull(investigatedBatch.getItems());
        assertEquals(4, investigatedBatch.getItems().size());

        // Check if real Gemini or fallback was invoked
        ReconciliationItem firstItem = investigatedBatch.getItems().get(0);
        assertNotNull(firstItem.getAiInvestigation(), "AI investigation should not be null");

        boolean usedFallback = firstItem.getAiInvestigation().getPossibleReason() != null
                && firstItem.getAiInvestigation().getPossibleReason().contains(FALLBACK_MARKER);

        if (usedFallback) {
            System.err.println("\n================================================================================");
            System.err.println("[FAIL] The result was generated by generateFallbackInvestigation()!");
            System.err.println("Check your GEMINI_API_KEY and network connection.");
            System.err.println("================================================================================\n");
            fail("Real Gemini API was not invoked; fallback was used instead.");
        }

        // Print verified output
        System.out.println("\n================================================================================");
        System.out.println(">>> [SUCCESS] REAL GEMINI 2.5 FLASH API CALL SUCCEEDED! <<<");
        System.out.println("Model: gemini-2.5-flash");
        System.out.println("Verification: Genuine generative AI analysis received (NOT fallback)");
        System.out.println("================================================================================\n");

        for (ReconciliationItem item : investigatedBatch.getItems()) {
            AiInvestigation ai = item.getAiInvestigation();
            assertNotNull(ai, "AiInvestigation must be populated for " + item.getTransactionId());

            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Transaction ID     : " + item.getTransactionId());
            System.out.println("Status             : " + item.getStatus());
            System.out.println("Confidence Score   : " + ai.getConfidence());
            System.out.println("Possible Reason    : " + ai.getPossibleReason());
            System.out.println("Recommended Action : " + ai.getRecommendedAction());
            System.out.println("Evidence Reference : " + ai.getEvidence());
            System.out.println("Investigated At    : " + ai.getInvestigatedAt());

            // Assert real analysis attributes
            assertNotNull(ai.getPossibleReason(), "Possible reason must be present");
            assertNotNull(ai.getRecommendedAction(), "Recommended action must be present");
            assertNotNull(ai.getEvidence(), "Evidence must be present");
            assertTrue(ai.getConfidence() > 0.0 && ai.getConfidence() <= 1.0, "Confidence must be between 0 and 1");
        }
        System.out.println("--------------------------------------------------------------------------------\n");
    }
}
