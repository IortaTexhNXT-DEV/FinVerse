package com.iortatechnxt.finverse.fixedasset.api;

import com.iortatechnxt.finverse.fixedasset.api.dto.DepreciationLineResponse;
import com.iortatechnxt.finverse.fixedasset.api.dto.DepreciationPreviewResponse;
import com.iortatechnxt.finverse.fixedasset.api.dto.DepreciationRunResponse;
import com.iortatechnxt.finverse.fixedasset.domain.DepreciationRun;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationService;
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

/** REST API for the monthly depreciation run. */
@RestController
@RequestMapping("/api/v1/assets/depreciation")
public class DepreciationController {

  private final DepreciationService service;

  /**
   * Creates the controller.
   *
   * @param service depreciation service
   */
  public DepreciationController(DepreciationService service) {
    this.service = service;
  }

  /**
   * Depreciation of a period: the posted run, or the proposal when not yet posted.
   *
   * @param companyId company
   * @param period period (YYYY-MM)
   * @return preview
   */
  @GetMapping("/preview")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public DepreciationPreviewResponse preview(
      @RequestParam Long companyId, @RequestParam YearMonth period) {
    Optional<DepreciationRun> run = service.findRun(companyId, period);
    if (run.isPresent()) {
      return DepreciationPreviewResponse.of(
          period.toString(), DepreciationRunResponse.from(run.get()), lines(run.get().getId()));
    }
    return DepreciationPreviewResponse.of(
        period.toString(),
        null,
        service.preview(companyId, period).stream().map(DepreciationLineResponse::from).toList());
  }

  /**
   * Posts the depreciation of a period (idempotent).
   *
   * @param companyId company
   * @param period period (YYYY-MM)
   * @return run
   */
  @PostMapping("/runs")
  @PreAuthorize("hasAuthority('PERIOD_END_RUN')")
  public DepreciationRunResponse post(
      @RequestParam Long companyId, @RequestParam YearMonth period) {
    return DepreciationRunResponse.from(service.post(companyId, period));
  }

  /**
   * Lists posted runs.
   *
   * @param companyId company
   * @return runs
   */
  @GetMapping("/runs")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<DepreciationRunResponse> runs(@RequestParam Long companyId) {
    return service.runs(companyId).stream().map(DepreciationRunResponse::from).toList();
  }

  /**
   * Lines of a run.
   *
   * @param id run
   * @return lines
   */
  @GetMapping("/runs/{id}/lines")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<DepreciationLineResponse> lines(@PathVariable Long id) {
    return service.lines(id).stream().map(DepreciationLineResponse::from).toList();
  }
}
