package com.iortatechnxt.finverse.party.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.party.domain.Party;
import java.util.List;
import org.springframework.stereotype.Component;

/** Approval inbox source: business partners (parties) pending authorization. */
@Component
public class PartyApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public PartyApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return records.pending(
        viewer,
        Party.class,
        p ->
            new RecordFacts(
                "Party (" + p.getPartyType() + ")",
                p.getCode(),
                p.getName(),
                p.getCompanyId(),
                "/setup/parties"));
  }
}
