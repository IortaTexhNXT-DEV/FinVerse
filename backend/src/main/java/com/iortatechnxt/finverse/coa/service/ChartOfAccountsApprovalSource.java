package com.iortatechnxt.finverse.coa.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
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
