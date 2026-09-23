package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Planned allocation of one transaction in the preview of an RI allocation run (policy currency).
 *
 * @param policyId policy
 * @param policyNo policy number
 * @param endorsementNo endorsement number (0 = original issue)
 * @param documentNo document number
 * @param kind NEW or endorsement type
 * @param businessLine line of business
 * @param approvalDate RI accounting date
 * @param currency policy currency
 * @param basis allocation basis, null when the plan failed
 * @param ourSi company sum insured
 * @param ourPremium company net premium
 * @param retention premium retained
 * @param quotaShare premium to the quota share
 * @param surplus premium to the surplus
 * @param fac facultative premium
 * @param message why the transaction cannot be planned yet, else null
 */
public record AllocationPreviewRow(
    Long policyId,
    String policyNo,
    int endorsementNo,
    String documentNo,
    String kind,
    String businessLine,
    LocalDate approvalDate,
    String currency,
    String basis,
    BigDecimal ourSi,
    BigDecimal ourPremium,
    BigDecimal retention,
    BigDecimal quotaShare,
    BigDecimal surplus,
    BigDecimal fac,
    String message) {

  /**
   * Builds a row.
   *
   * @param txn transaction
   * @param plan plan, null when planning failed
   * @param message failure message, null when planned
   * @return row
   */
  static AllocationPreviewRow of(PremiumTransaction txn, CessionPlan plan, String message) {
    BigDecimal ourPremium = txn.premium().getOurNetPremium();
    boolean planned = plan != null;
    return new AllocationPreviewRow(
        txn.ref().policyId(),
        txn.policy().policyNo(),
        txn.endorsementNo(),
        txn.documentNo(),
        txn.kind(),
        txn.policy().businessLine(),
        txn.approvalDate(),
        txn.policy().currency(),
        planned ? plan.basis().name() : null,
        txn.premium().getOurSumInsured(),
        ourPremium,
        planned ? plan.premiumOf(RiLayer.RETENTION) : ourPremium,
        planned ? plan.premiumOf(RiLayer.QUOTA_SHARE) : Money.zero(),
        planned ? plan.premiumOf(RiLayer.SURPLUS) : Money.zero(),
        planned ? plan.premiumOf(RiLayer.FAC) : Money.zero(),
        message);
  }
}
