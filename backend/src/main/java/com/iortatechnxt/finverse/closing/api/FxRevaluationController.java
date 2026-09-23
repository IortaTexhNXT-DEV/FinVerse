package com.iortatechnxt.finverse.closing.api;

import com.iortatechnxt.finverse.closing.api.dto.FxPreviewResponse;
import com.iortatechnxt.finverse.closing.api.dto.FxRevaluationRequest;
import com.iortatechnxt.finverse.closing.api.dto.FxRevaluationResponse;
import com.iortatechnxt.finverse.closing.service.FxRevaluationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Period-end FX revaluation: preview, post (idempotent per period) and run history. */
@RestController
@RequestMapping("/api/v1/closing/fx-revaluations")
@PreAuthorize("hasAuthority('PERIOD_END_RUN')")
public class FxRevaluationController {

  private final FxRevaluationService service;

  /**
   * Creates the controller.
   *
   * @param service revaluation service
   */
  public FxRevaluationController(FxRevaluationService service) {
    this.service = service;
  }

  /**
   * Lists runs.
   *
   * @param companyId company
   * @return runs
   */
  @GetMapping
  public List<FxRevaluationResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(FxRevaluationResponse::summary).toList();
  }

  /**
   * Gets a run.
   *
   * @param id id
   * @return run with lines
   */
  @GetMapping("/{id}")
  public FxRevaluationResponse get(@PathVariable Long id) {
    return FxRevaluationResponse.detail(service.get(id));
  }

  /**
   * Previews the revaluation of a period.
   *
   * @param companyId company
   * @param periodId period
   * @return preview
   */
  @GetMapping("/preview")
  public FxPreviewResponse preview(@RequestParam Long companyId, @RequestParam Long periodId) {
    return FxPreviewResponse.from(service.preview(companyId, periodId));
  }

  /**
   * Posts the revaluation of a period.
   *
   * @param request request
   * @return run
   */
  @PostMapping
  public FxRevaluationResponse post(@Valid @RequestBody FxRevaluationRequest request) {
    service.post(
        request.companyId(), request.periodId(), request.autoReverse(), request.gainLossAccount());
    return FxRevaluationResponse.detail(
        service.forPeriod(request.companyId(), request.periodId()).orElseThrow());
  }
}
