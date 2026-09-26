package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest.RequestFacts;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeService.PayeeAuthorized;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Spec;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Status;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Type;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receives payment requests (DIS 2.5.0-2.6.2, 3.25.0-3.25.2): from modules through the gateway,
 * from upload rows and encoded by Disbursement users, numbered {@code DSR-<yyyy>-nnnnnn}. The payee
 * is matched to the master; a gateway refund or remittance (parameter {@code
 * DISB_AUTO_APPROVER_ROUTING}) or a request flagged straight-to-approval gets its voucher at once
 * in FOR_APPROVAL, an encoded request gets its voucher with the processor, the others wait in the
 * system requests list. A request whose payee is not maintained is held as NO_PAYEE with the alert
 * {@code DISB_PAYEE_NO_MATCH}, a payee request and a notice to the requestor, and continues when
 * the payee is authorised; with {@code DISB_NO_PAYEE_ACTION = RETURN} it is returned to its source
 * at once (AQ11, AQ12).
 */
@Service
@Transactional
public class RequestIntakeService {

  private static final String RETURN_LOV = "DISB_RETURN_REASON";
  private static final String NO_MATCH_ALERT = "DISB_PAYEE_NO_MATCH";
  private static final String STATUS_EVENT = "OPS_DISBURSEMENT_STATUS";

  private final IntakeRequestRepository requests;
  private final PayeeQueryService payees;
  private final PayeeService payeeService;
  private final VoucherFactory factory;
  private final GatewaySync gateway;
  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final AlertService alerts;
  private final NotificationService notifications;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests payment requests
   * @param payees payee reads
   * @param payeeService payee requests
   * @param factory voucher builder
   * @param gateway Operations request progress
   * @param numbers request numbers
   * @param parameters business parameters
   * @param alerts alerts
   * @param notifications notifications
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RequestIntakeService(
      IntakeRequestRepository requests,
      PayeeQueryService payees,
      PayeeService payeeService,
      VoucherFactory factory,
      GatewaySync gateway,
      DocumentNumberService numbers,
      SystemParameterService parameters,
      AlertService alerts,
      NotificationService notifications,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.payees = payees;
    this.payeeService = payeeService;
    this.factory = factory;
    this.gateway = gateway;
    this.numbers = numbers;
    this.parameters = parameters;
    this.alerts = alerts;
    this.notifications = notifications;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Receives a request of a module through the gateway (DIS 2.6.2, 3.25.0), once per source
   * transaction.
   *
   * @param companyId company
   * @param spec what to pay
   * @param gatewayRequestId linked Operations request
   * @return the request
   */
  public IntakeRequest receive(Long companyId, Spec spec, Long gatewayRequestId) {
    Optional<IntakeRequest> earlier =
        requests.findBySourceModuleAndSourceRef(spec.sourceModule(), spec.sourceRef());
    if (earlier.isPresent()) {
      return earlier.get();
    }
    RequestSource source =
        "PAYREQUEST".equals(spec.sourceModule()) ? RequestSource.PAYREQUEST : RequestSource.GATEWAY;
    RequestFacts facts =
        new RequestFacts(
            companyId,
            source,
            spec.sourceModule(),
            spec.sourceRef(),
            spec.rfpNo(),
            spec.disbursementType() != null ? spec.disbursementType() : spec.type().name(),
            spec.payeeClass(),
            spec.payeeCode(),
            spec.payeeName(),
            spec.currency(),
            spec.amount(),
            spec.description(),
            spec.rootInvoiceNo(),
            spec.attachmentRefs(),
            spec.accountingRefs(),
            spec.straightToApproval(),
            gatewayRequestId,
            null,
            null,
            null);
    return route(save(facts), false);
  }

  /**
   * Registers a request encoded from an e-mail (DIS 2.6.1) or read from an upload row (DIS 2.5.0).
   * An encoded request gets its voucher with the processor at once (manual template, DIS 2.7.5).
   *
   * @param facts what to pay
   * @return the request
   */
  public IntakeRequest register(RequestFacts facts) {
    if (!Money.isPositive(facts.amount())) {
      throw new BusinessRuleException("DISB_AMOUNT", "A payment request needs a positive amount");
    }
    lovs.requireValid("DISBURSEMENT_TYPE", facts.disbursementType(), LocalDate.now(clock));
    if (facts.sourceRef() != null
        && requests
            .findBySourceModuleAndSourceRef(facts.sourceModule(), facts.sourceRef())
            .isPresent()) {
      throw new BusinessRuleException(
          "DISB_REQUEST_DUPLICATE", "Request " + facts.sourceRef() + " was already received");
    }
    return route(save(facts), facts.source() == RequestSource.ENCODED);
  }

  private IntakeRequest save(RequestFacts facts) {
    IntakeRequest saved =
        requests.save(
            new IntakeRequest(
                numbers.next(DisbursementSettings.series("DSR", LocalDate.now(clock))),
                facts,
                clock.instant()));
    audit.record(
        DisbursementSettings.REQUEST,
        saved.getRequestNo(),
        AuditAction.CREATE,
        saved.getSource()
            + " "
            + saved.getDisbursementType()
            + " "
            + saved.getCurrency()
            + " "
            + saved.getAmount()
            + " to "
            + saved.getPayeeCode()
            + " from "
            + saved.getSourceModule()
            + " "
            + saved.getSourceRef());
    return saved;
  }

  private IntakeRequest route(IntakeRequest r, boolean voucherNow) {
    Optional<Payee> payee = payees.match(r.getCompanyId(), r.getPayeeCode(), r.getPayeeName());
    if (payee.isEmpty()) {
      noPayee(r);
      return r;
    }
    Payee p = payee.get();
    r.matched(p.getId(), p.getName(), p.getPayeeClass());
    if (Type.CWT2307.name().equals(r.getDisbursementType()) || handledOnQueue(r)) {
      return r;
    }
    boolean straight = automatic(r);
    if (straight || voucherNow) {
      factory.create(r, p, straight);
    }
    return r;
  }

  /**
   * Whether the Operations request moved on without Disbursement (DV number given on the Operations
   * queue screen, returned or cancelled there): no voucher is built for it.
   */
  private boolean handledOnQueue(IntakeRequest r) {
    Status status = gateway.status(r);
    return status != null && status != Status.SENT && status != Status.ACKNOWLEDGED;
  }

  private boolean automatic(IntakeRequest r) {
    if (r.getSource() != RequestSource.GATEWAY && r.getSource() != RequestSource.PAYREQUEST) {
      return false;
    }
    return r.isStraightToApproval()
        || parameters.items(DisbursementSettings.AUTO_ROUTING).contains(r.getDisbursementType());
  }

  private void noPayee(IntakeRequest r) {
    String reason = "Payee " + r.getPayeeCode() + " is not maintained in the payee master";
    r.noPayee(reason);
    alerts.raise(
        NO_MATCH_ALERT,
        new AlertFacts(
            r.getCompanyId(),
            null,
            DisbursementSettings.REQUEST,
            r.getRequestNo(),
            reason + " (" + r.getSourceModule() + " " + r.getSourceRef() + ")",
            r.getAmount(),
            NO_MATCH_ALERT + ":" + r.getRequestNo()));
    payeeService.request(
        new PayeeRequest(
            r.getCompanyId(),
            PayeeRequestSource.NO_MATCH,
            r.getPayeeCode(),
            r.getPayeeName() == null ? r.getPayeeCode() : r.getPayeeName(),
            r.getDisbursementType() + " " + r.getCurrency() + " " + r.getAmount(),
            r.getRequestNo()));
    Notice notice =
        new Notice(
            "Payment request " + r.getRequestNo() + ": payee not maintained",
            reason,
            "/disbursement/requests",
            DisbursementSettings.REQUEST,
            r.getRequestNo());
    notifications.notifyPermission(DisbursementSettings.PAYEE_MAINTAIN, notice);
    notifications.notifyUser(r.getCreatedBy(), notice, STATUS_EVENT);
    if ("RETURN".equals(noPayeeAction())) {
      r.returned(DisbursementSettings.PAYEE_NOT_MAINTAINED);
      gateway.returned(r, DisbursementSettings.PAYEE_NOT_MAINTAINED + ": " + reason);
    }
  }

  private String noPayeeAction() {
    return parameters
        .text(DisbursementSettings.NO_PAYEE_ACTION, "HOLD")
        .strip()
        .toUpperCase(Locale.ROOT);
  }

  /**
   * A payee became usable: the requests that waited for it continue (DIS 2.2.1, 3.25.0).
   *
   * @param event payee authorised
   */
  @EventListener
  public void on(PayeeAuthorized event) {
    for (IntakeRequest r :
        requests.findByCompanyIdAndPayeeCodeAndStatusOrderByIdAsc(
            event.companyId(), event.payeeCode(), RequestStatus.NO_PAYEE)) {
      route(r, false);
      audit.record(
          DisbursementSettings.REQUEST,
          r.getRequestNo(),
          AuditAction.UPDATE,
          "Payee " + event.payeeCode() + " maintained: request continues as " + r.getStatus());
    }
  }

  /**
   * Creates the voucher of a received request (processor, DIS 2.7.5).
   *
   * @param id request
   * @return the voucher's request
   */
  public IntakeRequest createVoucher(Long id) {
    IntakeRequest r = get(id);
    if (r.getStatus() != RequestStatus.RECEIVED && r.getStatus() != RequestStatus.NO_PAYEE) {
      throw new BusinessRuleException(
          "DISB_REQUEST_STATUS", "Request " + r.getRequestNo() + " is " + r.getStatus());
    }
    Payee payee =
        payees
            .match(r.getCompanyId(), r.getPayeeCode(), r.getPayeeName())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "DISB_PAYEE_NOT_MAINTAINED",
                        "Maintain payee " + r.getPayeeCode() + " before creating the voucher"));
    r.matched(payee.getId(), payee.getName(), payee.getPayeeClass());
    Voucher v = factory.create(r, payee, false);
    audit.record(
        DisbursementSettings.REQUEST, r.getRequestNo(), AuditAction.UPDATE, "DV " + v.getDvNo());
    return r;
  }

  /**
   * Returns a request without voucher to its source with a reason (DIS 3.25.0).
   *
   * @param id request
   * @param reasonCode reason (LOV {@code DISB_RETURN_REASON})
   * @param comment comment, may be null
   * @return the request
   */
  public IntakeRequest returnToSource(Long id, String reasonCode, String comment) {
    lovs.requireValid(RETURN_LOV, reasonCode, LocalDate.now(clock));
    IntakeRequest r = get(id);
    if (r.getVoucherId() != null) {
      throw new BusinessRuleException(
          "DISB_REQUEST_IN_VOUCHER", "Cancel or reject the voucher of " + r.getRequestNo());
    }
    String reason = comment == null || comment.isBlank() ? reasonCode : reasonCode + ": " + comment;
    r.returned(reason);
    gateway.returned(r, reason);
    audit.record(
        DisbursementSettings.REQUEST, r.getRequestNo(), AuditAction.REJECT, "Returned: " + reason);
    return r;
  }

  /**
   * Releases BIR 2307 certificates requested without a payment (DBMID.001): the source module
   * releases its batch.
   *
   * @param id request
   * @return the request
   */
  public IntakeRequest release(Long id) {
    IntakeRequest r = get(id);
    if (!Type.CWT2307.name().equals(r.getDisbursementType())) {
      throw new BusinessRuleException(
          "DISB_NOT_A_RELEASE", "Only BIR 2307 requests are released without a voucher");
    }
    r.released();
    gateway.released(r);
    audit.record(
        DisbursementSettings.REQUEST, r.getRequestNo(), AuditAction.UPDATE, "Documents released");
    return r;
  }

  /**
   * The request of a source transaction.
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return request
   */
  @Transactional(readOnly = true)
  public IntakeRequest byReference(String sourceModule, String sourceRef) {
    return requests
        .findBySourceModuleAndSourceRef(sourceModule, sourceRef)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.REQUEST, sourceRef));
  }

  /**
   * A request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public IntakeRequest get(Long id) {
    return requests
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.REQUEST, id));
  }
}
