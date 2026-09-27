package com.iortatechnxt.brokerverse.eb.setup.api;

import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRule;
import com.iortatechnxt.brokerverse.eb.setup.api.dto.SetupDtos.RequiredDocumentResponse;
import com.iortatechnxt.brokerverse.eb.setup.api.dto.SetupDtos.ThresholdRuleResponse;
import com.iortatechnxt.brokerverse.eb.setup.service.EbSetupService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** EB Setup (FR-EB-034, 042): threshold rules and required documents with maker-checker. */
@RestController
@RequestMapping("/api/v1/eb/setup")
public class EbSetupController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String SETUP = "hasAuthority('EB_SETUP')";
  private static final String ACTION = "{id}/{action:authorize|deactivate}";

  private final EbSetupService setup;

  /**
   * Creates the controller.
   *
   * @param setup EB set-up
   */
  public EbSetupController(EbSetupService setup) {
    this.setup = setup;
  }

  /**
   * The threshold rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/threshold-rules")
  @PreAuthorize(VIEW)
  public List<ThresholdRuleResponse> rules(@RequestParam Long companyId) {
    return setup.rules(companyId).stream().map(ThresholdRuleResponse::from).toList();
  }

  /**
   * Creates a threshold rule.
   *
   * @param companyId company
   * @param data rule
   * @return rule, pending authorisation
   */
  @PostMapping("/threshold-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(SETUP)
  public ThresholdRuleResponse createRule(
      @RequestParam Long companyId, @RequestBody EbThresholdRule.Data data) {
    return ThresholdRuleResponse.from(setup.createRule(companyId, data));
  }

  /**
   * Changes a threshold rule.
   *
   * @param id rule
   * @param companyId company
   * @param data rule
   * @return rule, pending authorisation
   */
  @PutMapping("/threshold-rules/{id}")
  @PreAuthorize(SETUP)
  public ThresholdRuleResponse updateRule(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody EbThresholdRule.Data data) {
    return ThresholdRuleResponse.from(setup.updateRule(companyId, id, data));
  }

  /**
   * Authorises or deactivates a threshold rule.
   *
   * @param id rule
   * @param action authorize or deactivate
   * @param companyId company
   * @return rule
   */
  @PostMapping("/threshold-rules/" + ACTION)
  @PreAuthorize(SETUP)
  public ThresholdRuleResponse ruleAction(
      @PathVariable Long id, @PathVariable String action, @RequestParam Long companyId) {
    return ThresholdRuleResponse.from(
        "authorize".equals(action)
            ? setup.authorizeRule(companyId, id)
            : setup.deactivateRule(companyId, id));
  }

  /**
   * The required documents.
   *
   * @param companyId company
   * @return requirements
   */
  @GetMapping("/required-documents")
  @PreAuthorize(VIEW)
  public List<RequiredDocumentResponse> required(@RequestParam Long companyId) {
    return setup.requiredDocuments(companyId).stream().map(RequiredDocumentResponse::from).toList();
  }

  /**
   * Creates a required document.
   *
   * @param companyId company
   * @param data requirement
   * @return requirement, pending authorisation
   */
  @PostMapping("/required-documents")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(SETUP)
  public RequiredDocumentResponse createRequired(
      @RequestParam Long companyId, @RequestBody EbRequiredDocument.Data data) {
    return RequiredDocumentResponse.from(setup.createRequired(companyId, data));
  }

  /**
   * Changes a required document.
   *
   * @param id requirement
   * @param companyId company
   * @param data requirement
   * @return requirement, pending authorisation
   */
  @PutMapping("/required-documents/{id}")
  @PreAuthorize(SETUP)
  public RequiredDocumentResponse updateRequired(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestBody EbRequiredDocument.Data data) {
    return RequiredDocumentResponse.from(setup.updateRequired(companyId, id, data));
  }

  /**
   * Authorises or deactivates a required document.
   *
   * @param id requirement
   * @param action authorize or deactivate
   * @param companyId company
   * @return requirement
   */
  @PostMapping("/required-documents/" + ACTION)
  @PreAuthorize(SETUP)
  public RequiredDocumentResponse requiredAction(
      @PathVariable Long id, @PathVariable String action, @RequestParam Long companyId) {
    return RequiredDocumentResponse.from(
        "authorize".equals(action)
            ? setup.authorizeRequired(companyId, id)
            : setup.deactivateRequired(companyId, id));
  }
}
