package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering items in the universal "My Approvals" inbox: receipt series pending authorization
 * (CSHID.006/015), cancellations and reinstatements for approval (CSHID.001-005) and dispositions
 * for approval (CSHID.024/025). Makers never see their own items.
 */
@Component
@Transactional(readOnly = true)
public class CashieringApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "CASHIERING";
  private static final String FOR_APPROVAL = "FOR_APPROVAL";
  private static final int MAX_ITEMS = 200;

  private final MasterRecordApprovals masters;
  private final ReceiptActionRepository actions;
  private final CashReceiptRepository receipts;
  private final UnappliedRepository items;
  private final DispositionRepository dispositions;

  /**
   * Creates the source.
   *
   * @param masters master record approvals
   * @param actions receipt actions
   * @param receipts receipts
   * @param items unapplied items
   * @param dispositions dispositions
   */
  public CashieringApprovalSource(
      MasterRecordApprovals masters,
      ReceiptActionRepository actions,
      CashReceiptRepository receipts,
      UnappliedRepository items,
      DispositionRepository dispositions) {
    this.masters = masters;
    this.actions = actions;
    this.receipts = receipts;
    this.items = items;
    this.dispositions = dispositions;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> pending = new ArrayList<>();
    pending.addAll(
        masters.pending(
            viewer,
            new Scope(MODULE, MasterRecordApprovals.PERMISSION),
            ReceiptSeries.class,
            s ->
                new RecordFacts(
                    "Receipt series",
                    s.getPrefix() + s.getFromNo(),
                    s.getKind() + " " + s.getFromNo() + "-" + s.getToNo(),
                    s.getCompanyId(),
                    "/cashiering/series")));
    if (viewer.can("CASH_APPROVE")) {
      for (ReceiptAction a : actions.findByStageOrderByIdAsc(FOR_APPROVAL)) {
        if (viewer.mayApproveItemOf(a.getCreatedBy())) {
          pending.add(action(a));
        }
      }
    }
    if (viewer.can("CASH_DISPOSITION_APPROVE")) {
      for (Unapplied u : items.findByStageOrderByIdAsc(FOR_APPROVAL, Pageable.ofSize(MAX_ITEMS))) {
        dispositions.findByUnappliedIdOrderByIdAsc(u.getId()).stream()
            .reduce((x, y) -> y)
            .filter(d -> viewer.mayApproveItemOf(d.getCreatedBy()))
            .ifPresent(d -> pending.add(disposition(u, d)));
      }
    }
    return pending;
  }

  private PendingApproval action(ReceiptAction a) {
    Receipt r = receipts.findById(a.getReceiptId()).orElse(null);
    return new PendingApproval(
        MODULE,
        a.isReinstatement() ? "Receipt reinstatement" : "Receipt cancellation",
        a.getTransactionNo(),
        (r == null ? "" : r.getReceiptNo() + " " + r.getPayorName() + " - ") + a.getReasonCode(),
        a.getAmount(),
        r == null ? null : r.getCurrency(),
        a.getCreatedBy(),
        a.getCreatedAt(),
        a.getCompanyId(),
        "/cashiering/receipts/" + a.getReceiptId());
  }

  private static PendingApproval disposition(Unapplied u, Disposition d) {
    return new PendingApproval(
        MODULE,
        "Unapplied disposition",
        u.getReference(),
        d.getDispositionType() + " - " + (u.getPayorName() == null ? "" : u.getPayorName()),
        d.getAmount(),
        u.getCurrency(),
        d.getCreatedBy(),
        d.getUpdatedAt() != null ? d.getUpdatedAt() : d.getCreatedAt(),
        u.getCompanyId(),
        "/cashiering/unapplied/" + u.getId());
  }
}
