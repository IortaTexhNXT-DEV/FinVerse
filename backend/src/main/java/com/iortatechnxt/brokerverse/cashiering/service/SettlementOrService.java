package com.iortatechnxt.brokerverse.cashiering.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.SettlementOr;
import com.iortatechnxt.brokerverse.cashiering.domain.SettlementOrRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.SettlementReceiptIssued;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.IssuedReceipt;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.ReceiptRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.Status;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The commission and incentive ORs of insurer settlements kept until Disbursement approves the
 * payment request (FRS.CSH.07.01.01; Appendix R, C12): the request is kept when the remittance
 * batch is approved, the OR is issued when Disbursement reports the voucher approved (or the
 * payment released), the source module is told the OR number, a cancelled payment request cancels
 * the request, and an OR that failed is issued again from the list.
 */
@Service
public class SettlementOrService {

  static final String ENTITY = "SettlementOr";
  private static final String APPROVED = "APPROVED";

  private final SettlementOrRepository ors;
  private final CashReceiptService receipts;
  private final ObjectMapper json;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final Clock clock;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param ors kept requests
   * @param receipts OR issuance
   * @param json JSON (request kept)
   * @param events OR issued event
   * @param audit audit trail
   * @param clock clock
   * @param txManager one transaction per OR issued
   */
  public SettlementOrService(
      SettlementOrRepository ors,
      CashReceiptService receipts,
      ObjectMapper json,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      Clock clock,
      PlatformTransactionManager txManager) {
    this.ors = ors;
    this.receipts = receipts;
    this.json = json;
    this.events = events;
    this.audit = audit;
    this.clock = clock;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Keeps a request until Disbursement approves its payment request (idempotent on the source).
   *
   * @param request OR request
   * @return deferral, or the OR already issued for the source
   */
  @Transactional
  public IssuedReceipt keep(ReceiptRequest request) {
    SettlementOr or =
        ors.findBySourceModuleAndSourceRef(request.source().module(), request.source().reference())
            .orElseGet(
                () ->
                    new SettlementOr(
                        request.companyId(),
                        request.source().module(),
                        request.source().reference(),
                        request.orType(),
                        request.currency()));
    if (SettlementOr.ISSUED.equals(or.getStatus())) {
      return new IssuedReceipt(
          Status.ISSUED, or.getReceiptNo(), null, "Official receipt " + or.getReceiptNo());
    }
    or.await(
        request.source().awaitRef(),
        new String[] {request.payee().partyCode(), request.payee().name()},
        request.gross(),
        write(request));
    SettlementOr saved = ors.save(or);
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        request.orType() + " OR of " + saved.getSourceRef() + " waits for " + saved.getAwaitRef());
    return new IssuedReceipt(
        Status.DEFERRED,
        null,
        null,
        "The OR is issued once Disbursement approves payment request " + saved.getAwaitRef());
  }

  /**
   * An OR of a source was issued at once: a kept request of the source is closed.
   *
   * @param request request
   * @param receiptNo OR number
   */
  @Transactional
  public void issuedAtOnce(ReceiptRequest request, String receiptNo) {
    ors.findBySourceModuleAndSourceRef(request.source().module(), request.source().reference())
        .filter(o -> !SettlementOr.ISSUED.equals(o.getStatus()))
        .ifPresent(o -> o.issued(receiptNo, clock.instant()));
  }

  /**
   * A payment request changed status in Disbursement: the kept ORs are issued once the voucher is
   * approved or the payment released, and cancelled with the payment request.
   *
   * @param event status change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(DisbursementStatusChanged event) {
    boolean approved =
        APPROVED.equals(event.dvStatus()) || event.status() == DisbursementRequest.Status.PAID;
    boolean cancelled = event.status() == DisbursementRequest.Status.CANCELLED;
    if (!approved && !cancelled || event.sourceRef() == null) {
      return;
    }
    List<Long> waiting =
        ors
            .findByCompanyIdAndSourceModuleAndAwaitRefAndStatusOrderByIdAsc(
                event.companyId(), event.sourceModule(), event.sourceRef(), SettlementOr.PENDING)
            .stream()
            .map(SettlementOr::getId)
            .toList();
    for (Long id : waiting) {
      if (approved) {
        issue(id);
      } else {
        tx.executeWithoutResult(
            s -> {
              SettlementOr or = get(id);
              or.cancelled(event.reason());
              audit.record(ENTITY, id, AuditAction.UPDATE, or.getMessage());
            });
      }
    }
  }

  /**
   * Issues again an OR that failed.
   *
   * @param id kept request
   * @return the request
   */
  public SettlementOr issueAgain(Long id) {
    if (!SettlementOr.FAILED.equals(get(id).getStatus())) {
      throw new BusinessRuleException(
          "SETTLEMENT_OR_NOT_FAILED", "Only an OR that could not be issued is issued again");
    }
    issue(id);
    return get(id);
  }

  private void issue(Long id) {
    try {
      tx.executeWithoutResult(s -> issueNow(get(id)));
    } catch (BusinessRuleException | IllegalStateException ex) {
      tx.executeWithoutResult(
          s -> {
            SettlementOr or = get(id);
            or.failed("The OR could not be issued: " + ex.getMessage());
            audit.record(ENTITY, id, AuditAction.UPDATE, or.getMessage());
          });
    }
  }

  private void issueNow(SettlementOr or) {
    ReceiptRequest request = read(or.getRequestJson());
    Receipt receipt =
        receipts.issueOr(CashieringReceiptIssuer.orIssue(request, BusinessClock.today(clock)));
    or.issued(receipt.getReceiptNo(), clock.instant());
    audit.record(
        ENTITY, or.getId(), AuditAction.UPDATE, "OR " + receipt.getReceiptNo() + " issued");
    events.publishEvent(
        new SettlementReceiptIssued(
            or.getCompanyId(),
            or.getSourceModule(),
            or.getSourceRef(),
            receipt.getReceiptNo(),
            receipt.getJournalBatchNo()));
  }

  /**
   * A kept request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public SettlementOr get(Long id) {
    return ors.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * The kept requests of a company, latest first.
   *
   * @param companyId company
   * @param pageable page
   * @return page
   */
  @Transactional(readOnly = true)
  public Page<SettlementOr> list(Long companyId, Pageable pageable) {
    return ors.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  private ReceiptRequest read(String request) {
    try {
      return json.readValue(request, ReceiptRequest.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("The OR request kept cannot be read", ex);
    }
  }

  private String write(ReceiptRequest request) {
    try {
      return json.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("The OR request cannot be kept", ex);
    }
  }
}
