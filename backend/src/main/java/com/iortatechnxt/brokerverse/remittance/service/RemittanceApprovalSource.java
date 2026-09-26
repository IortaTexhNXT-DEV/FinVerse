package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittanceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remittance items of the universal approval inbox (RMTID.010, MKTID.006/009): batches for approval
 * (REMIT_APPROVE), hold requests, extensions and cancellations (HOLD_APPROVE) and special
 * remittances (SPECIAL_REMIT_APPROVE); makers never see their own items (four eyes).
 */
@Component
@Transactional(readOnly = true)
public class RemittanceApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "REMITTANCE";

  private final RemittanceBatchRepository batches;
  private final HoldRequestRepository holds;
  private final SpecialRemittanceRepository specials;

  /**
   * Creates the source.
   *
   * @param batches batches
   * @param holds hold requests
   * @param specials special remittance requests
   */
  public RemittanceApprovalSource(
      RemittanceBatchRepository batches,
      HoldRequestRepository holds,
      SpecialRemittanceRepository specials) {
    this.batches = batches;
    this.holds = holds;
    this.specials = specials;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    if (viewer.can("REMIT_APPROVE")) {
      batches(viewer, items);
    }
    if (viewer.can(HoldService.APPROVER)) {
      holds(viewer, items);
    }
    if (viewer.can("SPECIAL_REMIT_APPROVE")) {
      specials(viewer, items);
    }
    return items;
  }

  private void batches(ApprovalViewer viewer, List<PendingApproval> items) {
    for (RemittanceBatch b : batches.findByStageInOrderByIdAsc(List.of(BatchStage.FOR_APPROVAL))) {
      if (viewer.mayApproveItemOf(b.getSubmittedBy())
          && viewer.mayApproveItemOf(b.getProcessor())) {
        items.add(
            new PendingApproval(
                MODULE,
                "Remittance batch",
                b.getBatchNo(),
                b.getInsurerCode() + " - " + b.getLineCount() + " account(s)",
                b.getTotals().payable(),
                b.getCurrency(),
                b.getSubmittedBy(),
                b.getSubmittedAt(),
                b.getCompanyId(),
                "/remittance/batches/" + b.getId()));
      }
    }
  }

  private void holds(ApprovalViewer viewer, List<PendingApproval> items) {
    List<HoldStage> stages =
        List.of(
            HoldStage.FOR_APPROVAL,
            HoldStage.EXTENSION_FOR_APPROVAL,
            HoldStage.CANCEL_FOR_APPROVAL);
    for (HoldRequest h : holds.findByStageInOrderByIdAsc(stages)) {
      if (viewer.mayApproveItemOf(h.getRequestedBy())) {
        items.add(
            new PendingApproval(
                MODULE,
                "Remittance hold",
                h.getRequestNo(),
                h.getInvoiceNo() + " until " + h.getHoldUntil() + " (" + h.getStage() + ")",
                null,
                null,
                h.getRequestedBy(),
                h.getUpdatedAt() == null ? h.getCreatedAt() : h.getUpdatedAt(),
                h.getCompanyId(),
                "/remittance/holds/" + h.getId()));
      }
    }
  }

  private void specials(ApprovalViewer viewer, List<PendingApproval> items) {
    for (SpecialRemittance s :
        specials.findByStageInOrderByIdAsc(List.of(SpecialStage.FOR_APPROVAL))) {
      if (viewer.mayApproveItemOf(s.getRequestedBy())) {
        items.add(
            new PendingApproval(
                MODULE,
                "Special remittance",
                s.getRequestNo(),
                s.getInvoiceNo() + " - " + s.getConditionCode(),
                null,
                null,
                s.getRequestedBy(),
                s.getCreatedAt(),
                s.getCompanyId(),
                "/remittance/special/" + s.getId()));
      }
    }
  }
}
