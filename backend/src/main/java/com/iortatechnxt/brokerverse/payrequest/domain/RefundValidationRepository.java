package com.iortatechnxt.brokerverse.payrequest.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Refund validation tasks (MKT 1.11.0). */
public interface RefundValidationRepository extends JpaRepository<RefundValidation, Long> {

  /**
   * Tasks of a request, newest round first.
   *
   * @param requestId request
   * @return tasks
   */
  List<RefundValidation> findByRequestIdOrderByRoundNoDescLineNoAscValidatorAsc(Long requestId);

  /**
   * Tasks of one round.
   *
   * @param requestId request
   * @param roundNo round
   * @return tasks
   */
  List<RefundValidation> findByRequestIdAndRoundNo(Long requestId, int roundNo);

  /**
   * A task by its validator and source reference (the result event).
   *
   * @param validator validator
   * @param sourceRef source reference
   * @return task
   */
  Optional<RefundValidation> findByValidatorAndSourceRef(String validator, String sourceRef);
}
