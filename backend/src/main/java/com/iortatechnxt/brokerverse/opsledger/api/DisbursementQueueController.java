package com.iortatechnxt.brokerverse.opsledger.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.opsledger.api.dto.QueueDtos.DisbursementResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.QueueDtos.DvRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
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
 * The in-app Disbursement queue (default Disbursement gateway, OQ02; RMTID.019, DBMID.001): payment
 * requests with acknowledge, DV number, paid and return. Disbursement users only ({@code
 * DISB_PROCESS}).
 */
@RestController
@RequestMapping("/api/v1/ops/disbursements")
@PreAuthorize(OpsAccess.DISBURSEMENT)
public class DisbursementQueueController {

  private final DisbursementQueueService queue;

  /**
   * Creates the controller.
   *
   * @param queue Disbursement queue
   */
  public DisbursementQueueController(DisbursementQueueService queue) {
    this.queue = queue;
  }

  /**
   * Payment requests of a company.
   *
   * @param companyId company
   * @param status statuses, optional (all when absent)
   * @param page page
   * @param size size
   * @return requests, newest first
   */
  @GetMapping
  public PageResponse<DisbursementResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<DisbursementRequest.Status> status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queue.list(
            companyId,
            status,
            PageRequest.of(Math.max(page, 0), Math.min(size, OpsAccess.MAX_PAGE))),
        DisbursementResponse::from);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/{id}")
  public DisbursementResponse get(@PathVariable Long id) {
    return DisbursementResponse.from(queue.get(id));
  }

  /**
   * Acknowledges receipt.
   *
   * @param id request
   * @return request
   */
  @PostMapping("/{id}/acknowledge")
  public DisbursementResponse acknowledge(@PathVariable Long id) {
    return DisbursementResponse.from(queue.acknowledge(id));
  }

  /**
   * Assigns the DV number.
   *
   * @param id request
   * @param request DV number
   * @return request
   */
  @PostMapping("/{id}/dv")
  public DisbursementResponse assignDv(
      @PathVariable Long id, @Valid @RequestBody DvRequest request) {
    return DisbursementResponse.from(queue.assignDv(id, request.dvNo()));
  }

  /**
   * Marks the payment released.
   *
   * @param id request
   * @return request
   */
  @PostMapping("/{id}/paid")
  public DisbursementResponse markPaid(@PathVariable Long id) {
    return DisbursementResponse.from(queue.markPaid(id));
  }

  /**
   * Returns a request to its source.
   *
   * @param id request
   * @param request reason
   * @return request
   */
  @PostMapping("/{id}/return")
  public DisbursementResponse returnToSource(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return DisbursementResponse.from(queue.returnToSource(id, request.reason()));
  }

  /**
   * Cancels a request or its approved DV (DIS 2.20.0).
   *
   * @param id request
   * @param request reason
   * @return request
   */
  @PostMapping("/{id}/cancel")
  public DisbursementResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return DisbursementResponse.from(queue.cancel(id, request.reason()));
  }
}
