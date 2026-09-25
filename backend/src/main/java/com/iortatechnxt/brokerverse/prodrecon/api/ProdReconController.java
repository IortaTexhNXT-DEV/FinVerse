package com.iortatechnxt.brokerverse.prodrecon.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.BulkFeedbackRequest;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.CommentRequest;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.CycleResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.FeedbackRequest;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.ItemQuery;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.ItemResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.PairRequest;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator.Line;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconCycleService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconCycleService.BoardFilter;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconItemService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconItemService.BulkChange;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconItemService.ItemFilter;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatchingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Production reconciliation cycles and the reconciliation workbench (PRCID.012-033): the cycles
 * board, items by bucket with filters, feedback and disposition, manual pairing, automatch,
 * closing, the unbooked repository and the early incentive validation.
 */
@RestController
@RequestMapping("/api/v1/prodrecon")
public class ProdReconController {

  private final ReconCycleService cycles;
  private final ReconItemService items;
  private final ReconMatchingService matching;
  private final EarlyIncentiveValidator incentives;

  /**
   * Creates the controller.
   *
   * @param cycles cycles
   * @param items workbench
   * @param matching matching engine
   * @param incentives early incentive validation
   */
  public ProdReconController(
      ReconCycleService cycles,
      ReconItemService items,
      ReconMatchingService matching,
      EarlyIncentiveValidator incentives) {
    this.cycles = cycles;
    this.items = items;
    this.matching = matching;
    this.incentives = incentives;
  }

  /**
   * The cycles board (PRCID.013).
   *
   * @param companyId company
   * @param insurer insurer
   * @param month any day of the production month
   * @param stage stage
   * @param page page
   * @param size size
   * @return cycles
   */
  @GetMapping("/cycles")
  @PreAuthorize(ReconAccess.READ)
  public PageResponse<CycleResponse> cycles(
      @RequestParam Long companyId,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) LocalDate month,
      @RequestParam(required = false) String stage,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        cycles.board(
            companyId,
            new BoardFilter(blank(insurer), month, blank(stage)),
            ReconAccess.page(page, size)),
        CycleResponse::from);
  }

  /**
   * A cycle.
   *
   * @param id cycle
   * @return cycle
   */
  @GetMapping("/cycles/{id}")
  @PreAuthorize(ReconAccess.READ)
  public CycleResponse cycle(@PathVariable Long id) {
    return CycleResponse.from(cycles.view(id));
  }

  /**
   * Items of a cycle (PRCID.012-014/021/030).
   *
   * @param id cycle
   * @param query bucket, filters and page
   * @return items
   */
  @GetMapping("/cycles/{id}/items")
  @PreAuthorize(ReconAccess.READ)
  public PageResponse<ItemResponse> items(@PathVariable Long id, ItemQuery query) {
    return PageResponse.of(
        items.items(
            id,
            new ItemFilter(
                query.bucket(),
                query.q(),
                query.ao(),
                query.unit(),
                query.segment(),
                query.productLine()),
            PageRequest.of(
                query.page(), Math.min(query.size(), ReconAccess.MAX_PAGE), Sort.by("id"))),
        ItemResponse::from);
  }

  /**
   * Re-runs the matching of a cycle (PRCID.025).
   *
   * @param id cycle
   * @return items whose status changed
   */
  @PostMapping("/cycles/{id}/automatch")
  @PreAuthorize(ReconAccess.PROCESS)
  public Map<String, Integer> automatch(@PathVariable Long id) {
    return Map.of("changed", matching.rematch(id));
  }

  /**
   * Closes a cycle.
   *
   * @param id cycle
   * @param request comment
   * @return cycle
   */
  @PostMapping("/cycles/{id}/close")
  @PreAuthorize(ReconAccess.PROCESS)
  public CycleResponse close(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    cycles.close(id, request.comment());
    return CycleResponse.from(cycles.view(id));
  }

  /**
   * Early incentive validation of a cycle (PRCID.028).
   *
   * @param id cycle
   * @return one line per booked invoice
   */
  @GetMapping("/cycles/{id}/early-incentive")
  @PreAuthorize(ReconAccess.READ)
  public List<Line> earlyIncentive(@PathVariable Long id) {
    return incentives.validate(cycles.require(id).getCompanyId(), id);
  }

  /**
   * Records feedback and disposition on an item (PRCID.015/016/018).
   *
   * @param id item
   * @param request feedback
   * @return item
   */
  @PutMapping("/items/{id}/feedback")
  @PreAuthorize(ReconAccess.PROCESS)
  public ItemResponse feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request) {
    return ItemResponse.from(items.feedback(id, request.toFeedback()));
  }

  /**
   * Sets company concerned, disposition and closure on a selection.
   *
   * @param request items and values
   * @return items
   */
  @PostMapping("/items/feedback")
  @PreAuthorize(ReconAccess.PROCESS)
  public List<ItemResponse> bulkFeedback(@Valid @RequestBody BulkFeedbackRequest request) {
    return items
        .bulkFeedback(
            request.ids(),
            new BulkChange(
                blank(request.companyConcerned()),
                blank(request.disposition()),
                request.forClosure()))
        .stream()
        .map(ItemResponse::from)
        .toList();
  }

  /**
   * Pairs a booked item with an insurer-only item by hand.
   *
   * @param id booked item
   * @param request insurer-only item
   * @return paired item
   */
  @PostMapping("/items/{id}/pair")
  @PreAuthorize(ReconAccess.PROCESS)
  public ItemResponse pair(@PathVariable Long id, @Valid @RequestBody PairRequest request) {
    return ItemResponse.from(matching.pair(id, request.insurerItemId()));
  }

  /**
   * Splits a paired item.
   *
   * @param id item
   * @return the booked item
   */
  @PostMapping("/items/{id}/split")
  @PreAuthorize(ReconAccess.PROCESS)
  public ItemResponse split(@PathVariable Long id) {
    return ItemResponse.from(matching.split(id));
  }

  /**
   * The unbooked repository (PRCID.019/033).
   *
   * @param companyId company
   * @param insurer insurer
   * @param status resolution
   * @param q search text
   * @param page page
   * @param size size
   * @return insurer-only items
   */
  @GetMapping("/unbooked")
  @PreAuthorize(ReconAccess.READ)
  public PageResponse<ItemResponse> unbooked(
      @RequestParam Long companyId,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) UnbookedStatus status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        items.unbooked(companyId, insurer, status, q, ReconAccess.page(page, size)),
        ItemResponse::from);
  }

  private static String blank(String v) {
    return v == null || v.isBlank() ? null : v.strip();
  }
}
