package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee.PayeeDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount.AccountDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequestRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistoryRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payee maintenance (DIS 2.2.0-2.2.8, workflow {@code DISB_PAYEE}): a maker saves a draft, submits
 * it, and another user authorises it; an active payee is amended, deactivated and reactivated the
 * same way (four eyes). A payee never used by a voucher may be deleted (DIS 2.2.4). On
 * authorisation the open payee requests of the party code are closed and {@link PayeeAuthorized} is
 * published, so the payment requests that waited for the payee continue (DIS 3.25.0).
 */
@Service
@Transactional
public class PayeeService {

  private static final String PAYEE_CLASS = "PAYEE_CLASS";
  private static final String DISBURSEMENT_TYPE = "DISBURSEMENT_TYPE";

  private final PayeeRepository payees;
  private final PayeeRequestRepository requests;
  private final PayeeQueryService query;
  private final WorkflowService workflow;
  private final WorkCaseRepository cases;
  private final WorkCaseHistoryRepository history;
  private final LovService lovs;
  private final CurrencyService currencies;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param payees payees
   * @param requests payee requests
   * @param query payee reads
   * @param workflow payee workflow
   * @param cases work cases (deletion of an unused payee)
   * @param history work case history (deletion of an unused payee)
   * @param lovs lists of values
   * @param currencies currencies
   * @param events event publisher
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public PayeeService(
      PayeeRepository payees,
      PayeeRequestRepository requests,
      PayeeQueryService query,
      WorkflowService workflow,
      WorkCaseRepository cases,
      WorkCaseHistoryRepository history,
      LovService lovs,
      CurrencyService currencies,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.payees = payees;
    this.requests = requests;
    this.query = query;
    this.workflow = workflow;
    this.cases = cases;
    this.history = history;
    this.lovs = lovs;
    this.currencies = currencies;
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Saves a new payee as a draft (DIS 2.2.0, 2.2.2, 2.2.6-2.2.7).
   *
   * @param companyId company
   * @param payeeCode party code
   * @param details details
   * @param accounts bank accounts
   * @param source where it came from
   * @return the payee
   */
  public Payee create(
      Long companyId,
      String payeeCode,
      PayeeDetails details,
      List<AccountDetails> accounts,
      PayeeSource source) {
    String code = payeeCode.strip();
    if (query.byCode(companyId, code).isPresent()) {
      throw new DuplicateResourceException(DisbursementSettings.PAYEE, code);
    }
    validate(details);
    Payee payee = new Payee(companyId, code, source, null);
    payee.update(details);
    accounts.forEach(a -> payee.addAccount(new PayeeAccount(payee, a)));
    Payee saved = payees.save(payee);
    workflow.start(
        new StartCase(
            companyId,
            DisbursementSettings.WF_PAYEE,
            new CaseRecord(
                DisbursementSettings.PAYEE,
                saved.getId().toString(),
                code,
                details.name(),
                DisbursementSettings.payeeLink(saved.getId()),
                details.payeeClass()),
            null));
    audit.record(
        DisbursementSettings.PAYEE,
        code,
        AuditAction.CREATE,
        "Payee " + details.name() + " (" + details.payeeClass() + ") saved as draft");
    return saved;
  }

  /**
   * Changes the details of a draft, or of an active payee, which then goes back to the checker (DIS
   * 2.2.3).
   *
   * @param id payee
   * @param details details
   * @return the payee
   */
  public Payee update(Long id, PayeeDetails details) {
    Payee payee = query.get(id);
    validate(details);
    boolean active = payee.getStage() == PayeeStage.ACTIVE;
    if (!active && payee.getStage() != PayeeStage.DRAFT) {
      throw new BusinessRuleException(
          "PAYEE_NOT_EDITABLE", "Payee " + payee.getPayeeCode() + " is " + payee.getStage());
    }
    payee.update(details);
    workflow.describe(
        DisbursementSettings.PAYEE, id.toString(), payee.getPayeeCode(), details.name());
    if (active) {
      workflow.transition(DisbursementSettings.PAYEE, id.toString(), "amend", TransitionNote.NONE);
    }
    audit.record(
        DisbursementSettings.PAYEE, payee.getPayeeCode(), AuditAction.UPDATE, "Payee updated");
    return payee;
  }

  /**
   * Adds a bank account (DIS 2.2.0); an active payee goes back to the checker.
   *
   * @param id payee
   * @param account account
   * @return the payee
   */
  public Payee addAccount(Long id, AccountDetails account) {
    Payee payee = query.get(id);
    currencies.requireActive(account.currency());
    payee.addAccount(new PayeeAccount(payee, account));
    audit.record(
        DisbursementSettings.PAYEE,
        payee.getPayeeCode(),
        AuditAction.UPDATE,
        "Bank account " + PayeeAccount.mask(account.accountNo()) + " added");
    if (payee.getStage() == PayeeStage.ACTIVE) {
      workflow.transition(DisbursementSettings.PAYEE, id.toString(), "amend", TransitionNote.NONE);
    }
    return payee;
  }

  /**
   * Deactivates a bank account of a payee.
   *
   * @param id payee
   * @param accountId account
   * @return the payee
   */
  public Payee deactivateAccount(Long id, Long accountId) {
    Payee payee = query.get(id);
    PayeeAccount account =
        payee.getAccounts().stream()
            .filter(a -> a.getId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Payee account", accountId));
    account.deactivate();
    audit.record(
        DisbursementSettings.PAYEE,
        payee.getPayeeCode(),
        AuditAction.DEACTIVATE,
        "Bank account " + PayeeAccount.mask(account.getAccountNo()) + " deactivated");
    return payee;
  }

  /**
   * Submits a draft for authorisation.
   *
   * @param id payee
   * @return the payee
   */
  public Payee submit(Long id) {
    return move(id, "submit", "Submitted for authorisation");
  }

  /**
   * Authorises the pending change of a payee: the checker is not the maker (DIS 2.2.0).
   *
   * @param id payee
   * @return the payee
   */
  public Payee authorize(Long id) {
    Payee payee = query.get(id);
    String maker = payee.getUpdatedBy() != null ? payee.getUpdatedBy() : payee.getCreatedBy();
    if (CurrentUser.sameUser(maker, currentUser.username())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A payee cannot be authorised by the user who maintained it");
    }
    move(id, "authorize", "Authorised");
    if (payee.getStage().usable()) {
      closeRequests(payee);
      events.publishEvent(new PayeeAuthorized(payee.getCompanyId(), payee.getPayeeCode()));
    }
    return payee;
  }

  /**
   * Requests the deactivation of an active payee (DIS 2.2.4).
   *
   * @param id payee
   * @return the payee
   */
  public Payee deactivate(Long id) {
    return move(id, "deactivate", "Deactivation requested");
  }

  /**
   * Requests the reactivation of an inactive payee.
   *
   * @param id payee
   * @return the payee
   */
  public Payee reactivate(Long id) {
    return move(id, "reactivate", "Reactivation requested");
  }

  /**
   * Deletes a payee that was never authorised nor used (DIS 2.2.4); other payees are deactivated.
   *
   * @param id payee
   */
  public void delete(Long id) {
    Payee payee = query.get(id);
    if (payee.isUsed() || payee.getStage() != PayeeStage.DRAFT) {
      throw new BusinessRuleException(
          "PAYEE_IN_USE", "Only a draft payee never used can be deleted; deactivate it instead");
    }
    cases
        .findByEntityTypeAndEntityId(DisbursementSettings.PAYEE, id.toString())
        .ifPresent(
            c -> {
              history.deleteAll(history.findByCaseIdOrderByIdAsc(c.getId()));
              cases.delete(c);
            });
    payees.delete(payee);
    audit.record(
        DisbursementSettings.PAYEE, payee.getPayeeCode(), AuditAction.DEACTIVATE, "Deleted draft");
  }

  /**
   * Raises a payee maintenance request (DIS 2.2.1), once per source reference.
   *
   * @param request request
   * @return the open request
   */
  public PayeeRequest request(PayeeRequest request) {
    if (request.getSourceRef() != null) {
      var open =
          requests.findFirstByCompanyIdAndSourceRefAndStatus(
              request.getCompanyId(), request.getSourceRef(), PayeeRequestStatus.OPEN);
      if (open.isPresent()) {
        return open.get();
      }
    }
    PayeeRequest saved = requests.save(request);
    audit.record(
        DisbursementSettings.PAYEE,
        request.getPayeeCode() == null ? request.getPayeeName() : request.getPayeeCode(),
        AuditAction.CREATE,
        "Payee request " + request.getSource() + " for " + request.getPayeeName());
    return saved;
  }

  /**
   * Closes a payee request without a payee (cancelled) or as done.
   *
   * @param requestId request
   * @param outcome DONE or CANCELLED
   * @return the request
   */
  public PayeeRequest closeRequest(Long requestId, PayeeRequestStatus outcome) {
    PayeeRequest r =
        requests
            .findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Payee request", requestId));
    r.close(outcome, null, currentUser.username(), clock.instant());
    audit.record(
        DisbursementSettings.PAYEE,
        r.getPayeeName(),
        AuditAction.UPDATE,
        "Payee request " + outcome);
    return r;
  }

  private void closeRequests(Payee payee) {
    for (PayeeRequest r :
        requests.findByCompanyIdAndPayeeCodeAndStatus(
            payee.getCompanyId(), payee.getPayeeCode(), PayeeRequestStatus.OPEN)) {
      r.close(PayeeRequestStatus.DONE, payee.getId(), currentUser.username(), clock.instant());
    }
  }

  private Payee move(Long id, String action, String summary) {
    Payee payee = query.get(id);
    workflow.transition(DisbursementSettings.PAYEE, id.toString(), action, TransitionNote.NONE);
    audit.record(DisbursementSettings.PAYEE, payee.getPayeeCode(), AuditAction.UPDATE, summary);
    return payee;
  }

  private void validate(PayeeDetails details) {
    LocalDate today = LocalDate.now(clock);
    lovs.requireValid(PAYEE_CLASS, details.payeeClass(), today);
    details.disbursementTypes().forEach(t -> lovs.requireValid(DISBURSEMENT_TYPE, t, today));
    currencies.requireActive(details.currency());
    if (details.allowedModes().isEmpty()) {
      throw new BusinessRuleException("PAYEE_MODE", "Select at least one mode of payment");
    }
  }

  /**
   * A payee became usable: requests waiting for it continue.
   *
   * @param companyId company
   * @param payeeCode party code
   */
  public record PayeeAuthorized(Long companyId, String payeeCode) {}
}
