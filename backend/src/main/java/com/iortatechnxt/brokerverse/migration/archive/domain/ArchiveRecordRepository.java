package com.iortatechnxt.brokerverse.migration.archive.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Legacy archive records. */
public interface ArchiveRecordRepository
    extends JpaRepository<ArchiveRecord, Long>, JpaSpecificationExecutor<ArchiveRecord> {

  /**
   * A record by its legacy key.
   *
   * @param companyId company
   * @param sourceSystem source system
   * @param recordType record type
   * @param legacyKey legacy key
   * @return record
   */
  Optional<ArchiveRecord> findByCompanyIdAndSourceSystemAndRecordTypeAndLegacyKey(
      Long companyId, String sourceSystem, String recordType, String legacyKey);

  /**
   * Live records of a batch.
   *
   * @param batchId batch
   * @return count
   */
  long countByBatchIdAndRolledBackFalse(Long batchId);
}
