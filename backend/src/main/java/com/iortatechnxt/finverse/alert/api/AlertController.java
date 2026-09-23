package com.iortatechnxt.finverse.alert.api;

import com.iortatechnxt.finverse.alert.api.dto.AlertCommentRequest;
import com.iortatechnxt.finverse.alert.api.dto.AlertResponse;
import com.iortatechnxt.finverse.alert.api.dto.AlertSearchParams;
import com.iortatechnxt.finverse.alert.api.dto.ExceptionCodeRequest;
import com.iortatechnxt.finverse.alert.api.dto.ExceptionCodeResponse;
import com.iortatechnxt.finverse.alert.domain.AlertSeverity;
import com.iortatechnxt.finverse.alert.service.AlertDailyJob;
import com.iortatechnxt.finverse.alert.service.AlertService;
import com.iortatechnxt.finverse.alert.service.AlertService.CodeSettings;
import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.system.api.dto.JobRunResponse;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import com.iortatechnxt.finverse.system.service.JobRegistry;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Alerts inbox, acknowledgement, Exception Codes Master and on-demand checks. */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

  private static final String VIEW = "hasAuthority('ALERT_VIEW')";
  private static final String MANAGE = "hasAuthority('ALERT_MANAGE')";
  private static final int MAX_PAGE_SIZE = 200;

  private final AlertService alerts;
  private final JobRegistry jobs;

  /**
   * Creates the controller.
   *
   * @param alerts alert service
   * @param jobs job registry (on-demand checks)
   */
  public AlertController(AlertService alerts, JobRegistry jobs) {
    this.alerts = alerts;
    this.jobs = jobs;
  }

  /**
   * Searches alerts.
   *
   * @param params filters (all optional)
   * @param page page index
   * @param size page size
   * @return alerts, newest first
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<AlertResponse> search(
      @ModelAttribute AlertSearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        alerts.search(params.toCriteria(), PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        AlertResponse::from);
  }

  /**
   * Live (open or acknowledged) alerts per severity.
   *
   * @return summary
   */
  @GetMapping("/summary")
  @PreAuthorize(VIEW)
  public AlertSummary summary() {
    Map<AlertSeverity, Long> bySeverity = alerts.liveSummary();
    return new AlertSummary(
        bySeverity.values().stream().mapToLong(Long::longValue).sum(), bySeverity);
  }

  /**
   * Acknowledges an alert.
   *
   * @param id id
   * @param request optional comment
   * @return alert
   */
  @PostMapping("/{id}/acknowledge")
  @PreAuthorize(MANAGE)
  public AlertResponse acknowledge(
      @PathVariable Long id, @Valid @RequestBody(required = false) AlertCommentRequest request) {
    return AlertResponse.from(alerts.acknowledge(id, request == null ? null : request.comment()));
  }

  /**
   * Resolves an alert.
   *
   * @param id id
   * @param request resolution comment
   * @return alert
   */
  @PostMapping("/{id}/resolve")
  @PreAuthorize(MANAGE)
  public AlertResponse resolve(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return AlertResponse.from(alerts.resolve(id, request.reason()));
  }

  /**
   * Exception Codes Master.
   *
   * @return codes
   */
  @GetMapping("/exception-codes")
  @PreAuthorize("hasAnyAuthority('ALERT_VIEW','SYSTEM_PARAMETER_MANAGE')")
  public List<ExceptionCodeResponse> exceptionCodes() {
    return alerts.codes().stream().map(ExceptionCodeResponse::from).toList();
  }

  /**
   * Tunes an exception code.
   *
   * @param code code
   * @param request settings
   * @return code
   */
  @PutMapping("/exception-codes/{code}")
  @PreAuthorize("hasAuthority('SYSTEM_PARAMETER_MANAGE')")
  public ExceptionCodeResponse configure(
      @PathVariable String code, @Valid @RequestBody ExceptionCodeRequest request) {
    return ExceptionCodeResponse.from(
        alerts.configure(
            code,
            new CodeSettings(
                request.severity(),
                request.thresholdAmount(),
                request.thresholdDays(),
                request.active())));
  }

  /**
   * Runs the daily exception checks now.
   *
   * @return job run
   */
  @PostMapping("/checks/run")
  @PreAuthorize(MANAGE)
  public JobRunResponse runChecks() {
    return JobRunResponse.from(jobs.run(AlertDailyJob.JOB_NAME, JobTrigger.MANUAL));
  }

  /**
   * Live alert counts.
   *
   * @param live open plus acknowledged alerts
   * @param bySeverity counts per severity
   */
  public record AlertSummary(long live, Map<AlertSeverity, Long> bySeverity) {}
}
