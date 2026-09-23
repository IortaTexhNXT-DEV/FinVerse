package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameter;
import com.iortatechnxt.finverse.reserves.domain.ReserveType;
import com.iortatechnxt.finverse.reserves.domain.RunLine;
import com.iortatechnxt.finverse.reserves.domain.RunStatus;
import com.iortatechnxt.finverse.reserves.domain.TakafulSetting;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import com.iortatechnxt.finverse.reserves.domain.ValuationRunRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approval inbox source: valuation runs waiting for approval (checkers with PERIOD_END_RUN) and
 * reserve parameters / takaful settings pending authorization (MASTER_AUTHORIZE).
 */
@Component
@Transactional(readOnly = true)
public class ReserveApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "RESERVES";
  private static final String APPROVER = "PERIOD_END_RUN";

  private final ValuationRunRepository runs;
  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param runs valuation runs
   * @param records master record helper
   */
  public ReserveApprovalSource(ValuationRunRepository runs, MasterRecordApprovals records) {
    this.runs = runs;
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> out = new ArrayList<>();
    if (viewer.can(APPROVER)) {
      runs.findByStatus(RunStatus.PENDING_APPROVAL).stream()
          .filter(r -> viewer.mayApproveItemOf(r.getSubmittedBy()))
          .map(ReserveApprovalSource::item)
          .forEach(out::add);
    }
    out.addAll(
        records.pending(
            viewer,
            ReserveParameter.class,
            p ->
                new RecordFacts(
                    "Reserve parameters",
                    p.getBusinessLine() + " from " + p.getEffectiveFrom(),
                    "IBNR " + p.terms().ibnrMethod(),
                    p.getCompanyId(),
                    "/reserves/parameters")));
    out.addAll(
        records.pending(
            viewer,
            TakafulSetting.class,
            s ->
                new RecordFacts(
                    "Takaful settings",
                    "TAKAFUL",
                    s.isEnabled() ? "Surplus run enabled" : "Surplus run disabled",
                    s.getCompanyId(),
                    "/reserves/parameters")));
    return out;
  }

  private static PendingApproval item(ValuationRun r) {
    BigDecimal upr =
        r.getLines().stream()
            .filter(l -> l.getReserveType() == ReserveType.UPR)
            .map(RunLine::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new PendingApproval(
        MODULE,
        "Valuation run",
        r.getPeriodName(),
        "Technical reserves as at " + r.getValuationDate() + " (gross UPR shown)",
        upr,
        r.getBaseCurrency(),
        r.getSubmittedBy(),
        r.getSubmittedAt(),
        r.getCompanyId(),
        "/reserves/runs/" + r.getId());
  }
}
