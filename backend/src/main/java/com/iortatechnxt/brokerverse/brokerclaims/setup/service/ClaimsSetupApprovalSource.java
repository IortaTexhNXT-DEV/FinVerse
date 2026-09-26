package com.iortatechnxt.brokerverse.brokerclaims.setup.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.domain.AttributeChange;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccess;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * My Approvals source of Claims Setup (FR-CL-040/041/043: pending values appear in the authoriser's
 * My Approvals): status access matrix rows and status / settlement attribute changes waiting for
 * another BCL_SETUP user; the maker never sees his own.
 */
@Component
public class ClaimsSetupApprovalSource implements PendingApprovalSource {

  private static final Scope SCOPE = new Scope(ClaimCodes.MODULE, "BCL_SETUP");
  private static final String LINK = "/claims-handling/setup";

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public ClaimsSetupApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    items.addAll(
        records.pending(
            viewer,
            SCOPE,
            StatusAccess.class,
            r ->
                new RecordFacts(
                    "Claim status access",
                    r.getStatusCode() + ":" + r.getRoleCode(),
                    r.getUnitCode() == null ? "Any unit" : r.getUnitCode(),
                    null,
                    LINK)));
    items.addAll(
        records.pending(
            viewer,
            SCOPE,
            AttributeChange.class,
            c ->
                new RecordFacts(
                    "Claims list attribute",
                    c.getTypeCode() + ":" + c.getCode(),
                    c.getAttribute() + " = " + (c.getNewValue() == null ? "(none)" : c.getNewValue()),
                    null,
                    LINK)));
    return items;
  }
}
