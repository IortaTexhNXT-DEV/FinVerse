package com.iortatechnxt.brokerverse.collections.disposition.api;

import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.DetailsRequest;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.DispositionRequest;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.DispositionResponse;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.DispositionRuleResponse;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.EffortRequest;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.EffortResponse;
import com.iortatechnxt.brokerverse.collections.disposition.api.dto.DispositionDtos.HandoffResponse;
import com.iortatechnxt.brokerverse.collections.disposition.service.AccountTimelineService;
import com.iortatechnxt.brokerverse.collections.disposition.service.EffortService;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService;
import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService;
import com.iortatechnxt.brokerverse.collections.worklist.api.ClxAccess;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.TimelineResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.ItemResponse;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
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
 * The collector's work on accounts (BRCLXN.016-023, 051, 057; p.40-46): PR dispositions and efforts
 * on one or several accounts, remarks and tagging category, and the account's dispositions,
 * efforts, hand-offs to Operations and timeline.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class DispositionController {

  private final PrDispositionService dispositions;
  private final EffortService efforts;
  private final AccountTimelineService timeline;
  private final OutboxService outbox;
  private final CollectionItems items;
  private final LovService lovs;
  private final LovAttributes attributes;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param dispositions dispositions
   * @param efforts efforts and details
   * @param timeline account timeline
   * @param outbox hand-offs
   * @param items items
   * @param lovs lists of values
   * @param attributes disposition rules
   * @param clock clock
   */
  public DispositionController(
      PrDispositionService dispositions,
      EffortService efforts,
      AccountTimelineService timeline,
      OutboxService outbox,
      CollectionItems items,
      LovService lovs,
      LovAttributes attributes,
      Clock clock) {
    this.dispositions = dispositions;
    this.efforts = efforts;
    this.timeline = timeline;
    this.outbox = outbox;
    this.items = items;
    this.lovs = lovs;
    this.attributes = attributes;
    this.clock = clock;
  }

  /**
   * The PR collector dispositions usable today with their rules (BRCLXN.016-018): category, owner,
   * Operations action (which hand-off details it needs) and the roles allowed to set it.
   *
   * @return rules in display order
   */
  @GetMapping("/disposition-rules")
  @PreAuthorize(ClxAccess.VIEW)
  public List<DispositionRuleResponse> rules() {
    return lovs.activeValues(LovAttributes.PR_DISPOSITION, LocalDate.now(clock)).stream()
        .map(v -> DispositionRuleResponse.from(v.getLabel(), attributes.prRule(v.getCode())))
        .toList();
  }

  /**
   * Records a disposition on one or several accounts (BRCLXN.016-023, 051).
   *
   * @param companyId company
   * @param request accounts, code, remarks and hand-off details
   * @return the dispositions
   */
  @PostMapping("/dispositions")
  @PreAuthorize(ClxAccess.WORK)
  public List<DispositionResponse> dispose(
      @RequestParam Long companyId, @Valid @RequestBody DispositionRequest request) {
    return dispositions.record(companyId, request.toCommand()).stream()
        .map(DispositionResponse::from)
        .toList();
  }

  /**
   * Logs an effort on one or several accounts (p.43-46, BRCLXN.051).
   *
   * @param companyId company
   * @param request accounts and what was done
   * @return the efforts
   */
  @PostMapping("/efforts")
  @PreAuthorize(ClxAccess.WORK)
  public List<EffortResponse> effort(
      @RequestParam Long companyId, @Valid @RequestBody EffortRequest request) {
    return efforts.log(companyId, request.toCommand()).stream().map(EffortResponse::from).toList();
  }

  /**
   * Changes the remarks and tagging category of an account (p.41).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param request remarks and category
   * @return the account
   */
  @PutMapping("/items/{invoiceNo}/details")
  @PreAuthorize(ClxAccess.WORK)
  public ItemResponse details(
      @RequestParam Long companyId,
      @PathVariable String invoiceNo,
      @Valid @RequestBody DetailsRequest request) {
    return ItemResponse.from(
        efforts.updateDetails(companyId, invoiceNo, request.remarks(), request.category()));
  }

  /**
   * Dispositions of an account, newest first (BRCLXN.021).
   *
   * @param invoiceNo invoice
   * @return dispositions
   */
  @GetMapping("/items/{invoiceNo}/dispositions")
  @PreAuthorize(ClxAccess.VIEW)
  public List<DispositionResponse> dispositions(@PathVariable String invoiceNo) {
    return dispositions.ofItem(items.require(invoiceNo).getId()).stream()
        .map(DispositionResponse::from)
        .toList();
  }

  /**
   * Efforts of an account, latest first.
   *
   * @param invoiceNo invoice
   * @return efforts
   */
  @GetMapping("/items/{invoiceNo}/efforts")
  @PreAuthorize(ClxAccess.VIEW)
  public List<EffortResponse> efforts(@PathVariable String invoiceNo) {
    return efforts.ofItem(items.require(invoiceNo).getId()).stream()
        .map(EffortResponse::from)
        .toList();
  }

  /**
   * Hand-offs of an account to Operations (COLLECTIONS_DESIGN 2.2).
   *
   * @param invoiceNo invoice
   * @return outbox items, newest first
   */
  @GetMapping("/items/{invoiceNo}/handoffs")
  @PreAuthorize(ClxAccess.VIEW)
  public List<HandoffResponse> handoffs(@PathVariable String invoiceNo) {
    items.require(invoiceNo);
    return outbox.ofInvoice(invoiceNo).stream()
        .map(o -> HandoffResponse.from(o, outbox.read(o.getFields())))
        .toList();
  }

  /**
   * The account timeline (BRCLXN.057).
   *
   * @param invoiceNo invoice
   * @return entries, newest first
   */
  @GetMapping("/items/{invoiceNo}/timeline")
  @PreAuthorize(ClxAccess.VIEW)
  public List<TimelineResponse> timeline(@PathVariable String invoiceNo) {
    return timeline.timeline(invoiceNo).stream().map(TimelineResponse::from).toList();
  }
}
