package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.closing.service.CheckItem;
import com.iortatechnxt.brokerverse.closing.service.PeriodEndCheckProvider;
import com.iortatechnxt.brokerverse.reserves.domain.RunStatus;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Period-end checklist control "Actuarial reserves posted": the valuation run of the month must be
 * posted. Companies that do not use actuarial reserving (no reserve parameters and no run, as every
 * BDOI company) get no checklist row.
 */
@Component
@Transactional(readOnly = true)
public class ReserveCloseCheck implements PeriodEndCheckProvider {

  /** Checklist code. */
  public static final String CODE = "RESERVE_VALUATION";

  private static final String LABEL = "Actuarial reserves valued and posted";

  private final ValuationRunService runs;
  private final ReserveParameterService parameters;

  /**
   * Creates the control.
   *
   * @param runs valuation runs
   * @param parameters reserve parameters
   */
  public ReserveCloseCheck(ValuationRunService runs, ReserveParameterService parameters) {
    this.runs = runs;
    this.parameters = parameters;
  }

  @Override
  public List<CheckItem> periodEndChecks(
      Long companyId, LocalDate periodStart, LocalDate periodEnd) {
    Optional<ValuationRun> run = runs.forMonth(companyId, periodEnd);
    if (run.isEmpty() && parameters.list(companyId).isEmpty() && runs.list(companyId).isEmpty()) {
      return List.of();
    }
    boolean posted = run.map(r -> r.getStatus() == RunStatus.POSTED).orElse(false);
    String detail =
        run.map(r -> "Valuation run " + r.getPeriodName() + " is " + r.getStatus())
            .orElse("No valuation run for " + ValuationRunService.monthEnd(periodEnd));
    return List.of(CheckItem.of(CODE, LABEL, posted, detail));
  }
}
