package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.renewal.check.service.Evaluation;
import com.iortatechnxt.brokerverse.renewal.check.service.TsiThresholdCheck;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The dispositions BDOI's sanitation rules give to a Clean renewal account (BDOI Renewal FRS
 * FRRN.007.02, FRRN.008.01): For Quotation when the total sum insured is above the threshold (the
 * check passes as information when {@value #TSI_ROUTE} is QUOTATION), For Proposal for the lines of
 * {@value #PROPOSAL_LINES} (Personal Accident and Group Personal Accident), and For Renewal for
 * every other Clean account when {@value #CLEAN_AUTO} is true. A disposition of the decision matrix
 * takes precedence.
 */
@Component
public class BdoiSanitation {

  /** Parameter: QUOTATION (Clean, For Quotation) or REVIEW (Review, proposal for TSU). */
  public static final String TSI_ROUTE = "RNW_TSI_ABOVE_ROUTE";

  /** Parameter: product lines whose Clean renewals get For Proposal. */
  public static final String PROPOSAL_LINES = "RNW_PROPOSAL_LINES";

  /** Parameter: true to give For Renewal to the Clean renewals. */
  public static final String CLEAN_AUTO = "RNW_CLEAN_AUTO_RENEWAL";

  /** Value of {@value #TSI_ROUTE} for BDOI's rule. */
  public static final String QUOTATION = "QUOTATION";

  private final SystemParameterService parameters;

  /**
   * Creates the rules.
   *
   * @param parameters system parameters
   */
  public BdoiSanitation(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Whether the TSI above the threshold leads to For Quotation on a Clean account.
   *
   * @return true for BDOI's rule
   */
  public boolean tsiForQuotation() {
    return QUOTATION.equals(parameters.text(TSI_ROUTE, QUOTATION).strip());
  }

  /**
   * The disposition of a Clean renewal by BDOI's rules, with its reason. A migrated policy (no BIBS
   * account, so no claims, endorsement or premium checks) is not disposed For Renewal
   * automatically: Marketing confirms it.
   *
   * @param c renewal (classified)
   * @param evaluation checks of the classification
   * @return disposition, empty when the renewal is not Clean or no rule applies
   */
  public Optional<Rule> dispositionOf(RenewalCandidate c, Evaluation evaluation) {
    if (c.getBucket() != Bucket.CLEAN) {
      return Optional.empty();
    }
    boolean tsiAbove =
        evaluation
            .finding(TsiThresholdCheck.CODE)
            .map(f -> TsiThresholdCheck.QUOTATION_DETAIL.equals(f.detail()))
            .orElse(false);
    Rule rule = null;
    if (tsiAbove) {
      rule = new Rule(RenewalDisposition.FOR_QUOTATION, "Sum insured above the threshold: TSU");
    } else if (proposalLine(c)) {
      rule = new Rule(RenewalDisposition.FOR_PROPOSAL, "Personal accident line");
    } else if (c.getExpiringArn() != null
        && "true".equals(parameters.text(CLEAN_AUTO, "true").strip())) {
      rule = new Rule(RenewalDisposition.FOR_RENEWAL, "Clean account");
    }
    return Optional.ofNullable(rule);
  }

  private boolean proposalLine(RenewalCandidate c) {
    CandidateSnapshot.SnapshotProduct p = c.getSnapshot().product();
    String line = p == null ? null : p.lineCode();
    return line != null
        && parameters.items(PROPOSAL_LINES).stream().anyMatch(l -> l.strip().equals(line));
  }

  /**
   * A disposition of BDOI's sanitation rules.
   *
   * @param disposition disposition
   * @param reason the rule in words
   */
  public record Rule(RenewalDisposition disposition, String reason) {}
}
