package com.finpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finpilot.model.AiInvestigation;
import com.finpilot.model.ReconciliationBatch;
import com.finpilot.model.ReconciliationItem;
import com.finpilot.model.ReconciliationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Autonomous AI Finance Controller Investigation Service.
 * Uses Google Gemini (gemini-2.5-flash) to investigate reconciliation exceptions.
 */
@Service
public class AiInvestigationService {

    private static final Logger log = LoggerFactory.getLogger(AiInvestigationService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent}")
    private String apiUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiInvestigationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @jakarta.annotation.PostConstruct
    public void logAiStatusOnStartup() {
        String key = getEffectiveApiKey();
        if (key != null) {
            String masked = key.length() > 8 ? key.substring(0, 6) + "..." : "******";
            log.info(">>> [FINPILOT AI READY] GEMINI LIVE API IS ACTIVE! (Detected Key: {}) <<<", masked);
        } else {
            log.warn(">>> [FINPILOT AI READY] DEMO / FALLBACK MODE ACTIVE (GEMINI_API_KEY not found) <<<");
        }
    }

    /**
     * Resolves the API key from Spring property, Java System property, OS env variable, or .env file.
     * Safely trims and strips any surrounding quotes.
     */
    private String getEffectiveApiKey() {
        String key = null;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            key = apiKey.trim();
        } else if (System.getProperty("gemini.api.key") != null && !System.getProperty("gemini.api.key").trim().isEmpty()) {
            key = System.getProperty("gemini.api.key").trim();
        } else if (System.getProperty("GEMINI_API_KEY") != null && !System.getProperty("GEMINI_API_KEY").trim().isEmpty()) {
            key = System.getProperty("GEMINI_API_KEY").trim();
        } else if (System.getenv("GEMINI_API_KEY") != null && !System.getenv("GEMINI_API_KEY").trim().isEmpty()) {
            key = System.getenv("GEMINI_API_KEY").trim();
        } else if (System.getenv("gemini_api_key") != null && !System.getenv("gemini_api_key").trim().isEmpty()) {
            key = System.getenv("gemini_api_key").trim();
        } else {
            // Check for local .env file in backend/ or root/
            key = readKeyFromDotEnvFile();
        }

        if (key != null) {
            key = key.replaceAll("^[\"']+|[\"']+$", "").trim();
            if (!key.isEmpty()) {
                return key;
            }
        }
        return null;
    }

    private String readKeyFromDotEnvFile() {
        java.nio.file.Path[] paths = new java.nio.file.Path[]{
                java.nio.file.Paths.get(".env"),
                java.nio.file.Paths.get("../.env")
        };
        for (java.nio.file.Path p : paths) {
            if (java.nio.file.Files.exists(p)) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(p);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("GEMINI_API_KEY=")) {
                            return line.substring("GEMINI_API_KEY=".length()).trim();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Investigates all exceptions in a ReconciliationBatch using Gemini 2.5 Flash.
     */
    public ReconciliationBatch investigateBatch(ReconciliationBatch batch) {
        if (batch == null || batch.getItems() == null) {
            return batch;
        }

        // 1. Filter only exceptions (skip MATCHED records)
        List<ReconciliationItem> exceptions = batch.getItems().stream()
                .filter(item -> item.getStatus() != ReconciliationStatus.MATCHED)
                .collect(Collectors.toList());

        if (exceptions.isEmpty()) {
            return batch;
        }

        String effectiveKey = getEffectiveApiKey();
        boolean isKeyPresent = (effectiveKey != null && !effectiveKey.isEmpty());

        log.info("[AI DEBUG] API key present: {}", isKeyPresent);

        // 2. If API Key is not configured, apply deterministic domain-grounded fallback
        if (!isKeyPresent) {
            log.warn("[AI DEBUG] Falling back because: GEMINI_API_KEY environment variable is not configured or not accessible to running Java process");
            for (ReconciliationItem item : exceptions) {
                item.setAiInvestigation(generateFallbackInvestigation(item));
            }
            return batch;
        }

        // 3. Call Gemini API
        log.info("[AI DEBUG] Calling Gemini API for {} exceptions...", exceptions.size());
        try {
            Map<String, AiInvestigation> investigations = callGeminiForExceptions(exceptions, effectiveKey);
            
            if (investigations.isEmpty()) {
                log.warn("[AI DEBUG] Falling back because: Gemini API returned 200 OK but response contained 0 parsed exception items");
                for (ReconciliationItem item : exceptions) {
                    item.setAiInvestigation(generateFallbackInvestigation(item));
                }
            } else {
                log.info("[AI DEBUG] Gemini response parsed successfully: {} items mapped to GEMINI", investigations.size());
                for (ReconciliationItem item : exceptions) {
                    AiInvestigation aiResult = investigations.get(item.getTransactionId());
                    if (aiResult != null) {
                        aiResult.setSource("GEMINI");
                        item.setAiInvestigation(aiResult);
                    } else {
                        log.warn("[AI DEBUG] Transaction {} missing in Gemini response array, applying fallback for this item", item.getTransactionId());
                        item.setAiInvestigation(generateFallbackInvestigation(item));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[AI DEBUG] Falling back because: Gemini API request failed ({})", e.getMessage());
            for (ReconciliationItem item : exceptions) {
                item.setAiInvestigation(generateFallbackInvestigation(item));
            }
        }

        return batch;
    }

    /**
     * Builds structured prompt, executes POST to Gemini with up to 3 automatic retries for transient spikes.
     */
    private Map<String, AiInvestigation> callGeminiForExceptions(List<ReconciliationItem> exceptions, String effectiveKey) throws Exception {
        String prompt = buildPrompt(exceptions);

        // Build Gemini request payload with structured JSON mode
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode contentsArray = rootNode.putArray("contents");
        ObjectNode contentObject = contentsArray.addObject();
        ArrayNode partsArray = contentObject.putArray("parts");
        partsArray.addObject().put("text", prompt);

        ObjectNode genConfig = rootNode.putObject("generationConfig");
        genConfig.put("responseMimeType", "application/json");

        String requestJson = objectMapper.writeValueAsString(rootNode);
        String targetUrl = apiUrl + "?key=" + effectiveKey;

        int maxAttempts = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri(java.net.URI.create(targetUrl))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestJson)
                        .retrieve()
                        .body(String.class);

                log.info("[AI DEBUG] Gemini HTTP success (Received {} bytes)", responseBody != null ? responseBody.length() : 0);
                return parseGeminiResponse(responseBody);
            } catch (org.springframework.web.client.RestClientResponseException ex) {
                lastException = ex;
                int statusCode = ex.getStatusCode().value();
                log.error("[AI DEBUG] Gemini HTTP error: Status {} - {}", statusCode, ex.getResponseBodyAsString());

                // Fast fallback: Do not retry permanent client/model configuration 4xx errors (except 429 Too Many Requests)
                if (statusCode >= 400 && statusCode < 500 && statusCode != 429) {
                    log.warn("[AI DEBUG] Permanent HTTP {} client/configuration error encountered. Skipping retries and falling back immediately.", statusCode);
                    break;
                }

                if (attempt < maxAttempts) {
                    log.warn("Retrying Gemini API call in 2 seconds (attempt {}/{})...", attempt, maxAttempts);
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                lastException = e;
                log.error("[AI DEBUG] Gemini connection error: {}", e.getMessage());
                if (attempt < maxAttempts) {
                    log.warn("Retrying Gemini API call in 2 seconds (attempt {}/{})...", attempt, maxAttempts);
                    Thread.sleep(2000);
                }
            }
        }

        throw (lastException != null) ? lastException : new RuntimeException("Failed to call Gemini after " + maxAttempts + " attempts");
    }

    private String buildPrompt(List<ReconciliationItem> exceptions) throws Exception {
        String itemsJson = objectMapper.writeValueAsString(exceptions);

        return """
                You are FinPilot AI, an autonomous financial controller specializing in payment reconciliation and dispute resolution.
                
                Analyze the following reconciliation exceptions between payment gateway transactions and bank settlement records.
                
                CRITICAL INSTRUCTIONS:
                1. Ground your analysis strictly in the provided data (amounts, IDs, difference, status).
                2. Do NOT invent external transactions or assumptions not supported by the numbers.
                3. For AMOUNT_MISMATCH: compute the exact percentage difference, check if it matches typical gateway MDR fees (1-3%) or fixed processing surcharges.
                4. For MISSING_SETTLEMENT: check pending settlement cycles (T+1/T+2 banking delays) vs gateway capture state.
                5. For DUPLICATE_TRANSACTION: check whether customer was double charged or if duplicate webhook triggers occurred.
                6. For UNKNOWN_TRANSACTION: identify unlinked bank payouts requiring UTR ledger matching.
                
                INPUT DATA:
                """ + itemsJson + """
                
                You MUST return a valid JSON array of objects with the exact following schema:
                [
                  {
                    "transactionId": "string",
                    "possibleReason": "string (clear root cause explanation)",
                    "confidence": 0.95 (float between 0.0 and 1.0),
                    "recommendedAction": "string (exact corrective action for finance team)",
                    "evidence": "string (explicit reference to amounts, IDs, and mathematical facts)"
                  }
                ]
                """;
    }

    private Map<String, AiInvestigation> parseGeminiResponse(String responseBody) {
        Map<String, AiInvestigation> results = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    for (JsonNode part : parts) {
                        String jsonText = part.path("text").asText("");
                        if (jsonText.isEmpty()) continue;

                        jsonText = jsonText.trim();
                        if (jsonText.startsWith("```json")) {
                            jsonText = jsonText.substring(7);
                        } else if (jsonText.startsWith("```")) {
                            jsonText = jsonText.substring(3);
                        }
                        if (jsonText.endsWith("```")) {
                            jsonText = jsonText.substring(0, jsonText.length() - 3);
                        }
                        jsonText = jsonText.trim();

                        try {
                            JsonNode arrayNode = objectMapper.readTree(jsonText);
                            if (arrayNode.isArray() && !arrayNode.isEmpty()) {
                                for (JsonNode itemNode : arrayNode) {
                                    String txnId = itemNode.path("transactionId").asText();
                                    if (!txnId.isEmpty()) {
                                        AiInvestigation investigation = AiInvestigation.builder()
                                                .possibleReason(itemNode.path("possibleReason").asText())
                                                .confidence(itemNode.path("confidence").asDouble(0.95))
                                                .recommendedAction(itemNode.path("recommendedAction").asText())
                                                .evidence(itemNode.path("evidence").asText())
                                                .source("GEMINI")
                                                .investigatedAt(LocalDateTime.now())
                                                .build();
                                        results.put(txnId, investigation);
                                    }
                                }
                                if (!results.isEmpty()) {
                                    break;
                                }
                            }
                        } catch (Exception parseEx) {
                            log.debug("Part not direct JSON array, checking next part...");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[AI DEBUG] Error parsing Gemini response JSON: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Rule-grounded deterministic fallback when Gemini API key is not configured or network fails.
     */
    public AiInvestigation generateFallbackInvestigation(ReconciliationItem item) {
        String reason;
        String action;
        String evidence;
        double confidence = 0.92;

        BigDecimal payAmt = item.getPaymentAmount() != null ? item.getPaymentAmount() : BigDecimal.ZERO;
        BigDecimal settAmt = item.getSettledAmount() != null ? item.getSettledAmount() : BigDecimal.ZERO;
        BigDecimal diff = item.getDifferenceAmount() != null ? item.getDifferenceAmount().abs() : BigDecimal.ZERO;

        switch (item.getStatus()) {
            case AMOUNT_MISMATCH -> {
                reason = "Potential payment gateway MDR fee or processing charge deduction of " + diff + " " + item.getCurrency() + ".";
                action = "Post " + diff + " " + item.getCurrency() + " to Payment Gateway Processing Fee Expense ledger and reconcile order.";
                evidence = "Payment amount is " + payAmt + " " + item.getCurrency() + " while settled amount is " + settAmt + " " + item.getCurrency() + " (Diff: " + diff + ").";
                confidence = 0.94;
            }
            case MISSING_SETTLEMENT -> {
                reason = "Transaction captured on gateway but pending bank settlement cycle (T+1/T+2 days) or payout hold.";
                action = "Verify settlement batch status on Razorpay dashboard; if older than 48 hours, file bank inquiry.";
                evidence = "Order " + item.getOrderId() + " has captured payment of " + payAmt + " " + item.getCurrency() + " with no corresponding credit in bank settlement statement.";
                confidence = 0.90;
            }
            case DUPLICATE_TRANSACTION -> {
                reason = "Duplicate payment capture records detected for identical Transaction ID " + item.getTransactionId() + ". Likely customer double-click or webhook retry.";
                action = "Audit gateway logs for double charge. If customer was charged twice, initiate an immediate refund for the secondary charge.";
                evidence = "Duplicate row with Transaction ID " + item.getTransactionId() + " charging " + payAmt + " " + item.getCurrency() + ".";
                confidence = 0.96;
            }
            case UNKNOWN_TRANSACTION -> {
                reason = "Bank payout received without an originating gateway transaction record. Potential direct NEFT/RTGS credit or untracked refund reversal.";
                action = "Review bank statement reference (" + (item.getBankReference() != null ? item.getBankReference() : "N/A") + ") with accounts receivable to map to correct merchant invoice.";
                evidence = "Settlement ID " + item.getSettlementId() + " credited " + settAmt + " " + item.getCurrency() + " with no associated payment in gateway records.";
                confidence = 0.88;
            }
            default -> {
                reason = "Standard transaction record.";
                action = "No action needed.";
                evidence = "Record matches expected criteria.";
                confidence = 1.0;
            }
        }

        return AiInvestigation.builder()
                .possibleReason(reason)
                .confidence(confidence)
                .recommendedAction(action)
                .evidence(evidence)
                .source("FALLBACK")
                .investigatedAt(LocalDateTime.now())
                .build();
    }
}
