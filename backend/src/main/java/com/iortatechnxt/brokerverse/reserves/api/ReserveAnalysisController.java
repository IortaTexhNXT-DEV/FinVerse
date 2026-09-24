package com.iortatechnxt.brokerverse.reserves.api;

import com.iortatechnxt.brokerverse.reserves.api.dto.ReserveSummaryResponse;
import com.iortatechnxt.brokerverse.reserves.api.dto.TriangleResponse;
import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.brokerverse.reserves.domain.TriangleBasis;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reserve read models: technical reserves summary (dashboard) and IBNR development triangles. */
@RestController
@RequestMapping("/api/v1/reserves")
@PreAuthorize("hasAnyAuthority('RESERVE_PREPARE', 'PERIOD_END_RUN', 'REPORT_FINANCIAL')")
public class ReserveAnalysisController {

  private final ReserveAnalysisService service;

  /**
   * Creates the controller.
   *
   * @param service analysis service
   */
  public ReserveAnalysisController(ReserveAnalysisService service) {
    this.service = service;
  }

  /**
   * Technical reserves summary of a valuation month vs the previous posted valuation.
   *
   * @param companyId company
   * @param asOf any date of the valuation month
   * @return summary
   */
  @GetMapping("/summary")
  public ReserveSummaryResponse summary(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return ReserveSummaryResponse.from(service.summary(companyId, asOf));
  }

  /**
   * Chain-ladder triangles of a line of business.
   *
   * @param companyId company
   * @param businessLine line of business
   * @param asOf valuation date
   * @param basis PAID or INCURRED (default: parameter)
   * @param period YEAR or QUARTER (default: parameter)
   * @param accidentPeriods number of accident periods, 2 to 20 (default: parameter)
   * @return triangles
   */
  @GetMapping("/triangles")
  public TriangleResponse triangles(
      @RequestParam Long companyId,
      @RequestParam String businessLine,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(required = false) TriangleBasis basis,
      @RequestParam(required = false) DevelopmentPeriod period,
      @RequestParam(required = false) Integer accidentPeriods) {
    return TriangleResponse.from(
        service.triangles(companyId, businessLine, asOf, basis, period, accidentPeriods));
  }
}
