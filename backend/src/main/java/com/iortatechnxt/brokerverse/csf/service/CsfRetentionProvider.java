package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChange;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChangeRepository;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (rule of V1040; FR-CSF-040 R2: 5 years
 * online, 15 years in total): contact changes, refusals and referrals made on or before the cutoff.
 * Read only; archive and purge stay parked.
 */
@Component
@Transactional(readOnly = true)
public class CsfRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "CSF_CONTACT_CHANGE";

  private static final String APPLIED_AT = "appliedAt";

  private final CsfContactChangeRepository changes;

  /**
   * Creates the provider.
   *
   * @param changes contact changes
   */
  public CsfRetentionProvider(CsfContactChangeRepository changes) {
    this.changes = changes;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return changes.count(eligible(criteria));
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return changes
        .findAll(
            eligible(criteria), PageRequest.of(0, Math.max(limit, 1), Sort.by(APPLIED_AT, "id")))
        .stream()
        .map(CsfRetentionProvider::candidate)
        .toList();
  }

  private static Specification<CsfContactChange> eligible(RetentionCriteria criteria) {
    Set<ChangeStatus> statuses =
        criteria.statuses().stream()
            .filter(s -> Arrays.stream(ChangeStatus.values()).anyMatch(c -> c.name().equals(s)))
            .map(ChangeStatus::valueOf)
            .collect(Collectors.toSet());
    Instant limit = BusinessClock.startOf(criteria.lastActivityOnOrBefore().plusDays(1));
    return (root, query, cb) ->
        statuses.isEmpty()
            ? cb.disjunction()
            : cb.and(
                root.get("status").in(statuses), cb.lessThan(root.<Instant>get(APPLIED_AT), limit));
  }

  private static RetentionCandidate candidate(CsfContactChange c) {
    return new RetentionCandidate(
        c.getChangeNo(),
        c.getClientCode() + " " + c.getClientName(),
        c.getStatus().name(),
        BusinessClock.dateOf(c.getAppliedAt()),
        "/csf/clients/" + c.getClientId() + "?tab=history");
  }
}
