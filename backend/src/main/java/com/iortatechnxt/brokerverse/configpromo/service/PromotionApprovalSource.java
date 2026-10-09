package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportStatus;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImportRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Configuration imports waiting for approval, in the approval inbox of their approvers. */
@Component
public class PromotionApprovalSource implements PendingApprovalSource {

  private final PromotionImportRepository imports;

  /**
   * Creates the source.
   *
   * @param imports imports
   */
  public PromotionApprovalSource(PromotionImportRepository imports) {
    this.imports = imports;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can("CONFIG_IMPORT_APPROVE")) {
      return List.of();
    }
    return imports.findByStatusOrderByIdAsc(ImportStatus.SUBMITTED).stream()
        .filter(i -> viewer.mayApproveItemOf(i.getPreparedBy()))
        .map(
            i ->
                new PendingApproval(
                    "CONFIG_PROMOTION",
                    "Configuration import",
                    i.getImportNo(),
                    (i.getChangeReference() == null ? "" : i.getChangeReference() + " - ")
                        + (i.getReason() == null ? "Configuration package" : i.getReason()),
                    null,
                    null,
                    i.getSubmittedBy(),
                    i.getSubmittedAt(),
                    null,
                    "/admin/config-promotion/imports?import=" + i.getId()))
        .toList();
  }
}
