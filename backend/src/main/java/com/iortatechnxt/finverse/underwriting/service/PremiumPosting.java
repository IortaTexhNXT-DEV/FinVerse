package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import java.time.LocalDate;

/**
 * Everything needed to account for an approved policy or endorsement.
 *
 * @param eventType accounting event (POLICY_ISSUE, POLICY_ENDORSEMENT, POLICY_CANCELLATION)
 * @param sourceReference unique key, e.g. {@code POLICY:12} or {@code POLICY:12:ENDT:2}
 * @param reference business reference shown on the ledger (policy / endorsement number)
 * @param policy policy (company, branch, currency, parties, line of business)
 * @param date accounting date
 * @param narration narration
 * @param premium premium figures (negative for return premium)
 */
public record PremiumPosting(
    String eventType,
    String sourceReference,
    String reference,
    Policy policy,
    LocalDate date,
    String narration,
    PremiumBreakdown premium) {

  /**
   * Posting of an original policy issue.
   *
   * @param p approved policy
   * @return posting
   */
  public static PremiumPosting of(Policy p) {
    return new PremiumPosting(
        "POLICY_ISSUE",
        sourceKey(p),
        p.getPolicyNo(),
        p,
        p.getWorkflow().getApprovalDate(),
        "Policy " + p.getPolicyNo() + " issued - " + p.getInsuredName(),
        p.getPremium());
  }

  /**
   * Posting of an endorsement.
   *
   * @param e approved endorsement
   * @param eventType event type of the endorsement type
   * @return posting
   */
  public static PremiumPosting of(Endorsement e, String eventType) {
    Policy p = e.getPolicy();
    return new PremiumPosting(
        eventType,
        sourceKey(p) + ":ENDT:" + e.getEndorsementNo(),
        e.documentNo(),
        p,
        e.getWorkflow().getApprovalDate(),
        e.getEndorsementType() + " endorsement " + e.documentNo() + " - " + p.getInsuredName(),
        e.getPremium());
  }

  /**
   * Accounting source key of a policy.
   *
   * @param p policy
   * @return key
   */
  public static String sourceKey(Policy p) {
    return "POLICY:" + p.getId();
  }

  /**
   * Coinsurer to credit (only when the company leads and bills the coinsurers' share).
   *
   * @return coinsurer or null
   */
  public Party leadingCoinsurer() {
    return policy.isCoinsuranceLeader() ? policy.getCoinsurer() : null;
  }
}
