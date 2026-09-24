package com.iortatechnxt.brokerverse.coa.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import java.util.List;
import org.springframework.stereotype.Component;

/** Approval inbox source: GL accounts pending authorization. */
@Component
public class ChartOfAccountsApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public ChartOfAccountsApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return records.pending(
        viewer,
        GlAccount.class,
        a ->
            new RecordFacts(
                "GL account", a.getCode(), a.getName(), a.getCompanyId(), "/gl/accounts"));
  }
}
