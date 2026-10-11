package com.iortatechnxt.brokerverse.identity.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.identity.api.dto.DirectoryAccountDto;
import com.iortatechnxt.brokerverse.identity.api.dto.DirectoryProfileResponse;
import com.iortatechnxt.brokerverse.identity.api.dto.IdentityEventResponse;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.identity.service.IdentityEventQuery;
import com.iortatechnxt.brokerverse.identity.service.IdentityProperties;
import com.iortatechnxt.brokerverse.identity.service.IdentitySyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identity synchronisation for the System Administrator (BDOI FRS FRUM.002.01 to FRUM.003.03): the
 * events received with their outcome and the reprocessing of a refused one, the on-demand
 * synchronisation of a user, the directory details of a user, and the creation of a user from an
 * existing and active Enterprise SSO account.
 */
@RestController
@RequestMapping("/api/v1/admin/identity")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class IdentityAdminController {

  private final IdentitySyncService sync;
  private final IdentityEventQuery query;
  private final IdentityProperties properties;

  /**
   * Creates the controller.
   *
   * @param sync synchronisation
   * @param query events
   * @param properties connection (simulator or platform)
   */
  public IdentityAdminController(
      IdentitySyncService sync, IdentityEventQuery query, IdentityProperties properties) {
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
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        query.search(new IdentityEventQuery.Filter(status, windowsId, from, to), page, size),
        IdentityEventResponse::from);
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
  public ResponseEntity<DirectoryProfileResponse> profile(@PathVariable String username) {
    return sync.profile(username)
        .map(p -> ResponseEntity.ok(DirectoryProfileResponse.from(p)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * The details of an active Enterprise SSO account, before a user is created from it.
   *
   * @param windowsId Windows ID
   * @return the account
   */
  @GetMapping("/directory")
  public DirectoryAccountDto lookup(@RequestParam String windowsId) {
    return DirectoryAccountDto.from(sync.activeAccount(windowsId));
  }

  /**
   * Creates a user from an existing and active Enterprise SSO account.
   *
   * @param request Windows ID
   * @return the event of the creation
   */
  @PostMapping("/users/from-directory")
  public IdentityEventResponse createFromDirectory(@Valid @RequestBody FromDirectory request) {
    return IdentityEventResponse.from(sync.createFromDirectory(request.windowsId()));
  }

  /**
   * Request of a creation from the directory.
   *
   * @param windowsId Windows ID of the account
   */
  public record FromDirectory(@NotBlank String windowsId) {}

  /**
   * How users are provisioned.
   *
   * @param provisioning whether the events of the Enterprise SSO platform / UIDM-ISC apply
   * @param directoryName the directory in use
   * @param simulator whether the simulator replaces the platform (SIT and UAT)
   */
  public record Settings(boolean provisioning, String directoryName, boolean simulator) {}
}
