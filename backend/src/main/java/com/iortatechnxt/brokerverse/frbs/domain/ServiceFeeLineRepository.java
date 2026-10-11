package com.iortatechnxt.brokerverse.frbs.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lines of the service-fee runs (FRBS 2.10.0-2.10.2). */
public interface ServiceFeeLineRepository extends JpaRepository<ServiceFeeLine, Long> {

  /**
   * The lines of a run.
   *
   * @param runId run
   * @return lines in order
   */
  List<ServiceFeeLine> findByRunIdOrderByLineNoAsc(Long runId);

  /**
   * One line of a run.
   *
   * @param runId run
   * @param lineNo line number
   * @return line
   */
  Optional<ServiceFeeLine> findByRunIdAndLineNo(Long runId, int lineNo);
}
