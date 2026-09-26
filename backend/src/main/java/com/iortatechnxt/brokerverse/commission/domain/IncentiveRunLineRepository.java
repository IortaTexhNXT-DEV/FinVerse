package com.iortatechnxt.brokerverse.commission.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lines of the incentive runs. */
public interface IncentiveRunLineRepository extends JpaRepository<IncentiveRunLine, Long> {

  /**
   * Lines of a run.
   *
   * @param runId run
   * @param pageable page
   * @return lines
   */
  Page<IncentiveRunLine> findByRunIdOrderByIdAsc(Long runId, Pageable pageable);

  /**
   * Every line of a run.
   *
   * @param runId run
   * @return lines
   */
  List<IncentiveRunLine> findByRunIdOrderByIdAsc(Long runId);
}
