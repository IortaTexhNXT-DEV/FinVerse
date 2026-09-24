package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.ClaimsFeed;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Origin;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceRef;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittanceRepository;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Decision;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Special remittance requests (MKTID.009, RMTID.030/033, OPS_SPECIAL_REMIT): Marketing asks to
 * remit one invoice outside the schedule for a condition (claims, renewal, installment due,
 * immediate OR). The system validates the request at once (invoice unprocessed or partially
 * remitted, paid AR applied, check holding period passed, not on hold); the approver (four eyes)
 * sends it to Process Remittance as its own SPECIAL batch, and the request follows the batch until
 * it is pushed to Disbursement. The claims condition is confirmed through the {@link ClaimsFeed}
 * port when the Claims system is connected (parked, OQ46).
 */
@Service
@Transactional
public class SpecialRemittanceService {

  /** Entity type in the workflow. */
  public static final String ENTITY = "SpecialRemittance";

  /** Claims feed code (OQ46). */
  public static final String CLAIMS_FEED = "CLAIMS_SPECIAL_REMIT";

  private static final String WORKFLOW = "OPS_SPECIAL_REMIT";
  private static final String CONDITION_LOV = "SPECIAL_REMIT_CONDITION";
  private static final String CLAIMS = "CLAIMS";
  private static final Set<SpecialStage> LIVE =
      Set.of(SpecialStage.REQUESTED, SpecialStage.FOR_APPROVAL, SpecialStage.IN_PROCESS_REMITTANCE);
  private static final Set<RemittanceStatus> ALLOWED =
      Set.of(RemittanceStatus.UNPROCESSED, RemittanceStatus.PARTIALLY_REMITTED);

  private final SpecialRemittanceRepository requests;
  private final InvoiceLedgerQueryService ledger;
  private final ExtractionWork checks;
  private final ExtractionService extraction;
  private final ObjectProvider<ClaimsFeed> claims;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param ledger ledger reads
   * @param checks eligibility rules
   * @param extraction special batches
   * @param claims Claims system port, when connected
   * @param workflow OPS_SPECIAL_REMIT workflow
   * @param numbers request numbers
   * @param lovs conditions and reasons
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public SpecialRemittanceService(
      SpecialRemittanceRepository requests,
      InvoiceLedgerQueryService ledger,
      ExtractionWork checks,
      ExtractionService extraction,
      ObjectProvider<ClaimsFeed> claims,
      WorkflowService workflow,
      DocumentNumberService numbers,
      LovService lovs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.ledger = ledger;
    this.checks = checks;
    this.extraction = extraction;
    this.claims = claims;
    this.workflow = workflow;
    this.numbers = numbers;
    this.lovs = lovs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * A request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public SpecialRemittance get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Requests a special remittance and validates it at once (MKTID.009).
   *
   * @param companyId company
   * @param request invoice, condition and remarks
   * @param source screen or Collection feed
   * @return the request, for approval
   */
  public SpecialRemittance request(Long companyId, NewRequest request, RequestSource source) {
    LocalDate today = LocalDate.now(clock);
    lovs.requireValid(CONDITION_LOV, request.conditionCode(), today);
    OpsInvoice invoice = ledger.require(request.invoiceNo().strip());
    requests
        .findFirstByInvoiceInvoiceNoAndStageIn(invoice.getInvoiceNo(), LIVE)
        .ifPresent(
            r -> {
              throw new BusinessRuleException(
                  "SPECIAL_REMIT_DUPLICATE",
                  "Invoice " + invoice.getInvoiceNo() + " already has request " + r.getRequestNo());
            });
    String note = validate(invoice, request.conditionCode(), today);
    SpecialRemittance saved =
        requests.save(
            new SpecialRemittance(
                companyId,
                numbers.next("SPR-" + today.getYear()),
                new InvoiceRef(
                    invoice.getInvoiceNo(),
                    invoice.getArn(),
                    invoice.getInsurerCode(),
                    invoice.getClientCode(),
                    invoice.getAssuredName()),
                new SpecialRemittance.Details(
                    invoice.getPolicyNo(),
                    invoice.getClassification().segment(),
                    request.conditionCode(),
                    request.remarks()),
                new Origin(source, currentUser.username())));
    saved.validated(note);
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                saved.getId().toString(),
                saved.getRequestNo(),
                invoice.getInvoiceNo() + " - " + invoice.getAssuredName(),
                "/remittance/special/" + saved.getId(),
                invoice.getClassification().segment()),
            null));
    workflow.systemTransition(
        ENTITY, saved.getId().toString(), "validate", TransitionNote.comment(note));
    audit.record(
        ENTITY, saved.getRequestNo(), AuditAction.CREATE, request.conditionCode() + ": " + note);
    notifications.notifyPermission(
        "SPECIAL_REMIT_APPROVE",
        notice(saved, "Special remittance " + saved.getRequestNo() + " for approval"),
        "REMIT_BATCH_FOR_APPROVAL");
    return saved;
  }

  private String validate(OpsInvoice invoice, String condition, LocalDate today) {
    if (!ALLOWED.contains(invoice.getRemittanceStatus())) {
      throw notEligible(invoice, "remittance status is " + invoice.getRemittanceStatus());
    }
    Decision decision = checks.check(invoice, today);
    if (decision.tag() != ExtractionTag.EXTRACTED) {
      throw notEligible(
          invoice,
          decision.reasons().isEmpty()
              ? decision.remarks()
              : String.join(", ", decision.reasons()));
    }
    String note = "Validated: paid AR " + decision.remittable() + " applied and cleared";
    return CLAIMS.equals(condition) ? note + claimsNote(invoice) : note;
  }

  private String claimsNote(OpsInvoice invoice) {
    ClaimsFeed feed = claims.getIfAvailable();
    if (feed == null) {
      return "; claims confirmation not connected (OQ46)";
    }
    boolean confirmed =
        feed.fetch(invoice.getCompanyId(), CLAIMS_FEED, invoice.getBookingDate()).stream()
            .anyMatch(item -> invoice.getInvoiceNo().equals(item.key()));
    if (!confirmed) {
      throw notEligible(invoice, "the Claims system has no claim for it");
    }
    return "; claim confirmed by the Claims system";
  }

  private static BusinessRuleException notEligible(OpsInvoice invoice, String why) {
    return new BusinessRuleException(
        "SPECIAL_REMIT_NOT_ELIGIBLE",
        "Invoice " + invoice.getInvoiceNo() + " cannot be remitted specially: " + why);
  }

  /**
   * Approves the request (SPECIAL_REMIT_APPROVE, four eyes) and creates its SPECIAL batch in
   * Process Remittance (MKTID.009).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public SpecialRemittance approve(Long id, String comment) {
    SpecialRemittance request = get(id);
    request.approved(currentUser.username(), clock.instant());
    workflow.transition(ENTITY, id.toString(), "approve", TransitionNote.comment(comment));
    RemittanceBatch batch =
        extraction.special(
            request.getCompanyId(),
            request.getInvoiceNo(),
            request.getRequestNo(),
            LocalDate.now(clock));
    request.inBatch(batch.getBatchNo());
    audit.record(
        ENTITY, request.getRequestNo(), AuditAction.AUTHORIZE, "Batch " + batch.getBatchNo());
    notifications.notifyPermission(
        ExtractionService.PROCESSORS,
        notice(request, "Special remittance batch " + batch.getBatchNo()),
        "REMIT_EXTRACTION_DONE");
    return request;
  }

  /**
   * Rejects the request with a reason.
   *
   * @param id request
   * @param reasonCode reason (LOV REMIT_RETURN_REASON)
   * @param comment comment
   * @return the request
   */
  public SpecialRemittance reject(Long id, String reasonCode, String comment) {
    SpecialRemittance request = get(id);
    workflow.transition(ENTITY, id.toString(), "reject", new TransitionNote(reasonCode, comment));
    request.rejected(reasonCode + (comment == null ? "" : " - " + comment));
    audit.record(ENTITY, request.getRequestNo(), AuditAction.REJECT, reasonCode);
    return request;
  }

  /**
   * The special batch was approved and pushed to Disbursement.
   *
   * @param requestNo request
   */
  public void batchApproved(String requestNo) {
    requests
        .findByRequestNo(requestNo)
        .ifPresent(
            r -> {
              workflow.systemTransition(
                  ENTITY, r.getId().toString(), "pushed", TransitionNote.comment(r.getBatchNo()));
              r.pushed(clock.instant());
            });
  }

  /**
   * The special batch was returned.
   *
   * @param requestNo request
   */
  public void batchReturned(String requestNo) {
    requests
        .findByRequestNo(requestNo)
        .ifPresent(
            r ->
                workflow.systemTransition(
                    ENTITY,
                    r.getId().toString(),
                    "returned",
                    TransitionNote.comment(r.getBatchNo())));
  }

  /**
   * Requests of an invoice (invoice 360).
   *
   * @param invoiceNo invoice
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public List<SpecialRemittance> ofInvoice(String invoiceNo) {
    return requests.findByInvoiceInvoiceNoOrderByIdDesc(invoiceNo);
  }

  private static Notice notice(SpecialRemittance r, String title) {
    return new Notice(
        title,
        r.getInvoiceNo() + " - " + r.getAssuredName() + " (" + r.getConditionCode() + ")",
        "/remittance/special/" + r.getId(),
        ENTITY,
        r.getId().toString());
  }

  /**
   * A new request.
   *
   * @param invoiceNo invoice
   * @param conditionCode condition (LOV SPECIAL_REMIT_CONDITION)
   * @param remarks remarks
   */
  public record NewRequest(String invoiceNo, String conditionCode, String remarks) {}
}
