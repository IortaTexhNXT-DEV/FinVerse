package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.FreeFirstYear;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.account.service.AccountPricing.Terms;
import com.iortatechnxt.brokerverse.account.service.AccountRules.Resolved;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts (BRNB.025/029/051/053/054/102/111): creation with ARN, draft editing, pricing, and the
 * business actions of the NB_ACCOUNT workflow — submit / resubmit (Marketing), validate and direct
 * booking (Processing), TSU clearance. Generic actions (return, void) run from the workflow panel;
 * {@link AccountStatusListener} mirrors the stage. Later modules create accounts with {@link
 * #createDraft} and move them with {@link AccountLifecycleService}.
 */
@Service
@Transactional
public class AccountService {

  /** Entity type of accounts in the workflow, attachments and audit trail. */
  public static final String ENTITY = "Account";

  /** Workflow of accounts. */
  public static final String WORKFLOW = "NB_ACCOUNT";

  private static final Pattern ARN = Pattern.compile("ARN-\\d{4}-\\d{6}");
  private static final Set<AccountStatus> MARKETING_EDITABLE =
      EnumSet.of(AccountStatus.DRAFT, AccountStatus.RETURNED_TO_MARKETING);
  private static final Set<String> POLICY_DOCUMENTS = Set.of("POLICY_COPY", "EPOLICY");

  private final AccountRepository accounts;
  private final AccountRules rules;
  private final AccountChecks checks;
  private final AccountPricing pricing;
  private final SalesOrganisationService sales;
  private final ClientService clients;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts accounts
   * @param rules draft validation
   * @param checks completeness checks
   * @param pricing premium calculation
   * @param sales sales organisation
   * @param clients clients
   * @param workflow workflow engine
   * @param numbers document numbers (ARN)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccountService(
      AccountRepository accounts,
      AccountRules rules,
      AccountChecks checks,
      AccountPricing pricing,
      SalesOrganisationService sales,
      ClientService clients,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.rules = rules;
    this.checks = checks;
    this.pricing = pricing;
    this.sales = sales;
    this.clients = clients;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a draft account and opens its NB_ACCOUNT work case. Contract for the quotation and
   * proposal modules: pass the ARN generated at quotation / PRF creation and the premium already
   * computed; a direct account gets a new ARN and is rated now (BRNB.102).
   *
   * @param request company, ARN, origin, data, premium and account officer
   * @return the account in DRAFT
   */
  public Account createDraft(NewAccount request) {
    Resolved resolved = rules.resolve(request.companyId(), request.draft());
    String arn = arnFor(request.arn());
    String officer =
        request.accountOfficer() != null ? request.accountOfficer() : currentUser.username();
    SalesStamp stamp =
        sales
            .assignmentOf(request.companyId(), officer)
            .map(a -> new SalesStamp(a.region(), a.department(), a.team(), officer, a.costCenter()))
            .orElse(new SalesStamp(null, null, null, officer, null));
    Account account =
        Account.create(request.companyId(), arn, request.origin(), resolved.data(), stamp);
    applyTags(account, request.draft(), true);
    pricing.price(account, resolved.product(), terms(request.draft()), request.premium());
    checks.rejectDuplicates(account);
    Account saved = accounts.save(account);
    workflow.start(
        new StartCase(
            saved.getCompanyId(),
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(saved.getId()),
                arn,
                title(saved),
                "/accounts/" + saved.getId(),
                stamp.team() != null ? stamp.team() : saved.getMarketSegment()),
            null));
    audit.record(
        ENTITY,
        arn,
        AuditAction.CREATE,
        "Account for " + saved.getClientName() + ", product " + saved.getProductCode());
    return saved;
  }

  private String arnFor(String given) {
    if (given == null || given.isBlank()) {
      return numbers.next("ARN-" + LocalDate.now(clock).getYear());
    }
    if (!ARN.matcher(given).matches()) {
      throw new BusinessRuleException("ARN_INVALID", "The ARN must read ARN-yyyy-nnnnnn");
    }
    if (accounts.existsByArn(given)) {
      throw new BusinessRuleException("ARN_IN_USE", "An account already has ARN " + given);
    }
    return given;
  }

  /**
   * Changes a draft or returned account (Marketing), or a submitted one (Processing, BRNB.054).
   *
   * @param id account
   * @param draft new data
   * @return the account
   */
  public Account update(Long id, AccountDraft draft) {
    Account account = get(id);
    requireEditable(account);
    Resolved resolved = rules.resolve(account.getCompanyId(), draft);
    account.apply(resolved.data());
    applyTags(account, draft, account.getStatus() == AccountStatus.DRAFT);
    pricing.price(account, resolved.product(), terms(draft), null);
    checks.rejectDuplicates(account);
    workflow.describe(ENTITY, String.valueOf(id), account.getArn(), title(account));
    audit.record(ENTITY, account.getArn(), AuditAction.UPDATE, "Account data updated");
    return account;
  }

  private void requireEditable(Account account) {
    boolean marketing = MARKETING_EDITABLE.contains(account.getStatus());
    boolean processing =
        account.getStatus() == AccountStatus.SUBMITTED
            && currentUser.hasAuthority("ACCOUNT_PROCESS");
    if (!marketing && !processing) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_EDITABLE", "Account " + account.getArn() + " is " + account.getStatus());
    }
  }

  private void applyTags(Account account, AccountDraft draft, boolean mayClearFfy) {
    PaymentArrangement arrangement = account.getPaymentArrangement();
    PaymentArrangement wanted =
        draft.paymentArrangement() == null
            ? PaymentArrangement.VIA_BDOI
            : draft.paymentArrangement();
    if (wanted != arrangement) {
      account.setPaymentArrangement(wanted, currentUser.username(), clock.instant());
      audit.record(ENTITY, account.getArn(), AuditAction.UPDATE, "Payment arrangement " + wanted);
    }
    FreeFirstYear ffy = account.getFreeFirstYear();
    if (draft.ffyStart() != null && !draft.ffyStart().equals(ffy.start())) {
      account.setFreeFirstYear(FreeFirstYear.startingOn(draft.ffyStart()));
    } else if (draft.ffyStart() == null && ffy.active() && mayClearFfy) {
      account.setFreeFirstYear(FreeFirstYear.NONE);
    }
  }

  /**
   * Submits a draft to Processing (Marketing): minimum fields, mandatory documents, premium and
   * duplicate fall-out are checked first.
   *
   * @param id account
   * @param comment comment
   * @return the account
   */
  public Account submit(Long id, String comment) {
    return send(id, "submit", comment);
  }

  /**
   * Resubmits an account returned by Processing (Marketing, BRNB.033).
   *
   * @param id account
   * @param comment comment
   * @return the account
   */
  public Account resubmit(Long id, String comment) {
    return send(id, "resubmit", comment);
  }

  private Account send(Long id, String action, String comment) {
    Account account = get(id);
    checks.requireComplete(account);
    checks.applyTsu(account);
    workflow.transition(ENTITY, String.valueOf(id), action, TransitionNote.comment(comment));
    return account;
  }

  /**
   * Validates a submitted account (Processing): the client must be confirmed (BRNB.029), the data
   * complete and TSU clearance given when a routing rule requires it (BRNB.098). A direct-payment
   * account skips the payment gate (BRNB.114).
   *
   * @param id account
   * @param comment comment
   * @return the account
   */
  public Account validate(Long id, String comment) {
    Account account = get(id);
    clients.requireConfirmed(account.getClientId());
    checks.requireComplete(account);
    TsuDecision decision = checks.applyTsu(account);
    if (!account.getTsu().satisfied()) {
      throw new BusinessRuleException(
          "TSU_CLEARANCE_REQUIRED", "TSU must clear the account first: " + decision.reason());
    }
    String key = String.valueOf(id);
    workflow.transition(ENTITY, key, "validate", TransitionNote.comment(comment));
    if (account.getPaymentArrangement() == PaymentArrangement.DIRECT_TO_INSURER) {
      account.recordPayment(PaymentStatus.DIRECT, "Direct payment to insurer", clock.instant());
      workflow.systemTransition(
          ENTITY,
          key,
          "payment_confirmed",
          TransitionNote.comment("Paid directly to the insurer: payment gate not applicable"));
    }
    return account;
  }

  /**
   * Records the TSU clearance of an account (TSU, BRNB.098).
   *
   * @param id account
   * @param comment comment
   * @return the account
   */
  public Account clearTsu(Long id, String comment) {
    Account account = get(id);
    if (!MARKETING_EDITABLE.contains(account.getStatus())
        && account.getStatus() != AccountStatus.SUBMITTED) {
      throw new BusinessRuleException(
          "TSU_CLEARANCE_CLOSED", "TSU clearance is given before the account is validated");
    }
    account.clearTsu(currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        account.getArn(),
        AuditAction.AUTHORIZE,
        "TSU clearance" + (comment == null || comment.isBlank() ? "" : ": " + comment));
    return account;
  }

  /**
   * Books directly an account whose policy the insurer has already issued (BRNB.111): the issued
   * policy (policy copy or e-policy) must be attached; placement is bypassed.
   *
   * @param id account
   * @param comment comment
   * @return the account
   */
  public Account directBooking(Long id, String comment) {
    Account account = get(id);
    clients.requireConfirmed(account.getClientId());
    if (checks.documentTypes(account).stream().noneMatch(POLICY_DOCUMENTS::contains)) {
      throw new BusinessRuleException(
          "POLICY_DOCUMENT_REQUIRED",
          "Attach the issued policy (policy copy or e-policy) before direct booking");
    }
    account.markDirectBooking();
    workflow.transition(
        ENTITY,
        String.valueOf(id),
        "direct_booking",
        TransitionNote.comment(
            "Policy already issued by the insurer" + (comment == null ? "" : ": " + comment)));
    return account;
  }

  /**
   * One account.
   *
   * @param id id
   * @return account
   */
  @Transactional(readOnly = true)
  public Account get(Long id) {
    return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private static Terms terms(AccountDraft draft) {
    return new Terms(draft.ratingBasis(), draft.commissionRate());
  }

  private static String title(Account account) {
    return account.getClientName() + " - " + account.getProductCode();
  }
}
