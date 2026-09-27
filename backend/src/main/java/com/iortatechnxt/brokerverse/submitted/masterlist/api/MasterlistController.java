package com.iortatechnxt.brokerverse.submitted.masterlist.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTracking;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.ActionRequest;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.AssignRequest;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.HistoryRow;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.PolicyDetail;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.PolicyRequest;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.PolicyRow;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.dto.PolicyDtos.TagRequest;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistFilter.Tab;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistQueryService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Submitted Masterlist (FRS FR-SP-003, 010-012, 031, 033): the work list with its tabs and
 * counters, the record, manual entry and edit, the renewal tag, the tracking fields, the Assign
 * Handler bulk action, the handlers' actions and the field history.
 */
@RestController
@RequestMapping("/api/v1/submitted/policies")
public class MasterlistController {

  /** View permission of the Submitted Policies screens. */
  public static final String VIEW = "hasAuthority('SBM_VIEW')";

  /** Maintain permission. */
  public static final String MAINTAIN = "hasAuthority('SBM_MAINTAIN')";

  private static final String PROCESS = "hasAuthority('SBM_PROCESS')";
  private static final Set<String> ACTIONS = Set.of("dispose", "exclude", "reinstate", "close");

  private final MasterlistService masterlist;
  private final MasterlistQueryService queries;

  /**
   * Creates the controller.
   *
   * @param masterlist masterlist changes
   * @param queries masterlist reads
   */
  public MasterlistController(MasterlistService masterlist, MasterlistQueryService queries) {
    this.masterlist = masterlist;
    this.queries = queries;
  }

  /**
   * A page of the masterlist.
   *
   * @param params criteria
   * @param pageable page and sort (expiry first by default)
   * @return rows
   */
  @GetMapping
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public PageResponse<PolicyRow> list(
      @ModelAttribute MasterlistParams params,
      @PageableDefault(size = 50, sort = "terms.expiryDate", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return PageResponse.of(queries.list(params.filter(), pageable), PolicyRow::from);
  }

  /**
   * The counters of the tabs.
   *
   * @param params criteria (the tab is ignored)
   * @return count per tab
   */
  @GetMapping("/counts")
  @PreAuthorize(VIEW)
  public Map<Tab, Long> counts(@ModelAttribute MasterlistParams params) {
    return queries.counts(params.filter());
  }

  /**
   * A record.
   *
   * @param id record
   * @return record with every field
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public PolicyDetail get(@PathVariable Long id) {
    return PolicyDetail.from(masterlist.get(id));
  }

  /**
   * Creates a record by hand.
   *
   * @param companyId company
   * @param request policy data and tracking
   * @return the record
   */
  @PostMapping
  @PreAuthorize(MAINTAIN)
  @Transactional
  public PolicyDetail create(
      @RequestParam Long companyId, @Valid @RequestBody PolicyRequest request) {
    return PolicyDetail.from(masterlist.create(companyId, request.data(), request.tracking()));
  }

  /**
   * Edits a record.
   *
   * @param id record
   * @param request policy data
   * @return the record
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public PolicyDetail update(@PathVariable Long id, @Valid @RequestBody PolicyRequest request) {
    return PolicyDetail.from(masterlist.update(id, request.data()));
  }

  /**
   * Tags the renewal opportunity.
   *
   * @param id record
   * @param request tag and reason
   * @return the record
   */
  @PostMapping("/{id}/renewal-tag")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public PolicyDetail tag(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
    return PolicyDetail.from(masterlist.tag(id, request.tag(), request.reason()));
  }

  /**
   * Updates the handler, AO, conversion status, opportunity and remarks.
   *
   * @param id record
   * @param request tracking fields
   * @return the record
   */
  @PutMapping("/{id}/tracking")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public PolicyDetail track(@PathVariable Long id, @RequestBody SbmTracking request) {
    return PolicyDetail.from(masterlist.track(id, request));
  }

  /**
   * Assigns a handler to records.
   *
   * @param request records and handler
   * @return records assigned
   */
  @PostMapping("/assign")
  @PreAuthorize(MAINTAIN)
  public Map<String, Integer> assign(@Valid @RequestBody AssignRequest request) {
    return Map.of(
        "assigned", masterlist.assign(request.companyId(), request.ids(), request.handler()));
  }

  /**
   * A handler's action: dispose for renewal, exclude, reinstate or close.
   *
   * @param id record
   * @param action action
   * @param request reason and comment
   * @return the record
   */
  @PostMapping("/{id}/actions/{action}")
  @PreAuthorize(PROCESS)
  @Transactional
  public PolicyDetail act(
      @PathVariable Long id, @PathVariable String action, @RequestBody ActionRequest request) {
    if (!ACTIONS.contains(action)) {
      throw new BusinessRuleException("SBM_ACTION_UNKNOWN", "This action is not available");
    }
    return PolicyDetail.from(masterlist.act(id, action, request.reasonCode(), request.comment()));
  }

  /**
   * The field history of a record.
   *
   * @param id record
   * @return changes, newest first
   */
  @GetMapping("/{id}/history")
  @PreAuthorize(VIEW)
  public List<HistoryRow> history(@PathVariable Long id) {
    masterlist.get(id);
    return queries.history(id).stream().map(HistoryRow::from).toList();
  }
}
