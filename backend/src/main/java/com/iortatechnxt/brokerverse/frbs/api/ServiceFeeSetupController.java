package com.iortatechnxt.brokerverse.frbs.api;

import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.RecipientBody;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.RecipientResponse;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.RuleBody;
import com.iortatechnxt.brokerverse.frbs.api.dto.ServiceFeeDtos.RuleResponse;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeSetupService;
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
 * Service-fee configuration (FRBS 2.10.0; values AQ20): rates per service-fee segment and the
 * recipients and cost centres of the sales units.
 */
@RestController
@RequestMapping("/api/v1/frbs/service-fee")
public class ServiceFeeSetupController {

  private final ServiceFeeSetupService setup;

  /**
   * Creates the controller.
   *
   * @param setup configuration
   */
  public ServiceFeeSetupController(ServiceFeeSetupService setup) {
    this.setup = setup;
  }

  /**
   * The rules.
   *
   * @return rules
   */
  @GetMapping("/rules")
  @PreAuthorize(FrbsAccess.VIEW)
  public List<RuleResponse> rules() {
    return setup.rules().stream().map(RuleResponse::from).toList();
  }

  /**
   * Adds a rule.
   *
   * @param body rule
   * @return rule
   */
  @PostMapping("/rules")
  @PreAuthorize(FrbsAccess.SETUP)
  public RuleResponse createRule(@Valid @RequestBody RuleBody body) {
    return RuleResponse.from(setup.createRule(body.values()));
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param body rule
   * @return rule
   */
  @PutMapping("/rules/{id}")
  @PreAuthorize(FrbsAccess.SETUP)
  public RuleResponse updateRule(@PathVariable Long id, @Valid @RequestBody RuleBody body) {
    return RuleResponse.from(setup.updateRule(id, body.values()));
  }

  /**
   * The recipients of a company.
   *
   * @param companyId company
   * @return recipients
   */
  @GetMapping("/recipients")
  @PreAuthorize(FrbsAccess.VIEW)
  public List<RecipientResponse> recipients(@RequestParam Long companyId) {
    return setup.recipients(companyId).stream().map(RecipientResponse::from).toList();
  }

  /**
   * Names the recipient of a sales unit.
   *
   * @param companyId company
   * @param body recipient
   * @return recipient
   */
  @PostMapping("/recipients")
  @PreAuthorize(FrbsAccess.SETUP)
  public RecipientResponse createRecipient(
      @RequestParam Long companyId, @Valid @RequestBody RecipientBody body) {
    return RecipientResponse.from(
        setup.createRecipient(companyId, body.salesUnit(), body.values()));
  }

  /**
   * Changes a recipient.
   *
   * @param id recipient
   * @param body recipient
   * @return recipient
   */
  @PutMapping("/recipients/{id}")
  @PreAuthorize(FrbsAccess.SETUP)
  public RecipientResponse updateRecipient(
      @PathVariable Long id, @Valid @RequestBody RecipientBody body) {
    return RecipientResponse.from(setup.updateRecipient(id, body.values()));
  }
}
