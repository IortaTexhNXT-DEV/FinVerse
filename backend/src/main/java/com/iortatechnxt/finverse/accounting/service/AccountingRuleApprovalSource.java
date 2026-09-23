package com.iortatechnxt.finverse.accounting.service;

import com.iortatechnxt.finverse.accounting.domain.AccountingRule;
import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import java.util.List;
import org.springframework.stereotype.Component;

/** Approval inbox source: accounting rules pending authorization. */
@Component
public class AccountingRuleApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public AccountingRuleApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return records.pending(
        viewer,
        AccountingRule.class,
        r ->
            new RecordFacts(
                "Accounting rule",
                r.getEventType() + " #" + r.getId(),
                r.getName(),
                r.getCompanyId(),
                "/accounting/rules/" + r.getId()));
  }
}
