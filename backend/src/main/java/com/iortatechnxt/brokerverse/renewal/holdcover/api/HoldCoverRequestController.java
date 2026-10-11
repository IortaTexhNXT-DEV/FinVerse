package com.iortatechnxt.brokerverse.renewal.holdcover.api;

import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAsk;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.HoldCoverRequests;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Hold cover requests of a renewal account per insurer, extensions and responses (FRRN.036). */
@RestController
@RequestMapping("/api/v1/renewal/candidates/{ref}/hold-cover-requests")
public class HoldCoverRequestController {

  private static final String ACT = "hasAnyAuthority('RNW_PROCESS','RNW_DISPOSE')";

  private final HoldCoverRequests requests;
  private final RenewalRecords records;

  /**
   * Creates the controller.
   *
   * @param requests hold cover requests
   * @param records renewals
   */
  public HoldCoverRequestController(HoldCoverRequests requests, RenewalRecords records) {
    this.requests = requests;
    this.records = records;
  }

  /**
   * The requests of a renewal and its default duration.
   *
   * @param companyId company
   * @param ref renewal
   * @return requests
   */
  @GetMapping
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  @Transactional(readOnly = true)
  public Requests list(@RequestParam Long companyId, @PathVariable String ref) {
    return new Requests(
        requests.defaultDays(records.get(companyId, ref)),
        requests.of(companyId, ref).stream().map(View::of).toList());
  }

  /**
   * Requests the hold cover, one request per insurer.
   *
   * @param companyId company
   * @param ref renewal
   * @param body duration and start, the defaults when null
   * @return the requests
   */
  @PostMapping
  @PreAuthorize(ACT)
  public List<View> request(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Body body) {
    return requests.request(companyId, ref, body.days(), body.start()).stream()
        .map(View::of)
        .toList();
  }

  /**
   * Requests the extension of the confirmed hold cover.
   *
   * @param companyId company
   * @param ref renewal
   * @param body days of the extension
   * @return the requests
   */
  @PostMapping("/extension")
  @PreAuthorize(ACT)
  public List<View> extend(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Body body) {
    return requests.extend(companyId, ref, body.days() == null ? 0 : body.days()).stream()
        .map(View::of)
        .toList();
  }

  /**
   * Records the insurer's response to a request.
   *
   * @param companyId company
   * @param ref renewal
   * @param requestNo request
   * @param response approval or rejection
   * @return the request
   */
  @PostMapping("/{requestNo}/response")
  @PreAuthorize(ACT)
  public View respond(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @PathVariable String requestNo,
      @RequestBody HoldCoverRequests.Response response) {
    return View.of(requests.respond(companyId, requestNo, response));
  }

  /**
   * The duration and start of a request.
   *
   * @param days days
   * @param start first day
   */
  public record Body(Integer days, LocalDate start) {}

  /**
   * The requests of a renewal.
   *
   * @param defaultDays default duration
   * @param requests requests
   */
  public record Requests(int defaultDays, List<View> requests) {}

  /**
   * A request.
   *
   * @param requestNo reference number
   * @param kind kind
   * @param insurerCode insurer
   * @param share share
   * @param days days
   * @param start start
   * @param end end
   * @param status status
   * @param channel MFT or EMAIL
   * @param batchNo file of the insurer
   * @param insurerRef insurer reference
   * @param remarks insurer remarks
   * @param respondedAt response time
   * @param requestedBy requester
   * @param requestedAt request time
   */
  public record View(
      String requestNo,
      String kind,
      String insurerCode,
      BigDecimal share,
      int days,
      LocalDate start,
      LocalDate end,
      String status,
      String channel,
      String batchNo,
      String insurerRef,
      String remarks,
      Instant respondedAt,
      String requestedBy,
      Instant requestedAt) {

    static View of(HoldCoverAsk a) {
      return new View(
          a.getRequestNo(),
          a.getKind(),
          a.getInsurerCode(),
          a.getSharePercent(),
          a.getDays(),
          a.getStartDate(),
          a.getEndDate(),
          a.getStatus(),
          a.getChannel(),
          a.getBatchNo(),
          a.getInsurerRef(),
          a.getResponseRemarks(),
          a.getRespondedAt(),
          a.getCreatedBy(),
          a.getCreatedAt());
    }
  }
}
