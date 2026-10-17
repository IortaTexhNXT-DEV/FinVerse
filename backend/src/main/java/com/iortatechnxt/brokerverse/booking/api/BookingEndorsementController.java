package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.EndorsementPreviewResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.EndorsementRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.EndorsementResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.EndorsementResultResponse;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endorsements and cancellations of booked accounts (BRNB.061/076/081/094): list, live preview and
 * posting. Positive and non-financial endorsements are Processing's (BOOKING_PROCESS); negative
 * endorsements and cancellations are Adjustment's (BOOKING_ADJUST).
 */
@RestController
@RequestMapping("/api/v1/booking/endorsements")
public class BookingEndorsementController {

  private static final String ADJUST = "BOOKING_ADJUST";
  private static final String PROCESS = "BOOKING_PROCESS";

  private final EndorsementPostingService endorsements;
  private final BookingQueryService queries;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param endorsements endorsement posting
   * @param queries endorsement reads
   * @param currentUser current user
   */
  public BookingEndorsementController(
      EndorsementPostingService endorsements,
      BookingQueryService queries,
      CurrentUser currentUser) {
    this.endorsements = endorsements;
    this.queries = queries;
    this.currentUser = currentUser;
  }

  /**
   * Endorsements of a company.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return endorsements, newest first
   */
  @GetMapping
  @PreAuthorize(BookingAccess.VIEW)
  public PageResponse<EndorsementResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.endorsements(companyId, BookingController.pageOf(page, size)),
        EndorsementResponse::from);
  }

  /**
   * Live calculation and journal preview.
   *
   * @param request endorsement
   * @return preview
   */
  @PostMapping("/preview")
  @PreAuthorize(BookingAccess.VIEW)
  public EndorsementPreviewResponse preview(@Valid @RequestBody EndorsementRequest request) {
    return EndorsementPreviewResponse.from(endorsements.preview(request.toPosting()));
  }

  /**
   * Posts an endorsement or cancellation.
   *
   * @param request endorsement
   * @return endorsement number, invoice and journals
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.VIEW)
  public EndorsementResultResponse post(@Valid @RequestBody EndorsementRequest request) {
    boolean returns =
        request.type() == EndorsementType.NEGATIVE
            || request.type() == EndorsementType.CANCELLATION;
    String needed = returns ? ADJUST : PROCESS;
    if (request.type() != EndorsementType.NON_FINANCIAL && !currentUser.hasAuthority(needed)) {
      throw new AccessDeniedException(request.type() + " endorsements need " + needed);
    }
    return EndorsementResultResponse.from(endorsements.post(request.toPosting()));
  }
}
