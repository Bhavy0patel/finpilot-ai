package com.finpilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the reconciliation result for an individual transaction or settlement record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationItem {

    /** Gateway Transaction ID */
    private String transactionId;

    /** Merchant Order ID (if available) */
    private String orderId;

    /** Bank Settlement ID (if available) */
    private String settlementId;

    /** Gross payment amount from gateway */
    private BigDecimal paymentAmount;

    /** Net settled amount from bank */
    private BigDecimal settledAmount;

    /** Difference: paymentAmount - settledAmount */
    private BigDecimal differenceAmount;

    /** Currency (e.g. INR) */
    private String currency;

    /** Reconciliation status detected */
    private ReconciliationStatus status;

    /** Human-readable explanation of why this status was assigned */
    private String discrepancyReason;

    /** Payment channel (UPI, Card, etc.) */
    private String paymentMethod;

    /** Bank UTR / Reference number */
    private String bankReference;

    /** Autonomous AI root-cause investigation (populated for exceptions) */
    private AiInvestigation aiInvestigation;
}
