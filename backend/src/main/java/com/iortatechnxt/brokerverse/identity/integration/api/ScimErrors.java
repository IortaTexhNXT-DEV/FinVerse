package com.iortatechnxt.brokerverse.identity.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.api.RequestBodyTooLargeException;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The errors of the provisioning interface in the SCIM error schema (RFC 7644 section 3.12), ahead
 * of the application's own problem details: a refused request is 400 {@code invalidValue} with the
 * business reason, an unreadable body 400 {@code invalidSyntax}, an oversized one 413, and any
 * other failure 500 with a reference only. No stack trace and no value of the request is returned
 * or logged.
 */
@RestControllerAdvice(
    assignableTypes = {ScimProvisioningController.class, ScimGroupsController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
class ScimErrors {

  private static final Logger LOG = LoggerFactory.getLogger(ScimErrors.class);

  private final ScimAnswers answers;

  ScimErrors(ScimAnswers answers) {
    this.answers = answers;
  }

  @ExceptionHandler(BusinessRuleException.class)
  ResponseEntity<JsonNode> refused(BusinessRuleException ex) {
    return answers.error(HttpStatus.BAD_REQUEST, "invalidValue", ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<JsonNode> unreadable(HttpMessageNotReadableException ex) {
    for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
      if (cause instanceof RequestBodyTooLargeException) {
        return answers.error(
            HttpStatus.PAYLOAD_TOO_LARGE, null, "The request is larger than allowed");
      }
    }
    return answers.error(HttpStatus.BAD_REQUEST, "invalidSyntax", "The body is not valid JSON");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<JsonNode> mediaType(HttpMediaTypeNotSupportedException ex) {
    return answers.error(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, null, "Send application/scim+json or application/json");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<JsonNode> failed(Exception ex) {
    String reference = Long.toString(System.nanoTime(), Character.MAX_RADIX);
    LOG.error("Provisioning request failed (reference {}): {}", reference, ex.getClass().getName());
    return answers.error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        null,
        "The request could not be processed (reference " + reference + ")");
  }
}
