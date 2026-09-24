package com.iortatechnxt.brokerverse.common.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into RFC 7807 problem responses.
 *
 * <p>Every error response carries {@code code} (stable, for UI/support) and {@code detail}.
 * Unexpected errors are logged with a correlation-friendly message and never leak internals.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String CODE = "code";

  /**
   * Handles business rule violations.
   *
   * @param ex exception
   * @return problem detail (422)
   */
  @ExceptionHandler(BusinessRuleException.class)
  public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage());
  }

  /**
   * Handles missing resources.
   *
   * @param ex exception
   * @return problem detail (404)
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
  }

  /**
   * Handles duplicate business keys.
   *
   * @param ex exception
   * @return problem detail (409)
   */
  @ExceptionHandler(DuplicateResourceException.class)
  public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
    return problem(HttpStatus.CONFLICT, "DUPLICATE", ex.getMessage());
  }

  /**
   * Handles concurrent modification of the same record.
   *
   * @param ex exception
   * @return problem detail (409)
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    LOG.info("Optimistic lock conflict: {}", ex.getMessage());
    return problem(
        HttpStatus.CONFLICT,
        "CONCURRENT_MODIFICATION",
        "The record was changed by another user. Please reload and try again.");
  }

  /**
   * Handles bean validation failures and lists field errors.
   *
   * @param ex exception
   * @return problem detail (400)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
    ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request");
    pd.setProperty("errors", errors);
    return pd;
  }

  /**
   * Handles malformed request arguments.
   *
   * @param ex exception
   * @return problem detail (400)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
  }

  /**
   * Handles request bodies that are not valid JSON or do not match the expected types, and missing
   * or mistyped query/path parameters. Details of the parser are not exposed.
   *
   * @param ex exception
   * @return problem detail (400)
   */
  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ProblemDetail handleUnreadableRequest(Exception ex) {
    String detail =
        switch (ex) {
          case MissingServletRequestParameterException m ->
              "Required parameter '" + m.getParameterName() + "' is missing";
          case MethodArgumentTypeMismatchException m ->
              "Parameter '" + m.getName() + "' has an invalid value";
          default -> "The request body is not valid JSON or has fields of the wrong type";
        };
    return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", detail);
  }

  /**
   * Handles requests for routes that do not exist (e.g. a mistyped API path).
   *
   * @param ex exception
   * @return problem detail (404)
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail handleNoRoute(NoResourceFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such endpoint: " + ex.getResourcePath());
  }

  /**
   * Handles uploads above the multipart limit (the attachment service applies the business limit).
   *
   * @param ex exception
   * @return problem detail (413)
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
    return problem(
        HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "The uploaded file is too large");
  }

  /**
   * Handles failed logins.
   *
   * @param ex exception
   * @return problem detail (401)
   */
  @ExceptionHandler({BadCredentialsException.class, LockedException.class})
  public ProblemDetail handleAuthentication(RuntimeException ex) {
    return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage());
  }

  /**
   * Handles missing permissions.
   *
   * @param ex exception
   * @return problem detail (403)
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    return problem(
        HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not permitted to perform this action");
  }

  /**
   * Last-resort handler: logs the failure with a reference the user can quote to support and
   * returns a generic message (internal details are never exposed to clients).
   *
   * @param ex exception
   * @return problem detail (500)
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    String reference = UUID.randomUUID().toString();
    LOG.error("Unexpected error, reference {}", reference, ex);
    ProblemDetail pd =
        problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected error occurred. Quote reference " + reference + " to support.");
    pd.setProperty("reference", reference);
    return pd;
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setProperty(CODE, code);
    return pd;
  }
}
