package com.finpilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a single payment gateway transaction record (from payments_sample.csv).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    /** Unique transaction identifier from Razorpay (e.g. TXN_1001) */
    private String transactionId;

    /** Merchant order identifier (e.g. ORD_5001) */
    private String orderId;

    /** Gross payment amount charged to customer (e.g. 1500.00) */
    private BigDecimal amount;

    /** Currency code (e.g. INR) */
    private String currency;

    /** Payment status (e.g. captured, refunded, failed) */
    private String status;

    /** Payment method used (e.g. upi, card, netbanking, wallet) */
    private String paymentMethod;

    /** Timestamp when payment was created */
    private String createdAt;
}
