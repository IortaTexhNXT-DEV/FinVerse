package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Correction entries waiting for approval in the approval inbox (ACSL 2.11.0): shown to the holders
 * of {@code ACSL_APPROVE}, never to the correction's maker (creator or preparer).
 */
@Component
@Transactional(readOnly = true)
public class AcslApprovalSource implements PendingApprovalSource {

  private final CorrectionRepository corrections;

  /**
   * Creates the source.
   *
   * @param corrections corrections
   */
  public AcslApprovalSource(CorrectionRepository corrections) {
    this.corrections = corrections;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can("ACSL_APPROVE")) {
      return List.of();
    }
    return corrections.findByStageInOrderByIdAsc(List.of(CorrectionStage.FOR_APPROVAL)).stream()
        .filter(c -> viewer.mayApproveItemOf(c.getCreatedBy()))
        .filter(c -> c.getSubmittedBy() == null || viewer.mayApproveItemOf(c.getSubmittedBy()))
        .map(AcslApprovalSource::pending)
        .toList();
  }

  private static PendingApproval pending(Correction c) {
    return new PendingApproval(
        Acsl.MODULE,
        "Correction entry",
        c.getCorrectionNo(),
        c.getDescription(),
        c.totalDebit(),
        c.getCurrency(),
        c.getReviewedBy() == null ? c.getSubmittedBy() : c.getReviewedBy(),
        c.getReviewedAt() == null ? c.getSubmittedAt() : c.getReviewedAt(),
        c.getCompanyId(),
        Acsl.correctionLink(c.getId()));
  }
}
