package com.iortatechnxt.brokerverse.issuance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.IssuanceRowResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.PolicyRecordResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.TriggerRuleResponse;
import com.iortatechnxt.brokerverse.issuance.service.DocumentTriggerService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService.IssuanceCounts;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceTab;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issuance Workbench (BRNB.073/077/070): tabs and tile counts, the policy record of an account and
 * the document trigger rules (BRNB.105).
 */
@RestController
@RequestMapping("/api/v1/issuance")
public class IssuanceController {

  static final String VIEW = "hasAnyAuthority('ACCOUNT_VIEW', 'EPOLICY_MANAGE', 'EPOLICY_SEND')";
  static final String MANAGE = "hasAuthority('EPOLICY_MANAGE')";
  static final int MAX_PAGE = 100;

  private final IssuanceQueryService queries;
  private final DocumentTriggerService triggers;

  /**
   * Creates the controller.
   *
   * @param queries issuance reads
   * @param triggers document triggers
   */
  public IssuanceController(IssuanceQueryService queries, DocumentTriggerService triggers) {
    this.queries = queries;
    this.triggers = triggers;
  }

  /**
   * One page of a workbench tab.
   *
   * @param companyId company
   * @param tab tab
   * @param text ARN or client fragment
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/workbench")
  @PreAuthorize(VIEW)
  public PageResponse<IssuanceRowResponse> workbench(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "AWAITING_POLICY") IssuanceTab tab,
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.workbench(companyId, tab, text, pageable(page, size)), IssuanceRowResponse::from);
  }

  /**
   * Tile counts.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/workbench/counts")
  @PreAuthorize(VIEW)
  public IssuanceCounts counts(@RequestParam Long companyId) {
    return queries.counts(companyId);
  }

  /**
   * The policy record of an account.
   *
   * @param arn Account Reference Number
   * @return policy numbers, e-policies and advices
   */
  @GetMapping("/policies/{arn}")
  @PreAuthorize(VIEW)
  public PolicyRecordResponse policy(@PathVariable String arn) {
    return PolicyRecordResponse.from(queries.policyFor(arn));
  }

  /**
   * The document trigger rules.
   *
   * @return rules
   */
  @GetMapping("/triggers")
  @PreAuthorize(VIEW)
  public List<TriggerRuleResponse> triggers() {
    return triggers.rules().stream().map(TriggerRuleResponse::from).toList();
  }

  static PageRequest pageable(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE));
  }
}
