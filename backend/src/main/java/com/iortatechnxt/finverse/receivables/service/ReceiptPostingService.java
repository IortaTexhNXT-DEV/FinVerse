package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.receivables.api.dto.ApplyRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.finverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.finverse.receivables.domain.PdcRepository;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.domain.ReceiptAllocation;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import com.iortatechnxt.finverse.receivables.service.AllocationPlanner.Planned;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.ItemMatch;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounting side of receipts (checker actions): approval with GL posting and open-item matching,
 * later application of money held on account, cancellation and bounced cheques.
 *
 * <p>Everything happens in one transaction: the receipt status, the journals and the sub-ledger are
 * always consistent.
 */
@Service
@Transactional
public class ReceiptPostingService {

  private static final String RECEIPT_DOC = "RECEIPT";

  private final ReceiptService receipts;
  private final ReceiptAccounting accounting;
  private final AllocationPlanner planner;
  private final OpenItemService openItems;
  private final PdcRepository pdcs;
  private final PdcStatusRecorder pdcStatus;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param receipts receipt service
   * @param accounting receipt accounting
   * @param planner allocation planner
   * @param openItems open item service
   * @param pdcs PDC repository
   * @param pdcStatus PDC status recorder
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReceiptPostingService(
      ReceiptService receipts,
      ReceiptAccounting accounting,
      AllocationPlanner planner,
      OpenItemService openItems,
      PdcRepository pdcs,
      PdcStatusRecorder pdcStatus,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.receipts = receipts;
    this.accounting = accounting;
    this.planner = planner;
    this.openItems = openItems;
    this.pdcs = pdcs;
    this.pdcStatus = pdcStatus;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Approves a receipt: posts the journals, records the receipt's CREDIT open item and matches it
   * against the allocated debit notes (FIFO allocations are computed now).
   *
   * @param id receipt
   * @return approved receipt
   */
  public Receipt approve(Long id) {
    Receipt r = receipts.get(id);
    r.requireStatus(ReceiptStatus.PENDING_APPROVAL);
    String checker = currentUser.username();
    if (Objects.equals(r.getCreatedBy(), checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A receipt cannot be approved by the user who entered it");
    }
    if (r.getAllocationMethod() == AllocationMethod.FIFO) {
      planner
          .fifo(r.getCompanyId(), r.getPartyId(), r.getCurrency(), r.getAmount())
          .forEach(p -> r.addAllocation(p.item().getId(), p.item().getDocumentNo(), p.amount()));
    }
    BigDecimal applied =
        r.getAllocations().stream()
            .map(ReceiptAllocation::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    String batchNo = accounting.postApproval(r, applied);
    Long creditItemId = null;
    if (r.getPayerType().hasParty()) {
      OpenItem credit = openItems.record(creditItem(r, batchNo));
      creditItemId = credit.getId();
      for (ReceiptAllocation a : r.getAllocations()) {
        ItemMatch m =
            openItems.match(a.getDebitItemId(), creditItemId, a.getAmount(), r.getReceiptDate());
        a.matched(m.getId(), r.getReceiptDate());
      }
      if (applied.signum() > 0) {
        r.apply(applied);
      }
    }
    r.approve(checker, clock.instant(), batchNo, creditItemId);
    audit.record(
        ReceiptService.ENTITY,
        r.getReceiptNo(),
        AuditAction.AUTHORIZE,
        "Approved; applied " + applied + ", on account " + r.unapplied() + ", journal " + batchNo);
    return r;
  }

  /**
   * Applies money held on account to debit notes of the payer.
   *
   * @param id receipt
   * @param request date and allocations
   * @return receipt
   */
  public Receipt applyUnapplied(Long id, ApplyRequest request) {
    Receipt r = receipts.get(id);
    checkApplicable(r, request);
    List<Planned> plan =
        request.method() == AllocationMethod.MANUAL
            ? planner.manual(r.getPartyId(), r.getCurrency(), r.unapplied(), request.allocations())
            : planner.fifo(r.getCompanyId(), r.getPartyId(), r.getCurrency(), r.unapplied());
    BigDecimal total = AllocationPlanner.total(plan);
    if (total.signum() == 0) {
      throw new BusinessRuleException(
          "NO_OPEN_ITEMS", "The payer has no open debit items to apply the money to");
    }
    r.apply(total);
    int sequence = r.nextApplicationNo();
    if (r.getPayerType().usesPremiumDeposit()) {
      accounting.postApplication(r, total, request.date(), sequence);
    }
    for (Planned p : plan) {
      ReceiptAllocation a = r.addAllocation(p.item().getId(), p.item().getDocumentNo(), p.amount());
      ItemMatch m =
          openItems.match(p.item().getId(), r.getCreditItemId(), p.amount(), request.date());
      a.matched(m.getId(), request.date());
    }
    audit.record(
        ReceiptService.ENTITY,
        r.getReceiptNo(),
        AuditAction.UPDATE,
        "Applied " + total + " from money on account");
    return r;
  }

  private static void checkApplicable(Receipt r, ApplyRequest request) {
    r.requireStatus(ReceiptStatus.APPROVED);
    if (!r.getPayerType().hasParty() || r.unapplied().signum() == 0) {
      throw new BusinessRuleException(
          "NOTHING_ON_ACCOUNT", "Receipt " + r.getReceiptNo() + " has no money on account");
    }
    if (request.date().isBefore(r.getReceiptDate())) {
      throw new BusinessRuleException(
          "INVALID_APPLICATION_DATE", "Application date cannot precede the receipt date");
    }
  }

  /**
   * Cancels an approved receipt: reverses its journals, re-opens the debit notes it settled and
   * neutralises its open item.
   *
   * @param id receipt
   * @param request date and reason
   * @return receipt
   */
  public Receipt cancel(Long id, ReversalRequest request) {
    return reverse(id, ReceiptStatus.CANCELLED, request);
  }

  /**
   * Records a bounced (dishonoured) cheque: like a cancellation, and a PDC behind the receipt is
   * marked BOUNCED.
   *
   * @param id receipt
   * @param request date and reason
   * @return receipt
   */
  public Receipt bounce(Long id, ReversalRequest request) {
    return reverse(id, ReceiptStatus.BOUNCED, request);
  }

  private Receipt reverse(Long id, ReceiptStatus to, ReversalRequest request) {
    Receipt r = receipts.get(id);
    LocalDate date = request.date();
    r.reverse(to, currentUser.username(), date, request.reason());
    if (r.getCreditItemId() != null) {
      openItems.unmatchAll(r.getCreditItemId(), to + " " + r.getReceiptNo());
      r.getAllocations().forEach(ReceiptAllocation::unmatched);
      String suffix = to == ReceiptStatus.BOUNCED ? "-BNC" : "-CAN";
      OpenItem reversal = openItems.record(reversalItem(r, date, suffix));
      openItems.match(reversal.getId(), r.getCreditItemId(), r.getAmount(), date);
    }
    String batchNo = accounting.postReversal(r, date);
    if (r.getPdcId() != null && to == ReceiptStatus.BOUNCED) {
      pdcStatus.transition(
          pdcs.findById(r.getPdcId()).orElseThrow(),
          PdcStatus.BOUNCED,
          date,
          request.reason(),
          r.getReceiptNo());
    }
    audit.record(
        ReceiptService.ENTITY,
        r.getReceiptNo(),
        AuditAction.REVERSE,
        to + " on " + date + " (journal " + batchNo + "): " + request.reason());
    return r;
  }

  private static OpenItemValues creditItem(Receipt r, String batchNo) {
    return new OpenItemValues(
        r.getCompanyId(),
        r.getBranchId(),
        r.getPartyId(),
        r.getPartyCode(),
        ItemDirection.CREDIT,
        RECEIPT_DOC,
        r.getReceiptNo(),
        r.getReceiptDate(),
        r.getReceiptDate(),
        r.getCurrency(),
        r.getAmount(),
        r.getBaseAmount(),
        ReceiptAccounting.MODULE,
        ReceiptAccounting.key(r),
        batchNo,
        ReceiptAccounting.narration(r));
  }

  private static OpenItemValues reversalItem(Receipt r, LocalDate date, String suffix) {
    return new OpenItemValues(
        r.getCompanyId(),
        r.getBranchId(),
        r.getPartyId(),
        r.getPartyCode(),
        ItemDirection.DEBIT,
        RECEIPT_DOC + "_REVERSAL",
        r.getReceiptNo() + suffix,
        date,
        date,
        r.getCurrency(),
        r.getAmount(),
        r.getBaseAmount(),
        ReceiptAccounting.MODULE,
        ReceiptAccounting.key(r) + ":REV",
        null,
        r.getReversalReason());
  }
}
