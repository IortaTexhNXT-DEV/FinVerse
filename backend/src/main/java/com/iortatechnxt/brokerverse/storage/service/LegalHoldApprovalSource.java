package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.storage.domain.LegalHoldRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Pending legal hold requests in the approval inbox of the DOA approvers. */
@Component
public class LegalHoldApprovalSource implements PendingApprovalSource {

  /** Permission of the approvers. */
  static final String APPROVE = "FILE_LEGAL_HOLD_APPROVE";

  private final LegalHoldService holds;

  /**
   * Creates the source.
   *
   * @param holds legal hold service
   */
  public LegalHoldApprovalSource(LegalHoldService holds) {
    this.holds = holds;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(APPROVE)) {
      return List.of();
    }
    return holds.pending().stream()
        .filter(r -> viewer.mayApproveItemOf(r.getRequestedBy()))
        .map(LegalHoldApprovalSource::item)
        .toList();
  }

  private static PendingApproval item(LegalHoldRequest request) {
    return new PendingApproval(
        "STORAGE",
        "Legal hold " + request.getAction().name().toLowerCase(Locale.ROOT),
        "LH-" + request.getId(),
        "File " + request.getStoredFileId() + ": " + request.getReason(),
        null,
        null,
        request.getRequestedBy(),
        request.getRequestedAt(),
        null,
        null);
  }
}
