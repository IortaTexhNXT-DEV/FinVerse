package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Cashiering legacy batches waiting in My Approvals: income reclassifications for the
 * Cashiering team lead and then for top management, legacy PR 2307 reversals for the team lead;
 * never to their requester or, at the second level, to the first approver.
 */
@Component
@Transactional(readOnly = true)
public class LegacyBatchApprovals implements PendingApprovalSource {

  private static final String MODULE = "CASHIERING";

  private final LegacyBatchRepository batches;

  /**
   * Creates the source.
   *
   * @param batches batches
   */
  public LegacyBatchApprovals(LegacyBatchRepository batches) {
    this.batches = batches;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> pending = new ArrayList<>();
    for (LegacyBatch b : batches.findByStatusOrderByIdAsc(LegacyBatch.Status.FOR_APPROVAL)) {
      String permission =
          b.getKind() == LegacyBatch.Kind.INCOME_RECLASS
              ? "CASH_DISPOSITION_APPROVE"
              : "LEGACY_REVERSAL_APPROVE";
      if (viewer.can(permission) && viewer.mayApproveItemOf(b.getSubmittedBy())) {
        pending.add(of(b));
      }
    }
    if (viewer.can("CASH_UPP_INCOME_APPROVE")) {
      for (LegacyBatch b :
          batches.findByStatusOrderByIdAsc(LegacyBatch.Status.FOR_TOP_MANAGEMENT)) {
        if (viewer.mayApproveItemOf(b.getSubmittedBy())
            && viewer.mayApproveItemOf(b.getFirstApprovedBy())) {
          pending.add(of(b));
        }
      }
    }
    return pending;
  }

  private static PendingApproval of(LegacyBatch b) {
    boolean income = b.getKind() == LegacyBatch.Kind.INCOME_RECLASS;
    return new PendingApproval(
        MODULE,
        income ? "Unapplied payments to other income" : "Legacy PR 2307 reversal",
        b.getBatchNo(),
        b.getLineCount() + " line(s) - " + b.getReason(),
        b.getTotal(),
        b.getCurrency(),
        b.getSubmittedBy(),
        b.getSubmittedAt(),
        b.getCompanyId(),
        "/cashiering/legacy-batches/" + b.getBatchNo());
  }
}
