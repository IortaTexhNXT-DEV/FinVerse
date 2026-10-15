package com.iortatechnxt.brokerverse.identity.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.identity.api.dto.DirectoryProfileResponse;
import com.iortatechnxt.brokerverse.identity.api.dto.IdentityEventResponse;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.identity.service.IdentityEventQuery;
import com.iortatechnxt.brokerverse.identity.service.IdentityMonitoring;
import com.iortatechnxt.brokerverse.identity.service.IdentityProperties;
import com.iortatechnxt.brokerverse.identity.service.IdentityReconciliationJob;
import com.iortatechnxt.brokerverse.identity.service.IdentitySyncService;
import com.iortatechnxt.brokerverse.system.api.dto.JobRunResponse;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identity synchronisation for the User Access Administrator (BDOI FRS FRUM.002.01 to FRUM.003.03):
 * the state of the connection with UIDM-ISC, the events received with their outcome, the
 * reprocessing or dismissal of a refused one, the reconciliation mismatches with their resolution
 * and Synchronise Now, the on-demand synchronisation of a user, the directory details of a user,
 * and the creation of a user from an existing and active Enterprise SSO account.
 */
@RestController
@RequestMapping("/api/v1/admin/identity")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class IdentityAdminController {

  private final IdentitySyncService sync;
  private final IdentityEventQuery query;
  private final IdentityProperties properties;
  private final IdentityMonitoring monitoring;
  private final JobRegistry jobs;

  /**
   * Creates the controller.
   *
   * @param sync synchronisation
   * @param query events
   * @param properties connection (simulator or platform)
   * @param monitoring connection status, dismissals and reconciliation
   * @param jobs background jobs (the reconciliation)
   */
  public IdentityAdminController(
      IdentitySyncService sync,
      IdentityEventQuery query,
      IdentityProperties properties,
      IdentityMonitoring monitoring,
      JobRegistry jobs) {
    this.monitoring = monitoring;
    this.jobs = jobs;
    this.sync = sync;
    this.query = query;
    this.properties = properties;
  }

  /**
   * How users are provisioned on this deployment.
   *
   * @return settings
   */
  @GetMapping("/settings")
  public Settings settings() {
    return new Settings(sync.enabled(), sync.directoryName(), properties.simulator());
  }

  /**
   * The events received, newest first.
   *
   * @param status outcome
   * @param windowsId part of the Windows ID
   * @param from first date received
   * @param to last date received
   * @param open only the events not dismissed
   * @param page page
   * @param size page size
   * @return events
   */
  @GetMapping("/events")
  @SuppressWarnings("java:S107") // filters of the screen
  public PageResponse<IdentityEventResponse> events(
      @RequestParam(required = false) IdentityEventStatus status,
      @RequestParam(required = false) String windowsId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "false") boolean open,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        query.search(new IdentityEventQuery.Filter(status, windowsId, from, to, open), page, size),
        IdentityEventResponse::from);
  }

  /**
   * The state of the connection with UIDM-ISC.
   *
   * @return status
   */
  @GetMapping("/status")
  public IdentityMonitoring.Status status() {
    return monitoring.status();
  }

  /**
   * Compares every user with the UIDM-ISC directory now (Synchronise Now; Run Now of the
   * reconciliation) and records the mismatches.
   *
   * @return the run
   */
  @PostMapping("/reconciliation/run")
  public JobRunResponse synchroniseNow() {
    return JobRunResponse.from(jobs.run(IdentityReconciliationJob.JOB_NAME, JobTrigger.MANUAL));
  }

  /**
   * The reconciliation mismatches, newest first.
   *
   * @param open only the mismatches not resolved
   * @return mismatches
   */
  @GetMapping("/reconciliation")
  public List<IdentityMonitoring.Mismatch> mismatches(
      @RequestParam(defaultValue = "true") boolean open) {
    return monitoring.mismatches(open);
  }

  /**
   * Resolves a mismatch with a remark.
   *
   * @param id mismatch
   * @param request remark
   */
  @PostMapping("/reconciliation/{id}/resolve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resolve(@PathVariable Long id, @Valid @RequestBody Remark request) {
    monitoring.resolve(id, request.remark());
  }

  /**
   * Dismisses a refused or failed event with a remark.
   *
   * @param id event
   * @param request remark
   */
  @PostMapping("/events/{id}/dismiss")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dismiss(@PathVariable Long id, @Valid @RequestBody Remark request) {
    monitoring.dismiss(id, request.remark());
  }

  /**
   * Processes a refused or failed event again.
   *
   * @param id event
   * @return the event
   */
  @PostMapping("/events/{id}/reprocess")
  public IdentityEventResponse reprocess(@PathVariable Long id) {
    return IdentityEventResponse.from(sync.reprocess(id));
  }

  /**
   * Synchronises a user now from the Enterprise SSO directory.
   *
   * @param username user
   * @return the event of the synchronisation
   */
  @PostMapping("/users/{username}/sync")
  public IdentityEventResponse synchronise(@PathVariable String username) {
    return IdentityEventResponse.from(sync.synchronise(username));
  }

  /**
   * The directory details of a user and the latest synchronisation.
   *
   * @param username user
   * @return details, 204 when the user was never synchronised
   */
  @GetMapping("/users/{username}/profile")
  @PreAuthorize("hasAnyAuthority('USER_MANAGE','USER_VIEW')")
  public ResponseEntity<DirectoryProfileResponse> profile(@PathVariable String username) {
    return sync.profile(username)
        .map(p -> ResponseEntity.ok(DirectoryProfileResponse.from(p)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * The remark of a dismissal or a resolution.
   *
   * @param remark remark
   */
  public record Remark(@NotBlank @Size(max = 500) String remark) {}

  /**
   * How users are provisioned.
   *
   * @param provisioning whether the events of the Enterprise SSO platform / UIDM-ISC apply
   * @param directoryName the directory in use
   * @param simulator whether the simulator replaces the platform (SIT and UAT)
   */
  public record Settings(boolean provisioning, String directoryName, boolean simulator) {}
}
