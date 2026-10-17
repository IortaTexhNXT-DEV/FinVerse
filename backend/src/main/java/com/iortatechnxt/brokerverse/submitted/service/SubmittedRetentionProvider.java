package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
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
 * Retention candidates of record type {@value #RECORD_TYPE} (rule of V1076): masterlist records
 * booked, not renewed, excluded or closed whose last change is on or before the cutoff. Read only;
 * archive and purge stay parked.
 */
@Component
@Transactional(readOnly = true)
public class SubmittedRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "SUBMITTED_POLICY";

  private static final Set<SbmPolicyStatus> CLOSED =
      EnumSet.of(
          SbmPolicyStatus.BOOKED,
          SbmPolicyStatus.NOT_RENEWED,
          SbmPolicyStatus.EXCLUDED,
          SbmPolicyStatus.CLOSED);

  private final SbmPolicyRepository policies;

  /**
   * Creates the provider.
   *
   * @param policies masterlist
   */
  public SubmittedRetentionProvider(SbmPolicyRepository policies) {
    this.policies = policies;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return policies.count(eligible(criteria));
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return policies
        .findAll(
            eligible(criteria),
            PageRequest.of(0, Math.max(limit, 1), Sort.by("updatedAt", "createdAt", "id")))
        .stream()
        .map(SubmittedRetentionProvider::candidate)
        .toList();
  }

  private static Specification<SbmPolicy> eligible(RetentionCriteria criteria) {
    Set<SbmPolicyStatus> statuses =
        criteria.statuses().stream()
            .filter(s -> CLOSED.stream().anyMatch(c -> c.name().equals(s)))
            .map(SbmPolicyStatus::valueOf)
            .collect(Collectors.toSet());
    Instant limit = BusinessClock.startOf(criteria.lastActivityOnOrBefore().plusDays(1));
    return (root, query, cb) ->
        statuses.isEmpty()
            ? cb.disjunction()
            : cb.and(
                root.get("status").in(statuses),
                cb.lessThan(
                    cb.coalesce(root.<Instant>get("updatedAt"), root.<Instant>get("createdAt")),
                    limit));
  }

  private static RetentionCandidate candidate(SbmPolicy p) {
    Instant last = p.getUpdatedAt() == null ? p.getCreatedAt() : p.getUpdatedAt();
    return new RetentionCandidate(
        p.getSbmNo(),
        p.getAssured().assuredName() + " - expiring " + p.getTerms().expiryDate(),
        p.getStatus().name(),
        BusinessClock.dateOf(last),
        SubmittedCodes.link(p.getId()));
  }
}
