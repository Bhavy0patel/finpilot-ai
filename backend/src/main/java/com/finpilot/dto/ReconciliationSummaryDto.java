package com.finpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a lightweight summary of a reconciliation batch.
 * Used for listing historical batches without loading every individual line item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationSummaryDto {

    private String id;
    private String batchName;
    private LocalDateTime processedAt;
    private int totalPayments;
    private int totalSettlements;
    private int matchedCount;
    private int amountMismatchCount;
    private int missingSettlementCount;
    private int duplicateCount;
    private int unknownCount;
    private BigDecimal totalPaymentValue;
    private BigDecimal totalSettledValue;
    private BigDecimal totalDiscrepancyValue;
}
