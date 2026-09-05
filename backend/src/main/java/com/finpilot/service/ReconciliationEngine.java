package com.finpilot.service;

import com.finpilot.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core Deterministic Reconciliation Engine.
 * 
 * Reconciles payment gateway transactions against bank settlement records
 * using exact Java mathematical and logic rules.
 */
@Service
public class ReconciliationEngine {

    /**
     * Reconciles payments and settlements, categorizing each into one of the 5 statuses:
     *  - MATCHED
     *  - AMOUNT_MISMATCH
     *  - MISSING_SETTLEMENT
     *  - DUPLICATE_TRANSACTION
     *  - UNKNOWN_TRANSACTION
     */
    public ReconciliationBatch reconcile(String batchName, List<PaymentTransaction> payments, List<SettlementRecord> settlements) {
        ReconciliationBatch batch = ReconciliationBatch.builder()
                .batchName(batchName != null ? batchName : "Batch-" + System.currentTimeMillis())
                .processedAt(LocalDateTime.now())
                .totalPayments(payments.size())
                .totalSettlements(settlements.size())
                .items(new ArrayList<>())
                .build();

        // 1. Group payments by transactionId (preserves insertion order)
        Map<String, List<PaymentTransaction>> paymentsByTxn = new LinkedHashMap<>();
        BigDecimal totalPaymentVal = BigDecimal.ZERO;
        for (PaymentTransaction p : payments) {
            paymentsByTxn.computeIfAbsent(p.getTransactionId(), k -> new ArrayList<>()).add(p);
            if (p.getAmount() != null) {
                totalPaymentVal = totalPaymentVal.add(p.getAmount());
            }
        }
        batch.setTotalPaymentValue(totalPaymentVal);

        // 2. Group settlements by transactionId
        Map<String, List<SettlementRecord>> settlementsByTxn = new LinkedHashMap<>();
        BigDecimal totalSettledVal = BigDecimal.ZERO;
        for (SettlementRecord s : settlements) {
            settlementsByTxn.computeIfAbsent(s.getTransactionId(), k -> new ArrayList<>()).add(s);
            if (s.getSettledAmount() != null) {
                totalSettledVal = totalSettledVal.add(s.getSettledAmount());
            }
        }
        batch.setTotalSettledValue(totalSettledVal);

        Set<String> processedSettlementIds = new HashSet<>();
        BigDecimal totalDiscrepancyVal = BigDecimal.ZERO;

        int matchedCount = 0;
        int mismatchCount = 0;
        int missingCount = 0;
        int duplicateCount = 0;
        int unknownCount = 0;

        // 3. Process each Payment Transaction group
        for (Map.Entry<String, List<PaymentTransaction>> entry : paymentsByTxn.entrySet()) {
            String txnId = entry.getKey();
            List<PaymentTransaction> paymentList = entry.getValue();

            // Check: DUPLICATE_TRANSACTION
            if (paymentList.size() > 1) {
                for (PaymentTransaction dupPayment : paymentList) {
                    ReconciliationItem item = ReconciliationItem.builder()
                            .transactionId(dupPayment.getTransactionId())
                            .orderId(dupPayment.getOrderId())
                            .paymentAmount(dupPayment.getAmount())
                            .currency(dupPayment.getCurrency())
                            .paymentMethod(dupPayment.getPaymentMethod())
                            .status(ReconciliationStatus.DUPLICATE_TRANSACTION)
                            .discrepancyReason("Duplicate transaction detected in payment records (Transaction ID appears " + paymentList.size() + " times).")
                            .build();

                    // If a settlement exists, attach it for context
                    List<SettlementRecord> sList = settlementsByTxn.get(txnId);
                    if (sList != null && !sList.isEmpty()) {
                        SettlementRecord s = sList.get(0);
                        item.setSettlementId(s.getSettlementId());
                        item.setSettledAmount(s.getSettledAmount());
                        item.setBankReference(s.getBankReference());
                        processedSettlementIds.add(s.getSettlementId());
                    }

                    batch.getItems().add(item);
                    duplicateCount++;
                }
                continue;
            }

            // Single Payment Transaction
            PaymentTransaction payment = paymentList.get(0);
            List<SettlementRecord> matchedSettlements = settlementsByTxn.get(txnId);

            // Check: MISSING_SETTLEMENT
            if (matchedSettlements == null || matchedSettlements.isEmpty()) {
                ReconciliationItem item = ReconciliationItem.builder()
                        .transactionId(payment.getTransactionId())
                        .orderId(payment.getOrderId())
                        .paymentAmount(payment.getAmount())
                        .differenceAmount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .paymentMethod(payment.getPaymentMethod())
                        .status(ReconciliationStatus.MISSING_SETTLEMENT)
                        .discrepancyReason("Payment was captured in gateway, but no matching settlement was found in bank records.")
                        .build();

                batch.getItems().add(item);
                missingCount++;
                if (payment.getAmount() != null) {
                    totalDiscrepancyVal = totalDiscrepancyVal.add(payment.getAmount());
                }
            } else {
                // Settlement exists -> Check MATCHED vs AMOUNT_MISMATCH
                SettlementRecord settlement = matchedSettlements.get(0);
                processedSettlementIds.add(settlement.getSettlementId());

                BigDecimal payAmt = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
                BigDecimal settAmt = settlement.getSettledAmount() != null ? settlement.getSettledAmount() : BigDecimal.ZERO;
                BigDecimal diff = payAmt.subtract(settAmt);

                ReconciliationItem item = ReconciliationItem.builder()
                        .transactionId(payment.getTransactionId())
                        .orderId(payment.getOrderId())
                        .settlementId(settlement.getSettlementId())
                        .paymentAmount(payAmt)
                        .settledAmount(settAmt)
                        .differenceAmount(diff)
                        .currency(payment.getCurrency())
                        .paymentMethod(payment.getPaymentMethod())
                        .bankReference(settlement.getBankReference())
                        .build();

                if (diff.compareTo(BigDecimal.ZERO) == 0) {
                    item.setStatus(ReconciliationStatus.MATCHED);
                    item.setDiscrepancyReason("Exact match between payment amount and settled amount.");
                    matchedCount++;
                } else {
                    item.setStatus(ReconciliationStatus.AMOUNT_MISMATCH);
                    item.setDiscrepancyReason("Amount mismatch: Payment is " + payAmt + " " + item.getCurrency() + 
                            ", but settled amount is " + settAmt + " " + item.getCurrency() + 
                            " (Difference: " + diff.abs() + " " + item.getCurrency() + ").");
                    mismatchCount++;
                    totalDiscrepancyVal = totalDiscrepancyVal.add(diff.abs());
                }

                batch.getItems().add(item);
            }
        }

        // 4. Check for UNKNOWN_TRANSACTION (Settlements with no matching payment)
        for (SettlementRecord settlement : settlements) {
            if (!processedSettlementIds.contains(settlement.getSettlementId())) {
                BigDecimal settAmt = settlement.getSettledAmount() != null ? settlement.getSettledAmount() : BigDecimal.ZERO;

                ReconciliationItem item = ReconciliationItem.builder()
                        .transactionId(settlement.getTransactionId())
                        .settlementId(settlement.getSettlementId())
                        .settledAmount(settAmt)
                        .differenceAmount(settAmt)
                        .currency(settlement.getCurrency())
                        .bankReference(settlement.getBankReference())
                        .status(ReconciliationStatus.UNKNOWN_TRANSACTION)
                        .discrepancyReason("Bank settlement payout was received, but no corresponding transaction ID was found in payment gateway records.")
                        .build();

                batch.getItems().add(item);
                unknownCount++;
                totalDiscrepancyVal = totalDiscrepancyVal.add(settAmt);
            }
        }

        // 5. Populate Batch Summary Statistics
        batch.setMatchedCount(matchedCount);
        batch.setAmountMismatchCount(mismatchCount);
        batch.setMissingSettlementCount(missingCount);
        batch.setDuplicateCount(duplicateCount);
        batch.setUnknownCount(unknownCount);
        batch.setTotalDiscrepancyValue(totalDiscrepancyVal);

        return batch;
    }
}
