package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItem;
import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItemRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.WriteOffAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService.FlagChange;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-off and credit of premium receivable balances (ADJID.026, OPERATIONS_DESIGN section 5 row
 * 21): the minimal balance file (10.00 to 100.00, parameter {@code MIN_BALANCE_FILE_RANGE}, OQ11)
 * and the write-off request type. A debit balance is written off by component (event {@code
 * OPS_WRITE_OFF}: expense against the premium receivable); a credit balance (overpaid) is cleared
 * against other income. The ledger records a {@code WRITE_OFF} movement ({@code WO:<invoice>}, once
 * per invoice) and the {@code WRITTEN_OFF} flag.
 */
@Service
@Transactional
public class WriteOffService {

  /** Parameter of the file range. */
  public static final String RANGE_PARAMETER = "MIN_BALANCE_FILE_RANGE";

  private static final BigDecimal DEFAULT_MIN = new BigDecimal("10.00");
  private static final BigDecimal DEFAULT_MAX = new BigDecimal("100.00");
  private static final Map<LedgerComponent, String> EVENT_COMPONENTS = eventComponents();

  private final InvoiceLedgerQueryService queries;
  private final InvoiceLedgerService ledger;
  private final AdjustmentEvents events;
  private final MinBalanceItemRepository items;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param queries ledger reads
   * @param ledger ledger writes
   * @param events accounting events
   * @param items write-offs done
   * @param parameters business parameters (range)
   * @param audit audit trail
   * @param clock clock
   */
  public WriteOffService(
      InvoiceLedgerQueryService queries,
      InvoiceLedgerService ledger,
      AdjustmentEvents events,
      MinBalanceItemRepository items,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.queries = queries;
    this.ledger = ledger;
    this.events = events;
    this.items = items;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The balance range of the minimal balance file.
   *
   * @return minimum and maximum absolute balance
   */
  @Transactional(readOnly = true)
  public Range fileRange() {
    String text = parameters.text(RANGE_PARAMETER, "").strip();
    String[] bounds = text.split("-");
    if (bounds.length != 2) {
      return new Range(DEFAULT_MIN, DEFAULT_MAX);
    }
    try {
      return new Range(new BigDecimal(bounds[0].strip()), new BigDecimal(bounds[1].strip()));
    } catch (NumberFormatException e) {
      return new Range(DEFAULT_MIN, DEFAULT_MAX);
    }
  }

  /**
   * Why a file line cannot be processed (ADJID.026 negatives: missing invoice, out-of-range amount,
   * balance different from the file, already written off, locked by another module).
   *
   * @param invoiceNo invoice of the line
   * @param fileBalance balance stated in the file
   * @return errors, empty when the line can be processed
   */
  @Transactional(readOnly = true)
  public List<String> check(String invoiceNo, BigDecimal fileBalance) {
    List<String> errors = new ArrayList<>();
    Optional<OpsInvoice> found = queries.find(invoiceNo);
    if (found.isEmpty()) {
      errors.add("Invoice " + invoiceNo + " is not in the Operations ledger");
      return errors;
    }
    OpsInvoice invoice = found.get();
    BigDecimal balance = invoice.premiumBalance();
    Range range = fileRange();
    if (!range.contains(balance.abs())) {
      errors.add(
          "The premium receivable balance "
              + balance.toPlainString()
              + " is outside "
              + range.min().toPlainString()
              + " to "
              + range.max().toPlainString());
    }
    if (fileBalance != null && fileBalance.compareTo(balance) != 0) {
      errors.add(
          "The file balance "
              + fileBalance.toPlainString()
              + " does not match the ledger balance "
              + balance.toPlainString());
    }
    if (invoice.isWrittenOff() || items.findByInvoiceNo(invoiceNo).isPresent()) {
      errors.add("Invoice " + invoiceNo + " is already written off");
    }
    if (invoice.getLockOwner() != null && !Adjustments.MODULE.equals(invoice.getLockOwner())) {
      errors.add("Invoice " + invoiceNo + " is locked by " + invoice.getLockOwner());
    }
    return errors;
  }

  /**
   * Writes off (debit balance) or credits (credit balance) an invoice's premium receivable.
   *
   * @param invoiceNo invoice
   * @param fileRef minimal balance file (upload) or endorsement request
   * @param withinFileRange whether the balance must be within the file range
   * @return the write-off (the existing one when the invoice was already written off)
   */
  public MinBalanceItem writeOff(String invoiceNo, String fileRef, boolean withinFileRange) {
    Optional<MinBalanceItem> done = items.findByInvoiceNo(invoiceNo);
    if (done.isPresent()) {
      return done.get();
    }
    OpsInvoice invoice = queries.require(invoiceNo);
    BigDecimal balance = invoice.premiumBalance();
    if (balance.signum() == 0) {
      throw new BusinessRuleException(
          "ADJ_NOTHING_TO_WRITE_OFF", invoiceNo + " has no premium receivable balance");
    }
    if (withinFileRange && !fileRange().contains(balance.abs())) {
      throw new BusinessRuleException(
          "ADJ_BALANCE_OUT_OF_RANGE",
          invoiceNo + " has a balance of " + balance.toPlainString() + ", outside the file range");
    }
    WriteOffAction action = balance.signum() > 0 ? WriteOffAction.WRITE_OFF : WriteOffAction.CREDIT;
    JournalBatch journal = postBalances(invoice, fileRef, action);
    ledger.setFlag(
        new FlagChange(invoiceNo, InvoiceFlag.WRITTEN_OFF, true, Adjustments.MODULE, fileRef));
    MinBalanceItem item =
        items.save(
            new MinBalanceItem(
                invoice.getCompanyId(),
                fileRef,
                new MinBalanceItem.InvoiceKeys(
                    invoiceNo, invoice.getArn(), invoice.getClientCode(), invoice.getCurrency()),
                balance,
                action,
                journal.getBatchNo()));
    audit.record(
        "MinBalanceItem",
        invoiceNo,
        AuditAction.POST,
        action + " " + balance.toPlainString() + " (" + fileRef + ")");
    return item;
  }

  private JournalBatch postBalances(OpsInvoice invoice, String fileRef, WriteOffAction action) {
    String invoiceNo = invoice.getInvoiceNo();
    Map<LedgerComponent, BigDecimal> components = premiumBalances(invoice);
    String sourceRef = "WO:" + invoiceNo;
    String what = action + " of the premium receivable balance of " + invoiceNo;
    JournalBatch journal =
        events.post(
            invoice,
            LocalDate.now(clock),
            new AdjustmentEvents.Spec(
                AdjustmentEvents.WRITE_OFF,
                sourceRef,
                invoiceNo,
                invoice.getClientCode(),
                what + " (" + fileRef + ")",
                eventAmounts(components, invoice.premiumBalance(), action),
                Map.of()));
    ledger.post(
        new MovementRequest(
            invoiceNo,
            MovementType.WRITE_OFF,
            Adjustments.MODULE,
            sourceRef,
            journal.getValueDate(),
            components,
            new DocumentRefs(null, null, fileRef, journal.getBatchNo()),
            action + " (" + fileRef + ")"));
    return journal;
  }

  private static Map<LedgerComponent, BigDecimal> premiumBalances(OpsInvoice invoice) {
    Map<LedgerComponent, BigDecimal> map = new EnumMap<>(LedgerComponent.class);
    invoice
        .balances()
        .forEach(
            (component, amount) -> {
              if (component.isPremiumReceivable() && amount.signum() != 0) {
                map.put(component, amount);
              }
            });
    return map;
  }

  private static Map<String, BigDecimal> eventAmounts(
      Map<LedgerComponent, BigDecimal> components, BigDecimal balance, WriteOffAction action) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    components.forEach((c, a) -> amounts.put(EVENT_COMPONENTS.get(c), a));
    amounts.put(action == WriteOffAction.WRITE_OFF ? "WRITE_OFF" : "CREDIT_BALANCE", balance.abs());
    return amounts;
  }

  private static Map<LedgerComponent, String> eventComponents() {
    Map<LedgerComponent, String> map = new EnumMap<>(LedgerComponent.class);
    map.put(LedgerComponent.BASIC, "PR_BASIC");
    map.put(LedgerComponent.DST, "PR_DST");
    map.put(LedgerComponent.PREMIUM_TAX_VAT, "PR_PTX_VAT");
    map.put(LedgerComponent.LGT, "PR_LGT");
    map.put(LedgerComponent.FST, "PR_FST");
    map.put(LedgerComponent.OTHER, "PR_OTHER");
    return map;
  }

  /**
   * Absolute balance range of the file.
   *
   * @param min minimum (inclusive)
   * @param max maximum (inclusive)
   */
  public record Range(BigDecimal min, BigDecimal max) {

    /**
     * Whether an absolute balance is within the range.
     *
     * @param amount absolute balance
     * @return true when min &lt;= amount &lt;= max
     */
    public boolean contains(BigDecimal amount) {
      return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
    }
  }
}
