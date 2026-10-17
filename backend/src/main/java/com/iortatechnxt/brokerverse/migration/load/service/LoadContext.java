package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import java.time.LocalDate;

/**
 * What a loader knows about the batch it loads.
 *
 * @param batch the batch
 * @param maps approved code maps used by the validation
 * @param businessDate business date of the load
 */
public record LoadContext(MigBatch batch, CodeMaps maps, LocalDate businessDate) {

  /**
   * Company of the batch.
   *
   * @return company id
   */
  public Long companyId() {
    return batch.getCompanyId();
  }

  /**
   * Batch number (stored on the migrated records as their migration batch).
   *
   * @return batch number
   */
  public String batchNo() {
    return batch.getBatchNo();
  }
}
