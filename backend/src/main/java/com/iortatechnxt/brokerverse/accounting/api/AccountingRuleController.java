package com.iortatechnxt.brokerverse.accounting.api;

import com.iortatechnxt.brokerverse.accounting.api.dto.EventLogResponse;
import com.iortatechnxt.brokerverse.accounting.api.dto.EventTypeResponse;
import com.iortatechnxt.brokerverse.accounting.api.dto.RuleRequest;
import com.iortatechnxt.brokerverse.accounting.api.dto.RuleResponse;
import com.iortatechnxt.brokerverse.accounting.api.dto.SimulationRequest;
import com.iortatechnxt.brokerverse.accounting.domain.EventStatus;
import com.iortatechnxt.brokerverse.accounting.service.AccountingRuleService;
import com.iortatechnxt.brokerverse.accounting.service.AccountingRuleService.Simulation;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

/** Accounting engine configuration: event types, rules, simulator and event register. */
@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingRuleController {

  private static final String VIEW = "hasAuthority('MASTER_VIEW')";
  private static final String MANAGE = "hasAuthority('ACCOUNTING_RULE_MANAGE')";
  private static final int MAX_PAGE_SIZE = 200;

  private final AccountingRuleService service;

  /**
   * Creates the controller.
   *
   * @param service rule service
   */
  public AccountingRuleController(AccountingRuleService service) {
    this.service = service;
  }

  /**
   * Lists event types.
   *
   * @return event types
   */
  @GetMapping("/event-types")
  @PreAuthorize(VIEW)
  public List<EventTypeResponse> eventTypes() {
    return service.eventTypes().stream().map(EventTypeResponse::from).toList();
  }

  /**
   * Lists rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/rules")
  @PreAuthorize(VIEW)
  public List<RuleResponse> rules(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(RuleResponse::from).toList();
  }

  /**
   * Creates a rule.
   *
   * @param request request
   * @return rule
   */
  @PostMapping("/rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public RuleResponse create(@Valid @RequestBody RuleRequest request) {
    return RuleResponse.from(service.create(request));
  }

  /**
   * Updates a rule.
   *
   * @param id id
   * @param request request
   * @return rule
   */
  @PutMapping("/rules/{id}")
  @PreAuthorize(MANAGE)
  public RuleResponse update(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
    return RuleResponse.from(service.update(id, request));
  }

  /**
   * Authorizes a rule.
   *
   * @param id id
   * @return rule
   */
  @PostMapping("/rules/{id}/authorize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public RuleResponse authorize(@PathVariable Long id) {
    return RuleResponse.from(service.authorize(id));
  }

  /**
   * Previews the journal of a sample event.
   *
   * @param request sample event
   * @return simulation
   */
  @PostMapping("/simulate")
  @PreAuthorize(VIEW)
  public Simulation simulate(@Valid @RequestBody SimulationRequest request) {
    return service.simulate(request);
  }

  /**
   * Searches the event register.
   *
   * @param companyId company
   * @param status status
   * @param eventType event type
   * @param from value date from
   * @param to value date to
   * @param page page
   * @param size size
   * @return events
   */
  @GetMapping("/events")
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public PageResponse<EventLogResponse> events(
      @RequestParam Long companyId,
      @RequestParam(required = false) EventStatus status,
      @RequestParam(required = false) String eventType,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    var pageable =
        PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(
        service.events(companyId, status, blankToNull(eventType), from, to, pageable),
        EventLogResponse::from);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
