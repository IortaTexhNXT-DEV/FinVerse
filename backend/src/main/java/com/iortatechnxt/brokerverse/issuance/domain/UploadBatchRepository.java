package com.iortatechnxt.brokerverse.issuance.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Bulk e-policy uploads. */
public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {}
