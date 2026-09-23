package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.receivables.domain.ReceiptRepository;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source of receivables: receipts pending approval ({@code
 * RECEIPT_PAYMENT_AUTHORIZE}), never shown to the user who entered them. Receipt approval applies
 * no authorization limit (the money is already received). The link opens the receipt in the
 * approval queue, where the approve and reject actions are.
 */
@Component
public class ReceiptApprovalSource implements PendingApprovalSource {

  /** Module code of receivables items. */
  public static final String MODULE = "RECEIVABLES";

  private static final String PERMISSION = "RECEIPT_PAYMENT_AUTHORIZE";

  private final ReceiptRepository receipts;

  /**
   * Creates the source.
   *
   * @param receipts receipt repository
   */
  public ReceiptApprovalSource(ReceiptRepository receipts) {
    this.receipts = receipts;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(PERMISSION)) {
      return List.of();
    }
    return receipts.findByStatusOrderByIdAsc(ReceiptStatus.PENDING_APPROVAL).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getCreatedBy()))
        .map(
            r ->
                new PendingApproval(
                    MODULE,
                    "Receipt (" + r.getMode() + ")",
                    r.getReceiptNo(),
                    r.getPayerName(),
                    r.getAmount(),
                    r.getCurrency(),
                    r.getCreatedBy(),
                    r.getCreatedAt(),
                    r.getCompanyId(),
                    "/receivables/approvals/" + r.getId()))
        .toList();
  }
}
