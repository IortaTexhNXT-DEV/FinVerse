package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A CBG Fire account awaiting payment that can be billed through CLPC (BRNB.067).
 *
 * @param accountId account
 * @param arn reference
 * @param clientName borrower
 * @param pnNumbers PN numbers
 * @param loanApplicationNo loan application number
 * @param mortgageeBank mortgagee bank
 * @param periodFrom period start
 * @param grossPremium premium
 */
public record BillingCandidateResponse(
    Long accountId,
    String arn,
    String clientName,
    List<String> pnNumbers,
    String loanApplicationNo,
    String mortgageeBank,
    LocalDate periodFrom,
    BigDecimal grossPremium) {

  /**
   * Maps an account.
   *
   * @param a account
   * @return response
   */
  public static BillingCandidateResponse from(Account a) {
    return new BillingCandidateResponse(
        a.getId(),
        a.getArn(),
        a.getClientName(),
        a.getPnNumbers(),
        a.getLoanApplicationNo(),
        a.getMortgageeBank(),
        a.getPeriodFrom(),
        a.getPremium().grossPremium());
  }
}
