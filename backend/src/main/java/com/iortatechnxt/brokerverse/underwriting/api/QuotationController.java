package com.iortatechnxt.brokerverse.underwriting.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.ConvertQuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyResponse;
import com.iortatechnxt.brokerverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.QuotationResponse;
import com.iortatechnxt.brokerverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationExpiryJob;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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

/** REST API for quotations: iterations, approval and conversion into a policy. */
@RestController
@RequestMapping("/api/v1/underwriting/quotations")
public class QuotationController {

  private static final String MAINTAIN = "hasAuthority('POLICY_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('POLICY_AUTHORIZE')";

  private final QuotationService service;
  private final QuotationExpiryJob expiry;

  /**
   * Creates the controller.
   *
   * @param service quotation service
   * @param expiry quotation expiry run
   */
  public QuotationController(QuotationService service, QuotationExpiryJob expiry) {
    this.service = service;
    this.expiry = expiry;
  }

  /**
   * Lists quotations.
   *
   * @param companyId company
   * @param status status filter
   * @param from issue date from
   * @param to issue date to
   * @return quotations
   */
  @GetMapping
  @PreAuthorize("hasAuthority('POLICY_VIEW')")
  public List<QuotationResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) QuotationStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.search(companyId, status, from, to).stream()
        .map(QuotationResponse::from)
        .toList();
  }

  /**
   * Gets a quotation.
   *
   * @param id id
   * @return quotation
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('POLICY_VIEW')")
  public QuotationResponse get(@PathVariable Long id) {
    return QuotationResponse.from(service.get(id));
  }

  /**
   * Creates a quotation with its first iteration.
   *
   * @param request request
   * @return quotation
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public QuotationResponse create(@Valid @RequestBody QuotationRequest request) {
    return QuotationResponse.from(service.create(request));
  }

  /**
   * Updates the terms of a draft quotation.
   *
   * @param id id
   * @param request request
   * @return quotation
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse update(
      @PathVariable Long id, @Valid @RequestBody QuotationRequest request) {
    return QuotationResponse.from(service.update(id, request));
  }

  /**
   * Adds an iteration.
   *
   * @param id id
   * @param request figures
   * @return quotation
   */
  @PostMapping("/{id}/iterations")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse iterate(
      @PathVariable Long id, @Valid @RequestBody IterationRequest request) {
    return QuotationResponse.from(service.iterate(id, request));
  }

  /**
   * Submits a quotation.
   *
   * @param id id
   * @return quotation
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse submit(@PathVariable Long id) {
    return QuotationResponse.from(service.submit(id));
  }

  /**
   * Approves a quotation.
   *
   * @param id id
   * @return quotation
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(AUTHORIZE)
  public QuotationResponse approve(@PathVariable Long id) {
    return QuotationResponse.from(service.approve(id));
  }

  /**
   * Rejects a quotation.
   *
   * @param id id
   * @param request reason
   * @return quotation
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(AUTHORIZE)
  public QuotationResponse reject(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return QuotationResponse.from(service.reject(id, request.reason()));
  }

  /**
   * Converts an approved quotation into a draft policy.
   *
   * @param id id
   * @param request options
   * @return the draft policy
   */
  @PostMapping("/{id}/convert")
  @PreAuthorize(MAINTAIN)
  public PolicyResponse convert(
      @PathVariable Long id, @Valid @RequestBody ConvertQuotationRequest request) {
    return PolicyResponse.withRisks(service.convert(id, request));
  }

  /**
   * Expires the lapsed quotations of a company now (the daily {@code QUOTATION_EXPIRY} job does the
   * same for every company); the run is recorded in the job monitor.
   *
   * @param companyId company
   * @param asOf date (default today)
   * @return number expired
   */
  @PostMapping("/expire")
  @PreAuthorize("hasAnyAuthority('POLICY_MAINTAIN', 'POLICY_AUTHORIZE')")
  public Map<String, Integer> expire(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return Map.of("expired", expiry.runFor(companyId, asOf));
  }
}
