package com.iortatechnxt.finverse.investment.api;

import com.iortatechnxt.finverse.investment.api.dto.InvestmentRunResponse;
import com.iortatechnxt.finverse.investment.api.dto.RunPreviewResponse;
import com.iortatechnxt.finverse.investment.api.dto.RunPreviewResponse.Line;
import com.iortatechnxt.finverse.investment.api.dto.TransactionResponse;
import com.iortatechnxt.finverse.investment.domain.InvestmentRun;
import com.iortatechnxt.finverse.investment.domain.RunType;
import com.iortatechnxt.finverse.investment.service.InvestmentRunService;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for month-end interest accrual and amortization runs. */
@RestController
@RequestMapping("/api/v1/investments/runs")
public class InvestmentRunController {

  private final InvestmentRunService service;

  /**
   * Creates the controller.
   *
   * @param service run service
   */
  public InvestmentRunController(InvestmentRunService service) {
    this.service = service;
  }

  /**
   * A period's run: the posted run, or the proposal when not yet posted.
   *
   * @param companyId company
   * @param type run type
   * @param period period (YYYY-MM)
   * @return preview
   */
  @GetMapping("/preview")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public RunPreviewResponse preview(
      @RequestParam Long companyId, @RequestParam RunType type, @RequestParam YearMonth period) {
    Optional<InvestmentRun> run = service.findRun(companyId, type, period);
    if (run.isPresent()) {
      List<Line> lines = service.transactions(run.get().getId()).stream().map(Line::of).toList();
      return RunPreviewResponse.of(
          type, period.toString(), InvestmentRunResponse.from(run.get()), lines);
    }
    List<Line> lines =
        service.preview(companyId, type, period).stream()
            .map(p -> Line.of(p.holding(), p.due()))
            .toList();
    return RunPreviewResponse.of(type, period.toString(), null, lines);
  }

  /**
   * Posts a run (idempotent).
   *
   * @param companyId company
   * @param type run type
   * @param period period (YYYY-MM)
   * @return run
   */
  @PostMapping
  @PreAuthorize("hasAuthority('PERIOD_END_RUN')")
  public InvestmentRunResponse post(
      @RequestParam Long companyId, @RequestParam RunType type, @RequestParam YearMonth period) {
    return InvestmentRunResponse.from(service.post(companyId, type, period));
  }

  /**
   * Lists posted runs.
   *
   * @param companyId company
   * @return runs
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<InvestmentRunResponse> runs(@RequestParam Long companyId) {
    return service.runs(companyId).stream().map(InvestmentRunResponse::from).toList();
  }

  /**
   * Transactions posted by a run.
   *
   * @param id run
   * @return transactions
   */
  @GetMapping("/{id}/transactions")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<TransactionResponse> transactions(@PathVariable Long id) {
    return service.transactions(id).stream().map(TransactionResponse::from).toList();
  }
}
