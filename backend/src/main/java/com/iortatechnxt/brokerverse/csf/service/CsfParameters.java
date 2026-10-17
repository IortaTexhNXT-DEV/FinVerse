package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.stereotype.Component;

/**
 * Business parameters of the Customer Servicing Facility (FRS section 9.1, category
 * CUSTOMER_SERVICE), with the defaults of the design when a parameter is missing.
 */
@Component
public class CsfParameters {

  private static final int DEFAULT_HISTORY_MONTHS = 12;
  private static final int DEFAULT_MIN_MATCHES = 2;
  private static final int DEFAULT_MAX_FAILS = 3;
  private static final int DEFAULT_VALID_MINUTES = 30;
  private static final int DEFAULT_MIN_CHARS = 3;
  private static final int DEFAULT_MAX_RESULTS = 50;

  private final SystemParameterService parameters;

  /**
   * Creates the reader.
   *
   * @param parameters business parameters
   */
  public CsfParameters(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Months of payment history shown by default.
   *
   * @return months
   */
  public int paymentHistoryMonths() {
    return parameters.intValue(CsfCodes.PAYMENT_HISTORY_MONTHS, DEFAULT_HISTORY_MONTHS);
  }

  /**
   * Checks that must match for a passed verification.
   *
   * @return count
   */
  public int verifyMinMatches() {
    return parameters.intValue(CsfCodes.VERIFY_MIN_MATCHES, DEFAULT_MIN_MATCHES);
  }

  /**
   * Failed verifications of one client in a day that raise the alert.
   *
   * @return count
   */
  public int verifyMaxFails() {
    return parameters.intValue(CsfCodes.VERIFY_MAX_FAILS, DEFAULT_MAX_FAILS);
  }

  /**
   * Minutes a passed verification stays valid.
   *
   * @return minutes
   */
  public int verificationValidMinutes() {
    return parameters.intValue(CsfCodes.VERIFICATION_VALID_MINUTES, DEFAULT_VALID_MINUTES);
  }

  /**
   * Whether contact changes are sent to the legacy systems.
   *
   * @return true when enabled
   */
  public boolean legacySyncEnabled() {
    return Boolean.parseBoolean(parameters.text(CsfCodes.LEGACY_SYNC_ENABLED, "false").strip());
  }

  /**
   * Minimum characters of a name search.
   *
   * @return characters
   */
  public int searchMinChars() {
    return parameters.intValue(CsfCodes.SEARCH_MIN_CHARS, DEFAULT_MIN_CHARS);
  }

  /**
   * Maximum clients a search returns.
   *
   * @return clients
   */
  public int searchMaxResults() {
    return parameters.intValue(CsfCodes.SEARCH_MAX_RESULTS, DEFAULT_MAX_RESULTS);
  }
}
