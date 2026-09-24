package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account.Origin;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;

/**
 * Request to create an account (contract used by the account screens, the bulk upload and the
 * quotation / proposal modules).
 *
 * @param companyId company
 * @param arn ARN generated at quotation or PRF creation; null to generate one (BRNB.102)
 * @param origin quotation and proposal references (plain values)
 * @param draft account data
 * @param premium premium already computed by the quotation; null to rate the account now
 * @param accountOfficer account officer; null for the current user
 */
public record NewAccount(
    Long companyId,
    String arn,
    Origin origin,
    AccountDraft draft,
    PremiumBreakdown premium,
    String accountOfficer) {

  /**
   * A direct account (ARN generated, rated now, current user as account officer).
   *
   * @param companyId company
   * @param draft account data
   * @return request
   */
  public static NewAccount direct(Long companyId, AccountDraft draft) {
    return new NewAccount(companyId, null, Origin.DIRECT, draft, null, null);
  }
}
