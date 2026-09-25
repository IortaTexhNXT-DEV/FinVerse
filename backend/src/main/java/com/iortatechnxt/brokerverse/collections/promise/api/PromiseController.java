package com.iortatechnxt.brokerverse.collections.promise.api;

import com.iortatechnxt.brokerverse.collections.promise.api.dto.PromiseDtos.PromiseRequest;
import com.iortatechnxt.brokerverse.collections.promise.api.dto.PromiseDtos.PromiseResponse;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Promises to pay (BRCLXN.055): the work list by status and promised date, the promises of an
 * account, recording ({@code CLX_WORK}) and withdrawal. Evaluation is done by the job {@code
 * CLX_PROMISE_CHECK}.
 */
@RestController
@RequestMapping("/api/v1/collections/promises")
public class PromiseController {

  private static final int MAX_PAGE = 200;

  private final PromiseService promises;

  /**
   * Creates the controller.
   *
   * @param promises promises
   */
  public PromiseController(PromiseService promises) {
    this.promises = promises;
  }

  /**
   * Promises by promised date, soonest first.
   *
   * @param companyId company
   * @param status statuses
   * @param from first promised date
   * @param to last promised date
   * @param q invoice, account, client or assured
   * @param page page
   * @param size size
   * @return promises
   */
  @GetMapping
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public PageResponse<PromiseResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<PromiseStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        promises.search(
            companyId,
            status,
            from,
            to,
            q,
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE),
                Sort.by("promisedDate", "id"))),
        PromiseResponse::from);
  }

  /**
   * Promises of a collection account, newest first.
   *
   * @param invoiceNo invoice
   * @return promises
   */
  @GetMapping("/by-invoice/{invoiceNo}")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public List<PromiseResponse> byInvoice(@PathVariable String invoiceNo) {
    return promises.forInvoice(invoiceNo).stream().map(PromiseResponse::from).toList();
  }

  /**
   * Records a promise to pay.
   *
   * @param request account, dates and amount
   * @return promise
   */
  @PostMapping
  @PreAuthorize("hasAuthority('CLX_WORK')")
  @ResponseStatus(HttpStatus.CREATED)
  public PromiseResponse record(@Valid @RequestBody PromiseRequest request) {
    return PromiseResponse.from(
        promises.record(request.companyId(), request.invoiceNo(), request.toInput(), null));
  }

  /**
   * Withdraws an open promise.
   *
   * @param id promise
   * @param request reason
   * @return promise
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('CLX_WORK')")
  public PromiseResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return PromiseResponse.from(promises.cancel(id, request.reason()));
  }
}
