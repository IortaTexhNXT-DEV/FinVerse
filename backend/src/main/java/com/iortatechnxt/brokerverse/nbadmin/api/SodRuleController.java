package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.nbadmin.api.dto.SodRuleRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.SodRuleResponse;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestDescriber;
import com.iortatechnxt.brokerverse.nbadmin.service.SodRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Separation-of-duties rules (FRS BRD-11, permissions matrix): the Business Administrator adds and
 * deactivates the pairs of group profiles one user may not hold; Information Security authorises
 * each change.
 */
@RestController
@RequestMapping("/api/v1/nbadmin/sod-rules")
public class SodRuleController {

  private static final String MAINTAIN = "hasAuthority('UAM_SOD_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('UAM_SOD_AUTHORIZE')";

  private final SodRuleService rules;
  private final AccessRequestDescriber describer;

  /**
   * Creates the controller.
   *
   * @param rules rules
   * @param describer profile names
   */
  public SodRuleController(SodRuleService rules, AccessRequestDescriber describer) {
    this.rules = rules;
    this.describer = describer;
  }

  /**
   * Every rule.
   *
   * @return rules
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('UAM_SOD_MAINTAIN', 'UAM_SOD_AUTHORIZE', 'AUDIT_VIEW')")
  public List<SodRuleResponse> list() {
    return rules.list().stream().map(this::view).toList();
  }

  /**
   * Adds a rule, pending authorisation.
   *
   * @param body the two group profiles and the reason
   * @return rule
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public SodRuleResponse create(@Valid @RequestBody SodRuleRequest body) {
    return view(rules.create(body.profileA(), body.profileB(), body.description()));
  }

  /**
   * Asks for the deactivation of a rule.
   *
   * @param id rule
   * @return rule
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize(MAINTAIN)
  public SodRuleResponse deactivate(@PathVariable Long id) {
    return view(rules.requestDeactivation(id));
  }

  /**
   * Authorises the pending change of a rule.
   *
   * @param id rule
   * @return rule
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public SodRuleResponse authorize(@PathVariable Long id) {
    return view(rules.authorize(id));
  }

  /**
   * Rejects the pending change of a rule.
   *
   * @param id rule
   * @return rule
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(AUTHORIZE)
  public SodRuleResponse reject(@PathVariable Long id) {
    return view(rules.reject(id));
  }

  private SodRuleResponse view(SodRule r) {
    return SodRuleResponse.from(r, describer);
  }
}
