package com.iortatechnxt.brokerverse.renewal.retention;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (RENEWAL_DESIGN section 13; rule of
 * V1018): renewals closed (renewed or not) whose last change is on or before the cutoff. Read only;
 * archive and purge stay parked.
 */
@Component
@Transactional(readOnly = true)
public class RenewalRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "RENEWAL_CANDIDATE";

  private static final Set<RenewalStage> CLOSED =
      EnumSet.of(RenewalStage.RENEWED, RenewalStage.CLOSED);

  private final RenewalCandidateRepository candidates;

  /**
   * Creates the provider.
   *
   * @param candidates renewals
   */
  public RenewalRetentionProvider(RenewalCandidateRepository candidates) {
    this.candidates = candidates;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return candidates.count(eligible(criteria));
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return candidates
        .findAll(
            eligible(criteria),
            PageRequest.of(0, Math.max(limit, 1), Sort.by("updatedAt", "createdAt", "id")))
        .stream()
        .map(RenewalRetentionProvider::candidate)
        .toList();
  }

  private static Specification<RenewalCandidate> eligible(RetentionCriteria criteria) {
    Set<RenewalStage> stages =
        criteria.statuses().stream()
            .filter(s -> CLOSED.stream().anyMatch(c -> c.name().equals(s)))
            .map(RenewalStage::valueOf)
            .collect(Collectors.toSet());
    Instant limit = BusinessClock.startOf(criteria.lastActivityOnOrBefore().plusDays(1));
    return (root, query, cb) ->
        stages.isEmpty()
            ? cb.disjunction()
            : cb.and(
                root.get("stage").in(stages),
                cb.lessThan(
                    cb.coalesce(root.<Instant>get("updatedAt"), root.<Instant>get("createdAt")),
                    limit));
  }

  private static RetentionCandidate candidate(RenewalCandidate c) {
    Instant last = c.getUpdatedAt() == null ? c.getCreatedAt() : c.getUpdatedAt();
    return new RetentionCandidate(
        c.getRenewalRef(),
        c.getSnapshot().clientName() + " - expiring " + c.getExpiryDate(),
        c.getStage().name(),
        BusinessClock.dateOf(last),
        RenewalCodes.LINK + c.getRenewalRef());
  }
}
