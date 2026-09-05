package com.finpilot.service;

import com.finpilot.model.PaymentTransaction;
import com.finpilot.model.ReconciliationBatch;
import com.finpilot.model.ReconciliationStatus;
import com.finpilot.model.SettlementRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationEngineTest {

    private CsvParserService parserService;
    private ReconciliationEngine engine;

    @BeforeEach
    void setUp() {
        parserService = new CsvParserService();
        engine = new ReconciliationEngine();
    }

    @Test
    @DisplayName("Should accurately reconcile sample datasets and detect all 5 statuses")
    void testReconcileSampleData() {
        String paymentsCsv = """
                transaction_id,order_id,amount,currency,status,payment_method,created_at
                TXN_1001,ORD_5001,1500.00,INR,captured,upi,2026-08-20T10:15:30Z
                TXN_1002,ORD_5002,2499.00,INR,captured,card,2026-08-20T11:00:15Z
                TXN_1003,ORD_5003,5000.00,INR,captured,netbanking,2026-08-20T11:45:00Z
                TXN_1004,ORD_5004,1200.00,INR,captured,upi,2026-08-20T12:30:10Z
                TXN_1005,ORD_5005,850.00,INR,captured,wallet,2026-08-20T13:10:00Z
                TXN_1005,ORD_5005,850.00,INR,captured,wallet,2026-08-20T13:10:05Z
                TXN_1006,ORD_5006,3100.00,INR,captured,upi,2026-08-20T14:00:00Z
                """;

        String settlementsCsv = """
                settlement_id,transaction_id,settled_amount,currency,settlement_status,settlement_date,bank_reference
                SETTL_9001,TXN_1001,1500.00,INR,settled,2026-08-21T04:00:00Z,UTRIB00001001
                SETTL_9002,TXN_1002,2499.00,INR,settled,2026-08-21T04:00:00Z,UTRIB00001002
                SETTL_9003,TXN_1003,4900.00,INR,settled,2026-08-21T04:00:00Z,UTRIB00001003
                SETTL_9004,TXN_1005,850.00,INR,settled,2026-08-21T04:00:00Z,UTRIB00001005
                SETTL_9005,TXN_9999,750.00,INR,settled,2026-08-21T04:00:00Z,UTRIB00009999
                SETTL_9006,TXN_1006,3100.00,INR,settled,2026-08-21T04:00:00Z,UTRIB00001006
                """;

        List<PaymentTransaction> payments = parserService.parsePayments(
                new ByteArrayInputStream(paymentsCsv.getBytes(StandardCharsets.UTF_8))
        );
        List<SettlementRecord> settlements = parserService.parseSettlements(
                new ByteArrayInputStream(settlementsCsv.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(7, payments.size());
        assertEquals(6, settlements.size());

        ReconciliationBatch batch = engine.reconcile("Test-Batch-001", payments, settlements);

        assertNotNull(batch);
        assertEquals(3, batch.getMatchedCount(), "Should have 3 MATCHED (TXN_1001, TXN_1002, TXN_1006)");
        assertEquals(1, batch.getAmountMismatchCount(), "Should have 1 AMOUNT_MISMATCH (TXN_1003)");
        assertEquals(1, batch.getMissingSettlementCount(), "Should have 1 MISSING_SETTLEMENT (TXN_1004)");
        assertEquals(2, batch.getDuplicateCount(), "Should have 2 DUPLICATE_TRANSACTION instances (TXN_1005 x 2)");
        assertEquals(1, batch.getUnknownCount(), "Should have 1 UNKNOWN_TRANSACTION (TXN_9999)");

        // Verify total discrepancy value (100.00 mismatch + 1200.00 missing + 750.00 unknown = 2050.00)
        assertEquals(new BigDecimal("2050.00"), batch.getTotalDiscrepancyValue());
    }
}
