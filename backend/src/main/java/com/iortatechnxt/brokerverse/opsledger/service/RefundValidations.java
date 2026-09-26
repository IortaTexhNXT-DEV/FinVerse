package com.iortatechnxt.brokerverse.opsledger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.ValidationRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.ValidationTicket;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry point of the refund validations (MKT 1.11.0, ACSL 2.5.5): routes each request to the {@link
 * RefundValidationSource} bean of its validator (acsl for ACSL, cashiering for CASHIERING) and
 * hands it over when that module is not installed, so payrequest never depends on which validators
 * exist.
 */
@Service
@Transactional
public class RefundValidations {

  private final List<RefundValidationSource> sources;
  private final RefundValidationSource fallback;

  /**
   * Creates the router.
   *
   * @param sources validation beans of the installed modules (may include the hand-off default)
   * @param handoffs hand-offs of the fallback
   * @param json JSON mapper of the fallback
   */
  public RefundValidations(
      List<RefundValidationSource> sources, HandoffService handoffs, ObjectMapper json) {
    this.sources = List.copyOf(sources);
    this.fallback = new HandoffRefundValidationSource(handoffs, json);
  }

  /**
   * Opens a validation with the validator the request names.
   *
   * @param request what to validate; {@code validator} is ACSL or CASHIERING
   * @return the validator's ticket, DEFERRED when handed over
   */
  public ValidationTicket open(ValidationRequest request) {
    return sources.stream()
        .filter(s -> s.validator().equals(request.validator()))
        .findFirst()
        .orElse(fallback)
        .open(request);
  }

  /**
   * Validators served by an installed module (not handed over).
   *
   * @return validator codes
   */
  public List<String> installed() {
    return sources.stream()
        .map(RefundValidationSource::validator)
        .filter(v -> !RefundValidationSource.ANY.equals(v))
        .toList();
  }
}
