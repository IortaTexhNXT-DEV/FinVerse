package com.iortatechnxt.brokerverse.productmaint.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stored comparative outputs. */
public interface ComparativeOutputRepository extends JpaRepository<ComparativeOutput, Long> {

  /**
   * Outputs of a request, newest first.
   *
   * @param requestId request
   * @return outputs
   */
  List<ComparativeOutput> findByRequestIdOrderByIdDesc(Long requestId);

  /**
   * The current master of a request.
   *
   * @param requestId request
   * @param kind MASTER
   * @return master
   */
  Optional<ComparativeOutput> findByRequestIdAndKindAndCurrentTrue(
      Long requestId, ComparativeOutput.Kind kind);

  /**
   * Number of outputs generated since a time (home tile "outputs of the week").
   *
   * @param since time
   * @return count
   */
  long countByCreatedAtGreaterThanEqual(Instant since);
}
