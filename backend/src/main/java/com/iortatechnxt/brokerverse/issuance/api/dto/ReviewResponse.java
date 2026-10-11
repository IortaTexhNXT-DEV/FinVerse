package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.Review;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extraction review (BRNB.104): the values extracted from the e-policy next to the account values,
 * with the differences to check before confirming.
 *
 * @param epolicy e-policy with the extracted values
 * @param account account values
 * @param differences differences between the two
 */
public record ReviewResponse(
    EpolicyResponse epolicy, AccountValues account, List<String> differences) {

  /**
   * Maps a review.
   *
   * @param review e-policy and account
   * @return response
   */
  public static ReviewResponse from(Review review) {
    Account a = review.account();
    AccountValues values =
        new AccountValues(
            a.getId(),
            a.getArn(),
            a.getClientName(),
            a.getStatus().name(),
            a.getInsurerCode(),
            a.getProductCode(),
            a.getPeriodFrom(),
            a.getPeriodTo(),
            a.getPremium().grossPremium(),
            a.getTermYears(),
            a.getPolicyNumbers());
    return new ReviewResponse(
        EpolicyResponse.from(review.epolicy()), values, differences(review.epolicy(), values));
  }

  private static List<String> differences(Epolicy e, AccountValues a) {
    List<String> found = new ArrayList<>();
    int numbers = e.getExtractedPolicyNumberList().size();
    if (numbers != a.termYears()) {
      found.add(a.termYears() + " policy number(s) expected, " + numbers + " found");
    }
    differ(found, "Period start", e.getExtractedPeriodFrom(), a.periodFrom());
    differ(found, "Period end", e.getExtractedPeriodTo(), a.periodTo());
    BigDecimal premium = e.getExtractedPremium();
    if (premium != null && a.grossPremium() != null && premium.compareTo(a.grossPremium()) != 0) {
      found.add(
          "Premium "
              + premium.toPlainString()
              + " differs from "
              + a.grossPremium().toPlainString());
    }
    return found;
  }

  private static void differ(
      List<String> found, String what, LocalDate extracted, LocalDate account) {
    if (extracted != null && !Objects.equals(extracted, account)) {
      found.add(what + " " + extracted + " differs from " + account);
    }
  }

  /**
   * Account values compared with the e-policy.
   *
   * @param accountId account id
   * @param arn ARN
   * @param clientName insured
   * @param status account status
   * @param insurerCode insurer
   * @param productCode product
   * @param periodFrom period start
   * @param periodTo period end
   * @param grossPremium gross premium
   * @param termYears policy years (one policy number each)
   * @param policyNumbers policy numbers already recorded
   */
  public record AccountValues(
      Long accountId,
      String arn,
      String clientName,
      String status,
      String insurerCode,
      String productCode,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal grossPremium,
      int termYears,
      List<String> policyNumbers) {}
}
