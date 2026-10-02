package com.iortatechnxt.brokerverse.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account.Origin;
import com.iortatechnxt.brokerverse.account.domain.AccountClassification;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import org.junit.jupiter.api.Test;

/** Business type rules of shared work item BT0 (plain JUnit). */
class AccountClassificationTest {

  @Test
  void defaultsToNewBusinessCreatedDirectly() {
    AccountClassification c = new AccountClassification(null, " ", null);
    assertThat(c.businessType()).isEqualTo(BusinessType.NEW_BUSINESS);
    assertThat(c.origin()).isEqualTo(AccountOrigin.DIRECT);
    assertThat(c.renewalOfRef()).isNull();
    assertThat(c.renewal()).isFalse();
  }

  @Test
  void onlyARenewalNamesWhatItRenews() {
    assertThatThrownBy(
            () -> new AccountClassification(BusinessType.NEW_BUSINESS, "ARN-2025-000001", null))
        .extracting("code")
        .isEqualTo("ACCOUNT_RENEWAL_OF_NOT_RENEWAL");
    AccountClassification renewal =
        new AccountClassification(
            BusinessType.RENEWAL, " SBM-2026-000009 ", AccountOrigin.SUBMITTED_POLICY);
    assertThat(renewal.renewalOfRef()).isEqualTo("SBM-2026-000009");
    assertThat(renewal.renewal()).isTrue();
  }

  @Test
  void theOriginKindFollowsTheReferences() {
    assertThat(AccountOrigin.of(new Origin("QT-2026-000001", null)))
        .isEqualTo(AccountOrigin.QUOTATION);
    assertThat(AccountOrigin.of(new Origin(null, "PRF-2026-000001")))
        .isEqualTo(AccountOrigin.PROPOSAL);
    assertThat(AccountOrigin.of(Origin.DIRECT)).isEqualTo(AccountOrigin.DIRECT);
  }

  @Test
  void existingRequestsStayNewBusinessAndRenewalRequestsAreRenewals() {
    NewAccount quotation =
        new NewAccount(1L, "ARN-2026-000001", new Origin("QT-2026-000001", null), null, null, null);
    assertThat(quotation.businessType()).isEqualTo(BusinessType.NEW_BUSINESS);
    assertThat(quotation.classification().origin()).isEqualTo(AccountOrigin.QUOTATION);
    assertThat(NewAccount.direct(1L, null).businessType()).isEqualTo(BusinessType.NEW_BUSINESS);
    NewAccount eb = NewAccount.newBusiness(1L, AccountOrigin.EMPLOYEE_BENEFITS, null, null, "ao");
    assertThat(eb.classification().origin()).isEqualTo(AccountOrigin.EMPLOYEE_BENEFITS);
    NewAccount renewal =
        NewAccount.renewal(1L, AccountOrigin.RENEWAL, null, "ARN-2025-000001", null, 3);
    assertThat(renewal.businessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(renewal.productVersionNo()).isEqualTo(3);
    assertThat(renewal.classification().renewalOfRef()).isEqualTo("ARN-2025-000001");
  }
}
