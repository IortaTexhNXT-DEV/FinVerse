package com.iortatechnxt.brokerverse.accounting.api;

import com.iortatechnxt.brokerverse.accounting.api.dto.CostCenterRuleRequest;
import com.iortatechnxt.brokerverse.accounting.api.dto.CostCenterRuleResponse;
import com.iortatechnxt.brokerverse.accounting.service.CostCenterRuleService;
import jakarta.validation.Valid;
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

/** Cost-centre rules of the accounting engine (FRBS 3.1.1, DIS 3.30.0). */
@RestController
@RequestMapping("/api/v1/accounting/cost-center-rules")
public class CostCenterRuleController {

  private static final String MANAGE =
      "hasAnyAuthority('ACCOUNTING_RULE_MANAGE','MASTER_MAINTAIN')";

  private final CostCenterRuleService service;

  /**
   * Creates the controller.
   *
   * @param service rule service
   */
  public CostCenterRuleController(CostCenterRuleService service) {
    this.service = service;
  }

  /**
   * Rules of a company in evaluation order.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<CostCenterRuleResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(CostCenterRuleResponse::from).toList();
  }

  /**
   * Creates a rule.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public CostCenterRuleResponse create(@Valid @RequestBody CostCenterRuleRequest request) {
    return CostCenterRuleResponse.from(service.create(request.companyId(), request.values()));
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param request rule
   * @return rule
   */
  @PutMapping("/{id}")
  @PreAuthorize(MANAGE)
  public CostCenterRuleResponse update(
      @PathVariable Long id, @Valid @RequestBody CostCenterRuleRequest request) {
    return CostCenterRuleResponse.from(service.update(id, request.values()));
  }
}
