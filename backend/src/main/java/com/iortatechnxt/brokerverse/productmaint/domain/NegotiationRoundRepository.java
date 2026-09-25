package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Negotiation rounds. */
public interface NegotiationRoundRepository extends JpaRepository<NegotiationRound, Long> {

  /**
   * Rounds of a request, first round first, with their insurers.
   *
   * @param requestId request
   * @return rounds
   */
  @EntityGraph(attributePaths = "insurers")
  List<NegotiationRound> findByRequestIdOrderByRoundNo(Long requestId);

  /**
   * The latest round of a request.
   *
   * @param requestId request
   * @return round
   */
  @EntityGraph(attributePaths = "insurers")
  Optional<NegotiationRound> findFirstByRequestIdOrderByRoundNoDesc(Long requestId);

  /**
   * One round of a request.
   *
   * @param requestId request
   * @param roundNo round number
   * @return round
   */
  @EntityGraph(attributePaths = "insurers")
  Optional<NegotiationRound> findByRequestIdAndRoundNo(Long requestId, int roundNo);
}
