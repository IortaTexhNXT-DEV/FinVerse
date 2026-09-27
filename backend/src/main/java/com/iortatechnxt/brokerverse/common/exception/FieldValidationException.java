package com.iortatechnxt.brokerverse.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A business rule violation tied to fields of the submitted record (e.g. the minimum-field matrix
 * of a product, BRNB.003). Mapped to HTTP 422 like {@link BusinessRuleException}, with the field
 * errors in the {@code errors} property so forms show them next to the fields.
 */
@SuppressWarnings("PMD.LooseCoupling") // a serializable, ordered map is needed on an exception
public class FieldValidationException extends BusinessRuleException {

  private static final long serialVersionUID = 1L;

  private final LinkedHashMap<String, String> fieldErrors;

  /**
   * Creates the exception.
   *
   * @param code stable error code, UPPER_SNAKE_CASE
   * @param message summary message
   * @param fieldErrors messages by field path (e.g. "items[0].engineNo")
   */
  public FieldValidationException(String code, String message, Map<String, String> fieldErrors) {
    super(code, message);
    this.fieldErrors = new LinkedHashMap<>(fieldErrors);
  }

  /**
   * Field errors in the order they were found.
   *
   * @return messages by field path
   */
  public Map<String, String> getFieldErrors() {
    return Collections.unmodifiableMap(fieldErrors);
  }
}
