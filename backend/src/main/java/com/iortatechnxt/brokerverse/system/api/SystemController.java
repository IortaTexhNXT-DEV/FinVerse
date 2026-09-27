package com.iortatechnxt.brokerverse.system.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.system.api.dto.JobResponse;
import com.iortatechnxt.brokerverse.system.api.dto.JobRunResponse;
import com.iortatechnxt.brokerverse.system.api.dto.ParameterResponse;
import com.iortatechnxt.brokerverse.system.api.dto.ParameterUpdateRequest;
import com.iortatechnxt.brokerverse.system.api.dto.SessionPolicyResponse;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import com.iortatechnxt.brokerverse.system.service.SecurityParameterApprovals;
import com.iortatechnxt.brokerverse.system.service.SystemInfoService;
import com.iortatechnxt.brokerverse.system.service.SystemInfoService.About;
import com.iortatechnxt.brokerverse.system.service.SystemInfoService.ConfigEntry;
import com.iortatechnxt.brokerverse.system.service.SystemInfoService.SystemInfo;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * System administration API: business parameters, configuration view, job monitor and application
 * information. About and session policy are available to every signed-in user.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

  private static final String MONITOR =
      "hasAnyAuthority('SYSTEM_MONITOR','SYSTEM_PARAMETER_MANAGE')";
  private static final String MANAGE = "hasAuthority('SYSTEM_PARAMETER_MANAGE')";
  private static final String PARAMETERS =
      "hasAnyAuthority('SYSTEM_MONITOR','SYSTEM_PARAMETER_MANAGE','SECURITY_PARAMETER_APPROVE')";
  private static final String APPROVE = "hasAuthority('SECURITY_PARAMETER_APPROVE')";
  private static final int DEFAULT_TIMEOUT_MINUTES = 30;
  private static final int WARNING_SECONDS = 60;
  private static final int SECONDS_PER_MINUTE = 60;
  private static final int DEFAULT_IDLE_WARNING_MINUTES = 15;
  private static final int DEFAULT_EXPIRY_WARNING_MINUTES = 30;
  private static final int DEFAULT_HISTORY_DAYS = 90;
  private static final int MAX_PAGE_SIZE = 200;

  private final SystemParameterService parameters;
  private final SecurityParameterApprovals approvals;
  private final SystemInfoService info;
  private final JobRegistry jobs;
  private final JobRunService runs;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param parameters parameter service
   * @param approvals changes of the parameters (second approval of the security parameters)
   * @param info application information
   * @param jobs job registry
   * @param runs job run history
   * @param clock clock
   */
  public SystemController(
      SystemParameterService parameters,
      SecurityParameterApprovals approvals,
      SystemInfoService info,
      JobRegistry jobs,
      JobRunService runs,
      Clock clock) {
    this.parameters = parameters;
    this.approvals = approvals;
    this.info = info;
    this.jobs = jobs;
    this.runs = runs;
    this.clock = clock;
  }

  /**
   * Lists business parameters.
   *
   * @return parameters
   */
  @GetMapping("/parameters")
  @PreAuthorize(PARAMETERS)
  public List<ParameterResponse> parameters() {
    return parameters.list().stream().map(ParameterResponse::from).toList();
  }

  /**
   * Changes a business parameter; a security parameter waits for a second approval.
   *
   * @param key key
   * @param request new value
   * @return parameter
   */
  @PutMapping("/parameters/{key}")
  @PreAuthorize(MANAGE)
  public ParameterResponse updateParameter(
      @PathVariable String key, @Valid @RequestBody ParameterUpdateRequest request) {
    return ParameterResponse.from(approvals.change(key, request.value()));
  }

  /**
   * Approves the change of a security parameter (not by its requester).
   *
   * @param key key
   * @return parameter
   */
  @PostMapping("/parameters/{key}/approve")
  @PreAuthorize(APPROVE)
  public ParameterResponse approveParameter(@PathVariable String key) {
    return ParameterResponse.from(approvals.approve(key));
  }

  /**
   * Rejects the change of a security parameter; its requester may withdraw it.
   *
   * @param key key
   * @return parameter
   */
  @PostMapping("/parameters/{key}/reject")
  @PreAuthorize("hasAnyAuthority('SECURITY_PARAMETER_APPROVE','SYSTEM_PARAMETER_MANAGE')")
  public ParameterResponse rejectParameter(@PathVariable String key) {
    return ParameterResponse.from(approvals.reject(key));
  }

  /**
   * Read-only non-secret configuration.
   *
   * @return configuration entries
   */
  @GetMapping("/configuration")
  @PreAuthorize(MONITOR)
  public List<ConfigEntry> configuration() {
    return info.configuration();
  }

  /**
   * Application information (version, migration level, health).
   *
   * @return information
   */
  @GetMapping("/info")
  @PreAuthorize(MONITOR)
  public SystemInfo info() {
    return info.info();
  }

  /**
   * Product and version for the About dialog (every signed-in user).
   *
   * @return about information
   */
  @GetMapping("/about")
  @PreAuthorize("isAuthenticated()")
  public About about() {
    return info.about();
  }

  /**
   * Session policy for the web client (every signed-in user).
   *
   * @return policy
   */
  @GetMapping("/session-policy")
  @PreAuthorize("isAuthenticated()")
  public SessionPolicyResponse sessionPolicy() {
    int timeout =
        parameters.intValue(
            SystemParameterService.SESSION_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES);
    int idleWarning =
        parameters.intValue(
            SystemParameterService.SESSION_IDLE_WARNING_MINUTES, DEFAULT_IDLE_WARNING_MINUTES);
    int warningSeconds =
        idleWarning < timeout
            ? Math.max(WARNING_SECONDS, (timeout - idleWarning) * SECONDS_PER_MINUTE)
            : WARNING_SECONDS;
    return new SessionPolicyResponse(
        timeout,
        warningSeconds,
        parameters.intValue(
            SystemParameterService.SESSION_EXPIRY_WARNING_MINUTES, DEFAULT_EXPIRY_WARNING_MINUTES));
  }

  /**
   * Job monitor: every registered job with last and next run.
   *
   * @return jobs
   */
  @GetMapping("/jobs")
  @PreAuthorize(MONITOR)
  public List<JobResponse> jobs() {
    return jobs.statuses().stream().map(JobResponse::from).toList();
  }

  /**
   * Job run history within the configured history window.
   *
   * @param job job name filter
   * @param page page index
   * @param size page size
   * @return runs
   */
  @GetMapping("/jobs/runs")
  @PreAuthorize(MONITOR)
  public PageResponse<JobRunResponse> runs(
      @RequestParam(required = false) String job,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    int days = parameters.intValue(SystemParameterService.JOB_HISTORY_DAYS, DEFAULT_HISTORY_DAYS);
    return PageResponse.of(
        runs.history(
            job == null || job.isBlank() ? null : job,
            clock.instant().minus(Duration.ofDays(days)),
            PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        JobRunResponse::from);
  }

  /**
   * Runs a job now.
   *
   * @param name job name
   * @return finished run
   */
  @PostMapping("/jobs/{name}/run")
  @PreAuthorize(MANAGE)
  public JobRunResponse runJob(@PathVariable String name) {
    return JobRunResponse.from(jobs.run(name, JobTrigger.MANUAL));
  }
}
