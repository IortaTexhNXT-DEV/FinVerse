package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Status;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Values;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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

/**
 * Remittance Settings (FRS BRD-02 FRID.RMT.003): the extraction schedule, the shared folder of each
 * remittance type and the naming of the extract files; a change is proposed by a maker and approved
 * or rejected by the Remittance Team Leader.
 */
@OwnedRecord(RemittanceSettingChange.class)
@RestController
@RequestMapping("/api/v1/remittance/settings")
public class RemittanceSettingsController {

  private static final String READ =
      "hasAnyAuthority('REMIT_SETTINGS_MAINTAIN', 'REMIT_SETTINGS_APPROVE', 'REMIT_PROCESS',"
          + " 'REMIT_APPROVE')";

  private final RemittanceSettingsService settings;

  /**
   * Creates the controller.
   *
   * @param settings settings
   */
  public RemittanceSettingsController(RemittanceSettingsService settings) {
    this.settings = settings;
  }

  /**
   * The settings in force and the changes, newest first.
   *
   * @param companyId company
   * @return settings
   */
  @GetMapping
  @PreAuthorize(READ)
  public SettingsResponse get(@RequestParam Long companyId) {
    return new SettingsResponse(
        settings.current(companyId),
        settings.history(companyId).stream().map(ChangeResponse::from).toList());
  }

  /**
   * Proposes a change (maker).
   *
   * @param request settings and reason
   * @return the pending change
   */
  @PostMapping("/changes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('REMIT_SETTINGS_MAINTAIN')")
  public ChangeResponse propose(@Valid @RequestBody ChangeRequest request) {
    return ChangeResponse.from(
        settings.propose(request.companyId(), request.values(), request.reason().strip()));
  }

  /**
   * Approves a pending change (Remittance Team Leader).
   *
   * @param id change
   * @param request comment
   * @return the change
   */
  @PostMapping("/changes/{id}/approve")
  @PreAuthorize("hasAuthority('REMIT_SETTINGS_APPROVE')")
  public ChangeResponse approve(@PathVariable Long id, @RequestBody DecisionRequest request) {
    return ChangeResponse.from(settings.decide(id, true, request.comment()));
  }

  /**
   * Rejects a pending change (Remittance Team Leader).
   *
   * @param id change
   * @param request comment
   * @return the change
   */
  @PostMapping("/changes/{id}/reject")
  @PreAuthorize("hasAuthority('REMIT_SETTINGS_APPROVE')")
  public ChangeResponse reject(@PathVariable Long id, @RequestBody DecisionRequest request) {
    return ChangeResponse.from(settings.decide(id, false, request.comment()));
  }

  /**
   * A proposed change.
   *
   * @param companyId company
   * @param values settings
   * @param reason reason
   */
  public record ChangeRequest(
      @NotNull Long companyId,
      @NotNull @Valid Values values,
      @NotBlank @Size(max = 250) String reason) {}

  /**
   * A decision.
   *
   * @param comment comment, may be null
   */
  public record DecisionRequest(@Size(max = 250) String comment) {}

  /**
   * The settings in force and the changes.
   *
   * @param current settings in force
   * @param changes changes, newest first
   */
  public record SettingsResponse(Values current, List<ChangeResponse> changes) {

    /** Defensive copy. */
    public SettingsResponse {
      changes = List.copyOf(changes);
    }
  }

  /**
   * A change.
   *
   * @param id id
   * @param values proposed settings
   * @param reason reason
   * @param status status
   * @param requestedBy maker
   * @param requestedAt time
   * @param decidedBy checker
   * @param decidedAt time
   * @param decisionComment comment
   */
  public record ChangeResponse(
      Long id,
      Values values,
      String reason,
      Status status,
      String requestedBy,
      Instant requestedAt,
      String decidedBy,
      Instant decidedAt,
      String decisionComment) {

    /**
     * Maps a change.
     *
     * @param c change
     * @return DTO
     */
    public static ChangeResponse from(RemittanceSettingChange c) {
      return new ChangeResponse(
          c.getId(),
          c.values(),
          c.getReason(),
          c.getStatus(),
          c.getRequestedBy(),
          c.getRequestedAt(),
          c.getDecidedBy(),
          c.getDecidedAt(),
          c.getDecisionComment());
    }
  }
}
