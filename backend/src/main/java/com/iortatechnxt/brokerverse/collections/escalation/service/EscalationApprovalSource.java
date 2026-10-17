package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import java.util.List;
import org.springframework.stereotype.Component;

/** Approval inbox source: escalation rules waiting for authorization (BRCLXN.049). */
@Component
public class EscalationApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public EscalationApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return records.pending(
        viewer,
        EscalationRule.class,
        r ->
            new RecordFacts(
                "Escalation Rule",
                r.getCode(),
                r.getName(),
                r.getCompanyId(),
                "/collections/escalation-rules"));
  }
}
