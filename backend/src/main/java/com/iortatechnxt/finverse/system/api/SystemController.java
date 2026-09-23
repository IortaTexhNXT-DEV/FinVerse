package com.iortatechnxt.finverse.system.api;

import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.system.api.dto.JobResponse;
import com.iortatechnxt.finverse.system.api.dto.JobRunResponse;
import com.iortatechnxt.finverse.system.api.dto.ParameterResponse;
import com.iortatechnxt.finverse.system.api.dto.ParameterUpdateRequest;
import com.iortatechnxt.finverse.system.api.dto.SessionPolicyResponse;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import com.iortatechnxt.finverse.system.service.JobRegistry;
import com.iortatechnxt.finverse.system.service.JobRunService;
import com.iortatechnxt.finverse.system.service.SystemInfoService;
import com.iortatechnxt.finverse.system.service.SystemInfoService.About;
import com.iortatechnxt.finverse.system.service.SystemInfoService.ConfigEntry;
import com.iortatechnxt.finverse.system.service.SystemInfoService.SystemInfo;
import com.iortatechnxt.finverse.system.service.SystemParameterService;
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
  private static final int DEFAULT_TIMEOUT_MINUTES = 30;
  private static final int WARNING_SECONDS = 60;
  private static final int DEFAULT_HISTORY_DAYS = 90;
  private static final int MAX_PAGE_SIZE = 200;

  private final SystemParameterService parameters;
  private final SystemInfoService info;
  private final JobRegistry jobs;
  private final JobRunService runs;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param parameters parameter service
   * @param info application information
   * @param jobs job registry
   * @param runs job run history
   * @param clock clock
   */
  public SystemController(
      SystemParameterService parameters,
      SystemInfoService info,
      JobRegistry jobs,
      JobRunService runs,
      Clock clock) {
    this.parameters = parameters;
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
  @PreAuthorize(MONITOR)
  public List<ParameterResponse> parameters() {
    return parameters.list().stream().map(ParameterResponse::from).toList();
  }

  /**
   * Changes a business parameter.
   *
   * @param key key
   * @param request new value
   * @return parameter
   */
  @PutMapping("/parameters/{key}")
  @PreAuthorize(MANAGE)
  public ParameterResponse updateParameter(
      @PathVariable String key, @Valid @RequestBody ParameterUpdateRequest request) {
    return ParameterResponse.from(parameters.update(key, request.value()));
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
    return new SessionPolicyResponse(
        parameters.intValue(
            SystemParameterService.SESSION_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES),
        WARNING_SECONDS);
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
