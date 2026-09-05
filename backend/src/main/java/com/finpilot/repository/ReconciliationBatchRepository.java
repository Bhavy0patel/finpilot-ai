package com.finpilot.repository;

import com.finpilot.model.ReconciliationBatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for ReconciliationBatch persistence.
 */
@Repository
public interface ReconciliationBatchRepository extends MongoRepository<ReconciliationBatch, String> {

    /**
     * Retrieves all batches ordered by processing timestamp (latest first).
     */
    List<ReconciliationBatch> findAllByOrderByProcessedAtDesc();
}
