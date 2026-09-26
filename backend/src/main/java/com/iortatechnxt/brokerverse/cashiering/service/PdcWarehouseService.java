package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem.PdcCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItemRepository;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The PDC warehouse (CSHID.008 item 4): post-dated checks are stored with a traceable {@code PDCW-}
 * number, viewable by maturity; at maturity ({@code PDC_MATURITY}) each becomes a payment with its
 * AR, matched and applied like any other payment. Before maturity a check can be returned, replaced
 * or pulled out.
 */
@Service
@Transactional
public class PdcWarehouseService {

  private static final String ENTITY = "PdcItem";
  private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

  private final PdcItemRepository items;
  private final PaymentIntakeService intake;
  private final DocumentNumberService numbers;
  private final ItemTransactions transactions;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items warehouse
   * @param intake payment intake
   * @param numbers document numbers
   * @param transactions one transaction per item
   * @param audit audit trail
   * @param clock clock
   */
  public PdcWarehouseService(
      PdcItemRepository items,
      PaymentIntakeService intake,
      DocumentNumberService numbers,
      ItemTransactions transactions,
      AuditTrailService audit,
      Clock clock) {
    this.items = items;
    this.intake = intake;
    this.numbers = numbers;
    this.transactions = transactions;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Warehouses a check; the same bank and check number is refused twice.
   *
   * @param companyId company
   * @param branchId branch
   * @param check check details
   * @return the item
   */
  public PdcItem warehouse(Long companyId, Long branchId, PdcCheck check) {
    if (items.existsByCompanyIdAndBankCodeAndCheckNo(
        companyId, check.bankCode(), check.checkNo())) {
      throw new BusinessRuleException(
          "PDC_DUPLICATE",
          "Check " + check.bankCode() + " " + check.checkNo() + " is already warehoused");
    }
    if (check.amount() == null || check.amount().signum() <= 0) {
      throw new BusinessRuleException(
          "PDC_AMOUNT", "A post-dated check needs an amount above zero");
    }
    PdcItem item =
        items.save(
            new PdcItem(
                companyId,
                branchId,
                numbers.next("PDCW-" + LocalDate.now(clock).getYear()),
                check));
    audit.record(
        ENTITY,
        item.getWarehouseNo(),
        AuditAction.CREATE,
        check.checkNo() + " matures " + check.maturityDate());
    return item;
  }

  /**
   * Checks of the warehouse.
   *
   * @param companyId company
   * @param status status, null for all
   * @param from maturity from, null for open
   * @param to maturity to, null for open
   * @param pageable page
   * @return checks by maturity
   */
  @Transactional(readOnly = true)
  public Page<PdcItem> list(
      Long companyId, PdcStatus status, LocalDate from, LocalDate to, Pageable pageable) {
    return items.search(
        companyId, status, from == null ? EARLIEST : from, to == null ? LATEST : to, pageable);
  }

  /**
   * Takes a check out of the warehouse before maturity.
   *
   * @param id item
   * @param outcome RETURNED, REPLACED or PULLED_OUT
   * @param reason reason
   * @return the item
   */
  public PdcItem release(Long id, PdcStatus outcome, String reason) {
    PdcItem item = items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    item.release(outcome, reason);
    audit.record(ENTITY, item.getWarehouseNo(), AuditAction.UPDATE, outcome + ": " + reason);
    return item;
  }

  /**
   * Matures every warehoused check due on or before a date, each in its own transaction.
   *
   * @param businessDate business date
   * @return checks matured
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public int mature(LocalDate businessDate) {
    List<Long> due =
        items
            .findByStatusAndMaturityDateLessThanEqualOrderByIdAsc(
                PdcStatus.WAREHOUSED, businessDate)
            .stream()
            .map(PdcItem::getId)
            .toList();
    int matured = 0;
    for (Long id : due) {
      if (transactions.run("PDC:" + id, () -> matureOne(id))) {
        matured++;
      }
    }
    return matured;
  }

  private boolean matureOne(Long id) {
    PdcItem item = items.findById(id).orElseThrow();
    IntakeResult result =
        intake.receive(
            new IntakeTarget(item.getCompanyId(), item.getBranchId(), "PDC", ReceiptSource.PDC),
            new PaymentIntake(
                PaymentChannel.PDC,
                item.getWarehouseNo(),
                item.getWarehouseNo(),
                null,
                item.getReference(),
                List.of(),
                new PaymentIntake.Payor(item.getClientCode(), item.getPayorName()),
                null,
                new PaymentIntake.Money(
                    item.getAmount(), item.getCurrency(), item.getMaturityDate()),
                new PaymentIntake.Tender(
                    PaymentMode.PDC, item.getCheckNo(), item.getBankCode(), null, false)));
    item.matured(
        result.payment().getId(),
        result.receipt() == null ? null : result.receipt().getReceiptNo(),
        result.payment().getAppliedAmount().signum() > 0);
    audit.record(
        ENTITY,
        item.getWarehouseNo(),
        AuditAction.POST,
        "Matured: " + result.payment().getMatchCategory());
    return true;
  }
}
