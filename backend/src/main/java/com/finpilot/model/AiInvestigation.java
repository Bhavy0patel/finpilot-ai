package com.finpilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Embedded model representing autonomous AI root-cause analysis for an exception item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInvestigation {

    /** Identified root cause (e.g. MDR fee deduction, timing cut-off, duplicate checkout) */
    private String possibleReason;

    /** Model confidence score between 0.0 and 1.0 */
    private Double confidence;

    /** Prescriptive financial controller action to resolve discrepancy */
    private String recommendedAction;

    /** Evidence strictly referenced from transaction data (amounts, IDs, difference) */
    private String evidence;

    /** Source of investigation: "GEMINI" for live API, or "FALLBACK" for rule-based demo mode */
    private String source;

    /** Timestamp when AI analysis was completed */
    @Builder.Default
    private LocalDateTime investigatedAt = LocalDateTime.now();
}
