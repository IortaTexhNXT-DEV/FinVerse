package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRiskRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of the insurer renewable lists of Renewal Setup (FR-RN-020, 112): the rows
 * pending authorization, for the holders of {@code RNW_SETUP} other than their maker.
 */
@Component
@Transactional(readOnly = true)
public class InsurerRenewableApprovalSource implements PendingApprovalSource {

  private final InsurerRenewableRiskRepository rows;

  /**
   * Creates the source.
   *
   * @param rows insurer renewable lists
   */
  public InsurerRenewableApprovalSource(InsurerRenewableRiskRepository rows) {
    this.rows = rows;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(RenewalCodes.SETUP)) {
      return List.of();
    }
    return rows.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getMaker()))
        .map(
            r ->
                new PendingApproval(
                    "RENEWAL",
                    "Insurer renewable list",
                    r.getInsurerCode() + " " + r.getRiskCode(),
                    "Risk code " + r.getRiskCode() + " renewed by the insurer",
                    null,
                    null,
                    r.getMaker(),
                    r.getUpdatedAt() == null ? r.getCreatedAt() : r.getUpdatedAt(),
                    r.getCompanyId(),
                    "/renewal/setup?tab=insurer-lists"))
        .toList();
  }
}
