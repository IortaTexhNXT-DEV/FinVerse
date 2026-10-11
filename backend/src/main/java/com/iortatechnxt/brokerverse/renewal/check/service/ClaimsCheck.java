package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.brokerclaims.service.ClaimExperienceQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.service.ClaimExperienceQueryService.ClaimExperience;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code CLAIMS} (BRRN.031/034; decision D4): the loss experience of the expiring term from the
 * Claims module ({@code ClaimExperienceQueryService.summary(arn, policyYear)}). Open claims fail
 * (Review: no automatic disposition); paid claims pass with the count. Without the Claims module
 * the check reports "claims not connected" and does not block. A total-loss indicator is not
 * defined yet (CLQ28).
 */
@Component
public class ClaimsCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "CLAIMS";

  /** Message when the Claims module is absent. */
  public static final String NOT_CONNECTED = "Claims not connected";

  private final ObjectProvider<ClaimExperienceQueryService> claims;

  /**
   * Creates the check.
   *
   * @param claims Claims read API, when the module is deployed
   */
  public ClaimsCheck(ObjectProvider<ClaimExperienceQueryService> claims) {
    this.claims = claims;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    ClaimExperienceQueryService service = claims.getIfAvailable();
    if (service == null) {
      return Verdict.info(NOT_CONNECTED);
    }
    if (c.getExpiringArn() == null) {
      return Verdict.notApplicable("No BIBS account to read the claims of");
    }
    ClaimExperience experience = service.summary(c.getExpiringArn(), c.getPolicyYear());
    if (experience.openCount() > 0) {
      return Verdict.fail(
          count(experience.openCount(), "open claim") + " on the expiring term",
          "claims " + experience.claimCount() + ", open " + experience.openCount());
    }
    return experience.withClaim()
        ? new Verdict(
            CheckOutcome.PASS,
            count(experience.claimCount(), "claim") + ", none open",
            "claims " + experience.claimCount() + ", open 0")
        : Verdict.pass("No claim on the expiring term");
  }

  /** "1 open claim", "2 open claims". */
  private static String count(int n, String noun) {
    return n + " " + noun + (n == 1 ? "" : "s");
  }
}
