package com.iortatechnxt.brokerverse.migration.recon.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reconciliation lines. */
public interface ReconLineRepository extends JpaRepository<ReconLine, Long> {

  /**
   * Lines of a run.
   *
   * @param runId run
   * @return lines by level
   */
  List<ReconLine> findByRunIdOrderByLevelAscIdAsc(Long runId);

  /**
   * Lines of runs in a status.
   *
   * @param runIds runs
   * @param status status
   * @return lines
   */
  List<ReconLine> findByRunIdInAndStatus(Collection<Long> runIds, ReconLine.Status status);
}
