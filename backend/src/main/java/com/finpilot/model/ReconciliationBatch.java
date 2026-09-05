package com.finpilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Document representing a complete reconciliation execution batch.
 * Stores summary metrics alongside all reconciled line items for auditing and reporting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reconciliation_batches")
public class ReconciliationBatch {

    /** Unique MongoDB document ID */
    @Id
    private String id;

    /** Name / label for this batch run */
    private String batchName;

    /** Timestamp when reconciliation was executed */
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

    /** Total count of payment records processed */
    private int totalPayments;

    /** Total count of settlement records processed */
    private int totalSettlements;

    /** Count of perfectly matched records */
    private int matchedCount;

    /** Count of amount mismatch anomalies */
    private int amountMismatchCount;

    /** Count of missing settlement anomalies */
    private int missingSettlementCount;

    /** Count of duplicate transaction anomalies */
    private int duplicateCount;

    /** Count of unknown settlement records */
    private int unknownCount;

    /** Sum of all gross payments processed */
    @Builder.Default
    private BigDecimal totalPaymentValue = BigDecimal.ZERO;

    /** Sum of all bank settlements processed */
    @Builder.Default
    private BigDecimal totalSettledValue = BigDecimal.ZERO;

    /** Sum of all unresolved discrepancy amounts */
    @Builder.Default
    private BigDecimal totalDiscrepancyValue = BigDecimal.ZERO;

    /** Detailed list of reconciled items (both matched and exceptions) */
    @Builder.Default
    private List<ReconciliationItem> items = new ArrayList<>();
}
