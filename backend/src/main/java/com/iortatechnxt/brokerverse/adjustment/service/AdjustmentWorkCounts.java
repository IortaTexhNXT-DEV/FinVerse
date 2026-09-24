package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adjustment tiles of the Operations home (BRQID.003): endorsement requests for validation, for
 * approval, for posting, returned to the requester and posted with payments to re-apply.
 */
@Component
@Transactional(readOnly = true)
public class AdjustmentWorkCounts implements OpsWorkCountSource {

  private static final String LIST = "/adjustment?stage=";

  private final EndorsementRequestRepository requests;

  /**
   * Creates the source.
   *
   * @param requests requests
   */
  public AdjustmentWorkCounts(EndorsementRequestRepository requests) {
    this.requests = requests;
  }

  @Override
  public List<WorkCount> counts(Long companyId) {
    return List.of(
        tile(companyId, RequestStage.FOR_VALIDATION, "Endorsements for validation", Severity.INFO),
        tile(companyId, RequestStage.FOR_APPROVAL, "Endorsements for approval", Severity.WARNING),
        tile(companyId, RequestStage.FOR_POSTING, "Endorsements for posting", Severity.INFO),
        tile(companyId, RequestStage.RETURNED, "Returned to requester", Severity.WARNING),
        tile(
            companyId,
            RequestStage.AWAITING_REAPPLICATION,
            "Payments to re-apply",
            Severity.ALERT));
  }

  private WorkCount tile(Long companyId, RequestStage stage, String label, Severity severity) {
    return new WorkCount(
        Section.ADJUSTMENT,
        "ADJ_" + stage.name(),
        label,
        requests.countByCompanyIdAndStage(companyId, stage),
        severity,
        LIST + stage.name());
  }
}
