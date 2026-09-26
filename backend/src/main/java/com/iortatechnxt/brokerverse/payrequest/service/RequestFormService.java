package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.payrequest.domain.CancellationTarget;
import com.iortatechnxt.brokerverse.payrequest.domain.Payee;
import com.iortatechnxt.brokerverse.payrequest.domain.PayeeType;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestContent;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts.CashAdvance;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts.CheckCancellation;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts.Refund;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raising and changing requests (MKT 1.2.0, 1.10.0, 1.19.0, 2.23.0): the Refund Request Form with
 * its lines, the Request for Payment of a cash advance and the cancellation of a disbursed check.
 * Each new request is numbered ({@code RRF-}, {@code RFP-}, {@code CCR-<yyyy>}) and opens its work
 * case; it may be changed while in DRAFT, PREPARING or REQUESTED.
 */
@Service
@Transactional
public class RequestFormService {

  private static final List<RequestStage> ENDED =
      List.of(RequestStage.CANCELLED, RequestStage.DISBURSED, RequestStage.SENT);

  private static final Set<String> CHECK_MODES =
      Set.of(Payee.CHECK, "MANAGERS_CHECK", "DEMAND_DRAFT");

  private final PaymentRequestRepository requests;
  private final PayRequestRules rules;
  private final LovService lovs;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param rules form checks
   * @param lovs lists of values
   * @param workflow workflow engine
   * @param numbers request numbers
   * @param audit audit trail
   * @param clock clock
   */
  public RequestFormService(
      PaymentRequestRepository requests,
      PayRequestRules rules,
      LovService lovs,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.rules = rules;
    this.lovs = lovs;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Raises a Refund Request Form (MKT 1.10.0): single or multiple accounts of one client.
   *
   * @param companyId company
   * @param draft form
   * @return the draft request
   */
  public PaymentRequest createRefund(Long companyId, Refund draft) {
    List<RefundLine> lines = rules.refundLines(companyId, draft.lines(), null);
    PaymentRequest request =
        open(companyId, rules.branchOf(companyId, draft.lines()), RequestKind.REFUND);
    fillRefund(request, draft, lines);
    return start(requests.save(request));
  }

  /**
   * Changes a refund request while it is being prepared.
   *
   * @param id request
   * @param draft form
   * @return the request
   */
  public PaymentRequest updateRefund(Long id, Refund draft) {
    PaymentRequest request = editable(id, RequestKind.REFUND);
    List<RefundLine> lines = rules.refundLines(request.getCompanyId(), draft.lines(), id);
    request.clearLines();
    requests.flush();
    fillRefund(request, draft, lines);
    return changed(request);
  }

  /**
   * Raises a Request for Payment of an employee cash advance (MKT 1.10.0).
   *
   * @param companyId company
   * @param draft form
   * @return the draft request
   */
  public PaymentRequest createCashAdvance(Long companyId, CashAdvance draft) {
    PaymentRequest request =
        open(companyId, rules.branchOf(companyId, List.of()), RequestKind.CASH_ADVANCE);
    fillCashAdvance(request, draft);
    return start(requests.save(request));
  }

  /**
   * Changes a cash-advance request while it is a draft.
   *
   * @param id request
   * @param draft form
   * @return the request
   */
  public PaymentRequest updateCashAdvance(Long id, CashAdvance draft) {
    PaymentRequest request = editable(id, RequestKind.CASH_ADVANCE);
    fillCashAdvance(request, draft);
    return changed(request);
  }

  /**
   * Requests the cancellation of a disbursed check (MKT 1.19.0): the paid request must have a DV,
   * and only one cancellation of it may be live at a time.
   *
   * @param companyId company
   * @param draft target, check number and reason
   * @return the request
   */
  public PaymentRequest createCheckCancellation(Long companyId, CheckCancellation draft) {
    PaymentRequest paid =
        requests
            .findByRequestNo(draft.targetRequestNo() == null ? "" : draft.targetRequestNo().strip())
            .filter(r -> r.getCompanyId().equals(companyId) && r.getKind().pays())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PRQ_TARGET_UNKNOWN",
                        "Request " + draft.targetRequestNo() + " is not a refund or cash advance"));
    if (!CHECK_MODES.contains(paid.getPayee().mode())) {
      throw new BusinessRuleException(
          "PRQ_TARGET_NOT_CHECK", paid.getRequestNo() + " was not paid by check");
    }
    String dvNo = paid.trackOrNone().dvNo();
    if (dvNo == null) {
      throw new BusinessRuleException(
          "PRQ_TARGET_NOT_DISBURSED", paid.getRequestNo() + " has no disbursement voucher yet");
    }
    if (requests.countLiveCancellations(paid.getRequestNo(), ENDED) > 0) {
      throw new BusinessRuleException(
          "PRQ_CANCELLATION_PENDING",
          "A cancellation of the check of " + paid.getRequestNo() + " is already in progress");
    }
    lovs.requireValid("DISB_CANCEL_REASON", draft.reasonCode(), LocalDate.now(clock));
    PaymentRequest request = open(companyId, paid.getBranchId(), RequestKind.CHECK_CANCELLATION);
    RequestContent content =
        rules.content(
            new RequestContent(
                paid.getContent().segment(),
                paid.getRequestNo(),
                paid.getContent().requestingUnit(),
                null,
                draft.remarks(),
                paid.getContent().currency()),
            "Cancellation of the check of " + paid.getRequestNo());
    request.fill(content, paid.getPayee(), paid.getAmount());
    request.aimAt(
        new CancellationTarget(
            paid.getRequestNo(),
            dvNo,
            draft.checkNo() == null ? null : draft.checkNo().strip(),
            draft.reasonCode()));
    return start(requests.save(request));
  }

  private void fillRefund(PaymentRequest request, Refund draft, List<RefundLine> lines) {
    Payee payee = rules.refundPayee(draft.payout(), draft.lines());
    RequestContent content = rules.content(draft.content(), "Refund to client");
    BigDecimal total =
        draft.lines().stream()
            .map(RefundLineValues::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    request.fill(content, payee, total.setScale(2));
    request.replaceLines(lines, lines.stream().anyMatch(RefundLine::isCancelledPolicy));
  }

  private void fillCashAdvance(PaymentRequest request, CashAdvance draft) {
    Payee payee =
        rules.payee(PayeeType.EMPLOYEE, draft.employeeNo(), draft.employeeName(), draft.payout());
    RequestContent given = draft.content();
    if (given.purpose() == null || given.purpose().isBlank()) {
      throw new BusinessRuleException(
          "PRQ_PURPOSE_REQUIRED", "Give the purpose of the cash advance (Appendix D RFP)");
    }
    RequestContent content =
        rules.content(
            new RequestContent(
                given.segment(),
                given.referenceText(),
                given.requestingUnit(),
                given.rfpType() == null ? "CASH_ADVANCE" : given.rfpType(),
                given.purpose(),
                given.currency()),
            "Cash advance");
    request.fill(content, payee, rules.amount(draft.amount()));
  }

  private PaymentRequest open(Long companyId, Long branchId, RequestKind kind) {
    LocalDate today = LocalDate.now(clock);
    String number = numbers.next(kind.prefix() + "-" + today.getYear());
    return new PaymentRequest(companyId, branchId, number, kind, today);
  }

  private PaymentRequest start(PaymentRequest saved) {
    workflow.start(
        new StartCase(
            saved.getCompanyId(),
            saved.getKind().workflow(),
            new CaseRecord(
                PayRequests.ENTITY,
                String.valueOf(saved.getId()),
                saved.getRequestNo(),
                title(saved),
                PayRequests.link(saved.getId()),
                saved.getContent().segment()),
            null));
    audit.record(
        PayRequests.ENTITY,
        saved.getRequestNo(),
        AuditAction.CREATE,
        saved.getKind() + " request of " + saved.getAmount().toPlainString());
    return saved;
  }

  private PaymentRequest changed(PaymentRequest request) {
    workflow.describe(
        PayRequests.ENTITY,
        String.valueOf(request.getId()),
        request.getRequestNo(),
        title(request));
    audit.record(PayRequests.ENTITY, request.getRequestNo(), AuditAction.UPDATE, "Form changed");
    return request;
  }

  private PaymentRequest editable(Long id, RequestKind kind) {
    PaymentRequest request =
        requests
            .findLoaded(id)
            .filter(r -> r.getKind() == kind)
            .orElseThrow(() -> new ResourceNotFoundException(PayRequests.ENTITY, id));
    if (!request.getStage().isEditable()) {
      throw new BusinessRuleException(
          PayRequests.WRONG_STAGE,
          request.getRequestNo() + " can no longer be changed (" + request.getStage() + ")");
    }
    return request;
  }

  private static String title(PaymentRequest r) {
    return r.getPayee().name()
        + " - "
        + r.getContent().currency()
        + " "
        + r.getAmount().toPlainString();
  }
}
