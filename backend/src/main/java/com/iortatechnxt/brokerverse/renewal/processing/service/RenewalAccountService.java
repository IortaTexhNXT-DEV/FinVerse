package com.iortatechnxt.brokerverse.renewal.processing.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The renewal account (FR-RN-063; RENEWAL_DESIGN section 13): when a For Renewal candidate reaches
 * processing, BIBS creates the draft BRD-1 account of business type RENEWAL with the client,
 * product, items, insurer, PN and mortgage, and contact of the expiring account, the next period
 * with the same term, a link to the expiring ARN and, for a renew-as-is, the package version of the
 * expiring account. A migrated policy renews on the package its legacy package resolved to.
 */
@Service
@Transactional
public class RenewalAccountService {

  private static final String PHP = "PHP";
  private static final SnapshotProduct NO_PRODUCT =
      new SnapshotProduct(null, null, null, null, null, null);
  private static final SnapshotPremium NO_PREMIUM =
      new SnapshotPremium(null, null, null, null, null, null);

  private final RenewalRecords records;
  private final AccountService accounts;
  private final AccountQueryService accountQueries;
  private final AccountRepository accountRepository;
  private final ReevaluationService reevaluation;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param accounts account creation
   * @param accountQueries account reads (draft of the expiring account)
   * @param accountRepository accounts (look-up)
   * @param reevaluation checks
   * @param audit audit trail
   */
  public RenewalAccountService(
      RenewalRecords records,
      AccountService accounts,
      AccountQueryService accountQueries,
      AccountRepository accountRepository,
      ReevaluationService reevaluation,
      AuditTrailService audit) {
    this.records = records;
    this.accounts = accounts;
    this.accountQueries = accountQueries;
    this.accountRepository = accountRepository;
    this.reevaluation = reevaluation;
    this.audit = audit;
  }

  /**
   * Creates the renewal account of a renewal in processing, unless it has one.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return the renewal account
   */
  public Account create(Long companyId, String renewalRef) {
    return create(records.get(companyId, renewalRef));
  }

  /**
   * Creates the renewal account of a renewal already loaded, unless it has one.
   *
   * @param c renewal
   * @return the renewal account
   */
  public Account create(RenewalCandidate c) {
    if (c.getRenewalArn() != null) {
      return accountRepository
          .findByArn(c.getRenewalArn())
          .orElseThrow(
              () ->
                  new BusinessRuleException(
                      "RNW_ACCOUNT_MISSING",
                      "Renewal account " + c.getRenewalArn() + " not found"));
    }
    RenewalRecords.requireStage(c, RenewalStage.FOR_PROCESSING, RenewalStage.IN_PROCESSING);
    AccountDraft draft = draft(c);
    String renewalOf = c.getExpiringArn() != null ? c.getExpiringArn() : c.getSourceRef();
    Integer version =
        c.getResolvedVersionNo() != null ? c.getResolvedVersionNo() : c.getSnapshot().versionNo();
    String officer = c.getAssignedAo() != null ? c.getAssignedAo() : aoOf(c);
    Account account =
        accounts.createDraft(
            NewAccount.renewal(
                c.getCompanyId(), AccountOrigin.RENEWAL, draft, renewalOf, officer, version));
    c.linkRenewalAccount(account.getArn());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "Renewal account " + account.getArn() + " created from " + renewalOf);
    reevaluation.reevaluate(c, CheckTrigger.EVENT);
    return account;
  }

  /**
   * The renewal account of a renewal, when created.
   *
   * @param c renewal
   * @return account
   */
  @Transactional(readOnly = true)
  public Optional<Account> of(RenewalCandidate c) {
    return c.getRenewalArn() == null
        ? Optional.empty()
        : accountRepository.findByArn(c.getRenewalArn());
  }

  private AccountDraft draft(RenewalCandidate c) {
    if (c.getExpiringArn() != null) {
      AccountDraft d = accountQueries.draftOf(c.getExpiringArn());
      LocalDate from = d.periodTo();
      Period term = Period.between(d.periodFrom(), d.periodTo());
      String product =
          c.getResolvedProductCode() != null ? c.getResolvedProductCode() : d.productCode();
      return new AccountDraft(
          d.clientId(),
          product,
          d.marketSegment(),
          d.sourceChannel(),
          d.insurerCode(),
          d.insurerBranch(),
          from,
          from.plus(term),
          false,
          1,
          d.currency(),
          d.paymentArrangement(),
          d.mortgage(),
          d.contact(),
          d.items(),
          d.ratingBasis(),
          d.commissionRate(),
          null);
    }
    return legacyDraft(c);
  }

  private static AccountDraft legacyDraft(RenewalCandidate c) {
    CandidateSnapshot s = c.getSnapshot();
    Long clientId = s.client() == null ? null : s.client().clientId();
    SnapshotProduct product = s.product() == null ? NO_PRODUCT : s.product();
    SnapshotPremium premium = s.premium() == null ? NO_PREMIUM : s.premium();
    String productCode =
        c.getResolvedProductCode() != null ? c.getResolvedProductCode() : product.productCode();
    if (clientId == null || productCode == null) {
      throw new BusinessRuleException(
          "RNW_ACCOUNT_DATA",
          "Renewal "
              + c.getRenewalRef()
              + " needs a BIBS client and package before its renewal account is created");
    }
    LocalDate from = s.expiryDate();
    return new AccountDraft(
        clientId,
        productCode,
        product.segment(),
        null,
        s.insurerCode(),
        null,
        from,
        from.plusYears(1),
        false,
        1,
        premium.currency() == null ? PHP : premium.currency(),
        PaymentArrangement.VIA_BDOI,
        null,
        null,
        List.of(),
        null,
        premium.commissionRate(),
        null);
  }

  private static String aoOf(RenewalCandidate c) {
    return c.getSnapshot().sales() == null ? null : c.getSnapshot().sales().accountOfficer();
  }
}
