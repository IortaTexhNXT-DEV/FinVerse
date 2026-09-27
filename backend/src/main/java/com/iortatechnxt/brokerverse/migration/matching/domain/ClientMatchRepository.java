package com.iortatechnxt.brokerverse.migration.matching.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client match pairs. */
public interface ClientMatchRepository extends JpaRepository<ClientMatch, Long> {

  /**
   * Pairs of a batch.
   *
   * @param batchId batch
   * @return pairs
   */
  List<ClientMatch> findByBatchIdOrderByClusterNoAscIdAsc(Long batchId);

  /**
   * Pairs of a batch with a decision.
   *
   * @param batchId batch
   * @param decisions decisions
   * @return pairs
   */
  List<ClientMatch> findByBatchIdAndDecisionIn(
      Long batchId, Collection<ClientMatch.Decision> decisions);

  /**
   * Pairs waiting for review, oldest first.
   *
   * @param decision REVIEW
   * @return pairs
   */
  List<ClientMatch> findByDecisionOrderByIdAsc(ClientMatch.Decision decision);

  /**
   * Removes the pairs of a batch before matching again (decided pairs are kept).
   *
   * @param batchId batch
   * @param decisions decisions to remove
   */
  void deleteByBatchIdAndDecisionIn(Long batchId, Collection<ClientMatch.Decision> decisions);

  /**
   * Number of pairs with a decision.
   *
   * @param batchId batch
   * @param decision decision
   * @return count
   */
  long countByBatchIdAndDecision(Long batchId, ClientMatch.Decision decision);
}
