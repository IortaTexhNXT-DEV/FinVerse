package com.iortatechnxt.brokerverse.submitted.setup.api;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRule;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.Control;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.ControlView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.InsurerView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.LetterView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.LimitView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.MatrixView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.ScopeRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.ScopeView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.SourceRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.SourceView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.StatusMapRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.SetupDtos.StatusMapView;
import com.iortatechnxt.brokerverse.submitted.setup.service.SetupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
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
 * Submitted Policies setup (FR-SP-080 to 084): limit, insurer and letter rules and the approval
 * matrix (maker and checker), the source register, the legacy status map and the user scopes.
 */
@RestController("sbmSetupController")
@RequestMapping("/api/v1/submitted/setup")
public class SetupController {

  private static final String READ =
      "hasAnyAuthority('SBM_RULE_MAINTAIN','SBM_RULE_APPROVE','SBM_VIEW')";
  private static final String MAINTAIN = "hasAuthority('SBM_RULE_MAINTAIN')";
  private static final String APPROVE = "hasAuthority('SBM_RULE_APPROVE')";

  private final SetupService setup;

  /**
   * Creates the controller.
   *
   * @param setup setup
   */
  public SetupController(SetupService setup) {
    this.setup = setup;
  }

  /**
   * Limit rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/limits")
  @PreAuthorize(READ)
  public List<LimitView> limits(@RequestParam Long companyId) {
    return setup.limitRules(companyId).stream().map(LimitView::from).toList();
  }

  /**
   * Adds a limit rule.
   *
   * @param companyId company
   * @param l limits
   * @return rule
   */
  @PostMapping("/limits")
  @PreAuthorize(MAINTAIN)
  public LimitView addLimit(@RequestParam Long companyId, @RequestBody SbmLimitRule.Limits l) {
    return LimitView.from(setup.saveLimitRule(companyId, null, l));
  }

  /**
   * Changes a limit rule.
   *
   * @param companyId company
   * @param id rule
   * @param l limits
   * @return rule
   */
  @PutMapping("/limits/{id}")
  @PreAuthorize(MAINTAIN)
  public LimitView changeLimit(
      @RequestParam Long companyId, @PathVariable Long id, @RequestBody SbmLimitRule.Limits l) {
    return LimitView.from(setup.saveLimitRule(companyId, id, l));
  }

  /**
   * Insurer rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/insurers")
  @PreAuthorize(READ)
  public List<InsurerView> insurers(@RequestParam Long companyId) {
    return setup.insurerRules(companyId).stream().map(InsurerView::from).toList();
  }

  /**
   * Adds or changes an insurer rule.
   *
   * @param companyId company
   * @param id rule, absent for a new one
   * @param row rule
   * @return rule
   */
  @PostMapping("/insurers")
  @PreAuthorize(MAINTAIN)
  public InsurerView saveInsurer(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long id,
      @RequestBody SbmInsurerRule.Row row) {
    return InsurerView.from(setup.saveInsurerRule(companyId, id, row));
  }

  /**
   * Letter rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/letters")
  @PreAuthorize(READ)
  public List<LetterView> letters(@RequestParam Long companyId) {
    return setup.letterRules(companyId).stream().map(LetterView::from).toList();
  }

  /**
   * Adds or changes a letter rule.
   *
   * @param companyId company
   * @param id rule, absent for a new one
   * @param row rule
   * @return rule
   */
  @PostMapping("/letters")
  @PreAuthorize(MAINTAIN)
  public LetterView saveLetter(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long id,
      @RequestBody SbmLetterRule.Row row) {
    return LetterView.from(setup.saveLetterRule(companyId, id, row));
  }

  /**
   * The approval matrix.
   *
   * @param companyId company
   * @return levels
   */
  @GetMapping("/matrix")
  @PreAuthorize(READ)
  public List<MatrixView> matrix(@RequestParam Long companyId) {
    return setup.matrix(companyId).stream().map(MatrixView::from).toList();
  }

  /**
   * Adds or changes a level of the matrix.
   *
   * @param companyId company
   * @param id level, absent for a new one
   * @param row level
   * @return level
   */
  @PostMapping("/matrix")
  @PreAuthorize(MAINTAIN)
  public MatrixView saveMatrix(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long id,
      @RequestBody SbmApprovalMatrix.Row row) {
    return MatrixView.from(setup.saveMatrix(companyId, id, row));
  }

  /**
   * Approves a pending setup record.
   *
   * @param kind limits, insurers, letters or matrix
   * @param id record
   * @return state
   */
  @PostMapping("/{kind}/{id}/authorize")
  @PreAuthorize(APPROVE)
  public ControlView authorize(@PathVariable String kind, @PathVariable Long id) {
    return view(id, setup.authorize(kind(kind), id));
  }

  /**
   * Deactivates a setup record.
   *
   * @param kind limits, insurers, letters or matrix
   * @param id record
   * @return state
   */
  @PostMapping("/{kind}/{id}/deactivate")
  @PreAuthorize(MAINTAIN)
  public ControlView deactivate(@PathVariable String kind, @PathVariable Long id) {
    return view(id, setup.deactivate(kind(kind), id));
  }

  /**
   * The source register.
   *
   * @return sources
   */
  @GetMapping("/sources")
  @PreAuthorize(READ)
  public List<SourceView> sources() {
    return setup.sources().stream().map(SourceView::from).toList();
  }

  /**
   * Changes a source.
   *
   * @param id source
   * @param r change
   * @return source
   */
  @PutMapping("/sources/{id}")
  @PreAuthorize(MAINTAIN)
  public SourceView saveSource(@PathVariable Long id, @Valid @RequestBody SourceRequest r) {
    return SourceView.from(setup.saveSource(id, r.name(), r.mandatoryFields(), r.active()));
  }

  /**
   * The legacy status map.
   *
   * @return mappings
   */
  @GetMapping("/status-map")
  @PreAuthorize(READ)
  public List<StatusMapView> statusMap() {
    return setup.statusMap().stream().map(StatusMapView::from).toList();
  }

  /**
   * Adds or changes a mapping.
   *
   * @param r mapping
   * @return mapping
   */
  @PostMapping("/status-map")
  @PreAuthorize(MAINTAIN)
  public StatusMapView saveStatusMap(@Valid @RequestBody StatusMapRequest r) {
    return StatusMapView.from(setup.saveStatusMap(r.legacyStatus(), r.status(), r.bucket()));
  }

  /**
   * User scopes.
   *
   * @param companyId company
   * @return scopes
   */
  @GetMapping("/scopes")
  @PreAuthorize(READ)
  public List<ScopeView> scopes(@RequestParam Long companyId) {
    return setup.scopes(companyId).stream().map(ScopeView::from).toList();
  }

  /**
   * Saves the scope of a user.
   *
   * @param companyId company
   * @param r scope
   * @return scope
   */
  @PostMapping("/scopes")
  @PreAuthorize(MAINTAIN)
  public ScopeView saveScope(@RequestParam Long companyId, @Valid @RequestBody ScopeRequest r) {
    return ScopeView.from(
        setup.saveScope(
            companyId,
            r.username(),
            r.segments() == null ? List.of() : r.segments(),
            r.ownRecordsOnly()));
  }

  private static String kind(String path) {
    return switch (path.toLowerCase(Locale.ROOT)) {
      case "limits" -> "LIMIT";
      case "insurers" -> "INSURER";
      case "letters" -> "LETTER";
      case "matrix" -> "MATRIX";
      default -> path.toUpperCase(Locale.ROOT);
    };
  }

  private static ControlView view(Long id, AuthorizableEntity e) {
    return new ControlView(id, Control.of(e));
  }
}
