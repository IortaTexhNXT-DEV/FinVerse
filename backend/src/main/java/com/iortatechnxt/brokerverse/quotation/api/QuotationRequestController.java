package com.iortatechnxt.brokerverse.quotation.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationRequestDtos.CloseRequest;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationRequestDtos.IntakeRequest;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationRequestDtos.RequestResponse;
import com.iortatechnxt.brokerverse.quotation.domain.RequestStatus;
import com.iortatechnxt.brokerverse.quotation.service.QuotationRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Quotation request inbox (BRNB.041): requests captured from e-mails (the e-mail is attached to the
 * request through the attachments API, document type REQUEST_EMAIL), uploaded or received from
 * source systems; prospect creation and closing.
 */
@RestController
@RequestMapping("/api/v1/quotation-requests")
public class QuotationRequestController {

  private static final String MAINTAIN = "hasAuthority('QUOTE_MAINTAIN')";
  private static final int MAX_PAGE = 200;

  private final QuotationRequestService requests;

  /**
   * Creates the controller.
   *
   * @param requests request service
   */
  public QuotationRequestController(QuotationRequestService requests) {
    this.requests = requests;
  }

  /**
   * Requests, newest first.
   *
   * @param companyId company
   * @param status status, all when empty
   * @param text request number, prospect or cover fragment
   * @param page page
   * @param size size
   * @return page of requests
   */
  @GetMapping
  @PreAuthorize("hasAuthority('QUOTE_VIEW')")
  public PageResponse<RequestResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) RequestStatus status,
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "receivedAt", "id"));
    return PageResponse.of(
        requests.search(companyId, status, text, pageable), RequestResponse::from);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('QUOTE_VIEW')")
  public RequestResponse get(@PathVariable Long id) {
    return RequestResponse.from(requests.get(id));
  }

  /**
   * Captures a request received by e-mail.
   *
   * @param body request
   * @return the request
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public RequestResponse create(@Valid @RequestBody IntakeRequest body) {
    return RequestResponse.from(requests.receive(body.companyId(), body.incoming()));
  }

  /**
   * Creates the prospect named by the request.
   *
   * @param id request
   * @return the request with its client
   */
  @PostMapping("/{id}/prospect")
  @PreAuthorize("hasAuthority('QUOTE_MAINTAIN') and hasAuthority('CLIENT_MAINTAIN')")
  public RequestResponse prospect(@PathVariable Long id) {
    return RequestResponse.from(requests.createProspect(id));
  }

  /**
   * Closes a request without a quotation.
   *
   * @param id request
   * @param body reason
   * @return the request
   */
  @PostMapping("/{id}/close")
  @PreAuthorize(MAINTAIN)
  public RequestResponse close(@PathVariable Long id, @Valid @RequestBody CloseRequest body) {
    return RequestResponse.from(requests.close(id, body.reason()));
  }
}
