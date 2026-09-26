package com.iortatechnxt.brokerverse.screening.config.api;

import com.iortatechnxt.brokerverse.screening.common.api.DecisionRequest;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.config.api.dto.ConfigVersionDetail;
import com.iortatechnxt.brokerverse.screening.config.api.dto.ConfigVersionDto;
import com.iortatechnxt.brokerverse.screening.config.api.dto.NewDraftRequest;
import com.iortatechnxt.brokerverse.screening.config.api.dto.SaveDraftRequest;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigChange;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigDecisionService;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionService;
import jakarta.validation.Valid;
import java.util.List;
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
 * Screening configuration versions (SNSRP-101-109; FR-SS-010 to 019): list, detail with rows and
 * changes, draft, save, submit, approve, reject and discard. Makers need SCR_CONFIG_MAINTAIN,
 * checkers SCR_CONFIG_APPROVE.
 */
@RestController
@RequestMapping("/api/v1/screening/config")
public class ConfigVersionController {

  private final ConfigVersionService service;
  private final ConfigDecisionService decisions;

  /**
   * Creates the controller.
   *
   * @param service configuration versions
   * @param decisions checker decisions
   */
  public ConfigVersionController(ConfigVersionService service, ConfigDecisionService decisions) {
    this.service = service;
    this.decisions = decisions;
  }

  /**
   * The versions of a type, newest first (all template types for TEMPLATE).
   *
   * @param companyId company
   * @param type type
   * @return versions
   */
  @GetMapping("/versions")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_ACCESS)
  public List<ConfigVersionDto> versions(
      @RequestParam Long companyId, @RequestParam ConfigType type) {
    return service.versions(companyId, type).stream().map(ConfigVersionDto::from).toList();
  }

  /**
   * One version with its rows and changes.
   *
   * @param id version id
   * @return detail
   */
  @GetMapping("/versions/{id}")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_ACCESS)
  public ConfigVersionDetail version(@PathVariable Long id) {
    return detail(service.get(id));
  }

  /**
   * The before / after changes of a version.
   *
   * @param id version id
   * @return changes
   */
  @GetMapping("/versions/{id}/changes")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_ACCESS)
  public List<ConfigChange> changes(@PathVariable Long id) {
    return service.changes(id);
  }

  /**
   * Opens the draft of a type: the existing one or a copy of the latest approved version.
   *
   * @param request company, type and template type
   * @return the draft
   */
  @PostMapping("/drafts")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_MAINTAIN)
  public ConfigVersionDetail newDraft(@Valid @RequestBody NewDraftRequest request) {
    return detail(service.newDraft(request.companyId(), request.type(), request.scope()));
  }

  /**
   * Saves a draft.
   *
   * @param id draft id
   * @param request header and rows
   * @return the draft
   */
  @PutMapping("/versions/{id}")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_MAINTAIN)
  public ConfigVersionDetail save(
      @PathVariable Long id, @Valid @RequestBody SaveDraftRequest request) {
    return detail(
        service.saveDraft(id, request.effectiveFrom(), request.changeNote(), request.content()));
  }

  /**
   * Submits a draft for approval.
   *
   * @param id draft id
   * @return the pending version
   */
  @PostMapping("/versions/{id}/submit")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_MAINTAIN)
  public ConfigVersionDetail submit(@PathVariable Long id) {
    return detail(service.submit(id));
  }

  /**
   * Discards a draft (kept as REJECTED "withdrawn by maker").
   *
   * @param id draft id
   * @return the version
   */
  @PostMapping("/versions/{id}/withdraw")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_MAINTAIN)
  public ConfigVersionDto withdraw(@PathVariable Long id) {
    return ConfigVersionDto.from(service.withdraw(id));
  }

  /**
   * Approves a pending version.
   *
   * @param id version id
   * @return the active version
   */
  @PostMapping("/versions/{id}/approve")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_APPROVE)
  public ConfigVersionDto approve(@PathVariable Long id) {
    return ConfigVersionDto.from(decisions.approve(id));
  }

  /**
   * Rejects a pending version with a reason.
   *
   * @param id version id
   * @param request reason
   * @return the rejected version
   */
  @PostMapping("/versions/{id}/reject")
  @PreAuthorize(ScreeningPermissions.HAS_CONFIG_APPROVE)
  public ConfigVersionDto reject(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return ConfigVersionDto.from(decisions.reject(id, request.remarks()));
  }

  private ConfigVersionDetail detail(ConfigVersion v) {
    return new ConfigVersionDetail(
        ConfigVersionDto.from(v), service.content(v), service.changes(v.getId()));
  }
}
