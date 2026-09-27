package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.LegacyInvoiceRef;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovementRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot.Line;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshotRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the open legacy invoices of the Data Migration (object F01; DATA_MIGRATION_DESIGN 14.1)
 * in the Operations ledger: origin MIGRATED, ledger context LEGACY, feed source MIGRATION. The
 * component balances are brought to the open position at cut-over by the movements BOOKED,
 * LEGACY_ADJUSTED, LEGACY_PAID, LEGACY_REMITTED and LEGACY_WRITTEN_OFF (or BOOKED = open balance in
 * open-balance mode), and the original values are frozen in the origin snapshot. The intake
 * publishes {@code OpsInvoiceBooked} with source MIGRATION, which the listeners that would notify
 * or reconcile production ignore. A rolled-back batch reverses the movements and cancels the
 * invoice; the year-end adjustments after go-live post LEGACY_ADJUSTED or LEGACY_WRITTEN_OFF.
 */
@Service
@Transactional
public class LegacyInvoiceIntake {

  /** Source module of the migration movements. */
  public static final String MODULE = "MIGRATION";

  private static final String ENTITY = "OpsInvoice";
  private static final String ROLLBACK_SUFFIX = ":RB";

  private final OpsInvoiceRepository invoices;
  private final OpsInvoiceMovementRepository movements;
  private final OpsInvoiceOriginSnapshotRepository snapshots;
  private final InvoiceLedgerService ledger;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the intake.
   *
   * @param invoices ledger invoices
   * @param movements movements
   * @param snapshots origin snapshots
   * @param ledger movement writer
   * @param audit audit trail
   * @param events event publisher
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the intake
  public LegacyInvoiceIntake(
      OpsInvoiceRepository invoices,
      OpsInvoiceMovementRepository movements,
      OpsInvoiceOriginSnapshotRepository snapshots,
      InvoiceLedgerService ledger,
      AuditTrailService audit,
      ApplicationEventPublisher events,
      CurrentUser currentUser,
      Clock clock) {
    this.invoices = invoices;
    this.movements = movements;
    this.snapshots = snapshots;
    this.ledger = ledger;
    this.audit = audit;
    this.events = events;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The ledger number of a legacy invoice: its legacy number when free, else the source system and
   * the legacy number (a number used by two source systems).
   *
   * @param legacyInvoiceNo number in the source system
   * @param sourceSystem source system
   * @param prefixOnCollision prefix the source system when the number is taken
   * @return invoice number
   */
  @Transactional(readOnly = true)
  public String numberFor(String legacyInvoiceNo, String sourceSystem, boolean prefixOnCollision) {
    if (!invoices.existsByInvoiceNo(legacyInvoiceNo)) {
      return legacyInvoiceNo;
    }
    String prefixed = sourceSystem + "-" + legacyInvoiceNo;
    if (!prefixOnCollision || invoices.existsByInvoiceNo(prefixed)) {
      throw new BusinessRuleException(
          "MIG_INVOICE_NO_TAKEN",
          "Invoice number " + legacyInvoiceNo + " is already in the ledger");
    }
    return prefixed;
  }

  /**
   * A legacy invoice by its number in the source system.
   *
   * @param legacyInvoiceNo legacy number
   * @param sourceSystem source system
   * @return the invoice, empty when not loaded
   */
  @Transactional(readOnly = true)
  public Optional<OpsInvoice> findByLegacyNo(String legacyInvoiceNo, String sourceSystem) {
    return invoices.findByLegacyLegacyInvoiceNoOrderByIdAsc(legacyInvoiceNo).stream()
        .filter(i -> sourceSystem.equals(i.getRecordOrigin().sourceSystem()))
        .findFirst();
  }

  /**
   * Records an open legacy invoice with its opening movements and snapshot.
   *
   * @param legacy the invoice
   * @return the ledger invoice
   */
  public OpsInvoice record(LegacyInvoice legacy) {
    for (Line line : legacy.positions()) {
      if (!legacy.header().openBalanceMode() && !line.consistent()) {
        throw new BusinessRuleException(
            "MIG_POSITION_INCONSISTENT",
            "Component "
                + line.component()
                + " of invoice "
                + legacy.legacyInvoiceNo()
                + ": booked + adjusted - paid - remitted - written off is not the open balance");
      }
    }
    OpsInvoice invoice =
        OpsInvoice.of(withRoot(legacy.data()), legacy.shares(), FeedSource.MIGRATION);
    invoice.markLegacy(legacy.origin(), LegacyInvoiceRef.legacy(legacy.legacyInvoiceNo()));
    invoice.setFlag(InvoiceFlag.HOLD, legacy.hold());
    invoice = invoices.save(invoice);
    post(invoice, legacy);
    snapshots.save(
        new OpsInvoiceOriginSnapshot(
            invoice, legacy.header(), legacy.positions(), currentUser.username(), clock.instant()));
    audit.record(
        ENTITY,
        invoice.getInvoiceNo(),
        AuditAction.CREATE,
        "Open legacy invoice "
            + legacy.legacyInvoiceNo()
            + " of "
            + legacy.origin().sourceSystem()
            + " (batch "
            + legacy.origin().migrationBatch()
            + ")");
    events.publishEvent(
        new OpsInvoiceBooked(
            invoice.getCompanyId(),
            invoice.getInvoiceNo(),
            invoice.getArn(),
            invoice.getKind(),
            invoice.getClientCode(),
            invoice.getInsurerCode(),
            invoice.getPolicyNo(),
            invoice.isDpFlag(),
            FeedSource.MIGRATION));
    return invoice;
  }

  private OpsInvoiceData withRoot(OpsInvoiceData d) {
    OpsInvoiceData.Keys k = d.keys();
    String root =
        k.parentInvoiceNo() == null
            ? k.invoiceNo()
            : invoices
                .findByInvoiceNo(k.parentInvoiceNo())
                .map(OpsInvoice::getRootInvoiceNo)
                .orElse(k.parentInvoiceNo());
    return new OpsInvoiceData(
        new OpsInvoiceData.Keys(
            k.companyId(),
            k.branchId(),
            k.invoiceNo(),
            k.arn(),
            k.accountId(),
            k.kind(),
            k.endorsementNo(),
            k.parentInvoiceNo(),
            root,
            k.policyNo(),
            k.policyYear()),
        d.parties(),
        d.classification(),
        d.amounts(),
        d.flags());
  }

  private void post(OpsInvoice invoice, LegacyInvoice legacy) {
    boolean openOnly = legacy.header().openBalanceMode();
    LocalDate date = legacy.header().invoiceDate();
    String ref = sourceRef(invoice);
    movement(
        invoice,
        MovementType.BOOKED,
        ref,
        date,
        legacy,
        openOnly ? Line::openBalance : Line::booked);
    if (openOnly) {
      return;
    }
    movement(invoice, MovementType.LEGACY_ADJUSTED, ref, date, legacy, Line::adjusted);
    movement(invoice, MovementType.LEGACY_PAID, ref, date, legacy, Line::paid);
    movement(invoice, MovementType.LEGACY_REMITTED, ref, date, legacy, Line::remitted);
    movement(invoice, MovementType.LEGACY_WRITTEN_OFF, ref, date, legacy, Line::writtenOff);
  }

  private void movement(
      OpsInvoice invoice,
      MovementType type,
      String ref,
      LocalDate date,
      LegacyInvoice legacy,
      Function<Line, BigDecimal> amount) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    legacy.positions().forEach(l -> amounts.merge(l.component(), amount.apply(l), BigDecimal::add));
    MovementRequest request =
        new MovementRequest(
            invoice.getInvoiceNo(),
            type,
            MODULE,
            ref,
            date,
            amounts,
            new DocumentRefs(null, null, legacy.origin().migrationBatch(), null),
            "Legacy position at cut-over");
    if (!request.amounts().isEmpty()) {
      ledger.record(invoice, request);
    }
  }

  private static String sourceRef(OpsInvoice invoice) {
    return "MIG:INV:" + invoice.getInvoiceNo();
  }

  /**
   * Whether a legacy invoice was worked in BIBS after its load (movements of other modules).
   *
   * @param invoiceId invoice id
   * @return true when another module posted on it
   */
  @Transactional(readOnly = true)
  public boolean workedAfterLoad(Long invoiceId) {
    return movements.findByInvoiceIdOrderByIdAsc(invoiceId).stream()
        .anyMatch(m -> !MODULE.equals(m.getSourceModule()));
  }

  /**
   * Undoes a legacy invoice of a rolled-back batch: the opening movements are reversed and the
   * invoice is cancelled. Refused when another module already worked on it.
   *
   * @param invoiceId invoice id
   * @param batchNo rolled-back batch
   * @return the invoice
   */
  public OpsInvoice rollback(Long invoiceId, String batchNo) {
    OpsInvoice invoice =
        invoices
            .findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Operations invoice", invoiceId));
    if (workedAfterLoad(invoiceId)) {
      throw new BusinessRuleException(
          "MIG_INVOICE_WORKED",
          "Invoice " + invoice.getInvoiceNo() + " was worked after its load and cannot be undone");
    }
    Map<MovementType, Map<LedgerComponent, BigDecimal>> reverse = new LinkedHashMap<>();
    for (OpsInvoiceMovement m : movements.findByInvoiceIdOrderByIdAsc(invoiceId)) {
      reverse
          .computeIfAbsent(m.getMovementType(), t -> new EnumMap<>(LedgerComponent.class))
          .merge(m.getComponent(), m.getAmount().negate(), BigDecimal::add);
    }
    String ref = sourceRef(invoice) + ROLLBACK_SUFFIX;
    reverse.forEach(
        (type, amounts) ->
            ledger.record(
                invoice,
                new MovementRequest(
                    invoice.getInvoiceNo(),
                    type,
                    MODULE,
                    ref,
                    invoice.getBookingDate(),
                    amounts,
                    new DocumentRefs(null, null, batchNo, null),
                    "Migration batch " + batchNo + " rolled back")));
    ledger.setFlag(
        new InvoiceLedgerService.FlagChange(
            invoice.getInvoiceNo(),
            InvoiceFlag.CANCELLED,
            true,
            MODULE,
            "Migration batch " + batchNo + " rolled back"));
    return invoice;
  }

  /**
   * Adjusts the opening position of a legacy invoice after go-live (year-end true-up, section
   * 17.7): LEGACY_ADJUSTED or LEGACY_WRITTEN_OFF movements on its components.
   *
   * @param invoiceNo invoice
   * @param type LEGACY_ADJUSTED or LEGACY_WRITTEN_OFF
   * @param amounts signed amounts per component
   * @param sourceRef true-up reference
   * @param valueDate value date
   * @return the movements
   */
  public List<OpsInvoiceMovement> adjustOpening(
      String invoiceNo,
      MovementType type,
      Map<LedgerComponent, BigDecimal> amounts,
      String sourceRef,
      LocalDate valueDate) {
    if (type != MovementType.LEGACY_ADJUSTED && type != MovementType.LEGACY_WRITTEN_OFF) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_MOVEMENT", "An opening adjustment is an adjustment or a write-off");
    }
    OpsInvoice invoice =
        invoices
            .findByInvoiceNo(invoiceNo)
            .orElseThrow(() -> new ResourceNotFoundException("Operations invoice", invoiceNo));
    if (!invoice.getLegacy().isLegacy()) {
      throw new BusinessRuleException(
          "MIG_NOT_LEGACY_INVOICE", "Invoice " + invoiceNo + " is not a legacy invoice");
    }
    return ledger.post(
        new MovementRequest(
            invoiceNo,
            type,
            MODULE,
            sourceRef,
            valueDate,
            amounts,
            DocumentRefs.NONE,
            "Year-end adjustment of the opening position"));
  }

  /**
   * The origin snapshot of a legacy invoice.
   *
   * @param invoiceId invoice id
   * @return snapshot with its lines, empty for BIBS invoices
   */
  @Transactional(readOnly = true)
  public Optional<OpsInvoiceOriginSnapshot> snapshot(Long invoiceId) {
    Optional<OpsInvoiceOriginSnapshot> s = snapshots.findByInvoiceId(invoiceId);
    s.ifPresent(OpsInvoiceOriginSnapshot::loadLines);
    return s;
  }
}
