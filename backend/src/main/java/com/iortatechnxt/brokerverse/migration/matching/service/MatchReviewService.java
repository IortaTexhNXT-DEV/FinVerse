package com.iortatechnxt.brokerverse.migration.matching.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch.Decision;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatchRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Data Steward's client review queue (FR-DM-031): side-by-side legacy records of a doubtful
 * pair with the keys that matched and the score; the steward merges the pair or keeps the two
 * clients. The load of a client batch waits until the queue of the batch is empty.
 */
@Service
@Transactional
public class MatchReviewService {

  private static final String ENTITY = "MigClientMatch";

  private final ClientMatchRepository matches;
  private final StageRowRepository rows;
  private final MigBatchRepository batches;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param matches pairs
   * @param rows staged rows
   * @param batches batches
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public MatchReviewService(
      ClientMatchRepository matches,
      StageRowRepository rows,
      MigBatchRepository batches,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.matches = matches;
    this.rows = rows;
    this.batches = batches;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Pairs waiting for review.
   *
   * @return pairs, oldest first
   */
  @Transactional(readOnly = true)
  public List<ClientMatch> queue() {
    return matches.findByDecisionOrderByIdAsc(Decision.REVIEW);
  }

  /**
   * Pairs of a batch.
   *
   * @param batchId batch
   * @return pairs
   */
  @Transactional(readOnly = true)
  public List<ClientMatch> ofBatch(Long batchId) {
    return matches.findByBatchIdOrderByClusterNoAscIdAsc(batchId);
  }

  /**
   * The values of the two records of a pair.
   *
   * @param pairId pair
   * @return left and right values (right empty for a BIBS client)
   */
  @Transactional(readOnly = true)
  public Sides sides(Long pairId) {
    ClientMatch m = get(pairId);
    return new Sides(values(m.getLeftRowId()), values(m.getRightRowId()));
  }

  private Map<String, String> values(Long rowId) {
    if (rowId == null) {
      return Map.of();
    }
    Optional<StageRow> row = rows.findById(rowId);
    return row.map(r -> r.getMappedPayload().isEmpty() ? r.getRawPayload() : r.getMappedPayload())
        .orElse(Map.of());
  }

  /**
   * Decides a pair.
   *
   * @param pairId pair
   * @param merge merge the two records
   * @return the pair
   */
  public ClientMatch decide(Long pairId, boolean merge) {
    ClientMatch m = get(pairId);
    if (m.getDecision() != Decision.REVIEW) {
      throw new BusinessRuleException("MIG_PAIR_DECIDED", "This pair is already decided");
    }
    m.decide(merge, currentUser.username(), clock.instant());
    MigBatch batch = batches.findById(m.getBatchId()).orElseThrow();
    long left = matches.countByBatchIdAndDecision(batch.getId(), Decision.REVIEW);
    batch.validationFindings(batch.getUnmappedCount(), (int) left);
    audit.record(
        ENTITY,
        pairId,
        AuditAction.UPDATE,
        (merge ? "Merged " : "Kept separate ") + m.getLeftKey() + " and " + m.getRightKey());
    return m;
  }

  private ClientMatch get(Long pairId) {
    return matches
        .findById(pairId)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, pairId));
  }

  /**
   * The two records of a pair.
   *
   * @param left left values
   * @param right right values
   */
  public record Sides(Map<String, String> left, Map<String, String> right) {}
}
