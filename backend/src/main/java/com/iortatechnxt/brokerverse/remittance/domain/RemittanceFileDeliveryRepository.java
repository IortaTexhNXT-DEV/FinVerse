package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Copies of the extract files to the shared folder. */
public interface RemittanceFileDeliveryRepository
    extends JpaRepository<RemittanceFileDelivery, Long> {

  /**
   * Copies of a batch, newest first.
   *
   * @param batchId batch
   * @return copies
   */
  List<RemittanceFileDelivery> findByBatchIdOrderByIdDesc(Long batchId);
}
