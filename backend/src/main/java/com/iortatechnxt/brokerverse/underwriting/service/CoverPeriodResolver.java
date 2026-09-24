package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.underwriting.domain.Endorsement;
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Rebuilds the policy periods of one policy (original period and approved renewals) and derives the
 * cover period of each of its premium transactions.
 *
 * <p>A renewal overwrites the policy header period, so the start of the original period is no
 * longer stored once a policy is renewed: it is then taken as the policy issue date (the original
 * period ends the day before the first renewed period starts).
 */
final class CoverPeriodResolver {

  private final PolicySnapshot policy;
  private final List<Endorsement> renewals;

  /**
   * Creates the resolver.
   *
   * @param policy policy header (current period)
   * @param endorsements endorsements of the policy (any status)
   */
  CoverPeriodResolver(PolicySnapshot policy, List<Endorsement> endorsements) {
    this.policy = policy;
    this.renewals =
        endorsements.stream()
            .filter(e -> e.getEndorsementType() == EndorsementType.RENEWAL)
            .filter(e -> e.getWorkflow().getApprovalDate() != null)
            .sorted(Comparator.comparingInt(Endorsement::getEndorsementNo))
            .toList();
  }

  /**
   * Cover period of one transaction of this policy.
   *
   * @param t transaction
   * @return cover period
   */
  CoverPeriod coverOf(PremiumTransaction t) {
    if (t.ref().isOriginal()) {
      return originalPeriod();
    }
    if (EndorsementType.RENEWAL.name().equals(t.kind())) {
      return renewals.stream()
          .filter(r -> r.getEndorsementNo() == t.endorsementNo())
          .findFirst()
          .map(r -> new CoverPeriod(r.getNewPeriodFrom(), r.getNewPeriodTo()))
          .orElseGet(() -> new CoverPeriod(t.effectiveDate(), policy.periodTo()));
    }
    return new CoverPeriod(t.effectiveDate(), periodInForceAt(t.endorsementNo()).to());
  }

  /** Period in force when endorsement {@code endorsementNo} was made: the latest renewal before. */
  private CoverPeriod periodInForceAt(int endorsementNo) {
    CoverPeriod period = originalPeriod();
    for (Endorsement r : renewals) {
      if (r.getEndorsementNo() < endorsementNo) {
        period = new CoverPeriod(r.getNewPeriodFrom(), r.getNewPeriodTo());
      }
    }
    return period;
  }

  private CoverPeriod originalPeriod() {
    if (renewals.isEmpty()) {
      return new CoverPeriod(policy.periodFrom(), policy.periodTo());
    }
    LocalDate to = renewals.get(0).getNewPeriodFrom().minusDays(1);
    LocalDate from = policy.issueDate().isAfter(to) ? to : policy.issueDate();
    return new CoverPeriod(from, to);
  }
}
