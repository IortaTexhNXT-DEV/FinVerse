package com.iortatechnxt.brokerverse.eb.tracked.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.tracked.api.dto.ItemRequests.ItemFilter;
import com.iortatechnxt.brokerverse.eb.tracked.api.dto.ItemRequests.ItemRequest;
import com.iortatechnxt.brokerverse.eb.tracked.api.dto.ItemRequests.StatusRequest;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery.ItemRow;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
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
 * Pending Items (BRID-030; FR-EB-057; design 10.1): tracked contracts, HMO cards, card replacements
 * and billings of the EB programmes, filtered by programme, member, type, party, status and
 * overdue; opened, changed and moved on by Marketing and Processing.
 */
@RestController
@RequestMapping("/api/v1/eb/pending-items")
public class TrackedItemController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MAINTAIN = "hasAnyAuthority('EB_MARKET', 'EB_PROCESS')";

  private final TrackedItemService items;
  private final TrackedItemQuery query;

  /**
   * Creates the controller.
   *
   * @param items item maintenance
   * @param query item list
   */
  public TrackedItemController(TrackedItemService items, TrackedItemQuery query) {
    this.items = items;
    this.query = query;
  }

  /**
   * The Pending Items list.
   *
   * @param companyId company
   * @param filter programme, member, type, responsible party, status, overdue, search text, page
   *     and size
   * @return items
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<ItemRow> search(@RequestParam Long companyId, ItemFilter filter) {
    return query.search(companyId, filter.criteria(), filter.pageNo(), filter.pageSize());
  }

  /**
   * Opens an item.
   *
   * @param companyId company
   * @param request programme, type and details
   * @return the item as listed
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public ItemRow open(@RequestParam Long companyId, @RequestBody ItemRequest request) {
    if (request.programmeId() == null) {
      throw new BusinessRuleException("EB_PROGRAMME_REQUIRED", "Select the programme");
    }
    Long id = items.open(companyId, request.programmeId(), request.toInput()).getId();
    return row(companyId, id);
  }

  /**
   * Changes a pending item.
   *
   * @param id item
   * @param companyId company
   * @param request details
   * @return the item as listed
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public ItemRow update(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody ItemRequest request) {
    items.update(companyId, id, request.toDetails());
    return row(companyId, id);
  }

  /**
   * Receives, releases or closes an item.
   *
   * @param id item
   * @param companyId company
   * @param request action, dates and remarks
   * @return the item as listed
   */
  @PostMapping("/{id}/status")
  @PreAuthorize(MAINTAIN)
  public ItemRow change(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody StatusRequest request) {
    if (request.action() == null) {
      throw new BusinessRuleException(
          "EB_ITEM_ACTION_REQUIRED", "Select what happened to the item");
    }
    items.change(companyId, id, request.toChange());
    return row(companyId, id);
  }

  private ItemRow row(Long companyId, Long id) {
    return query.row(companyId, id);
  }
}
