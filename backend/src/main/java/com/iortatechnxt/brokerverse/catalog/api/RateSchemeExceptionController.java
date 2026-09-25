package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.RateExceptionDtos.ExceptionRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.RateExceptionDtos.ExceptionResponse;
import com.iortatechnxt.brokerverse.catalog.service.RateSchemeExceptionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rate-scheme exceptions (BRPM.007): requested from a quotation or account, decided in My Approvals
 * through {@code /api/v1/catalog/records/RATE_SCHEME_EXCEPTION/{id}/authorize} (approve) or {@code
 * .../deactivate} (reject).
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
}
