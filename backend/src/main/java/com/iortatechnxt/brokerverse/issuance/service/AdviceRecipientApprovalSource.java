package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceRecipientRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of the Insurance Advice recipient set-up (FR-NB-107): the set-ups pending
 * authorization, for the holders of {@code LOV_MANAGE} or {@code MASTER_AUTHORIZE} other than their
 * maker.
 */
@Component
@Transactional(readOnly = true)
public class AdviceRecipientApprovalSource implements PendingApprovalSource {

  private final AdviceRecipientRepository rows;

  /**
   * Creates the source.
   *
   * @param rows recipient set-ups
   */
  public AdviceRecipientApprovalSource(AdviceRecipientRepository rows) {
    this.rows = rows;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can("LOV_MANAGE") && !viewer.can("MASTER_AUTHORIZE")) {
      return List.of();
    }
    return rows.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getMaker()))
        .map(
            r ->
                new PendingApproval(
                    "NEW_BUSINESS",
                    "Insurance Advice recipient",
                    r.getMortgageeBank()
                        + (r.getMarketSegment() == null ? "" : " " + r.getMarketSegment()),
                    (r.isAutoSend() ? "Automatic sending to " : "Recipients ")
                        + String.join(", ", r.getTo()),
                    null,
                    null,
                    r.getMaker(),
                    r.getUpdatedAt() == null ? r.getCreatedAt() : r.getUpdatedAt(),
                    r.getCompanyId(),
                    "/issuance/advice-recipients"))
        .toList();
  }
}
