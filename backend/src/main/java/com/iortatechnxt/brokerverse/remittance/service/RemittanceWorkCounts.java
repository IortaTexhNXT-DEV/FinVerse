package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittanceRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remittance tiles of the Operations home (BRQID.003): batches in review, for approval and waiting
 * for the insurer OR; holds and special remittances for approval; active holds.
 */
@Component
@Transactional(readOnly = true)
public class RemittanceWorkCounts implements OpsWorkCountSource {

  private static final String BATCHES = "/remittance/batches";

  private final RemittanceBatchRepository batches;
  private final HoldRequestRepository holds;
  private final SpecialRemittanceRepository specials;

  /**
   * Creates the source.
   *
   * @param batches batches
   * @param holds hold requests
   * @param specials special remittance requests
   */
  public RemittanceWorkCounts(
      RemittanceBatchRepository batches,
      HoldRequestRepository holds,
      SpecialRemittanceRepository specials) {
    this.batches = batches;
    this.holds = holds;
    this.specials = specials;
  }

  @Override
  public List<WorkCount> counts(Long companyId) {
    return List.of(
        tile(
            "remit-batches-review",
            "Batches in review",
            batches.countByCompanyIdAndStageIn(
                companyId, List.of(BatchStage.REVIEW_IN_PROCESS, BatchStage.ON_HOLD)),
            Severity.INFO,
            BATCHES),
        tile(
            "remit-batches-approval",
            "Batches for approval",
            batches.countByCompanyIdAndStageIn(companyId, List.of(BatchStage.FOR_APPROVAL)),
            Severity.WARNING,
            BATCHES + "?tab=FOR_APPROVAL"),
        tile(
            "remit-batches-or",
            "Awaiting insurer OR",
            batches.countByCompanyIdAndStageIn(
                companyId, List.of(BatchStage.PARTIALLY_REMITTED, BatchStage.FULLY_REMITTED)),
            Severity.INFO,
            "/remittance/insurer-or"),
        tile(
            "remit-holds-approval",
            "Holds for approval",
            holds.countByCompanyIdAndStageIn(
                companyId,
                List.of(
                    HoldStage.FOR_APPROVAL,
                    HoldStage.EXTENSION_FOR_APPROVAL,
                    HoldStage.CANCEL_FOR_APPROVAL)),
            Severity.WARNING,
            "/remittance/holds"),
        tile(
            "remit-holds-active",
            "Active holds",
            holds.countByCompanyIdAndStageIn(companyId, List.of(HoldStage.ACTIVE)),
            Severity.INFO,
            "/remittance/holds"),
        tile(
            "remit-special-approval",
            "Special remittances for approval",
            specials.countByCompanyIdAndStageIn(companyId, List.of(SpecialStage.FOR_APPROVAL)),
            Severity.WARNING,
            "/remittance/special"));
  }

  private static WorkCount tile(
      String key, String label, long count, Severity severity, String link) {
    return new WorkCount(Section.REMITTANCE, key, label, count, severity, link);
  }
}
