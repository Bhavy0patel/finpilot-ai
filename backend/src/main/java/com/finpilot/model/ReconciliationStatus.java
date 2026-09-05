package com.finpilot.model;

/**
 * Represents the 5 possible reconciliation statuses for transactions.
 */
public enum ReconciliationStatus {
    
    /** Exact match on Transaction ID and Amount between payment and settlement. */
    MATCHED,

    /** Transaction IDs match, but payment amount does not equal settled amount. */
    AMOUNT_MISMATCH,

    /** Payment exists in gateway records, but missing in bank settlements. */
    MISSING_SETTLEMENT,

    /** Same Transaction ID appears multiple times in payment records. */
    DUPLICATE_TRANSACTION,

    /** Bank settlement received for a Transaction ID not found in payment gateway. */
    UNKNOWN_TRANSACTION
}
