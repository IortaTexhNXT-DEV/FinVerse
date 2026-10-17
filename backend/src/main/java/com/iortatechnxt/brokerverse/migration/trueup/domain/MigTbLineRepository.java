package com.iortatechnxt.brokerverse.migration.trueup.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link MigTbLine}. */
public interface MigTbLineRepository extends JpaRepository<MigTbLine, Long> {

  /**
   * The lines of a batch.
   *
   * @param batchId batch
   * @return lines
   */
  List<MigTbLine> findByBatchId(Long batchId);
}
