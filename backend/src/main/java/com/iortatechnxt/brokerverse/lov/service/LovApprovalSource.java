package com.iortatechnxt.brokerverse.lov.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import java.util.List;
import org.springframework.stereotype.Component;

/** Approval inbox source: list values pending authorization (BRNB.079 "LOV updates"). */
@Component
public class LovApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public LovApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return records.pending(
        viewer,
        LovValue.class,
        v ->
            new RecordFacts(
                "List value",
                v.getTypeCode() + ":" + v.getCode(),
                v.getLabel(),
                null,
                "/broking-setup/lists?type=" + v.getTypeCode()));
  }
}
