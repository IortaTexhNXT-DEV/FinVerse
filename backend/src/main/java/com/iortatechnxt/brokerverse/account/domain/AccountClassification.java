package com.iortatechnxt.brokerverse.account.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Business type, renewal link and origin of an account (shared work item BT0, V822; BRNB.097,
 * BRID-022.01, BRRN.033, BRIDSP-26/27). Set when the account is created and never changed: a
 * renewal with changes still takes the new-business path but stays a renewal.
 *
 * @param businessType NEW_BUSINESS or RENEWAL, required
 * @param renewalOfRef what a renewal renews (expiring ARN, SBM number or legacy reference); null
 *     for new business
 * @param origin how the account was created
 */
@Embeddable
public record AccountClassification(
    @Enumerated(EnumType.STRING) @Column(name = "business_type", nullable = false, length = 20)
        BusinessType businessType,
    @Column(name = "renewal_of_ref", length = 40) String renewalOfRef,
    @Enumerated(EnumType.STRING) @Column(name = "origin", nullable = false, length = 30)
        AccountOrigin origin) {

  /** Defaults (new business, created directly) and the renewal-link rule. */
  public AccountClassification {
    businessType = businessType == null ? BusinessType.NEW_BUSINESS : businessType;
    origin = origin == null ? AccountOrigin.DIRECT : origin;
    renewalOfRef = renewalOfRef == null || renewalOfRef.isBlank() ? null : renewalOfRef.strip();
    if (businessType == BusinessType.NEW_BUSINESS && renewalOfRef != null) {
      throw new BusinessRuleException(
          "ACCOUNT_RENEWAL_OF_NOT_RENEWAL",
          "Only a renewal account names the policy it renews (" + renewalOfRef + ")");
    }
  }

  /**
   * New business of an origin.
   *
   * @param origin origin
   * @return classification
   */
  public static AccountClassification newBusiness(AccountOrigin origin) {
    return new AccountClassification(BusinessType.NEW_BUSINESS, null, origin);
  }

  /**
   * Whether the account is a renewal.
   *
   * @return true for RENEWAL
   */
  public boolean renewal() {
    return businessType == BusinessType.RENEWAL;
  }
}
