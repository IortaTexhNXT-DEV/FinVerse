package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contract of the account lifecycle for the placement, issuance and booking modules. Each method
 * performs the NB_ACCOUNT transition and updates the account in one transaction (the caller's).
 *
 * <p>The transition is a <b>user</b> action (permission of the transition checked, e.g.
 * PLACEMENT_MANAGE for {@code place}) when a user is signed in, and a <b>system</b> action (flagged
 * automatic in the status history) when none is, i.e. in managed jobs such as CLPC payment matching
 * or batch booking. Status changes publish {@link AccountStatusChanged}.
 */
@Service
@Transactional
public class AccountLifecycleService {

  private final AccountRepository accounts;
  private final ProductCatalogService catalog;
  private final DimensionService dimensions;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts accounts
   * @param catalog products (payment gate)
   * @param dimensions dimensions (cost center)
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccountLifecycleService(
      AccountRepository accounts,
      ProductCatalogService catalog,
      DimensionService dimensions,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.catalog = catalog;
    this.dimensions = dimensions;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Payment or client confirmation received (BRD 2.3.1): the account is ready for placement ({@code
   * payment_confirmed}). The status is PAID for products whose gate is payment (CBG Fire and Motor)
   * and CLIENT_CONFIRMED otherwise.
   *
   * @param arn account
   * @param source source (CLPC report, payment report, confirmation e-mail...)
   * @return the account
   */
  public Account markPaymentConfirmed(String arn, String source) {
    Account account = require(arn);
    PaymentGate gate = catalog.requireProduct(account.getProductCode()).getPaymentGate();
    PaymentStatus status =
        gate == PaymentGate.PAID ? PaymentStatus.PAID : PaymentStatus.CLIENT_CONFIRMED;
    move(account, "payment_confirmed", TransitionNote.comment(source));
    account.recordPayment(status, source, clock.instant());
    record(account, "Payment gate: " + status + " (" + source + ")");
    return account;
  }

  /**
   * Placement slip sent to the insurer ({@code place}); the insurer and branch are confirmed.
   *
   * @param arn account
   * @param slipRef placement slip number
   * @param insurerCode insurer party code
   * @param branchCode insurer branch
   * @return the account
   */
  public Account recordPlacement(
      String arn, String slipRef, String insurerCode, String branchCode) {
    Account account = require(arn);
    if (account.getInsurerCode() != null && !account.getInsurerCode().equals(insurerCode)) {
      throw new BusinessRuleException(
          "PLACEMENT_INSURER_MISMATCH",
          "Account " + arn + " is set up with insurer " + account.getInsurerCode());
    }
    move(account, "place", TransitionNote.comment("Placement slip " + slipRef));
    account.recordPlacement(slipRef, clock.instant());
    record(account, "Placed with " + insurerCode + "/" + branchCode + " on slip " + slipRef);
    return account;
  }

  /**
   * Placement returned by the insurer with remarks ({@code insurer_return}, BRNB.034).
   *
   * @param arn account
   * @param reasonCode reason (list RETURN_REASON)
   * @param comment insurer remarks
   * @return the account
   */
  public Account recordInsurerReturn(String arn, String reasonCode, String comment) {
    Account account = require(arn);
    move(account, "insurer_return", new TransitionNote(reasonCode, comment));
    return account;
  }

  /**
   * Resubmits a placement returned by the insurer ({@code resubmit} from RETURNED_BY_INSURER).
   *
   * @param arn account
   * @param comment comment
   * @return the account
   */
  public Account resubmitPlacement(String arn, String comment) {
    Account account = require(arn);
    requireStatus(account, AccountStatus.RETURNED_BY_INSURER);
    move(account, "resubmit", TransitionNote.comment(comment));
    return account;
  }

  /**
   * Cancels the placement before issuance ({@code cancel_placement}, BRNB.062/094).
   *
   * @param arn account
   * @param reasonCode reason (list CANCELLATION_REASON)
   * @param comment comment
   * @return the account
   */
  public Account cancelPlacement(String arn, String reasonCode, String comment) {
    Account account = require(arn);
    move(account, "cancel_placement", new TransitionNote(reasonCode, comment));
    return account;
  }

  /**
   * Reactivates a cancelled placement ({@code reactivate}, BRD 2.1.16).
   *
   * @param arn account
   * @param comment comment
   * @return the account
   */
  public Account reactivate(String arn, String comment) {
    Account account = require(arn);
    move(account, "reactivate", TransitionNote.comment(comment));
    return account;
  }

  /**
   * Records the hold cover requested from or confirmed by the insurer (BRNB.072/103); no status
   * change.
   *
   * @param arn account
   * @param status hold cover status
   * @param insurerRef insurer reference
   * @param date request or confirmation date
   * @return the account
   */
  public Account recordHoldCover(
      String arn, HoldCoverStatus status, String insurerRef, LocalDate date) {
    Account account = require(arn);
    account.recordHoldCover(status, insurerRef, date);
    record(account, "Hold cover " + status + (insurerRef == null ? "" : " ref " + insurerRef));
    return account;
  }

  /**
   * Records the issued policy number(s) and issue date ({@code policy_received}); a multi-year
   * account needs one policy number per year (BRNB.112). For an account booked directly the status
   * is already POLICY_ISSUED and only the numbers are recorded.
   *
   * @param arn account
   * @param policyNumbers policy numbers, one per policy year
   * @param issueDate issue date
   * @return the account
   */
  public Account recordPolicy(String arn, List<String> policyNumbers, LocalDate issueDate) {
    Account account = require(arn);
    List<String> numbers =
        policyNumbers == null
            ? List.of()
            : policyNumbers.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::strip)
                .toList();
    if (numbers.size() != account.getTermYears()) {
      throw new BusinessRuleException(
          "POLICY_NUMBERS_MISMATCH",
          "Account "
              + arn
              + " needs "
              + account.getTermYears()
              + " policy number(s), one per year");
    }
    boolean alreadyIssued =
        account.isDirectBooking() && account.getStatus() == AccountStatus.POLICY_ISSUED;
    if (!alreadyIssued) {
      move(
          account,
          "policy_received",
          TransitionNote.comment("Policy " + String.join(", ", numbers)));
    }
    account.recordPolicies(numbers, issueDate);
    record(account, "Policy " + String.join(", ", numbers) + " issued " + issueDate);
    return account;
  }

  /**
   * Records the booking ({@code book}, BRNB.027/107/108): booking reference and date, incentive
   * flag and cost center.
   *
   * @param arn account
   * @param bookingRef booking reference
   * @param date booking date
   * @param incentiveFlag incentive eligibility
   * @param costCenter cost center (dimension COST_CENTER)
   * @return the account
   */
  public Account recordBooking(
      String arn, String bookingRef, LocalDate date, boolean incentiveFlag, String costCenter) {
    Account account = require(arn);
    dimensions.validateOptional(account.getCompanyId(), DimensionType.COST_CENTER, costCenter);
    move(account, "book", TransitionNote.comment("Booking " + bookingRef));
    account.recordBooking(bookingRef, date, incentiveFlag);
    if (costCenter != null && !costCenter.isBlank()) {
      account.setCostCenter(costCenter);
    }
    record(account, "Booked " + bookingRef + " on " + date);
    return account;
  }

  /**
   * Records a cancellation after issuance ({@code cancel}, BRNB.094).
   *
   * @param arn account
   * @param reasonCode reason (list CANCELLATION_REASON)
   * @param date cancellation date
   * @return the account
   */
  public Account recordCancellation(String arn, String reasonCode, LocalDate date) {
    Account account = require(arn);
    move(account, "cancel", new TransitionNote(reasonCode, "Cancelled on " + date));
    account.recordCancellation(date, reasonCode);
    record(account, "Cancelled on " + date + " (" + reasonCode + ")");
    return account;
  }

  private void move(Account account, String action, TransitionNote note) {
    String id = String.valueOf(account.getId());
    if (currentUser.optionalUsername().isPresent()) {
      workflow.transition(AccountService.ENTITY, id, action, note);
    } else {
      workflow.systemTransition(AccountService.ENTITY, id, action, note);
    }
  }

  private static void requireStatus(Account account, AccountStatus status) {
    if (account.getStatus() != status) {
      throw new BusinessRuleException(
          "ACCOUNT_STATUS_INVALID",
          "Account " + account.getArn() + " is " + account.getStatus() + ", not " + status);
    }
  }

  private Account require(String arn) {
    return accounts
        .findByArn(arn)
        .orElseThrow(() -> new ResourceNotFoundException(AccountService.ENTITY, arn));
  }

  private void record(Account account, String summary) {
    audit.record(AccountService.ENTITY, account.getArn(), AuditAction.UPDATE, summary);
  }
}
