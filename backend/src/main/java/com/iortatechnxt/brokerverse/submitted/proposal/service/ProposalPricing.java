package com.iortatechnxt.brokerverse.submitted.proposal.service;

import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposal;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService.Line;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService.Proposed;
import com.iortatechnxt.brokerverse.submitted.renewal.service.HandOffService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * The insurer, nominated rate and premium proposed for a record (FR-SP-066, 067): the insurer of
 * the hand-off rules (else the expiring insurer) or the one chosen, its nominated rate, the rate
 * entered with its reason, and the premium in percent of the sum insured.
 */
@Component
public class ProposalPricing {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final NominatedRateService rates;
  private final HandOffService handOff;
  private final InsurerService insurers;
  private final Clock clock;

  /**
   * Creates the pricing.
   *
   * @param rates nominated rates
   * @param handOff insurer rules of the hand-off (default insurer)
   * @param insurers insurer master
   * @param clock clock
   */
  public ProposalPricing(
      NominatedRateService rates, HandOffService handOff, InsurerService insurers, Clock clock) {
    this.rates = rates;
    this.handOff = handOff;
    this.insurers = insurers;
    this.clock = clock;
  }

  Proposed proposed(SbmPolicy p, String chosen) {
    String fallback = handOff.assignInsurer(p).orElse(p.getTerms().insurerCode());
    String insurer = chosen == null ? fallback : chosen;
    BigDecimal nominated = nominated(p, insurer);
    return new Proposed(
        p.getId(),
        p.getSbmNo(),
        p.getAssured().assuredName(),
        p.getSegment(),
        p.getRisk().vehicleType(),
        p.getTerms().sumInsured(),
        fallback,
        insurer,
        nominated,
        nominated == null ? null : premium(p, nominated),
        nominated == null ? "No nominated rate" : null);
  }

  SbmProposal.Terms terms(SbmPolicy p, Line line) {
    String chosen = blank(line.insurerCode()) ? null : line.insurerCode().strip();
    Proposed proposed = proposed(p, chosen);
    if (chosen != null) {
      insurers.requireUsableInsurer(p.getCompanyId(), chosen);
    }
    BigDecimal applied = applied(p, line, proposed.nominatedRate());
    boolean edited =
        proposed.nominatedRate() == null || applied.compareTo(proposed.nominatedRate()) != 0;
    if (edited && blank(line.reason())) {
      throw new BusinessRuleException(
          "SBM_PROPOSAL_RATE_REASON", "Enter the reason for changing the nominated rate");
    }
    return new SbmProposal.Terms(
        proposed.defaultInsurer(),
        proposed.insurerCode(),
        proposed.nominatedRate(),
        applied,
        edited ? line.reason().strip() : null,
        premium(p, applied));
  }

  /** The rate applied: the one entered, else the nominated one; between 0 and 100 percent. */
  private static BigDecimal applied(SbmPolicy p, Line line, BigDecimal nominated) {
    BigDecimal applied = line.rate() == null ? nominated : line.rate();
    if (applied == null) {
      throw new BusinessRuleException(
          "SBM_PROPOSAL_NO_RATE",
          "Policy " + p.getSbmNo() + " has no nominated rate. Enter the rate or remove the policy");
    }
    if (applied.signum() <= 0 || applied.compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException(
          "SBM_NOMINATED_RATE_RANGE", "The rate must be between 0 and 100 percent");
    }
    return applied;
  }

  BigDecimal nominated(SbmPolicy p, String insurerCode) {
    return rates
        .rateOf(
            p.getCompanyId(),
            p.getSegment(),
            p.getRisk().vehicleType(),
            insurerCode,
            BusinessClock.today(clock))
        .orElse(null);
  }

  static BigDecimal premium(SbmPolicy p, BigDecimal rate) {
    BigDecimal sumInsured = p.getTerms().sumInsured();
    return sumInsured == null
        ? null
        : sumInsured.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
}
