package com.iortatechnxt.brokerverse.renewal.setup.api;

import com.iortatechnxt.brokerverse.renewal.domain.BucketRule;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionRule;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.renewal.setup.api.dto.SetupDtos.CheckSettingView;
import com.iortatechnxt.brokerverse.renewal.setup.api.dto.SetupDtos.PackageMapView;
import com.iortatechnxt.brokerverse.renewal.setup.api.dto.SetupDtos.RiskCodeView;
import com.iortatechnxt.brokerverse.renewal.setup.api.dto.SetupDtos.VersionView;
import com.iortatechnxt.brokerverse.renewal.setup.service.PackageMapService;
import com.iortatechnxt.brokerverse.renewal.setup.service.RenewalSetupService;
import com.iortatechnxt.brokerverse.renewal.setup.service.RuleVersionService;
import com.iortatechnxt.brokerverse.renewal.setup.service.RuleVersionService.Header;
import com.iortatechnxt.brokerverse.renewal.setup.service.RuleVersionService.Step;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
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
 * Renewal Setup (FR-RN-080-083): non-renewable risk codes, check settings, bucket rules, the
 * decision matrix and the package map of migrated policies. Every change waits for a checker.
 */
@RestController
@RequestMapping("/api/v1/renewal/setup")
public class RenewalSetupController {

  private static final String VIEW = "hasAnyAuthority('RNW_SETUP','RNW_VIEW')";
  private static final String SETUP = "hasAuthority('RNW_SETUP')";
  private static final String MAP_VIEW = "hasAnyAuthority('RNW_SETUP','RNW_PACKAGE_REMAP')";
  private static final String MAP = "hasAnyAuthority('RNW_SETUP','RNW_PACKAGE_REMAP')";

  private final RenewalSetupService setup;
  private final RuleVersionService versions;
  private final PackageMapService map;

  /**
   * Creates the controller.
   *
   * @param setup risk codes and check settings
   * @param versions bucket rules and decision matrix
   * @param map package map
   */
  public RenewalSetupController(
      RenewalSetupService setup, RuleVersionService versions, PackageMapService map) {
    this.setup = setup;
    this.versions = versions;
    this.map = map;
  }

  /**
   * Non-renewable risk codes.
   *
   * @param companyId company
   * @return codes
   */
  @GetMapping("/risk-codes")
  @PreAuthorize(VIEW)
  public List<RiskCodeView> riskCodes(@RequestParam Long companyId) {
    return setup.riskCodes(companyId).stream().map(RiskCodeView::of).toList();
  }

  /**
   * Adds a non-renewable risk code.
   *
   * @param companyId company
   * @param data code
   * @return code
   */
  @PostMapping("/risk-codes")
  @PreAuthorize(SETUP)
  public RiskCodeView createRiskCode(
      @RequestParam Long companyId, @RequestBody NonRenewableRiskCode.Data data) {
    return RiskCodeView.of(setup.createRiskCode(companyId, data));
  }

  /**
   * Changes a non-renewable risk code.
   *
   * @param companyId company
   * @param id code
   * @param data code
   * @return code
   */
  @PutMapping("/risk-codes/{id}")
  @PreAuthorize(SETUP)
  public RiskCodeView updateRiskCode(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @RequestBody NonRenewableRiskCode.Data data) {
    return RiskCodeView.of(setup.updateRiskCode(companyId, id, data));
  }

  /**
   * Authorizes or deactivates a non-renewable risk code.
   *
   * @param companyId company
   * @param id code
   * @param action AUTHORIZE or DEACTIVATE
   * @return code
   */
  @PostMapping("/risk-codes/{id}/{action}")
  @PreAuthorize(SETUP)
  public RiskCodeView riskCodeAction(
      @RequestParam Long companyId, @PathVariable Long id, @PathVariable RecordAction action) {
    return RiskCodeView.of(
        action == RecordAction.AUTHORIZE
            ? setup.authorizeRiskCode(companyId, id)
            : setup.deactivateRiskCode(companyId, id));
  }

  /**
   * Check settings.
   *
   * @return settings
   */
  @GetMapping("/checks")
  @PreAuthorize(VIEW)
  public List<CheckSettingView> checks() {
    return setup.checkSettings().stream().map(CheckSettingView::of).toList();
  }

  /**
   * Changes a check setting.
   *
   * @param code check
   * @param request setting
   * @return setting
   */
  @PutMapping("/checks/{code}")
  @PreAuthorize(SETUP)
  public CheckSettingView updateCheck(
      @PathVariable String code, @Valid @RequestBody CheckSettingRequest request) {
    return CheckSettingView.of(
        setup.updateCheckSetting(code, request.active(), request.severity(), request.parameters()));
  }

  /**
   * Authorizes a check setting.
   *
   * @param code check
   * @return setting
   */
  @PostMapping("/checks/{code}/authorize")
  @PreAuthorize(SETUP)
  public CheckSettingView authorizeCheck(@PathVariable String code) {
    return CheckSettingView.of(setup.authorizeCheckSetting(code));
  }

  /**
   * Versions of the bucket rules.
   *
   * @param companyId company
   * @return versions, newest first
   */
  @GetMapping("/bucket-rules")
  @PreAuthorize(VIEW)
  public List<VersionView<BucketRule.Data>> bucketRules(@RequestParam Long companyId) {
    return versions.bucketRuleSets(companyId).stream().map(VersionView::ofBuckets).toList();
  }

  /**
   * Saves a draft of the bucket rules.
   *
   * @param companyId company
   * @param request draft
   * @return draft
   */
  @PostMapping("/bucket-rules")
  @PreAuthorize(SETUP)
  public VersionView<BucketRule.Data> saveBucketRules(
      @RequestParam Long companyId, @Valid @RequestBody VersionRequest<BucketRule.Data> request) {
    return VersionView.ofBuckets(
        versions.saveBucketRules(companyId, request.id(), request.header(), request.rules()));
  }

  /**
   * Submits, activates or rejects a version of the bucket rules.
   *
   * @param companyId company
   * @param id version
   * @param step step
   * @param request remarks
   * @return version
   */
  @PostMapping("/bucket-rules/{id}/{step}")
  @PreAuthorize(SETUP)
  public VersionView<BucketRule.Data> decideBucketRules(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @PathVariable Step step,
      @RequestBody(required = false) RemarksRequest request) {
    return VersionView.ofBuckets(versions.decideBucketRules(companyId, id, step, remarks(request)));
  }

  /**
   * Versions of the decision matrix.
   *
   * @param companyId company
   * @return versions, newest first
   */
  @GetMapping("/matrix")
  @PreAuthorize(VIEW)
  public List<VersionView<DecisionRule.Data>> matrices(@RequestParam Long companyId) {
    return versions.matrices(companyId).stream().map(VersionView::ofMatrix).toList();
  }

  /**
   * Saves a draft of the decision matrix.
   *
   * @param companyId company
   * @param request draft
   * @return draft
   */
  @PostMapping("/matrix")
  @PreAuthorize(SETUP)
  public VersionView<DecisionRule.Data> saveMatrix(
      @RequestParam Long companyId, @Valid @RequestBody VersionRequest<DecisionRule.Data> request) {
    return VersionView.ofMatrix(
        versions.saveMatrix(companyId, request.id(), request.header(), request.rules()));
  }

  /**
   * Submits, activates or rejects a version of the decision matrix.
   *
   * @param companyId company
   * @param id version
   * @param step step
   * @param request remarks
   * @return version
   */
  @PostMapping("/matrix/{id}/{step}")
  @PreAuthorize(SETUP)
  public VersionView<DecisionRule.Data> decideMatrix(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @PathVariable Step step,
      @RequestBody(required = false) RemarksRequest request) {
    return VersionView.ofMatrix(versions.decideMatrix(companyId, id, step, remarks(request)));
  }

  /**
   * The package map.
   *
   * @param companyId company
   * @return entries
   */
  @GetMapping("/package-map")
  @PreAuthorize(MAP_VIEW)
  public List<PackageMapView> packageMap(@RequestParam Long companyId) {
    return map.entries(companyId).stream().map(PackageMapView::of).toList();
  }

  /**
   * Adds an entry to the package map.
   *
   * @param companyId company
   * @param data entry
   * @return entry
   */
  @PostMapping("/package-map")
  @PreAuthorize(MAP)
  public PackageMapView createMapEntry(
      @RequestParam Long companyId, @RequestBody PackageMapEntry.Data data) {
    return PackageMapView.of(map.create(companyId, data, PackageMapService.SOURCE_SETUP));
  }

  /**
   * Changes an entry of the package map.
   *
   * @param companyId company
   * @param id entry
   * @param data entry
   * @return entry
   */
  @PutMapping("/package-map/{id}")
  @PreAuthorize(MAP)
  public PackageMapView updateMapEntry(
      @RequestParam Long companyId, @PathVariable Long id, @RequestBody PackageMapEntry.Data data) {
    return PackageMapView.of(map.update(companyId, id, data));
  }

  /**
   * Authorizes or deactivates an entry of the package map.
   *
   * @param companyId company
   * @param id entry
   * @param action AUTHORIZE or DEACTIVATE
   * @return entry
   */
  @PostMapping("/package-map/{id}/{action}")
  @PreAuthorize(MAP)
  public PackageMapView mapEntryAction(
      @RequestParam Long companyId, @PathVariable Long id, @PathVariable RecordAction action) {
    return PackageMapView.of(
        action == RecordAction.AUTHORIZE
            ? map.authorize(companyId, id)
            : map.deactivate(companyId, id));
  }

  private static String remarks(RemarksRequest request) {
    return request == null ? null : request.remarks();
  }

  /** Checker actions on a maintained record. */
  public enum RecordAction {
    /** Authorize. */
    AUTHORIZE,
    /** Deactivate. */
    DEACTIVATE
  }

  /**
   * A check setting.
   *
   * @param active active
   * @param severity severity
   * @param parameters parameters
   */
  public record CheckSettingRequest(
      boolean active, @NotNull CheckSeverity severity, String parameters) {}

  /**
   * A draft version.
   *
   * @param id draft, null for a new version
   * @param effectiveFrom effective date
   * @param description description
   * @param rules rules
   * @param <R> rule type
   */
  public record VersionRequest<R>(
      Long id, LocalDate effectiveFrom, String description, @NotNull List<R> rules) {

    Header header() {
      return new Header(effectiveFrom, description);
    }
  }

  /**
   * Remarks of a decision.
   *
   * @param remarks remarks
   */
  public record RemarksRequest(String remarks) {}
}
