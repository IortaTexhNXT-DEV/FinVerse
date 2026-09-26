package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@code BROKER_CLAIM} (NFR p.41, Q39; rule seeded by V1020:
 * closed claims, 10 years online, purge after 15): claims in the rule's phases whose last change is
 * on or before the cutoff. Read only; archive and purge stay parked.
 */
@Component
@Transactional(readOnly = true)
public class BrokerClaimRetentionProvider implements RetentionCandidateProvider {

  private final BrokerClaimRepository claims;

  /**
   * Creates the provider.
   *
   * @param claims claims
   */
  public BrokerClaimRetentionProvider(BrokerClaimRepository claims) {
    this.claims = claims;
  }

  @Override
  public String recordType() {
    return ClaimCodes.RETENTION_RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    Set<ClaimPhase> phases = phases(criteria);
    return phases.isEmpty() ? 0 : claims.countRetention(phases, cutoff(criteria));
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    Set<ClaimPhase> phases = phases(criteria);
    if (phases.isEmpty()) {
      return List.of();
    }
    return claims
        .retention(phases, cutoff(criteria), PageRequest.of(0, Math.max(1, limit)))
        .stream()
        .map(BrokerClaimRetentionProvider::candidate)
        .toList();
  }

  private static Set<ClaimPhase> phases(RetentionCriteria criteria) {
    return Arrays.stream(ClaimPhase.values())
        .filter(p -> criteria.statuses().contains(p.name()))
        .collect(Collectors.toSet());
  }

  private static Instant cutoff(RetentionCriteria criteria) {
    return criteria.lastActivityOnOrBefore().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static RetentionCandidate candidate(Claim c) {
    return new RetentionCandidate(
        c.getClaimNo(),
        c.getCover().getAssuredName() + " - " + c.getCover().getArn(),
        c.getProgress().getPhase().name(),
        RetentionQueries.lastActivity(c),
        "/claims-handling/" + c.getId());
  }
}
