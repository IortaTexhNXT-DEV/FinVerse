package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalReferral;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The New Business account of an accepted transfer request (FRRN.011.03): the data of the renewal
 * account is copied, the receiving unit reviews and changes it, and the account is created in New
 * Business (Draft, its own workflow) and linked to the Transfer Request Number.
 */
@Service
@Transactional
public class ReferralNewBusiness {

  private final ReferralService referrals;
  private final RenewalRecords records;
  private final AccountQueryService accountQueries;
  private final AccountService accounts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param referrals transfer requests
   * @param records renewals
   * @param accountQueries data of the expiring account
   * @param accounts New Business accounts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReferralNewBusiness(
      ReferralService referrals,
      RenewalRecords records,
      AccountQueryService accountQueries,
      AccountService accounts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.referrals = referrals;
    this.records = records;
    this.accountQueries = accountQueries;
    this.accounts = accounts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The data copied to the New Business account, for the receiving unit to review and change.
   *
   * @param companyId company
   * @param id accepted request
   * @return the proposed account data
   */
  @Transactional(readOnly = true)
  public NewBusinessInput proposal(Long companyId, Long id) {
    RenewalReferral r = referrals.get(companyId, id);
    RenewalCandidate c = records.byId(r.getCandidateId());
    AccountDraft d = copied(c);
    return new NewBusinessInput(
        d.productCode(), d.marketSegment(), d.periodTo(), d.periodTo().plusYears(1), null, true);
  }

  /**
   * Creates the New Business account of an accepted request, with the copied data and the changes
   * of the receiving unit; the account is linked to the Transfer Request Number.
   *
   * @param companyId company
   * @param id accepted request
   * @param input product, segment, period and Account Officer as reviewed
   * @return the New Business account
   */
  public Account createNewBusiness(Long companyId, Long id, NewBusinessInput input) {
    RenewalReferral r = referrals.get(companyId, id);
    referrals.requireReceiver(r);
    RenewalCandidate c = records.byId(r.getCandidateId());
    AccountDraft d = copied(c);
    LocalDate from = input.periodFrom() == null ? d.periodTo() : input.periodFrom();
    LocalDate to = input.periodTo() == null ? from.plusYears(1) : input.periodTo();
    if (!to.isAfter(from)) {
      throw new BusinessRuleException("RNW_REFERRAL_PERIOD", "The period must end after it starts");
    }
    AccountDraft draft =
        new AccountDraft(
            d.clientId(),
            orElse(input.productCode(), d.productCode()),
            orElse(input.marketSegment(), d.marketSegment()),
            d.sourceChannel(),
            d.insurerCode(),
            d.insurerBranch(),
            from,
            to,
            false,
            1,
            d.currency(),
            d.paymentArrangement(),
            d.mortgage(),
            d.contact(),
            input.copyRiskItems() ? d.items() : List.of(),
            d.ratingBasis(),
            d.commissionRate(),
            null);
    String officer = orElse(input.accountOfficer(), currentUser.username());
    Account account =
        accounts.createDraft(
            NewAccount.newBusiness(companyId, AccountOrigin.DIRECT, draft, null, officer));
    r.linkNewBusiness(account.getArn(), currentUser.username(), clock.instant());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "New Business account " + account.getArn() + " of transfer request " + r.getReferralNo());
    return account;
  }

  private AccountDraft copied(RenewalCandidate c) {
    if (c.getExpiringArn() == null) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_NO_ACCOUNT",
          "Renewal account " + c.getRenewalRef() + " has no account to copy the data from");
    }
    return accountQueries.draftOf(c.getExpiringArn());
  }

  private static String orElse(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.strip();
  }

  /**
   * The New Business account data as the receiving unit reviewed it.
   *
   * @param productCode product (risk code)
   * @param marketSegment market segment
   * @param periodFrom start of the period
   * @param periodTo end of the period
   * @param accountOfficer Account Officer of the receiving unit, null for the current user
   * @param copyRiskItems true to copy the insured items of the renewal account
   */
  public record NewBusinessInput(
      String productCode,
      String marketSegment,
      LocalDate periodFrom,
      LocalDate periodTo,
      String accountOfficer,
      boolean copyRiskItems) {}
}
