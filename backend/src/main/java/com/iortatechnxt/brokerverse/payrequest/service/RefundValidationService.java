package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RefundValidationCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.RefundValidations;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.Source;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.Status;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.ValidationRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.ValidationTicket;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundValidation;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundValidationRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.domain.ValidationStatus;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validation of refunds of cancelled policies (MKT 1.11.0, ACSL 2.5.5): opens one ACSL and one
 * Cashiering task per cancelled account through {@code RefundValidations} (the Operations router of
 * {@code RefundValidationSource}), collects the answers ({@code RefundValidationCompleted}, or
 * entered by hand for a handed-over task) and moves the request on: to review when every task of
 * the round is confirmed, back to the preparer when one is rejected.
 */
@Service
@Transactional
public class RefundValidationService {

  private static final List<String> VALIDATORS =
      List.of(RefundValidationSource.ACSL, RefundValidationSource.CASHIERING);

  private final RefundValidationRepository validations;
  private final PaymentRequestRepository requests;
  private final RefundValidations router;
  private final WorkflowService workflow;
  private final PayRequestNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param validations validation tasks
   * @param requests requests
   * @param router Operations router of the validators
   * @param workflow workflow engine
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RefundValidationService(
      RefundValidationRepository validations,
      PaymentRequestRepository requests,
      RefundValidations router,
      WorkflowService workflow,
      PayRequestNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.validations = validations;
    this.requests = requests;
    this.router = router;
    this.workflow = workflow;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Opens a new round of validation tasks for the cancelled accounts of a refund.
   *
   * @param request refund request
   * @return the tasks opened
   */
  public List<RefundValidation> open(PaymentRequest request) {
    int round = request.nextValidationRound();
    List<RefundValidation> opened = new ArrayList<>();
    for (RefundLine line : request.getLines()) {
      if (line.isCancelledPolicy()) {
        for (String validator : VALIDATORS) {
          opened.add(openOne(request, line, round, validator));
        }
      }
    }
    return opened;
  }

  private RefundValidation openOne(
      PaymentRequest request, RefundLine line, int round, String validator) {
    String ref = request.getRequestNo() + "#" + round + ":" + line.getLineNo();
    RefundValidation task =
        validations.save(
            new RefundValidation(request.getId(), round, line.getLineNo(), validator, ref));
    ValidationTicket ticket =
        router.open(
            new ValidationRequest(
                request.getCompanyId(),
                validator,
                line.getInvoiceNo() == null ? line.getRootInvoiceNo() : line.getInvoiceNo(),
                line.getArNo(),
                line.getClientCode(),
                request.getContent().currency(),
                line.getAmount(),
                new Source(
                    PayRequests.MODULE,
                    ref,
                    currentUser.username(),
                    request.getContent().purpose())));
    task.opened(ticket.status() == Status.DEFERRED, ticket.reference(), ticket.message());
    return task;
  }

  /**
   * The answer of a validating module (acsl, later cashiering), in its transaction.
   *
   * @param event result
   */
  @EventListener
  public void on(RefundValidationCompleted event) {
    if (PayRequests.MODULE.equals(event.sourceModule())) {
      validations
          .findByValidatorAndSourceRef(event.validator(), event.sourceRef())
          .filter(v -> v.getStatus().isPending())
          .ifPresent(v -> complete(v, event.confirmed(), event.newArNo(), event.remarks()));
    }
  }

  /**
   * Enters the result of a handed-over task by hand (the validator's module is not installed).
   *
   * @param requestId request
   * @param validationId task
   * @param confirmed true when confirmed
   * @param newArNo new AR number from Cashiering, may be null
   * @param remarks remarks
   * @return the task
   */
  public RefundValidation record(
      Long requestId, Long validationId, boolean confirmed, String newArNo, String remarks) {
    RefundValidation task =
        validations
            .findById(validationId)
            .filter(v -> v.getRequestId().equals(requestId))
            .orElseThrow(() -> new ResourceNotFoundException("RefundValidation", validationId));
    if (!task.getStatus().isPending()) {
      throw new BusinessRuleException(
          "PRQ_VALIDATION_DONE", "This validation was already " + task.getStatus());
    }
    complete(task, confirmed, blankToNull(newArNo), blankToNull(remarks));
    return task;
  }

  /**
   * Validation tasks of a request, newest round first.
   *
   * @param requestId request
   * @return tasks
   */
  @Transactional(readOnly = true)
  public List<RefundValidation> of(Long requestId) {
    return validations.findByRequestIdOrderByRoundNoDescLineNoAscValidatorAsc(requestId);
  }

  private void complete(RefundValidation task, boolean confirmed, String newArNo, String remarks) {
    task.complete(confirmed, newArNo, remarks, currentUser.username(), clock.instant());
    PaymentRequest request =
        requests
            .findLoaded(task.getRequestId())
            .orElseThrow(
                () -> new ResourceNotFoundException(PayRequests.ENTITY, task.getRequestId()));
    audit.record(
        PayRequests.ENTITY,
        request.getRequestNo(),
        AuditAction.UPDATE,
        task.getValidator()
            + " validation "
            + task.getStatus()
            + " (line "
            + task.getLineNo()
            + ")");
    if (request.getStage() == RequestStage.FOR_VALIDATION
        && task.getRoundNo() == request.getValidationRound()) {
      decide(request, task);
    }
  }

  private void decide(PaymentRequest request, RefundValidation last) {
    List<RefundValidation> round =
        validations.findByRequestIdAndRoundNo(request.getId(), request.getValidationRound());
    String id = String.valueOf(request.getId());
    if (last.getStatus() == ValidationStatus.REJECTED) {
      workflow.systemTransition(
          PayRequests.ENTITY,
          id,
          "validation_failed",
          PayRequests.note(last.getValidator() + " rejected: " + text(last.getRemarks())));
      notifier.requester(request, "validation rejected by " + last.getValidator());
    } else if (round.stream().allMatch(v -> v.getStatus() == ValidationStatus.CONFIRMED)) {
      workflow.systemTransition(
          PayRequests.ENTITY, id, "validated", PayRequests.note("ACSL and Cashiering confirmed"));
      notifier.team("PRQ_REVIEW", request, "validated, for review");
    }
  }

  private static String text(String value) {
    return value == null ? "no remarks" : value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
