package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatch;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Legacy direct payment PR reversal batches waiting in My Approvals, never to their requester. */
@Component
@Transactional(readOnly = true)
public class DpprBatchApprovals implements PendingApprovalSource {

  private final DpprBatchRepository batches;

  /**
   * Creates the source.
   *
   * @param batches batches
   */
  public DpprBatchApprovals(DpprBatchRepository batches) {
    this.batches = batches;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> pending = new ArrayList<>();
    if (!viewer.can("LEGACY_REVERSAL_APPROVE")) {
      return pending;
    }
    for (DpprBatch b : batches.findByStatusOrderByIdAsc(DpprBatch.Status.FOR_APPROVAL)) {
      if (viewer.mayApproveItemOf(b.getSubmittedBy())) {
        pending.add(
            new PendingApproval(
                "COMMISSION",
                "Legacy direct payment PR reversal",
                b.getBatchNo(),
                b.getLineCount() + " invoice(s) - " + b.getReason(),
                b.getTotal(),
                null,
                b.getSubmittedBy(),
                b.getSubmittedAt(),
                b.getCompanyId(),
                "/commission/dppr-batches/" + b.getBatchNo()));
      }
    }
    return pending;
  }
}
