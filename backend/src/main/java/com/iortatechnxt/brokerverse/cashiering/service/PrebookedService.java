package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.domain.ExceptionCode;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.domain.PrebookedRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The pre-booked queue (CSHID.020): payments matched to accounts that are not booked yet. When the
 * account is booked (ledger event, {@code PREBOOKED_REMATCH} job or "re-run now") the AR's money is
 * applied to its invoices and the item leaves the queue; any excess becomes an unapplied item.
 * Items waiting beyond the {@code PREBOOKED_AGEING} threshold raise that alert.
 */
@Service
@Transactional
public class PrebookedService {

  private static final String ENTITY = "Prebooked";
  private static final String AGEING = "PREBOOKED_AGEING";
  private static final int DEFAULT_AGEING_DAYS = 5;

  private final PrebookedRepository items;
  private final CashReceiptRepository receipts;
  private final InvoiceLedgerQueryService ledger;
  private final PaymentIntakeService intake;
  private final UnappliedService unapplied;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final ItemTransactions transactions;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items pre-booked items
   * @param receipts receipts
   * @param ledger invoice ledger
   * @param intake payment intake (application oldest first)
   * @param unapplied unapplied workbench
   * @param alerts alerts
   * @param audit audit trail
   * @param transactions one transaction per item
   * @param clock clock
   */
  public PrebookedService(
      PrebookedRepository items,
      CashReceiptRepository receipts,
      InvoiceLedgerQueryService ledger,
      PaymentIntakeService intake,
      UnappliedService unapplied,
      AlertService alerts,
      AuditTrailService audit,
      ItemTransactions transactions,
      Clock clock) {
    this.items = items;
    this.receipts = receipts;
    this.ledger = ledger;
    this.intake = intake;
    this.unapplied = unapplied;
    this.alerts = alerts;
    this.audit = audit;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * Items of a company in a status.
   *
   * @param companyId company
   * @param status OPEN, APPLIED or RELEASED
   * @param pageable page
   * @return items, oldest first
   */
  @Transactional(readOnly = true)
  public Page<Prebooked> list(Long companyId, String status, Pageable pageable) {
    return items.findByCompanyIdAndStatusOrderByIdAsc(companyId, status, pageable);
  }

  /**
   * Re-matches the open items of an account (booking event).
   *
   * @param arn account just booked
   * @return items applied
   */
  public int rematchAccount(String arn) {
    int applied = 0;
    for (Prebooked item : items.findByArnAndStatusOrderByIdAsc(arn, Prebooked.OPEN)) {
      if (rematch(item)) {
        applied++;
      }
    }
    return applied;
  }

  /**
   * Re-matches every open item and raises the ageing alert (job and "re-run now").
   *
   * @param businessDate business date
   * @return items applied
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public int rematchAll(LocalDate businessDate) {
    int ageingDays =
        alerts.activeCode(AGEING).map(ExceptionCode::getThresholdDays).orElse(DEFAULT_AGEING_DAYS);
    int applied = 0;
    List<Long> open =
        items.findByStatusOrderByIdAsc(Prebooked.OPEN).stream().map(Prebooked::getId).toList();
    for (Long id : open) {
      if (transactions.run("PRE:" + id, () -> rematchOrAge(get(id), ageingDays, businessDate))) {
        applied++;
      }
    }
    return applied;
  }

  private boolean rematchOrAge(Prebooked item, int ageingDays, LocalDate businessDate) {
    if (rematch(item)) {
      return true;
    }
    if (item.getFirstSeen().plusDays(ageingDays).isBefore(businessDate)) {
      alerts.raise(
          AGEING,
          new AlertFacts(
              item.getCompanyId(),
              null,
              ENTITY,
              String.valueOf(item.getId()),
              "Payment for "
                  + item.getArn()
                  + " waits for the booking since "
                  + item.getFirstSeen(),
              item.getAmount(),
              AGEING + ":" + item.getId()));
    }
    return false;
  }

  /**
   * Re-matches one item.
   *
   * @param id item
   * @return the item
   */
  public Prebooked rematchNow(Long id) {
    Prebooked item = get(id);
    requireOpen(item);
    rematch(item);
    return item;
  }

  /**
   * Releases an item to the unapplied workbench (no booking expected).
   *
   * @param id item
   * @param reason reason
   * @return the item
   */
  public Prebooked release(Long id, String reason) {
    Prebooked item = get(id);
    requireOpen(item);
    Receipt ar = receipt(item);
    unapplied.create(
        ar.getCompanyId(),
        ar.getBranchId(),
        spec(item, ar, UnappliedOrigin.PREBOOKED, ar.unappliedAmount(), reason));
    item.resolve(Prebooked.RELEASED, null, reason, clock.instant());
    audit.record(ENTITY, item.getArn(), AuditAction.UPDATE, "Released: " + reason);
    return item;
  }

  private boolean rematch(Prebooked item) {
    List<OpsInvoice> invoices =
        ledger.forArn(item.getArn()).stream().filter(ApplicationService::receivable).toList();
    if (invoices.isEmpty()) {
      item.attempted(clock.instant());
      return false;
    }
    Receipt ar = receipt(item);
    BigDecimal money = ar.unappliedAmount();
    List<Application> made =
        intake.applyOldestFirst(
            invoices,
            money,
            new Application.Origin(
                ar.getId(), null, ApplicationSource.PREBOOKED, "PRE:" + item.getId()),
            new ApplyOptions(LocalDate.now(clock), false, ar.getReceiptNo()));
    BigDecimal applied =
        made.stream().map(Application::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal excess = money.subtract(applied);
    if (excess.signum() > 0) {
      unapplied.create(
          ar.getCompanyId(),
          ar.getBranchId(),
          spec(item, ar, UnappliedOrigin.EXCESS, excess, "Excess after booking"));
    }
    item.resolve(
        Prebooked.APPLIED,
        made.isEmpty() ? null : made.get(0).getId(),
        "Booked: applied " + applied + (excess.signum() > 0 ? ", excess " + excess : ""),
        clock.instant());
    audit.record(ENTITY, item.getArn(), AuditAction.UPDATE, item.getRemarks());
    return true;
  }

  private static UnappliedSpec spec(
      Prebooked item, Receipt ar, UnappliedOrigin origin, BigDecimal amount, String remarks) {
    return new UnappliedSpec(
        origin,
        ar.getId(),
        item.getPaymentId(),
        null,
        ar.getPayorCode(),
        ar.getPayorName(),
        ar.getSalesUnit(),
        item.getCurrency(),
        amount,
        null,
        CashieringSettings.MODULE,
        "PRE:" + item.getId(),
        remarks);
  }

  private Receipt receipt(Prebooked item) {
    return receipts
        .findById(item.getReceiptId())
        .orElseThrow(
            () -> new ResourceNotFoundException(CashReceiptService.ENTITY, item.getReceiptId()));
  }

  private Prebooked get(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private static void requireOpen(Prebooked item) {
    if (!item.isOpen()) {
      throw new BusinessRuleException(
          "PREBOOKED_NOT_OPEN",
          "Pre-booked payment of " + item.getArn() + " is " + item.getStatus());
    }
  }
}
