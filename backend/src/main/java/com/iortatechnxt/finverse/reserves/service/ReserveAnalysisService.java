package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.finverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.RunLine;
import com.iortatechnxt.finverse.reserves.domain.TriangleBasis;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read models of the reserves: the technical reserves summary (current vs previous valuation) and
 * the IBNR development triangles.
 */
@Service
@Transactional(readOnly = true)
public class ReserveAnalysisService {

  private static final int MIN_PERIODS = 2;
  private static final int MAX_PERIODS = 20;

  private final ValuationRunService runs;
  private final ReserveParameterService parameters;
  private final IbnrCalculator ibnr;

  /**
   * Creates the service.
   *
   * @param runs valuation runs
   * @param parameters reserve parameters
   * @param ibnr IBNR calculator (triangles)
   */
  public ReserveAnalysisService(
      ValuationRunService runs, ReserveParameterService parameters, IbnrCalculator ibnr) {
    this.runs = runs;
    this.parameters = parameters;
    this.ibnr = ibnr;
  }

  /**
   * Summary of a valuation month: its live run (any status) or else the latest posted run on or
   * before the date, compared with the posted run before it.
   *
   * @param companyId company
   * @param asOf valuation date (any day of the month)
   * @return summary (empty rows when no run exists)
   */
  public ReserveSummary summary(Long companyId, LocalDate asOf) {
    Optional<ValuationRun> current =
        runs.forMonth(companyId, asOf)
            .map(r -> runs.get(r.getId()))
            .or(() -> runs.latestPosted(companyId, ValuationRunService.monthEnd(asOf)));
    if (current.isEmpty()) {
      return new ReserveSummary(null, null, null, null, null, List.of());
    }
    ValuationRun run = current.get();
    Optional<ValuationRun> previous = runs.postedBefore(companyId, run.getValuationDate());
    return new ReserveSummary(
        run.getId(),
        run.getValuationDate(),
        run.getStatus().name(),
        previous.map(ValuationRun::getId).orElse(null),
        previous.map(ValuationRun::getValuationDate).orElse(null),
        ReserveSummary.rows(
            values(run), previous.map(ReserveAnalysisService::values).orElse(List.of())));
  }

  /**
   * Chain-ladder triangles of a line of business. Options left blank take the line's parameters in
   * force at the date.
   *
   * @param companyId company
   * @param businessLine line of business
   * @param asOf valuation date
   * @param basis paid or incurred, null = parameter
   * @param period year or quarter, null = parameter
   * @param accidentPeriods number of accident periods (clamped to 2..20), null = parameter
   * @return analysis
   */
  public TriangleAnalysis triangles(
      Long companyId,
      String businessLine,
      LocalDate asOf,
      TriangleBasis basis,
      DevelopmentPeriod period,
      Integer accidentPeriods) {
    ReserveParameterTerms t = parameters.inForce(companyId, asOf).terms(businessLine);
    ReserveParameterTerms terms =
        new ReserveParameterTerms(
            t.ibnrMethod(),
            t.ibnrRate(),
            basis == null ? t.triangleBasis() : basis,
            period == null ? t.developmentPeriod() : period,
            accidentPeriods == null
                ? t.accidentPeriods()
                : Math.clamp(accidentPeriods, MIN_PERIODS, MAX_PERIODS),
            t.mfadPct(),
            t.ulaePct(),
            t.expectedLossRatio(),
            t.treatyCommissionPct(),
            t.facCommissionPct(),
            t.remarks());
    return ibnr.analyse(companyId, asOf, businessLine, terms);
  }

  /**
   * Lines of a run as values.
   *
   * @param run run (lines loaded)
   * @return values
   */
  public static List<ReserveLineValues> values(ValuationRun run) {
    return run.getLines().stream().map(RunLine::values).toList();
  }
}
