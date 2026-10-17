package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account.Origin;
import com.iortatechnxt.brokerverse.account.domain.AccountClassification;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;

/**
 * Request to create an account (contract used by the account screens, the bulk upload and the
 * quotation / proposal modules, and by Renewal, Employee Benefits and Submitted Policies through
 * {@link #renewal}).
 *
 * <p>Business type (shared work item BT0, BRNB.097): every constructor and factory except {@link
 * #renewal} creates NEW_BUSINESS, so the existing callers do not change.
 *
 * @param companyId company
 * @param arn ARN generated at quotation or PRF creation; null to generate one (BRNB.102)
 * @param origin quotation and proposal references (plain values)
 * @param draft account data
 * @param premium premium already computed by the quotation; null to rate the account now
 * @param accountOfficer account officer; null for the current user
 * @param productVersionNo package version that priced the given premium (quotation / PRF,
 *     BRPM.007), or the version a renewal keeps (renew as is, PQ11); null to record the version the
 *     account is rated on now
 * @param rateOverrideRef approved rate-scheme exception of the quotation, null when none
 * @param classification business type, renewal link and origin kind; null for new business of the
 *     references' kind (quotation, PRF or direct)
 */
public record NewAccount(
    Long companyId,
    String arn,
    Origin origin,
    AccountDraft draft,
    PremiumBreakdown premium,
    String accountOfficer,
    Integer productVersionNo,
    String rateOverrideRef,
    AccountClassification classification) {

  /** New business of the references' kind when no classification is given. */
  public NewAccount {
    origin = origin == null ? Origin.DIRECT : origin;
    classification =
        classification == null
            ? AccountClassification.newBusiness(AccountOrigin.of(origin))
            : classification;
  }

  /**
   * A new-business request with package version and exception (quotation, PRF).
   *
   * @param companyId company
   * @param arn ARN, null to generate one
   * @param origin quotation and proposal references
   * @param draft account data
   * @param premium premium already computed, null to rate now
   * @param accountOfficer account officer, null for the current user
   * @param productVersionNo package version that priced the premium
   * @param rateOverrideRef approved rate-scheme exception, null when none
   */
  public NewAccount(
      Long companyId,
      String arn,
      Origin origin,
      AccountDraft draft,
      PremiumBreakdown premium,
      String accountOfficer,
      Integer productVersionNo,
      String rateOverrideRef) {
    this(
        companyId,
        arn,
        origin,
        draft,
        premium,
        accountOfficer,
        productVersionNo,
        rateOverrideRef,
        null);
  }

  /**
   * A new-business request without package version or exception (non-package products, bulk
   * upload).
   *
   * @param companyId company
   * @param arn ARN, null to generate one
   * @param origin quotation and proposal references
   * @param draft account data
   * @param premium premium already computed, null to rate now
   * @param accountOfficer account officer, null for the current user
   */
  public NewAccount(
      Long companyId,
      String arn,
      Origin origin,
      AccountDraft draft,
      PremiumBreakdown premium,
      String accountOfficer) {
    this(companyId, arn, origin, draft, premium, accountOfficer, null, null, null);
  }

  /**
   * A direct new-business account (ARN generated, rated now, current user as account officer).
   *
   * @param companyId company
   * @param draft account data
   * @return request
   */
  public static NewAccount direct(Long companyId, AccountDraft draft) {
    return new NewAccount(companyId, null, Origin.DIRECT, draft, null, null);
  }

  /**
   * A new-business account created by a business flow of its own (for example an Employee Benefits
   * cycle of type NEW_BUSINESS): ARN generated, rated now unless a premium is given.
   *
   * @param companyId company
   * @param origin origin kind
   * @param draft account data
   * @param premium premium already computed, null to rate now
   * @param accountOfficer account officer, null for the current user
   * @return request
   */
  public static NewAccount newBusiness(
      Long companyId,
      AccountOrigin origin,
      AccountDraft draft,
      PremiumBreakdown premium,
      String accountOfficer) {
    return new NewAccount(
        companyId,
        null,
        Origin.DIRECT,
        draft,
        premium,
        accountOfficer,
        null,
        null,
        AccountClassification.newBusiness(origin));
  }

  /**
   * A RENEWAL account (BT0; BRRN.033, BRID-022.01, BRIDSP-26/27): ARN generated, rated now on the
   * current package version with the RENEWAL rating purpose.
   *
   * @param companyId company
   * @param origin who renews: RENEWAL, EMPLOYEE_BENEFITS or SUBMITTED_POLICY
   * @param draft account data of the renewal term
   * @param renewalOfRef what it renews: expiring ARN, SBM number or legacy reference
   * @param accountOfficer account officer, null for the current user
   * @return request
   */
  public static NewAccount renewal(
      Long companyId,
      AccountOrigin origin,
      AccountDraft draft,
      String renewalOfRef,
      String accountOfficer) {
    return renewal(companyId, origin, draft, renewalOfRef, accountOfficer, null);
  }

  /**
   * A RENEWAL account that keeps a package version (renew as is, PQ11): rated with the RENEWAL
   * purpose on {@code productVersionNo} while it is RELEASED or SUPERSEDED, else on the current
   * version (catalog {@code SchemeResolver}).
   *
   * @param companyId company
   * @param origin who renews: RENEWAL, EMPLOYEE_BENEFITS or SUBMITTED_POLICY
   * @param draft account data of the renewal term
   * @param renewalOfRef what it renews: expiring ARN, SBM number or legacy reference
   * @param accountOfficer account officer, null for the current user
   * @param productVersionNo package version of the expiring account, null for the current one
   * @return request
   */
  public static NewAccount renewal(
      Long companyId,
      AccountOrigin origin,
      AccountDraft draft,
      String renewalOfRef,
      String accountOfficer,
      Integer productVersionNo) {
    return new NewAccount(
        companyId,
        null,
        Origin.DIRECT,
        draft,
        null,
        accountOfficer,
        productVersionNo,
        null,
        new AccountClassification(BusinessType.RENEWAL, renewalOfRef, origin));
  }

  /**
   * The business type of the request.
   *
   * @return NEW_BUSINESS or RENEWAL
   */
  public BusinessType businessType() {
    return classification.businessType();
  }
}
