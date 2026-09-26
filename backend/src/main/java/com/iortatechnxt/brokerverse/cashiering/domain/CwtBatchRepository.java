package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** BIR 2307 batches (CSHID.027). */
public interface CwtBatchRepository extends JpaRepository<CwtBatch, Long> {

  /**
   * A batch by number.
   *
   * @param batchNo CWB- number
   * @return batch
   */
  Optional<CwtBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @return batches
   */
  List<CwtBatch> findTop50ByCompanyIdOrderByIdDesc(Long companyId);
}
