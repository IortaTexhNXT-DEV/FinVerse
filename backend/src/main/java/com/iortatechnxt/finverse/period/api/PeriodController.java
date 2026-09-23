package com.iortatechnxt.finverse.period.api;

import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.period.api.dto.CreateFiscalYearRequest;
import com.iortatechnxt.finverse.period.api.dto.FiscalYearResponse;
import com.iortatechnxt.finverse.period.api.dto.PeriodResponse;
import com.iortatechnxt.finverse.period.service.PeriodService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for the financial calendar and period status console. */
@RestController
@RequestMapping("/api/v1/periods")
public class PeriodController {

  private static final String MANAGE = "hasAuthority('PERIOD_MANAGE')";

  private final PeriodService service;

  /**
   * Creates the controller.
   *
   * @param service period service
   */
  public PeriodController(PeriodService service) {
    this.service = service;
  }

  /**
   * Lists fiscal years.
   *
   * @param companyId company
   * @return years
   */
  @GetMapping("/years")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<FiscalYearResponse> years(@RequestParam Long companyId) {
    return service.listYears(companyId).stream().map(FiscalYearResponse::from).toList();
  }

  /**
   * Creates a fiscal year with its twelve periods.
   *
   * @param request request
   * @return year
   */
  @PostMapping("/years")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public FiscalYearResponse createYear(@Valid @RequestBody CreateFiscalYearRequest request) {
    return FiscalYearResponse.from(
        service.createFiscalYear(request.companyId(), request.yearCode()));
  }

  /**
   * Lists the periods of a fiscal year.
   *
   * @param yearId year
   * @return periods
   */
  @GetMapping("/years/{yearId}/periods")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<PeriodResponse> periods(@PathVariable Long yearId) {
    return service.listPeriods(yearId).stream().map(PeriodResponse::from).toList();
  }

  /**
   * Opens a period.
   *
   * @param id period
   * @return period
   */
  @PostMapping("/{id}/open")
  @PreAuthorize(MANAGE)
  public PeriodResponse open(@PathVariable Long id) {
    return PeriodResponse.from(service.open(id));
  }

  /**
   * Starts closing a period (soft close).
   *
   * @param id period
   * @return period
   */
  @PostMapping("/{id}/start-closing")
  @PreAuthorize(MANAGE)
  public PeriodResponse startClosing(@PathVariable Long id) {
    return PeriodResponse.from(service.startClosing(id));
  }

  /**
   * Hard-closes a period.
   *
   * @param id period
   * @return period
   */
  @PostMapping("/{id}/close")
  @PreAuthorize(MANAGE)
  public PeriodResponse close(@PathVariable Long id) {
    return PeriodResponse.from(service.close(id));
  }

  /**
   * Reopens a closed period.
   *
   * @param id period
   * @param request reason
   * @return period
   */
  @PostMapping("/{id}/reopen")
  @PreAuthorize(MANAGE)
  public PeriodResponse reopen(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return PeriodResponse.from(service.reopen(id, request.reason()));
  }
}
