package com.iortatechnxt.brokerverse.screening.watchlist.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.screening.common.api.DecisionRequest;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.ChangeDto;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.EntryDetail;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.EntryRequest;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.EntryRow;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDecisionService;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
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
 * Watchlist entries and their changes (SNSRP-203, 204; FR-SS-022, 023): search, detail with
 * history, add / change / deactivate (maker, SCR_LIST_MAINTAIN) and approve / reject (checker,
 * SCR_LIST_APPROVE).
 */
@RestController
@RequestMapping("/api/v1/screening/watchlist")
public class WatchlistController {

  private static final int MAX_PAGE = 200;

  private final WatchlistService service;
  private final WatchlistDecisionService decisions;

  /**
   * Creates the controller.
   *
   * @param service watchlist maintenance
   * @param decisions checker decisions
   */
  public WatchlistController(WatchlistService service, WatchlistDecisionService decisions) {
    this.service = service;
    this.decisions = decisions;
  }

  /**
   * Searches entries.
   *
   * @param source source code, blank for all
   * @param status status, blank for all
   * @param search name or reference
   * @param page page
   * @param size size
   * @return entries
   */
  @GetMapping("/entries")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public PageResponse<EntryRow> entries(
      @RequestParam(required = false) String source,
      @RequestParam(required = false) EntryStatus status,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    Map<Long, String> codes = service.sourceCodes();
    return PageResponse.of(
        service.search(source, status, search, PageRequest.of(page, Math.min(size, MAX_PAGE))),
        e -> EntryRow.from(e, codes));
  }

  /**
   * An entry with its aliases and history.
   *
   * @param id entry
   * @return detail
   */
  @GetMapping("/entries/{id}")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public EntryDetail entry(@PathVariable Long id) {
    WatchlistEntry entry = service.entry(id);
    return new EntryDetail(
        EntryRow.from(entry, service.sourceCodes()),
        service.values(entry).aliases(),
        service.history(id).stream().map(c -> change(c, entry)).toList());
  }

  /**
   * Adds an entry (PENDING until approved).
   *
   * @param request entry and remarks
   * @return the pending change
   */
  @PostMapping("/entries")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public ChangeDto add(@Valid @RequestBody EntryRequest request) {
    return change(service.add(request.sourceCode(), request.values(), request.remarks()));
  }

  /**
   * Changes an entry (PENDING until approved).
   *
   * @param id entry
   * @param request new values and remarks
   * @return the pending change
   */
  @PutMapping("/entries/{id}")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public ChangeDto change(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
    return change(service.change(id, request.values(), request.remarks()));
  }

  /**
   * Deactivates an entry (PENDING until approved; entries are never deleted).
   *
   * @param id entry
   * @param request remarks
   * @return the pending change
   */
  @PostMapping("/entries/{id}/deactivate")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public ChangeDto deactivate(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return change(service.deactivate(id, request.remarks()));
  }

  /**
   * Changes in a status.
   *
   * @param status status (default PENDING)
   * @param page page
   * @param size size
   * @return changes
   */
  @GetMapping("/changes")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public PageResponse<ChangeDto> changes(
      @RequestParam(defaultValue = "PENDING") ChangeStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        service.changes(status, PageRequest.of(page, Math.min(size, MAX_PAGE))), this::change);
  }

  /**
   * One change with its before and after values.
   *
   * @param id change
   * @return change
   */
  @GetMapping("/changes/{id}")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public ChangeDto changeDetail(@PathVariable Long id) {
    return change(service.change(id));
  }

  /**
   * Approves a change.
   *
   * @param id change
   * @param request optional remarks
   * @return the change
   */
  @PostMapping("/changes/{id}/approve")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_APPROVE)
  public ChangeDto approve(
      @PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest request) {
    return change(decisions.approve(id, request == null ? null : request.remarks()));
  }

  /**
   * Rejects a change with remarks.
   *
   * @param id change
   * @param request remarks
   * @return the change
   */
  @PostMapping("/changes/{id}/reject")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_APPROVE)
  public ChangeDto reject(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return change(decisions.reject(id, request.remarks()));
  }

  private ChangeDto change(WatchlistChange c) {
    return change(c, service.entry(c.getEntryId()));
  }

  private ChangeDto change(WatchlistChange c, WatchlistEntry entry) {
    return ChangeDto.from(c, entry, service::readValues);
  }
}
