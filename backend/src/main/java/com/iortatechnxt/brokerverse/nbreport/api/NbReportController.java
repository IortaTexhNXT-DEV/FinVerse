package com.iortatechnxt.brokerverse.nbreport.api;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbreport.api.dto.NbDashboardResponse;
import com.iortatechnxt.brokerverse.nbreport.api.dto.ReportVariantDto;
import com.iortatechnxt.brokerverse.nbreport.api.dto.SalesTargetDto;
import com.iortatechnxt.brokerverse.nbreport.api.dto.SaveVariantRequest;
import com.iortatechnxt.brokerverse.nbreport.domain.ReportVariant;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboardService;
import com.iortatechnxt.brokerverse.nbreport.service.ReportVariantService;
import com.iortatechnxt.brokerverse.nbreport.service.SalesTargetService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * New Business dashboard (BRNB.012), saved report variants (BRNB.057) and production targets
 * (BRNB.075). The NB reports themselves run through the generic report API ({@code
 * /api/v1/reports}, category New Business).
 */
@RestController
@RequestMapping("/api/v1/nb")
public class NbReportController {

  private final NbDashboardService dashboard;
  private final ReportVariantService variants;
  private final SalesTargetService targets;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param dashboard dashboard
   * @param variants report variants
   * @param targets production targets
   * @param currentUser current user
   * @param clock clock (default date)
   */
  public NbReportController(
      NbDashboardService dashboard,
      ReportVariantService variants,
      SalesTargetService targets,
      CurrentUser currentUser,
      Clock clock) {
    this.dashboard = dashboard;
    this.variants = variants;
    this.targets = targets;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The New Business dashboard of a company.
   *
   * @param companyId company
   * @param asOf date of the figures, default today
   * @return dashboard
   */
  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('WORK_VIEW')")
  public NbDashboardResponse dashboard(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return NbDashboardResponse.from(
        dashboard.dashboard(companyId, asOf == null ? LocalDate.now(clock) : asOf));
  }

  /**
   * The saved variants of a report the user sees.
   *
   * @param reportCode report
   * @return variants
   */
  @GetMapping("/report-variants")
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public List<ReportVariantDto> variants(@RequestParam String reportCode) {
    return variants.visible(reportCode).stream().map(this::dto).toList();
  }

  /**
   * Saves a report variant of the user.
   *
   * @param request variant
   * @return variant
   */
  @PostMapping("/report-variants")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public ReportVariantDto saveVariant(@Valid @RequestBody SaveVariantRequest request) {
    return dto(
        variants.save(
            request.reportCode(), request.name(), request.parameters(), request.shared()));
  }

  /**
   * Deletes one of the user's report variants.
   *
   * @param id variant
   */
  @DeleteMapping("/report-variants/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public void deleteVariant(@PathVariable Long id) {
    variants.delete(id);
  }

  /**
   * Production targets whose period overlaps a range.
   *
   * @param companyId company
   * @param from range start
   * @param to range end
   * @return targets
   */
  @GetMapping("/targets")
  @PreAuthorize("hasAnyAuthority('WORK_ASSIGN', 'MASTER_VIEW')")
  public List<SalesTargetDto> targets(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return targets.targets(companyId, from, to).stream().map(SalesTargetDto::from).toList();
  }

  /**
   * Adds or changes a production target.
   *
   * @param companyId company
   * @param request target
   * @return target
   */
  @PutMapping("/targets")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public SalesTargetDto saveTarget(
      @RequestParam Long companyId, @Valid @RequestBody SalesTargetDto request) {
    return SalesTargetDto.from(targets.save(companyId, request.unit(), request.values()));
  }

  private ReportVariantDto dto(ReportVariant v) {
    return ReportVariantDto.from(
        v, CurrentUser.sameUser(v.getOwner(), currentUser.username()), variants.parameters(v));
  }
}
