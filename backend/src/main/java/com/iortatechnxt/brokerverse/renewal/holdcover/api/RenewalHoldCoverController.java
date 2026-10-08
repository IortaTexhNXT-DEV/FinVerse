package com.iortatechnxt.brokerverse.renewal.holdcover.api;

import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.RenewalHoldCoverService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hold cover of a renewal (BRRN.042; FR-RN-086): the current hold cover, the request from the
 * renewal by the Marketing AO or Processing, the insurer's confirmation or decline recorded by
 * Processing, and the cancellation with a reason.
 */
@RestController
@RequestMapping("/api/v1/renewal/candidates/{ref}/hold-cover")
public class RenewalHoldCoverController {

  private static final String VIEW = "hasAuthority('RNW_VIEW')";
  private static final String REQUEST =
      "hasAnyAuthority('RNW_DISPOSE','RNW_PROCESS','RNW_INSURER')";
  private static final String CONFIRM = "hasAnyAuthority('RNW_PROCESS','RNW_INSURER')";

  private final RenewalHoldCoverService holdCovers;

  /**
   * Creates the controller.
   *
   * @param holdCovers hold covers of renewals
   */
  public RenewalHoldCoverController(RenewalHoldCoverService holdCovers) {
    this.holdCovers = holdCovers;
  }

  /**
   * The current hold cover and the durations offered.
   *
   * @param companyId company
   * @param ref renewal
   * @return view
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public HoldCoverPanel get(@RequestParam Long companyId, @PathVariable String ref) {
    return new HoldCoverPanel(
        holdCovers.durations(),
        holdCovers.current(companyId, ref).map(HoldCoverView::of).orElse(null));
  }

  /**
   * Requests the hold cover from the insurer.
   *
   * @param companyId company
   * @param ref renewal
   * @param request start, duration and remarks
   * @return hold cover
   */
  @PostMapping("/request")
  @PreAuthorize(REQUEST)
  public HoldCoverView request(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestBody RenewalHoldCoverService.Request request) {
    return HoldCoverView.of(holdCovers.request(companyId, ref, request));
  }

  /**
   * Records the insurer's confirmation.
   *
   * @param companyId company
   * @param ref renewal
   * @param confirmation reference, date, validity and conditions
   * @return hold cover
   */
  @PostMapping("/confirm")
  @PreAuthorize(CONFIRM)
  public HoldCoverView confirm(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestBody RenewalHoldCoverService.Confirmation confirmation) {
    return HoldCoverView.of(holdCovers.confirm(companyId, ref, confirmation));
  }

  /**
   * Records the insurer's decline.
   *
   * @param companyId company
   * @param ref renewal
   * @param request insurer reference as the reason text
   * @return hold cover
   */
  @PostMapping("/decline")
  @PreAuthorize(CONFIRM)
  public HoldCoverView decline(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody ReasonRequest request) {
    return HoldCoverView.of(holdCovers.decline(companyId, ref, request.reason()));
  }

  /**
   * Cancels the hold cover with a reason.
   *
   * @param companyId company
   * @param ref renewal
   * @param request reason
   * @return hold cover
   */
  @PostMapping("/cancel")
  @PreAuthorize(REQUEST)
  public HoldCoverView cancel(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody ReasonRequest request) {
    return HoldCoverView.of(holdCovers.cancel(companyId, ref, request.reason()));
  }

  /**
   * The hold cover panel of a renewal.
   *
   * @param durations durations offered, the default first
   * @param current current hold cover, null when none
   */
  public record HoldCoverPanel(List<Integer> durations, HoldCoverView current) {}

  /**
   * A hold cover.
   *
   * @param status status
   * @param insurerCode insurer
   * @param startDate first day
   * @param expiryDate last day
   * @param durationDays duration requested
   * @param expiringPolicyNo expiring policy
   * @param remarks remarks of the request
   * @param requestedBy requester
   * @param requestedAt request time (as recorded)
   * @param insurerRef insurer reference
   * @param confirmedOn confirmation date
   * @param conditions conditions of the confirmation
   * @param cancelReason reason of a cancellation
   */
  public record HoldCoverView(
      HoldCoverStatus status,
      String insurerCode,
      LocalDate startDate,
      LocalDate expiryDate,
      Integer durationDays,
      String expiringPolicyNo,
      String remarks,
      String requestedBy,
      java.time.Instant requestedAt,
      String insurerRef,
      LocalDate confirmedOn,
      String conditions,
      String cancelReason) {

    /**
     * Maps a hold cover.
     *
     * @param h hold cover
     * @return view
     */
    public static HoldCoverView of(HoldCover h) {
      return new HoldCoverView(
          h.getStatus(),
          h.getInsurerCode(),
          h.getStartDate(),
          h.getExpiryDate(),
          h.getDetails().durationDays(),
          h.getDetails().expiringPolicyNo(),
          h.getDetails().remarks(),
          h.getDetails().requestedBy() == null ? h.getCreatedBy() : h.getDetails().requestedBy(),
          h.getCreatedAt(),
          h.getInsurerRef(),
          h.getConfirmedOn(),
          h.getConditions(),
          h.getCancelReason());
    }
  }
}
