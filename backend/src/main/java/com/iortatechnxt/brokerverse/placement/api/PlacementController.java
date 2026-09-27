package com.iortatechnxt.brokerverse.placement.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.CancelPlacementRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.GateResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.HoldCoverConfirmRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.HoldCoverRequestBody;
import com.iortatechnxt.brokerverse.placement.api.dto.HoldCoverResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.InsurerReturnRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.InsurerReturnResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.NoteRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.PlacementViewResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.ReactivateRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.ReadinessResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.SlipResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.WorkbenchRowResponse;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.HoldCoverConfirmation;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.HoldCoverRequest;
import com.iortatechnxt.brokerverse.placement.service.InsurerReturnService;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService;
import com.iortatechnxt.brokerverse.placement.service.PlacementBatchService;
import com.iortatechnxt.brokerverse.placement.service.PlacementBatchService.ItemResult;
import com.iortatechnxt.brokerverse.placement.service.PlacementQueryService;
import com.iortatechnxt.brokerverse.placement.service.PlacementQueryService.WorkbenchCounts;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService;
import com.iortatechnxt.brokerverse.placement.service.WorkbenchTab;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placement Workbench and account placement actions (BRNB.033/034/062/069/072/103, BRD 2.1.16):
 * workbench lists and tiles, slip readiness, the placement view of an account, insurer returns,
 * resubmission, cancel / reactivate placement and hold cover.
 */
@RestController
@RequestMapping("/api/v1/placement")
public class PlacementController {

  static final String VIEW =
      "hasAnyAuthority('ACCOUNT_VIEW', 'PLACEMENT_MANAGE', 'BILLING_MANAGE')";
  static final String MANAGE = "hasAuthority('PLACEMENT_MANAGE')";
  private static final String MANAGE_OR_MARKETING =
      "hasAnyAuthority('PLACEMENT_MANAGE', 'ACCOUNT_MAINTAIN')";
  private static final int MAX_PAGE = 100;

  private final PlacementQueryService queries;
  private final PlacementSlipService slips;
  private final PaymentGateService gate;
  private final InsurerReturnService returns;
  private final HoldCoverService holdCovers;
  private final PlacementBatchService batch;

  /**
   * Creates the controller.
   *
   * @param queries placement reads
   * @param slips placement slips
   * @param gate payment gate
   * @param returns insurer returns
   * @param holdCovers hold cover
   * @param batch multi-account actions
   */
  public PlacementController(
      PlacementQueryService queries,
      PlacementSlipService slips,
      PaymentGateService gate,
      InsurerReturnService returns,
      HoldCoverService holdCovers,
      PlacementBatchService batch) {
    this.queries = queries;
    this.slips = slips;
    this.gate = gate;
    this.returns = returns;
    this.holdCovers = holdCovers;
    this.batch = batch;
  }

  /**
   * One page of a workbench tab.
   *
   * @param companyId company
   * @param tab tab
   * @param text ARN (proposal number), client code or name
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/workbench")
  @PreAuthorize(VIEW)
  public PageResponse<WorkbenchRowResponse> workbench(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "FOR_PLACEMENT") WorkbenchTab tab,
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.workbench(
            companyId,
            tab,
            text,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE))),
        WorkbenchRowResponse::from);
  }

  /**
   * Tile counts of the workbench.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/workbench/counts")
  @PreAuthorize(VIEW)
  public WorkbenchCounts counts(@RequestParam Long companyId) {
    return queries.counts(companyId);
  }

  /**
   * Slip readiness of accounts with each unmet prerequisite (BRNB.069).
   *
   * @param companyId company
   * @param arn accounts
   * @return readiness per account
   */
  @GetMapping("/readiness")
  @PreAuthorize(VIEW)
  public List<ReadinessResponse> readiness(
      @RequestParam Long companyId, @RequestParam List<String> arn) {
    return slips.readiness(companyId, arn).stream().map(ReadinessResponse::from).toList();
  }

  /**
   * Placement of one account: payment gate, slips, hold covers and returns.
   *
   * @param arn Account Reference Number
   * @return view
   */
  @GetMapping("/accounts/{arn}")
  @PreAuthorize(VIEW)
  public PlacementViewResponse account(@PathVariable String arn) {
    return new PlacementViewResponse(
        GateResponse.from(gate.view(arn)),
        queries.slipsFor(arn).stream().map(SlipResponse::from).toList(),
        queries.holdCovers(arn).stream().map(HoldCoverResponse::from).toList(),
        returns.returnsOf(arn).stream().map(InsurerReturnResponse::from).toList());
  }

  /**
   * Captures a placement returned by the insurer (BRNB.034).
   *
   * @param arn placed account
   * @param request reason and remarks
   * @return the return
   */
  @PostMapping("/accounts/{arn}/insurer-return")
  @PreAuthorize(MANAGE)
  public InsurerReturnResponse insurerReturn(
      @PathVariable String arn, @Valid @RequestBody InsurerReturnRequest request) {
    return InsurerReturnResponse.from(
        returns.recordReturn(arn, request.reasonCode(), request.remarks()));
  }

  /**
   * Resubmits a returned placement after the correction.
   *
   * @param arn account returned by the insurer
   * @param request comment
   * @return the account's gate view
   */
  @PostMapping("/accounts/{arn}/resubmit")
  @PreAuthorize(MANAGE)
  public GateResponse resubmit(@PathVariable String arn, @Valid @RequestBody NoteRequest request) {
    returns.resubmit(arn, request.comment());
    return GateResponse.from(gate.view(arn));
  }

  /**
   * Cancels the placement of one or more accounts (BRNB.062).
   *
   * @param request accounts, reason and comment
   * @return outcome per account
   */
  @PostMapping("/accounts/cancel")
  @PreAuthorize(MANAGE)
  public List<ItemResult> cancel(@Valid @RequestBody CancelPlacementRequest request) {
    return batch.cancel(request.arns(), request.reasonCode(), request.comment());
  }

  /**
   * Reactivates one or more cancelled placements (BRD 2.1.16).
   *
   * @param request accounts and comment
   * @return outcome per account
   */
  @PostMapping("/accounts/reactivate")
  @PreAuthorize(MANAGE_OR_MARKETING)
  public List<ItemResult> reactivate(@Valid @RequestBody ReactivateRequest request) {
    return batch.reactivate(request.arns(), request.comment());
  }

  /**
   * Sends the hold cover request to the insurer (BRNB.072).
   *
   * @param arn account in placement
   * @param request start date and recipients
   * @return the hold cover
   */
  @PostMapping("/accounts/{arn}/hold-cover")
  @PreAuthorize(MANAGE)
  public HoldCoverResponse requestHoldCover(
      @PathVariable String arn, @Valid @RequestBody HoldCoverRequestBody request) {
    return HoldCoverResponse.from(
        holdCovers.request(arn, new HoldCoverRequest(request.startDate(), request.to())));
  }

  /**
   * Records the insurer's hold cover confirmation (BRNB.103).
   *
   * @param arn account
   * @param request insurer, reference, date and expiry
   * @return the hold cover
   */
  @PostMapping("/accounts/{arn}/hold-cover/confirm")
  @PreAuthorize(MANAGE_OR_MARKETING)
  public HoldCoverResponse confirmHoldCover(
      @PathVariable String arn, @Valid @RequestBody HoldCoverConfirmRequest request) {
    return HoldCoverResponse.from(
        holdCovers.confirm(
            arn,
            new HoldCoverConfirmation(
                request.insurerCode(),
                request.reference(),
                request.confirmedOn(),
                request.expiryDate())));
  }

  /**
   * Records that the insurer declined the hold cover.
   *
   * @param arn account
   * @param request insurer reference
   * @return the hold cover
   */
  @PostMapping("/accounts/{arn}/hold-cover/decline")
  @PreAuthorize(MANAGE_OR_MARKETING)
  public HoldCoverResponse declineHoldCover(
      @PathVariable String arn, @Valid @RequestBody NoteRequest request) {
    return HoldCoverResponse.from(holdCovers.decline(arn, request.comment()));
  }
}
