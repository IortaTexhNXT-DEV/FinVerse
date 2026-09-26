package com.iortatechnxt.brokerverse.collections.escalation.api;

import com.iortatechnxt.brokerverse.collections.escalation.api.dto.EscalationDtos.MatchResponse;
import com.iortatechnxt.brokerverse.collections.escalation.api.dto.EscalationDtos.RuleRequest;
import com.iortatechnxt.brokerverse.collections.escalation.api.dto.EscalationDtos.RuleResponse;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationEngine;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationRuleService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/**
 * Escalation rules (BRCLXN.049): list, create and change on Collections Setup ({@code CLX_SETUP}),
 * authorization by another user ({@code MASTER_AUTHORIZE}), deactivation, and the preview of the
 * accounts a rule escalates on a date.
 */
@RestController
@RequestMapping("/api/v1/collections/escalation-rules")
public class EscalationRuleController {

  private static final int MAX_PREVIEW = 200;

  private final EscalationRuleService rules;
  private final EscalationEngine engine;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param rules rules
   * @param engine rule evaluation (preview)
   * @param clock clock
   */
  public EscalationRuleController(
      EscalationRuleService rules, EscalationEngine engine, Clock clock) {
    this.rules = rules;
    this.engine = engine;
    this.clock = clock;
  }

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('CLX_VIEW', 'CLX_SETUP', 'MASTER_AUTHORIZE')")
  public List<RuleResponse> list(@RequestParam Long companyId) {
    return rules.list(companyId).stream().map(RuleResponse::from).toList();
  }

  /**
   * Creates a rule, pending authorization.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping
  @PreAuthorize("hasAuthority('CLX_SETUP')")
  @ResponseStatus(HttpStatus.CREATED)
  public RuleResponse create(@Valid @RequestBody RuleRequest request) {
    if (request.companyId() == null || request.code() == null || request.code().isBlank()) {
      throw new BusinessRuleException("CLX_RULE_CODE", "Enter the company and the rule code");
    }
    return RuleResponse.from(rules.create(request.companyId(), request.code(), request.toTerms()));
  }

  /**
   * Changes a rule; it must be authorized again.
   *
   * @param id rule
   * @param request rule
   * @return rule
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('CLX_SETUP')")
  public RuleResponse update(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
    return RuleResponse.from(rules.update(id, request.toTerms()));
  }

  /**
   * Authorizes a rule (four eyes).
   *
   * @param id rule
   * @return rule
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public RuleResponse authorize(@PathVariable Long id) {
    return RuleResponse.from(rules.authorize(id));
  }

  /**
   * Deactivates a rule.
   *
   * @param id rule
   * @return rule
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasAuthority('CLX_SETUP')")
  public RuleResponse deactivate(@PathVariable Long id) {
    return RuleResponse.from(rules.deactivate(id));
  }

  /**
   * The open accounts a rule escalates on a date (whether or not it is authorized yet).
   *
   * @param id rule
   * @param asOf business date (default today)
   * @return accounts, at most 200
   */
  @GetMapping("/{id}/matches")
  @PreAuthorize("hasAnyAuthority('CLX_SETUP', 'MASTER_AUTHORIZE')")
  public List<MatchResponse> matches(
      @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return engine.matches(rules.get(id), asOf == null ? LocalDate.now(clock) : asOf).stream()
        .limit(MAX_PREVIEW)
        .map(MatchResponse::from)
        .toList();
  }
}
