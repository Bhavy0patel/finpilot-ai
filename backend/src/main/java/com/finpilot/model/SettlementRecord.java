package com.finpilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a single bank settlement payout record (from settlements_sample.csv).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecord {

    /** Unique settlement identifier from bank (e.g. SETTL_9001) */
    private String settlementId;

    /** Associated transaction ID (e.g. TXN_1001) */
    private String transactionId;

    /** Net amount deposited to merchant bank account (e.g. 1500.00) */
    private BigDecimal settledAmount;

    /** Currency code (e.g. INR) */
    private String currency;

    /** Settlement status (e.g. settled, pending) */
    private String settlementStatus;

    /** Timestamp when settlement was credited */
    private String settlementDate;

    /** Bank UTR / reference number (e.g. UTRIB00001001) */
    private String bankReference;
}
