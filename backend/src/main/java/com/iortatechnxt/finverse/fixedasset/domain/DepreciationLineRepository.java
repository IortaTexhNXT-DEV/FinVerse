package com.iortatechnxt.finverse.fixedasset.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link DepreciationLine}. */
public interface DepreciationLineRepository extends JpaRepository<DepreciationLine, Long> {

  /**
   * Lines of a run.
   *
   * @param runId run
   * @return lines in posting order
   */
  List<DepreciationLine> findByRunIdOrderById(Long runId);
}
