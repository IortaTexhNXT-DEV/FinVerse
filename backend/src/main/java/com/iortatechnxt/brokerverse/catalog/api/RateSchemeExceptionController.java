package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.RateExceptionDtos.Decision;
import com.iortatechnxt.brokerverse.catalog.api.dto.RateExceptionDtos.ExceptionDetail;
import com.iortatechnxt.brokerverse.catalog.api.dto.RateExceptionDtos.ExceptionRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.RateExceptionDtos.ExceptionResponse;
import com.iortatechnxt.brokerverse.catalog.service.RateSchemeExceptionService;
import jakarta.validation.Valid;
import java.util.List;
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
 * Rate-scheme exceptions (BRPM.007, FR-PM-051): requested from a quotation or account; My Approvals
 * opens the exception record, where an approver other than the requester approves it (optional
 * comment) or rejects it (reason).
 */
@RestController
@RequestMapping("/api/v1/catalog/rate-scheme-exceptions")
public class RateSchemeExceptionController {

  private final RateSchemeExceptionService exceptions;

  /**
   * Creates the controller.
   *
   * @param exceptions rate-scheme exceptions
   */
  public RateSchemeExceptionController(RateSchemeExceptionService exceptions) {
    this.exceptions = exceptions;
  }

  /**
   * Every exception, or those of one transaction, newest first.
   *
   * @param transactionRef quotation number or ARN
   * @return exceptions
   */
  @GetMapping
  @PreAuthorize(CatalogAccess.READ)
  public List<ExceptionResponse> list(@RequestParam(required = false) String transactionRef) {
    return exceptions.list(transactionRef).stream().map(ExceptionResponse::from).toList();
  }

  /**
   * Requests an exception, pending authorisation.
   *
   * @param request product, version or rate, transaction and reason
   * @return the exception with its reference
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.REQUEST_EXCEPTION)
  public ExceptionResponse request(@Valid @RequestBody ExceptionRequest request) {
    return ExceptionResponse.from(exceptions.request(request.toRequest()));
  }

  /**
   * One exception with the product name and the scheme in force.
   *
   * @param reference reference number
   * @return exception record
   */
  @GetMapping("/{reference}")
  @PreAuthorize(CatalogAccess.READ)
  public ExceptionDetail get(@PathVariable String reference) {
    return ExceptionDetail.from(exceptions.detail(reference));
  }

  /**
   * Approves a pending exception (checker, never the requester).
   *
   * @param reference reference number
   * @param decision optional comment
   * @return the approved exception
   */
  @PostMapping("/{reference}/approve")
  @PreAuthorize(CatalogAccess.DECIDE_EXCEPTION)
  public ExceptionResponse approve(
      @PathVariable String reference, @Valid @RequestBody(required = false) Decision decision) {
    return ExceptionResponse.from(
        exceptions.approve(reference, decision == null ? null : decision.comment()));
  }

  /**
   * Rejects a pending exception with its reason (checker, never the requester).
   *
   * @param reference reference number
   * @param decision the reason
   * @return the rejected exception
   */
  @PostMapping("/{reference}/reject")
  @PreAuthorize(CatalogAccess.DECIDE_EXCEPTION)
  public ExceptionResponse reject(
      @PathVariable String reference, @Valid @RequestBody Decision decision) {
    return ExceptionResponse.from(exceptions.reject(reference, decision.comment()));
  }
}
