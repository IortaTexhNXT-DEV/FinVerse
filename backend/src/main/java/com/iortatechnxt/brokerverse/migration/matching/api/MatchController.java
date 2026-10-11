package com.iortatechnxt.brokerverse.migration.matching.api;

import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.matching.api.dto.PairResponse;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import com.iortatechnxt.brokerverse.migration.matching.service.MatchReviewService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client matching review (FR-DM-031; screen Matching): the queue of doubtful pairs, the two records
 * side by side and the Data Steward's merge or keep-separate decision.
 */
@RestController
@RequestMapping("/api/v1/migration/matches")
@Transactional
public class MatchController {

  private final MatchReviewService review;
  private final MigBatchRepository batches;
  private final BatchPlanService plans;

  /**
   * Creates the controller.
   *
   * @param review review
   * @param batches batches
   * @param plans batch lookup
   */
  public MatchController(
      MatchReviewService review, MigBatchRepository batches, BatchPlanService plans) {
    this.review = review;
    this.batches = batches;
    this.plans = plans;
  }

  /**
   * Pairs waiting for review, or all pairs of a batch.
   *
   * @param batchNo batch filter
   * @return pairs
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public List<PairResponse> list(@RequestParam(required = false) String batchNo) {
    List<ClientMatch> pairs =
        batchNo == null ? review.queue() : review.ofBatch(plans.get(batchNo).getId());
    Map<Long, String> nos = new HashMap<>();
    return pairs.stream()
        .map(
            m ->
                PairResponse.from(
                    m,
                    nos.computeIfAbsent(
                        m.getBatchId(),
                        id -> batches.findById(id).map(MigBatch::getBatchNo).orElse(null))))
        .toList();
  }

  /**
   * The two records of a pair.
   *
   * @param pairId pair
   * @return values side by side
   */
  @GetMapping("/{pairId}/sides")
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public MatchReviewService.Sides sides(@PathVariable Long pairId) {
    return review.sides(pairId);
  }

  /**
   * Decides a pair.
   *
   * @param pairId pair
   * @param merge merge the two records, or keep them separate
   * @return the pair
   */
  @PostMapping("/{pairId}/decide")
  @PreAuthorize("hasAuthority('MIG_MATCH_DECIDE')")
  public PairResponse decide(@PathVariable Long pairId, @RequestParam boolean merge) {
    ClientMatch m = review.decide(pairId, merge);
    return PairResponse.from(
        m, batches.findById(m.getBatchId()).map(MigBatch::getBatchNo).orElse(null));
  }
}
