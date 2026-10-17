package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeader.LegacyPolicy;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * An in-force legacy policy imported as an account by the data migration (object P01; see {@link
 * LegacyAccountImport}).
 *
 * @param companyId company
 * @param clientCode BIBS code of the migrated client
 * @param productCode BIBS risk code (mapped)
 * @param businessType new business or renewal, as in legacy
 * @param cover insurer, period, currency, segment, channel, arrangement and mortgage
 * @param sales sales region, team and account officer
 * @param sumInsured total sum insured
 * @param riskDescription description of the risk
 * @param premium premium breakdown as in legacy
 * @param policyNos policy numbers
 * @param legacy legacy header
 */
public record LegacyAccount(
    Long companyId,
    String clientCode,
    String productCode,
    BusinessType businessType,
    Cover cover,
    SalesStamp sales,
    BigDecimal sumInsured,
    String riskDescription,
    AccountPremium premium,
    List<String> policyNos,
    LegacyPolicy legacy) {

  /** Defensive copy. */
  public LegacyAccount {
    policyNos = policyNos == null ? List.of() : List.copyOf(policyNos);
  }

  /**
   * The cover of the policy.
   *
   * @param insurerCode insurer (lead insurer of a co-insured policy)
   * @param insurerBranch insurer branch
   * @param periodFrom inception
   * @param periodTo expiry
   * @param currency currency
   * @param marketSegment market segment
   * @param sourceChannel business origin
   * @param arrangement who collects the premium
   * @param mortgage mortgagee bank, loan application and promissory notes
   */
  public record Cover(
      String insurerCode,
      String insurerBranch,
      LocalDate periodFrom,
      LocalDate periodTo,
      String currency,
      String marketSegment,
      String sourceChannel,
      PaymentArrangement arrangement,
      Mortgage mortgage) {}
}
