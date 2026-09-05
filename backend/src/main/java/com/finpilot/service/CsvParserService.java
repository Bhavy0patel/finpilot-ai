package com.finpilot.service;

import com.finpilot.model.PaymentTransaction;
import com.finpilot.model.SettlementRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for parsing and validating Payment and Settlement CSV files.
 */
@Service
public class CsvParserService {

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

    /**
     * Parses a Payments CSV file into a list of PaymentTransaction objects.
     * Expected CSV headers: transaction_id, order_id, amount, currency, status, payment_method, created_at
     */
    public List<PaymentTransaction> parsePayments(InputStream inputStream) {
        List<PaymentTransaction> transactions = new ArrayList<>();
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSV_FORMAT)) {

            for (CSVRecord record : csvParser) {
                String txnId = getCleanValue(record, "transaction_id");
                if (txnId == null || txnId.isEmpty()) {
                    continue; // skip blank line
                }

                PaymentTransaction transaction = PaymentTransaction.builder()
                        .transactionId(txnId)
                        .orderId(getCleanValue(record, "order_id"))
                        .amount(parseBigDecimal(getCleanValue(record, "amount")))
                        .currency(getCleanValue(record, "currency", "INR"))
                        .status(getCleanValue(record, "status"))
                        .paymentMethod(getCleanValue(record, "payment_method"))
                        .createdAt(getCleanValue(record, "created_at"))
                        .build();

                transactions.add(transaction);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Payments CSV: " + e.getMessage(), e);
        }
        return transactions;
    }

    /**
     * Parses a Settlements CSV file into a list of SettlementRecord objects.
     * Expected CSV headers: settlement_id, transaction_id, settled_amount, currency, settlement_status, settlement_date, bank_reference
     */
    public List<SettlementRecord> parseSettlements(InputStream inputStream) {
        List<SettlementRecord> settlements = new ArrayList<>();
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSV_FORMAT)) {

            for (CSVRecord record : csvParser) {
                String settlementId = getCleanValue(record, "settlement_id");
                String txnId = getCleanValue(record, "transaction_id");

                if ((settlementId == null || settlementId.isEmpty()) && (txnId == null || txnId.isEmpty())) {
                    continue; // skip blank line
                }

                SettlementRecord settlement = SettlementRecord.builder()
                        .settlementId(settlementId)
                        .transactionId(txnId)
                        .settledAmount(parseBigDecimal(getCleanValue(record, "settled_amount")))
                        .currency(getCleanValue(record, "currency", "INR"))
                        .settlementStatus(getCleanValue(record, "settlement_status"))
                        .settlementDate(getCleanValue(record, "settlement_date"))
                        .bankReference(getCleanValue(record, "bank_reference"))
                        .build();

                settlements.add(settlement);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Settlements CSV: " + e.getMessage(), e);
        }
        return settlements;
    }

    private String getCleanValue(CSVRecord record, String headerName) {
        return getCleanValue(record, headerName, "");
    }

    private String getCleanValue(CSVRecord record, String headerName, String defaultValue) {
        if (record.isMapped(headerName)) {
            String value = record.get(headerName);
            return (value != null) ? value.trim() : defaultValue;
        }
        return defaultValue;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
