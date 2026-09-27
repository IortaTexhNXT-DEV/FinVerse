package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovementRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChange;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChange.Change;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChangeRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceFlagChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceLocked;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceUnlocked;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RemittanceStatusChanged;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write contract of the Operations ledger for the Operations modules (OPERATIONS_DESIGN 2.1):
 * movements on invoice components (RMTID.038), flags, remittance status (RMTID.019/032) and locks
 * (RMTID.040). Every call locks the invoice row, runs in the caller's transaction, writes the
 * status history and the audit trail, and publishes an {@link OpsLedgerEvents} event.
 *
 * <p>Lock rule: while a module holds an invoice's lock, other modules may only post {@link
 * MovementType#APPLIED} movements (money received is always applied); any other movement, flag,
 * status or lock change by another module is refused with {@code INVOICE_LOCKED}.
 */
@Service
@Transactional
public class InvoiceLedgerService {

  /** Module name of the ledger itself (derived status changes). */
  public static final String MODULE = "OPSLEDGER";

  private static final String ENTITY = "OpsInvoice";
  private static final String REMITTANCE_STATUS = "REMITTANCE_STATUS";
  private static final String PAYMENT_STATUS = "PAYMENT_STATUS";

  private final OpsInvoiceRepository invoices;
  private final OpsInvoiceMovementRepository movements;
  private final OpsInvoiceStatusChangeRepository history;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param invoices invoices
   * @param movements movements
   * @param history status history
   * @param events event publisher
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public InvoiceLedgerService(
      OpsInvoiceRepository invoices,
      OpsInvoiceMovementRepository movements,
      OpsInvoiceStatusChangeRepository history,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.invoices = invoices;
    this.movements = movements;
    this.history = history;
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Posts the movements of one business transaction on an invoice. Idempotent on (source module,
   * source reference, invoice): a repeated request returns the movements already posted.
   *
   * @param request invoice, type, source and signed amounts per component
   * @return the movements of the transaction
   */
  public List<OpsInvoiceMovement> post(MovementRequest request) {
    OpsInvoice invoice = lockInvoice(request.invoiceNo());
    List<OpsInvoiceMovement> earlier =
        movements.findBySourceModuleAndSourceRefAndInvoiceIdOrderByIdAsc(
            request.sourceModule(), request.sourceRef(), invoice.getId());
    if (!earlier.isEmpty()) {
      return earlier;
    }
    if (request.amounts().isEmpty()) {
      throw new BusinessRuleException(
          "MOVEMENT_WITHOUT_AMOUNT", "A movement needs at least one amount other than zero");
    }
    if (request.type() == MovementType.BOOKED) {
      throw new BusinessRuleException(
          "MOVEMENT_TYPE_RESERVED", "BOOKED movements come from the booking feed only");
    }
    if (request.type() != MovementType.APPLIED) {
      invoice.requireNotLockedByOther(request.sourceModule());
    }
    return record(invoice, request);
  }

  /**
   * Records movements without the lock and type checks (the booking feed).
   *
   * @param invoice invoice (locked or new)
   * @param request movements
   * @return movements
   */
  List<OpsInvoiceMovement> record(OpsInvoice invoice, MovementRequest request) {
    PaymentStatus payment = invoice.getPaymentStatus();
    RemittanceStatus remittance = invoice.getRemittanceStatus();
    OpsInvoiceMovement.Source source = source(request);
    List<OpsInvoiceMovement> saved = new ArrayList<>();
    request
        .amounts()
        .forEach(
            (component, amount) -> {
              invoice.move(request.type(), component, amount);
              saved.add(
                  movements.save(
                      new OpsInvoiceMovement(
                          invoice.getId(),
                          new OpsInvoiceMovement.Entry(request.type(), component, amount),
                          source)));
            });
    statusChanged(invoice, PAYMENT_STATUS, payment, invoice.getPaymentStatus(), request);
    if (remittance != invoice.getRemittanceStatus()) {
      statusChanged(invoice, REMITTANCE_STATUS, remittance, invoice.getRemittanceStatus(), request);
      events.publishEvent(
          new RemittanceStatusChanged(
              invoice.getCompanyId(),
              invoice.getInvoiceNo(),
              remittance,
              invoice.getRemittanceStatus(),
              MODULE));
    }
    audit.record(
        ENTITY,
        invoice.getInvoiceNo(),
        AuditAction.UPDATE,
        request.type()
            + " "
            + request.amounts()
            + " by "
            + request.sourceModule()
            + " "
            + request.sourceRef());
    events.publishEvent(
        new InvoiceMovementPosted(
            invoice.getCompanyId(),
            invoice.getInvoiceNo(),
            request.type(),
            request.sourceModule(),
            request.sourceRef(),
            request.amounts()));
    return saved;
  }

  private OpsInvoiceMovement.Source source(MovementRequest request) {
    return new OpsInvoiceMovement.Source(
        request.sourceModule(),
        request.sourceRef(),
        request.valueDate(),
        request.refs().arNo(),
        request.refs().orNo(),
        request.refs().batchNo(),
        request.refs().journalBatchNo(),
        request.remarks(),
        clock.instant(),
        currentUser.username());
  }

  private void statusChanged(
      OpsInvoice invoice, String field, Enum<?> from, Enum<?> to, MovementRequest request) {
    if (from != to) {
      history.save(
          new OpsInvoiceStatusChange(
              invoice.getId(),
              new Change(field, from.name(), to.name()),
              MODULE,
              request.type() + " " + request.sourceModule() + " " + request.sourceRef(),
              currentUser.username(),
              clock.instant()));
    }
  }

  /**
   * Sets or clears a flag (hold, pending negative adjustment, written off, cancelled, estimated).
   *
   * @param change invoice, flag, value, module and reason
   * @return the invoice
   */
  public OpsInvoice setFlag(FlagChange change) {
    OpsInvoice invoice = lockInvoice(change.invoiceNo());
    invoice.requireNotLockedByOther(change.module());
    boolean previous = invoice.setFlag(change.flag(), change.value());
    if (previous != change.value()) {
      log(
          invoice,
          new Change(
              change.flag().name(), String.valueOf(previous), String.valueOf(change.value())),
          change.module(),
          change.reason());
      events.publishEvent(
          new InvoiceFlagChanged(
              invoice.getCompanyId(),
              invoice.getInvoiceNo(),
              change.flag(),
              change.value(),
              change.module(),
              change.reason()));
    }
    return invoice;
  }

  /**
   * Sets the remittance status (remittance module, RMTID.019).
   *
   * @param invoiceNo invoice
   * @param status new status
   * @param module module
   * @param reason reason, may be null
   * @return the invoice
   */
  public OpsInvoice setRemittanceStatus(
      String invoiceNo, RemittanceStatus status, String module, String reason) {
    OpsInvoice invoice = lockInvoice(invoiceNo);
    invoice.requireNotLockedByOther(module);
    RemittanceStatus previous = invoice.changeRemittanceStatus(status);
    if (previous != invoice.getRemittanceStatus()) {
      log(
          invoice,
          new Change(REMITTANCE_STATUS, previous.name(), invoice.getRemittanceStatus().name()),
          module,
          reason);
      events.publishEvent(
          new RemittanceStatusChanged(
              invoice.getCompanyId(), invoiceNo, previous, invoice.getRemittanceStatus(), module));
    }
    return invoice;
  }

  /**
   * Locks an invoice for a module (RMTID.040): during remittance, while Comptrollership works on it
   * (OQ28), during an adjustment posting. Locking again by the same module refreshes the reason.
   *
   * @param invoiceNo invoice
   * @param owner module taking the lock
   * @param reason reason
   * @return the invoice
   */
  public OpsInvoice lock(String invoiceNo, String owner, String reason) {
    OpsInvoice invoice = lockInvoice(invoiceNo);
    String previous = invoice.getLockOwner();
    invoice.lock(owner, reason, currentUser.username(), clock.instant());
    if (previous == null) {
      log(invoice, new Change("LOCK", null, owner), owner, reason);
      events.publishEvent(new InvoiceLocked(invoice.getCompanyId(), invoiceNo, owner, reason));
    }
    return invoice;
  }

  /**
   * Releases a module's lock; nothing happens when the invoice is not locked.
   *
   * @param invoiceNo invoice
   * @param owner module holding the lock
   * @param reason reason, may be null
   * @return the invoice
   */
  public OpsInvoice unlock(String invoiceNo, String owner, String reason) {
    OpsInvoice invoice = lockInvoice(invoiceNo);
    if (invoice.unlock(owner)) {
      log(invoice, new Change("LOCK", owner, null), owner, reason);
      events.publishEvent(new InvoiceUnlocked(invoice.getCompanyId(), invoiceNo, owner));
    }
    return invoice;
  }

  /**
   * Refuses a change while another module holds the invoice's lock (RMTID.040, ADJID.001).
   *
   * @param invoiceNo invoice
   * @param module module that wants to change it
   */
  @Transactional(readOnly = true)
  public void requireUnlocked(String invoiceNo, String module) {
    invoices
        .findByInvoiceNo(invoiceNo)
        .orElseThrow(() -> notFound(invoiceNo))
        .requireNotLockedByOther(module);
  }

  private void log(OpsInvoice invoice, Change change, String module, String reason) {
    history.save(
        new OpsInvoiceStatusChange(
            invoice.getId(), change, module, reason, currentUser.username(), clock.instant()));
    audit.record(
        ENTITY,
        invoice.getInvoiceNo(),
        AuditAction.UPDATE,
        change.field()
            + " "
            + change.from()
            + " -> "
            + change.to()
            + " by "
            + module
            + (reason == null ? "" : ": " + reason));
  }

  private OpsInvoice lockInvoice(String invoiceNo) {
    return invoices.lockByInvoiceNo(invoiceNo).orElseThrow(() -> notFound(invoiceNo));
  }

  private static ResourceNotFoundException notFound(String invoiceNo) {
    return new ResourceNotFoundException("Operations invoice", invoiceNo);
  }

  /**
   * A flag change.
   *
   * @param invoiceNo invoice
   * @param flag flag
   * @param value new value
   * @param module module changing it
   * @param reason reason, may be null
   */
  public record FlagChange(
      String invoiceNo, InvoiceFlag flag, boolean value, String module, String reason) {}
}
