package com.iortatechnxt.brokerverse.renewal.acceptance.service;

import com.iortatechnxt.brokerverse.booking.service.port.IncentiveAcceptanceGate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAcceptanceRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The acceptance gate of the incentive evaluation for renewals (BRRN.041; FR-RN-087): the renewal
 * account of a renewal is evaluated only once the acceptance of the client is recorded through the
 * Account Officer. An account that renews nothing (New Business) is not gated.
 */
@Component
@Transactional(readOnly = true)
public class RenewalIncentiveGate implements IncentiveAcceptanceGate {

  private final RenewalCandidateRepository candidates;
  private final RenewalAcceptanceRepository acceptances;

  /**
   * Creates the gate.
   *
   * @param candidates renewals
   * @param acceptances client acceptances
   */
  public RenewalIncentiveGate(
      RenewalCandidateRepository candidates, RenewalAcceptanceRepository acceptances) {
    this.candidates = candidates;
    this.acceptances = acceptances;
  }

  @Override
  public boolean acceptanceConfirmed(String arn) {
    return candidates
        .findFirstByRenewalArn(arn)
        .map(c -> !acceptances.findByCandidateIdOrderByIdDesc(c.getId()).isEmpty())
        .orElse(true);
  }
}
