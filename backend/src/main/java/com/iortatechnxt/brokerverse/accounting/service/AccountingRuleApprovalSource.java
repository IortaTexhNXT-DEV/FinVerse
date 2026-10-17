package com.iortatechnxt.brokerverse.accounting.service;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingRule;
import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
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
