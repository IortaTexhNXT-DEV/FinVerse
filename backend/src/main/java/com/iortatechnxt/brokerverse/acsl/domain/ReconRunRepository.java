package com.iortatechnxt.brokerverse.acsl.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** SOA reconciliation runs (ACSL 2.13.0). */
public interface ReconRunRepository extends JpaRepository<ReconRun, Long> {

  /**
   * Number of runs of an upload.
   *
   * @param uploadId upload
   * @return count
   */
  long countByUploadId(Long uploadId);
}
