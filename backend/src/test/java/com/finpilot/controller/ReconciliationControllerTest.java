package com.finpilot.controller;

import com.finpilot.model.AiInvestigation;
import com.finpilot.model.PaymentTransaction;
import com.finpilot.model.ReconciliationBatch;
import com.finpilot.model.ReconciliationItem;
import com.finpilot.model.ReconciliationStatus;
import com.finpilot.model.SettlementRecord;
import com.finpilot.repository.ReconciliationBatchRepository;
import com.finpilot.service.AiInvestigationService;
import com.finpilot.service.CsvParserService;
import com.finpilot.service.ReconciliationEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReconciliationController.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CsvParserService csvParserService;

    @MockBean
    private ReconciliationEngine reconciliationEngine;

    @MockBean
    private ReconciliationBatchRepository batchRepository;

    @MockBean
    private AiInvestigationService aiInvestigationService;

    @Test
    @DisplayName("POST /reconcile/upload should successfully reconcile and return 201 Created")
    void testUploadAndReconcile() throws Exception {
        MockMultipartFile paymentsFile = new MockMultipartFile(
                "paymentsFile", "payments.csv", "text/csv", "transaction_id,amount\nTXN_1001,1500.00".getBytes());
        MockMultipartFile settlementsFile = new MockMultipartFile(
                "settlementsFile", "settlements.csv", "text/csv", "settlement_id,transaction_id,settled_amount\nSETTL_9001,TXN_1001,1500.00".getBytes());

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id("batch_123")
                .batchName("August-Run")
                .processedAt(LocalDateTime.now())
                .totalPayments(1)
                .totalSettlements(1)
                .matchedCount(1)
                .items(List.of(
                        ReconciliationItem.builder()
                                .transactionId("TXN_1001")
                                .status(ReconciliationStatus.MATCHED)
                                .paymentAmount(new BigDecimal("1500.00"))
                                .settledAmount(new BigDecimal("1500.00"))
                                .build()
                ))
                .build();

        Mockito.when(csvParserService.parsePayments(any())).thenReturn(List.of(new PaymentTransaction()));
        Mockito.when(csvParserService.parseSettlements(any())).thenReturn(List.of(new SettlementRecord()));
        Mockito.when(reconciliationEngine.reconcile(eq("August-Run"), any(), any())).thenReturn(batch);
        Mockito.when(batchRepository.save(any(ReconciliationBatch.class))).thenReturn(batch);

        mockMvc.perform(multipart("/reconcile/upload")
                        .file(paymentsFile)
                        .file(settlementsFile)
                        .param("batchName", "August-Run"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("batch_123"))
                .andExpect(jsonPath("$.batchName").value("August-Run"))
                .andExpect(jsonPath("$.matchedCount").value(1));
    }

    @Test
    @DisplayName("GET /reconcile/batches should return batch summaries")
    void testGetAllBatches() throws Exception {
        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id("batch_123")
                .batchName("August-Run")
                .processedAt(LocalDateTime.now())
                .totalPayments(5)
                .matchedCount(3)
                .amountMismatchCount(1)
                .missingSettlementCount(1)
                .build();

        Mockito.when(batchRepository.findAllByOrderByProcessedAtDesc()).thenReturn(List.of(batch));

        mockMvc.perform(get("/reconcile/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("batch_123"))
                .andExpect(jsonPath("$[0].totalPayments").value(5))
                .andExpect(jsonPath("$[0].matchedCount").value(3));
    }

    @Test
    @DisplayName("GET /reconcile/batches/{batchId}/exceptions should return only non-matched records")
    void testGetExceptions() throws Exception {
        ReconciliationItem matchedItem = ReconciliationItem.builder()
                .transactionId("TXN_1001")
                .status(ReconciliationStatus.MATCHED)
                .build();
        ReconciliationItem mismatchItem = ReconciliationItem.builder()
                .transactionId("TXN_1003")
                .status(ReconciliationStatus.AMOUNT_MISMATCH)
                .build();

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id("batch_123")
                .items(List.of(matchedItem, mismatchItem))
                .build();

        Mockito.when(batchRepository.findById("batch_123")).thenReturn(Optional.of(batch));

        mockMvc.perform(get("/reconcile/batches/batch_123/exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("TXN_1003"))
                .andExpect(jsonPath("$[0].status").value("AMOUNT_MISMATCH"));
    }

    @Test
    @DisplayName("POST /reconcile/batches/{batchId}/investigate should invoke AI and return enriched batch")
    void testInvestigateBatch() throws Exception {
        AiInvestigation aiInvestigation = AiInvestigation.builder()
                .possibleReason("Gateway MDR deduction")
                .confidence(0.95)
                .recommendedAction("Post fee to ledger")
                .evidence("100.00 difference")
                .build();

        ReconciliationItem exceptionItem = ReconciliationItem.builder()
                .transactionId("TXN_1003")
                .status(ReconciliationStatus.AMOUNT_MISMATCH)
                .aiInvestigation(aiInvestigation)
                .build();

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id("batch_123")
                .items(List.of(exceptionItem))
                .build();

        Mockito.when(batchRepository.findById("batch_123")).thenReturn(Optional.of(batch));
        Mockito.when(aiInvestigationService.investigateBatch(any())).thenReturn(batch);
        Mockito.when(batchRepository.save(any(ReconciliationBatch.class))).thenReturn(batch);

        mockMvc.perform(post("/reconcile/batches/batch_123/investigate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("batch_123"))
                .andExpect(jsonPath("$.items[0].aiInvestigation.possibleReason").value("Gateway MDR deduction"))
                .andExpect(jsonPath("$.items[0].aiInvestigation.confidence").value(0.95));
    }
}
